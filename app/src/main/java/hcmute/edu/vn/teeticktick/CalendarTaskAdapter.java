package hcmute.edu.vn.teeticktick;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import hcmute.edu.vn.teeticktick.database.TaskEntity;

public class CalendarTaskAdapter extends RecyclerView.Adapter<CalendarTaskAdapter.ViewHolder> {

    private List<TaskEntity> tasks;
    private OnTaskClickListener clickListener;
    private OnTaskCheckedChangeListener checkedChangeListener;
    
    private static final int[] COLORS = {
        0xFFFFE082, // Yellow
        0xFFCE93D8, // Purple
        0xFF90CAF9, // Blue
        0xFFA5D6A7, // Green
        0xFFFFAB91, // Orange
        0xFFEF9A9A  // Red
    };

    public interface OnTaskClickListener {
        void onTaskClick(TaskEntity task);
    }
    
    public interface OnTaskCheckedChangeListener {
        void onTaskCheckedChange(TaskEntity task, boolean isChecked);
    }

    public CalendarTaskAdapter(List<TaskEntity> tasks) {
        this.tasks = tasks;
    }
    
    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.clickListener = listener;
    }
    
    public void setOnTaskCheckedChangeListener(OnTaskCheckedChangeListener listener) {
        this.checkedChangeListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TaskEntity task = tasks.get(position);
        
        // Set emoji
        if (task.getEmoji() != null && !task.getEmoji().isEmpty()) {
            holder.taskEmoji.setText(task.getEmoji());
            holder.taskEmoji.setVisibility(View.VISIBLE);
        } else {
            holder.taskEmoji.setVisibility(View.GONE);
        }
        
        // Set title
        holder.taskTitle.setText(task.getTitle());
        
        // Set time from dueDate
        if (task.getDueDate() != null) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            holder.taskTime.setText(timeFormat.format(new Date(task.getDueDate())));
        } else {
            holder.taskTime.setText("--:--");
        }
        
        // Set category
        String category = task.getListName();
        if (category != null && !category.isEmpty()) {
            holder.taskCategory.setText(getCategoryDisplayName(category));
            holder.taskCategory.setVisibility(View.VISIBLE);
        } else {
            holder.taskCategory.setVisibility(View.GONE);
        }
        
        // Set color indicator based on task id
        int colorIndex = Math.abs(task.getId()) % COLORS.length;
        holder.timeIndicator.setBackgroundColor(COLORS[colorIndex]);
        
        // Set checkbox
        holder.taskCheckbox.setOnCheckedChangeListener(null); // Clear listener first
        holder.taskCheckbox.setChecked(task.isCompleted());
        holder.taskCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (checkedChangeListener != null) {
                checkedChangeListener.onTaskCheckedChange(task, isChecked);
            }
        });
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onTaskClick(task);
            }
        });
    }
    
    private String getCategoryDisplayName(String category) {
        if (category == null) return "";
        switch (category) {
            case "Work": return "Công việc";
            case "Personal": return "Cá nhân";
            case "Shopping": return "Mua sắm";
            case "Learning": return "Học tập";
            case "Inbox": return "Hộp thư";
            default: return category;
        }
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }
    
    public void updateTasks(List<TaskEntity> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView taskTime;
        TextView taskEmoji;
        TextView taskTitle;
        TextView taskCategory;
        View timeIndicator;
        CheckBox taskCheckbox;

        ViewHolder(View itemView) {
            super(itemView);
            taskTime = itemView.findViewById(R.id.task_time);
            taskEmoji = itemView.findViewById(R.id.task_emoji);
            taskTitle = itemView.findViewById(R.id.task_title);
            taskCategory = itemView.findViewById(R.id.task_category);
            timeIndicator = itemView.findViewById(R.id.time_indicator);
            taskCheckbox = itemView.findViewById(R.id.task_checkbox);
        }
    }
}
