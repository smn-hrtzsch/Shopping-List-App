package com.CapyCode.ShoppingList;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

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
    @Exclude
    public long getId() { return id; }
    @Exclude
    public void setId(long id) { this.id = id; }

    @PropertyName("name")
    public String getName() { return name; }
    @PropertyName("name")
    public void setName(String name) { this.name = name; }

    @PropertyName("quantity")
    public String getQuantity() { return quantity; }
    @PropertyName("quantity")
    public void setQuantity(String quantity) { this.quantity = quantity; }

    @PropertyName("unit")
    public String getUnit() { return unit; }
    @PropertyName("unit")
    public void setUnit(String unit) { this.unit = unit; }

    @PropertyName("done")
    public boolean isDone() { return isDone; }
    @PropertyName("done")
    public void setDone(boolean done) { isDone = done; }

    @Exclude
    public long getListId() { return listId; }
    @Exclude
    public void setListId(long listId) { this.listId = listId; }

    @PropertyName("notes")
    public String getNotes() { return notes; }
    @PropertyName("notes")
    public void setNotes(String notes) { this.notes = notes; }

    @PropertyName("position")
    public int getPosition() { return position; }
    @PropertyName("position")
    public void setPosition(int position) { this.position = position; }

    @Exclude
    public String getFirebaseId() { return firebaseId; }
    @Exclude
    public void setFirebaseId(String firebaseId) { this.firebaseId = firebaseId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShoppingItem that = (ShoppingItem) o;
        
        // If both have firebaseId, compare them
        if (firebaseId != null && that.firebaseId != null) return firebaseId.equals(that.firebaseId);
        // If both have id, compare them
        if (id != 0 && that.id != 0) return id == that.id;
        
        // Fallback for optimistic items: if one has neither, compare by name and listId
        // This prevents flickering when the cloud version with a firebaseId arrives
        return name != null && name.equals(that.name) && listId == that.listId;
    }

    @Override
    public int hashCode() {
        if (firebaseId != null) return firebaseId.hashCode();
        if (id != 0) return (int) (id ^ (id >>> 32));
        return name != null ? name.hashCode() : super.hashCode();
    }
}