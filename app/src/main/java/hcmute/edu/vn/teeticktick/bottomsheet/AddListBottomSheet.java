package hcmute.edu.vn.teeticktick.bottomsheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import hcmute.edu.vn.teeticktick.R;
import hcmute.edu.vn.teeticktick.utils.IconHelper;
import hcmute.edu.vn.teeticktick.utils.NotificationSoundHelper;

public class AddListBottomSheet extends BottomSheetDialogFragment {

    private EditText listNameInput;
    private ImageView emojiSelector; // Changed from TextView
    private TextView soundSelector;
    private Button submitButton;     // Changed from addListButton
    private ImageView cancelButton;  // Changed from closeButton (Must be ImageView since XML has it as ImageView)
    
    private String selectedIconKey = "inbox"; // Changed from selectedEmoji = "📁"
    private String selectedSoundId = "default";
    private OnListAddedListener listener;

    public interface OnListAddedListener {
        void onListAdded(String listName, String iconKey, String soundId);
    }

    public void setOnListAddedListener(OnListAddedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupClickListeners();
        
        listNameInput.requestFocus();
        emojiSelector.setImageResource(IconHelper.getIconDrawable(selectedIconKey));
        emojiSelector.setColorFilter(IconHelper.getIconColor(selectedIconKey));
        updateSoundDisplay();
    }

    private void initViews(View view) {
        listNameInput = view.findViewById(R.id.list_name_input);
        emojiSelector = view.findViewById(R.id.emoji_selector);
        soundSelector = view.findViewById(R.id.sound_selector);
        cancelButton = view.findViewById(R.id.close_button); // Renamed closeButton to cancelButton
        submitButton = view.findViewById(R.id.add_list_button); // Renamed addListButton to submitButton
    }

    private void setupClickListeners() {
        cancelButton.setOnClickListener(v -> dismiss()); // Changed closeButton to cancelButton
        
        // Emoji selector - open emoji picker
        emojiSelector.setOnClickListener(v -> {
            CategoryIconPickerBottomSheet iconPicker = new CategoryIconPickerBottomSheet();
            iconPicker.setOnIconSelectedListener((iconRes, color) -> {
                selectedIconKey = getResources().getResourceEntryName(iconRes);
                emojiSelector.setImageResource(iconRes);
                emojiSelector.setColorFilter(color);
            });
            iconPicker.show(getParentFragmentManager(), "CategoryIconPicker");
        });
        
        // Sound selector - open sound picker
        soundSelector.setOnClickListener(v -> {
            SoundPickerBottomSheet soundPicker = new SoundPickerBottomSheet();
            soundPicker.setCurrentSound(selectedSoundId);
            soundPicker.setOnSoundSelectedListener(soundId -> {
                selectedSoundId = soundId;
                updateSoundDisplay();
            });
            soundPicker.show(getParentFragmentManager(), "SoundPicker");
        });
        
        // Add list button
        submitButton.setOnClickListener(v -> { // Changed addListButton to submitButton
            String listName = listNameInput.getText().toString().trim(); // Changed 'name' to 'listName'
            if (listName.isEmpty()) { // Changed condition and error handling
                listNameInput.setError(getString(R.string.error_empty_list_name));
                return;
            }

            if (listener != null) {
                listener.onListAdded(listName, selectedIconKey, selectedSoundId);
            }
            dismiss();
        });
    }

    private void updateSoundDisplay() {
        String soundName = NotificationSoundHelper.getSoundName(requireContext(), selectedSoundId);
        soundSelector.setText(soundName);
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }
}
