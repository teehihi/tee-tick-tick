package hcmute.edu.vn.teeticktick.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class CategoryEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String name;
    private String emoji;
    private String systemId; // To identify default categories like Inbox, Work

    public CategoryEntity(String name, String emoji, String systemId) {
        this.name = name;
        this.emoji = emoji;
        this.systemId = systemId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
    
    public String getSystemId() { return systemId; }
    public void setSystemId(String systemId) { this.systemId = systemId; }
}
