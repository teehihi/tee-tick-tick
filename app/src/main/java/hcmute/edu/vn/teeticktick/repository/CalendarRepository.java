package hcmute.edu.vn.teeticktick.repository;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;

import hcmute.edu.vn.teeticktick.database.TaskEntity;

/**
 * CalendarRepository — tích hợp trực tiếp với app Lịch (Calendar) trên Android.
 */
public class CalendarRepository {

    private static final String TAG = "CalendarRepo";
    private static final String EVENT_SOURCE_APP = "TeeTickTick";

    private final ContentResolver contentResolver;
    private final Context context;

    public CalendarRepository(Context context) {
        this.context = context.getApplicationContext();
        this.contentResolver = this.context.getContentResolver();
    }

    // -------------------------------------------------------------------------
    // Kiểm tra quyền
    // -------------------------------------------------------------------------

    public boolean hasCalendarPermission() {
        boolean read = context.checkSelfPermission(Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
        boolean write = context.checkSelfPermission(Manifest.permission.WRITE_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
        Log.d(TAG, "Permission check — READ=" + read + " WRITE=" + write);
        return read && write;
    }

    // -------------------------------------------------------------------------
    // 1. Tìm calendarId — query TẤT CẢ calendars, không filter
    // -------------------------------------------------------------------------

    public long getDefaultCalendarId() {
        if (!hasCalendarPermission()) {
            Log.e(TAG, "❌ NO CALENDAR PERMISSION");
            return -1;
        }

        long calId = queryForCalendarId();
        if (calId == -1) {
            Log.d(TAG, "❌ No calendar found. Attempting to create a local calendar.");
            calId = createLocalCalendar();
        } else {
            // Đảm bảo lịch tìm thấy hoặc vừa tạo được hiển thị
            ensureCalendarVisible(calId);
        }
        return calId;
    }

    private void ensureCalendarVisible(long calId) {
        try {
            ContentValues values = new ContentValues();
            values.put(CalendarContract.Calendars.VISIBLE, 1);
            values.put(CalendarContract.Calendars.SYNC_EVENTS, 1);
            
            Uri updateUri = ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calId);
            contentResolver.update(updateUri, values, null, null);
            Log.d(TAG, " Ensured calendar " + calId + " is VISIBLE");
        } catch (Exception e) {
            Log.e(TAG, " Error setting calendar visible: " + e.getMessage());
        }
    }

    private long queryForCalendarId() {
        // Query tất cả calendars với thông tin về visibility và sync
        String[] projection = {
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.ACCOUNT_TYPE,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.VISIBLE,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        };

        long firstId = -1;
        long googlePrimaryId = -1;
        long googleWritableId = -1;

        try {
            Cursor cursor = contentResolver.query(
                    CalendarContract.Calendars.CONTENT_URI,
                    projection, null, null, null);

            if (cursor == null) {
                Log.e(TAG, "❌ Calendar query returned NULL cursor");
                return -1;
            }

            Log.d(TAG, "📋 Total calendars on device: " + cursor.getCount());

            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String accountName = cursor.getString(1);
                String accountType = cursor.getString(2);
                String displayName = cursor.getString(3);
                int visible = cursor.getInt(4);
                int accessLevel = cursor.getInt(5);

                Log.d(TAG, "  📅 Calendar: id=" + id
                        + " | account=" + accountName
                        + " | type=" + accountType
                        + " | name=" + displayName
                        + " | visible=" + visible
                        + " | access=" + accessLevel);

                if (firstId == -1) firstId = id;

                // Tìm Google calendar có quyền ghi (owner hoặc contributor)
                if ("com.google".equals(accountType)) {
                    boolean canWrite = (accessLevel >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR);
                    
                    // Ưu tiên calendar chính (thường là email@gmail.com)
                    if (accountName != null && accountName.equals(displayName) && canWrite) {
                        googlePrimaryId = id;
                    }
                    
                    // Fallback: bất kỳ Google calendar nào có quyền ghi
                    if (googleWritableId == -1 && canWrite) {
                        googleWritableId = id;
                    }
                }
            }
            cursor.close();
        } catch (SecurityException se) {
            Log.e(TAG, "❌ SecurityException querying calendars: " + se.getMessage());
            return -1;
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception querying calendars: " + e.getMessage());
            return -1;
        }

        // Ưu tiên: Google primary > Google writable > first calendar
        long chosen = -1;
        if (googlePrimaryId != -1) {
            chosen = googlePrimaryId;
            Log.d(TAG, "✅ Chosen Google PRIMARY calendar ID = " + chosen);
        } else if (googleWritableId != -1) {
            chosen = googleWritableId;
            Log.d(TAG, "✅ Chosen Google WRITABLE calendar ID = " + chosen);
        } else {
            chosen = firstId;
            Log.d(TAG, "✅ Chosen FIRST calendar ID = " + chosen);
        }
        
        return chosen;
    }

