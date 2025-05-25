package com.example.databuahpks;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.databuahpks.adapter.TruckAdapter;
import com.example.databuahpks.model.Truck;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class RiwayatTruckActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TruckAdapter adapter;
    public List<Truck> truckList = new ArrayList<>();
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_riwayat_truck);

        firestore = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recyclerViewTruck);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TruckAdapter(truckList, kode -> {
            firestore.collection("Truck").document(kode).delete()
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Truck dihapus", Toast.LENGTH_SHORT).show();
                        muatData();
                    });
        });
        recyclerView.setAdapter(adapter);

        // Swipe to delete (left) and edit (right)
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Truck truck = adapter.getItem(position);

                if (direction == ItemTouchHelper.LEFT) {
                    // Swipe left to delete
                    firestore.collection("Truck").document(truck.getKodeTruck())
                            .delete()
                            .addOnSuccessListener(unused -> {
                                truckList.remove(position);
                                adapter.notifyItemRemoved(position);
                                Toast.makeText(RiwayatTruckActivity.this, "Data truck dihapus", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(RiwayatTruckActivity.this, "Gagal hapus data", Toast.LENGTH_SHORT).show();
                                adapter.notifyItemChanged(position);
                            });
                } else if (direction == ItemTouchHelper.RIGHT) {
                    // Swipe right to edit
                    Intent intent = new Intent(RiwayatTruckActivity.this, InputTruckActivity.class);
                    intent.putExtra("kodeTruck", truck.getKodeTruck());
                    intent.putExtra("namaPengemudi", truck.getNamaPengemudi());
                    intent.putExtra("isEditMode", true);
                    startActivity(intent);
                    adapter.notifyItemChanged(position); // Reset swipe
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        .addSwipeLeftBackgroundColor(ContextCompat.getColor(RiwayatTruckActivity.this, R.color.red))
                        .addSwipeLeftActionIcon(R.drawable.baseline_delete_24)
                        .addSwipeLeftLabel("Hapus")
                        .setSwipeLeftLabelColor(Color.WHITE)
                        .addSwipeRightBackgroundColor(ContextCompat.getColor(RiwayatTruckActivity.this, R.color.green))
                        .addSwipeRightActionIcon(R.drawable.baseline_edit_24)
                        .addSwipeRightLabel("Edit")
                        .setSwipeRightLabelColor(Color.WHITE)
                        .create()
                        .decorate();
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);

        setupBottomNav();
        muatData();
    }

    private void muatData() {
        truckList.clear();
        firestore.collection("Truck").get().addOnSuccessListener(querySnapshot -> {
            for (QueryDocumentSnapshot doc : querySnapshot) {
                Truck t = new Truck(
                        doc.getString("kodeTruck"),
                        doc.getString("namaPengemudi")
                );
                truckList.add(t);
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setSelectedItemId(R.id.nav_history);

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_add) {
                tampilkanBottomSheetAdd("RiwayatTruck");
                return true;
            } else if (id == R.id.nav_history) {
                tampilkanBottomSheetHistory("RiwayatTruck");
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, AccountActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void tampilkanBottomSheetAdd(String currentActivity) {
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_add, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);

        Button btnInputTruck = view.findViewById(R.id.btnInputTruck);
        Button btnInputBuah = view.findViewById(R.id.btnInputBuah);

        if (currentActivity.equals("InputBuah")) {
            btnInputBuah.setEnabled(false);
            btnInputBuah.setAlpha(0.5f);
        } else if (currentActivity.equals("InputTruck")) {
            btnInputTruck.setEnabled(false);
            btnInputTruck.setAlpha(0.5f);
        }

        btnInputTruck.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, InputTruckActivity.class));
            finish();
        });

        btnInputBuah.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, InputBuahActivity.class));
            finish();
        });

        dialog.show();
    }

    private void tampilkanBottomSheetHistory(String currentActivity) {
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_history, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);

        Button btnRiwayatBuah = view.findViewById(R.id.btnRiwayatBuah);
        Button btnRiwayatTruck = view.findViewById(R.id.btnRiwayatTruck);

        if (currentActivity.equals("RiwayatBuah")) {
            btnRiwayatBuah.setEnabled(false);
            btnRiwayatBuah.setAlpha(0.5f);
        } else if (currentActivity.equals("RiwayatTruck")) {
            btnRiwayatTruck.setEnabled(false);
            btnRiwayatTruck.setAlpha(0.5f);
        }

        btnRiwayatBuah.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, RiwayatBuahActivity.class));
            finish();
        });

        btnRiwayatTruck.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, RiwayatTruckActivity.class));
            finish();
        });

        dialog.show();
    }
}