package com.CapyCode.ShoppingList;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShoppingListManager {
    private ShoppingListRepository repository;
    private SharedPreferences sharedPreferences;

    private static final String PREFS_NAME = "list_order_prefs";
    private static final String KEY_LIST_ORDER = "list_order";

    public interface OnListsLoadedListener {
        void onListsLoaded(List<ShoppingList> shoppingLists);
    }

    public ShoppingListManager(Context context) {
        repository = new ShoppingListRepository(context);
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveListOrder(List<ShoppingList> lists) {
        List<String> listIds = new ArrayList<>();
        for (ShoppingList list : lists) {
            if (list.getFirebaseId() != null) {
                listIds.add(list.getFirebaseId());
            } else {
                listIds.add(String.valueOf(list.getId()));
            }
        }
        String order = String.join(",", listIds);
        sharedPreferences.edit().putString(KEY_LIST_ORDER, order).apply();
    }

    public List<ShoppingList> sortListsBasedOnSavedOrder(List<ShoppingList> lists) {
        String orderString = sharedPreferences.getString(KEY_LIST_ORDER, null);
        if (orderString == null || orderString.isEmpty()) {
            return lists;
        }

        String[] orderedIds = orderString.split(",");
        Map<String, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < orderedIds.length; i++) {
            orderMap.put(orderedIds[i], i);
        }

        Collections.sort(lists, (l1, l2) -> {
            String id1 = l1.getFirebaseId() != null ? l1.getFirebaseId() : String.valueOf(l1.getId());
            String id2 = l2.getFirebaseId() != null ? l2.getFirebaseId() : String.valueOf(l2.getId());
            Integer pos1 = orderMap.get(id1);
            Integer pos2 = orderMap.get(id2);
            if (pos1 == null) pos1 = Integer.MAX_VALUE;
            if (pos2 == null) pos2 = Integer.MAX_VALUE;
            int comp = pos1.compareTo(pos2);
            if (comp != 0) {
                return comp;
            }
            // Fallback: Sort by name (case-insensitive) to have a stable order for unsorted lists
            return l1.getName().compareToIgnoreCase(l2.getName());
        });

        return lists;
    }

    public void updateListPositions(List<ShoppingList> lists) {
        repository.updateListPositions(lists);
    }

    public void getAllShoppingLists(OnListsLoadedListener listener) {
        repository.getAllShoppingLists(listener::onListsLoaded);
    }

    public ShoppingList getShoppingList(long listId) {
        return repository.getShoppingListById(listId);
    }

    public long addShoppingList(String name, int position) {
        return repository.addShoppingList(name, position);
    }

    public void updateShoppingList(ShoppingList list) {
        repository.updateShoppingList(list);
    }

    public void deleteShoppingList(ShoppingList list) {
        repository.deleteShoppingList(list);
    }

    public long addItemToShoppingList(long listId, ShoppingItem item) {
        return repository.addItemToShoppingList(listId, item);
    }
}