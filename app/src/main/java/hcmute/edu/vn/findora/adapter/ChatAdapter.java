package hcmute.edu.vn.findora.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.findora.R;
import hcmute.edu.vn.findora.model.ChatMessage;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    public interface OnReplyListener {
        void onReply(ChatMessage message);
    }

    private final Context context;
    private final List<ChatMessage> messages;
    private final String currentUserId;
    private final String otherUserId;
    private OnReplyListener replyListener;
    
    // Track which message has its time visible (only 1 at a time, -1 = none)
    private int expandedPosition = -1;

    public ChatAdapter(Context context, List<ChatMessage> messages, String currentUserId, String otherUserId) {
        this.context = context;
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.otherUserId = otherUserId;
    }

    public void setOnReplyListener(OnReplyListener listener) {
        this.replyListener = listener;
    }

    /**
     * Attach swipe-to-reply ItemTouchHelper to RecyclerView.
     */
    public void attachSwipeToReply(RecyclerView recyclerView) {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, 0) {

            private static final float MAX_SWIPE = 160f;
            private static final float TRIGGER_THRESHOLD = 100f;
            private boolean triggered = false;

            @Override
            public int getSwipeDirs(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                int pos = vh.getAdapterPosition();
                if (pos == RecyclerView.NO_ID) return 0;
                ChatMessage msg = messages.get(pos);
                boolean isSent = currentUserId.equals(msg.getSenderId());
                return isSent ? ItemTouchHelper.LEFT : ItemTouchHelper.RIGHT;
            }

            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                notifyItemChanged(vh.getAdapterPosition());
            }

            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return 2f;
            }

            @Override
            public float getSwipeEscapeVelocity(float defaultValue) {
                return Float.MAX_VALUE;
            }

            @Override
            public void clearView(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                super.clearView(rv, vh);
                triggered = false;
                View bubble = getBubbleView(vh);
                if (bubble != null) {
                    bubble.animate().translationX(0).setDuration(150).start();
                }
            }

            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas c,
                                    @NonNull RecyclerView rv,
                                    @NonNull RecyclerView.ViewHolder vh,
                                    float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                if (actionState != ItemTouchHelper.ACTION_STATE_SWIPE) {
                    super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive);
                    return;
                }

                int pos = vh.getAdapterPosition();
                if (pos == RecyclerView.NO_ID) return;
                ChatMessage msg = messages.get(pos);
                boolean isSent = currentUserId.equals(msg.getSenderId());

                float clampedDx;
                if (isSent) {
                    clampedDx = Math.max(dX, -MAX_SWIPE);
                } else {
                    clampedDx = Math.min(dX, MAX_SWIPE);
                }

                View bubble = getBubbleView(vh);
                if (bubble != null) {
                    bubble.setTranslationX(clampedDx);
                }

                float absDx = Math.abs(clampedDx);
                if (absDx >= TRIGGER_THRESHOLD && !triggered) {
                    triggered = true;
                    // Trigger reply immediately while finger is still swiping
                    if (replyListener != null) {
                        // Post to main thread to avoid calling from draw pass
                        rv.post(() -> replyListener.onReply(msg));
                    }
                    vh.itemView.performHapticFeedback(
                        android.view.HapticFeedbackConstants.LONG_PRESS);
                }
                // Reset trigger if user swipes back below threshold
                if (absDx < TRIGGER_THRESHOLD) {
                    triggered = false;
                }
            }

            private View getBubbleView(RecyclerView.ViewHolder vh) {
                if (vh instanceof ViewHolder) {
                    ViewHolder holder = (ViewHolder) vh;
                    int pos = holder.getAdapterPosition();
                    if (pos == RecyclerView.NO_ID) return null;
                    boolean isSent = currentUserId.equals(messages.get(pos).getSenderId());
                    return isSent ? holder.bubbleSent : holder.bubbleReceived;
                }
                return null;
            }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        boolean isSent = currentUserId.equals(message.getSenderId());
        MessagePosition msgPosition = getMessagePosition(position);
        
        // Show timestamp separator if needed
        bindTimestampSeparator(holder, position);

        if (isSent) {
            holder.layoutSent.setVisibility(View.VISIBLE);
            holder.layoutReceived.setVisibility(View.GONE);

            boolean hasReplySent = message.getReplyToText() != null && !message.getReplyToText().isEmpty();
            setMessageBackground(holder.bubbleSent, hasReplySent ? MessagePosition.SINGLE : msgPosition, true);
            setMessageMargin(holder.layoutSent, msgPosition);
            bindReplyQuote(holder, message, true);

            if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                holder.cvSentImage.setVisibility(View.VISIBLE);
                holder.tvSentMessage.setVisibility(View.GONE);
                com.bumptech.glide.Glide.with(context)
                        .load(message.getImageUrl())
                        .placeholder(R.drawable.ic_image)
                        .error(R.drawable.ic_image)
                        .into(holder.ivSentImage);
            } else {
                holder.cvSentImage.setVisibility(View.GONE);
                holder.tvSentMessage.setVisibility(View.VISIBLE);
                holder.tvSentMessage.setText(message.getText());
            }

            // Time + tick + seen avatar — all in one row
            boolean isExpanded = expandedPosition == position;
            boolean isLastSent = isLastSentMessage(position);
            boolean isLastReadSent = isLastReadSentMessage(position);

            // layoutSentMeta visible if: expanded OR has seen avatar OR is last unread sent
            boolean showMeta = isExpanded || isLastReadSent || (isLastSent && !message.isRead());
            holder.layoutSentMeta.setVisibility(showMeta ? View.VISIBLE : View.GONE);

            if (showMeta && message.getTimestamp() != null) {
                if (isExpanded) {
                    // Show exact time when clicked
                    holder.tvSentTime.setVisibility(View.VISIBLE);
                    holder.tvSentTime.setText(formatTime(message.getTimestamp().toDate()));
                    holder.ivReadIndicator.setVisibility(View.VISIBLE);
                    holder.ivReadIndicator.setImageResource(
                        message.isRead() ? R.drawable.ic_check_double : R.drawable.ic_check);
                } else if (isLastSent && !message.isRead()) {
                    // Show "Đã gửi X trước" + single tick
                    holder.tvSentTime.setVisibility(View.VISIBLE);
                    holder.tvSentTime.setText(formatRelativeTime(message.getTimestamp().toDate()));
                    holder.ivReadIndicator.setVisibility(View.VISIBLE);
                    holder.ivReadIndicator.setImageResource(R.drawable.ic_check);
                } else {
                    // Only seen avatar visible, hide time + tick
                    holder.tvSentTime.setVisibility(View.GONE);
                    holder.ivReadIndicator.setVisibility(View.GONE);
                }
            }

            // Seen avatar in same row
            if (isLastReadSent) {
                holder.ivSeenAvatar.setVisibility(View.VISIBLE);
                loadReceiverAvatar(holder.ivSeenAvatar);
            } else {
                holder.ivSeenAvatar.setVisibility(View.GONE);
            }

            // Click to toggle time — only 1 at a time
            holder.bubbleSent.setOnClickListener(v -> {
                int prev = expandedPosition;
                expandedPosition = (expandedPosition == position) ? -1 : position;
                if (prev != -1 && prev != position) notifyItemChanged(prev);
                notifyItemChanged(position);
            });

        } else {
            holder.layoutSent.setVisibility(View.GONE);
            holder.layoutReceived.setVisibility(View.VISIBLE);

            boolean hasReplyReceived = message.getReplyToText() != null && !message.getReplyToText().isEmpty();
            setMessageBackground(holder.bubbleReceived, hasReplyReceived ? MessagePosition.SINGLE : msgPosition, false);
            setMessageMargin(holder.layoutReceived, msgPosition);

            if (msgPosition == MessagePosition.BOTTOM || msgPosition == MessagePosition.SINGLE) {
                holder.ivReceivedAvatar.setVisibility(View.VISIBLE);
                loadSenderAvatar(holder.ivReceivedAvatar, message.getSenderId());
            } else {
                holder.ivReceivedAvatar.setVisibility(View.INVISIBLE);
            }

            bindReplyQuote(holder, message, false);

            if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                holder.cvReceivedImage.setVisibility(View.VISIBLE);
                holder.tvReceivedMessage.setVisibility(View.GONE);
                com.bumptech.glide.Glide.with(context)
                        .load(message.getImageUrl())
                        .placeholder(R.drawable.ic_image)
                        .error(R.drawable.ic_image)
                        .into(holder.ivReceivedImage);
            } else {
                holder.cvReceivedImage.setVisibility(View.GONE);
                holder.tvReceivedMessage.setVisibility(View.VISIBLE);
                holder.tvReceivedMessage.setText(message.getText());
            }

            // Time visibility based on click state
            boolean isExpandedReceived = expandedPosition == position;
            if (message.getTimestamp() != null) {
                holder.tvReceivedTime.setVisibility(isExpandedReceived ? View.VISIBLE : View.GONE);
                holder.tvReceivedTime.setText(formatTime(message.getTimestamp().toDate()));
            }

            // Click to toggle time — only 1 at a time
            holder.bubbleReceived.setOnClickListener(v -> {
                int prev = expandedPosition;
                expandedPosition = (expandedPosition == position) ? -1 : position;
                if (prev != -1 && prev != position) notifyItemChanged(prev);
                notifyItemChanged(position);
            });
        }
    }

    /**
     * Show timestamp separator if messages are >5 minutes apart
     */
    private void bindTimestampSeparator(ViewHolder holder, int position) {
        if (position == 0) {
            // Always show for first message
            ChatMessage msg = messages.get(0);
            if (msg.getTimestamp() != null) {
                holder.tvTimestampSeparator.setVisibility(View.VISIBLE);
                holder.tvTimestampSeparator.setText(formatSeparatorTimestamp(msg.getTimestamp().toDate()));
            } else {
                holder.tvTimestampSeparator.setVisibility(View.GONE);
            }
            return;
        }

        ChatMessage current = messages.get(position);
        ChatMessage previous = messages.get(position - 1);

        if (current.getTimestamp() == null || previous.getTimestamp() == null) {
            holder.tvTimestampSeparator.setVisibility(View.GONE);
            return;
        }

        long diff = current.getTimestamp().toDate().getTime() - previous.getTimestamp().toDate().getTime();
        long thirtyMinutes = 30 * 60 * 1000;

        if (diff > thirtyMinutes) {
            holder.tvTimestampSeparator.setVisibility(View.VISIBLE);
            holder.tvTimestampSeparator.setText(formatSeparatorTimestamp(current.getTimestamp().toDate()));
        } else {
            holder.tvTimestampSeparator.setVisibility(View.GONE);
        }
    }

    /**
     * Format timestamp separator:
     * - Today: "11:24"
     * - This week: "12:13 T6" (Friday)
     * - Other: "19:56 15 Tháng 4, 2026"
     */
    private String formatSeparatorTimestamp(Date date) {
        Calendar now = Calendar.getInstance();
        Calendar msgCal = Calendar.getInstance();
        msgCal.setTime(date);

        boolean sameDay = now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                          now.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR);

        if (sameDay) {
            // Today: just time
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
        }

        // Check if same week
        int nowWeek = now.get(Calendar.WEEK_OF_YEAR);
        int msgWeek = msgCal.get(Calendar.WEEK_OF_YEAR);
        int nowYear = now.get(Calendar.YEAR);
        int msgYear = msgCal.get(Calendar.YEAR);

        if (nowYear == msgYear && nowWeek == msgWeek) {
            // This week: "12:13 T6"
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
            String dayOfWeek = getDayOfWeekShort(msgCal.get(Calendar.DAY_OF_WEEK));
            return time + " " + dayOfWeek;
        }

        // Different week: "19:56 15 Tháng 4, 2026"
        return new SimpleDateFormat("HH:mm d 'Tháng' M, yyyy", new Locale("vi")).format(date);
    }

    private String getDayOfWeekShort(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.MONDAY:    return "T2";
            case Calendar.TUESDAY:   return "T3";
            case Calendar.WEDNESDAY: return "T4";
            case Calendar.THURSDAY:  return "T5";
            case Calendar.FRIDAY:    return "T6";
            case Calendar.SATURDAY:  return "T7";
            case Calendar.SUNDAY:    return "CN";
            default: return "";
        }
    }

    private void bindReplyQuote(ViewHolder holder, ChatMessage message, boolean isSent) {
        boolean hasReply = message.getReplyToText() != null && !message.getReplyToText().isEmpty();
        int overlapMargin = dpToPx(-16);
        int normalMargin = 0;

        if (isSent) {
            if (hasReply) {
                // Label: "Bạn đã trả lời [Tên]"
                holder.layoutReplyLabelSent.setVisibility(View.VISIBLE);
                String sender = message.getReplyToSender();
                boolean replyToSelf = sender != null && sender.equals("Bạn");
                holder.tvReplySenderSent.setText(replyToSelf
                    ? "Bạn đã trả lời chính mình"
                    : "Bạn đã trả lời " + sender);

                // Quote box: only content
                holder.layoutReplyQuoteSent.setVisibility(View.VISIBLE);
                holder.tvReplyTextSent.setText(message.getReplyToText());
                setTopMargin(holder.bubbleSent, overlapMargin);
            } else {
                holder.layoutReplyLabelSent.setVisibility(View.GONE);
                holder.layoutReplyQuoteSent.setVisibility(View.GONE);
                setTopMargin(holder.bubbleSent, normalMargin);
            }
        } else {
            if (hasReply) {
                // Label: "[Tên] đã trả lời bạn"
                holder.layoutReplyLabelReceived.setVisibility(View.VISIBLE);
                String sender = message.getReplyToSender();
                holder.tvReplySenderReceived.setText(
                    (sender != null ? sender : "Ai đó") + " đã trả lời bạn");

                // Quote box: only content
                holder.layoutReplyQuoteReceived.setVisibility(View.VISIBLE);
                holder.tvReplyTextReceived.setText(message.getReplyToText());
                setTopMargin(holder.bubbleReceived, overlapMargin);
            } else {
                holder.layoutReplyLabelReceived.setVisibility(View.GONE);
                holder.layoutReplyQuoteReceived.setVisibility(View.GONE);
                setTopMargin(holder.bubbleReceived, normalMargin);
            }
        }
    }

    private void setTopMargin(View view, int margin) {
        android.view.ViewGroup.MarginLayoutParams p =
            (android.view.ViewGroup.MarginLayoutParams) view.getLayoutParams();
        p.topMargin = margin;
        view.setLayoutParams(p);
    }

    // ─── Position helpers ───────────────────────────────────────────────────

    private enum MessagePosition { SINGLE, TOP, MIDDLE, BOTTOM }

    private MessagePosition getMessagePosition(int position) {
        if (messages.isEmpty()) return MessagePosition.SINGLE;
        ChatMessage current = messages.get(position);
        boolean hasPrev = position > 0;
        boolean hasNext = position < messages.size() - 1;
        
        boolean samePrev = false;
        boolean sameNext = false;
        
        if (hasPrev) {
            ChatMessage prev = messages.get(position - 1);
            samePrev = current.getSenderId().equals(prev.getSenderId())
                    && isWithinTimeGroup(prev, current);
        }
        if (hasNext) {
            ChatMessage next = messages.get(position + 1);
            sameNext = current.getSenderId().equals(next.getSenderId())
                    && isWithinTimeGroup(current, next);
        }

        if (!samePrev && !sameNext) return MessagePosition.SINGLE;
        if (!samePrev)              return MessagePosition.TOP;
        if (!sameNext)              return MessagePosition.BOTTOM;
        return MessagePosition.MIDDLE;
    }

    /** Two messages are in the same visual group if sent within 10 minutes of each other */
    private boolean isWithinTimeGroup(ChatMessage earlier, ChatMessage later) {
        if (earlier.getTimestamp() == null || later.getTimestamp() == null) return true;
        long diff = later.getTimestamp().toDate().getTime()
                  - earlier.getTimestamp().toDate().getTime();
        return diff < 10 * 60 * 1000; // 10 minutes
    }

    private void setMessageBackground(View bubble, MessagePosition position, boolean isSent) {
        int res;
        if (isSent) {
            switch (position) {
                case TOP:    res = R.drawable.bg_message_sent_top;    break;
                case MIDDLE: res = R.drawable.bg_message_sent_middle; break;
                case BOTTOM: res = R.drawable.bg_message_sent_bottom; break;
                default:     res = R.drawable.bg_message_sent_single;
            }
        } else {
            switch (position) {
                case TOP:    res = R.drawable.bg_message_received_top;    break;
                case MIDDLE: res = R.drawable.bg_message_received_middle; break;
                case BOTTOM: res = R.drawable.bg_message_received_bottom; break;
                default:     res = R.drawable.bg_message_received_single;
            }
        }
        bubble.setBackgroundResource(res);
    }

    private void setMessageMargin(View layout, MessagePosition position) {
        android.view.ViewGroup.MarginLayoutParams p =
                (android.view.ViewGroup.MarginLayoutParams) layout.getLayoutParams();
        int small = dpToPx(2);
        int large = dpToPx(12);
        switch (position) {
            case SINGLE: p.topMargin = large; p.bottomMargin = 0;     break;
            case TOP:    p.topMargin = large; p.bottomMargin = small;  break;
            case MIDDLE: p.topMargin = small; p.bottomMargin = small;  break;
            case BOTTOM: p.topMargin = small; p.bottomMargin = 0;      break;
        }
        layout.setLayoutParams(p);
    }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    // ─── Avatar loaders ─────────────────────────────────────────────────────

    private void loadSenderAvatar(android.widget.ImageView iv, String senderId) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(senderId).get()
                .addOnSuccessListener(doc -> {
                    String url = doc.exists() ? doc.getString("photoUrl") : null;
                    if (url != null && !url.isEmpty()) {
                        com.bumptech.glide.Glide.with(context).load(url)
                                .transform(new com.bumptech.glide.load.resource.bitmap.CircleCrop())
                                .placeholder(R.drawable.ic_person).error(R.drawable.ic_person)
                                .into(iv);
                    } else {
                        iv.setImageResource(R.drawable.ic_person);
                    }
                })
                .addOnFailureListener(e -> iv.setImageResource(R.drawable.ic_person));
    }

    private void loadReceiverAvatar(android.widget.ImageView iv) {
        if (otherUserId == null || otherUserId.isEmpty()) {
            iv.setImageResource(R.drawable.ic_person);
            return;
        }
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(otherUserId).get()
                .addOnSuccessListener(doc -> {
                    String url = doc.exists() ? doc.getString("photoUrl") : null;
                    if (url != null && !url.isEmpty()) {
                        com.bumptech.glide.Glide.with(context).load(url)
                                .transform(new com.bumptech.glide.load.resource.bitmap.CircleCrop())
                                .placeholder(R.drawable.ic_person).error(R.drawable.ic_person)
                                .into(iv);
                    } else {
                        iv.setImageResource(R.drawable.ic_person);
                    }
                })
                .addOnFailureListener(e -> iv.setImageResource(R.drawable.ic_person));
    }

    private String formatTime(Date date) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
    }
    
    /**
     * Check if this is the last sent message from current user
     */
    private boolean isLastSentMessage(int position) {
        ChatMessage current = messages.get(position);
        if (!currentUserId.equals(current.getSenderId())) {
            return false;
        }
        
        // Check if there's any sent message after this one
        for (int i = position + 1; i < messages.size(); i++) {
            if (currentUserId.equals(messages.get(i).getSenderId())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if this is the last READ sent message — the one where seen avatar should appear
     */
    private boolean isLastReadSentMessage(int position) {
        ChatMessage current = messages.get(position);
        if (!currentUserId.equals(current.getSenderId())) return false;
        if (!current.isRead()) return false;
        for (int i = position + 1; i < messages.size(); i++) {
            ChatMessage m = messages.get(i);
            if (currentUserId.equals(m.getSenderId()) && m.isRead()) return false;
        }
        return true;
    }
    
    /**
     * Format relative time: "Đã gửi", "Đã gửi 2 phút trước", "Đã gửi 1 giờ trước", etc.
     */
    private String formatRelativeTime(Date date) {
        long now = System.currentTimeMillis();
        long diff = now - date.getTime();
        
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        
        if (minutes < 1) {
            return "Đã gửi";
        } else if (minutes < 60) {
            return "Đã gửi " + minutes + " phút trước";
        } else if (hours < 24) {
            return "Đã gửi " + hours + " giờ trước";
        } else if (days < 7) {
            return "Đã gửi " + days + " ngày trước";
        } else {
            return "Đã gửi " + weeks + " tuần trước";
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    // ─── ViewHolder ─────────────────────────────────────────────────────────

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTimestampSeparator;
        LinearLayout layoutSent, layoutReceived, layoutSentMeta;
        LinearLayout layoutReplyQuoteSent, layoutReplyQuoteReceived;
        LinearLayout layoutReplyLabelSent, layoutReplyLabelReceived;
        View bubbleSent, bubbleReceived;
        TextView tvSentMessage, tvSentTime, tvReceivedMessage, tvReceivedTime;
        TextView tvReplySenderSent, tvReplyTextSent, tvReplySenderReceived, tvReplyTextReceived;
        android.widget.ImageView ivReceivedAvatar, ivReadIndicator, ivSentImage, ivReceivedImage, ivSeenAvatar;
        androidx.cardview.widget.CardView cvSentImage, cvReceivedImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTimestampSeparator    = itemView.findViewById(R.id.tvTimestampSeparator);
            layoutSent              = itemView.findViewById(R.id.layoutSent);
            layoutReceived          = itemView.findViewById(R.id.layoutReceived);
            layoutSentMeta          = itemView.findViewById(R.id.layoutSentMeta);
            layoutReplyQuoteSent    = itemView.findViewById(R.id.layoutReplyQuoteSent);
            layoutReplyQuoteReceived= itemView.findViewById(R.id.layoutReplyQuoteReceived);
            layoutReplyLabelSent    = itemView.findViewById(R.id.layoutReplyLabelSent);
            layoutReplyLabelReceived= itemView.findViewById(R.id.layoutReplyLabelReceived);
            bubbleSent              = itemView.findViewById(R.id.bubbleSent);
            bubbleReceived          = itemView.findViewById(R.id.bubbleReceived);
            tvSentMessage           = itemView.findViewById(R.id.tvSentMessage);
            tvSentTime              = itemView.findViewById(R.id.tvSentTime);
            tvReceivedMessage       = itemView.findViewById(R.id.tvReceivedMessage);
            tvReceivedTime          = itemView.findViewById(R.id.tvReceivedTime);
            tvReplySenderSent       = itemView.findViewById(R.id.tvReplySenderSent);
            tvReplyTextSent         = itemView.findViewById(R.id.tvReplyTextSent);
            tvReplySenderReceived   = itemView.findViewById(R.id.tvReplySenderReceived);
            tvReplyTextReceived     = itemView.findViewById(R.id.tvReplyTextReceived);
            ivReceivedAvatar        = itemView.findViewById(R.id.ivReceivedAvatar);
            ivReadIndicator         = itemView.findViewById(R.id.ivReadIndicator);
            ivSentImage             = itemView.findViewById(R.id.ivSentImage);
            ivReceivedImage         = itemView.findViewById(R.id.ivReceivedImage);
            ivSeenAvatar            = itemView.findViewById(R.id.ivSeenAvatar);
            cvSentImage             = itemView.findViewById(R.id.cvSentImage);
            cvReceivedImage         = itemView.findViewById(R.id.cvReceivedImage);
        }
    }
}
