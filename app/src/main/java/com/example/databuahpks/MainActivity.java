package com.example.databuahpks;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

public class MainActivity extends AppCompatActivity {
    private TextView txtTotalBuah, txtTotalBuahMatang, txtTotalBuahMentah, txtTotalBuahBusuk, txtTotalBuahLewatMatang;
    private BarChart barChart;
    private FirebaseFirestore firestore;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        // Force light theme
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_main);

        // Initialize views
        txtTotalBuah = findViewById(R.id.txtTotalBuah);
        txtTotalBuahMatang = findViewById(R.id.txtTotalBuahMatang);
        txtTotalBuahMentah = findViewById(R.id.txtTotalBuahMentah);
        txtTotalBuahBusuk = findViewById(R.id.txtTotalBuahBusuk);
        txtTotalBuahLewatMatang = findViewById(R.id.txtTotalBuahLewatMatang);
        barChart = findViewById(R.id.barChart);

        // Initialize SharedPreferences
        prefs = getSharedPreferences("DataBuahPrefs", MODE_PRIVATE);

        // Insert dummy data from file if not already done
        insertDummyData();

        setupBottomNav();
        loadFirestoreData();
    }

    private String readDummyDataFromRaw() {
        try {
            InputStream is = getResources().openRawResource(R.raw.dummy_data);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int i;
            while ((i = is.read()) != -1) {
                baos.write(i);
            }
            is.close();
            return baos.toString();
        } catch (IOException e) {
            Log.e("MainActivity", "Error reading dummy data file: ", e);
            Toast.makeText(this, "Failed to read dummy data file", Toast.LENGTH_SHORT).show();
            return "[]"; // Return empty array on error
        }
    }

    private void insertDummyData() {
        // Check if dummy data has already been inserted
        boolean isDummyDataInserted = prefs.getBoolean("isDummyDataInserted", false);
        if (isDummyDataInserted) {
            Log.d("MainActivity", "Dummy data already inserted, skipping.");
            return;
        }

        try {
            // Read and parse JSON from file
            String jsonString = readDummyDataFromRaw();
            JSONArray dummyData = new JSONArray(jsonString);

            // Use batch write for efficiency
            com.google.firebase.firestore.WriteBatch batch = firestore.batch();
            for (int i = 0; i < dummyData.length(); i++) {
                JSONObject entry = dummyData.getJSONObject(i);
                HashMap<String, Object> buah = new HashMap<>();
                buah.put("kodeTruck", entry.getString("kodeTruck"));
                buah.put("namaPengemudi", entry.getString("namaPengemudi"));
                buah.put("beratDatang", entry.getDouble("beratDatang"));
                buah.put("beratPulang", entry.getDouble("beratPulang"));
                buah.put("jumlahMatang", entry.getLong("jumlahMatang"));
                buah.put("jumlahMentah", entry.getLong("jumlahMentah"));
                buah.put("jumlahLewatMatang", entry.getLong("jumlahLewatMatang"));
                buah.put("jumlahBusuk", entry.getLong("jumlahBusuk"));
                buah.put("tanggalInput", entry.getString("tanggalInput"));
                buah.put("waktuInput", entry.getString("waktuInput"));

                // Generate document ID
                String documentId = buah.get("kodeTruck") + "_" + buah.get("tanggalInput") + "_" + buah.get("waktuInput");
                batch.set(firestore.collection("Buah").document(documentId), buah);
            }

            // Commit batch
            batch.commit()
                    .addOnSuccessListener(aVoid -> {
                        // Mark dummy data as inserted
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("isDummyDataInserted", true);
                        editor.apply();
                        Log.d("MainActivity", "Dummy data inserted successfully");
                        Toast.makeText(this, "Dummy data inserted successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("MainActivity", "Error inserting dummy data: ", e);
                        Toast.makeText(this, "Failed to insert dummy data", Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            Log.e("MainActivity", "Error parsing dummy data: ", e);
            Toast.makeText(this, "Failed to parse dummy data", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadFirestoreData() {
        // Initialize totals
        AtomicLong totalBuah = new AtomicLong();
        AtomicLong totalMatang = new AtomicLong();
        AtomicLong totalMentah = new AtomicLong();
        AtomicLong totalBusuk = new AtomicLong();
        AtomicLong totalLewatMatang = new AtomicLong();

        // Map to store monthly totals for jumlahMatang
        HashMap<Integer, Float> monthlyTotals = new HashMap<>();
        for (int i = 1; i <= 12; i++) {
            monthlyTotals.put(i, 0f); // Initialize months 1-12
        }

        // Current year for filtering
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);

        // Fetch data from Firestore
        firestore.collection("Buah")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        // Get numeric fields
                        Long jumlahMatang = doc.getLong("jumlahMatang");
                        Long jumlahMentah = doc.getLong("jumlahMentah");
                        Long jumlahBusuk = doc.getLong("jumlahBusuk");
                        Long jumlahLewatMatang = doc.getLong("jumlahLewatMatang");

                        // Add to totals (handle null values)
                        totalMatang.addAndGet(jumlahMatang != null ? jumlahMatang : 0);
                        totalMentah.addAndGet(jumlahMentah != null ? jumlahMentah : 0);
                        totalBusuk.addAndGet(jumlahBusuk != null ? jumlahBusuk : 0);
                        totalLewatMatang.addAndGet(jumlahLewatMatang != null ? jumlahLewatMatang : 0);

                        // Calculate totalBuah
                        totalBuah.set(totalMatang.get() + totalMentah.get() + totalBusuk.get() + totalLewatMatang.get());

                        // Get tanggalInput for monthly aggregation
                        String tanggalInput = doc.getString("tanggalInput");
                        if (tanggalInput != null) {
                            try {
                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                Date date = dateFormat.parse(tanggalInput);
                                if (date != null) {
                                    calendar.setTime(date);
                                    int year = calendar.get(Calendar.YEAR);
                                    int month = calendar.get(Calendar.MONTH) + 1; // Calendar.MONTH is 0-based
                                    if (year == currentYear && jumlahMatang != null) {
                                        monthlyTotals.put(month, monthlyTotals.get(month) + jumlahMatang);
                                    }
                                }
                            } catch (ParseException e) {
                                Log.e("MainActivity", "Error parsing date: " + tanggalInput, e);
                            }
                        }
                    }

                    // Update TextViews
                    txtTotalBuah.setText(String.valueOf(totalBuah.get()));
                    txtTotalBuahMatang.setText(String.valueOf(totalMatang.get()));
                    txtTotalBuahMentah.setText(String.valueOf(totalMentah.get()));
                    txtTotalBuahBusuk.setText(String.valueOf(totalBusuk.get()));
                    txtTotalBuahLewatMatang.setText(String.valueOf(totalLewatMatang.get()));

                    // Setup bar chart
                    updateBarChart(monthlyTotals);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gagal memuat data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("MainActivity", "Firestore error: ", e);
                });
    }

    private void updateBarChart(HashMap<Integer, Float> monthlyTotals) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        // Add entries for months 1-12
        for (int month = 1; month <= 12; month++) {
            entries.add(new BarEntry(month - 1, monthlyTotals.get(month)));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Jumlah Buah Matang");
        dataSet.setColor(Color.parseColor("#4CAF50"));

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.9f); // Bar width

        barChart.setData(data);

        // X-Axis settings
        barChart.getXAxis().setGranularity(1f);
        barChart.getXAxis().setGranularityEnabled(true);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getXAxis().setDrawAxisLine(true);
        barChart.getXAxis().setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setLabelCount(12); // Show all 12 months
        barChart.getXAxis().setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                // Map 0-11 to month names (Jan-Dec)
                String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                int index = (int) value;
                return index >= 0 && index < months.length ? months[index] : "";
            }
        });

        // Y-Axis and chart settings
        barChart.getAxisRight().setEnabled(false); // Disable right Y-axis
        barChart.getDescription().setEnabled(false); // Disable description
        barChart.setFitBars(true); // Fit bars to scale
        barChart.setScaleEnabled(false); // Disable zoom
        barChart.invalidate(); // Refresh chart
    }

    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setSelectedItemId(R.id.nav_home);

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // Already in MainActivity
                return true;
            } else if (id == R.id.nav_add) {
                tampilkanBottomSheetAdd("Main");
            } else if (id == R.id.nav_history) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    FirebaseFirestore.getInstance().collection("Users").document(currentUser.getUid())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                String role = documentSnapshot.getString("role");
                                if ("mandor".equals(role)) {
                                    tampilkanBottomSheetHistory("Main");
                                } else {
                                    Toast.makeText(this, "Hanya Mandor yang dapat mengakses riwayat", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("MainActivity", "Failed to load role: " + e.getMessage());
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
