package com.CapyCode.ShoppingList;

import java.util.ArrayList;
import java.util.List;

public class ShoppingList {
    private long id;
    private String name;
    private List<ShoppingItem> items;
    private int itemCount;
    private int position;

    // Firebase-related fields
    private String firebaseId;
    private String ownerId;
    private String ownerUsername;
    private List<String> members;
    private List<String> pendingMembers;

    private boolean isShared;

    public ShoppingList(long id, String name) {
        this.id = id;
        this.name = name;
        this.items = new ArrayList<>();
        this.itemCount = 0;
        this.position = 0;
        this.members = new ArrayList<>();
        this.pendingMembers = new ArrayList<>();
        this.isShared = false;
    }

    public ShoppingList(String name) {
        this.name = name;
        this.items = new ArrayList<>();
        this.itemCount = 0;
        this.members = new ArrayList<>();
        this.pendingMembers = new ArrayList<>();
        this.isShared = false;
    }

    // --- Getter und Setter ---
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<ShoppingItem> getItems() { return items; }
    public void setItems(List<ShoppingItem> items) { this.items = items; }
    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public String getFirebaseId() { return firebaseId; }
    public void setFirebaseId(String firebaseId) { this.firebaseId = firebaseId; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }

    public List<String> getPendingMembers() { return pendingMembers; }
    public void setPendingMembers(List<String> pendingMembers) { this.pendingMembers = pendingMembers; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public boolean isShared() { return isShared; }
    public void setShared(boolean shared) { isShared = shared; }

    public boolean isCurrentUserPending(String userId) {
        return pendingMembers != null && pendingMembers.contains(userId);
    }

    public boolean isOwner(String userId) {
        return ownerId != null && ownerId.equals(userId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShoppingList that = (ShoppingList) o;
        if (firebaseId != null && that.firebaseId != null) return firebaseId.equals(that.firebaseId);
        if (id != -1 && that.id != -1) return id == that.id;
        return name != null && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        if (firebaseId != null) return firebaseId.hashCode();
        if (id != -1) return (int) (id ^ (id >>> 32));
        return name != null ? name.hashCode() : super.hashCode();
    }
}