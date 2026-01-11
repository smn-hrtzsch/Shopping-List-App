package com.CapyCode.ShoppingList;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShoppingListRepository {
    private ShoppingListDatabaseHelper dbHelper;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Context context;
    private UserRepository userRepository;
    private com.google.firebase.firestore.ListenerRegistration listsListener1;
    private com.google.firebase.firestore.ListenerRegistration listsListener2;

    public interface OnListsLoadedListener {
        void onListsLoaded(List<ShoppingList> shoppingLists, boolean fromServer);
    }

    public interface OnItemsLoadedListener {
        void onItemsLoaded(List<ShoppingItem> shoppingItems, boolean hasPendingWrites);
    }

    public ShoppingListRepository(Context context) {
        this(context, FirebaseFirestore.getInstance(), FirebaseAuth.getInstance());
    }

    public ShoppingListRepository(Context context, FirebaseFirestore db, FirebaseAuth mAuth) {
        this.dbHelper = new ShoppingListDatabaseHelper(context);
        this.db = db;
        this.mAuth = mAuth;
        this.context = context;
        this.userRepository = new UserRepository(context, db, mAuth, com.google.firebase.storage.FirebaseStorage.getInstance());
    }

    public ShoppingListDatabaseHelper getShoppingListDatabaseHelper() {
        return dbHelper;
    }

    public boolean hasUnsyncedLists() {
        List<ShoppingList> lists = dbHelper.getAllShoppingLists();
        for (ShoppingList list : lists) {
            if (list.getFirebaseId() == null || list.getFirebaseId().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public int getLocalListCount() {
        return dbHelper.getShoppingListCount();
    }

    public int getNextPosition() {
        return dbHelper.getMaxPosition() + 1;
    }

    public void stopListeningToLists() {
        if (listsListener1 != null) {
            listsListener1.remove();
            listsListener1 = null;
        }
        if (listsListener2 != null) {
            listsListener2.remove();
            listsListener2 = null;
        }
    }

    public void getAllShoppingLists(OnListsLoadedListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        
        // If no user, local data is "final" (treated as fromServer/authoritative)
        listener.onListsLoaded(dbHelper.getAllShoppingLists(), currentUser == null);

        if (currentUser == null) return;

        if (!userRepository.isUserVerified()) {
            return; // Don't sync cloud lists if email is not verified
        }

        stopListeningToLists();

        String userId = currentUser.getUid();
        
        com.google.firebase.firestore.EventListener<com.google.firebase.firestore.QuerySnapshot> syncListener = (value, e) -> {
            if (e != null) {
                Log.w("Firestore", "Listen for lists failed.", e);
                // On failure, we might want to signal that "loading" is done (failed) so UI doesn't hang
                // but for now let's keep it silent or maybe send current local data as "fromServer"?
                // listener.onListsLoaded(dbHelper.getAllShoppingLists(), true); 
                return;
            }
            refreshAllListsFromServer(userId, listener);
        };

        listsListener1 = db.collection("shopping_lists").whereArrayContains("members", userId).addSnapshotListener(syncListener);
        listsListener2 = db.collection("shopping_lists").whereArrayContains("pending_members", userId).addSnapshotListener(syncListener);
    }

    private void refreshAllListsFromServer(String userId, OnListsLoadedListener listener) {
        db.collection("shopping_lists").whereArrayContains("members", userId).get()
            .addOnSuccessListener(membersSnap -> {
                db.collection("shopping_lists").whereArrayContains("pending_members", userId).get()
                    .addOnSuccessListener(pendingSnap -> {
                        List<QueryDocumentSnapshot> allDocs = new ArrayList<>();
                        for(QueryDocumentSnapshot d : membersSnap) allDocs.add(d);
                        for(QueryDocumentSnapshot d : pendingSnap) allDocs.add(d);
                        processServerLists(allDocs, listener);
                    });
            });
    }

    private void processServerLists(List<QueryDocumentSnapshot> docs, OnListsLoadedListener listener) {
        if (docs.isEmpty()) {
            dbHelper.deleteObsoleteCloudLists(new ArrayList<>());
            listener.onListsLoaded(dbHelper.getAllShoppingLists(), true);
            return;
        }

        int listCount = docs.size();
        java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(0);
        List<String> activeFirebaseIds = new ArrayList<>();

        for (QueryDocumentSnapshot doc : docs) {
            String id = doc.getId();
            String name = doc.getString("name");
            String ownerId = doc.getString("ownerId");
            Boolean isShared = doc.getBoolean("isShared");
            @SuppressWarnings("unchecked")
            List<String> members = (List<String>) doc.get("members");
            @SuppressWarnings("unchecked")
            List<String> pendingMembers = (List<String>) doc.get("pending_members");

            ShoppingList list = new ShoppingList(name);
            list.setFirebaseId(id);
            list.setOwnerId(ownerId);
            list.setShared(isShared != null ? isShared : false);
            list.setMembers(members != null ? members : new ArrayList<>());
            list.setPendingMembers(pendingMembers != null ? pendingMembers : new ArrayList<>());
            
            activeFirebaseIds.add(id);

            // Fetch owner name
            db.collection("users").document(ownerId).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String username = userDoc.getString("username");
                        String email = userDoc.getString("email");
                        list.setOwnerUsername(username != null ? username : (email != null ? email : "Unbekannt"));
                    } else {
                        list.setOwnerUsername("Unbekannt");
                    }
                    
                    // After owner name, fetch item count
                    db.collection("shopping_lists").document(id).collection("items").get()
                        .addOnSuccessListener(items -> {
                            list.setItemCount(items.size());
                            dbHelper.upsertCloudList(list);
                            if (counter.incrementAndGet() == listCount) {
                                dbHelper.deleteObsoleteCloudLists(activeFirebaseIds);
                                listener.onListsLoaded(dbHelper.getAllShoppingLists(), true);
                            }
                        })
                        .addOnFailureListener(failure -> {
                            list.setItemCount(0); 
                            dbHelper.upsertCloudList(list);
                            if (counter.incrementAndGet() == listCount) {
                                dbHelper.deleteObsoleteCloudLists(activeFirebaseIds);
                                listener.onListsLoaded(dbHelper.getAllShoppingLists(), true);
                            }
                        });
                })
                .addOnFailureListener(e -> {
                    list.setOwnerUsername("Fehler");
                    // Continue with item count anyway
                    db.collection("shopping_lists").document(id).collection("items").get()
                        .addOnCompleteListener(t -> {
                            if (t.isSuccessful()) list.setItemCount(t.getResult().size());
                            dbHelper.upsertCloudList(list);
                            if (counter.incrementAndGet() == listCount) {
                                dbHelper.deleteObsoleteCloudLists(activeFirebaseIds);
                                listener.onListsLoaded(dbHelper.getAllShoppingLists(), true);
                            }
                        });
                });
        }
    }

    public void getItemsForListId(long listId, OnItemsLoadedListener listener) {
        listener.onItemsLoaded(dbHelper.getAllItems(listId), false);
    }

    public void fetchItemsFromCloudOneTime(String firebaseListId, OnItemsLoadedListener listener) {
        if (!userRepository.isUserVerified()) {
            listener.onItemsLoaded(new ArrayList<>(), false);
            return;
        }
        db.collection("shopping_lists").document(firebaseListId).collection("items")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ShoppingItem> items = new ArrayList<>();
                    if (queryDocumentSnapshots != null) {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            ShoppingItem item = new ShoppingItem();
                            item.setName(doc.getString("name"));
                            item.setQuantity(doc.getString("quantity"));
                            item.setUnit(doc.getString("unit"));
                            Boolean done = doc.getBoolean("done");
                            item.setDone(done != null ? done : false);
                            item.setNotes(doc.getString("notes"));
                            Long pos = doc.getLong("position");
                            item.setPosition(pos != null ? pos.intValue() : 0);
                            item.setFirebaseId(doc.getId());
                            items.add(item);
                        }
                    }
                    // Manual sorting to match local DB behavior (done ASC, position ASC, ID ASC)
                    items.sort((a, b) -> {
                        if (a.isDone() != b.isDone()) return a.isDone() ? 1 : -1;
                        int posComp = Integer.compare(a.getPosition(), b.getPosition());
                        if (posComp != 0) return posComp;
                        return a.getFirebaseId().compareTo(b.getFirebaseId());
                    });
                    listener.onItemsLoaded(items, false);
                })
                .addOnFailureListener(e -> listener.onItemsLoaded(new ArrayList<>(), false));
    }

    public com.google.firebase.firestore.ListenerRegistration getItemsForListId(String firebaseListId, OnItemsLoadedListener listener) {
        if (!userRepository.isUserVerified()) {
            listener.onItemsLoaded(new ArrayList<>(), false);
            return null;
        }
        return db.collection("shopping_lists").document(firebaseListId).collection("items")
                .addSnapshotListener((value, e) -> {
                    if (e != null) {
                        android.util.Log.e("RepoDebug", "Error listening for items", e);
                        listener.onItemsLoaded(new ArrayList<>(), false);
                        return;
                    }
                    List<ShoppingItem> items = new ArrayList<>();
                    boolean hasPendingWrites = false;
                    if (value != null) {
                        hasPendingWrites = value.getMetadata().hasPendingWrites();
                        android.util.Log.d("RepoDebug", "Snapshot received. hasPendingWrites=" + hasPendingWrites + " count=" + value.size());
                        for (QueryDocumentSnapshot doc : value) {
                            ShoppingItem item = new ShoppingItem();
                            item.setName(doc.getString("name"));
                            item.setQuantity(doc.getString("quantity"));
                            item.setUnit(doc.getString("unit"));
                            Boolean done = doc.getBoolean("done");
                            item.setDone(done != null ? done : false);
                            item.setNotes(doc.getString("notes"));
                            Long pos = doc.getLong("position");
                            item.setPosition(pos != null ? pos.intValue() : 0);
                            item.setFirebaseId(doc.getId());
                            items.add(item);
                            // Log specific item if needed, but let's keep it quiet for now
                        }
                    }
                    // Manual sorting to match local DB behavior (done ASC, position ASC, ID ASC)
                    items.sort((a, b) -> {
                        if (a.isDone() != b.isDone()) return a.isDone() ? 1 : -1;
                        int posComp = Integer.compare(a.getPosition(), b.getPosition());
                        if (posComp != 0) return posComp;
                        return a.getFirebaseId().compareTo(b.getFirebaseId());
                    });
                    
                    listener.onItemsLoaded(items, hasPendingWrites);
                });
    }

    public void updateListPositions(List<ShoppingList> lists) {
        dbHelper.updateListPositions(lists);
    }

    public ShoppingList getShoppingListById(long listId) {
        return dbHelper.getShoppingList(listId);
    }

    public long addShoppingList(String listName, int position) {
        return dbHelper.addShoppingList(listName, position);
    }

    public void deleteShoppingList(ShoppingList list) {
        if (list.getFirebaseId() != null) {
            deleteShoppingList(list.getFirebaseId());
        } else {
            dbHelper.deleteShoppingList(list.getId());
        }
    }

    public void deleteShoppingList(long listId) {
        dbHelper.deleteShoppingList(listId);
    }

    public void deleteShoppingList(String firebaseListId) {
        db.collection("shopping_lists").document(firebaseListId).delete();
    }

    public void clearLocalDatabase() {
        dbHelper.deleteAllLists();
    }

    public interface OnActionListener {
        void onActionComplete();
    }

    public long addItemToShoppingList(long listId, ShoppingItem item) {
        item.setListId(listId);
        return dbHelper.addItem(item);
    }

    public void addItemToShoppingList(String firebaseListId, ShoppingItem item, OnActionListener listener) {
        if (!userRepository.isUserVerified()) {
            if (listener != null) listener.onActionComplete();
            return;
        }
        com.google.firebase.firestore.DocumentReference ref = db.collection("shopping_lists").document(firebaseListId).collection("items").document();
        item.setFirebaseId(ref.getId());
        ref.set(item)
            .addOnCompleteListener(task -> {
                if (listener != null) listener.onActionComplete();
            });
    }

    public void restoreItemToShoppingList(String firebaseListId, ShoppingItem item, OnActionListener listener) {
        if (!userRepository.isUserVerified()) {
            if (listener != null) listener.onActionComplete();
            return;
        }
        if (item.getFirebaseId() != null) {
            // Ensure we set the item with its original position
            db.collection("shopping_lists").document(firebaseListId).collection("items").document(item.getFirebaseId()).set(item)
                .addOnCompleteListener(task -> {
                    if (listener != null) listener.onActionComplete();
                });
        } else {
            addItemToShoppingList(firebaseListId, item, listener);
        }
    }

    public void updateItemInList(ShoppingItem item, String firebaseListId, OnActionListener listener) {
        if (firebaseListId != null && item.getFirebaseId() != null) {
            if (!userRepository.isUserVerified()) {
                if (listener != null) listener.onActionComplete();
                return;
            }
            // Use Map to ensure precise field updates and avoid potential POJO mapping issues
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("name", item.getName());
            itemData.put("quantity", item.getQuantity());
            itemData.put("unit", item.getUnit());
            itemData.put("done", item.isDone());
            itemData.put("notes", item.getNotes());
            itemData.put("position", item.getPosition());

            db.collection("shopping_lists").document(firebaseListId).collection("items").document(item.getFirebaseId())
                .update(itemData)
                .addOnCompleteListener(task -> {
                    if (listener != null) listener.onActionComplete();
                })
                .addOnFailureListener(e -> {
                    // Fallback to set if document doesn't exist (though it should)
                    db.collection("shopping_lists").document(firebaseListId).collection("items").document(item.getFirebaseId())
                        .set(itemData)
                        .addOnCompleteListener(t -> {
                             if (listener != null) listener.onActionComplete();
                        });
                });
        } else {
            dbHelper.updateItem(item);
            if (listener != null) listener.onActionComplete();
        }
    }

    public void updateItemPositions(List<ShoppingItem> items) {
        dbHelper.updateItemPositionsBatch(items);
    }

    public void updateItemPositionsInCloud(String firebaseListId, List<ShoppingItem> items, OnActionListener listener) {
        if (firebaseListId == null || items == null || items.isEmpty()) {
            if (listener != null) listener.onActionComplete();
            return;
        }

        if (!userRepository.isUserVerified()) {
            if (listener != null) listener.onActionComplete();
            return;
        }

        com.google.firebase.firestore.WriteBatch batch = db.batch();
        com.google.firebase.firestore.CollectionReference itemsRef = db.collection("shopping_lists").document(firebaseListId).collection("items");

        for (ShoppingItem item : items) {
            if (item.getFirebaseId() != null) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("position", item.getPosition());
                batch.update(itemsRef.document(item.getFirebaseId()), updates);
            }
        }

        batch.commit().addOnCompleteListener(task -> {
            if (listener != null) listener.onActionComplete();
        });
    }

    public void updateShoppingList(ShoppingList list) {
        if (list.getFirebaseId() != null) {
            if (!userRepository.isUserVerified()) return;
            db.collection("shopping_lists").document(list.getFirebaseId()).update("name", list.getName());
        } else {
            dbHelper.updateShoppingList(list);
        }
    }

    public void deleteItemFromList(long itemId) {
        dbHelper.deleteItem(itemId);
    }

    public void deleteItemFromList(String firebaseListId, String firebaseItemId) {
        if (!userRepository.isUserVerified()) return;
        db.collection("shopping_lists").document(firebaseListId).collection("items").document(firebaseItemId).delete();
    }

    public void toggleItemChecked(long itemId, boolean isChecked) {
        ShoppingItem item = dbHelper.getItem(itemId);
        if (item != null) {
            item.setDone(isChecked);
            dbHelper.updateItem(item);
        }
    }

    public void toggleItemChecked(String firebaseListId, String firebaseItemId, boolean isChecked, OnActionListener listener) {
        if (!userRepository.isUserVerified()) {
            if (listener != null) listener.onActionComplete();
            return;
        }
        db.collection("shopping_lists").document(firebaseListId).collection("items").document(firebaseItemId).update("done", isChecked)
            .addOnCompleteListener(task -> {
                if (listener != null) listener.onActionComplete();
            });
    }

    public void clearCheckedItemsFromList(long listId) {
        dbHelper.deleteCheckedItems(listId);
    }

    public void clearCheckedItemsFromList(String firebaseListId, OnActionListener listener) {
        if (!userRepository.isUserVerified()) {
            if (listener != null) listener.onActionComplete();
            return;
        }
        db.collection("shopping_lists").document(firebaseListId).collection("items")
                .whereEqualTo("done", true).get().addOnSuccessListener(snaps -> {
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : snaps) batch.delete(doc.getReference());
                    batch.commit().addOnCompleteListener(t -> {
                        if (listener != null) listener.onActionComplete();
                    });
                });
    }

    public void clearAllItemsFromList(long listId) {
        dbHelper.clearList(listId);
    }

    public void clearAllItemsFromList(String firebaseListId, OnActionListener listener) {
        if (!userRepository.isUserVerified()) {
            if (listener != null) listener.onActionComplete();
            return;
        }
        db.collection("shopping_lists").document(firebaseListId).collection("items").get().addOnSuccessListener(snaps -> {
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : snaps) batch.delete(doc.getReference());
                    batch.commit().addOnCompleteListener(t -> {
                        if (listener != null) listener.onActionComplete();
                    });
                });
    }

    public void decoupleListAndReplaceItems(long listId, List<ShoppingItem> newItems) {
        dbHelper.decoupleListAndReplaceItems(listId, newItems);
    }

    public interface OnMemberAddListener {
        void onMemberAdded();
        void onMemberAlreadyExists();
        void onError(String message);
    }

    public void addMemberToList(String firebaseListId, String userId, OnMemberAddListener listener) {
        if (!userRepository.isUserVerified()) {
            listener.onError("Email not verified.");
            return;
        }
        db.collection("shopping_lists").document(firebaseListId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        @SuppressWarnings("unchecked")
                        List<String> members = (List<String>) documentSnapshot.get("members");
                        @SuppressWarnings("unchecked")
                        List<String> pending = (List<String>) documentSnapshot.get("pending_members");
                        if ((members != null && members.contains(userId)) || (pending != null && pending.contains(userId))) {
                            listener.onMemberAlreadyExists();
                        } else {
                            db.collection("shopping_lists").document(firebaseListId)
                                    .update("pending_members", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                                    .addOnSuccessListener(aVoid -> listener.onMemberAdded())
                                    .addOnFailureListener(e -> listener.onError(e.getMessage()));
                        }
                    } else {
                        listener.onError("Liste nicht gefunden.");
                    }
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public void acceptInvitation(String firebaseListId, UserRepository.OnProfileActionListener listener) {
        if (!userRepository.isUserVerified()) return;
        String uid = mAuth.getUid();
        if (uid == null) return;
        
        db.collection("shopping_lists").document(firebaseListId)
                .update("members", com.google.firebase.firestore.FieldValue.arrayUnion(uid),
                        "pending_members", com.google.firebase.firestore.FieldValue.arrayRemove(uid))
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public void declineInvitation(String firebaseListId, UserRepository.OnProfileActionListener listener) {
        if (!userRepository.isUserVerified()) return;
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("shopping_lists").document(firebaseListId)
                .update("pending_members", com.google.firebase.firestore.FieldValue.arrayRemove(uid))
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public void leaveList(String firebaseListId, UserRepository.OnProfileActionListener listener) {
        String uid = mAuth.getUid();
        if (uid == null || !userRepository.isUserVerified()) return;
        
        db.collection("shopping_lists").document(firebaseListId)
                .update("members", com.google.firebase.firestore.FieldValue.arrayRemove(uid))
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public interface OnMembersLoadedListener {
        void onLoaded(List<Map<String, String>> membersWithNames);
        void onError(String error);
    }

    public com.google.firebase.firestore.ListenerRegistration getMembersWithNames(String firebaseListId, OnMembersLoadedListener listener) {
        return db.collection("shopping_lists").document(firebaseListId).addSnapshotListener((doc, e) -> {
            if (e != null) {
                // Ignore PERMISSION_DENIED as it often happens during auth state transitions
                if (e.getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    return;
                }
                listener.onError(e.getMessage());
                return;
            }
            if (doc == null || !doc.exists()) {
                listener.onError("Liste nicht gefunden.");
                return;
            }
            @SuppressWarnings("unchecked")
            List<String> memberIds = (List<String>) doc.get("members");
            @SuppressWarnings("unchecked")
            List<String> pendingIds = (List<String>) doc.get("pending_members");
            
            List<String> allIds = new ArrayList<>();
            if (memberIds != null) allIds.addAll(memberIds);
            if (pendingIds != null) allIds.addAll(pendingIds);

            if (allIds.isEmpty()) {
                listener.onLoaded(new ArrayList<>());
                return;
            }

            List<Map<String, String>> result = new ArrayList<>();
            java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(0);
            
            for (String uid : allIds) {
                db.collection("users").document(uid).get()
                    .addOnSuccessListener(userDoc -> {
                        Map<String, String> memberInfo = new HashMap<>();
                        memberInfo.put("uid", uid);
                        if (userDoc.exists()) {
                            String username = userDoc.getString("username");
                            String email = userDoc.getString("email");
                            memberInfo.put("username", username != null ? username : (email != null ? email : "Unbekannt"));
                            memberInfo.put("profileImageUrl", userDoc.getString("profileImageUrl"));
                        } else {
                            memberInfo.put("username", "Unbekannt");
                        }
                        
                        String role = context.getString(R.string.member_role_member);
                        if (uid.equals(doc.getString("ownerId"))) role = context.getString(R.string.member_role_owner);
                        else if (pendingIds != null && pendingIds.contains(uid)) role = context.getString(R.string.member_role_invited);
                        
                        memberInfo.put("role", role);
                        result.add(memberInfo);
                        if (counter.incrementAndGet() == allIds.size()) {
                            // Sort result based on order in allIds to maintain consistency (e.g. join order)
                            result.sort((m1, m2) -> {
                                int idx1 = allIds.indexOf(m1.get("uid"));
                                int idx2 = allIds.indexOf(m2.get("uid"));
                                return Integer.compare(idx1, idx2);
                            });
                            listener.onLoaded(result);
                        }
                    })
                    .addOnFailureListener(err -> {
                        if (counter.incrementAndGet() == allIds.size()) {
                            // Even on partial failure, sort what we have
                            result.sort((m1, m2) -> {
                                int idx1 = allIds.indexOf(m1.get("uid"));
                                int idx2 = allIds.indexOf(m2.get("uid"));
                                return Integer.compare(idx1, idx2);
                            });
                            listener.onLoaded(result);
                        }
                    });
            }
        });
    }

    public String getCurrentUserId() {
        return mAuth.getUid();
    }

    public void updateListTimestamp(String firebaseListId, OnActionListener listener) {
        if (!userRepository.isUserVerified()) {
            if (listener != null) listener.onActionComplete();
            return;
        }
        db.collection("shopping_lists").document(firebaseListId).update("lastModified", com.google.firebase.firestore.FieldValue.serverTimestamp())
            .addOnCompleteListener(task -> {
                if (listener != null) listener.onActionComplete();
            });
    }

    public void deleteAllUserData(Runnable onComplete) {
         FirebaseUser user = mAuth.getCurrentUser();
         if (user == null) {
             onComplete.run();
             return;
         }
         String uid = user.getUid();
         
         // Query all lists where user is a member (this includes lists they own)
         db.collection("shopping_lists").whereArrayContains("members", uid).get()
             .addOnSuccessListener(snaps -> {
                 List<com.google.firebase.firestore.DocumentSnapshot> docs = snaps.getDocuments();
                 cleanupNextListRecursive(docs, 0, uid, onComplete);
             })
             .addOnFailureListener(e -> onComplete.run());
    }

    private void cleanupNextListRecursive(List<com.google.firebase.firestore.DocumentSnapshot> docs, int index, String uid, Runnable onComplete) {
        if (index >= docs.size()) {
            onComplete.run();
            return;
        }

        com.google.firebase.firestore.DocumentSnapshot doc = docs.get(index);
        String ownerId = doc.getString("ownerId");
        
        if (uid.equals(ownerId)) {
            // User is owner -> Delete list and items
            deleteListInternal(doc.getReference(), () -> cleanupNextListRecursive(docs, index + 1, uid, onComplete));
        } else {
            // User is member -> Remove from members array
            doc.getReference().update("members", com.google.firebase.firestore.FieldValue.arrayRemove(uid))
                .addOnCompleteListener(t -> {
                     cleanupNextListRecursive(docs, index + 1, uid, onComplete);
                });
        }
    }

    private void deleteListInternal(com.google.firebase.firestore.DocumentReference listRef, Runnable onDone) {
        listRef.collection("items").get().addOnSuccessListener(itemSnaps -> {
            com.google.firebase.firestore.WriteBatch batch = db.batch();
            for(com.google.firebase.firestore.DocumentSnapshot item : itemSnaps) {
                batch.delete(item.getReference());
            }
            batch.commit().addOnCompleteListener(t -> {
                listRef.delete().addOnCompleteListener(t2 -> onDone.run());
            });
        }).addOnFailureListener(e -> {
             // If items fetch fails, try deleting list anyway
             listRef.delete().addOnCompleteListener(t2 -> onDone.run());
        });
    }

    private void deleteNextListRecursive(List<com.google.firebase.firestore.DocumentSnapshot> docs, int index, Runnable onComplete) {
        if (index >= docs.size()) {
            onComplete.run();
            return;
        }
        deleteListInternal(docs.get(index).getReference(), () -> deleteNextListRecursive(docs, index + 1, onComplete));
    }

    public void migrateLocalListsToCloud(Runnable onComplete) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.isAnonymous()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        if (!userRepository.isUserVerified()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        // Check for permanent provider
        boolean hasPermanentProvider = false;
        for (com.google.firebase.auth.UserInfo profile : user.getProviderData()) {
            String pid = profile.getProviderId();
            if (com.google.firebase.auth.EmailAuthProvider.PROVIDER_ID.equals(pid) || 
                com.google.firebase.auth.GoogleAuthProvider.PROVIDER_ID.equals(pid)) {
                hasPermanentProvider = true;
                break;
            }
        }
        
        if (!hasPermanentProvider) {
            if (onComplete != null) onComplete.run();
            return;
        }

        List<ShoppingList> localLists = dbHelper.getAllShoppingLists();
        List<ShoppingList> listsToUpload = new ArrayList<>();
        for (ShoppingList list : localLists) {
            // Only upload lists that don't have a Firebase ID yet
            if (list.getFirebaseId() == null || list.getFirebaseId().isEmpty()) {
                listsToUpload.add(list);
            }
        }

        if (listsToUpload.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        uploadNextList(listsToUpload, 0, user, onComplete);
    }

    private void uploadNextList(List<ShoppingList> lists, int index, FirebaseUser user, Runnable onComplete) {
        if (index >= lists.size()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        ShoppingList list = lists.get(index);
        List<ShoppingItem> items = dbHelper.getAllItems(list.getId());

        // Generate ID locally
        com.google.firebase.firestore.DocumentReference docRef = db.collection("shopping_lists").document();
        String firebaseId = docRef.getId();

        // Update local object and DB immediately BEFORE upload to prevent race condition with listeners
        list.setFirebaseId(firebaseId);
        list.setOwnerId(user.getUid());
        list.setMembers(java.util.Collections.singletonList(user.getUid()));
        dbHelper.updateLocalListAfterMigration(list);

        Map<String, Object> listData = new HashMap<>();
        listData.put("name", list.getName());
        listData.put("ownerId", user.getUid());
        listData.put("isShared", false);
        listData.put("members", java.util.Collections.singletonList(user.getUid()));
        listData.put("pending_members", new ArrayList<>());
        listData.put("lastModified", com.google.firebase.firestore.FieldValue.serverTimestamp());

        docRef.set(listData)
            .addOnSuccessListener(aVoid -> {
                if (items.isEmpty()) {
                     uploadNextList(lists, index + 1, user, onComplete);
                } else {
                     com.google.firebase.firestore.WriteBatch batch = db.batch();
                     for (ShoppingItem item : items) {
                         com.google.firebase.firestore.DocumentReference itemRef = docRef.collection("items").document();
                         item.setFirebaseId(itemRef.getId());
                         // Ensure we don't upload local ID
                         Map<String, Object> itemData = new HashMap<>();
                         itemData.put("name", item.getName());
                         itemData.put("quantity", item.getQuantity());
                         itemData.put("unit", item.getUnit());
                         itemData.put("done", item.isDone());
                         itemData.put("notes", item.getNotes());
                         itemData.put("position", item.getPosition());
                         
                         batch.set(itemRef, itemData);
                     }
                     batch.commit().addOnCompleteListener(t -> {
                         uploadNextList(lists, index + 1, user, onComplete);
                     });
                }
            })
            .addOnFailureListener(e -> {
                Log.e("Repo", "Failed to upload list " + list.getName(), e);
                // If upload fails, we might want to revert the local firebaseId?
                // For now, let's keep it or maybe set it to null?
                // If we keep it, it might be "stuck" in syncing state without being on server.
                // Safest might be to revert if it failed completely.
                list.setFirebaseId(null);
                dbHelper.updateShoppingListFirebaseId(list.getId(), null);
                
                uploadNextList(lists, index + 1, user, onComplete);
            });
    }

    public void uploadSingleListToCloud(ShoppingList list, Runnable onComplete) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        List<ShoppingList> singleList = new ArrayList<>();
        singleList.add(list);
        uploadNextList(singleList, 0, user, onComplete);
    }

    public void deleteSingleListFromCloud(String firebaseId, Runnable onComplete) {
        db.collection("shopping_lists").document(firebaseId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    List<com.google.firebase.firestore.DocumentSnapshot> list = new ArrayList<>();
                    list.add(doc);
                    deleteNextListRecursive(list, 0, onComplete);
                } else {
                    if (onComplete != null) onComplete.run();
                }
            })
            .addOnFailureListener(e -> {
                if (onComplete != null) onComplete.run();
            });
    }

    public void unsyncAllLists(Runnable onComplete) {
        List<ShoppingList> allLists = dbHelper.getAllShoppingLists();
        List<ShoppingList> syncedLists = new ArrayList<>();
        for (ShoppingList list : allLists) {
            if (list.getFirebaseId() != null) {
                syncedLists.add(list);
            }
        }
        
        if (syncedLists.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        unsyncNextListRecursive(syncedLists, 0, onComplete);
    }

    private void unsyncNextListRecursive(List<ShoppingList> lists, int index, Runnable onComplete) {
        if (index >= lists.size()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        ShoppingList list = lists.get(index);
        String firebaseId = list.getFirebaseId();

        // 1. Fetch items from cloud one last time to be sure we have latest (optional but good practice)
        // For simplicity and speed, and since we are "forcing" unsync, we might skip fetch and just keep local if we assume they are in sync.
        // But to be safe like 'performSafeUnsync', let's fetch.
        fetchItemsFromCloudOneTime(firebaseId, (cloudItems, hasPending) -> {
             // 2. Decouple local list
             // We reuse decouple logic but we need to ensure we use the fetched items
             decoupleListAndReplaceItems(list.getId(), cloudItems);
             
             // 3. Delete from cloud
             deleteSingleListFromCloud(firebaseId, () -> {
                 unsyncNextListRecursive(lists, index + 1, onComplete);
             });
        });
    }

    public void updateProfileImage(String url, UserRepository.OnProfileActionListener listener) {
        if (!userRepository.isUserVerified()) return;
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("profileImageUrl", url);
        
        if (url == null) {
             Map<String, Object> deleteData = new HashMap<>();
             deleteData.put("profileImageUrl", com.google.firebase.firestore.FieldValue.delete());
             db.collection("users").document(mAuth.getUid())
                .update(deleteData)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
        } else {
            db.collection("users").document(mAuth.getUid())
                    .set(updateData, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> listener.onSuccess())
                    .addOnFailureListener(e -> listener.onError(e.getMessage()));
        }
    }
}