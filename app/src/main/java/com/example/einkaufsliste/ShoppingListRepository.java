package com.example.einkaufsliste;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteConstraintException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ShoppingListRepository {

    private static final String TAG = "ShoppingListRepository";
    private final SQLiteOpenHelper dbHelper;

    // Constructor initializes the database helper
    public ShoppingListRepository(Context context) {
        dbHelper = new ShoppingListDatabaseHelper(context);
    }

    // Adds a new shopping list to the database
    public long addShoppingList(String name) {
        long id = -1;
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("name", name);
            id = db.insertOrThrow("shopping_lists", null, values); // Insert with exception handling
        } catch (SQLiteConstraintException e) {
            Log.e(TAG, "Constraint violation while adding shopping list: " + name, e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while adding shopping list: " + name, e);
        }
        return id; // Returns the ID of the newly created shopping list
    }

    // Adds a new item to a specific shopping list
    public long addShoppingItem(long listId, String itemName) {
        long id = -1;
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("list_id", listId);
            values.put("name", itemName);
            values.put("completed", 0); // Default value for completion is false (0)
            id = db.insertOrThrow("shopping_items", null, values);
        } catch (SQLiteConstraintException e) {
            Log.e(TAG, "Constraint violation while adding shopping item: " + itemName + " to list " + listId, e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while adding shopping item: " + itemName, e);
        }
        return id; // Returns the ID of the newly created shopping item
    }

    // Deletes a specific shopping item
    public void deleteShoppingItem(long id) {
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            int rowsAffected = db.delete("shopping_items", "id = ?", new String[]{String.valueOf(id)});
            if (rowsAffected == 0) {
                Log.w(TAG, "No shopping item found with id: " + id);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting shopping item with id: " + id, e);
        }
    }

    // Deletes a specific shopping list and all its items
    public void deleteShoppingList(long id) {
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            db.beginTransaction();
            try {
                db.delete("shopping_items", "list_id = ?", new String[]{String.valueOf(id)});
                int rowsAffected = db.delete("shopping_lists", "id = ?", new String[]{String.valueOf(id)});
                if (rowsAffected > 0) {
                    db.setTransactionSuccessful();
                } else {
                    Log.w(TAG, "No shopping list found with id: " + id);
                }
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting shopping list with id: " + id, e);
        }
    }

    // Retrieves all shopping lists from the database
    public List<ShoppingList> getAllShoppingLists() {
        List<ShoppingList> shoppingLists = new ArrayList<>();
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.query("shopping_lists", null, null, null, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    shoppingLists.add(new ShoppingList(id, name)); // Adds each shopping list to the result list
                } while (cursor.moveToNext());
            } else {
                Log.i(TAG, "No shopping lists found in the database.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving shopping lists", e);
        }
        return shoppingLists; // Returns the list of shopping lists
    }

    // Retrieves all items for a specific shopping list
    public List<ShoppingItem> getItemsForList(long listId) {
        List<ShoppingItem> items = new ArrayList<>();
        int position = 0;

        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.query("shopping_items", null, "list_id = ?",
                     new String[]{String.valueOf(listId)}, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    boolean completed = cursor.getInt(cursor.getColumnIndexOrThrow("completed")) > 0;
                    items.add(new ShoppingItem(id, name, completed, position));
                    position++;
                } while (cursor.moveToNext());
            } else {
                Log.i(TAG, "No items found for shopping list with id: " + listId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving items for shopping list with id: " + listId, e);
        }
        return items; // Returns the list of items
    }

    // Updates the completion status of a shopping item
    public void updateShoppingItemStatus(long id, boolean completed) {
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("completed", completed ? 1 : 0);
            int rowsAffected = db.update("shopping_items", values, "id = ?", new String[]{String.valueOf(id)});
            if (rowsAffected == 0) {
                Log.w(TAG, "No shopping item found with id: " + id);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating shopping item status with id: " + id, e);
        }
    }
}
