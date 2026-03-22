package hcmute.edu.vn.teeticktick;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import hcmute.edu.vn.teeticktick.database.TaskEntity;
import hcmute.edu.vn.teeticktick.service.TaskReminderScheduler;
import hcmute.edu.vn.teeticktick.utils.IconHelper;
import hcmute.edu.vn.teeticktick.viewmodel.TaskViewModel;

public class TaskDetailFragment extends Fragment {

    private TaskViewModel taskViewModel;
    private TaskEntity currentTask;
    private FrameLayout btnComplete;
    private TextView tvCompleteLabel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task_detail, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Hide MainActivity toolbar — this fragment has its own top bar
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideToolbarAndFab();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Only restore toolbar when actually navigating away (not when bottom sheet opens)
        // Check if we're still the current destination
        if (getActivity() instanceof MainActivity) {
            androidx.navigation.NavController nav = null;
            try {
                nav = androidx.navigation.Navigation.findNavController(
                        requireActivity(), R.id.nav_host_fragment_content_main);
            } catch (Exception ignored) {}
            if (nav != null && nav.getCurrentDestination() != null
                    && nav.getCurrentDestination().getId() == R.id.TaskDetailFragment) {
                return; // Still on this fragment (e.g. bottom sheet opened), don't restore
            }
            ((MainActivity) getActivity()).restoreToolbarAndFab();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Restore toolbar when fragment is destroyed (navigating away)
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).restoreToolbarAndFab();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        btnComplete = view.findViewById(R.id.btn_complete);
        tvCompleteLabel = view.findViewById(R.id.tv_complete_label);

        int taskId = getArguments() != null ? getArguments().getInt("taskId", -1) : -1;
        if (taskId == -1) {
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }

        taskViewModel.getAllTasks().observe(getViewLifecycleOwner(), tasks -> {
            if (tasks == null) return;
            for (TaskEntity task : tasks) {
                if (task.getId() == taskId) {
                    currentTask = task;
                    bindTask(view, task);
                    break;
                }
            }
        });

        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        // More options button → Chỉnh sửa / Chia sẻ / Xóa
        view.findViewById(R.id.btn_more).setOnClickListener(v -> {
            if (currentTask == null) return;
            showMoreOptionsMenu(v);
        });

