package hcmute.edu.vn.teeticktick;

public class Task {
    private int id; // ID từ database để có thể xóa
    private String title;
    private boolean isCompleted;
    private String emoji;

    public Task(String title) {
        this.title = title;
        this.isCompleted = false;
        this.emoji = "";
        this.id = 0; // 0 nghĩa là chưa lưu vào database
    }

    public Task(String emoji, String title) {
        this.emoji = emoji;
        this.title = title;
        this.isCompleted = false;
        this.id = 0;
    }
    
    public Task(int id, String emoji, String title, boolean isCompleted) {
        this.id = id;
        this.emoji = emoji;
        this.title = title;
        this.isCompleted = isCompleted;
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
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
}
