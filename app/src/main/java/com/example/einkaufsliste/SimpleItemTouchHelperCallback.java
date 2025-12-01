package com.example.einkaufsliste;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class SimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private final ItemTouchHelperAdapter mAdapter;
    private boolean mSwipeEnabled;

    public SimpleItemTouchHelperCallback(ItemTouchHelperAdapter adapter) {
        mAdapter = adapter;
        // Standardmäßig Swipe für Artikellisten aktivieren, für Listenübersicht wird es in getMovementFlags ignoriert
        this.mSwipeEnabled = true;
    }

    // Optional: Methode, um Swipe zur Laufzeit zu (de-)aktivieren
    public void setSwipeEnabled(boolean enabled) {
        this.mSwipeEnabled = enabled;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        // Swipe nur erlauben, wenn mSwipeEnabled true ist UND der ViewHolder vom Typ ItemViewHolder ist
        // (um Swipe für die "Add-Item"-Zeile zu verhindern, falls diese kein ItemViewHolder ist)
        // Diese Logik wird besser in getMovementFlags gehandhabt. Hier geben wir einfach mSwipeEnabled zurück.
        return mSwipeEnabled;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = 0; // Standardmäßig kein Swipe

        if (viewHolder instanceof MyRecyclerViewAdapter.ItemViewHolder) { // Nur für echte Artikel-Items
            if (mSwipeEnabled) {
                swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
            }
        } else if (viewHolder instanceof ListRecyclerViewAdapter.ViewHolder) {
            // Kein Swipe für Listenübersicht-Items
            swipeFlags = 0;
        } else {
            // Für andere Viewholder (z.B. AddItemViewHolder) kein Drag und kein Swipe
            dragFlags = 0;
        }
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        if (viewHolder.getItemViewType() != target.getItemViewType()) {
            return false;
        }
        return mAdapter.onItemMove(viewHolder.getBindingAdapterPosition(), target.getBindingAdapterPosition());
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // Stelle sicher, dass onItemDismiss nur für Viewholder aufgerufen wird, die es unterstützen
        // und wenn Swipe für diesen Typ aktiviert ist (was durch getMovementFlags schon geregelt sein sollte)
        if (viewHolder instanceof MyRecyclerViewAdapter.ItemViewHolder || viewHolder instanceof ListRecyclerViewAdapter.ViewHolder) {
            if (mSwipeEnabled && viewHolder instanceof MyRecyclerViewAdapter.ItemViewHolder) { // Explizit nur für MyRecycler Items, wenn Swipe an ist
                mAdapter.onItemDismiss(viewHolder.getBindingAdapterPosition());
            } else if (viewHolder instanceof ListRecyclerViewAdapter.ViewHolder) {
                // Wenn du Swipe-to-Dismiss für Listen auch willst, hier die Logik oder den Aufruf
                // mAdapter.onItemDismiss(viewHolder.getAdapterPosition());
                // Aktuell ist es für Listen deaktiviert in getMovementFlags.
            }
        }
    }
}