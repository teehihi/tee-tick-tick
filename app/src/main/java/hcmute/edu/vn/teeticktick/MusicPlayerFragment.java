package hcmute.edu.vn.teeticktick;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.teeticktick.adapter.SongAdapter;
import hcmute.edu.vn.teeticktick.model.Song;
import hcmute.edu.vn.teeticktick.service.MusicService;

/**
 * Fragment hiển thị danh sách nhạc từ MediaStore (Content Provider hệ thống)
 * và điều khiển phát nhạc qua MusicService (Bound Service).
 */
public class MusicPlayerFragment extends Fragment implements SongAdapter.OnSongClickListener {

    private static final int PERMISSION_REQUEST_CODE = 2001;

    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    private List<Song> songList = new ArrayList<>();

    // Player controls
    private LinearLayout playerControls;
    private LinearLayout emptyState;
    private TextView emptyText;
    private View btnGrantPermission;
    private TextView playerSongTitle, playerSongArtist;
    private TextView playerCurrentTime, playerTotalTime;
    private ImageButton btnPlayPause, btnPrevious, btnNext;
    private SeekBar playerSeekbar;

    // Bound Service
    private MusicService musicService;
    private boolean isBound = false;

    // SeekBar updater
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable seekBarUpdater;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;

            // Set callback
            musicService.setCallback(musicCallback);

