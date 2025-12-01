package com.example.einkaufsliste;

import java.util.ArrayList;
import java.util.List;

public class ShoppingList {
    private long id;
    private String name;
    private List<ShoppingItem> items;
    private int itemCount;
    private int position; // NEU

    // Firebase-related fields
    private String firebaseId;
    private String ownerId;
    private List<String> members;

    public ShoppingList(long id, String name) {
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
        this.itemCount = 0;
        this.position = 0; // NEU
        this.members = new ArrayList<>();
    }

    public ShoppingList(String name) {
        this.name = name;
        this.items = new ArrayList<>();
        this.itemCount = 0;
        this.members = new ArrayList<>();
    }

    // --- Getter und Setter ---
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<ShoppingItem> getItems() { return items; }
    public void setItems(List<ShoppingItem> items) { this.items = items; }
    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }

    // NEU: Getter und Setter für Position
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    // Firebase-related getters and setters
    public String getFirebaseId() { return firebaseId; }
    public void setFirebaseId(String firebaseId) { this.firebaseId = firebaseId; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }

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