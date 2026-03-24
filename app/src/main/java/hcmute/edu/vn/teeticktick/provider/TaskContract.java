package hcmute.edu.vn.teeticktick.provider;

import android.net.Uri;

/**
 * Contract class chứa các hằng số cho TaskContentProvider.
 * Các app bên ngoài dùng class này để truy cập đúng URI và column name.
 */
public final class TaskContract {

    private TaskContract() {} // Không cho khởi tạo

    public static final String AUTHORITY = "hcmute.edu.vn.teeticktick.provider";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    // ---- Tasks ----
    public static final class TaskEntry {
        public static final String TABLE_NAME = "tasks";
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(TABLE_NAME).build();

        // MIME types
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd." + AUTHORITY + "." + TABLE_NAME;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd." + AUTHORITY + "." + TABLE_NAME;

        // Column names
        public static final String _ID = "id";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_EMOJI = "emoji";
        public static final String COLUMN_IS_COMPLETED = "isCompleted";
        public static final String COLUMN_LIST_NAME = "listName";
        public static final String COLUMN_PRIORITY = "priority";
        public static final String COLUMN_CREATED_AT = "createdAt";
        public static final String COLUMN_START_DATE = "startDate";
        public static final String COLUMN_DUE_DATE = "dueDate";
    }

    // ---- Categories ----
    public static final class CategoryEntry {
        public static final String TABLE_NAME = "categories";
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(TABLE_NAME).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd." + AUTHORITY + "." + TABLE_NAME;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd." + AUTHORITY + "." + TABLE_NAME;

        public static final String _ID = "id";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_EMOJI = "emoji";
        public static final String COLUMN_SYSTEM_ID = "systemId";
        public static final String COLUMN_NOTIFICATION_SOUND = "notificationSound";
    }
}
