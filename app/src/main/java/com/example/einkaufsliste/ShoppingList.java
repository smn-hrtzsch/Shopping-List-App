package com.example.einkaufsliste;

import java.util.ArrayList;
import java.util.List;

public class ShoppingList {
    private long id;
    private String name;
    private List<ShoppingItem> items;

    public ShoppingList(long id, String name) {
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
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
