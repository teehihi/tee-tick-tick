package hcmute.edu.vn.teeticktick.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import hcmute.edu.vn.teeticktick.R;
import hcmute.edu.vn.teeticktick.model.SmartListItem;

public class SmartListSettingsAdapter extends RecyclerView.Adapter<SmartListSettingsAdapter.ViewHolder> {

    private List<SmartListItem> items;
    private OnToggleListener listener;

    public interface OnToggleListener {
        void onToggle(SmartListItem item, boolean isChecked);
    }

    public SmartListSettingsAdapter(List<SmartListItem> items, OnToggleListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_smart_list_setting, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SmartListItem item = items.get(position);
        
        holder.icon.setImageResource(item.getIconResId());
        holder.icon.setImageTintList(ColorStateList.valueOf(item.getIconColor()));
        holder.title.setText(item.getTitle());
        holder.subtitle.setText(item.getSubtitle());
        holder.toggle.setChecked(item.isVisible());

        // Update UI based on toggle state
        updateItemAppearance(holder, item.isVisible());

        holder.toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateItemAppearance(holder, isChecked);
            if (listener != null) {
                listener.onToggle(item, isChecked);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            holder.toggle.setChecked(!holder.toggle.isChecked());
        });
    }

    private void updateItemAppearance(ViewHolder holder, boolean isEnabled) {
        float alpha = isEnabled ? 1.0f : 0.4f;
        holder.icon.setAlpha(alpha);
        holder.title.setAlpha(alpha);
        holder.subtitle.setAlpha(alpha);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title;
        TextView subtitle;
        Switch toggle;

        ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.item_icon);
            title = itemView.findViewById(R.id.item_title);
            subtitle = itemView.findViewById(R.id.item_subtitle);
            toggle = itemView.findViewById(R.id.item_toggle);
        }
    }
}
