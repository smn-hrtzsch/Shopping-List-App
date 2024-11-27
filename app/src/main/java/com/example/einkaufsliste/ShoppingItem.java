package com.example.einkaufsliste;

public class ShoppingItem {
    private long id;
    private String name;
    private boolean completed;
    private int originalPosition; // Fügen Sie dies hinzu

    public ShoppingItem(long id, String name, boolean completed, int originalPosition) {
        this.id = id;
        this.name = name;
        this.completed = completed;
        this.originalPosition = originalPosition; // Initialisieren Sie dies
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public int getOriginalPosition() {
        return originalPosition;
    }

    public void setOriginalPosition(int originalPosition) {
        this.originalPosition = originalPosition;
    }
}
