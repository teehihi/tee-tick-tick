package hcmute.edu.vn.teeticktick.bottomsheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Arrays;
import java.util.List;

import hcmute.edu.vn.teeticktick.R;
import hcmute.edu.vn.teeticktick.adapter.ListPickerAdapter;
import hcmute.edu.vn.teeticktick.model.TaskList;

public class ListPickerBottomSheet extends BottomSheetDialogFragment {

    private String selectedList;
    private OnListSelectedListener listener;

    public interface OnListSelectedListener {
        void onListSelected(TaskList list);
    }

    public void setSelectedList(String selectedList) {
        this.selectedList = selectedList;
    }

    public void setOnListSelectedListener(OnListSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_list_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.list_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Store key in English, display in user's language
        List<TaskList> lists = Arrays.asList(
            new TaskList("👋", "Welcome", getString(R.string.list_inbox)),
            new TaskList("💼", "Work", getString(R.string.list_work)),
            new TaskList("🏠", "Personal", getString(R.string.list_personal)),
            new TaskList("📦", "Shopping", getString(R.string.list_shopping)),
            new TaskList("📚", "Learning", getString(R.string.list_learning))
        );

        ListPickerAdapter adapter = new ListPickerAdapter(lists, selectedList, list -> {
            if (listener != null) {
                listener.onListSelected(list);
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
