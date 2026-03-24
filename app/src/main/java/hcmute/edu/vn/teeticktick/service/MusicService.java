package hcmute.edu.vn.teeticktick.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import android.content.Context;
import hcmute.edu.vn.teeticktick.R;

import hcmute.edu.vn.teeticktick.model.Song;

/**
 * Bound Service phát nhạc nền sử dụng MediaPlayer.
 * Activity/Fragment bind vào service này để điều khiển play/pause/stop.
 */
public class MusicService extends Service {

    private static final String TAG = "MusicService";

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
        Notification notification = createNotification("TeeTickTick Music");
        startForeground(NOTIFICATION_ID, notification);
        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Player",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String title) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Đang phát nhạc")
                .setContentText(title)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification(String title) {
        Notification notification = createNotification(title);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Start foreground immediately to avoid ANR when startForegroundService is called
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification("TeeTickTick Music"));

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

            updateNotification(song.getTitle());

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
        }
    }

    public void resume() {
        if (mediaPlayer != null && isPrepared && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            if (callback != null) {
                callback.onPlaybackStateChanged(true);
            }
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
