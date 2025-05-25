package com.example.databuahpks;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class InputBuahActivity extends AppCompatActivity {

    private Spinner spinnerTruckPengemudi;
    private EditText edtBeratDatang, edtBeratPulang, edtJumlahMatang, edtJumlahLewatMatang,
            edtJumlahMentah, edtJumlahBusuk;
    private Button btnSimpanBuah;
    private FirebaseFirestore firestore;
    private ArrayList<String> truckList = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private HashMap<String, String[]> truckData = new HashMap<>(); // Stores displayText -> [kodeTruck, namaPengemudi]
    private String selectedKodeTruck = null;
    private String selectedNamaPengemudi = null;
    private boolean isEditMode = false;
    private String buahId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_buah);
        setupBottomNav();

        // Initialize views
        spinnerTruckPengemudi = findViewById(R.id.spinnerTruckPengemudi);
        edtBeratDatang = findViewById(R.id.edtBeratDatang);
        edtBeratPulang = findViewById(R.id.edtBeratPulang);
        edtJumlahMatang = findViewById(R.id.edtJumlahMatang);
        edtJumlahLewatMatang = findViewById(R.id.edtJumlahLewatMatang);
        edtJumlahMentah = findViewById(R.id.edtJumlahMentah);
        edtJumlahBusuk = findViewById(R.id.edtJumlahBusuk);
        btnSimpanBuah = findViewById(R.id.btnSimpanBuah);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Setup Spinner for truck selection
        truckList.add("Pilih Truck - Pengemudi"); // Placeholder
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, truckList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTruckPengemudi.setAdapter(adapter);

        // Load truck data from Firestore
        loadTruckData();

        // Handle Spinner selection
        spinnerTruckPengemudi.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                if (!selected.equals("Pilih Truck - Pengemudi") && truckData.containsKey(selected)) {
                    String[] truckInfo = truckData.get(selected);
                    selectedKodeTruck = truckInfo[0];
                    selectedNamaPengemudi = truckInfo[1];
                } else {
                    selectedKodeTruck = null;
                    selectedNamaPengemudi = null;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedKodeTruck = null;
                selectedNamaPengemudi = null;
            }
        });

        // Check if in edit mode
        Intent intent = getIntent();
        if (intent.getBooleanExtra("editMode", false)) {
            isEditMode = true;
            buahId = intent.getStringExtra("buahId");
            // Pre-populate form
            String kodeTruck = intent.getStringExtra("kodeTruck");
            String namaPengemudi = intent.getStringExtra("namaPengemudi");
            String displayText = kodeTruck + " - " + namaPengemudi;
            if (truckList.contains(displayText)) {
                spinnerTruckPengemudi.setSelection(adapter.getPosition(displayText));
            } else {
                truckList.add(displayText);
                truckData.put(displayText, new String[]{kodeTruck, namaPengemudi});
                adapter.notifyDataSetChanged();
                spinnerTruckPengemudi.setSelection(adapter.getPosition(displayText));
            }
            selectedKodeTruck = kodeTruck;
            selectedNamaPengemudi = namaPengemudi;
            spinnerTruckPengemudi.setEnabled(false); // Disable spinner in edit mode
            edtBeratDatang.setText(intent.getStringExtra("beratDatang"));
            edtBeratPulang.setText(intent.getStringExtra("beratPulang"));
            edtJumlahMatang.setText(intent.getStringExtra("jumlahMatang"));
            edtJumlahLewatMatang.setText(intent.getStringExtra("jumlahLewatMatang"));
            edtJumlahMentah.setText(intent.getStringExtra("jumlahMentah"));
            edtJumlahBusuk.setText(intent.getStringExtra("jumlahBusuk"));
        }

        // Save button listener
        btnSimpanBuah.setOnClickListener(v -> saveFruitData());
    }

    private void loadTruckData() {
        firestore.collection("Truck")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String kode = doc.getString("kodeTruck");
                        String nama = doc.getString("namaPengemudi");
                        if (kode != null && nama != null) {
                            String displayText = kode + " - " + nama;
                            if (!truckList.contains(displayText)) {
                                truckList.add(displayText);
                                truckData.put(displayText, new String[]{kode, nama});
                            }
                        }
                    }
                    Collections.sort(truckList.subList(1, truckList.size())); // Sort without placeholder
                    adapter.notifyDataSetChanged();
                    if (isEditMode) {
                        String kodeTruck = getIntent().getStringExtra("kodeTruck");
                        String namaPengemudi = getIntent().getStringExtra("namaPengemudi");
                        String displayText = kodeTruck + " - " + namaPengemudi;
                        if (truckList.contains(displayText)) {
                            spinnerTruckPengemudi.setSelection(adapter.getPosition(displayText));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gagal memuat data truck: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveFruitData() {
        String beratDatangStr = edtBeratDatang.getText().toString().trim();
        String beratPulangStr = edtBeratPulang.getText().toString().trim();
        String jumlahMatangStr = edtJumlahMatang.getText().toString().trim();
        String jumlahLewatMatangStr = edtJumlahLewatMatang.getText().toString().trim();
        String jumlahMentahStr = edtJumlahMentah.getText().toString().trim();
        String jumlahBusukStr = edtJumlahBusuk.getText().toString().trim();

        if (selectedKodeTruck == null || selectedNamaPengemudi == null) {
            Toast.makeText(this, "Pilih Truck dan Pengemudi yang valid", Toast.LENGTH_SHORT).show();
            return;
        }
        if (beratDatangStr.isEmpty()) {
            edtBeratDatang.setError("Berat Datang harus diisi");
            return;
        }
        if (beratPulangStr.isEmpty()) {
            edtBeratPulang.setError("Berat Pulang harus diisi");
            return;
        }
        if (jumlahMatangStr.isEmpty()) {
            edtJumlahMatang.setError("Jumlah Matang harus diisi");
            return;
        }
        if (jumlahLewatMatangStr.isEmpty()) {
            edtJumlahLewatMatang.setError("Jumlah Lewat Matang harus diisi");
            return;
        }
        if (jumlahMentahStr.isEmpty()) {
            edtJumlahMentah.setError("Jumlah Mentah harus diisi");
            return;
        }
        if (jumlahBusukStr.isEmpty()) {
            edtJumlahBusuk.setError("Jumlah Busuk harus diisi");
            return;
        }

        try {
            double beratDatang = Double.parseDouble(beratDatangStr);
            double beratPulang = Double.parseDouble(beratPulangStr);
            int jumlahMatang = Integer.parseInt(jumlahMatangStr);
            int jumlahLewatMatang = Integer.parseInt(jumlahLewatMatangStr);
            int jumlahMentah = Integer.parseInt(jumlahMentahStr);
            int jumlahBusuk = Integer.parseInt(jumlahBusukStr);

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            String tanggalInput = dateFormat.format(new Date());
            String waktuInput = timeFormat.format(new Date());

            HashMap<String, Object> buah = new HashMap<>();
            buah.put("kodeTruck", selectedKodeTruck);
            buah.put("namaPengemudi", selectedNamaPengemudi);
            buah.put("beratDatang", beratDatang);
            buah.put("beratPulang", beratPulang);
            buah.put("jumlahMatang", jumlahMatang);
            buah.put("jumlahLewatMatang", jumlahLewatMatang);
            buah.put("jumlahMentah", jumlahMentah);
            buah.put("jumlahBusuk", jumlahBusuk);
            buah.put("tanggalInput", tanggalInput);
            buah.put("waktuInput", waktuInput);

            if (isEditMode) {
                firestore.collection("Buah").document(buahId)
                        .set(buah)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Data buah berhasil diperbarui", Toast.LENGTH_SHORT).show();
                            clearForm();
                            isEditMode = false;
                            buahId = null;
                            spinnerTruckPengemudi.setEnabled(true);
                            spinnerTruckPengemudi.setSelection(0);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Gagal memperbarui: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                String documentId = selectedKodeTruck + "_" + tanggalInput + "_" + waktuInput;
                com.google.firebase.firestore.DocumentReference docRef = firestore.collection("Buah").document(documentId);
                docRef.get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        new AlertDialog.Builder(this)
                                .setTitle("Konfirmasi")
                                .setMessage("Data dengan Kode Truck dan waktu ini sudah ada. Ganti data?")
                                .setPositiveButton("Ganti", (dialog, which) -> simpanData(docRef, buah))
                                .setNegativeButton("Batal", null)
                                .show();
                    } else {
                        simpanData(docRef, buah);
                    }
                });
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Masukkan angka yang valid untuk berat dan jumlah", Toast.LENGTH_SHORT).show();
        }
    }

    private void simpanData(com.google.firebase.firestore.DocumentReference docRef, HashMap<String, Object> buah) {
        docRef.set(buah)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Data buah berhasil disimpan", Toast.LENGTH_SHORT).show();
                    clearForm();
                    spinnerTruckPengemudi.setSelection(0);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gagal menyimpan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void clearForm() {
        spinnerTruckPengemudi.setSelection(0);
        edtBeratDatang.setText("");
        edtBeratPulang.setText("");
        edtJumlahMatang.setText("");
        edtJumlahLewatMatang.setText("");
        edtJumlahMentah.setText("");
        edtJumlahBusuk.setText("");
        edtBeratDatang.requestFocus();
    }

    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setSelectedItemId(R.id.nav_add);

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish(); // Close current activity
                return true;
            } else if (id == R.id.nav_add) {
                tampilkanBottomSheetAdd("InputBuah"); // Pass current activity identifier
                return true;
            } else if (id == R.id.nav_history) {
                tampilkanBottomSheetHistory("InputBuah"); // Pass current activity identifier
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

        // Disable button based on current activity
        if (currentActivity.equals("InputBuah")) {
            btnInputBuah.setEnabled(false);
            btnInputBuah.setAlpha(0.5f); // Visually indicate disabled state
        } else if (currentActivity.equals("InputTruck")) {
            btnInputTruck.setEnabled(false);
            btnInputTruck.setAlpha(0.5f);
        }

        btnInputTruck.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, InputTruckActivity.class));
            finish(); // Close current activity
        });

        btnInputBuah.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, InputBuahActivity.class));
            finish(); // Close current activity
        });

        dialog.show();
    }

    private void tampilkanBottomSheetHistory(String currentActivity) {
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_history, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);

        Button btnRiwayatBuah = view.findViewById(R.id.btnRiwayatBuah);
        Button btnRiwayatTruck = view.findViewById(R.id.btnRiwayatTruck);

        // Disable button based on current activity
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
            finish(); // Close current activity
        });

        btnRiwayatTruck.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, RiwayatTruckActivity.class));
            finish(); // Close current activity
        });

        dialog.show();
    }
}