package hcmute.edu.vn.teeticktick.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.teeticktick.R;
import hcmute.edu.vn.teeticktick.model.Song;
import hcmute.edu.vn.teeticktick.receiver.MusicPlayerReceiver;

/**
 * Bound Service phát nhạc nền sử dụng MediaPlayer.
 * Activity/Fragment bind vào service này để điều khiển play/pause/stop.
 *
 * Tích hợp BroadcastReceiver:
 *   - Nhận commands từ MusicPlayerReceiver qua onStartCommand
 *   - Phát broadcast ACTION_SONG_CHANGED khi bài hát thay đổi
 *   - Media-style notification với các nút: Previous, Play/Pause, Next, Close
 */
public class MusicService extends Service {

    private static final String TAG = "MusicService";

    // Broadcast action gửi đi khi bài hát thay đổi
    public static final String ACTION_SONG_CHANGED = "hcmute.edu.vn.teeticktick.ACTION_SONG_CHANGED";
    public static final String EXTRA_SONG_TITLE = "extra_song_title";
    public static final String EXTRA_SONG_ARTIST = "extra_song_artist";
    public static final String EXTRA_IS_PLAYING = "extra_is_playing";

    private final IBinder binder = new MusicBinder();
    private MediaPlayer mediaPlayer;
    private List<Song> songList = new ArrayList<>();
    private int currentSongIndex = -1;
    private boolean isPrepared = false;

    // Callback interface
    public interface MusicCallback {
        void onSongChanged(Song song);
        void onPlaybackStateChanged(boolean isPlaying);
        void onSongCompleted();
    }

    private MusicCallback callback;

    /**
     * Binder class cho phép Activity/Fragment lấy reference đến service.
     */
    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    private static final int NOTIFICATION_ID = 202;
    private static final String CHANNEL_ID = "MusicPlayerChannel";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        // Xử lý action từ BroadcastReceiver (notification buttons)
        if (intent != null && intent.getAction() != null) {
            handleReceiverAction(intent.getAction());
            return START_NOT_STICKY;
        }