            // Nếu service đang phát nhạc, khôi phục trạng thái UI
            if (musicService.getCurrentSong() != null) {
                updatePlayerUI(musicService.getCurrentSong());
                playerControls.setVisibility(View.VISIBLE);
                songAdapter.setCurrentPlaying(musicService.getCurrentSongIndex());
                updatePlayPauseButton(musicService.isPlaying());
                if (musicService.isPlaying()) {
                    startSeekBarUpdate();
                }
            }
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
            getActivity().runOnUiThread(() -> {
                updatePlayerUI(song);
                playerControls.setVisibility(View.VISIBLE);
                songAdapter.setCurrentPlaying(musicService.getCurrentSongIndex());
            });
        }

        @Override
        public void onPlaybackStateChanged(boolean isPlaying) {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                updatePlayPauseButton(isPlaying);
                if (isPlaying) {
                    startSeekBarUpdate();
                } else {
                    stopSeekBarUpdate();
                }
            });
        }

        @Override
        public void onSongCompleted() {
            // MusicService tự động chuyển bài
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_music_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Update toolbar
        if (getActivity() != null) {
            ((MainActivity) getActivity()).getSupportActionBar().setTitle(R.string.nav_music);
        }

        initViews(view);
        setupRecyclerView();
        setupPlayerControls();

        // Check permission và load nhạc
        if (hasAudioPermission()) {
            loadSongsFromMediaStore();
        } else {
            showPermissionRequired();
        }

        // Bind to MusicService
        Intent intent = new Intent(requireContext(), MusicService.class);
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.songs_recycler_view);
        playerControls = view.findViewById(R.id.player_controls);
        emptyState = view.findViewById(R.id.empty_state);
        emptyText = view.findViewById(R.id.empty_text);
        btnGrantPermission = view.findViewById(R.id.btn_grant_permission);

        playerSongTitle = view.findViewById(R.id.player_song_title);
        playerSongArtist = view.findViewById(R.id.player_song_artist);
        playerCurrentTime = view.findViewById(R.id.player_current_time);
        playerTotalTime = view.findViewById(R.id.player_total_time);
        btnPlayPause = view.findViewById(R.id.btn_play_pause);
        btnPrevious = view.findViewById(R.id.btn_previous);
        btnNext = view.findViewById(R.id.btn_next);
        playerSeekbar = view.findViewById(R.id.player_seekbar);
    }

    private void setupRecyclerView() {
        songAdapter = new SongAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(songAdapter);
    }

    private void setupPlayerControls() {
        btnPlayPause.setOnClickListener(v -> {
            if (!isBound) return;
            if (musicService.isPlaying()) {
                musicService.pause();
            } else {
                musicService.resume();
            }
        });

        btnPrevious.setOnClickListener(v -> {
            if (isBound) musicService.playPrevious();
        });

        btnNext.setOnClickListener(v -> {
            if (isBound) musicService.playNext();
        });

        playerSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isBound) {
                    int duration = musicService.getDuration();
                    int newPosition = (int) ((long) progress * duration / 100);
                    musicService.seekTo(newPosition);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnGrantPermission.setOnClickListener(v -> requestAudioPermission());
    }

    // ---- MediaStore Content Provider - đọc nhạc từ thiết bị ----

    private void loadSongsFromMediaStore() {
        songList.clear();

        Uri audioUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID
        };

        // Chỉ lấy file nhạc (không phải ringtone, alarm, etc.)
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        Cursor cursor = requireContext().getContentResolver().query(
                audioUri, projection, selection, null, sortOrder);

        if (cursor != null) {
            int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idCol);
                String title = cursor.getString(titleCol);
                String artist = cursor.getString(artistCol);
                long duration = cursor.getLong(durationCol);
                long albumId = cursor.getLong(albumIdCol);

                // Build URI cho bài hát
                Uri songUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

                // Build URI cho album art
                Uri albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"), albumId);

                // Bỏ qua bài ngắn hơn 15 giây (thường là notification sounds)
                if (duration > 15000) {
                    songList.add(new Song(id, title, artist, duration, songUri, albumArtUri));
                }
            }
            cursor.close();
        }

        if (songList.isEmpty()) {
            showEmptyState("Không tìm thấy bài hát trên thiết bị");
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            songAdapter.setSongs(songList);

            // Cập nhật MusicService
            if (isBound) {
                musicService.setSongList(songList);
            }
        }
    }

    // ---- Permission handling ----

    private boolean hasAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_AUDIO},
                    PERMISSION_REQUEST_CODE);
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void showPermissionRequired() {
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        emptyText.setText("Cần quyền truy cập để đọc nhạc từ thiết bị");
        btnGrantPermission.setVisibility(View.VISIBLE);
    }

    private void showEmptyState(String message) {
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        emptyText.setText(message);
        btnGrantPermission.setVisibility(View.GONE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadSongsFromMediaStore();
            } else {
                showPermissionRequired();
            }
        }
    }

    // ---- SongAdapter callback ----

    @Override
    public void onSongClick(int position) {
        if (!isBound) return;
        musicService.setSongList(songList);
        musicService.play(position);
    }

    // ---- UI helpers ----

    private void updatePlayerUI(Song song) {
        playerSongTitle.setText(song.getTitle());
        playerSongArtist.setText(song.getArtist());
        playerTotalTime.setText(song.getFormattedDuration());
        playerCurrentTime.setText("0:00");
        playerSeekbar.setProgress(0);
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        btnPlayPause.setImageResource(
                isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
    }

    private void startSeekBarUpdate() {
        seekBarUpdater = new Runnable() {
            @Override
            public void run() {
                if (isBound && musicService.isPlaying()) {
                    int currentPosition = musicService.getCurrentPosition();
                    int duration = musicService.getDuration();
                    if (duration > 0) {
                        int progress = (int) ((long) currentPosition * 100 / duration);
                        playerSeekbar.setProgress(progress);
                        playerCurrentTime.setText(formatTime(currentPosition));
                    }
                    handler.postDelayed(this, 500);
                }
            }
        };
        handler.post(seekBarUpdater);
    }

    private void stopSeekBarUpdate() {
        if (seekBarUpdater != null) {
            handler.removeCallbacks(seekBarUpdater);
        }
    }

    private String formatTime(int milliseconds) {
        long totalSeconds = milliseconds / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    // ---- Lifecycle ----

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopSeekBarUpdate();
        if (isBound) {
            musicService.setCallback(null);
            requireContext().unbindService(serviceConnection);
            isBound = false;
        }
    }
}