    private long createLocalCalendar() {
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Calendars.ACCOUNT_NAME, "TeeTickTick local");
        values.put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL);
        values.put(CalendarContract.Calendars.NAME, "TeeTickTick Calendar");
        values.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "TeeTickTick Local Calendar");
        values.put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFF00BFA5); // Teal color
        values.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER);
        values.put(CalendarContract.Calendars.OWNER_ACCOUNT, "TeeTickTick local");
        values.put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, java.util.TimeZone.getDefault().getID());
        values.put(CalendarContract.Calendars.SYNC_EVENTS, 1);
        values.put(CalendarContract.Calendars.VISIBLE, 1); // Đảm bảo Lịch được hiển thị mặc định

        Uri uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "TeeTickTick local")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                .build();

        try {
            Uri result = contentResolver.insert(uri, values);
            if (result != null) {
                long newId = ContentUris.parseId(result);
                Log.d(TAG, "✅ Local calendar created with ID: " + newId);
                return newId;
            } else {
                Log.e(TAG, "❌ Failed to create local calendar: URI is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception while creating local calendar: " + e.getMessage());
        }
        return -1;
    }

    // -------------------------------------------------------------------------
    // 2. Thêm event mới từ task
    // -------------------------------------------------------------------------

    public long addEventForTask(TaskEntity task) {
        Log.d(TAG, "=== addEventForTask START: '" + task.getTitle() + "' ===");

        if (!hasCalendarPermission()) {
            Log.e(TAG, "❌ SKIP — no permission");
            return -1;
        }

        long calendarId = getDefaultCalendarId();
        if (calendarId == -1) {
            Log.e(TAG, "❌ SKIP — no calendar found");
            return -1;
        }

        Long startMillis = task.getStartDate();
        Long endMillis = task.getDueDate();

        Log.d(TAG, "  startDate=" + startMillis + " dueDate=" + endMillis);

        if (startMillis == null && endMillis == null) {
            Log.w(TAG, "❌ SKIP — task has no dates");
            return -1;
        }

        if (startMillis == null) startMillis = endMillis;
        if (endMillis == null) endMillis = startMillis;
        if (startMillis.equals(endMillis)) {
            endMillis = startMillis + 60 * 60 * 1000L;
        }

        Log.d(TAG, "  Final: start=" + startMillis + " end=" + endMillis
                + " tz=" + java.util.TimeZone.getDefault().getID());

        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.CALENDAR_ID, calendarId);
        values.put(CalendarContract.Events.TITLE, buildEventTitle(task));
        values.put(CalendarContract.Events.DESCRIPTION, buildEventDescription(task));
        values.put(CalendarContract.Events.DTSTART, startMillis);
        values.put(CalendarContract.Events.DTEND, endMillis);
        values.put(CalendarContract.Events.EVENT_TIMEZONE,
                java.util.TimeZone.getDefault().getID());
        
        // Các thuộc tính quan trọng để event hiển thị đúng
        values.put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED);
        values.put(CalendarContract.Events.HAS_ALARM, 0);
        values.put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY);
        
        // Note: VISIBLE field is read-only, only provider can set it
        
        // Thêm màu sắc để dễ nhận diện (teal color)
        values.put(CalendarContract.Events.EVENT_COLOR, 0xFF00BFA5);

        try {
            Uri uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values);
            if (uri != null) {
                long eventId = ContentUris.parseId(uri);
                Log.d(TAG, "✅ EVENT CREATED: eventId=" + eventId + " title=" + task.getTitle());
                
                // Note: verifyEventCreated() removed to prevent ANR
                
                return eventId;
            } else {
                Log.e(TAG, "❌ insert() returned NULL uri");
            }
        } catch (SecurityException se) {
            Log.e(TAG, "❌ SecurityException inserting event: " + se.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception inserting event: " + e.getMessage());
            e.printStackTrace();
        }

        return -1;
    }

    // -------------------------------------------------------------------------
    // 3. Cập nhật event
    // -------------------------------------------------------------------------

    public boolean updateEventForTask(TaskEntity task) {
        if (!hasCalendarPermission()) return false;

        Long eventId = task.getCalendarEventId();
        if (eventId == null || eventId <= 0) {
            long newId = addEventForTask(task);
            return newId != -1;
        }

        Long startMillis = task.getStartDate();
        Long endMillis = task.getDueDate();

        if (startMillis == null && endMillis == null) {
            return deleteEventById(eventId);
        }

        if (startMillis == null) startMillis = endMillis;
        if (endMillis == null) endMillis = startMillis;
        if (startMillis.equals(endMillis)) {
            endMillis = startMillis + 60 * 60 * 1000L;
        }

        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.TITLE, buildEventTitle(task));
        values.put(CalendarContract.Events.DESCRIPTION, buildEventDescription(task));
        values.put(CalendarContract.Events.DTSTART, startMillis);
        values.put(CalendarContract.Events.DTEND, endMillis);
        values.put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED);

        Uri updateUri = ContentUris.withAppendedId(
                CalendarContract.Events.CONTENT_URI, eventId);

        try {
            int rows = contentResolver.update(updateUri, values, null, null);
            Log.d(TAG, "Event updated: eventId=" + eventId + " rows=" + rows);
            return rows > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error updating: " + e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // 4. Xóa event
    // -------------------------------------------------------------------------

    public boolean deleteEventForTask(TaskEntity task) {
        if (!hasCalendarPermission()) return false;
        Long eventId = task.getCalendarEventId();
        if (eventId == null || eventId <= 0) return true;
        return deleteEventById(eventId);
    }

    private boolean deleteEventById(long eventId) {
        Uri deleteUri = ContentUris.withAppendedId(
                CalendarContract.Events.CONTENT_URI, eventId);
        try {
            int rows = contentResolver.delete(deleteUri, null, null);
            Log.d(TAG, "Event deleted: eventId=" + eventId + " rows=" + rows);
            return rows > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting: " + e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // 5. Sync tất cả tasks
    // -------------------------------------------------------------------------

    public void syncTasksToCalendar(java.util.List<TaskEntity> tasks,
                                    hcmute.edu.vn.teeticktick.database.TaskDao dao) {
        if (!hasCalendarPermission()) {
            Log.w(TAG, "syncTasksToCalendar: no permission");
            return;
        }
        if (tasks == null || tasks.isEmpty()) {
            Log.d(TAG, "syncTasksToCalendar: no tasks to sync");
            return;
        }

        Log.d(TAG, "=== SYNC START: " + tasks.size() + " tasks ===");
        int synced = 0;
        for (TaskEntity task : tasks) {
            if (task.getCalendarEventId() != null && task.getCalendarEventId() > 0) {
                Log.d(TAG, "  SKIP (already synced): " + task.getTitle());
                continue;
            }
            long eventId = addEventForTask(task);
            if (eventId != -1) {
                dao.updateCalendarEventId(task.getId(), eventId);
                synced++;
            }
        }
        Log.d(TAG, "=== SYNC DONE: " + synced + "/" + tasks.size() + " ===");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String buildEventTitle(TaskEntity task) {
        String prefix = task.isCompleted() ? "✅ " : "⬜ ";
        return prefix + task.getTitle();
    }

    private String buildEventDescription(TaskEntity task) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(EVENT_SOURCE_APP).append("]");
        if (task.getListName() != null && !task.getListName().isEmpty()) {
            sb.append(" 📂 ").append(task.getListName());
        }
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            sb.append("\n").append(task.getDescription());
        }
        return sb.toString();
    }
}
