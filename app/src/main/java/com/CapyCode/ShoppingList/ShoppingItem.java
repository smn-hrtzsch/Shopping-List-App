package com.CapyCode.ShoppingList;

public class ShoppingItem {
    private long id;
    private String name;
    private String quantity;
    private String unit;
    private boolean isDone; // isChecked wurde zu isDone geändert für Klarheit
    private long listId;   // Sicherstellen, dass dies long ist
    private String notes;
    private int position; // Für manuelle Sortierung
    private String firebaseId;

    // Default constructor required for calls to DataSnapshot.getValue(ShoppingItem.class)
    public ShoppingItem() {
    }

    // Konstruktor für das Erstellen aus der Datenbank
    public ShoppingItem(long id, String name, String quantity, String unit, boolean isDone, long listId, String notes, int position) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.isDone = isDone;
        this.listId = listId;
        this.notes = notes != null ? notes : "";
        this.position = position;
    }

    // Konstruktor für das Erstellen neuer Items (ID wird von der DB generiert)
    // Position wird oft initial auf 0 oder Ende der Liste gesetzt
    public ShoppingItem(String name, String quantity, String unit, boolean isDone, long listId, String notes, int position) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.isDone = isDone;
        this.listId = listId;
        this.notes = notes != null ? notes : "";
        this.position = position;
    }


    // Getter und Setter
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public boolean isDone() { return isDone; }
    public void setDone(boolean done) { isDone = done; }

    public long getListId() { return listId; }
    public void setListId(long listId) { this.listId = listId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public String getFirebaseId() { return firebaseId; }
    public void setFirebaseId(String firebaseId) { this.firebaseId = firebaseId; }
}