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
        
        // Custom sounds mapped to actual raw file names
        addSoundIfExists(context, sounds, "work",     "Công việc",  "work_reminder");
        addSoundIfExists(context, sounds, "personal", "Cá nhân",    "reminder");
        addSoundIfExists(context, sounds, "shopping", "Mua sắm",    "shopping_reminder");
        addSoundIfExists(context, sounds, "learning", "Học tập",    "study_reminder");
        
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

        // Map soundId → raw resource name
        String rawName;
        switch (soundId) {
            case "work":     rawName = "work_reminder";     break;
            case "personal": rawName = "reminder";          break;
            case "shopping": rawName = "shopping_reminder"; break;
            case "learning": rawName = "study_reminder";    break;
            case "overdue":  rawName = "mixi_reminder";     break;
            default:         rawName = soundId;             break; // allow direct raw name
        }

        try {
            int resId = context.getResources().getIdentifier(rawName, "raw", context.getPackageName());
            if (resId != 0) {
                return Uri.parse("android.resource://" + context.getPackageName() + "/" + resId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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
