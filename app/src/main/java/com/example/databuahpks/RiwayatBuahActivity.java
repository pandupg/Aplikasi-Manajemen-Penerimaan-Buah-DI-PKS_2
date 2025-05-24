package com.example.databuahpks;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;

import com.example.databuahpks.BuahAdapter;

public class RiwayatBuahActivity extends AppCompatActivity {
    private RecyclerView recyclerViewBuah;
    private BuahAdapter adapter;
    private ArrayList<HashMap<String, Object>> buahList;
    private FirebaseFirestore firestore;
    private ListenerRegistration buahListener;
    private BuahAdapter.OnItemClickListener itemClickListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_riwayat_buah);
        setupBottomNav();

        recyclerViewBuah = findViewById(R.id.recyclerViewBuah);
        firestore = FirebaseFirestore.getInstance();
        buahList = new ArrayList<>();

        itemClickListener = new BuahAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(HashMap<String, Object> buah) {
                Toast.makeText(RiwayatBuahActivity.this, "Clicked: " + buah.get("kodeTruck"), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteClick(HashMap<String, Object> buah) {
                deleteBuah(buah);
            }
        };

        adapter = new BuahAdapter(buahList, itemClickListener);

        recyclerViewBuah.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewBuah.setAdapter(adapter);

        // Setup swipe-to-delete (left) and swipe-to-edit (right)
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false; // No drag-and-drop
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    HashMap<String, Object> buah = buahList.get(position);
                    if (direction == ItemTouchHelper.LEFT) {
                        // Swipe left to delete
                        itemClickListener.onDeleteClick(buah);
                    } else if (direction == ItemTouchHelper.RIGHT) {
                        // Swipe right to edit
                        Intent intent = new Intent(RiwayatBuahActivity.this, InputBuahActivity.class);
                        intent.putExtra("editMode", true);
                        intent.putExtra("buahId", (String) buah.get("id"));
                        intent.putExtra("kodeTruck", (String) buah.get("kodeTruck"));
                        intent.putExtra("namaPengemudi", (String) buah.get("namaPengemudi"));
                        intent.putExtra("tanggalInput", (String) buah.get("tanggalInput")); // Map tanggal to tanggalInput
                        intent.putExtra("beratDatang", String.valueOf(buah.get("beratDatang")));
                        intent.putExtra("beratPulang", String.valueOf(buah.get("beratPulang")));
                        intent.putExtra("jumlahBusuk", String.valueOf(buah.get("jumlahBusuk")));
                        intent.putExtra("jumlahLewatMatang", String.valueOf(buah.get("jumlahLewatMatang")));
                        intent.putExtra("jumlahMatang", String.valueOf(buah.get("jumlahMatang")));
                        intent.putExtra("jumlahMentah", String.valueOf(buah.get("jumlahMentah")));
                        intent.putExtra("waktuInput", (String) buah.get("waktuInput"));
                        startActivity(intent);
                    }
                }
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;
                    ColorDrawable background;
                    int icon;
                    int iconMargin = (itemView.getHeight() - 48) / 2; // 48dp icon size

                    if (dX < 0) { // Swipe left (delete)
                        background = new ColorDrawable(ContextCompat.getColor(RiwayatBuahActivity.this, android.R.color.holo_red_light));
                        icon = android.R.drawable.ic_menu_delete;
                    } else { // Swipe right (edit)
                        background = new ColorDrawable(ContextCompat.getColor(RiwayatBuahActivity.this, android.R.color.holo_green_light));
                        icon = android.R.drawable.ic_menu_edit;
                    }

                    // Draw background
                    if (dX < 0) {
                        background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                    } else {
                        background.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + (int) dX, itemView.getBottom());
                    }
                    background.draw(c);

                    // Draw icon
                    android.graphics.drawable.Drawable drawable = ContextCompat.getDrawable(RiwayatBuahActivity.this, icon);
                    if (drawable != null) {
                        int iconTop = itemView.getTop() + iconMargin;
                        int iconBottom = iconTop + 48;
                        int iconLeft, iconRight;
                        if (dX < 0) { // Delete icon on right
                            iconRight = itemView.getRight() - iconMargin;
                            iconLeft = iconRight - 48;
                        } else { // Edit icon on left
                            iconLeft = itemView.getLeft() + iconMargin;
                            iconRight = iconLeft + 48;
                        }
                        drawable.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        drawable.draw(c);
                    }
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerViewBuah);

        loadBuahData();
    }

    private void loadBuahData() {
        buahListener = firestore.collection("Buah")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e("RiwayatBuahActivity", "Listener error: " + error.getMessage());
                        Toast.makeText(this, "Gagal memuat data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (querySnapshot != null) {
                        buahList.clear();
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                            HashMap<String, Object> buah = new HashMap<>();
                            buah.put("id", doc.getId());
                            buah.put("kodeTruck", doc.getString("kodeTruck"));
                            buah.put("namaPengemudi", doc.getString("namaPengemudi"));
                            buah.put("tanggalInput", doc.getString("tanggalInput")); // Map tanggalInput to tanggal
                            buah.put("beratDatang", doc.get("beratDatang"));
                            buah.put("beratPulang", doc.get("beratPulang"));
                            buah.put("jumlahBusuk", doc.get("jumlahBusuk"));
                            buah.put("jumlahLewatMatang", doc.get("jumlahLewatMatang"));
                            buah.put("jumlahMatang", doc.get("jumlahMatang"));
                            buah.put("jumlahMentah", doc.get("jumlahMentah"));
                            buah.put("waktuInput", doc.getString("waktuInput"));
                            buahList.add(buah);
                        }
                        adapter.notifyDataSetChanged();
                        Log.d("RiwayatBuahActivity", "Loaded " + buahList.size() + " buah items");
                    }
                });
    }

    private void deleteBuah(HashMap<String, Object> buah) {
        String buahId = (String) buah.get("id");
        if (buahId != null) {
            firestore.collection("Buah").document(buahId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d("RiwayatBuahActivity", "Deleted buah: " + buahId);
                        Toast.makeText(this, "Data buah dihapus", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("RiwayatBuahActivity", "Delete error: " + e.getMessage());
                        Toast.makeText(this, "Gagal menghapus: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (buahListener != null) {
            buahListener.remove();
        }
    }

    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setSelectedItemId(R.id.nav_history);

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
            } else if (id == R.id.nav_add) {
                tampilkanBottomSheetAdd();
            } else if (id == R.id.nav_history) {
                tampilkanBottomSheetHistory();
            }
            return true;
        });
    }
    private void tampilkanBottomSheetAdd() {
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_add, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);

        Button btnInputTruck = view.findViewById(R.id.btnInputTruck);
        Button btnInputBuah = view.findViewById(R.id.btnInputBuah);

        btnInputTruck.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, InputTruckActivity.class));
        });

        btnInputBuah.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, InputBuahActivity.class));
        });

        dialog.show();
    }
    private void tampilkanBottomSheetHistory() {
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_history, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);

        Button btnRiwayatBuah = view.findViewById(R.id.btnRiwayatBuah);
        Button btnRiwayatTruck = view.findViewById(R.id.btnRiwayatTruck);

        btnRiwayatBuah.setOnClickListener(v -> {
            dialog.dismiss();
            // Already in RiwayatBuahActivity
        });

        btnRiwayatTruck.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, RiwayatTruckActivity.class));
        });

        dialog.show();
    }
}