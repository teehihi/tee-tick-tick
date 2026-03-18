package hcmute.edu.vn.teeticktick.utils;

import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public class NotificationSoundHelper {
    
    public static class SoundOption {
        private String id;
        private String name;
        private Uri uri;
        
        public SoundOption(String id, String name, Uri uri) {
            this.id = id;
            this.name = name;
            this.uri = uri;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public Uri getUri() { return uri; }
    }
    
    public static List<SoundOption> getAvailableSounds(Context context) {
        List<SoundOption> sounds = new ArrayList<>();
        
        // Default system sound
        sounds.add(new SoundOption(
            "default",
            "Mặc định",
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ));
        
        // Check and add custom sounds if they exist
        addSoundIfExists(context, sounds, "sound_1", "Âm thanh 1 - Công việc", "notification_sound_1");
        addSoundIfExists(context, sounds, "sound_2", "Âm thanh 2 - Cá nhân", "notification_sound_2");
        addSoundIfExists(context, sounds, "sound_3", "Âm thanh 3 - Mua sắm", "notification_sound_3");
        addSoundIfExists(context, sounds, "sound_4", "Âm thanh 4 - Học tập", "notification_sound_4");
        
        return sounds;
    }
    
    private static void addSoundIfExists(Context context, List<SoundOption> sounds, 
                                        String id, String name, String resourceName) {
        try {
            int resId = context.getResources().getIdentifier(resourceName, "raw", context.getPackageName());
            if (resId != 0) {
                sounds.add(new SoundOption(
                    id,
                    name,
                    Uri.parse("android.resource://" + context.getPackageName() + "/" + resId)
                ));
            }
        } catch (Exception e) {
            // Resource doesn't exist, skip it
        }
    }
    
    public static Uri getSoundUri(Context context, String soundId) {
        if (soundId == null || soundId.equals("default")) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        
        // Try to get the resource ID
        try {
            int resId = context.getResources().getIdentifier(
                soundId.replace("sound_", "notification_sound_"),
                "raw",
                context.getPackageName()
            );
            
            if (resId != 0) {
                return Uri.parse("android.resource://" + context.getPackageName() + "/" + resId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Fallback to default
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    }
    
    public static String getSoundName(Context context, String soundId) {
        if (soundId == null || soundId.equals("default")) {
            return "Mặc định";
        }
        
        List<SoundOption> sounds = getAvailableSounds(context);
        for (SoundOption sound : sounds) {
            if (sound.getId().equals(soundId)) {
                return sound.getName();
            }
        }
        
        return "Mặc định";
    }
}
