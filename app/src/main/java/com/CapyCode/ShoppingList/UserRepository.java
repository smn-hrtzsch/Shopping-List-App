package com.CapyCode.ShoppingList;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import android.net.Uri;

import java.util.HashMap;
import java.util.Map;

public class UserRepository {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseStorage storage;
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
        void onError(String error);
    }

    public UserRepository(Context context) {
        this(context, FirebaseFirestore.getInstance(), FirebaseAuth.getInstance(), FirebaseStorage.getInstance());
    }

    public UserRepository(Context context, FirebaseFirestore db, FirebaseAuth auth, FirebaseStorage storage) {
        this.context = context;
        this.db = db;
        this.auth = auth;
        this.storage = storage;
    }

    public boolean isAuthenticated() {
        return auth.getCurrentUser() != null;
    }

    public String getCurrentUserId() {
        return auth.getUid();
    }

    public void getCurrentUsername(OnUsernameLoadedListener listener) {
        if (!isAuthenticated()) {
            listener.onError("Nicht eingeloggt (Auth == null).");
            return;
        }

        db.collection("users").document(getCurrentUserId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        listener.onLoaded(documentSnapshot.getString("username"));
                    } else {
                        // Dokument existiert nicht -> Kein Fehler, sondern einfach noch kein Profil
                        listener.onLoaded(null);
                    }
                })
                .addOnFailureListener(e -> listener.onError("Ladefehler: " + e.getMessage()));
    }

    public void checkUsernameChangeAllowed(OnProfileActionListener listener) {
        if (!isAuthenticated()) {
            listener.onError(context.getString(R.string.auth_required));
            return;
        }

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
        if (!isAuthenticated()) {
            listener.onError(context.getString(R.string.auth_required));
            return;
        }

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
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        listener.onError(context.getString(R.string.profile_error_check_failed) + ": " + errorMsg);
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

    public interface OnImageUploadListener {
        void onSuccess(String downloadUrl);
        void onError(String message);
    }

    public void uploadProfileImage(Uri imageUri, OnImageUploadListener listener) {
        if (!isAuthenticated()) {
            listener.onError(context.getString(R.string.auth_required));
            return;
        }
        
        String uid = getCurrentUserId();
        StorageReference ref = storage.getReference().child("profile_images/" + uid + ".jpg");
        
        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    String url = uri.toString();
                    // Save URL to Firestore user profile
                    updateProfileImage(url, new OnProfileActionListener() {
                        @Override
                        public void onSuccess() {
                            listener.onSuccess(url);
                        }
                        @Override
                        public void onError(String message) {
                            listener.onError("Image uploaded but profile update failed: " + message);
                        }
                    });
                }))
                .addOnFailureListener(e -> listener.onError("Upload failed: " + e.getMessage()));
    }

    public void removeProfileImage(OnProfileActionListener listener) {
        if (!isAuthenticated()) {
            listener.onError("Not logged in");
            return;
        }
        String uid = getCurrentUserId();
        
        // 1. Delete from Storage (Optional: if it exists)
        StorageReference ref = storage.getReference().child("profile_images/" + uid + ".jpg");
        ref.delete().addOnCompleteListener(task -> {
            // Even if delete fails (maybe file didn't exist), we remove the URL from Firestore
            updateProfileImage(null, listener);
        });
    }

    private void updateProfileImage(String url, OnProfileActionListener listener) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("profileImageUrl", url); // Firestore handles null correctly (sets to null or deletes field? update handles null as value)
        // Ideally we use FieldValue.delete() if url is null, but setting to null is fine for now if we check != null in code
        
        if (url == null) {
             Map<String, Object> deleteData = new HashMap<>();
             deleteData.put("profileImageUrl", com.google.firebase.firestore.FieldValue.delete());
             db.collection("users").document(getCurrentUserId())
                .update(deleteData)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
        } else {
            db.collection("users").document(getCurrentUserId())
                    .set(updateData, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> listener.onSuccess())
                    .addOnFailureListener(e -> listener.onError(e.getMessage()));
        }
    }

    public void getUserProfile(OnUserProfileLoadedListener listener) {
        if (!isAuthenticated()) {
             listener.onLoaded(null, null); // Or handle error
             return;
        }

        db.collection("users").document(getCurrentUserId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        String imageUrl = documentSnapshot.getString("profileImageUrl");
                        listener.onLoaded(username, imageUrl);
                    } else {
                        listener.onLoaded(null, null);
                    }
                })
                .addOnFailureListener(e -> listener.onError("Ladefehler: " + e.getMessage()));
    }

    public interface OnUserProfileLoadedListener {
        void onLoaded(String username, String imageUrl);
        void onError(String error);
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

