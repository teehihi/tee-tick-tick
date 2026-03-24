package hcmute.edu.vn.teeticktick.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.teeticktick.database.AppDatabase;

/**
 * Content Provider cho phép ứng dụng bên ngoài truy cập dữ liệu tasks và categories.
 *
 * URI patterns:
 *   content://hcmute.edu.vn.teeticktick.provider/tasks       → tất cả tasks
 *   content://hcmute.edu.vn.teeticktick.provider/tasks/#      → task theo ID
 *   content://hcmute.edu.vn.teeticktick.provider/categories   → tất cả categories
 *   content://hcmute.edu.vn.teeticktick.provider/categories/# → category theo ID
 */
public class TaskContentProvider extends ContentProvider {

    private static final int TASKS = 100;
    private static final int TASK_ID = 101;
    private static final int CATEGORIES = 200;
    private static final int CATEGORY_ID = 201;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(TaskContract.AUTHORITY, "tasks", TASKS);
        sUriMatcher.addURI(TaskContract.AUTHORITY, "tasks/#", TASK_ID);
        sUriMatcher.addURI(TaskContract.AUTHORITY, "categories", CATEGORIES);
        sUriMatcher.addURI(TaskContract.AUTHORITY, "categories/#", CATEGORY_ID);
    }

    private AppDatabase database;

    @Override
    public boolean onCreate() {
        database = AppDatabase.getInstance(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection,
                        @Nullable String selection, @Nullable String[] selectionArgs,
                        @Nullable String sortOrder) {

        SupportSQLiteDatabase db = database.getOpenHelper().getReadableDatabase();

        String tableName;
        String defaultSort;

        switch (sUriMatcher.match(uri)) {
            case TASKS:
                tableName = "tasks";
                defaultSort = "createdAt DESC";
                break;
            case TASK_ID:
                tableName = "tasks";
                defaultSort = null;
                selection = "id = ?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                break;
            case CATEGORIES:
                tableName = "categories";
                defaultSort = "id ASC";
                break;
            case CATEGORY_ID:
                tableName = "categories";
                defaultSort = null;
                selection = "id = ?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        // Build SQL query
        StringBuilder sql = new StringBuilder("SELECT ");
        if (projection != null && projection.length > 0) {
            sql.append(String.join(", ", projection));
        } else {
            sql.append("*");
        }
        sql.append(" FROM ").append(tableName);

        List<Object> bindArgs = new ArrayList<>();
        if (selection != null) {
            sql.append(" WHERE ").append(selection);
            if (selectionArgs != null) {
                for (String arg : selectionArgs) {
                    bindArgs.add(arg);
                }
            }
        }

        String sort = sortOrder != null ? sortOrder : defaultSort;
        if (sort != null) {
            sql.append(" ORDER BY ").append(sort);
        }

        SimpleSQLiteQuery query = new SimpleSQLiteQuery(sql.toString(), bindArgs.toArray());
        Cursor cursor = db.query(query);

        if (getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        SupportSQLiteDatabase db = database.getOpenHelper().getWritableDatabase();
        long id;
        String tableName;

        switch (sUriMatcher.match(uri)) {
            case TASKS:
                tableName = "tasks";
                if (values != null && !values.containsKey("createdAt")) {
                    values.put("createdAt", System.currentTimeMillis());
                }
                break;
            case CATEGORIES:
                tableName = "categories";
                break;
            default:
                throw new IllegalArgumentException("Insert not supported for URI: " + uri);
        }

        id = db.insert(tableName, 0, values);

        if (id > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
            return ContentUris.withAppendedId(uri, id);
        }
        return null;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values,
                      @Nullable String selection, @Nullable String[] selectionArgs) {
        SupportSQLiteDatabase db = database.getOpenHelper().getWritableDatabase();
        int rowsUpdated;
        String tableName;

        switch (sUriMatcher.match(uri)) {
            case TASKS:
                tableName = "tasks";
                break;
            case TASK_ID:
                tableName = "tasks";
                selection = "id = ?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                break;
            case CATEGORIES:
                tableName = "categories";
                break;
            case CATEGORY_ID:
                tableName = "categories";
                selection = "id = ?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                break;
            default:
                throw new IllegalArgumentException("Update not supported for URI: " + uri);
        }

        rowsUpdated = db.update(tableName, 0, values, selection, selectionArgs);

        if (rowsUpdated > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        SupportSQLiteDatabase db = database.getOpenHelper().getWritableDatabase();
        int rowsDeleted;
        String tableName;

        switch (sUriMatcher.match(uri)) {
            case TASKS:
                tableName = "tasks";
                break;
            case TASK_ID:
                tableName = "tasks";
                selection = "id = ?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                break;
            case CATEGORIES:
                tableName = "categories";
                break;
            case CATEGORY_ID:
                tableName = "categories";
                selection = "id = ?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                break;
            default:
                throw new IllegalArgumentException("Delete not supported for URI: " + uri);
        }

        rowsDeleted = db.delete(tableName, selection, selectionArgs);

        if (rowsDeleted > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case TASKS:
                return TaskContract.TaskEntry.CONTENT_TYPE;
            case TASK_ID:
                return TaskContract.TaskEntry.CONTENT_ITEM_TYPE;
            case CATEGORIES:
                return TaskContract.CategoryEntry.CONTENT_TYPE;
            case CATEGORY_ID:
                return TaskContract.CategoryEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }
}
