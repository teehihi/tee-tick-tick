package hcmute.edu.vn.teeticktick;

public class Task {
    private String title;
    private boolean isCompleted;
    private String emoji;

    public Task(String title) {
        this.title = title;
        this.isCompleted = false;
        this.emoji = "";
    }

    public Task(String emoji, String title) {
        this.emoji = emoji;
        this.title = title;
        this.isCompleted = false;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }
}
