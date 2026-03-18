package hcmute.edu.vn.teeticktick.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import hcmute.edu.vn.teeticktick.R;
import hcmute.edu.vn.teeticktick.utils.NotificationSoundHelper;

public class SoundPickerAdapter extends RecyclerView.Adapter<SoundPickerAdapter.SoundViewHolder> {

    private List<NotificationSoundHelper.SoundOption> sounds;
    private String selectedSoundId;
    private OnSoundClickListener listener;

    public interface OnSoundClickListener {
        void onSoundClick(NotificationSoundHelper.SoundOption sound);
    }

    public SoundPickerAdapter(List<NotificationSoundHelper.SoundOption> sounds, 
                             String selectedSoundId, 
                             OnSoundClickListener listener) {
        this.sounds = sounds;
        this.selectedSoundId = selectedSoundId != null ? selectedSoundId : "default";
        this.listener = listener;
    }

    @NonNull
    @Override
    public SoundViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sound_picker, parent, false);
        return new SoundViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SoundViewHolder holder, int position) {
        NotificationSoundHelper.SoundOption sound = sounds.get(position);
        holder.soundName.setText(sound.getName());
        holder.radioButton.setChecked(sound.getId().equals(selectedSoundId));

        holder.itemView.setOnClickListener(v -> {
            String previousSelected = selectedSoundId;
            selectedSoundId = sound.getId();
            
            // Update UI
            notifyItemChanged(sounds.indexOf(getSoundById(previousSelected)));
            notifyItemChanged(position);
            
            if (listener != null) {
                listener.onSoundClick(sound);
            }
        });
    }

    @Override
    public int getItemCount() {
        return sounds.size();
    }

    private NotificationSoundHelper.SoundOption getSoundById(String id) {
        for (NotificationSoundHelper.SoundOption sound : sounds) {
            if (sound.getId().equals(id)) {
                return sound;
            }
        }
        return sounds.get(0); // default
    }

    static class SoundViewHolder extends RecyclerView.ViewHolder {
        TextView soundName;
        RadioButton radioButton;

        SoundViewHolder(@NonNull View itemView) {
            super(itemView);
            soundName = itemView.findViewById(R.id.sound_name);
            radioButton = itemView.findViewById(R.id.radio_button);
        }
    }
}
