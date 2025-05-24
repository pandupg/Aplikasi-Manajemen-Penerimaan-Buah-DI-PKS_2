package com.example.databuahpks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;

public class BuahAdapter extends RecyclerView.Adapter<BuahAdapter.BuahViewHolder> {
    private ArrayList<HashMap<String, Object>> buahList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(HashMap<String, Object> buah);
        void onDeleteClick(HashMap<String, Object> buah);
    }

    public BuahAdapter(ArrayList<HashMap<String, Object>> buahList, OnItemClickListener listener) {
        this.buahList = buahList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BuahViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_buah, parent, false);
        return new BuahViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BuahViewHolder holder, int position) {
        HashMap<String, Object> buah = buahList.get(position);
        holder.bind(buah);
    }

    @Override
    public int getItemCount() {
        return buahList.size();
    }

    public class BuahViewHolder extends RecyclerView.ViewHolder {
        TextView txtKodeTruck, txtNamaPengemudi, txtTanggal, txtBeratDatang, txtBeratPulang,
                txtJumlahBusuk, txtJumlahLewatMatang, txtJumlahMatang, txtJumlahMentah, txtWaktuInput;

        public BuahViewHolder(@NonNull View itemView) {
            super(itemView);
            txtKodeTruck = itemView.findViewById(R.id.txtKodeTruck);
            txtNamaPengemudi = itemView.findViewById(R.id.txtNamaPengemudi);
            txtTanggal = itemView.findViewById(R.id.txtTanggal);
            txtBeratDatang = itemView.findViewById(R.id.txtBeratDatang);
            txtBeratPulang = itemView.findViewById(R.id.txtBeratPulang);
            txtJumlahBusuk = itemView.findViewById(R.id.txtJumlahBusuk);
            txtJumlahLewatMatang = itemView.findViewById(R.id.txtJumlahLewatMatang);
            txtJumlahMatang = itemView.findViewById(R.id.txtJumlahMatang);
            txtJumlahMentah = itemView.findViewById(R.id.txtJumlahMentah);
            txtWaktuInput = itemView.findViewById(R.id.txtWaktuInput);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(buahList.get(position));
                }
            });
        }

        public void bind(HashMap<String, Object> buah) {
            txtKodeTruck.setText(String.valueOf(buah.get("kodeTruck")));
            txtNamaPengemudi.setText(String.valueOf(buah.get("namaPengemudi")));
            txtTanggal.setText(String.valueOf(buah.get("tanggalInput")));
            txtBeratDatang.setText("Berat Datang: " + buah.get("beratDatang"));
            txtBeratPulang.setText("Berat Pulang: " + buah.get("beratPulang"));
            txtJumlahBusuk.setText("Jumlah Busuk: " + buah.get("jumlahBusuk"));
            txtJumlahLewatMatang.setText("Jumlah Lewat Matang: " + buah.get("jumlahLewatMatang"));
            txtJumlahMatang.setText("Jumlah Matang: " + buah.get("jumlahMatang"));
            txtJumlahMentah.setText("Jumlah Mentah: " + buah.get("jumlahMentah"));
            txtWaktuInput.setText("Waktu Input: " + buah.get("waktuInput"));
        }
    }
}