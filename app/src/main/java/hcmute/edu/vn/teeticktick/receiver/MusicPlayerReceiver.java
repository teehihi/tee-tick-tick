package hcmute.edu.vn.teeticktick.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import hcmute.edu.vn.teeticktick.service.MusicService;

/**
 * BroadcastReceiver xử lý các action điều khiển nhạc từ notification.
 *
 * Các action hỗ trợ:
 *   - ACTION_PLAY_PAUSE: Toggle play/pause
 *   - ACTION_NEXT:       Chuyển bài tiếp theo
 *   - ACTION_PREVIOUS:   Quay lại bài trước
 *   - ACTION_CLOSE:      Dừng nhạc và tắt service
 */
public class MusicPlayerReceiver extends BroadcastReceiver {

    private static final String TAG = "MusicPlayerReceiver";

    public static final String ACTION_PLAY_PAUSE = "hcmute.edu.vn.teeticktick.ACTION_PLAY_PAUSE";
    public static final String ACTION_NEXT = "hcmute.edu.vn.teeticktick.ACTION_NEXT";
    public static final String ACTION_PREVIOUS = "hcmute.edu.vn.teeticktick.ACTION_PREVIOUS";
    public static final String ACTION_CLOSE = "hcmute.edu.vn.teeticktick.ACTION_CLOSE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        Log.d(TAG, "Received action: " + action);

        // Gửi command tới MusicService thông qua Intent
        Intent serviceIntent = new Intent(context, MusicService.class);
        serviceIntent.setAction(action);

        switch (action) {
            case ACTION_PLAY_PAUSE:
            case ACTION_NEXT:
            case ACTION_PREVIOUS:
                // Gửi action tới service đang chạy
                context.startService(serviceIntent);
                break;

            case ACTION_CLOSE:
                // Dừng service hoàn toàn
                context.stopService(serviceIntent);
                break;

            default:
                Log.w(TAG, "Unknown action: " + action);
                break;
        }
    }
}
