package com.example.einkaufsliste;

import java.util.ArrayList;
import java.util.List;

public class ShoppingListManager {
    private static List<ShoppingList> shoppingLists = new ArrayList<>();

    public static List<ShoppingList> getShoppingLists() {
        return shoppingLists;
    }

    public static void addList(ShoppingList list) {
        shoppingLists.add(list);
    }

    public static void removeList(ShoppingList list) {
        shoppingLists.remove(list);
    }
}
