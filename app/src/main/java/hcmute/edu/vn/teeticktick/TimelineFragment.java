package hcmute.edu.vn.teeticktick;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import hcmute.edu.vn.teeticktick.database.TaskEntity;
import hcmute.edu.vn.teeticktick.databinding.FragmentTimelineBinding;
import hcmute.edu.vn.teeticktick.viewmodel.TaskViewModel;

public class TimelineFragment extends Fragment {

    private FragmentTimelineBinding binding;
    private TaskViewModel taskViewModel;

    private static final int NUM_DAYS      = 7;
    private static final int COLUMN_WIDTH_DP_MIN = 60;
    private static final int COLUMN_WIDTH_DP_DEFAULT = 88;
    private static final int COLUMN_WIDTH_DP_MAX = 600; // ~24h per column at max zoom
    
    // Current zoom level (column width)
    private int currentColumnWidthDp = COLUMN_WIDTH_DP_DEFAULT;

    // Week offset from current week
    private int weekOffset = 0;

    // Category visibility state (all visible by default)
    private final boolean[] categoryVisible = {true, true, true, true, true};

    private static final String[] CATEGORY_LABELS = {
            "Hộp thư", "Công việc", "Cá nhân", "Mua sắm", "Học tập"
    };

    // iOS-system accent colors per category
    private static final int[] CATEGORY_BAR_BG = {
            0x335AC8FA, // Inbox – light sky blue tint  (33% alpha)
            0x33BF5AF2, // Work  – light purple tint
            0x3334C759, // Personal – light green tint
            0x33FF9500, // Shopping – light orange tint
            0x33FF3B30  // Learning – light red tint
    };
    private static final int[] CATEGORY_BAR_TEXT = {
            0xFF007AFF, // Inbox  – iOS blue
            0xFF6E14D5, // Work   – iOS purple darker
            0xFF1E8A3D, // Personal – iOS green darker
            0xFFB86800, // Shopping – iOS orange darker
            0xFFD00000  // Learning – iOS red darker
    };
    private static final int[] CATEGORY_BAR_STROKE = {
            0xFF5AC8FA, // Inbox
            0xFFBF5AF2, // Work
            0xFF34C759, // Personal
            0xFFFF9500, // Shopping
            0xFFFF3B30  // Learning
    };

    private FrameLayout[] gridRows;
    private List<TaskEntity> cachedTasks = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTimelineBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        setHasOptionsMenu(true);

        gridRows = new FrameLayout[]{
                binding.rowInbox, binding.rowWork, binding.rowPersonal,
                binding.rowShopping, binding.rowLearning
        };

        binding.btnPrevWeek.setOnClickListener(v -> { weekOffset--; refreshTimeline(); });
        binding.btnNextWeek.setOnClickListener(v -> { weekOffset++; refreshTimeline(); });

        // Zoom controls
        binding.btnZoomIn.setOnClickListener(v -> {
            currentColumnWidthDp = Math.min(currentColumnWidthDp + 30, COLUMN_WIDTH_DP_MAX);
            refreshTimeline();
        });
        binding.btnZoomOut.setOnClickListener(v -> {
            currentColumnWidthDp = Math.max(currentColumnWidthDp - 30, COLUMN_WIDTH_DP_MIN);
            refreshTimeline();
        });

        // Setup pinch-to-zoom on the horizontal scroll view
        setupPinchToZoom();

        taskViewModel.getAllTasks().observe(getViewLifecycleOwner(), tasks -> {
            if (tasks != null) { cachedTasks = tasks; refreshTimeline(); }
        });

