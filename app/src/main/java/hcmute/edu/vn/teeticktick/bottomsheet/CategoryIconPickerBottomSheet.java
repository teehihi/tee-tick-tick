package hcmute.edu.vn.teeticktick.bottomsheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Arrays;
import java.util.List;

import hcmute.edu.vn.teeticktick.R;
import hcmute.edu.vn.teeticktick.adapter.CategoryIconPickerAdapter;
import hcmute.edu.vn.teeticktick.utils.IconHelper;

public class CategoryIconPickerBottomSheet extends BottomSheetDialogFragment {

    private OnIconSelectedListener listener;

    public interface OnIconSelectedListener {
        void onIconSelected(String iconKey);
    }

    public void setOnIconSelectedListener(OnIconSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_icon_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.icon_recyclerview);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 5));

        List<String> iconKeys = Arrays.asList(IconHelper.getAvailableIconKeys());

        CategoryIconPickerAdapter adapter = new CategoryIconPickerAdapter(iconKeys, iconKey -> {
            if (listener != null) {
                listener.onIconSelected(iconKey);
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
