package hcmute.edu.vn.teeticktick.model;

public class SmartListItem {
    private String emoji;
    private int iconResId;
    private int iconColor;
    private String title;
    private String subtitle;
    private boolean isVisible;

    public SmartListItem(int iconResId, int iconColor, String title, String subtitle, boolean isVisible) {
        this.iconResId = iconResId;
        this.iconColor = iconColor;
        this.title = title;
        this.subtitle = subtitle;
        this.isVisible = isVisible;
    }

    public String getEmoji() {
        return emoji;
    }

    public int getIconResId() {
        return iconResId;
    }

    public int getIconColor() {
        return iconColor;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }
}
