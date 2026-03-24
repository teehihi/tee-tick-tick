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
import android.widget.TextView;
import android.util.Log;
import android.media.MediaScannerConnection;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.Executors;
import android.provider.OpenableColumns;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

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
    private View playerControls;
    private LinearLayout emptyState;
    private TextView emptyText;
    private View btnGrantPermission;
    private View btnRescan;
    private TextView playerSongTitle, playerSongArtist;
    private ImageButton btnPlayPause, btnNext;
    private android.widget.ProgressBar playerSeekbar;

    // Bound Service
    private MusicService musicService;
    private boolean isBound = false;

    // SeekBar updater
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable seekBarUpdater;

    private final ActivityResultLauncher<String> pickAudioLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                if (uris != null && !uris.isEmpty()) {
                    importMultipleAudioFromUris(uris);
                }
            }
    );

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

        // Update toolbar & hide FAB only
        if (getActivity() != null) {
            androidx.appcompat.app.ActionBar ab = ((MainActivity) getActivity()).getSupportActionBar();
            if (ab != null) ab.setTitle(R.string.nav_music);
            View fab = getActivity().findViewById(R.id.fab);
            if (fab != null) fab.setVisibility(View.GONE);
        }

        initViews(view);
        setupRecyclerView();
        setupPlayerControls();

        // Đẩy mini player lên trên bottom nav
        View bottomNav = requireActivity().findViewById(R.id.bottom_nav);
        bottomNav.post(() -> {
            int navHeight = bottomNav.getHeight();
            if (navHeight > 0) {
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams lp =
                    (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) playerControls.getLayoutParams();
                lp.bottomMargin = navHeight / 4;
                playerControls.setLayoutParams(lp);
            }
        });

        // Check permission và load nhạc
        if (hasAudioPermission()) {
            loadSongsFromMediaStore();
        } else {
            showPermissionRequired();
        }

        // Bind to MusicService
        Intent intent = new Intent(requireContext(), MusicService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent);
        } else {
            requireContext().startService(intent);
        }
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.songs_recycler_view);
        playerControls = view.findViewById(R.id.player_controls);
        emptyState = view.findViewById(R.id.empty_state);
        emptyText = view.findViewById(R.id.empty_text);
        btnGrantPermission = view.findViewById(R.id.btn_grant_permission);
        btnRescan = view.findViewById(R.id.btn_rescan);

        playerSongTitle = view.findViewById(R.id.player_song_title);
        playerSongArtist = view.findViewById(R.id.player_song_artist);
        btnPlayPause = view.findViewById(R.id.btn_play_pause);
        btnNext = view.findViewById(R.id.btn_next);
        playerSeekbar = view.findViewById(R.id.player_seekbar);
        
        View btnAddMusic = view.findViewById(R.id.btn_add_music);
        if (btnAddMusic != null) {
            btnAddMusic.setOnClickListener(v -> pickAudioLauncher.launch("audio/*"));
        }

        // Bấm vào mini player → mở NowPlaying full screen
        playerControls.setOnClickListener(v -> {
            hcmute.edu.vn.teeticktick.bottomsheet.NowPlayingBottomSheet sheet =
                new hcmute.edu.vn.teeticktick.bottomsheet.NowPlayingBottomSheet();
            sheet.show(getParentFragmentManager(), "NowPlaying");
        });
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

        btnNext.setOnClickListener(v -> {
            if (isBound) musicService.playNext();
        });

        btnGrantPermission.setOnClickListener(v -> requestAudioPermission());
        
        btnRescan.setOnClickListener(v -> scanMediaFiles());
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

        // Lấy nhạc và các file ghi âm (loại bỏ nhạc chuông, thông báo, báo thức)
        String selection = MediaStore.Audio.Media.IS_ALARM + " == 0 AND " +
                           MediaStore.Audio.Media.IS_NOTIFICATION + " == 0 AND " +
                           MediaStore.Audio.Media.IS_RINGTONE + " == 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        Cursor cursor = requireContext().getContentResolver().query(
                audioUri, projection, selection, null, sortOrder);

        if (cursor != null) {
            Log.d("MusicPlayer", "MediaStore query trả về " + cursor.getCount() + " bài hát");
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
        } else {
            Log.d("MusicPlayer", "MediaStore query trả về chuỗi cursor null");
        }

        // Lấy nhạc đã import thủ công vào app
        File dir = new File(requireContext().getFilesDir(), "imported_music");
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    try {
                        android.media.MediaMetadataRetriever mmr = new android.media.MediaMetadataRetriever();
                        mmr.setDataSource(f.getAbsolutePath());
                        String title = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE);
                        if (title == null) title = f.getName();
                        String artist = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST);
                        if (artist == null) artist = "Unknown Artist";
                        String durationStr = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
                        long duration = durationStr != null ? Long.parseLong(durationStr) : 0;
                        
                        songList.add(new Song(f.lastModified(), title, artist, duration, Uri.fromFile(f), null));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        // Sắp xếp lại từ A-Z theo tiêu đề bài hát (tùy chọn, để mix cả MediaStore và file import)
        songList.sort((s1, s2) -> s1.getTitle().compareToIgnoreCase(s2.getTitle()));

        if (songList.isEmpty()) {
            showEmptyState("Không tìm thấy bài hát trên thiết bị.\nHãy copy nhạc và bấm Quét lại.", true);
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
        btnRescan.setVisibility(View.GONE);
    }

    private void showEmptyState(String message, boolean showRescan) {
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        emptyText.setText(message);
        btnGrantPermission.setVisibility(View.GONE);
        btnRescan.setVisibility(showRescan ? View.VISIBLE : View.GONE);
    }
    
    private void scanMediaFiles() {
        btnRescan.setEnabled(false);
        if (getActivity() != null) {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                String dirPath = Environment.getExternalStorageDirectory().toString();
                MediaScannerConnection.scanFile(getActivity().getApplicationContext(),
                        new String[]{dirPath}, null,
                        (path, uri) -> {
                            Log.i("MusicPlayer", "Quét xong: " + path);
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    btnRescan.setEnabled(true);
                                    loadSongsFromMediaStore();
                                });
                            }
                        });
            } else {
                btnRescan.setEnabled(true);
            }
        }
    }

    private void importMultipleAudioFromUris(List<Uri> uris) {
        Executors.newSingleThreadExecutor().execute(() -> {
            boolean importedAny = false;
            for (Uri uri : uris) {
                try {
                    Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null);
                    String displayName = "Imported Audio.mp3";
                    if (cursor != null && cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (nameIndex != -1) displayName = cursor.getString(nameIndex);
                        cursor.close();
                    }
                    
                    File dir = new File(requireContext().getFilesDir(), "imported_music");
                    if (!dir.exists()) dir.mkdirs();
                    
                    File outFile = new File(dir, java.util.UUID.randomUUID().toString() + "_" + displayName);
                    InputStream in = requireContext().getContentResolver().openInputStream(uri);
                    if (in == null) continue;
                    
                    FileOutputStream out = new FileOutputStream(outFile);
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    in.close();
                    out.close();
                    importedAny = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (importedAny && getActivity() != null) {
                getActivity().runOnUiThread(() -> loadSongsFromMediaStore());
            }
        });
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
    
    @Override
    public void onSongDeleteClick(int position) {
        if (position < 0 || position >= songList.size()) return;
        Song song = songList.get(position);
        
        // Remove locally if imported manually
        String path = song.getUri().getPath();
        if (path != null && path.contains("imported_music")) {
            File f = new File(path);
            if (f.exists()) {
                f.delete();
            }
        }
        
        songList.remove(position);
        songAdapter.notifyItemRemoved(position);
        
        // Cập nhật lại playlist cho service nếu đang phát
        if (isBound) {
            musicService.setSongList(songList);
            // Nếu bài bị xóa là bài đang phát, dừng hoặc chuyển
            if (musicService.getCurrentSongIndex() == position) {
                musicService.stop();
                playerControls.setVisibility(View.GONE);
            } else if (musicService.getCurrentSongIndex() > position) {
                // Adjust index internally in a basic way, or just let user click again
                // Not perfectly seamless without modifying MusicService, but okay for now
            }
        }

        if (songList.isEmpty()) {
            showEmptyState("Danh sách trống.\nHãy Thêm nhạc hoặc Quét lại.", false);
        }
    }

    // ---- UI helpers ----

    private void updatePlayerUI(Song song) {
        playerSongTitle.setText(song.getTitle());
        playerSongArtist.setText(song.getArtist());
        playerSeekbar.setProgress(0);
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        btnPlayPause.setImageResource(
                isPlaying ? R.drawable.ic_pause_fill : R.drawable.ic_play_fill);
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
        // Restore FAB when leaving music screen
        if (getActivity() != null) {
            View fab = getActivity().findViewById(R.id.fab);
            if (fab != null) fab.setVisibility(View.VISIBLE);
        }
    }
}
