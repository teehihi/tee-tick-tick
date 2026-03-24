package hcmute.edu.vn.teeticktick.model;

import android.net.Uri;

/**
 * Model class đại diện cho một bài hát đọc từ MediaStore.
 */
public class Song {
    private long id;
    private String title;
    private String artist;
    private long duration; // milliseconds
    private Uri uri;
    private Uri albumArtUri;

    public Song(long id, String title, String artist, long duration, Uri uri, Uri albumArtUri) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.uri = uri;
        this.albumArtUri = albumArtUri;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public long getDuration() { return duration; }
    public Uri getUri() { return uri; }
    public Uri getAlbumArtUri() { return albumArtUri; }

    /**
     * Format duration từ milliseconds sang mm:ss
     */
    public String getFormattedDuration() {
        long totalSeconds = duration / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
