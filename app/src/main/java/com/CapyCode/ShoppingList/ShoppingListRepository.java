package com.CapyCode.ShoppingList;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

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
        // 1. Immediately return all local lists (including cached cloud lists)
        listener.onListsLoaded(dbHelper.getAllShoppingLists());

        // 2. Get shared lists from Firestore
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String userId = currentUser.getUid();
        db.collection("shopping_lists")
                .whereArrayContains("members", userId)
                .addSnapshotListener((value, e) -> {
                    if (e != null) {
                        Log.w("Firestore", "Listen for failed.", e);
                        // On error, we just keep showing what we have from local DB
                        return;
                    }

                    if (value == null || value.isEmpty()) {
                        // No shared lists found on server, clear any locally cached cloud lists
                        dbHelper.deleteObsoleteCloudLists(new ArrayList<>());
                        listener.onListsLoaded(dbHelper.getAllShoppingLists());
                        return;
                    }

                    int listCount = value.size();
                    java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(0);
                    List<String> activeFirebaseIds = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : value) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        String ownerId = doc.getString("ownerId");
                        @SuppressWarnings("unchecked")
                        List<String> members = (List<String>) doc.get("members");

                        ShoppingList list = new ShoppingList(name);
                        list.setFirebaseId(id);
                        list.setOwnerId(ownerId);
                        list.setMembers(members);
                        
                        activeFirebaseIds.add(id);

                        // Fetch item count for each list
                        db.collection("shopping_lists").document(id).collection("items").get()
                                .addOnSuccessListener(items -> {
                                    list.setItemCount(items.size());
                                    // Update local cache for this list
                                    dbHelper.upsertCloudList(list);
                                    
                                    if (counter.incrementAndGet() == listCount) {
                                        // All lists processed
                                        // Remove lists that are no longer in the cloud result
                                        dbHelper.deleteObsoleteCloudLists(activeFirebaseIds);
                                        // Return updated full list from DB
                                        listener.onListsLoaded(dbHelper.getAllShoppingLists());
                                    }
                                })
                                .addOnFailureListener(failure -> {
                                    Log.w("Firestore", "Error fetching items for count", failure);
                                    // Even if counting fails, we upsert the list (count might be 0 or old)
                                    // Optionally we could try to read old count from DB, but for now 0 is safe fallback
                                    list.setItemCount(0); 
                                    dbHelper.upsertCloudList(list);
                                    
                                    if (counter.incrementAndGet() == listCount) {
                                        dbHelper.deleteObsoleteCloudLists(activeFirebaseIds);
                                        listener.onListsLoaded(dbHelper.getAllShoppingLists());
                                    }
                                });
                    }
                });
    }

    public void getItemsForListId(long listId, OnItemsLoadedListener listener) {
        if (listId == -1L) {
            Log.e("ShoppingListRepository", "Ungültige Listen-ID (-1) beim Abrufen von Items.");
            listener.onItemsLoaded(new ArrayList<>());
            return;
        }
        listener.onItemsLoaded(dbHelper.getAllItems(listId));
    }

    public void getItemsForListId(String firebaseListId, OnItemsLoadedListener listener) {
        db.collection("shopping_lists").document(firebaseListId).collection("items")
                .orderBy("position")
                .addSnapshotListener((value, e) -> {
                    if (e != null) {
                        Log.w("Firestore", "Listen for items failed.", e);
                        listener.onItemsLoaded(new ArrayList<>());
                        return;
                    }

                    List<ShoppingItem> items = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        ShoppingItem item = doc.toObject(ShoppingItem.class);
                        item.setFirebaseId(doc.getId());
                        items.add(item);
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
            db.collection("shopping_lists").document(list.getFirebaseId())
                    .update("name", list.getName());
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
                .whereEqualTo("done", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                    }
                });
    }

    public void clearAllItemsFromList(long listId) {
        dbHelper.clearList(listId);
    }

    public void clearAllItemsFromList(String firebaseListId) {
        db.collection("shopping_lists").document(firebaseListId).collection("items")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                    }
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
                        @SuppressWarnings("unchecked")
                        List<String> members = (List<String>) documentSnapshot.get("members");
                        if (members != null && members.contains(userId)) {
                            listener.onMemberAlreadyExists();
                        } else {
                            db.collection("shopping_lists").document(firebaseListId)
                                    .update("members", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                                    .addOnSuccessListener(aVoid -> listener.onMemberAdded())
                                    .addOnFailureListener(e -> listener.onError(e.getMessage()));
                        }
                    } else {
                        listener.onError("Liste nicht gefunden.");
                    }
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public void updateListTimestamp(String firebaseListId) {
        db.collection("shopping_lists").document(firebaseListId).update("lastModified", com.google.firebase.firestore.FieldValue.serverTimestamp());
    }
}