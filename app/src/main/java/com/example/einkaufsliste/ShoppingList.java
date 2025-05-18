package com.example.einkaufsliste;

import java.util.ArrayList;
import java.util.List;

public class ShoppingList {
    private long id;
    private String name;
    private List<ShoppingItem> items; // Kann beibehalten werden, wird aber nicht direkt in DB als Blob gespeichert
    private int itemCount; // Für die Anzeige

    public ShoppingList(long id, String name) {
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>(); // Initialisieren, um NullPointerExceptions zu vermeiden
        this.itemCount = 0; // Standardwert
    }

    public ShoppingList(String name) {
        this.name = name;
        this.items = new ArrayList<>();
        this.itemCount = 0;
    }

    // Getter und Setter
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ShoppingItem> getItems() {
        return items;
    }

    public void setItems(List<ShoppingItem> items) {
        this.items = items;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public void addItem(ShoppingItem item) {
        this.items.add(item);
        // this.itemCount = this.items.size(); // Deaktiviert, da itemCount aus DB kommt
    }

    public void removeItem(ShoppingItem item) {
        this.items.remove(item);
        // this.itemCount = this.items.size(); // Deaktiviert
    }

    @Override
    public String toString() {
        return name; // Für Spinner oder einfache Textanzeigen
    }
}