package hcmute.edu.vn.teeticktick;

import android.app.Application;

import com.facebook.stetho.Stetho;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.ios.IosEmojiProvider;

public class TeeTickTickApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        EmojiManager.install(new IosEmojiProvider());
        
        // Initialize Stetho for database inspection
        Stetho.initializeWithDefaults(this);
    }
}
