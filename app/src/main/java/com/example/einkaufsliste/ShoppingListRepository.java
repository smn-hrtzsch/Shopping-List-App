package com.example.einkaufsliste;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ShoppingListRepository {

    private static final String TAG = "ShoppingListRepository";
    private SQLiteOpenHelper dbHelper;

    public ShoppingListRepository(Context context) {
        dbHelper = new ShoppingListDatabaseHelper(context);
    }

    public long addShoppingList(String name) {
        long id = -1;
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("name", name);
            id = db.insert("shopping_lists", null, values);
        } catch (Exception e) {
            Log.e(TAG, "Error adding shopping list", e);
        } finally {
            if (db != null) {
                db.close();
            }
        }
        return id;
    }

    public long addShoppingItem(long listId, String itemName) {
        long id = -1;
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("list_id", listId);
            values.put("name", itemName);
            values.put("completed", 0); // 0 für false, 1 für true
            id = db.insert("shopping_items", null, values);
        } catch (Exception e) {
            Log.e(TAG, "Error adding shopping item", e);
        } finally {
            if (db != null) {
                db.close();
            }
        }
        return id;
    }

    public void deleteShoppingItem(long id) {
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            db.delete("shopping_items", "id = ?", new String[]{String.valueOf(id)});
        } catch (Exception e) {
            Log.e(TAG, "Error deleting shopping item", e);
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    public void deleteShoppingList(long id) {
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            db.delete("shopping_items", "list_id = ?", new String[]{String.valueOf(id)});
            db.delete("shopping_lists", "id = ?", new String[]{String.valueOf(id)});
        } catch (Exception e) {
            Log.e(TAG, "Error deleting shopping list", e);
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    public List<ShoppingList> getAllShoppingLists() {
        List<ShoppingList> shoppingLists = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.query("shopping_lists", null, null, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    shoppingLists.add(new ShoppingList(id, name));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting all shopping lists", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
        return shoppingLists;
    }

    public List<ShoppingItem> getItemsForList(long listId) {
        List<ShoppingItem> items = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        int position = 0;

        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.query("shopping_items", null, "list_id = ?", new String[]{String.valueOf(listId)}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    boolean completed = cursor.getInt(cursor.getColumnIndexOrThrow("completed")) > 0;
                    items.add(new ShoppingItem(id, name, completed, position));
                    position++;
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting items for list", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
        return items;
    }

    public void updateShoppingItemStatus(long id, boolean completed) {
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("completed", completed ? 1 : 0);
            db.update("shopping_items", values, "id = ?", new String[]{String.valueOf(id)});
        } catch (Exception e) {
            Log.e(TAG, "Error updating shopping item status", e);
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }
}
