package com.example.einkaufsliste;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ListRecyclerViewAdapter extends RecyclerView.Adapter<ListRecyclerViewAdapter.ViewHolder> {

    private List<ShoppingList> mData;
    private LayoutInflater mInflater;
    private ShoppingListRepository repository;
    private Context context;

    public ListRecyclerViewAdapter(Context context, List<ShoppingList> data, ShoppingListRepository repository) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.repository = repository;
        this.context = context;
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recyclerview_list_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShoppingList list = mData.get(position);
        holder.myTextView.setText(list.getName());

        holder.deleteButton.setOnClickListener(v -> {
            if (context instanceof MainActivity) {
                ((MainActivity) context).showDeleteListDialog(list);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, Einkaufsliste.class);
            intent.putExtra("list_name", list.getName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView myTextView;
        ImageView deleteButton;

        ViewHolder(View itemView) {
            super(itemView);
            myTextView = itemView.findViewById(R.id.listNameTextView);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
