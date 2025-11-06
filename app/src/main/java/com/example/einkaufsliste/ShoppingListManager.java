package com.example.einkaufsliste;

import android.content.Context;
import java.util.List;

public class ShoppingListManager {
    private ShoppingListRepository repository;

    public interface OnListsLoadedListener {
        void onListsLoaded(List<ShoppingList> shoppingLists);
    }

    public ShoppingListManager(Context context) {
        repository = new ShoppingListRepository(context);
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