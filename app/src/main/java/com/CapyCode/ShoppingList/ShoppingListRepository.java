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

    public interface OnListsLoadedListener {
        void onListsLoaded(List<ShoppingList> shoppingLists);
    }

    public interface OnItemsLoadedListener {
        void onItemsLoaded(List<ShoppingItem> shoppingItems);
    }

    public ShoppingListRepository(Context context) {
        this.dbHelper = new ShoppingListDatabaseHelper(context);
        this.db = FirebaseFirestore.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
        this.context = context;
    }

    public int getLocalListCount() {
        return dbHelper.getShoppingListCount();
    }

    public int getNextPosition() {
        return dbHelper.getMaxPosition() + 1;
    }

    public void getAllShoppingLists(OnListsLoadedListener listener) {
        listener.onListsLoaded(dbHelper.getAllShoppingLists());

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        
        com.google.firebase.firestore.EventListener<com.google.firebase.firestore.QuerySnapshot> syncListener = (value, e) -> {
            if (e != null) {
                Log.w("Firestore", "Listen for lists failed.", e);
                return;
            }
            refreshAllListsFromServer(userId, listener);
        };

        db.collection("shopping_lists").whereArrayContains("members", userId).addSnapshotListener(syncListener);
        db.collection("shopping_lists").whereArrayContains("pending_members", userId).addSnapshotListener(syncListener);
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
            listener.onListsLoaded(dbHelper.getAllShoppingLists());
            return;
        }

        int listCount = docs.size();
        java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(0);
        List<String> activeFirebaseIds = new ArrayList<>();

        for (QueryDocumentSnapshot doc : docs) {
            String id = doc.getId();
            String name = doc.getString("name");
            String ownerId = doc.getString("ownerId");
            @SuppressWarnings("unchecked")
            List<String> members = (List<String>) doc.get("members");
            @SuppressWarnings("unchecked")
            List<String> pendingMembers = (List<String>) doc.get("pending_members");

            ShoppingList list = new ShoppingList(name);
            list.setFirebaseId(id);
            list.setOwnerId(ownerId);
            list.setMembers(members != null ? members : new ArrayList<>());
            list.setPendingMembers(pendingMembers != null ? pendingMembers : new ArrayList<>());
            
            activeFirebaseIds.add(id);

            // Fetch owner name
            db.collection("users").document(ownerId).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        list.setOwnerUsername(userDoc.getString("username"));
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
                                listener.onListsLoaded(dbHelper.getAllShoppingLists());
                            }
                        })
                        .addOnFailureListener(failure -> {
                            list.setItemCount(0); 
                            dbHelper.upsertCloudList(list);
                            if (counter.incrementAndGet() == listCount) {
                                dbHelper.deleteObsoleteCloudLists(activeFirebaseIds);
                                listener.onListsLoaded(dbHelper.getAllShoppingLists());
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
                                listener.onListsLoaded(dbHelper.getAllShoppingLists());
                            }
                        });
                });
        }
    }

    public void getItemsForListId(long listId, OnItemsLoadedListener listener) {
        listener.onItemsLoaded(dbHelper.getAllItems(listId));
    }

    public com.google.firebase.firestore.ListenerRegistration getItemsForListId(String firebaseListId, OnItemsLoadedListener listener) {
        return db.collection("shopping_lists").document(firebaseListId).collection("items")
                .orderBy("position")
                .addSnapshotListener((value, e) -> {
                    if (e != null) {
                        listener.onItemsLoaded(new ArrayList<>());
                        return;
                    }
                    List<ShoppingItem> items = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            ShoppingItem item = doc.toObject(ShoppingItem.class);
                            item.setFirebaseId(doc.getId());
                            items.add(item);
                        }
                    }
                    listener.onItemsLoaded(items);
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

    public long addItemToShoppingList(long listId, ShoppingItem item) {
        item.setListId(listId);
        return dbHelper.addItem(item);
    }

    public void addItemToShoppingList(String firebaseListId, ShoppingItem item) {
        db.collection("shopping_lists").document(firebaseListId).collection("items").add(item);
    }

    public void updateItemInList(ShoppingItem item, String firebaseListId) {
        if (firebaseListId != null && item.getFirebaseId() != null) {
            db.collection("shopping_lists").document(firebaseListId).collection("items").document(item.getFirebaseId()).set(item);
        } else {
            dbHelper.updateItem(item);
        }
    }

    public void updateItemPositions(List<ShoppingItem> items) {
        dbHelper.updateItemPositionsBatch(items);
    }

    public void updateShoppingList(ShoppingList list) {
        if (list.getFirebaseId() != null) {
            db.collection("shopping_lists").document(list.getFirebaseId()).update("name", list.getName());
        } else {
            dbHelper.updateShoppingList(list);
        }
    }

    public void deleteItemFromList(long itemId) {
        dbHelper.deleteItem(itemId);
    }

    public void deleteItemFromList(String firebaseListId, String firebaseItemId) {
        db.collection("shopping_lists").document(firebaseListId).collection("items").document(firebaseItemId).delete();
    }

    public void toggleItemChecked(long itemId, boolean isChecked) {
        ShoppingItem item = dbHelper.getItem(itemId);
        if (item != null) {
            item.setDone(isChecked);
            dbHelper.updateItem(item);
        }
    }

    public void toggleItemChecked(String firebaseListId, String firebaseItemId, boolean isChecked) {
        db.collection("shopping_lists").document(firebaseListId).collection("items").document(firebaseItemId).update("done", isChecked);
    }

    public void clearCheckedItemsFromList(long listId) {
        dbHelper.deleteCheckedItems(listId);
    }

    public void clearCheckedItemsFromList(String firebaseListId) {
        db.collection("shopping_lists").document(firebaseListId).collection("items")
                .whereEqualTo("done", true).get().addOnSuccessListener(snaps -> {
                    for (QueryDocumentSnapshot doc : snaps) doc.getReference().delete();
                });
    }

    public void clearAllItemsFromList(long listId) {
        dbHelper.clearList(listId);
    }

    public void clearAllItemsFromList(String firebaseListId) {
        db.collection("shopping_lists").document(firebaseListId).collection("items").get().addOnSuccessListener(snaps -> {
                    for (QueryDocumentSnapshot doc : snaps) doc.getReference().delete();
                });
    }

    public interface OnMemberAddListener {
        void onMemberAdded();
        void onMemberAlreadyExists();
        void onError(String message);
    }

    public void addMemberToList(String firebaseListId, String userId, OnMemberAddListener listener) {
        db.collection("shopping_lists").document(firebaseListId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> members = (List<String>) documentSnapshot.get("members");
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
        String userId = getCurrentUserId();
        if (userId == null) return;
        db.collection("shopping_lists").document(firebaseListId)
                .update("members", com.google.firebase.firestore.FieldValue.arrayUnion(userId),
                        "pending_members", com.google.firebase.firestore.FieldValue.arrayRemove(userId))
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public void declineInvitation(String firebaseListId, UserRepository.OnProfileActionListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) return;
        db.collection("shopping_lists").document(firebaseListId)
                .update("pending_members", com.google.firebase.firestore.FieldValue.arrayRemove(userId))
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public void leaveList(String firebaseListId, UserRepository.OnProfileActionListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) return;
        db.collection("shopping_lists").document(firebaseListId)
                .update("members", com.google.firebase.firestore.FieldValue.arrayRemove(userId))
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public interface OnMembersLoadedListener {
        void onLoaded(List<Map<String, String>> membersWithNames);
        void onError(String error);
    }

    public void getMembersWithNames(String firebaseListId, OnMembersLoadedListener listener) {
        db.collection("shopping_lists").document(firebaseListId).get()
            .addOnSuccessListener(doc -> {
                if (!doc.exists()) {
                    listener.onError("Liste nicht gefunden.");
                    return;
                }
                List<String> memberIds = (List<String>) doc.get("members");
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
                            memberInfo.put("username", userDoc.exists() ? userDoc.getString("username") : "Unbekannt");
                            
                            String role = "Mitglied";
                            if (uid.equals(doc.getString("ownerId"))) role = "Besitzer";
                            else if (pendingIds != null && pendingIds.contains(uid)) role = "Eingeladen";
                            
                            memberInfo.put("role", role);
                            result.add(memberInfo);
                            if (counter.incrementAndGet() == allIds.size()) {
                                listener.onLoaded(result);
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (counter.incrementAndGet() == allIds.size()) {
                                listener.onLoaded(result);
                            }
                        });
                }
            });
    }

    public String getCurrentUserId() {
        return mAuth.getUid();
    }

    public void updateListTimestamp(String firebaseListId) {
        db.collection("shopping_lists").document(firebaseListId).update("lastModified", com.google.firebase.firestore.FieldValue.serverTimestamp());
    }

    public void migrateLocalListsToCloud(Runnable onComplete) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
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

        Map<String, Object> listData = new HashMap<>();
        listData.put("name", list.getName());
        listData.put("ownerId", user.getUid());
        listData.put("members", java.util.Collections.singletonList(user.getUid()));
        listData.put("pending_members", new ArrayList<>());
        listData.put("lastModified", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("shopping_lists").add(listData)
            .addOnSuccessListener(docRef -> {
                String firebaseId = docRef.getId();
                list.setFirebaseId(firebaseId);
                list.setOwnerId(user.getUid());
                list.setMembers(java.util.Collections.singletonList(user.getUid()));
                
                dbHelper.upsertCloudList(list);

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
                uploadNextList(lists, index + 1, user, onComplete);
            });
    }

    public void deleteAllUserData(Runnable onComplete) {
         FirebaseUser user = mAuth.getCurrentUser();
         if (user == null) {
             onComplete.run();
             return;
         }
         
         db.collection("shopping_lists").whereEqualTo("ownerId", user.getUid()).get()
             .addOnSuccessListener(snaps -> {
                 List<com.google.firebase.firestore.DocumentSnapshot> docs = snaps.getDocuments();
                 deleteNextListRecursive(docs, 0, onComplete);
             })
             .addOnFailureListener(e -> onComplete.run());
    }

    private void deleteNextListRecursive(List<com.google.firebase.firestore.DocumentSnapshot> docs, int index, Runnable onComplete) {
        if (index >= docs.size()) {
            onComplete.run();
            return;
        }
        
        com.google.firebase.firestore.DocumentReference listRef = docs.get(index).getReference();
        
        listRef.collection("items").get().addOnSuccessListener(itemSnaps -> {
            com.google.firebase.firestore.WriteBatch batch = db.batch();
            for(com.google.firebase.firestore.DocumentSnapshot item : itemSnaps) {
                batch.delete(item.getReference());
            }
            batch.commit().addOnCompleteListener(t -> {
                listRef.delete().addOnCompleteListener(t2 -> {
                     deleteNextListRecursive(docs, index + 1, onComplete);
                });
            });
        }).addOnFailureListener(e -> {
             // If items fetch fails, try deleting list anyway
             listRef.delete().addOnCompleteListener(t2 -> {
                  deleteNextListRecursive(docs, index + 1, onComplete);
             });
        });
    }
}