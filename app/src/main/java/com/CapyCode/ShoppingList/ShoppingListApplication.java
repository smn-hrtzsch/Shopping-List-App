package com.CapyCode.ShoppingList;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

public class ShoppingListApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        FirebaseApp.initializeApp(this);

        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        
        if (BuildConfig.DEBUG) {
            // Use Debug provider for local development (Emulator/Test Device)
            // This prints a debug token to Logcat which you can add to Firebase Console
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            );
        } else {
            // Use Play Integrity for Production (Release)
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            );
        }
    }
}