        Notification notification = createMediaNotification("TeeTickTick Music", "", false);
        startForeground(NOTIFICATION_ID, notification);
        return START_NOT_STICKY;
    }

    /**
     * Xử lý các action nhận từ MusicPlayerReceiver.
     */
    private void handleReceiverAction(String action) {
        Log.d(TAG, "handleReceiverAction: " + action);

        switch (action) {
            case MusicPlayerReceiver.ACTION_PLAY_PAUSE:
                if (isPlaying()) {
                    pause();
                } else {
                    resume();
                }
                break;

            case MusicPlayerReceiver.ACTION_NEXT:
                playNext();
                break;

            case MusicPlayerReceiver.ACTION_PREVIOUS:
                playPrevious();
                break;

            case MusicPlayerReceiver.ACTION_CLOSE:
                stop();
                stopForeground(true);
                stopSelf();
                break;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Player",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Điều khiển trình phát nhạc");
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Tạo PendingIntent cho notification action → gửi broadcast tới MusicPlayerReceiver.
     */
    private PendingIntent createActionPendingIntent(String action) {
        Intent intent = new Intent(this, MusicPlayerReceiver.class);
        intent.setAction(action);
        int requestCode;
        switch (action) {
            case MusicPlayerReceiver.ACTION_PREVIOUS:  requestCode = 1; break;
            case MusicPlayerReceiver.ACTION_PLAY_PAUSE: requestCode = 2; break;
            case MusicPlayerReceiver.ACTION_NEXT:      requestCode = 3; break;
            case MusicPlayerReceiver.ACTION_CLOSE:     requestCode = 4; break;
            default: requestCode = 0;
        }
        return PendingIntent.getBroadcast(this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Tạo media-style notification với các nút điều khiển:
     * [◀ Previous] [▶ Play / ❚❚ Pause] [▶▶ Next] [✕ Close]
     */
    private Notification createMediaNotification(String title, String artist, boolean isPlaying) {
        // PendingIntents cho các nút
        PendingIntent prevIntent = createActionPendingIntent(MusicPlayerReceiver.ACTION_PREVIOUS);
        PendingIntent playPauseIntent = createActionPendingIntent(MusicPlayerReceiver.ACTION_PLAY_PAUSE);
        PendingIntent nextIntent = createActionPendingIntent(MusicPlayerReceiver.ACTION_NEXT);
        PendingIntent closeIntent = createActionPendingIntent(MusicPlayerReceiver.ACTION_CLOSE);

        // Icon cho play/pause dựa trên trạng thái
        int playPauseIcon = isPlaying ? R.drawable.ic_pause_fill : R.drawable.ic_play_fill;
        String playPauseText = isPlaying ? "Pause" : "Play";

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(artist)
                .setSmallIcon(R.drawable.ic_ios_music)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(isPlaying)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                // Các nút điều khiển
                .addAction(R.drawable.ic_skip_previous_fill, "Previous", prevIntent)
                .addAction(playPauseIcon, playPauseText, playPauseIntent)
                .addAction(R.drawable.ic_skip_next_fill, "Next", nextIntent)
                .addAction(R.drawable.ic_ios_x_circle, "Close", closeIntent)
                // Media style: hiển thị 3 nút chính (prev, play/pause, next) ở compact view
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2))
                .build();
    }

    /**
     * Cập nhật notification với thông tin bài hát hiện tại.
     */
    private void updateMediaNotification() {
        Song song = getCurrentSong();
        String title = song != null ? song.getTitle() : "TeeTickTick Music";
        String artist = song != null ? song.getArtist() : "";

        Notification notification = createMediaNotification(title, artist, isPlaying());
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    /**
     * Phát broadcast thông tin bài hát đang phát.
     * App khác hoặc widget có thể đăng ký nhận broadcast này.
     */
    private void sendSongChangedBroadcast() {
        Song song = getCurrentSong();
        if (song == null) return;

        Intent broadcastIntent = new Intent(ACTION_SONG_CHANGED);
        broadcastIntent.putExtra(EXTRA_SONG_TITLE, song.getTitle());
        broadcastIntent.putExtra(EXTRA_SONG_ARTIST, song.getArtist());
        broadcastIntent.putExtra(EXTRA_IS_PLAYING, isPlaying());
        broadcastIntent.setPackage(getPackageName()); // Giới hạn trong app

        sendBroadcast(broadcastIntent);
        Log.d(TAG, "Broadcast sent: " + song.getTitle() + " | playing=" + isPlaying());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Start foreground immediately to avoid ANR when startForegroundService is called
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createMediaNotification("TeeTickTick Music", "", false));

        mediaPlayer = new MediaPlayer();

        mediaPlayer.setOnCompletionListener(mp -> {
            if (callback != null) {
                callback.onSongCompleted();
            }
            // Auto play next song
            playNext();
        });

        mediaPlayer.setOnPreparedListener(mp -> {
            isPrepared = true;
            mp.start();
            if (callback != null) {
                callback.onPlaybackStateChanged(true);
            }
            // Cập nhật notification và phát broadcast khi bắt đầu phát
            updateMediaNotification();
            sendSongChangedBroadcast();
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
            isPrepared = false;
            return true;
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    // ---- Public methods cho Fragment điều khiển ----

    public void setCallback(MusicCallback callback) {
        this.callback = callback;
    }

    public void setSongList(List<Song> songs) {
        this.songList = songs;
    }

    public void play(int songIndex) {
        if (songIndex < 0 || songIndex >= songList.size()) return;

        currentSongIndex = songIndex;
        Song song = songList.get(currentSongIndex);

        try {
            mediaPlayer.reset();
            isPrepared = false;
            mediaPlayer.setDataSource(this, song.getUri());
            mediaPlayer.prepareAsync();

            // Cập nhật notification ngay khi bắt đầu load bài
            updateMediaNotification();

            if (callback != null) {
                callback.onSongChanged(song);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error playing song: " + song.getTitle(), e);
        }
    }

    public void pause() {
        if (mediaPlayer != null && isPrepared && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            if (callback != null) {
                callback.onPlaybackStateChanged(false);
            }
            // Cập nhật notification: đổi icon sang Play
            updateMediaNotification();
            sendSongChangedBroadcast();
        }
    }

    public void resume() {
        if (mediaPlayer != null && isPrepared && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            if (callback != null) {
                callback.onPlaybackStateChanged(true);
            }
            // Cập nhật notification: đổi icon sang Pause
            updateMediaNotification();
            sendSongChangedBroadcast();
        }
    }

    public void playNext() {
        if (songList.isEmpty()) return;
        int nextIndex = (currentSongIndex + 1) % songList.size();
        play(nextIndex);
    }

    public void playPrevious() {
        if (songList.isEmpty()) return;
        int prevIndex = currentSongIndex - 1;
        if (prevIndex < 0) prevIndex = songList.size() - 1;
        play(prevIndex);
    }

    public void stop() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            isPrepared = false;
            if (callback != null) {
                callback.onPlaybackStateChanged(false);
            }
            updateMediaNotification();
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && isPrepared && mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null && isPrepared) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public int getDuration() {
        if (mediaPlayer != null && isPrepared) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    public void seekTo(int position) {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.seekTo(position);
        }
    }

    public Song getCurrentSong() {
        if (currentSongIndex >= 0 && currentSongIndex < songList.size()) {
            return songList.get(currentSongIndex);
        }
        return null;
    }

    public int getCurrentSongIndex() {
        return currentSongIndex;
    }
}
