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
                if (memberIds == null || memberIds.isEmpty()) {
                    listener.onLoaded(new ArrayList<>());
                    return;
                }
                List<Map<String, String>> result = new ArrayList<>();
                java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(0);
                for (String uid : memberIds) {
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener(userDoc -> {
                            Map<String, String> memberInfo = new HashMap<>();
                            memberInfo.put("uid", uid);
                            memberInfo.put("username", userDoc.exists() ? userDoc.getString("username") : "Unbekannt");
                            memberInfo.put("role", uid.equals(doc.getString("ownerId")) ? "Besitzer" : "Mitglied");
                            result.add(memberInfo);
                            if (counter.incrementAndGet() == memberIds.size()) listener.onLoaded(result);
                        })
                        .addOnFailureListener(e -> {
                            if (counter.incrementAndGet() == memberIds.size()) listener.onLoaded(result);
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
}