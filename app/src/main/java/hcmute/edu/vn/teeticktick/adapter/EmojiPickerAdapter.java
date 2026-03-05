package hcmute.edu.vn.teeticktick.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import hcmute.edu.vn.teeticktick.R;

public class EmojiPickerAdapter extends RecyclerView.Adapter<EmojiPickerAdapter.ViewHolder> {

    private List<String> emojis;
    private OnEmojiSelectedListener listener;

    public interface OnEmojiSelectedListener {
        void onEmojiSelected(String emoji);
    }

    public EmojiPickerAdapter(List<String> emojis, OnEmojiSelectedListener listener) {
        this.emojis = emojis;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_emoji, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String emoji = emojis.get(position);
        holder.emojiText.setText(emoji);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEmojiSelected(emoji);
            }
        });
    }

    @Override
    public int getItemCount() {
        return emojis.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView emojiText;

        ViewHolder(View itemView) {
            super(itemView);
            emojiText = itemView.findViewById(R.id.emoji_text);
        }
    }
}
