package hcmute.edu.vn.teeticktick;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Swipe-to-delete with two modes:
 *  - Slow swipe  → card snaps to PEEK_DP (80dp), reveals a "Xoá" button. Tap to confirm delete.
 *  - Fast flick or swipe past 50% → delete immediately (no confirmation needed).
 */
public class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

    public interface OnSwipeDeleteListener {
        void onDelete(int position);
    }

    // How far the card peeks open to show the delete button
    private static final int PEEK_DP = 80;

    private final OnSwipeDeleteListener listener;
    private final Paint bgPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int cornerRadius;
    private final int peekPx;
    private Drawable trashIcon;

    // Track which ViewHolder is currently "peeked open"
    private RecyclerView.ViewHolder peekedHolder = null;

    public SwipeToDeleteCallback(Context context, OnSwipeDeleteListener listener) {
        super(0, ItemTouchHelper.LEFT);
        this.listener    = listener;
        this.cornerRadius = dp(context, 14);
        this.peekPx      = dp(context, PEEK_DP);

        bgPaint.setColor(0xFFE53935);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(dp(context, 13));
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.CENTER);

        trashIcon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_delete);
        if (trashIcon != null) trashIcon.setTint(Color.WHITE);
    }

    // ─── ItemTouchHelper overrides ────────────────────────────────────────────

    @Override
    public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder a,
                          @NonNull RecyclerView.ViewHolder b) { return false; }

    @Override
    public int getSwipeDirs(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
        return (vh instanceof TaskGroupAdapter.TaskViewHolder) ? ItemTouchHelper.LEFT : 0;
    }

    /**
     * Called when ItemTouchHelper decides the swipe is "done".
     * We only reach here if the user swiped past the threshold (50%) or flicked fast.
     * → Delete immediately.
     */
    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        peekedHolder = null;
        listener.onDelete(viewHolder.getAdapterPosition());
    }

    /** 50% threshold → full swipe = instant delete */
    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder vh) { return 0.5f; }

    /** Lower escape velocity so a fast flick still triggers instant delete */
    @Override
    public float getSwipeEscapeVelocity(float def) { return def * 0.5f; }

    @Override
    public float getSwipeVelocityThreshold(float def) { return def * 1.2f; }

    // ─── Drawing ──────────────────────────────────────────────────────────────

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv,
                            @NonNull RecyclerView.ViewHolder vh,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {

        if (!(vh instanceof TaskGroupAdapter.TaskViewHolder)) {
            super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive);
            return;
        }

        TaskGroupAdapter.TaskViewHolder tvh = (TaskGroupAdapter.TaskViewHolder) vh;
        View itemView = vh.itemView;
        View card     = tvh.foregroundCard != null ? tvh.foregroundCard : itemView;

        // Clamp dX: don't let card slide further than full width during drag
        // (past 50% it will trigger onSwiped anyway)
        card.setTranslationX(dX);

        if (dX < 0) {
            float revealed = Math.abs(dX); // px revealed so far

            float top    = itemView.getTop()    + dp(rv.getContext(), 4);
            float bottom = itemView.getBottom() - dp(rv.getContext(), 4);
            float right  = itemView.getRight()  - dp(rv.getContext(), 8);
            float left   = right - revealed;

            // Draw red background
            bgPaint.setAlpha(255);
            c.drawRoundRect(new RectF(left, top, right, bottom), cornerRadius, cornerRadius, bgPaint);

            // Draw icon + label centered in the revealed strip
            float centerX = (left + right) / 2f;
            float centerY = (top + bottom) / 2f;

            if (trashIcon != null) {
                int iconSize = dp(rv.getContext(), 20);
                int iconLeft = (int)(centerX - iconSize / 2f);
                int iconTop  = (int)(centerY - iconSize / 2f - dp(rv.getContext(), 8));
                trashIcon.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize);
                trashIcon.setAlpha(255);
                trashIcon.draw(c);
            }

            // "Xoá" label below icon — only show when enough space
            if (revealed >= dp(rv.getContext(), 48)) {
                c.drawText("Xoá", centerX, centerY + dp(rv.getContext(), 16), textPaint);
            }
        }
        // Do NOT call super — we handle translation manually
    }

    /**
     * Called when the finger is lifted and the swipe didn't reach the full threshold.
     * Decide: snap to peek position (show button) or snap back to 0.
     */
    @Override
    public void clearView(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
        if (!(vh instanceof TaskGroupAdapter.TaskViewHolder)) {
            super.clearView(rv, vh);
            return;
        }

        TaskGroupAdapter.TaskViewHolder tvh = (TaskGroupAdapter.TaskViewHolder) vh;
        View card = tvh.foregroundCard != null ? tvh.foregroundCard : vh.itemView;
        float currentTx = card.getTranslationX();

        // If swiped past 25% of peek width → snap to peek, else snap back
        boolean shouldPeek = currentTx < -(peekPx * 0.25f);

        if (shouldPeek) {
            // Close any previously peeked item first
            if (peekedHolder != null && peekedHolder != vh) {
                snapBack(peekedHolder);
            }
            peekedHolder = vh;
            snapTo(card, -peekPx, new OvershootInterpolator(1.2f));

            // Attach a one-time click listener on the revealed area to confirm delete
            attachDeleteClick(rv, vh, card);
        } else {
            if (peekedHolder == vh) peekedHolder = null;
            snapTo(card, 0f, new DecelerateInterpolator());
        }

        super.clearView(rv, vh);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Animate card to a target translationX */
    private void snapTo(View card, float targetTx, android.view.animation.Interpolator interp) {
        ValueAnimator anim = ValueAnimator.ofFloat(card.getTranslationX(), targetTx);
        anim.setDuration(220);
        anim.setInterpolator(interp);
        anim.addUpdateListener(a -> card.setTranslationX((float) a.getAnimatedValue()));
        anim.start();
    }

    /** Snap a ViewHolder's card back to 0 */
    private void snapBack(RecyclerView.ViewHolder holder) {
        if (!(holder instanceof TaskGroupAdapter.TaskViewHolder)) return;
        TaskGroupAdapter.TaskViewHolder tvh = (TaskGroupAdapter.TaskViewHolder) holder;
        View card = tvh.foregroundCard != null ? tvh.foregroundCard : holder.itemView;
        snapTo(card, 0f, new DecelerateInterpolator());
        // Remove any lingering click listener
        holder.itemView.setOnClickListener(null);
    }

    /**
     * When peeked: clicking anywhere on the item (which is now offset) triggers delete.
     * We use the itemView's click — the card is shifted left so the red area is exposed.
     * Tapping the red area (right side of itemView) confirms delete.
     */
    private void attachDeleteClick(RecyclerView rv, RecyclerView.ViewHolder vh, View card) {
        vh.itemView.setOnClickListener(v -> {
            // Only fire if still peeked
            if (card.getTranslationX() < -peekPx * 0.1f) {
                vh.itemView.setOnClickListener(null);
                peekedHolder = null;
                int pos = vh.getAdapterPosition();
                if (pos != RecyclerView.NO_ID) {
                    listener.onDelete(pos);
                }
            }
        });

        // Also close peek when user taps elsewhere (RecyclerView touch outside)
        rv.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView r, @NonNull android.view.MotionEvent e) {
                if (e.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    if (peekedHolder == vh) {
                        // Check if touch is outside the item
                        View item = vh.itemView;
                        if (e.getY() < item.getTop() || e.getY() > item.getBottom()) {
                            snapBack(vh);
                            peekedHolder = null;
                            vh.itemView.setOnClickListener(null);
                            r.removeOnItemTouchListener(this);
                        }
                    } else {
                        r.removeOnItemTouchListener(this);
                    }
                }
                return false;
            }
        });
    }

    private int dp(Context ctx, int dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }
}
