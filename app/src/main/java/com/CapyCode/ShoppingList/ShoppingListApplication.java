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
        
        android.util.Log.d("ShoppingListApp", "Application onCreate called");

        FirebaseApp.initializeApp(this); // Explicitly initialize

        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        
        if (BuildConfig.DEBUG) {
            android.util.Log.d("ShoppingListApp", "Running in DEBUG mode. Installing DebugAppCheckProviderFactory.");
            // Use Debug provider for local development (Emulator/Test Device)
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            );
        } else {
            android.util.Log.d("ShoppingListApp", "Running in RELEASE mode. Installing PlayIntegrityAppCheckProviderFactory.");
            // Use Play Integrity for Production (Release)
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            );
        }
    }
}
