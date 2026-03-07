package hcmute.edu.vn.teeticktick.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import hcmute.edu.vn.teeticktick.R;
import hcmute.edu.vn.teeticktick.model.TaskList;
import hcmute.edu.vn.teeticktick.utils.IconHelper;

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
        // Set icon
        holder.listIcon.setImageResource(IconHelper.getIconDrawable(list.getEmoji()));
        holder.listName.setText(list.getDisplayName());
        
        // Check if selected
        boolean isSelected = list.getKey().equals(selectedList);
        if (isSelected) {
            holder.checkIcon.setVisibility(View.VISIBLE);
            holder.listName.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
            holder.listIcon.setColorFilter(holder.itemView.getContext().getResources().getColor(android.R.color.white));
        } else {
            holder.checkIcon.setVisibility(View.GONE);
            holder.listName.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_secondary));
            holder.listIcon.setColorFilter(IconHelper.getIconColor(list.getEmoji()));
        }
        
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

    public void updateData(List<TaskList> newLists) {
        this.lists = newLists;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView listIcon;
        TextView listName;
        ImageView checkIcon;

        ViewHolder(View itemView) {
            super(itemView);
            listIcon = itemView.findViewById(R.id.list_emoji);
            listName = itemView.findViewById(R.id.list_name);
            checkIcon = itemView.findViewById(R.id.check_icon);
        }
    }
}
