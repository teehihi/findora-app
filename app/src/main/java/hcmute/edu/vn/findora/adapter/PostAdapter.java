package hcmute.edu.vn.findora.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import hcmute.edu.vn.findora.R;
import hcmute.edu.vn.findora.model.Post;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private final Context context;
    private final List<Post> postList;
    // Cache like state: postId -> int[]{likeCount, isLiked (0/1)}
    private final java.util.HashMap<String, int[]> likeStateCache = new java.util.HashMap<>();
    // Cache user info: userId -> String[]{fullName, photoUrl}
    private final java.util.HashMap<String, String[]> userInfoCache = new java.util.HashMap<>();
    // SoundPool cho like/unlike sound effect
    private android.media.SoundPool soundPool;
    private int soundLike = -1;
    private boolean soundsLoaded = false;

    public PostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
        initSounds();
    }

    private void initSounds() {
        android.media.AudioAttributes attrs = new android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();
        soundPool = new android.media.SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(attrs)
            .build();
        soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
            if (status == 0) soundsLoaded = true;
        });
        try {
            soundLike = soundPool.load(context, R.raw.like_sound_x2, 1);
        } catch (Exception ignored) { }
    }

    /** Gọi khi adapter không còn dùng nữa (Activity/Fragment onDestroy) */
    public void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    private void playSound(int soundId) {
        if (soundPool != null && soundsLoaded && soundId > 0) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f);
        }
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        holder.tvTitle.setText(post.getTitle());

        // Description: 2 lines collapsed with "... Xem thêm" inline, click to expand
        holder.tvDescription.setTag(false); // false = collapsed
        setDescriptionWithReadMore(holder.tvDescription, post.getDescription(), false);
        holder.tvDescription.setOnClickListener(v -> {
            boolean expanded = (boolean) holder.tvDescription.getTag();
            setDescriptionWithReadMore(holder.tvDescription, post.getDescription(), !expanded);
            holder.tvDescription.setTag(!expanded);
        });

        // Badge
        if ("lost".equals(post.getType())) {
            holder.tvType.setText("THẤT LẠC");
            holder.tvType.setBackgroundResource(R.drawable.bg_badge_lost);
            holder.tvType.setTextColor(android.graphics.Color.parseColor("#FFFFFF")); // White text
        } else {
            holder.tvType.setText("TÌM THẤY");
            holder.tvType.setBackgroundResource(R.drawable.bg_badge_found);
            holder.tvType.setTextColor(android.graphics.Color.parseColor("#FFFFFF")); // White text
        }

        // Image - click to open fullscreen
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            holder.flImageContainer.setVisibility(View.VISIBLE);
            Glide.with(context).load(post.getImageUrl())
                 .placeholder(R.drawable.bg_img_placeholder).centerCrop().into(holder.ivPostImage);
            holder.flImageContainer.setOnClickListener(v -> openFullImage(post, holder.isLiked, holder.currentLikeCount, holder.currentCommentCount));
        } else {
            holder.flImageContainer.setVisibility(View.GONE);
            Glide.with(context).clear(holder.ivPostImage);
        }

        // Relative time
        if (post.getCreatedAt() != null) {
            holder.tvCreatedAt.setText(getRelativeTime(post.getCreatedAt().toDate()));
        } else {
            holder.tvCreatedAt.setText("Vừa xong");
        }

        // Location
        if (holder.tvLocation != null) {
            String address = post.getAddress();
            holder.tvLocation.setText(address != null && !address.isEmpty() ? address : "Đang cập nhật khu vực");
        }

        // Load poster name + avatar (có cache)
        if (post.getUserId() != null && !post.getUserId().isEmpty()) {
            String userId = post.getUserId();
            String[] cached = userInfoCache.get(userId);
            if (cached != null) {
                // Dùng cache ngay, không cần Firestore
                if (holder.tvPosterName != null) holder.tvPosterName.setText(cached[0]);
                if (cached[1] != null && !cached[1].isEmpty()) {
                    holder.ivPosterAvatar.setImageTintList(null);
                    Glide.with(context).load(cached[1]).circleCrop()
                        .placeholder(R.drawable.ic_person).error(R.drawable.ic_person)
                        .into(holder.ivPosterAvatar);
                } else {
                    holder.ivPosterAvatar.setImageResource(R.drawable.ic_person);
                }
            } else {
                // Chưa có cache, set placeholder trước rồi fetch
                if (holder.tvPosterName != null) holder.tvPosterName.setText("Người dùng");
                holder.ivPosterAvatar.setImageResource(R.drawable.ic_person);
                FirebaseFirestore.getInstance().collection("users").document(userId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String name = doc.getString("fullName");
                            String photoUrl = doc.getString("photoUrl");
                            String displayName = name != null ? name : "Người dùng";
                            String photo = photoUrl != null ? photoUrl : "";
                            // Lưu vào cache
                            userInfoCache.put(userId, new String[]{displayName, photo});
                            if (holder.tvPosterName != null) holder.tvPosterName.setText(displayName);
                            if (!photo.isEmpty()) {
                                holder.ivPosterAvatar.setImageTintList(null);
                                Glide.with(context).load(photo).circleCrop()
                                    .placeholder(R.drawable.ic_person).error(R.drawable.ic_person)
                                    .into(holder.ivPosterAvatar);
                            }
                        }
                    });
            }
        }

        // Like state
        loadLikeState(holder, post);

        // Comment count
        loadCommentCount(holder, post);

        // Header click → open detail
        if (holder.layoutHeader != null) {
            holder.layoutHeader.setOnClickListener(v -> openDetail(post));
        }

        // Title area click → open detail
        if (holder.layoutTitleDesc != null) {
            holder.layoutTitleDesc.setOnClickListener(v -> openDetail(post));
        }

        holder.btnLikeAction.setOnClickListener(v -> toggleLike(holder, post));
        holder.btnCommentAction.setOnClickListener(v -> openDetail(post));
    }

    private void setDescriptionWithReadMore(TextView tv, String text, boolean expanded) {
        if (text == null || text.isEmpty()) {
            tv.setText("");
            return;
        }
        if (expanded) {
            tv.setMaxLines(Integer.MAX_VALUE);
            tv.setEllipsize(null);
            tv.setText(text);
            return;
        }
        // Collapsed: dùng StaticLayout đo trước, set text 1 lần, không dùng tv.post()
        tv.setMaxLines(2);
        tv.setEllipsize(null);

        int tvWidth = tv.getWidth();
        if (tvWidth <= 0) {
            // View chưa được đo (lần đầu inflate), dùng ellipsize thông thường
            tv.setMaxLines(2);
            tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tv.setText(text);
            return;
        }

        // Kiểm tra text có vượt 2 dòng không
        android.text.StaticLayout checkLayout = android.text.StaticLayout.Builder
            .obtain(text, 0, text.length(), tv.getPaint(), tvWidth)
            .setMaxLines(Integer.MAX_VALUE)
            .build();

        if (checkLayout.getLineCount() <= 2) {
            tv.setText(text);
            return;
        }

        // Cắt text vừa 2 dòng với "... Xem thêm"
        String readMore = " Xem thêm";
        String suffix = "... ";
        int lineEnd = checkLayout.getLineEnd(1);
        String visibleText = text.substring(0, lineEnd).trim();

        while (visibleText.length() > 0) {
            String candidate = visibleText + suffix + readMore;
            android.text.StaticLayout sl = android.text.StaticLayout.Builder
                .obtain(candidate, 0, candidate.length(), tv.getPaint(), tvWidth)
                .setMaxLines(2)
                .build();
            if (sl.getLineCount() <= 2) break;
            visibleText = visibleText.substring(0, visibleText.length() - 1).trim();
        }

        android.text.SpannableString spannable = new android.text.SpannableString(visibleText + suffix + readMore);
        int start = (visibleText + suffix).length();
        spannable.setSpan(
            new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
            start, spannable.length(),
            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        spannable.setSpan(
            new android.text.style.ForegroundColorSpan(
                androidx.core.content.ContextCompat.getColor(context, R.color.on_surface)),
            start, spannable.length(),
            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        tv.setText(spannable);
    }

    private void openFullImage(Post post, boolean isLiked, int likeCount, int commentCount) {
        android.app.Dialog dialog = new android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_full_image);

        hcmute.edu.vn.findora.widget.ZoomableImageView ivFull = dialog.findViewById(R.id.ivFullImage);
        View btnClose = dialog.findViewById(R.id.btnCloseFullImage);
        TextView tvFullName = dialog.findViewById(R.id.tvFullName);
        TextView tvFullTime = dialog.findViewById(R.id.tvFullTime);
        TextView tvFullDesc = dialog.findViewById(R.id.tvFullDescription);
        ImageView ivFullAvatar = dialog.findViewById(R.id.ivFullAvatar);
        ImageView ivFullLikeIcon = dialog.findViewById(R.id.ivFullLikeIcon);
        TextView tvFullLikeCount = dialog.findViewById(R.id.tvFullLikeCount);
        TextView tvFullCommentCount = dialog.findViewById(R.id.tvFullCommentCount);
        LinearLayout btnFullLike = dialog.findViewById(R.id.btnFullLike);
        LinearLayout btnFullComment = dialog.findViewById(R.id.btnFullComment);

        // Swipe bất kỳ hướng để dismiss khi không zoom
        ivFull.setOnDismissListener(dialog::dismiss);

        // Load image — dùng listener để reset matrix sau khi ảnh load xong
        Glide.with(context)
            .load(post.getImageUrl())
            .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                @Override
                public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e,
                        Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                        boolean isFirstResource) { return false; }
                @Override
                public boolean onResourceReady(android.graphics.drawable.Drawable resource,
                        Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                        com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                    ivFull.post(ivFull::resetMatrix);
                    return false;
                }
            })
            .into(ivFull);

        // Description
        if (post.getDescription() != null && !post.getDescription().isEmpty()) {
            tvFullDesc.setText(post.getDescription());
            tvFullDesc.setVisibility(View.VISIBLE);
        } else {
            tvFullDesc.setVisibility(View.GONE);
        }

        // Time
        if (post.getCreatedAt() != null) {
            tvFullTime.setText(getRelativeTime(post.getCreatedAt().toDate()));
        }

        // Like state
        if (isLiked) {
            ivFullLikeIcon.setImageResource(R.drawable.ic_like_feather_filled);
        } else {
            ivFullLikeIcon.setImageResource(R.drawable.ic_like_feather);
        }
        if (likeCount > 0) {
            tvFullLikeCount.setText(String.valueOf(likeCount));
            tvFullLikeCount.setVisibility(View.VISIBLE);
        }
        if (commentCount > 0) {
            tvFullCommentCount.setText(String.valueOf(commentCount));
            tvFullCommentCount.setVisibility(View.VISIBLE);
        }

        // Load poster info
        if (post.getUserId() != null && !post.getUserId().isEmpty()) {
            FirebaseFirestore.getInstance().collection("users").document(post.getUserId()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("fullName");
                        tvFullName.setText(name != null ? name : "Người dùng");
                        String photoUrl = doc.getString("photoUrl");
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            ivFullAvatar.setImageTintList(null);
                            Glide.with(context).load(photoUrl).circleCrop().into(ivFullAvatar);
                        }
                    }
                });
        }

        // Like toggle
        final boolean[] liked = {isLiked};
        btnFullLike.setOnClickListener(v -> {
            // Optimistic update + sound ngay khi bấm
            liked[0] = !liked[0];
            playSound(soundLike);
            ivFullLikeIcon.setImageResource(liked[0] ? R.drawable.ic_like_feather_filled : R.drawable.ic_like_feather);
            // Cập nhật cache
            int cachedCount = likeStateCache.containsKey(post.getId())
                ? likeStateCache.get(post.getId())[0] : likeCount;
            int newCount = liked[0] ? cachedCount + 1 : cachedCount - 1;
            if (newCount < 0) newCount = 0;
            likeStateCache.put(post.getId(), new int[]{newCount, liked[0] ? 1 : 0});
            if (newCount > 0) {
                tvFullLikeCount.setText(String.valueOf(newCount));
                tvFullLikeCount.setVisibility(View.VISIBLE);
            } else {
                tvFullLikeCount.setVisibility(View.GONE);
            }

            String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            if (currentUserId == null || post.getId() == null) return;
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("posts").document(post.getId()).get()
                .addOnSuccessListener(doc -> {
                    @SuppressWarnings("unchecked")
                    List<String> likes = (List<String>) doc.get("likes");
                    if (likes == null) likes = new ArrayList<>();
                    if (!liked[0]) likes.remove(currentUserId);
                    else if (!likes.contains(currentUserId)) likes.add(currentUserId);
                    final List<String> finalLikes = likes;
                    db.collection("posts").document(post.getId()).update("likes", finalLikes)
                        .addOnSuccessListener(unused -> {
                            ivFullLikeIcon.setImageResource(liked[0] ? R.drawable.ic_like_feather_filled : R.drawable.ic_like_feather);
                            int serverCount = finalLikes.size();
                            likeStateCache.put(post.getId(), new int[]{serverCount, liked[0] ? 1 : 0});
                            if (serverCount > 0) {
                                tvFullLikeCount.setText(String.valueOf(serverCount));
                                tvFullLikeCount.setVisibility(View.VISIBLE);
                            } else {
                                tvFullLikeCount.setVisibility(View.GONE);
                            }
                        });
                });
        });

        btnFullComment.setOnClickListener(v -> {
            dialog.dismiss();
            openDetail(post);
        });

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        // Nút 3 chấm → menu lưu ảnh
        View btnMore = dialog.findViewById(R.id.btnMoreOptions);
        if (btnMore != null) {
            btnMore.setOnClickListener(v -> {
                android.widget.PopupMenu popup = new android.widget.PopupMenu(context, btnMore);
                popup.getMenu().add(0, 1, 0, "Lưu ảnh");
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == 1) {
                        saveImageToGallery(post.getImageUrl());
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }

        dialog.show();
    }

    private String getRelativeTime(Date date) {
        long now = System.currentTimeMillis();
        long diff = now - date.getTime();
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        java.util.Calendar postCal = java.util.Calendar.getInstance();
        postCal.setTime(date);
        java.util.Calendar nowCal = java.util.Calendar.getInstance();

        int postYear = postCal.get(java.util.Calendar.YEAR);
        int nowYear = nowCal.get(java.util.Calendar.YEAR);
        int postMonth = postCal.get(java.util.Calendar.MONTH);
        int nowMonth = nowCal.get(java.util.Calendar.MONTH);
        int postDay = postCal.get(java.util.Calendar.DAY_OF_YEAR);
        int nowDay = nowCal.get(java.util.Calendar.DAY_OF_YEAR);

        if (seconds < 60) return "Vừa xong";
        if (minutes < 60) return minutes + " phút trước";
        if (hours < 24 && postDay == nowDay) return hours + " giờ trước";

        // Hôm qua
        if (postYear == nowYear && nowDay - postDay == 1) {
            return "Hôm qua lúc " + new SimpleDateFormat("HH:mm", new Locale("vi")).format(date);
        }

        // Cùng tháng, cùng năm → ngày tháng + giờ
        if (postYear == nowYear && postMonth == nowMonth) {
            return new SimpleDateFormat("d 'tháng' M 'lúc' HH:mm", new Locale("vi")).format(date);
        }

        // Cùng năm, khác tháng → chỉ ngày tháng
        if (postYear == nowYear) {
            return new SimpleDateFormat("d 'tháng' M", new Locale("vi")).format(date);
        }

        // Khác năm → ngày tháng năm + giờ
        return new SimpleDateFormat("d 'tháng' M, yyyy 'lúc' HH:mm", new Locale("vi")).format(date);
    }

    private void loadCommentCount(@NonNull PostViewHolder holder, Post post) {
        if (post.getId() == null || post.getId().isEmpty()) return;
        FirebaseFirestore.getInstance().collection("posts").document(post.getId())
            .collection("comments").get()
            .addOnSuccessListener(snap -> {
                int count = snap.size();
                holder.currentCommentCount = count;
                if (holder.tvCommentCount != null) {
                    if (count > 0) {
                        holder.tvCommentCount.setText(String.valueOf(count));
                        holder.tvCommentCount.setVisibility(View.VISIBLE);
                    } else {
                        holder.tvCommentCount.setVisibility(View.GONE);
                    }
                }
            });
    }

    private void loadLikeState(@NonNull PostViewHolder holder, Post post) {
        if (post.getId() == null || post.getId().isEmpty()) return;

        // Dùng cache nếu có — tránh Firestore callback ghi đè optimistic update
        int[] cached = likeStateCache.get(post.getId());
        if (cached != null) {
            updateLikeUI(holder, cached[1] == 1, cached[0]);
            return;
        }

        FirebaseFirestore.getInstance().collection("posts").document(post.getId()).get()
            .addOnSuccessListener(doc -> {
                if (!doc.exists()) return;
                @SuppressWarnings("unchecked")
                List<String> likes = (List<String>) doc.get("likes");
                if (likes == null) likes = new ArrayList<>();

                String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
                boolean isLiked = likes.contains(currentUserId);
                int count = likes.size();

                // Lưu cache
                likeStateCache.put(post.getId(), new int[]{count, isLiked ? 1 : 0});
                updateLikeUI(holder, isLiked, count);
            });
    }

    private void toggleLike(@NonNull PostViewHolder holder, Post post) {
        if (post.getId() == null) return;
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
            ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (currentUserId == null) return;

        // Optimistic update: cập nhật UI ngay lập tức, không chờ Firestore
        boolean wasLiked = holder.isLiked;
        int newCount = wasLiked ? holder.currentLikeCount - 1 : holder.currentLikeCount + 1;
        updateLikeUI(holder, !wasLiked, newCount);
        // Sound effect
        playSound(soundLike);
        // Update cache ngay để loadLikeState không ghi đè
        if (post.getId() != null) {
            likeStateCache.put(post.getId(), new int[]{newCount, !wasLiked ? 1 : 0});
        }

        // Sync Firestore ở background
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("posts").document(post.getId()).get()
            .addOnSuccessListener(doc -> {
                if (!doc.exists()) return;
                @SuppressWarnings("unchecked")
                List<String> likes = (List<String>) doc.get("likes");
                if (likes == null) likes = new ArrayList<>();

                boolean serverLiked = likes.contains(currentUserId);
                if (serverLiked) likes.remove(currentUserId);
                else likes.add(currentUserId);

                final List<String> finalLikes = likes;
                db.collection("posts").document(post.getId())
                    .update("likes", finalLikes)
                    .addOnSuccessListener(unused -> {
                        // Update UI with actual server count
                        int serverCount = finalLikes.size();
                        boolean isLiked = finalLikes.contains(currentUserId);
                        likeStateCache.put(post.getId(), new int[]{serverCount, isLiked ? 1 : 0});
                        updateLikeUI(holder, isLiked, serverCount);
                    })
                    .addOnFailureListener(e -> {
                        // Rollback UI và cache nếu Firestore lỗi
                        likeStateCache.put(post.getId(), new int[]{holder.currentLikeCount, wasLiked ? 1 : 0});
                        updateLikeUI(holder, wasLiked, holder.currentLikeCount);
                    });
            })
            .addOnFailureListener(e -> {
                // Rollback UI và cache nếu không đọc được Firestore
                likeStateCache.put(post.getId(), new int[]{holder.currentLikeCount, wasLiked ? 1 : 0});
                updateLikeUI(holder, wasLiked, holder.currentLikeCount);
            });
    }

    private void updateLikeUI(@NonNull PostViewHolder holder, boolean isLiked, int count) {
        holder.isLiked = isLiked;
        holder.currentLikeCount = count;
        if (isLiked) {
            holder.ivLikeIcon.setImageResource(R.drawable.ic_like_feather_filled);
            if (holder.tvLikeCount != null) holder.tvLikeCount.setTextColor(ContextCompat.getColor(context, R.color.info));
        } else {
            holder.ivLikeIcon.setImageResource(R.drawable.ic_like_feather);
            if (holder.tvLikeCount != null) holder.tvLikeCount.setTextColor(ContextCompat.getColor(context, R.color.outline));
        }
        if (holder.tvLikeCount != null) {
            if (count > 0) {
                holder.tvLikeCount.setText(String.valueOf(count));
                holder.tvLikeCount.setVisibility(View.VISIBLE);
            } else {
                holder.tvLikeCount.setVisibility(View.GONE);
            }
        }
    }

    private void openDetail(Post post) {
        Intent intent = new Intent(context, hcmute.edu.vn.findora.PostDetailActivity.class);
        intent.putExtra("postId", post.getId());
        intent.putExtra("title", post.getTitle());
        intent.putExtra("description", post.getDescription());
        intent.putExtra("type", post.getType());
        intent.putExtra("userId", post.getUserId());
        if (post.getCreatedAt() != null) intent.putExtra("timestamp", post.getCreatedAt().getSeconds() * 1000L);
        if (post.getImageUrl() != null) intent.putExtra("imageUrl", post.getImageUrl());
        if (post.getLat() != null && post.getLng() != null) {
            intent.putExtra("lat", post.getLat());
            intent.putExtra("lng", post.getLng());
            if (post.getAddress() != null) intent.putExtra("address", post.getAddress());
        }
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() { return postList.size(); }

    private void saveImageToGallery(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return;
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        new Thread(() -> {
            try {
                android.graphics.Bitmap bitmap = com.bumptech.glide.Glide.with(context)
                    .asBitmap()
                    .load(imageUrl)
                    .submit()
                    .get();

                String fileName = "Findora_" + System.currentTimeMillis() + ".jpg";
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    // Android 10+ dùng MediaStore
                    android.content.ContentValues values = new android.content.ContentValues();
                    values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName);
                    values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_PICTURES + "/Findora");
                    android.net.Uri uri = context.getContentResolver()
                        .insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    if (uri != null) {
                        java.io.OutputStream out = context.getContentResolver().openOutputStream(uri);
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out);
                        if (out != null) out.close();
                        mainHandler.post(() -> android.widget.Toast.makeText(context, "Đã lưu ảnh vào thư viện", android.widget.Toast.LENGTH_SHORT).show());
                    }
                } else {
                    // Android 9 trở xuống
                    String dir = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_PICTURES).toString() + "/Findora";
                    java.io.File folder = new java.io.File(dir);
                    if (!folder.exists()) folder.mkdirs();
                    java.io.File file = new java.io.File(folder, fileName);
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, fos);
                    fos.close();
                    // Notify gallery
                    context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        android.net.Uri.fromFile(file)));
                    mainHandler.post(() -> android.widget.Toast.makeText(context, "Đã lưu ảnh vào thư viện", android.widget.Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                mainHandler.post(() -> android.widget.Toast.makeText(context, "Lưu ảnh thất bại", android.widget.Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvTitle, tvDescription, tvCreatedAt, tvLikeCount, tvCommentCount;
        TextView tvPosterName, tvLocation;
        ImageView ivPostImage, ivPosterAvatar, ivLikeIcon;
        android.widget.FrameLayout flImageContainer;
        LinearLayout btnLikeAction, btnCommentAction, layoutHeader, layoutTitleDesc;
        // State for full image viewer
        boolean isLiked = false;
        int currentLikeCount = 0;
        int currentCommentCount = 0;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType           = itemView.findViewById(R.id.tvType);
            tvTitle          = itemView.findViewById(R.id.tvTitle);
            tvDescription    = itemView.findViewById(R.id.tvDescription);
            tvCreatedAt      = itemView.findViewById(R.id.tvCreatedAt);
            tvPosterName     = itemView.findViewById(R.id.tvPosterName);
            tvLocation       = itemView.findViewById(R.id.tvLocation);
            ivPostImage      = itemView.findViewById(R.id.ivPostImage);
            flImageContainer = itemView.findViewById(R.id.flImageContainer);
            ivPosterAvatar   = itemView.findViewById(R.id.ivPosterAvatar);
            ivLikeIcon       = itemView.findViewById(R.id.ivLikeIcon);
            tvLikeCount      = itemView.findViewById(R.id.tvLikeCount);
            tvCommentCount   = itemView.findViewById(R.id.tvCommentCount);
            btnLikeAction    = itemView.findViewById(R.id.btnLikeAction);
            btnCommentAction = itemView.findViewById(R.id.btnCommentAction);
            layoutHeader     = itemView.findViewById(R.id.layoutHeader);
            layoutTitleDesc  = itemView.findViewById(R.id.layoutTitleDesc);
        }
    }
}
