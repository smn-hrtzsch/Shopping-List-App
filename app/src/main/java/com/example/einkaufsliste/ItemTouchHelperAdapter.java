package com.example.einkaufsliste;

public interface ItemTouchHelperAdapter {
    boolean onItemMove(int fromPosition, int toPosition);
    void onItemDismiss(int position); // Diese Zeile hinzufügen
}