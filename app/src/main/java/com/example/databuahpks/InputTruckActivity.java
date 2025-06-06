package com.example.databuahpks;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class InputTruckActivity extends AppCompatActivity {
    private EditText edtNamaPengemudi;
    private AutoCompleteTextView edtKodeTruck;
    private Button btnSimpan;
    private FirebaseFirestore firestore;
    private ArrayList<String> kodeList = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private boolean isEditMode = false;
    private ListenerRegistration truckListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_truck);

        // Initialize Firestore early
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

        edtKodeTruck = findViewById(R.id.edtKodeTruck);
        edtNamaPengemudi = findViewById(R.id.edtNamaPengemudi);
        btnSimpan = findViewById(R.id.btnSimpanTruck);

        kodeList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, kodeList);
        edtKodeTruck.setAdapter(adapter);

        // Check for edit mode
        Intent intent = getIntent();
        isEditMode = intent.getBooleanExtra("isEditMode", false);
        if (isEditMode) {
            String kodeTruck = intent.getStringExtra("kodeTruck");
            String namaPengemudi = intent.getStringExtra("namaPengemudi");
            if (kodeTruck != null && namaPengemudi != null) {
                edtKodeTruck.setText(kodeTruck);
                edtKodeTruck.setEnabled(false); // Disable kodeTruck editing
                edtNamaPengemudi.setText(namaPengemudi);
                btnSimpan.setText("Update");
            }
        } else {
            // Load suggestions only in non-edit mode
            loadTruckSuggestions();
        }

        edtKodeTruck.setThreshold(1);
        edtKodeTruck.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !isEditMode && !kodeList.isEmpty()) {
                Log.d("InputTruckActivity", "Focus gained, showing dropdown with kodeList: " + kodeList.toString());
                // Ensure adapter is up-to-date before showing dropdown
                adapter.notifyDataSetChanged();
                edtKodeTruck.showDropDown();
            }
        });

        edtKodeTruck.setOnItemClickListener((parent, view, position, id) -> {
            if (!isEditMode) {
                String selected = adapter.getItem(position);
                if (selected != null) {
                    Log.d("InputTruckActivity", "Selected item: " + selected);
                    firestore.collection("Truck").document(selected)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    String nama = documentSnapshot.getString("namaPengemudi");
                                    edtNamaPengemudi.setText(nama);
                                    Log.d("InputTruckActivity", "Set namaPengemudi: " + nama);
                                }
                            });
                }
            }
        });

        edtKodeTruck.addTextChangedListener(new TextWatcher() {
            private String currentText = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!isEditMode) {
                    String upper = s.toString().toUpperCase();
                    if (!upper.equals(currentText)) {
                        currentText = upper;
                        edtKodeTruck.removeTextChangedListener(this);
                        edtKodeTruck.setText(upper);
                        edtKodeTruck.setSelection(upper.length());
                        edtKodeTruck.addTextChangedListener(this);
                        Log.d("InputTruckActivity", "Text transformed to: " + upper);
                    }

                    adapter.getFilter().filter(currentText);

                    // Check if input matches any kode in kodeList
                    boolean cocok = false;
                    for (String kode : kodeList) {
                        if (kode.toUpperCase().startsWith(currentText)) {
                            cocok = true;
                            break;
                        }
                    }

                    if (cocok && !currentText.isEmpty()) {
                        Log.d("InputTruckActivity", "Match found for: " + currentText + ", showing dropdown");
                        // Ensure adapter is up-to-date
                        adapter.notifyDataSetChanged();
                        edtKodeTruck.showDropDown();
                    } else {
                        Log.d("InputTruckActivity", "No match for: " + currentText + ", hiding dropdown");
                        edtKodeTruck.dismissDropDown();
                    }
                }
            }
        });

        btnSimpan.setOnClickListener(v -> {
            String kodeTruck = edtKodeTruck.getText().toString().trim().toUpperCase();
            String namaPengemudi = edtNamaPengemudi.getText().toString().trim();

            if (kodeTruck.isEmpty() || namaPengemudi.isEmpty()) {
                Toast.makeText(this, "Harap isi semua field", Toast.LENGTH_SHORT).show();
                return;
            }

            HashMap<String, Object> truck = new HashMap<>();
            truck.put("kodeTruck", kodeTruck);
            truck.put("namaPengemudi", namaPengemudi);

            DocumentReference docRef = firestore.collection("Truck").document(kodeTruck);

            if (isEditMode) {
                // Update existing truck
                simpanData(docRef, truck);
            } else {
                // Check for existing truck before saving new one
                docRef.get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String existingNama = documentSnapshot.getString("namaPengemudi");
                        if (!namaPengemudi.equalsIgnoreCase(existingNama)) {
                            new AlertDialog.Builder(this)
                                    .setTitle("Konfirmasi")
                                    .setMessage("Kode Truck sudah ada dengan nama pengemudi berbeda.\nGanti nama pengemudi?")
                                    .setPositiveButton("Ganti", (dialog, which) -> simpanData(docRef, truck))
                                    .setNegativeButton("Batal", null)
                                    .show();
                        } else {
                            Toast.makeText(this, "Data sudah ada dan sama", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        simpanData(docRef, truck);
                    }
                });
            }
        });

        setupBottomNav();
    }

    private void loadTruckSuggestions() {
        kodeList.clear();
        Log.d("InputTruckActivity", "Starting listener for Truck collection");
        truckListener = firestore.collection("Truck")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e("InputTruckActivity", "Listener error: " + error.getMessage());
                        // Fallback to one-time query
                        loadTruckSuggestionsFallback();
                        return;
                    }
                    if (querySnapshot != null) {
                        kodeList.clear();
                        int count = 0;
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                            String kode = doc.getString("kodeTruck");
                            if (kode != null && !kodeList.contains(kode)) {
                                kodeList.add(kode);
                                count++;
                            }
                        }
                        Collections.sort(kodeList);
                        Collections.reverse(kodeList);
                        Log.d("InputTruckActivity", "Before adapter update, kodeList: " + kodeList.toString());
                        // Create a new adapter to ensure UI refresh
                        adapter = new ArrayAdapter<>(InputTruckActivity.this,
                                android.R.layout.simple_dropdown_item_1line, kodeList);
                        edtKodeTruck.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                        Log.d("InputTruckActivity", "After adapter update, adapter count: " + adapter.getCount());
                        // Force dropdown refresh
                        if (!isEditMode && !kodeList.isEmpty()) {
                            runOnUiThread(() -> {
                                edtKodeTruck.setText("");
                                edtKodeTruck.requestFocus();
                                Log.d("InputTruckActivity", "Forcing dropdown show with kodeList: " + kodeList.toString());
                                edtKodeTruck.showDropDown();
                            });
                        }
                        Log.d("InputTruckActivity", "kodeList terupdate: " + kodeList.toString() + ", count: " + count);
                    } else {
                        Log.w("InputTruckActivity", "QuerySnapshot is null");
                    }
                });
    }

    private void loadTruckSuggestionsFallback() {
        Log.d("InputTruckActivity", "Falling back to one-time query");
        firestore.collection("Truck")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    kodeList.clear();
                    int count = 0;
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String kode = doc.getString("kodeTruck");
                        if (kode != null && !kodeList.contains(kode)) {
                            kodeList.add(kode);
                            count++;
                        }
                    }
                    Collections.sort(kodeList);
                    Collections.reverse(kodeList);
                    Log.d("InputTruckActivity", "Before adapter update (fallback), kodeList: " + kodeList.toString());
                    // Create a new adapter to ensure UI refresh
                    adapter = new ArrayAdapter<>(InputTruckActivity.this,
                            android.R.layout.simple_dropdown_item_1line, kodeList);
                    edtKodeTruck.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                    Log.d("InputTruckActivity", "After adapter update (fallback), adapter count: " + adapter.getCount());
                    if (!isEditMode && !kodeList.isEmpty()) {
                        runOnUiThread(() -> {
                            edtKodeTruck.setText("");
                            edtKodeTruck.requestFocus();
                            Log.d("InputTruckActivity", "Forcing dropdown show (fallback) with kodeList: " + kodeList.toString());
                            edtKodeTruck.showDropDown();
                        });
                    }
                    Log.d("InputTruckActivity", "Fallback kodeList: " + kodeList.toString() + ", count: " + count);
                })
                .addOnFailureListener(e -> {
                    Log.e("InputTruckActivity", "Fallback query error: " + e.getMessage());
                });
    }

    private void simpanData(DocumentReference docRef, HashMap<String, Object> truck) {
        Log.d("InputTruckActivity", "Saving truck: " + truck.toString());
        docRef.set(truck)
                .addOnSuccessListener(aVoid -> {
                    Log.d("InputTruckActivity", "Save successful: " + truck.toString());
                    if (isEditMode) {
                        Toast.makeText(this, "Data truck berhasil diperbarui", Toast.LENGTH_SHORT).show();
                        isEditMode = false;
                        btnSimpan.setText("Simpan");
                        finish();
                    } else {
                        Toast.makeText(this, "Data truck berhasil disimpan", Toast.LENGTH_SHORT).show();
                        // Clear inputs
                        edtKodeTruck.setText("");
                        edtNamaPengemudi.setText("");
                        edtKodeTruck.requestFocus();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gagal menyimpan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("InputTruckActivity", "Save error: " + e.getMessage());
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove Firestore listener to prevent memory leaks
        if (truckListener != null) {
            truckListener.remove();
        }
    }

    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setSelectedItemId(R.id.nav_add);

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else if (id == R.id.nav_add) {
                tampilkanBottomSheetAdd("InputTruck");
            } else if (id == R.id.nav_history) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    firestore.collection("Users").document(currentUser.getUid())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                String role = documentSnapshot.getString("role");
                                if ("mandor".equals(role)) {
                                    tampilkanBottomSheetHistory("InputTruck");
                                } else {
                                    Toast.makeText(this, "Hanya Mandor yang dapat mengakses riwayat", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("InputTruckActivity", "Failed to load role: " + e.getMessage());
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