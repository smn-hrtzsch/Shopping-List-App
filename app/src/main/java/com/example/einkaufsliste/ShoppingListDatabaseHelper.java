// ShoppingListDataBaseHelper.java
package com.example.einkaufsliste;

import com.example.einkaufsliste.R;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase; // Sehr wichtig!
import android.database.sqlite.SQLiteOpenHelper; // Sehr wichtig!
import android.database.sqlite.SQLiteException; // Für Fehlerbehandlung
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

// ... Rest deiner ShoppingListDatabaseHelper Klasse

public class ShoppingListDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "shopping_list.db";
    // Datenbankversion erhöhen, wenn Schema-Änderungen persistent gemacht werden sollen,
    // die nicht nur Korrekturen sind. Für die aktuellen Korrekturen ist es nicht zwingend.
    private static final int DATABASE_VERSION = 2; // Beibehalten von deiner Version

    public static final String TABLE_LISTS = "shopping_lists";
    public static final String TABLE_ITEMS = "shopping_items";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_LIST_NAME = "name";
    public static final String COLUMN_LIST_ITEM_COUNT = "item_count"; // Aus deiner DB-Version 2
    public static final String COLUMN_ITEM_NAME = "name";
    public static final String COLUMN_ITEM_QUANTITY = "quantity";
    public static final String COLUMN_ITEM_UNIT = "unit";
    public static final String COLUMN_ITEM_IS_DONE = "is_done";
    public static final String COLUMN_ITEM_LIST_ID = "list_id";
    public static final String COLUMN_ITEM_NOTES = "notes";
    public static final String COLUMN_ITEM_POSITION = "position";

    private static final String SQL_CREATE_TABLE_LISTS =
            "CREATE TABLE " + TABLE_LISTS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_LIST_NAME + " TEXT NOT NULL," +
                    COLUMN_LIST_ITEM_COUNT + " INTEGER DEFAULT 0);";

    private static final String SQL_CREATE_TABLE_ITEMS =
            "CREATE TABLE " + TABLE_ITEMS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_ITEM_NAME + " TEXT NOT NULL," +
                    COLUMN_ITEM_QUANTITY + " TEXT," +
                    COLUMN_ITEM_UNIT + " TEXT," +
                    COLUMN_ITEM_IS_DONE + " INTEGER DEFAULT 0," +
                    COLUMN_ITEM_LIST_ID + " INTEGER," + // Sollte long sein, aber INTEGER in SQLite ist flexibel
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
        if (oldVersion < 2 && newVersion >= 2) { // Nur ausführen, wenn von Version < 2 auf >= 2 upgegradet wird
            Log.d("DBUpgrade", "Upgrading database from version " + oldVersion + " to " + newVersion);
            // Add item_count column if it doesn't exist (idempotent check would be better, but for simplicity...)
            try {
                db.execSQL("ALTER TABLE " + TABLE_LISTS + " ADD COLUMN " + COLUMN_LIST_ITEM_COUNT + " INTEGER DEFAULT 0;");
                Log.d("DBUpgrade", COLUMN_LIST_ITEM_COUNT + " column added to " + TABLE_LISTS);
            } catch (SQLiteException e) {
                Log.w("DBUpgrade", COLUMN_LIST_ITEM_COUNT + " column likely already exists.", e);
            }

            Log.d("DBUpgrade", "Initializing item counts for existing lists.");
            Cursor listCursor = db.rawQuery("SELECT " + COLUMN_ID + " FROM " + TABLE_LISTS, null);
            if (listCursor != null) {
                while (listCursor.moveToNext()) {
                    long listId = listCursor.getLong(listCursor.getColumnIndexOrThrow(COLUMN_ID));
                    Cursor itemCountCursor = null;
                    try {
                        itemCountCursor = db.rawQuery("SELECT COUNT(" + COLUMN_ID + ") FROM " + TABLE_ITEMS + " WHERE " + COLUMN_ITEM_LIST_ID + " = ?", new String[]{String.valueOf(listId)});
                        if (itemCountCursor != null && itemCountCursor.moveToFirst()) {
                            int count = itemCountCursor.getInt(0);
                            ContentValues values = new ContentValues();
                            values.put(COLUMN_LIST_ITEM_COUNT, count);
                            db.update(TABLE_LISTS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(listId)});
                            Log.d("DBUpgrade", "List ID: " + listId + " updated with item count: " + count);
                        }
                    } catch (Exception e) {
                        Log.e("DBUpgrade", "Error updating item count for list ID: " + listId, e);
                    } finally {
                        if (itemCountCursor != null && !itemCountCursor.isClosed()) {
                            itemCountCursor.close();
                        }
                    }
                }
                listCursor.close();
            } else {
                Log.d("DBUpgrade", "No lists found to update item counts.");
            }
        }
        // Weitere Upgrades hier hinzufügen, falls DATABASE_VERSION > 2
    }


    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // --- Listen-Methoden ---
    public long addShoppingList(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LIST_NAME, name);
        values.put(COLUMN_LIST_ITEM_COUNT, 0); // Standardmäßig 0 Artikel
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

    public ShoppingList getShoppingList(long listId) { // Parameter zu long
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_LISTS,
                    new String[]{COLUMN_ID, COLUMN_LIST_NAME, COLUMN_LIST_ITEM_COUNT},
                    COLUMN_ID + "=?",
                    new String[]{String.valueOf(listId)}, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                ShoppingList list = new ShoppingList(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LIST_NAME))
                );
                list.setItemCount(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LIST_ITEM_COUNT)));
                return list;
            }
        } finally {
            if (cursor != null) cursor.close();
            // db.close(); // Nicht hier schließen, wenn von Repository aufgerufen, das mehrere Operationen machen könnte
        }
        return null;
    }

    public List<ShoppingList> getAllShoppingLists() {
        List<ShoppingList> shoppingLists = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_LISTS, null, null, null, null, null, COLUMN_LIST_NAME + " ASC"); // Sortieren nach Name
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    ShoppingList shoppingList = new ShoppingList(
                            cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LIST_NAME))
                    );
                    shoppingList.setItemCount(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LIST_ITEM_COUNT)));
                    shoppingLists.add(shoppingList);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
            // db.close();
        }
        return shoppingLists;
    }

    public int updateShoppingListName(long listId, String newName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LIST_NAME, newName);
        int rows = 0;
        try {
            rows = db.update(TABLE_LISTS, values, COLUMN_ID + " = ?",
                    new String[]{String.valueOf(listId)});
        } finally {
            db.close();
        }
        return rows;
    }

    public void deleteShoppingList(long listId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.delete(TABLE_LISTS, COLUMN_ID + " = ?",
                    new String[]{String.valueOf(listId)}); // Kaskadierendes Löschen sollte Items entfernen
        } finally {
            db.close();
        }
    }

    // --- Item-Methoden ---
    public long addItem(ShoppingItem item) { // Nimmt ShoppingItem Objekt
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

            // Automatische Positionierung: neue Items ans Ende der nicht-erledigten
            if (!item.isDone()){
                Cursor c = db.rawQuery("SELECT MAX(" + COLUMN_ITEM_POSITION + ") FROM " + TABLE_ITEMS +
                                " WHERE " + COLUMN_ITEM_LIST_ID + " = ? AND " + COLUMN_ITEM_IS_DONE + " = 0",
                        new String[]{String.valueOf(item.getListId())});
                int maxPos = -1;
                if(c.moveToFirst()){
                    maxPos = c.getInt(0);
                }
                c.close();
                values.put(COLUMN_ITEM_POSITION, maxPos + 1);
            } else {
                // Erledigte Items könnten ans Ende aller Items oder Ende der erledigten Items
                // Fürs Erste: Position wird wie übergeben oder 0, falls nicht gesetzt.
                values.put(COLUMN_ITEM_POSITION, item.getPosition()); // oder eine Logik für erledigte
            }

            newId = db.insertOrThrow(TABLE_ITEMS, null, values);
            if (newId != -1) {
                updateListItemCount(db, item.getListId(), 1); // Interner Aufruf, DB bleibt offen
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


    public ShoppingItem getItem(long itemId) { // Parameter zu long
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_ITEMS, null, COLUMN_ID + "=?",
                    new String[]{String.valueOf(itemId)}, null, null, null);
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
            // db.close();
        }
        return null;
    }


    public List<ShoppingItem> getAllItems(long listId) { // Parameter zu long
        List<ShoppingItem> items = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            // Sortierung: is_done ASC (unerledigte zuerst), dann position ASC
            cursor = db.query(TABLE_ITEMS, null,
                    COLUMN_ITEM_LIST_ID + "=?", new String[]{String.valueOf(listId)},
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
            // db.close();
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
            rows = db.update(TABLE_ITEMS, values, COLUMN_ID + " = ?",
                    new String[]{String.valueOf(item.getId())});
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


    public void deleteItem(long itemId) { // Parameter zu long
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
                updateListItemCount(db, listId, -deletedRows); // Interne Methode, DB bleibt offen
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public int deleteCheckedItems(long listId) { // Parameter zu long
        SQLiteDatabase db = this.getWritableDatabase();
        int deletedRows = 0;
        db.beginTransaction();
        try {
            deletedRows = db.delete(TABLE_ITEMS, COLUMN_ITEM_LIST_ID + " = ? AND " + COLUMN_ITEM_IS_DONE + " = 1",
                    new String[]{String.valueOf(listId)});
            if (deletedRows > 0) {
                updateListItemCount(db, listId, -deletedRows); // Interne Methode
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
        return deletedRows;
    }

    public int clearList(long listId) { // Parameter zu long
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

    // Hilfsmethode, die eine offene DB erwartet (für Transaktionen)
    private void updateListItemCount(SQLiteDatabase db, long listId, int change) {
        Cursor currentCountCursor = null;
        try {
            currentCountCursor = db.query(TABLE_LISTS, new String[]{COLUMN_LIST_ITEM_COUNT}, COLUMN_ID + " = ?", new String[]{String.valueOf(listId)}, null, null, null);
            int currentCount = 0;
            if (currentCountCursor != null && currentCountCursor.moveToFirst()) {
                currentCount = currentCountCursor.getInt(currentCountCursor.getColumnIndexOrThrow(COLUMN_LIST_ITEM_COUNT));
            }

            int newCount = Math.max(0, currentCount + change); // Sicherstellen, dass der Zähler nicht negativ wird

            ContentValues values = new ContentValues();
            values.put(COLUMN_LIST_ITEM_COUNT, newCount);
            db.update(TABLE_LISTS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(listId)});
        } finally {
            if (currentCountCursor != null) currentCountCursor.close();
        }
    }
}