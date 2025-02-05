package com.example.einkaufsliste;

import java.util.ArrayList;
import java.util.List;

public class ShoppingList {
    private long id;
    private String name;
    private int sortOrder; // für persistente Reihenfolge
    private List<ShoppingItem> items;

    public ShoppingList(long id, String name, int sortOrder) {
        this.id = id;
        this.name = name;
        this.sortOrder = sortOrder;
        this.items = new ArrayList<>();
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    // Neuer Setter für den Namen:
    public void setName(String name) {
        this.name = name;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public List<ShoppingItem> getItems() {
        return items;
    }

    public void addItem(ShoppingItem item) {
        items.add(item);
    }

    public void removeItem(ShoppingItem item) {
        items.remove(item);
    }

    public void clearItems() {
        items.clear();
    }

    public void setItems(List<ShoppingItem> items) {
        this.items = items;
    }
}
