package hcmute.edu.vn.teeticktick;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TaskGroupAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_GROUP_HEADER = 0;
    private static final int TYPE_TASK = 1;

    private List<Object> items;
    private OnTaskDeleteListener deleteListener;
    private OnTaskCheckedChangeListener checkedChangeListener;
    private OnTaskClickListener clickListener;
    
    // Interface để callback khi xóa task
    public interface OnTaskDeleteListener {
        void onTaskDelete(Task task, int position);
    }
    
    // Interface để callback khi thay đổi trạng thái checkbox
    public interface OnTaskCheckedChangeListener {
        void onTaskCheckedChange(Task task, boolean isChecked);
    }
    
    // Interface để callback khi click vào task
    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public TaskGroupAdapter(List<Object> items) {
        this.items = items;
    }
    
    public void setOnTaskDeleteListener(OnTaskDeleteListener listener) {
        this.deleteListener = listener;
    }
    
    public void setOnTaskCheckedChangeListener(OnTaskCheckedChangeListener listener) {
        this.checkedChangeListener = listener;
    }
    
    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.clickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof TaskGroup) {
            return TYPE_GROUP_HEADER;
        } else {
            return TYPE_TASK;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_GROUP_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_task_group_header, parent, false);
            return new GroupHeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_task, parent, false);
            return new TaskViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof GroupHeaderViewHolder) {
            TaskGroup group = (TaskGroup) items.get(position);
            ((GroupHeaderViewHolder) holder).bind(group, position);
        } else if (holder instanceof TaskViewHolder) {
            Task task = (Task) items.get(position);
            ((TaskViewHolder) holder).bind(task);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class GroupHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView countTextView;
        ImageView expandIcon;

        public GroupHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.group_title);
            countTextView = itemView.findViewById(R.id.group_count);
            expandIcon = itemView.findViewById(R.id.expand_icon);
        }

        public void bind(TaskGroup group, int position) {
            titleTextView.setText(group.getTitle());
            countTextView.setText(String.valueOf(group.getTaskCount()));
            expandIcon.setRotation(group.isExpanded() ? 180 : 0);

            itemView.setOnClickListener(v -> {
                group.setExpanded(!group.isExpanded());
                expandIcon.setRotation(group.isExpanded() ? 180 : 0);
                notifyItemChanged(position);
                updateGroupVisibility(group, position);
            });
        }
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView emojiTextView;
        TextView titleTextView;
        androidx.cardview.widget.CardView foregroundCard;
        LinearLayout deleteButton;
        
        private float dX = 0f;
        private float startX = 0f;
        private boolean isSwiping = false;
        private boolean isOpen = false;
        private static final int SWIPE_DISTANCE_DP = 80;
        private float openPosition;
        private float swipeThreshold;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkbox_task);
            emojiTextView = itemView.findViewById(R.id.task_emoji);
            titleTextView = itemView.findViewById(R.id.textview_task_title);
            foregroundCard = itemView.findViewById(R.id.task_card_foreground);
            deleteButton = itemView.findViewById(R.id.delete_button);
            
            // Convert dp to pixels
            float density = itemView.getContext().getResources().getDisplayMetrics().density;
            openPosition = -SWIPE_DISTANCE_DP * density;
            swipeThreshold = openPosition / 2;
        }

        public void bind(Task task) {
            if (task.getEmoji() != null && !task.getEmoji().isEmpty()) {
                emojiTextView.setVisibility(View.VISIBLE);
                emojiTextView.setText(task.getEmoji());
            } else {
                emojiTextView.setVisibility(View.GONE);
            }
            
            titleTextView.setText(task.getTitle());
            
            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(task.isCompleted());

            updateStrikeThrough(titleTextView, task.isCompleted());

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                task.setCompleted(isChecked);
                updateStrikeThrough(titleTextView, isChecked);
                // Gọi callback để cập nhật database
                if (checkedChangeListener != null) {
                    checkedChangeListener.onTaskCheckedChange(task, isChecked);
                }
            });
            
            // Reset card position
            foregroundCard.setX(0);
            isOpen = false;
            
            // Setup click listener for task title
            titleTextView.setOnClickListener(v -> {
                if (clickListener != null && !isOpen) {
                    clickListener.onTaskClick(task);
                }
            });
            
            // Setup swipe gesture
            setupSwipeGesture();
            
            // Setup delete button
            deleteButton.setOnClickListener(v -> {
                animateCardOut(foregroundCard, () -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        // Gọi callback để xóa khỏi database
                        if (deleteListener != null) {
                            deleteListener.onTaskDelete(task, position);
                        }
                        items.remove(position);
                        notifyItemRemoved(position);
                    }
                });
            });
        }

        private void setupSwipeGesture() {
            foregroundCard.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        dX = v.getX() - event.getRawX();
                        isSwiping = false;
                        return true;

                    case android.view.MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - startX;
                        
                        // Detect swipe
                        if (Math.abs(deltaX) > 10) {
                            isSwiping = true;
                        }
                        
                        if (isSwiping) {
                            float newX = event.getRawX() + dX;
                            
                            // Allow free swiping left (no limit)
                            if (isOpen) {
                                // When open, allow swiping right to close or left to delete
                                if (newX <= 0) {
                                    v.setX(newX);
                                }
                            } else {
                                // When closed, allow swiping left freely
                                if (newX <= 0) {
                                    v.setX(newX);
                                }
                            }
                        }
                        return true;

                    case android.view.MotionEvent.ACTION_UP:
                        if (isSwiping) {
                            float currentX = v.getX();
                            float cardWidth = v.getWidth();
                            float deleteThreshold = -cardWidth / 2; // Half of card width
                            
                            if (currentX < deleteThreshold) {
                                // Swiped more than half - delete the item
                                animateCardOut(v, () -> {
                                    int position = getAdapterPosition();
                                    if (position != RecyclerView.NO_POSITION) {
                                        Task taskToDelete = (Task) items.get(position);
                                        // Gọi callback để xóa khỏi database
                                        if (deleteListener != null) {
                                            deleteListener.onTaskDelete(taskToDelete, position);
                                        }
                                        items.remove(position);
                                        notifyItemRemoved(position);
                                    }
                                });
                            } else if (isOpen) {
                                // If open, check if swiping right to close
                                if (currentX > openPosition + 30) {
                                    closeCard(v);
                                } else {
                                    openCard(v);
                                }
                            } else {
                                // If closed, check if swiping left to open
                                if (currentX < swipeThreshold) {
                                    openCard(v);
                                } else {
                                    closeCard(v);
                                }
                            }
                            
                            isSwiping = false;
                            return true;
                        } else {
                            // If not swiping, treat as click
                            if (isOpen) {
                                closeCard(v);
                                return true;
                            }
                        }
                        return false;
                }
                return false;
            });
        }

        private void openCard(View view) {
            isOpen = true;
            android.animation.ObjectAnimator animator = android.animation.ObjectAnimator.ofFloat(view, "x", view.getX(), openPosition);
            animator.setDuration(250);
            animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
            animator.start();
        }

        private void closeCard(View view) {
            isOpen = false;
            android.animation.ObjectAnimator animator = android.animation.ObjectAnimator.ofFloat(view, "x", view.getX(), 0f);
            animator.setDuration(250);
            animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
            animator.start();
        }

        private void animateCardOut(View view, Runnable onComplete) {
            android.animation.ObjectAnimator animator = android.animation.ObjectAnimator.ofFloat(view, "x", view.getX(), -view.getWidth());
            animator.setDuration(200);
            animator.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            });
            animator.start();
        }

        private void updateStrikeThrough(TextView textView, boolean isCompleted) {
            if (isCompleted) {
                textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                textView.setPaintFlags(textView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }
        }
    }

    private void updateGroupVisibility(TaskGroup group, int groupPosition) {
        int startPosition = groupPosition + 1;
        int taskCount = group.getTasks().size();

        if (group.isExpanded()) {
            for (int i = 0; i < taskCount; i++) {
                items.add(startPosition + i, group.getTasks().get(i));
            }
            notifyItemRangeInserted(startPosition, taskCount);
        } else {
            for (int i = 0; i < taskCount; i++) {
                items.remove(startPosition);
            }
            notifyItemRangeRemoved(startPosition, taskCount);
        }
    }
}
