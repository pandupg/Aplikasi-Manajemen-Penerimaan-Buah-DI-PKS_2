package com.example.databuahpks;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;

import com.example.databuahpks.BuahAdapter;

public class RiwayatBuahActivity extends AppCompatActivity {
    private RecyclerView recyclerViewBuah;
    private TextView txtEmptyState;
    private BuahAdapter adapter;
    private ArrayList<HashMap<String, Object>> buahList;
    private FirebaseFirestore firestore;
    private ListenerRegistration buahListener;
    private BuahAdapter.OnItemClickListener itemClickListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_riwayat_buah);

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

        recyclerViewBuah = findViewById(R.id.recyclerViewBuah);
        txtEmptyState = findViewById(R.id.txtEmptyState);
        buahList = new ArrayList<>();

        itemClickListener = new BuahAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(HashMap<String, Object> buah) {
                String kodeTruck = (String) buah.get("kodeTruck");
                Toast.makeText(RiwayatBuahActivity.this, "Clicked: " + (kodeTruck != null ? kodeTruck : "Unknown"), Toast.LENGTH_SHORT).show();
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
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && position < buahList.size()) {
                    HashMap<String, Object> buah = buahList.get(position);
                    if (direction == ItemTouchHelper.LEFT) {
                        itemClickListener.onDeleteClick(buah);
                    } else if (direction == ItemTouchHelper.RIGHT) {
                        Intent intent = new Intent(RiwayatBuahActivity.this, InputBuahActivity.class);
                        intent.putExtra("isEditMode", true);
                        intent.putExtra("buahId", (String) buah.get("id"));
                        intent.putExtra("kodeTruck", (String) buah.get("kodeTruck"));
                        intent.putExtra("namaPengemudi", (String) buah.get("namaPengemudi"));
                        intent.putExtra("tanggalInput", (String) buah.get("tanggalInput"));
                        intent.putExtra("beratDatang", buah.get("beratDatang") != null ? String.valueOf(buah.get("beratDatang")) : "");
                        intent.putExtra("beratPulang", buah.get("beratPulang") != null ? String.valueOf(buah.get("beratPulang")) : "");
                        intent.putExtra("jumlahBusuk", buah.get("jumlahBusuk") != null ? String.valueOf(buah.get("jumlahBusuk")) : "");
                        intent.putExtra("jumlahLewatMatang", buah.get("jumlahLewatMatang") != null ? String.valueOf(buah.get("jumlahLewatMatang")) : "");
                        intent.putExtra("jumlahMatang", buah.get("jumlahMatang") != null ? String.valueOf(buah.get("jumlahMatang")) : "");
                        intent.putExtra("jumlahMentah", buah.get("jumlahMentah") != null ? String.valueOf(buah.get("jumlahMentah")) : "");
                        intent.putExtra("waktuInput", (String) buah.get("waktuInput"));
                        startActivity(intent);
                    }
                }
                adapter.notifyItemChanged(position);
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;
                    ColorDrawable background;
                    int icon;
                    int iconMargin = (itemView.getHeight() - (int) (48 * getResources().getDisplayMetrics().density)) / 2;

                    if (dX < 0) {
                        background = new ColorDrawable(ContextCompat.getColor(RiwayatBuahActivity.this, android.R.color.holo_red_light));
                        icon = android.R.drawable.ic_menu_delete;
                    } else {
                        background = new ColorDrawable(ContextCompat.getColor(RiwayatBuahActivity.this, android.R.color.holo_green_light));
                        icon = android.R.drawable.ic_menu_edit;
                    }

                    if (dX < 0) {
                        background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                    } else {
                        background.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + (int) dX, itemView.getBottom());
                    }
                    background.draw(c);

                    android.graphics.drawable.Drawable drawable = ContextCompat.getDrawable(RiwayatBuahActivity.this, icon);
                    if (drawable != null) {
                        int iconTop = itemView.getTop() + iconMargin;
                        int iconBottom = iconTop + (int) (48 * getResources().getDisplayMetrics().density);
                        int iconLeft, iconRight;
                        if (dX < 0) {
                            iconRight = itemView.getRight() - iconMargin;
                            iconLeft = iconRight - (int) (48 * getResources().getDisplayMetrics().density);
                        } else {
                            iconLeft = itemView.getLeft() + iconMargin;
                            iconRight = iconLeft + (int) (48 * getResources().getDisplayMetrics().density);
                        }
                        drawable.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        drawable.draw(c);
                    }
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerViewBuah);

        setupBottomNav();
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
                            buah.put("tanggalInput", doc.getString("tanggalInput"));
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
                        txtEmptyState.setVisibility(buahList.isEmpty() ? View.VISIBLE : View.GONE);
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
        } else {
            Toast.makeText(this, "Gagal menghapus: ID tidak valid", Toast.LENGTH_SHORT).show();
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
                finish();
            } else if (id == R.id.nav_add) {
                tampilkanBottomSheetAdd("RiwayatBuah");
            } else if (id == R.id.nav_history) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    firestore.collection("Users").document(currentUser.getUid())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                String role = documentSnapshot.getString("role");
                                if ("mandor".equals(role)) {
                                    tampilkanBottomSheetHistory("RiwayatBuah");
                                } else {
                                    Toast.makeText(this, "Hanya Mandor yang dapat mengakses riwayat", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("RiwayatBuahActivity", "Failed to load role: " + e.getMessage());
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