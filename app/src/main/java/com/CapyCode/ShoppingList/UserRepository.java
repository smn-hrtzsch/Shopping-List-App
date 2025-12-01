package com.CapyCode.ShoppingList;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class UserRepository {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Context context;

    public interface OnUserSearchListener {
        void onUserFound(String uid);
        void onError(String message);
    }

    public interface OnProfileActionListener {
        void onSuccess();
        void onError(String message);
    }

    public interface OnUsernameLoadedListener {
        void onLoaded(String username);
    }

    public UserRepository(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public boolean isAuthenticated() {
        return auth.getCurrentUser() != null;
    }

    public String getCurrentUserId() {
        return auth.getUid();
    }

    public void getCurrentUsername(OnUsernameLoadedListener listener) {
        if (!isAuthenticated()) {
            listener.onLoaded(null);
            return;
        }

        db.collection("users").document(getCurrentUserId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        listener.onLoaded(documentSnapshot.getString("username"));
                    } else {
                        listener.onLoaded(null);
                    }
                })
                .addOnFailureListener(e -> listener.onLoaded(null));
    }

    public void checkUsernameChangeAllowed(OnProfileActionListener listener) {
        if (!isAuthenticated()) return;

        // Bypass check in Debug Mode
        if (BuildConfig.DEBUG) {
            listener.onSuccess();
            return;
        }
        
        db.collection("users").document(getCurrentUserId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("lastUsernameChange")) {
                        com.google.firebase.Timestamp lastChange = documentSnapshot.getTimestamp("lastUsernameChange");
                        if (lastChange != null) {
                            long oneWeekInMillis = 7 * 24 * 60 * 60 * 1000L;
                            long diff = System.currentTimeMillis() - lastChange.toDate().getTime();
                            if (diff < oneWeekInMillis) {
                                listener.onError(context.getString(R.string.error_username_cooldown));
                                return;
                            }
                        }
                    }
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> listener.onError(context.getString(R.string.error_check_date)));
    }

    public void setUsername(String username, OnProfileActionListener listener) {
        if (!isAuthenticated()) return;

        // 1. Check if username is taken by someone else
        db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot result = task.getResult();
                        if (result != null && !result.isEmpty()) {
                            // Check if it's me (I might be updating my own profile)
                            if (result.getDocuments().get(0).getId().equals(getCurrentUserId())) {
                                updateProfile(username, listener);
                            } else {
                                listener.onError(context.getString(R.string.profile_error_username_taken));
                            }
                        } else {
                            // Username is free
                            updateProfile(username, listener);
                        }
                    } else {
                        listener.onError(context.getString(R.string.profile_error_check_failed));
                    }
                });
    }

    private void updateProfile(String username, OnProfileActionListener listener) {
        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("username", username);
        userProfile.put("lastUsernameChange", com.google.firebase.firestore.FieldValue.serverTimestamp());
        // Keep creation date if exists, strictly speaking we usually merge, but set is ok here for simplicity 
        // if we assume 'created' was set on first creation. Let's use SetOptions.merge() logic manually or 
        // just set the fields we want.
        // To be safe regarding "created" date overwriting, we should probably use update or set with merge, 
        // but standard set overwrites. For now, let's just write these two.
        
        // Better approach: Check existence first or use merge. 
        // For this simple app, overwriting is "okay" but let's try to preserve 'created' if we can, 
        // but adding complex logic for that might be overkill. 
        // Let's just update these fields.
        
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("username", username);
        updateData.put("lastUsernameChange", com.google.firebase.firestore.FieldValue.serverTimestamp());
        
        // Use set with merge to create or update without losing other fields
        db.collection("users").document(getCurrentUserId())
                .set(updateData, com.google.firebase.firestore.SetOptions.merge()) 
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public void findUidByUsername(String username, OnUserSearchListener listener) {
        db.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        listener.onUserFound(queryDocumentSnapshots.getDocuments().get(0).getId());
                    } else {
                        listener.onError(context.getString(R.string.user_not_found, username));
                    }
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public void deleteAccount(OnProfileActionListener listener) {
        if (!isAuthenticated()) return;
        FirebaseUser user = auth.getCurrentUser();
        String uid = user.getUid();

        // 1. Delete User Document in 'users' collection
        db.collection("users").document(uid).delete()
                .addOnCompleteListener(task -> {
                    // 2. Delete Auth Account
                    if (user != null) {
                        user.delete()
                                .addOnSuccessListener(aVoid -> listener.onSuccess())
                                .addOnFailureListener(e -> listener.onError(context.getString(R.string.error_delete_account, e.getMessage())));
                    } else {
                        listener.onSuccess();
                    }
                });
    }
}
