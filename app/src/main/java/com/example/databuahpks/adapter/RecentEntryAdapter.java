package com.example.databuahpks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecentEntryAdapter extends RecyclerView.Adapter<RecentEntryAdapter.ViewHolder> {
    private final List<BuahEntry> entries;

    public static class BuahEntry {
        String kodeTruck;
        double lolosPercentage;
        boolean isLolos;

        BuahEntry(String kodeTruck, double lolosPercentage, boolean isLolos) {
            this.kodeTruck = kodeTruck;
            this.lolosPercentage = lolosPercentage;
            this.isLolos = isLolos;
        }
    }

    public RecentEntryAdapter(List<BuahEntry> entries) {
        this.entries = entries;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BuahEntry entry = entries.get(position);
        holder.txtEntry.setText(String.format("🚚 Truk %s: %.1f%% (%s)",
                entry.kodeTruck, entry.lolosPercentage, entry.isLolos ? "✅" : "❌"));
        // Color indicator
        holder.txtEntry.setTextColor(entry.isLolos ? 0xFF4CAF50 : 0xFFF44336);
    }

    @Override
    public int getItemCount() {
        return Math.min(entries.size(), 5); // Limit to 5 entries
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtEntry;

        ViewHolder(View itemView) {
            super(itemView);
            txtEntry = itemView.findViewById(R.id.txtEntry);
        }
    }
}