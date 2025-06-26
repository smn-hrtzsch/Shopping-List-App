// ShoppingListRepository.java
package com.example.einkaufsliste;

// import android.content.ContentValues; // Nicht mehr direkt hier gebraucht
import android.content.Context;
// import android.database.Cursor; // Nicht mehr direkt hier gebraucht
// import android.database.sqlite.SQLiteDatabase; // Nicht mehr direkt hier gebraucht
import android.util.Log;

import java.util.ArrayList;
// import java.util.Collections; // Nicht mehr direkt hier gebraucht
import java.util.List;

public class ShoppingListRepository {
    private ShoppingListDatabaseHelper dbHelper;
    private Context context; // context kann nützlich sein, wird aber aktuell nicht direkt verwendet

    public ShoppingListRepository(Context context) {
        this.dbHelper = new ShoppingListDatabaseHelper(context);
        this.context = context;
    }

    public void updateListPositions(List<ShoppingList> lists) {
        dbHelper.updateListPositions(lists);
    }

    public List<ShoppingItem> getItemsForListId(long listId) { // Parameter zu long geändert
        if (listId == -1L) {
            Log.e("ShoppingListRepository", "Ungültige Listen-ID (-1) beim Abrufen von Items.");
            return new ArrayList<>();
        }
        // Die Sortierung sollte idealerweise in der DB-Abfrage erfolgen oder hier konsistent angewendet werden
        // dbHelper.getAllItems(listId) sortiert bereits nach Position und dann isDone
        return dbHelper.getAllItems(listId); // getAllItems statt getItemsForList
    }

    public List<ShoppingList> getAllShoppingLists() {
        return dbHelper.getAllShoppingLists();
    }

    // Parameter zu long geändert für Konsistenz
    public ShoppingList getShoppingListById(long listId) {
        return dbHelper.getShoppingList(listId); // getShoppingList statt getShoppingListById
    }

    public long addShoppingList(String listName) {
        return dbHelper.addShoppingList(listName);
    }

    // updateShoppingList war in deinem Code, wird aber nicht direkt von den Activities genutzt,
    // stattdessen updateShoppingListName aus ShoppingListManager.
    // Falls du eine komplette Listenobjekt-Aktualisierung brauchst, kann diese Methode bleiben.
    // Fürs Erste kommentiere ich sie aus, da sie nicht verwendet wird und dbHelper.updateShoppingList fehlt.
    /*
    public void updateShoppingList(ShoppingList shoppingList) {
        // dbHelper.updateShoppingList(shoppingList); // Diese Methode fehlt im DBHelper
    }
    */

    public void deleteShoppingList(long listId) { // Parameter zu long
        dbHelper.deleteShoppingList(listId);
    }

    // Nimmt jetzt long listId und das ShoppingItem Objekt
    public long addItemToShoppingList(long listId, ShoppingItem item) {
        item.setListId(listId); // Sicherstellen, dass die ListId im Item gesetzt ist
        // Position muss hier ggf. noch bestimmt werden (z.B. Anzahl der aktuellen Items)
        // oder der DBHelper kümmert sich darum.
        // Fürs Erste übergeben wir das Item direkt.
        return dbHelper.addItem(item); // addItem statt der alten Variante
    }

    public void updateItemInList(ShoppingItem item) {
        dbHelper.updateItem(item);
    }

    public void updateItemPositions(List<ShoppingItem> items) {
        dbHelper.updateItemPositionsBatch(items); // Neue Batch-Methode im DBHelper
    }

    public void deleteItemFromList(long itemId) { // Parameter zu long
        dbHelper.deleteItem(itemId);
    }

    public void toggleItemChecked(long itemId, boolean isChecked) { // Parameter zu long
        ShoppingItem item = dbHelper.getItem(itemId); // getItem statt getItemById
        if (item != null) {
            item.setDone(isChecked);
            dbHelper.updateItem(item);
        }
    }

    public void clearCheckedItemsFromList(long listId) { // Parameter zu long
        dbHelper.deleteCheckedItems(listId); // deleteCheckedItems statt deleteCheckedItemsFromList
    }

    public void clearAllItemsFromList(long listId) { // Parameter zu long
        dbHelper.clearList(listId); // clearList statt deleteAllItemsFromList
    }
}