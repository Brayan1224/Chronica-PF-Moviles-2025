package com.example.chronicav1.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chronicav1.R;
import com.example.chronicav1.models.Entry;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador para mostrar la lista de entradas
 * VERSIÓN CON SOPORTE PARA BASE64
 */
public class EntryAdapter extends RecyclerView.Adapter<EntryAdapter.EntryViewHolder> {

    private Context context;
    private List<Entry> entryList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Entry entry);
    }

    // ✅ Constructor correcto - inicializa entryList
    public EntryAdapter(Context context, List<Entry> entryList) {
        this.context = context;
        // Protección contra null
        this.entryList = entryList != null ? entryList : new ArrayList<>();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public EntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_entry, parent, false);
        return new EntryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntryViewHolder holder, int position) {
        Entry entry = entryList.get(position);

        holder.tvEntryTitle.setText(entry.getTitle());
        holder.tvEntryPreview.setText(entry.getPreview());
        holder.tvEntryDate.setText(entry.getDate());
        holder.tvEntryLocation.setText(entry.getLocation());

        // Cargar imagen desde Base64 si existe
        if (entry.getImageBase64() != null && !entry.getImageBase64().isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(entry.getImageBase64(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.ivEntryImage.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
                holder.ivEntryImage.setImageResource(R.drawable.ic_logo);
            }
        } else {
            holder.ivEntryImage.setImageResource(R.drawable.ic_logo);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(entry);
            }
        });
    }

    @Override
    public int getItemCount() {
        return entryList != null ? entryList.size() : 0;
    }

    static class EntryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivEntryImage;
        TextView tvEntryTitle;
        TextView tvEntryPreview;
        TextView tvEntryDate;
        TextView tvEntryLocation;

        public EntryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivEntryImage = itemView.findViewById(R.id.ivEntryImage);
            tvEntryTitle = itemView.findViewById(R.id.tvEntryTitle);
            tvEntryPreview = itemView.findViewById(R.id.tvEntryPreview);
            tvEntryDate = itemView.findViewById(R.id.tvEntryDate);
            tvEntryLocation = itemView.findViewById(R.id.tvEntryLocation);
        }
    }

    public void updateData(List<Entry> newEntries) {
        this.entryList = newEntries != null ? newEntries : new ArrayList<>();
        notifyDataSetChanged();
    }
}