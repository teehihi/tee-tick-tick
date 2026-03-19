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

    public class TaskViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        android.widget.ImageView emojiImageView;
        TextView titleTextView;
        androidx.cardview.widget.CardView foregroundCard;
        LinearLayout deleteButton;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkbox_task);
            emojiImageView = itemView.findViewById(R.id.task_emoji);
            titleTextView = itemView.findViewById(R.id.textview_task_title);
            foregroundCard = itemView.findViewById(R.id.task_card_foreground);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }

        public void bind(Task task) {
            String iconField = task.getEmoji();
            if (iconField != null && !iconField.isEmpty()) {
                // Parse "iconName|#RRGGBB" or legacy plain iconName
                String iconName = iconField.contains("|") ? iconField.split("\\|")[0] : iconField;
                String colorHex = iconField.contains("|") ? iconField.split("\\|")[1] : null;
                int resId = itemView.getContext().getResources().getIdentifier(
                        iconName, "drawable", itemView.getContext().getPackageName());
                if (resId != 0) {
                    emojiImageView.setImageResource(resId);
                    int color;
                    if (colorHex != null) {
                        try { color = android.graphics.Color.parseColor(colorHex); }
                        catch (Exception e) { color = hcmute.edu.vn.teeticktick.utils.IconHelper.getIconColor(iconName.startsWith("ic_ios_") ? iconName.substring(7) : iconName); }
                    } else {
                        color = hcmute.edu.vn.teeticktick.utils.IconHelper.getIconColor(iconName.startsWith("ic_ios_") ? iconName.substring(7) : iconName);
                    }
                    emojiImageView.setColorFilter(color);
                    emojiImageView.setVisibility(View.VISIBLE);
                } else {
                    emojiImageView.setVisibility(View.GONE);
                }
            } else {
                emojiImageView.setVisibility(View.GONE);
            }

            titleTextView.setText(task.getTitle());

            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(task.isCompleted());
            updateStrikeThrough(titleTextView, task.isCompleted());

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                task.setCompleted(isChecked);
                updateStrikeThrough(titleTextView, isChecked);
                if (checkedChangeListener != null) {
                    checkedChangeListener.onTaskCheckedChange(task, isChecked);
                }
            });

            // Reset translation (important after swipe cancel / view recycling)
            itemView.setTranslationX(0);
            if (foregroundCard != null) foregroundCard.setTranslationX(0);
            // Clear any lingering swipe-peek click listener
            itemView.setOnClickListener(null);

            titleTextView.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onTaskClick(task);
            });
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
