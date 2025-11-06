package com.example.einkaufsliste;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ShoppingListDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "shopping_list.db";
    private static final int DATABASE_VERSION = 3; // Version 3 wegen neuer 'position'-Spalte

    // Tabellen- und Spaltennamen
    public static final String TABLE_LISTS = "shopping_lists";
    public static final String TABLE_ITEMS = "shopping_items";
    public static final String COLUMN_ID = "_id";

    // Spalten für die Listentabelle
    public static final String COLUMN_LIST_NAME = "name";
    public static final String COLUMN_LIST_ITEM_COUNT = "item_count";
    public static final String COLUMN_LIST_POSITION = "position"; // Für die Reihenfolge

    // Spalten für die Artikeltabelle
    public static final String COLUMN_ITEM_NAME = "name";
    public static final String COLUMN_ITEM_QUANTITY = "quantity";
    public static final String COLUMN_ITEM_UNIT = "unit";
    public static final String COLUMN_ITEM_IS_DONE = "is_done";
    public static final String COLUMN_ITEM_LIST_ID = "list_id";
    public static final String COLUMN_ITEM_NOTES = "notes";
    public static final String COLUMN_ITEM_POSITION = "position"; // Für die Reihenfolge

    private static final String SQL_CREATE_TABLE_LISTS =
            "CREATE TABLE " + TABLE_LISTS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_LIST_NAME + " TEXT NOT NULL," +
                    COLUMN_LIST_ITEM_COUNT + " INTEGER DEFAULT 0," +
                    COLUMN_LIST_POSITION + " INTEGER DEFAULT 0);";

    private static final String SQL_CREATE_TABLE_ITEMS =
            "CREATE TABLE " + TABLE_ITEMS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_ITEM_NAME + " TEXT NOT NULL," +
                    COLUMN_ITEM_QUANTITY + " TEXT," +
                    COLUMN_ITEM_UNIT + " TEXT," +
                    COLUMN_ITEM_IS_DONE + " INTEGER DEFAULT 0," +
                    COLUMN_ITEM_LIST_ID + " INTEGER," +
                    COLUMN_ITEM_NOTES + " TEXT," +
                    COLUMN_ITEM_POSITION + " INTEGER DEFAULT 0," +
                    "FOREIGN KEY(" + COLUMN_ITEM_LIST_ID + ") REFERENCES " +
                    TABLE_LISTS + "(" + COLUMN_ID + ") ON DELETE CASCADE);";


    public ShoppingListDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_LISTS);
        db.execSQL(SQL_CREATE_TABLE_ITEMS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_LISTS + " ADD COLUMN " + COLUMN_LIST_ITEM_COUNT + " INTEGER DEFAULT 0;");
            } catch (SQLiteException e) {
                Log.w("DBUpgrade", "Spalte " + COLUMN_LIST_ITEM_COUNT + " existiert vermutlich bereits.", e);
            }
        }
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_LISTS + " ADD COLUMN " + COLUMN_LIST_POSITION + " INTEGER DEFAULT 0;");
                // Initialisiere die Positionen für bestehende Listen, falls nötig
                Cursor cursor = db.rawQuery("SELECT " + COLUMN_ID + " FROM " + TABLE_LISTS, null);
                if (cursor != null) {
                    int i = 0;
                    while (cursor.moveToNext()) {
                        long listId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                        ContentValues values = new ContentValues();
                        values.put(COLUMN_LIST_POSITION, i++);
                        db.update(TABLE_LISTS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(listId)});
                    }
                    cursor.close();
                }
            } catch (SQLiteException e) {
                Log.w("DBUpgrade", "Spalte " + COLUMN_LIST_POSITION + " existiert vermutlich bereits.", e);
            }
        }
    }


    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // --- Listen-Methoden ---
    public long addShoppingList(String name, int position) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LIST_NAME, name);
        values.put(COLUMN_LIST_ITEM_COUNT, 0);
        values.put(COLUMN_LIST_POSITION, position); // NEU: Position wird gesetzt
        long id = -1;
        try {
            id = db.insertOrThrow(TABLE_LISTS, null, values);
        } catch (Exception e) {
            Log.e("DBHelper", "Error adding shopping list", e);
        } finally {
            db.close();
        }
        return id;
    }

    public ShoppingList getShoppingList(long listId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_LISTS,
                    new String[]{COLUMN_ID, COLUMN_LIST_NAME, COLUMN_LIST_ITEM_COUNT, COLUMN_LIST_POSITION},
                    COLUMN_ID + "=?", new String[]{String.valueOf(listId)}, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                ShoppingList list = new ShoppingList(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LIST_NAME))
                );
                list.setItemCount(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LIST_ITEM_COUNT)));
                list.setPosition(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LIST_POSITION)));
                return list;
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    public List<ShoppingList> getAllShoppingLists() {
        List<ShoppingList> shoppingLists = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_LISTS, null, null, null, null, null, COLUMN_LIST_POSITION + " ASC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    ShoppingList shoppingList = new ShoppingList(
                            cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LIST_NAME))
                    );
                    shoppingList.setItemCount(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LIST_ITEM_COUNT)));
                    shoppingList.setPosition(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LIST_POSITION)));
                    shoppingLists.add(shoppingList);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return shoppingLists;
    }

    public void updateListPositions(List<ShoppingList> lists) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (ShoppingList list : lists) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_LIST_POSITION, list.getPosition());
                db.update(TABLE_LISTS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(list.getId())});
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DBHelper", "Error updating list positions", e);
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public int updateShoppingListName(long listId, String newName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LIST_NAME, newName);
        int rows = 0;
        try {
            rows = db.update(TABLE_LISTS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(listId)});
        } finally {
            db.close();
        }
        return rows;
    }

    public int updateShoppingList(ShoppingList list) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LIST_NAME, list.getName());
        int rows = 0;
        try {
            rows = db.update(TABLE_LISTS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(list.getId())});
        } finally {
            db.close();
        }
        return rows;
    }

    public void deleteShoppingList(long listId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.delete(TABLE_LISTS, COLUMN_ID + " = ?", new String[]{String.valueOf(listId)});
        } finally {
            db.close();
        }
    }

    // --- Item-Methoden ---
    public long addItem(ShoppingItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        long newId = -1;
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_ITEM_NAME, item.getName());
            values.put(COLUMN_ITEM_QUANTITY, item.getQuantity());
            values.put(COLUMN_ITEM_UNIT, item.getUnit());
            values.put(COLUMN_ITEM_IS_DONE, item.isDone() ? 1 : 0);
            values.put(COLUMN_ITEM_LIST_ID, item.getListId());
            values.put(COLUMN_ITEM_NOTES, item.getNotes());
            values.put(COLUMN_ITEM_POSITION, item.getPosition());

            newId = db.insertOrThrow(TABLE_ITEMS, null, values);
            if (newId != -1) {
                updateListItemCount(db, item.getListId(), 1);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DBHelper", "Error adding item", e);
        } finally {
            db.endTransaction();
            db.close();
        }
        return newId;
    }


    public ShoppingItem getItem(long itemId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_ITEMS, null, COLUMN_ID + "=?", new String[]{String.valueOf(itemId)}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return new ShoppingItem(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEM_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEM_QUANTITY)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEM_UNIT)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ITEM_IS_DONE)) == 1,
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ITEM_LIST_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEM_NOTES)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ITEM_POSITION))
                );
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }


    public List<ShoppingItem> getAllItems(long listId) {
        List<ShoppingItem> items = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_ITEMS, null, COLUMN_ITEM_LIST_ID + "=?", new String[]{String.valueOf(listId)},
                    null, null, COLUMN_ITEM_IS_DONE + " ASC, " + COLUMN_ITEM_POSITION + " ASC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    items.add(new ShoppingItem(
                            cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEM_NAME)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEM_QUANTITY)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEM_UNIT)),
                            cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ITEM_IS_DONE)) == 1,
                            cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ITEM_LIST_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEM_NOTES)),
                            cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ITEM_POSITION))
                    ));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return items;
    }

    public int updateItem(ShoppingItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ITEM_NAME, item.getName());
        values.put(COLUMN_ITEM_QUANTITY, item.getQuantity());
        values.put(COLUMN_ITEM_UNIT, item.getUnit());
        values.put(COLUMN_ITEM_IS_DONE, item.isDone() ? 1 : 0);
        values.put(COLUMN_ITEM_NOTES, item.getNotes());
        values.put(COLUMN_ITEM_POSITION, item.getPosition());
        int rows = 0;
        try {
            rows = db.update(TABLE_ITEMS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(item.getId())});
        } finally {
            db.close();
        }
        return rows;
    }

    public void updateItemPositionsBatch(List<ShoppingItem> items) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (ShoppingItem item : items) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_ITEM_POSITION, item.getPosition());
                db.update(TABLE_ITEMS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(item.getId())});
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DBHelper", "Error updating item positions in batch", e);
        } finally {
            db.endTransaction();
            db.close();
        }
    }


    public void deleteItem(long itemId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            long listId = -1;
            Cursor cursor = db.query(TABLE_ITEMS, new String[]{COLUMN_ITEM_LIST_ID}, COLUMN_ID + " = ?", new String[]{String.valueOf(itemId)}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                listId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ITEM_LIST_ID));
            }
            if (cursor != null) cursor.close();

            int deletedRows = db.delete(TABLE_ITEMS, COLUMN_ID + " = ?", new String[]{String.valueOf(itemId)});
            if (deletedRows > 0 && listId != -1) {
                updateListItemCount(db, listId, -deletedRows);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public int deleteCheckedItems(long listId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int deletedRows = 0;
        db.beginTransaction();
        try {
            deletedRows = db.delete(TABLE_ITEMS, COLUMN_ITEM_LIST_ID + " = ? AND " + COLUMN_ITEM_IS_DONE + " = 1",
                    new String[]{String.valueOf(listId)});
            if (deletedRows > 0) {
                updateListItemCount(db, listId, -deletedRows);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
        return deletedRows;
    }

    public int clearList(long listId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int deletedRows = 0;
        db.beginTransaction();
        try {
            deletedRows = db.delete(TABLE_ITEMS, COLUMN_ITEM_LIST_ID + " = ?", new String[]{String.valueOf(listId)});
            ContentValues values = new ContentValues();
            values.put(COLUMN_LIST_ITEM_COUNT, 0);
            db.update(TABLE_LISTS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(listId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
        return deletedRows;
    }

    private void updateListItemCount(SQLiteDatabase db, long listId, int change) {
        db.execSQL("UPDATE " + TABLE_LISTS + " SET " + COLUMN_LIST_ITEM_COUNT + " = " +
                COLUMN_LIST_ITEM_COUNT + " + " + change + " WHERE " + COLUMN_ID + " = " + listId);
    }
}