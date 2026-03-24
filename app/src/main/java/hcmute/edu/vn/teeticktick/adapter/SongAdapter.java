package hcmute.edu.vn.teeticktick.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.teeticktick.R;
import hcmute.edu.vn.teeticktick.model.Song;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private List<Song> songs = new ArrayList<>();
    private int currentPlayingIndex = -1;
    private OnSongClickListener listener;

    public interface OnSongClickListener {
        void onSongClick(int position);
    }

    public SongAdapter(OnSongClickListener listener) {
        this.listener = listener;
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs;
        notifyDataSetChanged();
    }

    public void setCurrentPlaying(int index) {
        int oldIndex = currentPlayingIndex;
        currentPlayingIndex = index;
        if (oldIndex >= 0) notifyItemChanged(oldIndex);
        if (index >= 0) notifyItemChanged(index);
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.title.setText(song.getTitle());
        holder.artist.setText(song.getArtist());
        holder.duration.setText(song.getFormattedDuration());

        // Highlight bài đang phát
        boolean isPlaying = position == currentPlayingIndex;
        int highlightColor = isPlaying ? 0xFF4CAF50 : 0xFF333333; // green : dark
        holder.title.setTextColor(highlightColor);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSongClick(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView title, artist, duration;
        ImageView icon;

        SongViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.song_title);
            artist = itemView.findViewById(R.id.song_artist);
            duration = itemView.findViewById(R.id.song_duration);
            icon = itemView.findViewById(R.id.song_icon);
        }
    }
}
