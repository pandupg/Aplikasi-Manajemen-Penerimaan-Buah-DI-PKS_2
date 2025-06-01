package com.example.databuahpks;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.databuahpks.adapter.TruckAdapter;
import com.example.databuahpks.model.Truck;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class RiwayatTruckActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TextView txtEmptyState;
    private TruckAdapter adapter;
    private List<Truck> truckList = new ArrayList<>();
    private FirebaseFirestore firestore;
    private ListenerRegistration truckListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_riwayat_truck);

        firestore = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }

        firestore.collection("Users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        if (role == null) {
                            Toast.makeText(this, "Role tidak ditemukan", Toast.LENGTH_SHORT).show();
                            FirebaseAuth.getInstance().signOut();
                            startActivity(new Intent(this, SignInActivity.class));
                            finish();
                        } else if ("pekerja".equals(role)) {
                            Toast.makeText(this, "Hanya Mandor yang dapat mengakses riwayat", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Data pengguna tidak ditemukan", Toast.LENGTH_SHORT).show();
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(this, SignInActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(this.getClass().getSimpleName(), "Failed to load role: " + e.getMessage());
                    Toast.makeText(this, "Gagal memuat role: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        recyclerView = findViewById(R.id.recyclerViewTruck);
        txtEmptyState = findViewById(R.id.txtEmptyState);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TruckAdapter(truckList);
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
                if (position != RecyclerView.NO_POSITION && position < truckList.size()) {
                    Truck truck = adapter.getItem(position);
                    if (truck != null && truck.getKodeTruck() != null) {
                        if (direction == ItemTouchHelper.LEFT) {
                            new AlertDialog.Builder(RiwayatTruckActivity.this)
                                    .setTitle("Konfirmasi")
                                    .setMessage("Hapus truck " + truck.getKodeTruck() + "? Semua data buah terkait juga akan dihapus.")
                                    .setPositiveButton("Hapus", (dialog, which) -> deleteTruckAndBuah(truck, position))
                                    .setNegativeButton("Batal", (dialog, which) -> adapter.notifyItemChanged(position))
                                    .show();
                        } else if (direction == ItemTouchHelper.RIGHT) {
                            Intent intent = new Intent(RiwayatTruckActivity.this, InputTruckActivity.class);
                            intent.putExtra("kodeTruck", truck.getKodeTruck());
                            intent.putExtra("namaPengemudi", truck.getNamaPengemudi());
                            intent.putExtra("isEditMode", true);
                            startActivity(intent);
                            adapter.notifyItemChanged(position);
                        }
                    } else {
                        Toast.makeText(RiwayatTruckActivity.this, "Data truck tidak valid", Toast.LENGTH_SHORT).show();
                        adapter.notifyItemChanged(position);
                    }
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        .addSwipeLeftBackgroundColor(ContextCompat.getColor(RiwayatTruckActivity.this, android.R.color.holo_red_light))
                        .addSwipeLeftActionIcon(android.R.drawable.ic_menu_delete)
                        .addSwipeLeftLabel("Hapus")
                        .setSwipeLeftLabelColor(Color.WHITE)
                        .addSwipeRightBackgroundColor(ContextCompat.getColor(RiwayatTruckActivity.this, android.R.color.holo_green_light))
                        .addSwipeRightActionIcon(android.R.drawable.ic_menu_edit)
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

    private void deleteTruckAndBuah(Truck truck, int position) {
        String kodeTruck = truck.getKodeTruck();
        // Query Buah documents with matching kodeTruck
        firestore.collection("Buah")
                .whereEqualTo("kodeTruck", kodeTruck)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = firestore.batch();
                    // Add Buah documents to batch delete
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        batch.delete(doc.getReference());
                    }
                    // Add Truck document to batch delete
                    batch.delete(firestore.collection("Truck").document(kodeTruck));
                    // Commit batch
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d("RiwayatTruckActivity", "Deleted truck: " + kodeTruck + " and " + querySnapshot.size() + " buah");
                                Toast.makeText(RiwayatTruckActivity.this, "Data truck dan buah dihapus", Toast.LENGTH_SHORT).show();
                                txtEmptyState.setVisibility(truckList.isEmpty() ? View.VISIBLE : View.GONE);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("RiwayatTruckActivity", "Batch delete error: " + e.getMessage());
                                Toast.makeText(RiwayatTruckActivity.this, "Gagal hapus data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                adapter.notifyItemChanged(position);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("RiwayatTruckActivity", "Query buah error: " + e.getMessage());
                    Toast.makeText(RiwayatTruckActivity.this, "Gagal hapus data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    adapter.notifyItemChanged(position);
                });
    }

    private void muatData() {
        truckListener = firestore.collection("Truck")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e("RiwayatTruckActivity", "Listener error: " + error.getMessage());
                        Toast.makeText(this, "Gagal memuat data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (querySnapshot != null) {
                        truckList.clear();
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            String kodeTruck = doc.getString("kodeTruck");
                            String namaPengemudi = doc.getString("namaPengemudi");
                            if (kodeTruck != null) {
                                Truck t = new Truck(kodeTruck, namaPengemudi);
                                truckList.add(t);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        txtEmptyState.setVisibility(truckList.isEmpty() ? View.VISIBLE : View.GONE);
                        Log.d("RiwayatTruckActivity", "Loaded " + truckList.size() + " truck items");
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (truckListener != null) {
            truckListener.remove();
        }
    }

    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setSelectedItemId(R.id.nav_history);

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else if (id == R.id.nav_add) {
                tampilkanBottomSheetAdd("RiwayatTruck");
            } else if (id == R.id.nav_history) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    firestore.collection("Users").document(currentUser.getUid())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                String role = documentSnapshot.getString("role");
                                if ("mandor".equals(role)) {
                                    tampilkanBottomSheetHistory("RiwayatTruck");
                                } else {
                                    Toast.makeText(this, "Hanya Mandor yang dapat mengakses riwayat", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("RiwayatTruckActivity", "Failed to load role: " + e.getMessage());
                                Toast.makeText(this, "Gagal memuat role", Toast.LENGTH_SHORT).show();
                            });
                }
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, AccountActivity.class));
                finish();
            }
            return true;
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