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

public class AddListBottomSheet extends BottomSheetDialogFragment {

    private EditText listNameInput;
    private ImageView emojiSelector; // Changed from TextView
    private Button submitButton;     // Changed from addListButton
    private ImageView cancelButton;  // Changed from closeButton (Must be ImageView since XML has it as ImageView)
    
    private String selectedIconKey = "inbox"; // Changed from selectedEmoji = "📁"
    private OnListAddedListener listener;

    public interface OnListAddedListener {
        void onListAdded(String listName, String iconKey); // Changed 'emoji' to 'iconKey'
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
    }

    private void initViews(View view) {
        listNameInput = view.findViewById(R.id.list_name_input);
        emojiSelector = view.findViewById(R.id.emoji_selector);
        cancelButton = view.findViewById(R.id.close_button); // Renamed closeButton to cancelButton
        submitButton = view.findViewById(R.id.add_list_button); // Renamed addListButton to submitButton
    }

    private void setupClickListeners() {
        cancelButton.setOnClickListener(v -> dismiss()); // Changed closeButton to cancelButton
        
        // Emoji selector - open emoji picker
        emojiSelector.setOnClickListener(v -> {
            CategoryIconPickerBottomSheet iconPicker = new CategoryIconPickerBottomSheet(); // Changed to CategoryIconPickerBottomSheet
            iconPicker.setOnIconSelectedListener(iconKey -> { // Changed to setOnIconSelectedListener
                selectedIconKey = iconKey;
                emojiSelector.setImageResource(IconHelper.getIconDrawable(iconKey)); // Updated to use IconHelper and setImageResource
                emojiSelector.setColorFilter(IconHelper.getIconColor(iconKey));
            });
            iconPicker.show(getParentFragmentManager(), "CategoryIconPicker"); // Changed tag
        });
        
        // Add list button
        submitButton.setOnClickListener(v -> { // Changed addListButton to submitButton
            String listName = listNameInput.getText().toString().trim(); // Changed 'name' to 'listName'
            if (listName.isEmpty()) { // Changed condition and error handling
                listNameInput.setError(getString(R.string.error_empty_list_name));
                return;
            }

            if (listener != null) {
                listener.onListAdded(listName, selectedIconKey); // Passed selectedIconKey
            }
            dismiss();
        });
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }
}
