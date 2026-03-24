# 🎯 TeeTickTick — Ứng dụng quản lý công việc thông minh

> Ứng dụng Android quản lý task cá nhân, tích hợp **Content Provider**, **Bound Service phát nhạc**, **WorkManager**, và **Foreground Service**.

**Sinh viên:** Nguyễn Nhật Thiên, Phạm Công Trường  
**Trường:** HCMUTE — Lập trình di động  
**Package:** `hcmute.edu.vn.teeticktick`

---

## 📱 Tổng quan tính năng

| # | Tính năng | Thành phần Android |
|---|---|---|
| 1 | Quản lý task (CRUD, danh mục, ưu tiên) | Room Database, MVVM, LiveData |
| 2 | Nhắc nhở deadline task | WorkManager (OneTimeWorkRequest) |
| 3 | Thông báo tổng task hàng ngày | Foreground Service |
| 4 | Chia sẻ dữ liệu task cho app ngoài | **Content Provider** (tự tạo) |
| 5 | Phát nhạc tập trung từ thiết bị | **Content Provider** (MediaStore) + **Bound Service** |

---

## 🏗️ Kiến trúc tổng quan

```
┌──────────────────────────────────────────────────────┐
│                     UI Layer                         │
│  FirstFragment · SecondFragment · TimelineFragment   │
│  MusicPlayerFragment · SettingsFragment              │
├──────────────────────────────────────────────────────┤
│                  ViewModel Layer                     │
│         TaskViewModel · CategoryViewModel            │
├──────────────────────────────────────────────────────┤
│                 Repository Layer                     │
│        TaskRepository · CategoryRepository           │
├──────────────────────────────────────────────────────┤
│                  Database Layer                      │
│     AppDatabase (Room) · TaskDao · CategoryDao        │
│          TaskEntity · CategoryEntity                 │
├──────────────────────────────────────────────────────┤
│               Service / Provider Layer               │
│  TaskContentProvider · MusicService · DailyTaskService│
│  TaskReminderWorker · OverdueReminderWorker          │
└──────────────────────────────────────────────────────┘
```

---

## 📂 Cấu trúc thư mục chính

```
app/src/main/java/hcmute/edu/vn/teeticktick/
├── database/
│   ├── AppDatabase.java          # Room Database (tasks, categories)
│   ├── TaskDao.java              # DAO cho tasks
│   ├── TaskEntity.java           # Entity: id, title, description, dueDate, ...
│   ├── CategoryDao.java          # DAO cho categories
│   └── CategoryEntity.java      # Entity: id, name, emoji, notificationSound
│
├── repository/
│   ├── TaskRepository.java       # Repository pattern cho tasks
│   └── CategoryRepository.java   # Repository pattern cho categories
│
├── viewmodel/
│   ├── TaskViewModel.java        # ViewModel cho UI
│   └── CategoryViewModel.java
│
├── provider/                     # ★ CONTENT PROVIDER
│   ├── TaskContract.java         # Hằng số URI, column names
│   └── TaskContentProvider.java  # CRUD qua ContentResolver
│
├── service/                      # ★ SERVICES
│   ├── MusicService.java         # Bound Service - MediaPlayer
│   ├── DailyTaskService.java     # Foreground Service - daily summary
│   ├── TaskReminderWorker.java   # WorkManager - nhắc deadline
│   ├── OverdueReminderWorker.java# WorkManager - nhắc quá hạn
│   ├── TaskReminderScheduler.java# Lên lịch reminders
│   └── NotificationHelper.java  # Helper tạo notifications
│
├── model/
│   ├── Song.java                 # Model bài hát (MediaStore)
│   ├── TaskList.java
│   └── SmartListItem.java
│
├── adapter/
│   ├── SongAdapter.java          # Adapter danh sách nhạc
│   ├── CategoryIconPickerAdapter.java
│   ├── SoundPickerAdapter.java
│   └── ...
│
├── bottomsheet/                  # Bottom sheet dialogs
│   ├── AddListBottomSheet.java
│   ├── DateTimePickerBottomSheet.java
│   └── ...
│
├── MainActivity.java             # Activity chính, Navigation
├── MusicPlayerFragment.java      # ★ Tab Nhạc - query MediaStore
├── FirstFragment.java            # Tab Tasks
├── SecondFragment.java           # Tab Calendar
├── TimelineFragment.java         # Tab Timeline
└── SettingsFragment.java         # Tab Settings
```

---

## ★ Content Provider — Chi tiết

### TaskContentProvider (Tự tạo)

