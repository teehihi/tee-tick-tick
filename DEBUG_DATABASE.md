# Cách xem Database trong TeeTickTick App

## Phương pháp 1: Android Studio Database Inspector (Khuyên dùng)

### Bước 1: Chạy app
```bash
# Chạy app trên emulator hoặc device (debug mode)
```

### Bước 2: Mở Database Inspector
1. Android Studio → View → Tool Windows → App Inspection
2. Hoặc nhấn nút "App Inspection" ở thanh dưới cùng
3. Chọn tab "Database Inspector"

### Bước 3: Xem database
1. Chọn process: `hcmute.edu.vn.teeticktick`
2. Chọn database: `teetick_database`
3. Xem table: `tasks`
4. Click vào table để xem dữ liệu
5. Có thể chạy query SQL trực tiếp

**Ưu điểm:**
- Real-time updates
- GUI đẹp, dễ dùng
- Không cần cài thêm gì
- Query trực tiếp

---

## Phương pháp 2: Stetho + Chrome DevTools (Đã tích hợp)

### Bước 1: Chạy app (debug build)
```bash
# App đã có Stetho, chỉ cần chạy
```

### Bước 2: Mở Chrome
1. Mở Chrome browser
2. Vào: `chrome://inspect`
3. Tìm device của bạn
4. Click "inspect" bên dưới app name

### Bước 3: Xem database
1. Trong Chrome DevTools, chọn tab "Resources"
2. Mở "Web SQL" → "teetick_database"
3. Xem table "tasks"
4. Chạy query SQL trong console

**Ưu điểm:**
- Xem qua browser
- Network inspection
- Shared Preferences viewer
- View Hierarchy

---

## Phương pháp 3: ADB Shell + SQLite3

### Bước 1: Vào shell
```bash
adb shell
```

### Bước 2: Truy cập database
```bash
run-as hcmute.edu.vn.teeticktick
cd databases
ls
```

### Bước 3: Mở SQLite
```bash
sqlite3 teetick_database
```

### Các lệnh SQLite hữu ích:
```sql
-- Xem tất cả tables
.tables

-- Xem cấu trúc bảng
.schema tasks

-- Xem dữ liệu
SELECT * FROM tasks;

-- Xem với format đẹp
.mode column
.headers on
SELECT * FROM tasks;

-- Đếm số task
SELECT COUNT(*) FROM tasks;

-- Xem task chưa hoàn thành
SELECT * FROM tasks WHERE isCompleted = 0;

-- Xem task theo list
SELECT * FROM tasks WHERE listName = 'Welcome';

-- Thoát
.exit
```

---

## Phương pháp 4: Pull Database File

### Bước 1: Pull file về máy
```bash
# Cách 1: Dùng adb exec-out
adb exec-out run-as hcmute.edu.vn.teeticktick cat databases/teetick_database > ~/Desktop/teetick_database.db

# Cách 2: Dùng adb pull (cần root)
adb root
adb pull /data/data/hcmute.edu.vn.teeticktick/databases/teetick_database ~/Desktop/
```

### Bước 2: Mở bằng DB Browser
1. Download DB Browser for SQLite: https://sqlitebrowser.org/
2. Mở file `teetick_database.db`
3. Xem và edit dữ liệu

**Ưu điểm:**
- Offline viewing
- Export to CSV/JSON
- Visual query builder
- Schema designer

---

## Phương pháp 5: Logcat Debug (Đơn giản nhất)

Thêm code debug vào FirstFragment:

```java
taskViewModel.getAllTasks().observe(getViewLifecycleOwner(), tasks -> {
    Log.d("DATABASE", "Total tasks: " + tasks.size());
    for (TaskEntity task : tasks) {
        Log.d("DATABASE", "Task: " + task.getTitle() + 
              " | Completed: " + task.isCompleted() +
              " | List: " + task.getListName());
    }
});
```

Xem trong Logcat:
```bash
adb logcat | grep DATABASE
```

---

## Vị trí Database trên Device

```
/data/data/hcmute.edu.vn.teeticktick/databases/teetick_database
```

## Các file liên quan:
- `teetick_database` - Database chính
- `teetick_database-shm` - Shared memory file
- `teetick_database-wal` - Write-Ahead Log

---

## Khuyến nghị

**Trong quá trình phát triển:**
- Dùng Android Studio Database Inspector (dễ nhất)
- Hoặc Stetho + Chrome (nhiều tính năng)

**Khi debug lỗi:**
- Pull database file và dùng DB Browser
- Xem schema và data offline

**Khi demo:**
- Logcat để show real-time updates
