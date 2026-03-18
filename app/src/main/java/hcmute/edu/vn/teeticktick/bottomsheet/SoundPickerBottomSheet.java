package hcmute.edu.vn.teeticktick.bottomsheet;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

import hcmute.edu.vn.teeticktick.R;
import hcmute.edu.vn.teeticktick.adapter.SoundPickerAdapter;
import hcmute.edu.vn.teeticktick.utils.NotificationSoundHelper;

public class SoundPickerBottomSheet extends BottomSheetDialogFragment {

    private RecyclerView soundRecyclerView;
    private SoundPickerAdapter adapter;
    private OnSoundSelectedListener listener;
    private String currentSoundId;

    public interface OnSoundSelectedListener {
        void onSoundSelected(String soundId);
    }

    public void setOnSoundSelectedListener(OnSoundSelectedListener listener) {
        this.listener = listener;
    }

    public void setCurrentSound(String soundId) {
        this.currentSoundId = soundId;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_sound_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView closeButton = view.findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> dismiss());

        soundRecyclerView = view.findViewById(R.id.sound_recycler_view);
        soundRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        List<NotificationSoundHelper.SoundOption> sounds = 
            NotificationSoundHelper.getAvailableSounds(requireContext());

        adapter = new SoundPickerAdapter(sounds, currentSoundId, soundOption -> {
            // Play preview
            playSound(soundOption.getUri());
            
            // Notify listener
            if (listener != null) {
                listener.onSoundSelected(soundOption.getId());
            }
            
            // Dismiss after selection
            dismiss();
        });

        soundRecyclerView.setAdapter(adapter);
    }

    private void playSound(Uri soundUri) {
        try {
            Ringtone ringtone = RingtoneManager.getRingtone(requireContext(), soundUri);
            if (ringtone != null) {
                ringtone.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }
}
