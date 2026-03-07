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

import androidx.lifecycle.ViewModelProvider;
import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.teeticktick.R;
import hcmute.edu.vn.teeticktick.adapter.ListPickerAdapter;
import hcmute.edu.vn.teeticktick.database.CategoryEntity;
import hcmute.edu.vn.teeticktick.model.TaskList;
import hcmute.edu.vn.teeticktick.viewmodel.CategoryViewModel;

public class ListPickerBottomSheet extends BottomSheetDialogFragment {

    private String selectedList;
    private OnListSelectedListener listener;
    private CategoryViewModel categoryViewModel;
    private ListPickerAdapter adapter;

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

        adapter = new ListPickerAdapter(new ArrayList<>(), selectedList, list -> {
            if (listener != null) {
                listener.onListSelected(list);
            }
            dismiss();
        });
        recyclerView.setAdapter(adapter);

        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
        categoryViewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            List<TaskList> lists = new ArrayList<>();
            for (CategoryEntity entity : categories) {
                String displayName = entity.getName();
                if (entity.getSystemId() != null) {
                    switch (entity.getSystemId()) {
                        case "Inbox": displayName = getString(R.string.list_inbox); break;
                        case "Work": displayName = getString(R.string.list_work); break;
                        case "Personal": displayName = getString(R.string.list_personal); break;
                        case "Shopping": displayName = getString(R.string.list_shopping); break;
                        case "Learning": displayName = getString(R.string.list_learning); break;
                    }
                }
                lists.add(new TaskList(entity.getEmoji(), entity.getName(), displayName));
            }
            // Update adapter (we need a method on the adapter to update its list, or recreate it)
            adapter.updateData(lists);
        });
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }
}
