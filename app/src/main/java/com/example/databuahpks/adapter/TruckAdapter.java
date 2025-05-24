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
    public List<Truck> truckList;
    public OnDeleteClickListener deleteListener;

    public interface OnDeleteClickListener {
        void onDelete(String kodeTruck);
    }

    public TruckAdapter(List<Truck> truckList, OnDeleteClickListener deleteListener) {
        this.truckList = truckList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public TruckAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_truck, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TruckAdapter.ViewHolder holder, int position) {
        Truck truck = truckList.get(position);
        holder.txtKodeTruck.setText(truck.getKodeTruck());
        holder.txtNamaPengemudi.setText(truck.getNamaPengemudi());
    }

    @Override
    public int getItemCount() {
        return truckList.size();
    }

    public Truck getItem(int position) {
        return truckList.get(position);
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
