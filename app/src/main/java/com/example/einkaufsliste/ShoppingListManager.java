package com.example.einkaufsliste;

import android.content.Context;
import java.util.List;
import android.database.sqlite.SQLiteDatabase;

public class ShoppingListManager {
    private ShoppingListDatabaseHelper dbHelper;

    public ShoppingListManager(Context context) {
        dbHelper = new ShoppingListDatabaseHelper(context);
    }

    public List<ShoppingList> getAllShoppingLists() {
        return dbHelper.getAllShoppingLists();
    }

    public SQLiteDatabase getWritableDatabase() {
        return dbHelper.getWritableDatabase();
    }

    public ShoppingList getShoppingList(long listId) {
        return dbHelper.getShoppingList(listId);
    }

    public long addShoppingList(String name) {
        return dbHelper.addShoppingList(name);
    }

    public void updateShoppingListName(long listId, String newName) {
        dbHelper.updateShoppingListName(listId, newName);
    }

    public void deleteShoppingList(long listId) {
        dbHelper.deleteShoppingList(listId);
    }
}