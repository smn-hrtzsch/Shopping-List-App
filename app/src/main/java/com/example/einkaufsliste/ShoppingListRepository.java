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

    public ShoppingListRepository(Context context) {
        dbHelper = new ShoppingListDatabaseHelper(context);
    }

    public long addShoppingList(String name) {
        long id = -1;
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            Cursor cursor = db.rawQuery("SELECT MAX(sort_order) FROM shopping_lists", null);
            int order = 0;
            if (cursor.moveToFirst()) {
                order = cursor.getInt(0) + 1;
            }
            cursor.close();

            ContentValues values = new ContentValues();
            values.put("name", name);
            values.put("sort_order", order);
            id = db.insertOrThrow("shopping_lists", null, values);
        } catch (SQLiteConstraintException e) {
            Log.e(TAG, "Constraint violation while adding shopping list: " + name, e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while adding shopping list: " + name, e);
        }
        return id;
    }

    public long addShoppingItem(long listId, String itemName) {
        long id = -1;
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            Cursor cursor = db.rawQuery("SELECT MAX(sort_order) FROM shopping_items WHERE list_id = ?", new String[]{String.valueOf(listId)});
            int order = 0;
            if (cursor.moveToFirst()) {
                order = cursor.getInt(0) + 1;
            }
            cursor.close();

            ContentValues values = new ContentValues();
            values.put("list_id", listId);
            values.put("name", itemName);
            values.put("completed", 0);
            values.put("sort_order", order);
            id = db.insertOrThrow("shopping_items", null, values);
        } catch (SQLiteConstraintException e) {
            Log.e(TAG, "Constraint violation while adding shopping item: " + itemName + " to list " + listId, e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while adding shopping item: " + itemName, e);
        }
        return id;
    }

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

    public List<ShoppingList> getAllShoppingLists() {
        List<ShoppingList> shoppingLists = new ArrayList<>();
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.query("shopping_lists", null, null, null, null, null, "sort_order ASC")) {

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    int sortOrder = cursor.getInt(cursor.getColumnIndexOrThrow("sort_order"));
                    shoppingLists.add(new ShoppingList(id, name, sortOrder));
                } while (cursor.moveToNext());
            } else {
                Log.i(TAG, "No shopping lists found in the database.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving shopping lists", e);
        }
        return shoppingLists;
    }

    // ACHTUNG: Hier wurde der ORDER BY angepasst!
    public List<ShoppingItem> getItemsForList(long listId) {
        List<ShoppingItem> items = new ArrayList<>();
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.query("shopping_items", null, "list_id = ?",
                     new String[]{String.valueOf(listId)}, null, null, "completed ASC, sort_order ASC")) {

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    boolean completed = cursor.getInt(cursor.getColumnIndexOrThrow("completed")) > 0;
                    int sortOrder = cursor.getInt(cursor.getColumnIndexOrThrow("sort_order"));
                    items.add(new ShoppingItem(id, name, completed, sortOrder));
                } while (cursor.moveToNext());
            } else {
                Log.i(TAG, "No items found for shopping list with id: " + listId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving items for shopping list with id: " + listId, e);
        }
        return items;
    }

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

    public void updateShoppingListsOrder(List<ShoppingList> lists) {
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()){
            db.beginTransaction();
            try {
                for (int i = 0; i < lists.size(); i++) {
                    ContentValues values = new ContentValues();
                    values.put("sort_order", i);
                    db.update("shopping_lists", values, "id = ?", new String[]{String.valueOf(lists.get(i).getId())});
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating shopping lists order", e);
        }
    }

    public void updateShoppingItemsOrder(long listId, List<ShoppingItem> items) {
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()){
            db.beginTransaction();
            try {
                for (int i = 0; i < items.size(); i++) {
                    ContentValues values = new ContentValues();
                    values.put("sort_order", i);
                    db.update("shopping_items", values, "id = ?", new String[]{String.valueOf(items.get(i).getId())});
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating shopping items order", e);
        }
    }

    public void updateShoppingItemName(long id, String newName) {
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("name", newName);
            int rowsAffected = db.update("shopping_items", values, "id = ?", new String[]{String.valueOf(id)});
            if (rowsAffected == 0) {
                Log.w(TAG, "No shopping item found with id: " + id);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating shopping item name with id: " + id, e);
        }
    }

    public void updateShoppingListName(long id, String newName) {
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("name", newName);
            int rowsAffected = db.update("shopping_lists", values, "id = ?", new String[]{String.valueOf(id)});
            if (rowsAffected == 0) {
                Log.w(TAG, "No shopping list found with id: " + id);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating shopping list name with id: " + id, e);
        }
    }
}
