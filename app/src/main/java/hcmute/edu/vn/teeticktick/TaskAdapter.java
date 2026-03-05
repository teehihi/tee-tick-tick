package hcmute.edu.vn.teeticktick;

import android.animation.ObjectAnimator;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private OnTaskDeleteListener deleteListener;

    public interface OnTaskDeleteListener {
        void onTaskDelete(int position);
    }

    public TaskAdapter(List<Task> taskList) {
        this.taskList = taskList;
    }

    public void setOnTaskDeleteListener(OnTaskDeleteListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.textViewTitle.setText(task.getTitle());
        holder.checkBox.setChecked(task.isCompleted());

        // Show emoji if available
        if (task.getEmoji() != null && !task.getEmoji().isEmpty()) {
            holder.taskEmoji.setVisibility(View.VISIBLE);
            holder.taskEmoji.setText(task.getEmoji());
        } else {
            holder.taskEmoji.setVisibility(View.GONE);
        }

        updateStrikeThrough(holder.textViewTitle, task.isCompleted());

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setCompleted(isChecked);
            updateStrikeThrough(holder.textViewTitle, isChecked);
        });

        // Setup swipe gesture
        holder.setupSwipeGesture(position, deleteListener);
    }

    private void updateStrikeThrough(TextView textView, boolean isCompleted) {
        if (isCompleted) {
            textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            textView.setPaintFlags(textView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView textViewTitle;
        TextView taskEmoji;
        CardView foregroundCard;
        
        private float dX = 0f;
        private float startX = 0f;
        private boolean isSwiping = false;
        private static final float SWIPE_THRESHOLD = -300f;
        private static final float DELETE_THRESHOLD = -200f;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkbox_task);
            textViewTitle = itemView.findViewById(R.id.textview_task_title);
            taskEmoji = itemView.findViewById(R.id.task_emoji);
            foregroundCard = itemView.findViewById(R.id.task_card_foreground);
        }

        public void setupSwipeGesture(int position, OnTaskDeleteListener deleteListener) {
            foregroundCard.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startX = event.getRawX();
                            dX = v.getX() - event.getRawX();
                            isSwiping = false;
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            float newX = event.getRawX() + dX;
                            float deltaX = event.getRawX() - startX;
                            
                            // Only allow left swipe
                            if (deltaX < -20) {
                                isSwiping = true;
                            }
                            
                            if (isSwiping && newX <= 0 && newX >= SWIPE_THRESHOLD) {
                                v.setX(newX);
                            }
                            return true;

                        case MotionEvent.ACTION_UP:
                            if (isSwiping) {
                                float currentX = v.getX();
                                
                                if (currentX < DELETE_THRESHOLD) {
                                    // Delete the item
                                    animateCardOut(v, () -> {
                                        if (deleteListener != null) {
                                            deleteListener.onTaskDelete(position);
                                        }
                                    });
                                } else {
                                    // Snap back
                                    animateCardBack(v);
                                }
                                isSwiping = false;
                                return true;
                            }
                            return false;
                    }
                    return false;
                }
            });
        }

        private void animateCardBack(View view) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(view, "x", view.getX(), 0f);
            animator.setDuration(200);
            animator.start();
        }

        private void animateCardOut(View view, Runnable onComplete) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(view, "x", view.getX(), -view.getWidth());
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
    }
}