        btnComplete.setOnClickListener(v -> {
            if (currentTask == null) return;
            currentTask.setCompleted(!currentTask.isCompleted());
            taskViewModel.update(currentTask);
            updateCompleteButton(currentTask.isCompleted());
            Toast.makeText(requireContext(),
                    currentTask.isCompleted() ? "Đã hoàn thành!" : "Đã bỏ hoàn thành",
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void showMoreOptionsMenu(View anchor) {
        View popupView = LayoutInflater.from(requireContext())
                .inflate(R.layout.popup_more_options, null);

        // Measure to get size
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupWidth = (int) (200 * requireContext().getResources().getDisplayMetrics().density);
        int popupHeight = ViewGroup.LayoutParams.WRAP_CONTENT;

        PopupWindow popup = new PopupWindow(popupView, popupWidth, popupHeight, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setElevation(32f * requireContext().getResources().getDisplayMetrics().density);
        popup.setOutsideTouchable(true);

        // Position: below and to the left of anchor (top-right button)
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        int xOff = -(popupWidth - anchor.getWidth());
        popup.showAsDropDown(anchor, xOff, 8);

        popupView.findViewById(R.id.option_edit).setOnClickListener(v -> {
            popup.dismiss();
            openEditSheet();
        });
        popupView.findViewById(R.id.option_share).setOnClickListener(v -> {
            popup.dismiss();
            shareTask();
        });
        popupView.findViewById(R.id.option_delete).setOnClickListener(v -> {
            popup.dismiss();
            confirmDelete();
        });
    }

    private void openEditSheet() {
        if (currentTask == null) return;
        AddTaskBottomSheet sheet = new AddTaskBottomSheet();
        // Pre-fill with current task data
        Bundle args = new Bundle();
        args.putInt("editTaskId", currentTask.getId());
        args.putString("editTitle", currentTask.getTitle());
        args.putString("editDescription", currentTask.getDescription());
        args.putString("editEmoji", currentTask.getEmoji());
        args.putString("editListName", currentTask.getListName());
        args.putBoolean("calledFromDetail", true); // flag to prevent toolbar restore
        if (currentTask.getStartDate() != null) args.putLong("editStartDate", currentTask.getStartDate());
        if (currentTask.getDueDate() != null) args.putLong("editDueDate", currentTask.getDueDate());
        sheet.setArguments(args);
        sheet.setOnTaskAddedListener((title, description, emoji, listName, startDate, dueDate) -> {
            currentTask.setTitle(title);
            currentTask.setDescription(description);
            currentTask.setEmoji(emoji);
            currentTask.setListName(listName);
            currentTask.setStartDate(startDate);
            currentTask.setDueDate(dueDate);
            taskViewModel.update(currentTask);
        });
        sheet.show(getParentFragmentManager(), "EditTaskSheet");
    }

    private void shareTask() {
        if (currentTask == null) return;
        String text = currentTask.getTitle();
        if (currentTask.getDescription() != null && !currentTask.getDescription().isEmpty()) {
            text += "\n" + currentTask.getDescription();
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ task"));
    }

    private void confirmDelete() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa task")
                .setMessage("Bạn có chắc muốn xóa task này không?")
                .setPositiveButton("Xóa", (d, w) -> {
                    if (getContext() != null) {
                        TaskReminderScheduler.cancelReminder(getContext(), currentTask.getId());
                        TaskReminderScheduler.cancelOverdueReminders(getContext(), currentTask.getId());
                    }
                    taskViewModel.delete(currentTask);
                    NavHostFragment.findNavController(this).popBackStack();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void bindTask(View view, TaskEntity task) {
        // --- Icon ---
        FrameLayout iconContainer = view.findViewById(R.id.icon_container);
        ImageView taskIcon = view.findViewById(R.id.task_icon);
        int iconColor = Color.parseColor("#2B7FFF");

        String iconField = task.getEmoji();
        if (iconField != null && !iconField.isEmpty()) {
            String iconName = iconField.contains("|") ? iconField.split("\\|")[0] : iconField;
            String colorHex = iconField.contains("|") ? iconField.split("\\|")[1] : null;
            if (colorHex != null) {
                try { iconColor = Color.parseColor(colorHex); } catch (Exception ignored) {}
            }
            int resId = requireContext().getResources().getIdentifier(
                    iconName, "drawable", requireContext().getPackageName());
            if (resId != 0) {
                Drawable d = ContextCompat.getDrawable(requireContext(), resId);
                if (d != null) {
                    d = d.mutate();
                    d.setTint(iconColor);
                    taskIcon.setImageDrawable(d);
                }
            }
        }

        // Rounded square background (iOS app icon style) with 15% opacity
        GradientDrawable squareBg = new GradientDrawable();
        squareBg.setShape(GradientDrawable.RECTANGLE);
        squareBg.setCornerRadius(dpToPx(16));
        int alpha = (int) (255 * 0.15f);
        squareBg.setColor((alpha << 24) | (iconColor & 0x00FFFFFF));
        iconContainer.setBackground(squareBg);

        // --- Title ---
        TextView tvTitle = (TextView) view.findViewById(R.id.task_title);
        tvTitle.setText(task.getTitle());
        if (task.isCompleted()) {
            tvTitle.setPaintFlags(tvTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            tvTitle.setTextColor(Color.parseColor("#9CA3AF"));
        } else {
            tvTitle.setPaintFlags(tvTitle.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            tvTitle.setTextColor(Color.parseColor("#101828"));
        }

        // --- Note / Description ---
        TextView tvNote = view.findViewById(R.id.tv_note);
        String desc = task.getDescription();
        if (desc != null && !desc.isEmpty()) {
            tvNote.setText(desc);
            tvNote.setVisibility(View.VISIBLE);
        } else {
            tvNote.setVisibility(View.GONE);
        }

        // --- Dates ---
        SimpleDateFormat sdfSingle = new SimpleDateFormat("EEEE, dd 'tháng' MM yyyy", new Locale("vi", "VN"));
        SimpleDateFormat sdfDuration = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        boolean hasDuration = task.getStartDate() != null && task.getDueDate() != null
                && !task.getStartDate().equals(task.getDueDate());

        View sectionSingleDate = view.findViewById(R.id.section_single_date);
        View sectionDuration = view.findViewById(R.id.section_duration);

        if (hasDuration) {
            sectionSingleDate.setVisibility(View.GONE);
            sectionDuration.setVisibility(View.VISIBLE);

            ((TextView) view.findViewById(R.id.tv_start_date))
                    .setText(sdfDuration.format(new Date(task.getStartDate())));
            ((TextView) view.findViewById(R.id.tv_due_date_duration))
                    .setText(sdfDuration.format(new Date(task.getDueDate())));

            long diffMs = task.getDueDate() - task.getStartDate();
            long days = TimeUnit.MILLISECONDS.toDays(diffMs);
            long hours = TimeUnit.MILLISECONDS.toHours(diffMs) % 24;
            String durationText;
            if (days > 0 && hours > 0) durationText = "Thời lượng: " + days + " ngày " + hours + " giờ";
            else if (days > 0) durationText = "Thời lượng: " + days + " ngày";
            else durationText = "Thời lượng: " + hours + " giờ";
            ((TextView) view.findViewById(R.id.tv_duration)).setText(durationText);
        } else {
            sectionSingleDate.setVisibility(View.VISIBLE);
            sectionDuration.setVisibility(View.GONE);

            TextView tvDueDate = view.findViewById(R.id.tv_due_date);
            if (task.getDueDate() != null) {
                tvDueDate.setText(sdfSingle.format(new Date(task.getDueDate())));
            } else if (task.getStartDate() != null) {
                tvDueDate.setText(sdfSingle.format(new Date(task.getStartDate())));
            } else {
                tvDueDate.setText("Không có");
            }
        }

        // --- Category chip ---
        bindCategoryChip(view, task);

        // --- Status ---
        TextView tvStatus = view.findViewById(R.id.tv_status);
        tvStatus.setText(task.isCompleted() ? "Hoàn thành" : "Chưa hoàn thành");
        tvStatus.setTextColor(task.isCompleted()
                ? Color.parseColor("#2ECC71") : Color.parseColor("#8E8E93"));

        // --- Created at timestamp ---
        TextView tvCreatedAt = view.findViewById(R.id.tv_created_at);
        SimpleDateFormat sdfCreated = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());
        String createdStr = "Tạo lúc " + sdfCreated.format(new Date(task.getCreatedAt()));
        tvCreatedAt.setText(createdStr);

        // --- Complete button ---
        updateCompleteButton(task.isCompleted());
    }

    private void bindCategoryChip(View view, TaskEntity task) {
        LinearLayout chip = view.findViewById(R.id.category_chip);
        FrameLayout categoryDot = view.findViewById(R.id.category_dot);
        TextView chipName = view.findViewById(R.id.category_name);

        String listName = task.getListName();
        if (listName == null) listName = "Inbox";

        int chipColor = IconHelper.getIconColor(listName.toLowerCase());

        // Dot circle with category color
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(chipColor);
        categoryDot.setBackground(dotBg);

        chipName.setText(getLocalizedListName(listName));
    }

    private void updateCompleteButton(boolean isCompleted) {
        if (btnComplete == null || tvCompleteLabel == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(14));
        if (isCompleted) {
            bg.setColor(Color.parseColor("#00C950")); // green
            tvCompleteLabel.setText("✓  Đã hoàn thành");
            tvCompleteLabel.setTextColor(Color.WHITE);
        } else {
            bg.setColor(Color.parseColor("#F3F4F6")); // gray
            tvCompleteLabel.setText("Đánh dấu hoàn thành");
            tvCompleteLabel.setTextColor(Color.parseColor("#364153"));
        }
        btnComplete.setBackground(bg);
    }

    private String getLocalizedListName(String key) {
        if (key == null) return "Inbox";
        switch (key.toLowerCase()) {
            case "inbox": return getString(R.string.list_inbox);
            case "work": return getString(R.string.list_work);
            case "personal": return getString(R.string.list_personal);
            case "shopping": return getString(R.string.list_shopping);
            case "learning": return getString(R.string.list_learning);
            default: return key;
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * requireContext().getResources().getDisplayMetrics().density);
    }
}
