package hcmute.edu.vn.findora.widget;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * ImageView hỗ trợ:
 * - Pinch to zoom (0.5x – 5x)
 * - Double tap to zoom in/out
 * - Pan khi đang zoom
 * - Swipe bất kỳ hướng để dismiss khi scale = 1x
 */
public class ZoomableImageView extends AppCompatImageView {

    public interface OnDismissListener {
        void onDismiss();
    }

    private OnDismissListener dismissListener;

    // Scale
    private final Matrix matrix = new Matrix();
    private float currentScale = 1f;
    private float minScale = 1f;   // được set sau khi ảnh load, = scale fit-screen
    private static final float MAX_SCALE = 5f;

    // Swipe dismiss
    private float swipeStartX, swipeStartY;
    private static final float SWIPE_THRESHOLD = 120f;

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    // Kích thước ảnh và view
    private float imageWidth, imageHeight;
    private float viewWidth, viewHeight;
    private float lastFocusX, lastFocusY;
    private float translateX = 0f, translateY = 0f;

    public ZoomableImageView(Context context) {
        super(context);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.MATRIX);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                float newScale = currentScale * factor;
                newScale = Math.max(minScale, Math.min(MAX_SCALE, newScale));
                float actualFactor = newScale / currentScale;
                currentScale = newScale;

                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();

                matrix.postScale(actualFactor, actualFactor, focusX, focusY);
                clampTranslation();
                setImageMatrix(matrix);
                return true;
            }
        });

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (currentScale > minScale + 0.1f) {
                    // Reset về fit-screen
                    animateToScale(minScale, viewWidth / 2f, viewHeight / 2f);
                } else {
                    // Zoom vào 2.5x tại điểm chạm
                    animateToScale(minScale * 2.5f, e.getX(), e.getY());
                }
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (currentScale > minScale + 0.05f) {
                    // Pan khi đang zoom
                    matrix.postTranslate(-distanceX, -distanceY);
                    clampTranslation();
                    setImageMatrix(matrix);
                    return true;
                }
                return false;
            }
        });
    }

    public void setOnDismissListener(OnDismissListener listener) {
        this.dismissListener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
        resetMatrix();
    }

    public void resetMatrix() {
        if (getDrawable() == null) return;
        imageWidth = getDrawable().getIntrinsicWidth();
        imageHeight = getDrawable().getIntrinsicHeight();

        float scaleX = viewWidth / imageWidth;
        float scaleY = viewHeight / imageHeight;
        float scale = Math.min(scaleX, scaleY);

        // Lưu lại làm MIN_SCALE — đây là scale fit-screen thực tế
        minScale = scale;

        float dx = (viewWidth - imageWidth * scale) / 2f;
        float dy = (viewHeight - imageHeight * scale) / 2f;

        matrix.reset();
        matrix.postScale(scale, scale);
        matrix.postTranslate(dx, dy);
        currentScale = scale;
        setImageMatrix(matrix);
    }

    private void clampTranslation() {
        if (getDrawable() == null) return;
        float[] values = new float[9];
        matrix.getValues(values);

        float transX = values[Matrix.MTRANS_X];
        float transY = values[Matrix.MTRANS_Y];
        float scaleVal = values[Matrix.MSCALE_X];

        float scaledW = imageWidth * scaleVal;
        float scaledH = imageHeight * scaleVal;

        float minX = scaledW > viewWidth ? viewWidth - scaledW : (viewWidth - scaledW) / 2f;
        float maxX = scaledW > viewWidth ? 0f : (viewWidth - scaledW) / 2f;
        float minY = scaledH > viewHeight ? viewHeight - scaledH : (viewHeight - scaledH) / 2f;
        float maxY = scaledH > viewHeight ? 0f : (viewHeight - scaledH) / 2f;

        float clampedX = Math.max(minX, Math.min(maxX, transX));
        float clampedY = Math.max(minY, Math.min(maxY, transY));

        matrix.postTranslate(clampedX - transX, clampedY - transY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        // Swipe to dismiss chỉ khi không zoom
        if (currentScale <= minScale + 0.05f) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    swipeStartX = event.getX();
                    swipeStartY = event.getY();
                    break;
                case MotionEvent.ACTION_UP:
                    float dx = event.getX() - swipeStartX;
                    float dy = event.getY() - swipeStartY;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    if (dist > SWIPE_THRESHOLD && dismissListener != null) {
                        dismissListener.onDismiss();
                        return true;
                    }
                    break;
            }
        }

        return true;
    }

    private void animateToScale(float targetScale, float focusX, float focusY) {
        float startScale = currentScale;
        float[] startValues = new float[9];
        matrix.getValues(startValues);

        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(250);
        animator.setInterpolator(new DecelerateInterpolator());

        Matrix startMatrix = new Matrix(matrix);

        animator.addUpdateListener(anim -> {
            float fraction = (float) anim.getAnimatedValue();
            float scale = startScale + (targetScale - startScale) * fraction;
            float factor = scale / currentScale;
            currentScale = scale;
            matrix.postScale(factor, factor, focusX, focusY);
            clampTranslation();
            setImageMatrix(matrix);
        });

        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (targetScale <= minScale + 0.05f) {
                    resetMatrix();
                }
            }
        });

        animator.start();
    }
}
