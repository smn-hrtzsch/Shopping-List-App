package com.CapyCode.ShoppingList;

public interface ItemTouchHelperAdapter {
    boolean onItemMove(int fromPosition, int toPosition);
    void onItemDismiss(int position); // Diese Zeile hinzufügen
}