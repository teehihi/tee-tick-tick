package hcmute.edu.vn.teeticktick.model;

public class TaskList {
    private String emoji;
    private String key;  // Key for database (always English)
    private String displayName;  // Display name (localized)

    public TaskList(String emoji, String key, String displayName) {
        this.emoji = emoji;
        this.key = key;
        this.displayName = displayName;
    }

    // Backward compatibility constructor
    public TaskList(String emoji, String name) {
        this.emoji = emoji;
        this.key = name;
        this.displayName = name;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
