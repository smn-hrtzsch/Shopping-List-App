package com.example.einkaufsliste;

public class ShoppingItem {
    private long id;
    private String name;
    private boolean completed;
    private int sortOrder; // für persistente Reihenfolge

    public ShoppingItem(long id, String name, boolean completed, int sortOrder) {
        this.id = id;
        this.name = name;
        this.completed = completed;
        this.sortOrder = sortOrder;
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

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
