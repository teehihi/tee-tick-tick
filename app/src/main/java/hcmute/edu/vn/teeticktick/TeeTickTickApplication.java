package hcmute.edu.vn.teeticktick;

import android.app.Application;

import com.facebook.stetho.Stetho;

public class TeeTickTickApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Stetho for database inspection
        Stetho.initializeWithDefaults(this);
    }
}
