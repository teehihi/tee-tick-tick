package hcmute.edu.vn.teeticktick;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    private RadioGroup languageRadioGroup;
    private RadioButton radioVietnamese, radioEnglish;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Update toolbar title
        if (getActivity() != null) {
            ((MainActivity) getActivity()).getSupportActionBar().setTitle(R.string.settings);
        }

        prefs = requireActivity().getSharedPreferences("AppSettings", requireContext().MODE_PRIVATE);

        languageRadioGroup = view.findViewById(R.id.language_radio_group);
        radioVietnamese = view.findViewById(R.id.radio_vietnamese);
        radioEnglish = view.findViewById(R.id.radio_english);
        
        // Force green color for radio buttons
        int greenColor = getResources().getColor(R.color.green_primary, null);
        radioVietnamese.setButtonTintList(android.content.res.ColorStateList.valueOf(greenColor));
        radioEnglish.setButtonTintList(android.content.res.ColorStateList.valueOf(greenColor));

        // Load saved language
        String currentLanguage = prefs.getString("language", "vi");
        if (currentLanguage.equals("vi")) {
            radioVietnamese.setChecked(true);
        } else {
            radioEnglish.setChecked(true);
        }

        // Handle language change
        languageRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String newLanguage = checkedId == R.id.radio_vietnamese ? "vi" : "en";
            prefs.edit().putString("language", newLanguage).apply();

            // Restart activity to apply language
            Intent intent = requireActivity().getIntent();
            requireActivity().finish();
            startActivity(intent);
        });

        // Smart List Settings button
        view.findViewById(R.id.smart_list_settings_button).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SmartListSettingsActivity.class);
            startActivity(intent);
        });
    }
}
