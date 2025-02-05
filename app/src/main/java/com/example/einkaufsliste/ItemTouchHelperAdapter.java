package com.example.einkaufsliste;

public interface ItemTouchHelperAdapter {
    /**
     * Wird aufgerufen, wenn ein Element von einer Position zu einer anderen bewegt wurde.
     * @param fromPosition die alte Position
     * @param toPosition die neue Position
     * @return true, wenn die Bewegung verarbeitet wurde
     */
    boolean onItemMove(int fromPosition, int toPosition);
}
