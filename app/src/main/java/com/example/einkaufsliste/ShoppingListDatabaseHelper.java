package com.example.einkaufsliste;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ShoppingListDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "shoppinglist.db";
    private static final int DATABASE_VERSION = 2; // neue Version mit sort_order

    public ShoppingListDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Erstelle Tabellen mit Spalte sort_order
        db.execSQL("CREATE TABLE IF NOT EXISTS shopping_lists (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "sort_order INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS shopping_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "list_id INTEGER NOT NULL, " +
                "completed INTEGER DEFAULT 0, " +
                "sort_order INTEGER DEFAULT 0, " +
                "FOREIGN KEY(list_id) REFERENCES shopping_lists(id) ON DELETE CASCADE)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Für dieses Beispiel werden die Tabellen bei Upgrade neu erstellt.
        db.execSQL("DROP TABLE IF EXISTS shopping_items");
        db.execSQL("DROP TABLE IF EXISTS shopping_lists");
        onCreate(db);
    }
}
