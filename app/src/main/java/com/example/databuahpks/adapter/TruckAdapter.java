package com.example.databuahpks.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.databuahpks.R;
import com.example.databuahpks.model.Truck;

import java.util.List;

public class TruckAdapter extends RecyclerView.Adapter<TruckAdapter.ViewHolder> {
    private List<Truck> truckList;

    public TruckAdapter(List<Truck> truckList) {
        this.truckList = truckList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_truck, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Truck truck = truckList.get(position);
        holder.txtKodeTruck.setText(truck.getKodeTruck() != null ? truck.getKodeTruck() : "Unknown");
        holder.txtNamaPengemudi.setText(truck.getNamaPengemudi() != null ? truck.getNamaPengemudi() : "Unknown");
    }

    @Override
    public int getItemCount() {
        return truckList.size();
    }

    public Truck getItem(int position) {
        return position >= 0 && position < truckList.size() ? truckList.get(position) : null;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtKodeTruck, txtNamaPengemudi;

        public ViewHolder(View itemView) {
            super(itemView);
            txtKodeTruck = itemView.findViewById(R.id.txtKodeTruck);
            txtNamaPengemudi = itemView.findViewById(R.id.txtNamaPengemudi);
        }
    }
}