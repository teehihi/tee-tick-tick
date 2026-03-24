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

    @Override
    public void onCreate() {
        super.onCreate();
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
