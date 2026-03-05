package hcmute.edu.vn.teeticktick.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import hcmute.edu.vn.teeticktick.R;
import hcmute.edu.vn.teeticktick.model.TaskList;

public class ListPickerAdapter extends RecyclerView.Adapter<ListPickerAdapter.ViewHolder> {

    private List<TaskList> lists;
    private String selectedList;
    private OnListSelectedListener listener;

    public interface OnListSelectedListener {
        void onListSelected(TaskList list);
    }

    public ListPickerAdapter(List<TaskList> lists, String selectedList, OnListSelectedListener listener) {
        this.lists = lists;
        this.selectedList = selectedList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_list_option, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TaskList list = lists.get(position);
        holder.emoji.setText(list.getEmoji());
        holder.name.setText(list.getDisplayName());
        
        // Compare using key instead of display name
        boolean isSelected = list.getKey().equals(selectedList);
        holder.checkIcon.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onListSelected(list);
            }
        });
    }

    @Override
    public int getItemCount() {
        return lists.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView emoji;
        TextView name;
        ImageView checkIcon;

        ViewHolder(View itemView) {
            super(itemView);
            emoji = itemView.findViewById(R.id.list_emoji);
            name = itemView.findViewById(R.id.list_name);
            checkIcon = itemView.findViewById(R.id.check_icon);
        }
    }
}