**Mục đích:** Cho phép ứng dụng bên ngoài (widget, calendar, app đồng phát triển) truy cập dữ liệu tasks và categories.

**Authority:** `hcmute.edu.vn.teeticktick.provider`

| URI | Mô tả | Hỗ trợ |
|---|---|---|
| `content://.../tasks` | Tất cả tasks | query, insert |
| `content://.../tasks/{id}` | Task theo ID | query, update, delete |
| `content://.../categories` | Tất cả categories | query, insert |
| `content://.../categories/{id}` | Category theo ID | query, update, delete |

**Bảo mật:** Protected bằng custom permissions (`protectionLevel="signature"`):
- `hcmute.edu.vn.teeticktick.READ_TASKS` — quyền đọc
- `hcmute.edu.vn.teeticktick.WRITE_TASKS` — quyền ghi

**File liên quan:**
- `provider/TaskContract.java` — hằng số
- `provider/TaskContentProvider.java` — implement `query()`, `insert()`, `update()`, `delete()`, `getType()`
- `AndroidManifest.xml` — khai báo `<provider>` và `<permission>`

### MediaStore.Audio (Content Provider hệ thống)

**Mục đích:** Đọc danh sách nhạc có sẵn trên thiết bị người dùng.

**Cách dùng trong code (`MusicPlayerFragment.java`):**
```java
// Query MediaStore Content Provider
Cursor cursor = getContentResolver().query(
    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,  // URI
    projection,                                    // columns
    MediaStore.Audio.Media.IS_MUSIC + " != 0",    // filter
    null, sortOrder
);
```

**Permission cần thiết:**
- Android 13+: `READ_MEDIA_AUDIO`
- Android 12 trở xuống: `READ_EXTERNAL_STORAGE`

---

## ★ Bound Service — Music Player

**Mục đích:** Phát nhạc nền khi người dùng làm task. Activity/Fragment bind vào service để điều khiển.

**File:** `service/MusicService.java`

**Cách hoạt động:**
```
MusicPlayerFragment ──bindService()──► MusicService
       │                                    │
       │◄── MusicBinder.getService() ──────│
       │                                    │
       ├── play(index)                      ├── MediaPlayer.setDataSource()
       ├── pause()                          ├── MediaPlayer.pause()
       ├── resume()                         ├── MediaPlayer.start()
       ├── playNext() / playPrevious()      ├── Auto next on completion
       └── seekTo(position)                 └── MediaPlayer.seekTo()
```

**Callback interface:**
```java
public interface MusicCallback {
    void onSongChanged(Song song);           // Bài hát thay đổi
    void onPlaybackStateChanged(boolean p);  // Play ↔ Pause
    void onSongCompleted();                  // Hết bài
}
```

---

## ★ WorkManager & Foreground Service

### WorkManager — Task Reminder
- `TaskReminderWorker` — Nhắc 30 phút trước deadline
- `OverdueReminderWorker` — Nhắc khi quá hạn (0h, +1h, +2h, +1 ngày)
- `TaskReminderScheduler` — Lên lịch/hủy reminders

### Foreground Service — Daily Summary
- `DailyTaskService` — Hiển thị persistent notification "Bạn có X task hôm nay"
- Khởi động khi mở app

---

## 🔧 Cách build và chạy

```bash
# Clone project
git clone <repo-url>

# Build debug APK
./gradlew assembleDebug

# Install lên thiết bị
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Yêu cầu:**
- Android Studio Koala+
- JDK 11
- Min SDK: 24 (Android 7.0)
- Target SDK: 36

---

## 🧪 Test Content Provider

```bash
# Query tất cả tasks
adb shell content query --uri content://hcmute.edu.vn.teeticktick.provider/tasks

# Query task theo ID
adb shell content query --uri content://hcmute.edu.vn.teeticktick.provider/tasks/1

# Insert task mới
adb shell content insert \
  --uri content://hcmute.edu.vn.teeticktick.provider/tasks \
  --bind title:s:"Test Task" \
  --bind listName:s:"Inbox" \
  --bind priority:i:0 \
  --bind isCompleted:b:false

# Query categories
adb shell content query --uri content://hcmute.edu.vn.teeticktick.provider/categories
```

---

## 📦 Dependencies

| Thư viện | Mục đích |
|---|---|
| Room | Database ORM |
| Navigation | Fragment navigation |
| WorkManager | Background task scheduling |
| Material Components | UI components |
| Lifecycle (LiveData, ViewModel) | MVVM architecture |
| Vanniktech Emoji | Emoji picker |
