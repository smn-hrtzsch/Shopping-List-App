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
        Map<String, Integer> orderMap = new HashMap<>();
        if (orderString != null && !orderString.isEmpty()) {
            String[] orderedIds = orderString.split(",");
            for (int i = 0; i < orderedIds.length; i++) {
                orderMap.put(orderedIds[i], i);
            }
        }

        Collections.sort(lists, (l1, l2) -> {
            String id1 = l1.getFirebaseId() != null ? l1.getFirebaseId() : String.valueOf(l1.getId());
            String id2 = l2.getFirebaseId() != null ? l2.getFirebaseId() : String.valueOf(l2.getId());
            
            Integer pos1 = orderMap.get(id1);
            Integer pos2 = orderMap.get(id2);
            
            // If both are in manual order, use that
            if (pos1 != null && pos2 != null) {
                return pos1.compareTo(pos2);
            }
            
            // Items in manual order come before items not in manual order
            if (pos1 != null) return -1;
            if (pos2 != null) return 1;
            
            // Both are not in manual order (e.g. new lists), sort by DB position
            return Integer.compare(l1.getPosition(), l2.getPosition());
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

    public long addShoppingList(String name, int position, boolean isShared) {
        return repository.getShoppingListDatabaseHelper().addShoppingList(name, position, isShared);
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