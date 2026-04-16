package hcmute.edu.vn.findora.ml;

import android.content.Context;
import android.graphics.Bitmap;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.InterpreterApi;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.List;
import java.util.Map;

public class ImageClassifier {

    private static final String MODEL_NAME = "mobilenet_v1_1.0_224_quant.tflite";
    private static final String LABEL_FILE = "labels_mobilenet_quant_v1_224.txt";
    private static final int IMAGE_SIZE = 224;

    private InterpreterApi interpreter;
    private List<String> labels;

    public ImageClassifier(Context context) throws IOException {
        MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(context, MODEL_NAME);
        InterpreterApi.Options options = new InterpreterApi.Options();
        options.setRuntime(InterpreterApi.Options.TfLiteRuntime.FROM_SYSTEM_ONLY);
        interpreter = InterpreterApi.create(tfliteModel, options);
        labels = FileUtil.loadLabels(context, LABEL_FILE);
    }

    public Result classify(Bitmap bitmap) {
        if (interpreter == null) return null;

        // Create TensorImage for preprocessing
        TensorImage tensorImage = new TensorImage(interpreter.getInputTensor(0).dataType());
        tensorImage.load(bitmap);

        // MobileNet V1 requires 224x224
        // The Quantized version relies on UINT8 input, no NormalizeOp needed usually, 
        // but let's keep resize
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(IMAGE_SIZE, IMAGE_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .build();
        tensorImage = imageProcessor.process(tensorImage);

        // Pre-allocate buffer for probabilities
        TensorBuffer probabilityBuffer = TensorBuffer.createFixedSize(
                interpreter.getOutputTensor(0).shape(), 
                interpreter.getOutputTensor(0).dataType()
        );

        // Run inference
        interpreter.run(tensorImage.getBuffer(), probabilityBuffer.getBuffer());

        // We could extract labels easily with TensorLabel
        // For quantization, typically we use a DequantizeOp but TensorFlow Lite Support 
        // handles it if we map the labels directly. Since it's UINT8 output, returning range [0, 255],
        // we can convert to [0.0, 1.0] manually if needed.
        
        // Actually TensorLabel handles it mapping to Map<String, Float> if floating point, 
        // but for quant models probability is 0-255. We divide by 255.0f for Float output.
        // Let's use a simpler custom loop.
        float[] probabilities = probabilityBuffer.getFloatArray();
        float maxProb = 0;
        int maxIndex = -1;
        
        for (int i = 0; i < probabilities.length; i++) {
            // In Quantized Model, probabilities are 0-255 as uint8 cast to float array 
            // by the buffer if we request getFloatArray. Wait, TensorBuffer.getFloatArray() on UINT8
            // just casts each byte to float [0.0, 255.0].
            float p = probabilities[i] / 255.0f;
            if (p > maxProb) {
                maxProb = p;
                maxIndex = i;
            }
        }

        if (maxIndex != -1 && maxIndex < labels.size()) {
            return new Result(labels.get(maxIndex), maxProb);
        }
        return null;
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
        }
    }

    public static class Result {
        public final String label;
        public final float confidence;

        public Result(String label, float confidence) {
            this.label = label;
            this.confidence = confidence;
        }
    }
}
