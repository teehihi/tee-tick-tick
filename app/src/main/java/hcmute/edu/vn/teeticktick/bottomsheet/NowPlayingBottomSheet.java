package hcmute.edu.vn.teeticktick.bottomsheet;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import hcmute.edu.vn.teeticktick.R;
import hcmute.edu.vn.teeticktick.model.Song;
import hcmute.edu.vn.teeticktick.service.MusicService;

public class NowPlayingBottomSheet extends BottomSheetDialogFragment {

    private MusicService musicService;
    private boolean isBound = false;

    private TextView titleView, artistView, currentTimeView, durationView;
    private SeekBar seekBar;
    private FrameLayout btnPlayPause;
    private ImageView icPlayPause;
    private ImageButton btnPrevious, btnNext, btnDismiss;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable seekUpdater;
    private boolean isUserSeeking = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
            musicService.setCallback(musicCallback);
            updateUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            musicService = null;
        }
    };

    private MusicService.MusicCallback musicCallback = new MusicService.MusicCallback() {
        @Override
        public void onSongChanged(Song song) {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> updateUI());
        }

        @Override
        public void onPlaybackStateChanged(boolean isPlaying) {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                updatePlayPauseIcon(isPlaying);
                if (isPlaying) startSeekUpdate(); else stopSeekUpdate();
            });
        }

        @Override
        public void onSongCompleted() {}
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.NowPlayingBottomSheetStyle);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_now_playing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        titleView = view.findViewById(R.id.now_playing_title);
        artistView = view.findViewById(R.id.now_playing_artist);
        currentTimeView = view.findViewById(R.id.now_playing_current);
        durationView = view.findViewById(R.id.now_playing_duration);
        seekBar = view.findViewById(R.id.now_playing_seekbar);
        btnPlayPause = view.findViewById(R.id.btn_play_pause_detail);
        icPlayPause = view.findViewById(R.id.ic_play_pause_detail);
        btnPrevious = view.findViewById(R.id.btn_previous);
        btnNext = view.findViewById(R.id.btn_next_detail);
        btnDismiss = view.findViewById(R.id.btn_dismiss);

        // Kích hoạt marquee scroll cho tên bài hát
        titleView.setSelected(true);

        btnDismiss.setOnClickListener(v -> dismiss());

        btnPlayPause.setOnClickListener(v -> {
            if (!isBound) return;
            if (musicService.isPlaying()) musicService.pause();
            else musicService.resume();
        });

        btnPrevious.setOnClickListener(v -> {
            if (isBound) musicService.playPrevious();
        });

        btnNext.setOnClickListener(v -> {
            if (isBound) musicService.playNext();
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isBound) {
                    int duration = musicService.getDuration();
                    int pos = (int) ((long) progress * duration / 100);
                    currentTimeView.setText(formatTime(pos));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { isUserSeeking = true; }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;
                if (isBound) {
                    int duration = musicService.getDuration();
                    int pos = (int) ((long) seekBar.getProgress() * duration / 100);
                    musicService.seekTo(pos);
                }
            }
        });

        // Bind service
        Intent intent = new Intent(requireContext(), MusicService.class);
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Expand to full screen
        if (getDialog() != null && getDialog().getWindow() != null) {
            android.view.Window window = getDialog().getWindow();

            // Draw under status bar to sync gradient
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false);
            } else {
                window.getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            }
            window.setStatusBarColor(android.graphics.Color.TRANSPARENT);

            View bottomSheet = window.findViewById(
                com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                // Full height
                bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                bottomSheet.requestLayout();
            }
        }
    }

    private void updateUI() {
        if (!isBound || musicService == null) return;
        Song song = musicService.getCurrentSong();
        if (song == null) return;

        titleView.setText(song.getTitle());
        artistView.setText(song.getArtist());
        durationView.setText(formatTime(musicService.getDuration()));
        updatePlayPauseIcon(musicService.isPlaying());
        if (musicService.isPlaying()) startSeekUpdate();
    }

    private void updatePlayPauseIcon(boolean isPlaying) {
        icPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause_fill : R.drawable.ic_play_fill);
        // Pause icon cần màu hồng để nổi bật trên nền trắng
        icPlayPause.setColorFilter(isPlaying ? 0xFFFF6467 : 0xFFFF6467);
    }

    private void startSeekUpdate() {
        seekUpdater = new Runnable() {
            @Override
            public void run() {
                if (isBound && musicService != null && musicService.isPlaying() && !isUserSeeking) {
                    int pos = musicService.getCurrentPosition();
                    int dur = musicService.getDuration();
                    if (dur > 0) {
                        seekBar.setProgress((int) ((long) pos * 100 / dur));
                        currentTimeView.setText(formatTime(pos));
                    }
                    handler.postDelayed(this, 500);
                }
            }
        };
        handler.post(seekUpdater);
    }

    private void stopSeekUpdate() {
        if (seekUpdater != null) handler.removeCallbacks(seekUpdater);
    }

    private String formatTime(int ms) {
        long s = ms / 1000;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopSeekUpdate();
        if (isBound) {
            musicService.setCallback(null);
            requireContext().unbindService(serviceConnection);
            isBound = false;
        }
    }
}
