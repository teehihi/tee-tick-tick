package hcmute.edu.vn.teeticktick.bottomsheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Arrays;
import java.util.List;

import hcmute.edu.vn.teeticktick.R;
import hcmute.edu.vn.teeticktick.adapter.EmojiPickerAdapter;

public class EmojiPickerBottomSheet extends BottomSheetDialogFragment {

    private OnEmojiSelectedListener listener;

    public interface OnEmojiSelectedListener {
        void onEmojiSelected(String emoji);
    }

    public void setOnEmojiSelectedListener(OnEmojiSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_emoji_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.emoji_recyclerview);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 6));

        List<String> emojis = Arrays.asList(
            "✅", "📝", "💼", "🏠", "📚", "🛒",
            "🎯", "⭐", "❤️", "🔥", "💡", "🎨",
            "🎵", "🎮", "⚽", "🏃", "🍕", "☕",
            "✈️", "🚗", "📱", "💻", "📧", "📞",
            "💰", "🎁", "🎉", "🎂", "🌟", "🌈",
            "🌸", "🌺", "🍀", "🌙", "☀️", "⛅",
            "🔔", "⏰", "📅", "📌", "🔖", "📎",
            "✏️", "📐", "📊", "📈", "🔍", "🔑"
        );

        EmojiPickerAdapter adapter = new EmojiPickerAdapter(emojis, emoji -> {
            if (listener != null) {
                listener.onEmojiSelected(emoji);
            }
            dismiss();
        });

        recyclerView.setAdapter(adapter);
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }
}
