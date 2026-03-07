package hcmute.edu.vn.teeticktick.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import hcmute.edu.vn.teeticktick.R;
import hcmute.edu.vn.teeticktick.utils.IconHelper;

public class CategoryIconPickerAdapter extends RecyclerView.Adapter<CategoryIconPickerAdapter.ViewHolder> {

    private List<String> iconKeys;
    private OnIconSelectedListener listener;

    public interface OnIconSelectedListener {
        void onIconSelected(String iconKey);
    }

    public CategoryIconPickerAdapter(List<String> iconKeys, OnIconSelectedListener listener) {
        this.iconKeys = iconKeys;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_icon, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String iconKey = iconKeys.get(position);
        holder.iconImage.setImageResource(IconHelper.getIconDrawable(iconKey));
        holder.iconImage.setColorFilter(IconHelper.getIconColor(iconKey));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onIconSelected(iconKey);
            }
        });
    }

    @Override
    public int getItemCount() {
        return iconKeys.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iconImage;

        ViewHolder(View itemView) {
            super(itemView);
            iconImage = itemView.findViewById(R.id.category_icon_image);
        }
    }
}