        // After first layout pass, re-render pills with accurate row heights
        binding.ganttContainer.getViewTreeObserver().addOnGlobalLayoutListener(
            new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    binding.ganttContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    populateGanttGrid(cachedTasks);
                }
            }
        );
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    private void refreshTimeline() {
        updateWeekLabel();
        buildDateHeader();
        populateGanttGrid(cachedTasks);
        applyCategoryVisibility();
    }

    private void updateWeekLabel() {
        Calendar start = getWeekStart();
        Calendar end   = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_MONTH, NUM_DAYS - 1);
        SimpleDateFormat fmt = new SimpleDateFormat("MMM d", Locale.ENGLISH);
        binding.tvWeekLabel.setText(fmt.format(start.getTime()) + " – " + fmt.format(end.getTime()));
    }

    // ── Date Header ───────────────────────────────────────────────────────────

    private void buildDateHeader() {
        LinearLayout header = binding.dateHeaderRow;
        header.removeAllViews();

        Calendar cal   = getWeekStart();
        SimpleDateFormat dayFmt = new SimpleDateFormat("EEE", Locale.ENGLISH);

        Calendar today  = Calendar.getInstance();
        int todayYear   = today.get(Calendar.YEAR);
        int todayDoy    = today.get(Calendar.DAY_OF_YEAR);

        int colPx = dpToPx(currentColumnWidthDp);

        for (int i = 0; i < NUM_DAYS; i++) {
            boolean isToday = cal.get(Calendar.YEAR) == todayYear
                           && cal.get(Calendar.DAY_OF_YEAR) == todayDoy;

            // Day name
            TextView dayTv = new TextView(requireContext());
            dayTv.setText(dayFmt.format(cal.getTime()).toUpperCase());
            dayTv.setTextSize(10);
            dayTv.setGravity(Gravity.CENTER);
            dayTv.setLetterSpacing(0.08f);
            dayTv.setTextColor(isToday ? 0xFFFF3B30 : 0xFF8E8E93);

            // Date number
            TextView dateTv = new TextView(requireContext());
            dateTv.setText(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));
            dateTv.setTextSize(17);
            dateTv.setGravity(Gravity.CENTER);
            dateTv.setIncludeFontPadding(false);

            int circleSize = dpToPx(32);
            LinearLayout.LayoutParams dateLP = new LinearLayout.LayoutParams(circleSize, circleSize);
            dateLP.topMargin = dpToPx(2);
            dateTv.setLayoutParams(dateLP);

            if (isToday) {
                dateTv.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_date_today));
                dateTv.setTextColor(Color.WHITE);
                dateTv.setTextSize(16);
            } else {
                dateTv.setTextColor(0xFF1C1C1E);
            }

            // Cell
            LinearLayout cell = new LinearLayout(requireContext());
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams cellFill = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            cell.setLayoutParams(cellFill);
            cell.addView(dayTv);
            cell.addView(dateTv);

            // Hair-line right divider
            View div = new View(requireContext());
            LinearLayout.LayoutParams divLP = new LinearLayout.LayoutParams(dpToPx(1), ViewGroup.LayoutParams.MATCH_PARENT);
            div.setLayoutParams(divLP);
            div.setBackgroundColor(0xFFE5E5EA);

            LinearLayout col = new LinearLayout(requireContext());
            col.setOrientation(LinearLayout.HORIZONTAL);
            col.setLayoutParams(new LinearLayout.LayoutParams(colPx, ViewGroup.LayoutParams.MATCH_PARENT));
            col.addView(cell);
            col.addView(div);
            header.addView(col);

            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        int totalPx = colPx * NUM_DAYS;
        setRowWidth(binding.dateHeaderRow, totalPx);
        for (FrameLayout row : gridRows) setRowWidth(row, totalPx);
    }

    // ── Populate Grid ─────────────────────────────────────────────────────────

    private void populateGanttGrid(List<TaskEntity> allTasks) {
        for (FrameLayout row : gridRows) {
            row.removeAllViews();
            addColumnDividers(row);
        }

        Calendar ws      = getWeekStart();
        long weekStartMs = ws.getTimeInMillis();
        long dayMs       = 24L * 60 * 60 * 1000;
        long weekEndMs   = weekStartMs + NUM_DAYS * dayMs;

        String[] listKeys = {"Inbox", "Work", "Personal", "Shopping", "Learning"};

        // Use a Map to avoid raw generic array (which caused dex issue)
        Map<String, List<TaskEntity>> grouped = new HashMap<>();
        for (String k : listKeys) grouped.put(k, new ArrayList<>());

        boolean anyVisible = false;
        for (TaskEntity task : allTasks) {
            if (task.getDueDate() == null) continue;
            long taskStart = task.getCreatedAt();
            long taskEnd   = task.getDueDate();
            if (taskEnd <= weekStartMs || taskStart >= weekEndMs) continue;

            String key = task.getListName();
            if (key == null || !grouped.containsKey(key)) key = "Inbox";
            grouped.get(key).add(task);
            anyVisible = true;
        }

        // Sort tasks by createdAt time within each category
        for (List<TaskEntity> taskList : grouped.values()) {
            taskList.sort((t1, t2) -> Long.compare(t1.getCreatedAt(), t2.getCreatedAt()));
        }

        int colPx = dpToPx(currentColumnWidthDp);
        for (int i = 0; i < listKeys.length; i++) {
            placeSpannedBars(gridRows[i], grouped.get(listKeys[i]),
                    weekStartMs, dayMs, colPx, i);
        }

        // Always keep the grid visible — show empty rows if no tasks
        binding.timelineEmptyState.setVisibility(View.GONE);
        binding.ganttContainer.setVisibility(View.VISIBLE);
    }

    /**
     * Place pill bars using hour-based positioning within each day column.
     * leftPx  = dayCol * colPx + (startHourFraction * colPx)
     * barWidth = durationFraction * colPx  (clamped to visible window)
     * Multiple tasks on same day stack vertically.
     */
    private void placeSpannedBars(FrameLayout row, List<TaskEntity> tasks,
                                   long weekStartMs, long dayMs, int colPx, int categoryIdx) {
        if (tasks == null || tasks.isEmpty()) return;

        int bgColor     = CATEGORY_BAR_BG[categoryIdx];
        int textColor   = CATEGORY_BAR_TEXT[categoryIdx];
        int strokeColor = CATEGORY_BAR_STROKE[categoryIdx];

        boolean showTimeMode = currentColumnWidthDp >= 110;  // 1 hour = colPx/24, show time when ≥ ~4.5dp/hr
        int rowH      = row.getHeight() > 0 ? row.getHeight() : dpToPx(72);
        int barH      = showTimeMode ? dpToPx(36) : dpToPx(24);
        int stackStep = barH + dpToPx(4);

        // Pre-count tasks per day for vertical centering
        Map<Integer, Integer> dayTotalCount = new HashMap<>();
        for (TaskEntity task : tasks) {
            if (task.getDueDate() == null) continue;
            long taskStartMs = task.getCreatedAt();
            long taskEndMs   = task.getDueDate();
            if (taskEndMs <= weekStartMs || taskStartMs >= weekStartMs + NUM_DAYS * dayMs) continue;
            long visStart = Math.max(taskStartMs, weekStartMs);
            int dayCol = (int)((visStart - weekStartMs) / dayMs);
            dayCol = Math.max(0, Math.min(dayCol, NUM_DAYS - 1));
            dayTotalCount.put(dayCol, (dayTotalCount.containsKey(dayCol) ? dayTotalCount.get(dayCol) : 0) + 1);
        }

        // Per-day stack counter: key = day index (0-6)
        Map<Integer, Integer> dayStackCount = new HashMap<>();

        for (TaskEntity task : tasks) {
            if (task.getDueDate() == null) continue;

            long taskStartMs = task.getCreatedAt();
            long taskEndMs   = task.getDueDate();
            if (taskEndMs <= weekStartMs || taskStartMs >= weekStartMs + NUM_DAYS * dayMs) continue;

            // Clamp to visible week window
            long visStart = Math.max(taskStartMs, weekStartMs);
            long visEnd   = Math.min(taskEndMs, weekStartMs + NUM_DAYS * dayMs);
            if (visEnd <= visStart) continue;

            // Day column of the start position
            int startDayCol = (int) ((visStart - weekStartMs) / dayMs);
            startDayCol = Math.max(0, Math.min(startDayCol, NUM_DAYS - 1));

            // Hour fraction within the start day (0.0 = 00:00, 1.0 = 24:00)
            long dayStartMs = weekStartMs + (long) startDayCol * dayMs;
            float startFraction = (float)(visStart - dayStartMs) / dayMs;

            // Duration as fraction of a day (can span multiple days)
            float durationFraction = (float)(visEnd - visStart) / dayMs;
            // Cap width at remaining days in view
            float maxFraction = NUM_DAYS - startDayCol - startFraction;
            durationFraction = Math.min(durationFraction, maxFraction);
            durationFraction = Math.max(durationFraction, 1f / 24f); // min 1 hour width

            int leftPx   = (int)(startDayCol * colPx + startFraction * colPx) + dpToPx(2);
            int barWidth = Math.max((int)(durationFraction * colPx) - dpToPx(4), dpToPx(48));

            // Stack: count tasks already placed on the start day
            int stack = dayStackCount.containsKey(startDayCol) ? dayStackCount.get(startDayCol) : 0;
            dayStackCount.put(startDayCol, stack + 1);

            // Vertical centering: offset the whole group to center in row
            int totalInDay = dayTotalCount.containsKey(startDayCol) ? dayTotalCount.get(startDayCol) : 1;
            int groupHeight = totalInDay * barH + (totalInDay - 1) * dpToPx(4);
            int centerOffset = Math.max(0, (rowH - groupHeight) / 2);

            // Skip if overflows row
            int topMargin = centerOffset + stack * stackStep;
            if (topMargin + barH > rowH - dpToPx(2)) continue;

            // Build pill
            // Show time label when pill is wide enough to display it (≥ 60dp)
            boolean showTime = barWidth >= dpToPx(60);

            android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            shape.setCornerRadius(dpToPx(10));
            shape.setColor(bgColor);
            shape.setStroke(dpToPx(1), strokeColor);

            LinearLayout pill = new LinearLayout(requireContext());
            pill.setOrientation(LinearLayout.VERTICAL);
            pill.setGravity(Gravity.CENTER);
            pill.setPaddingRelative(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2));
            pill.setBackground(shape);

            TextView tvTitle = new TextView(requireContext());
            tvTitle.setText(buildLabel(task));
            tvTitle.setTextColor(textColor);
            tvTitle.setTextSize(11f);
            tvTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            tvTitle.setSingleLine(true);
            tvTitle.setEllipsize(TextUtils.TruncateAt.END);
            tvTitle.setIncludeFontPadding(false);
            tvTitle.setGravity(Gravity.CENTER);
            tvTitle.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            pill.addView(tvTitle);

            if (showTime) {
                TextView tvTime = new TextView(requireContext());
                tvTime.setText(buildTimeLabel(task));
                tvTime.setTextColor(0xFF888888);
                tvTime.setTextSize(9f);
                tvTime.setGravity(Gravity.CENTER);
                tvTime.setTypeface(android.graphics.Typeface.DEFAULT);
                tvTime.setSingleLine(true);
                tvTime.setEllipsize(TextUtils.TruncateAt.END);
                tvTime.setIncludeFontPadding(false);
                LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                timeLp.topMargin = dpToPx(1);
                tvTime.setLayoutParams(timeLp);
                pill.addView(tvTime);
            }

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(barWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = leftPx;
            lp.topMargin  = topMargin;
            pill.setLayoutParams(lp);

            final int taskId = task.getId();
            final TaskEntity taskRef = task;
            final int bgCol = bgColor;
            final int txtCol = textColor;
            final int strokeCol = strokeColor;
            pill.setOnClickListener(v -> {
                Bundle b = new Bundle();
                b.putInt("taskId", taskId);
                NavHostFragment.findNavController(TimelineFragment.this)
                        .navigate(R.id.TaskDetailFragment, b);
            });
            pill.setOnLongClickListener(v -> {
                showTaskPreviewPopup(taskRef, pill, bgCol, txtCol, strokeCol);
                return true;
            });

            row.addView(pill);
        }
    }

    private void showTaskPreviewPopup(TaskEntity task, View anchor, int bgColor, int textColor, int strokeColor) {
        View popupView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_task_preview, null);

        // Use stroke color (full opacity category color) as popup background base
        // Apply a light tint: mix strokeColor with white at ~80% white
        int sr = (strokeColor >> 16) & 0xFF;
        int sg = (strokeColor >> 8) & 0xFF;
        int sb = strokeColor & 0xFF;
        // 30% stroke color + 70% white → matches the pill tint feel but solid
        int solidBg = Color.rgb(
                (int)(sr * 0.30f + 255 * 0.70f),
                (int)(sg * 0.30f + 255 * 0.70f),
                (int)(sb * 0.30f + 255 * 0.70f)
        );
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(14));
        bg.setColor(solidBg);
        bg.setStroke(dpToPx(2), strokeColor);
        popupView.setBackground(bg);

        // Populate views
        TextView tvEmoji = popupView.findViewById(R.id.popup_emoji);
        TextView tvTitle = popupView.findViewById(R.id.popup_title);
        TextView tvTime  = popupView.findViewById(R.id.popup_time);
        TextView tvDesc  = popupView.findViewById(R.id.popup_description);
        TextView tvHint  = popupView.findViewById(R.id.popup_hint);

        String emoji = task.getEmoji();
        if (emoji != null && !emoji.isEmpty()) {
            tvEmoji.setText(emoji);
            tvEmoji.setVisibility(View.VISIBLE);
        } else {
            tvEmoji.setVisibility(View.GONE);
        }

        tvTitle.setText(task.getTitle() != null ? task.getTitle() : "");
        tvTitle.setTextColor(textColor);

        tvTime.setText(buildTimeLabel(task));
        tvTime.setTextColor(textColor);

        String desc = task.getDescription();
        if (desc != null && !desc.trim().isEmpty()) {
            tvDesc.setText(desc);
            tvDesc.setTextColor(0xFF666666);
            tvDesc.setVisibility(View.VISIBLE);
        } else {
            tvDesc.setVisibility(View.GONE);
            // Hide divider too when no description
            View divider = popupView.findViewById(R.id.popup_divider);
            if (divider != null) divider.setVisibility(View.GONE);
        }

        tvHint.setTextColor((textColor & 0x00FFFFFF) | 0x99000000);

        // Create popup window
        PopupWindow popup = new PopupWindow(
                popupView,
                dpToPx(260),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true // focusable — dismisses on outside touch
        );
        popup.setElevation(dpToPx(12));
        popup.setOutsideTouchable(true);
        popup.setAnimationStyle(R.style.PopupAnimation);

        // Dim background while popup is open
        final View decorView = requireActivity().getWindow().getDecorView();
        final android.view.WindowManager.LayoutParams wlp =
                (android.view.WindowManager.LayoutParams) decorView.getLayoutParams();
        requireActivity().getWindow().setDimAmount(0.4f);
        requireActivity().getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        popup.setOnDismissListener(() ->
                requireActivity().getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND));

        // Navigate to detail on popup click
        final int taskId = task.getId();
        popupView.setOnClickListener(v -> {
            popup.dismiss();
            Bundle bundle = new Bundle();
            bundle.putInt("taskId", taskId);
            NavHostFragment.findNavController(TimelineFragment.this)
                    .navigate(R.id.TaskDetailFragment, bundle);
        });

        // Show centered on screen
        popup.showAtLocation(decorView, Gravity.CENTER, 0, 0);
    }

    private String buildLabel(TaskEntity task) {
        String e = task.getEmoji();
        String t = task.getTitle() != null ? task.getTitle() : "";
        return (e != null && !e.isEmpty()) ? e + " " + t : t;
    }

    private String buildTimeLabel(TaskEntity task) {
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        long taskStartMs = task.getCreatedAt();
        long taskEndMs   = task.getDueDate() != null ? task.getDueDate() : taskStartMs;

        boolean multiDay = startOfDay(taskEndMs) > startOfDay(taskStartMs);
        if (multiDay) {
            return dateFmt.format(new Date(taskStartMs)) + " " + timeFmt.format(new Date(taskStartMs))
                 + " – " + dateFmt.format(new Date(taskEndMs)) + " " + timeFmt.format(new Date(taskEndMs));
        } else {
            return dateFmt.format(new Date(taskStartMs))
                 + "  " + timeFmt.format(new Date(taskStartMs))
                 + " – " + timeFmt.format(new Date(taskEndMs));
        }
    }

    // ── Column Dividers ───────────────────────────────────────────────────────

    private void addColumnDividers(FrameLayout row) {
        int colPx = dpToPx(currentColumnWidthDp);
        for (int i = 1; i < NUM_DAYS; i++) {
            View div = new View(requireContext());
            FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(dpToPx(1), ViewGroup.LayoutParams.MATCH_PARENT);
            p.leftMargin = i * colPx;
            div.setLayoutParams(p);
            div.setBackgroundColor(0xFFE5E5EA);
            row.addView(div);
        }
    }

    // ── Pinch to Zoom ─────────────────────────────────────────────────────────

    private void setupPinchToZoom() {
        final android.view.ScaleGestureDetector scaleDetector = new android.view.ScaleGestureDetector(
                requireContext(),
                new android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    private float lastScaleFactor = 1.0f;
                    
                    @Override
                    public boolean onScaleBegin(android.view.ScaleGestureDetector detector) {
                        lastScaleFactor = 1.0f;
                        return true;
                    }
                    
                    @Override
                    public boolean onScale(android.view.ScaleGestureDetector detector) {
                        float scaleFactor = detector.getScaleFactor();
                        
                        // Accumulate scale changes
                        lastScaleFactor *= scaleFactor;
                        
                        // Apply zoom with smoothing
                        int newWidth = (int) (currentColumnWidthDp * lastScaleFactor);
                        newWidth = Math.max(COLUMN_WIDTH_DP_MIN, Math.min(newWidth, COLUMN_WIDTH_DP_MAX));
                        
                        if (Math.abs(newWidth - currentColumnWidthDp) >= 5) {
                            currentColumnWidthDp = newWidth;
                            lastScaleFactor = 1.0f;
                            refreshTimeline();
                        }
                        return true;
                    }
                }
        );

        final android.view.GestureDetector gestureDetector = new android.view.GestureDetector(
                requireContext(),
                new android.view.GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onScroll(android.view.MotionEvent e1, android.view.MotionEvent e2,
                                          float distanceX, float distanceY) {
                        // Allow normal scrolling
                        return false;
                    }
                }
        );

        binding.ganttHorizontalScroll.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                // Handle pinch gesture
                scaleDetector.onTouchEvent(event);
                
                // If it's a multi-touch gesture (pinch), consume the event
                if (event.getPointerCount() > 1) {
                    return true;
                }
                
                // Otherwise, allow normal scrolling
                return false;
            }
        });
    }

    // ── Options Menu (filter icon in toolbar) ────────────────────────────────

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_timeline, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_filter_categories) {
            showFilterDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ── Filter Dialog ─────────────────────────────────────────────────────────

    private void showFilterDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_filter_categories, null);
        LinearLayout container = dialogView.findViewById(R.id.dialog_category_container);
        
        boolean[] tempChecked = categoryVisible.clone();
        
        for (int i = 0; i < CATEGORY_LABELS.length; i++) {
            final int index = i;
            View itemView = LayoutInflater.from(requireContext()).inflate(R.layout.item_dialog_filter_category, container, false);
            
            View dot = itemView.findViewById(R.id.item_category_dot);
            TextView name = itemView.findViewById(R.id.item_category_name);
            android.widget.Switch toggle = itemView.findViewById(R.id.item_category_toggle);
            
            dot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(CATEGORY_BAR_STROKE[i]));
            name.setText(CATEGORY_LABELS[i]);
            toggle.setChecked(tempChecked[i]);
            
            itemView.setOnClickListener(v -> {
                tempChecked[index] = !tempChecked[index];
                toggle.setChecked(tempChecked[index]);
            });
            
            container.addView(itemView);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btn_dialog_cancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btn_dialog_apply).setOnClickListener(v -> {
            System.arraycopy(tempChecked, 0, categoryVisible, 0, categoryVisible.length);
            applyCategoryVisibility();
            dialog.dismiss();
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        dialog.show();
    }

    private void applyCategoryVisibility() {
        if (binding == null) return;
        int[] sidebarIds = {R.id.sidebar_row_1, R.id.sidebar_row_2, R.id.sidebar_row_3,
                            R.id.sidebar_row_4, R.id.sidebar_row_5};
        for (int i = 0; i < categoryVisible.length; i++) {
            int vis = categoryVisible[i] ? View.VISIBLE : View.GONE;
            View sr = binding.sidebarLabels.findViewById(sidebarIds[i]);
            if (sr != null) sr.setVisibility(vis);
            gridRows[i].setVisibility(vis);
        }
        // Recalculate row heights after visibility changes
        binding.ganttContainer.post(() -> populateGanttGrid(cachedTasks));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Calendar getWeekStart() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_MONTH, -(cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY));
        cal.add(Calendar.WEEK_OF_YEAR, weekOffset);
        return cal;
    }

    private long startOfDay(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private void setRowWidth(View row, int widthPx) {
        ViewGroup.LayoutParams p = row.getLayoutParams();
        if (p == null) p = new ViewGroup.LayoutParams(widthPx, ViewGroup.LayoutParams.MATCH_PARENT);
        else p.width = widthPx;
        row.setLayoutParams(p);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
