package com.example.databuahpks;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.example.databuahpks.RecentEntryAdapter;

public class MainActivity extends AppCompatActivity {
    private TextView txtTotalBuah, txtTotalBuahMatang, txtTotalBuahMentah, txtTotalBuahBusuk, txtTotalBuahLewatMatang;
    private TextView txtLolosBasik, txtAverageBuah, txtTodayStats, txtWeeklyTotal, txtWeeklyLolos, txtWeeklyTopTruck;
    private ProgressBar progressBar;
    private Spinner spinnerYear;
    private Button btnToggleMatang, btnToggleMentah, btnToggleBusuk, btnToggleLewatMatang, btnViewReport, btnExport;
    private BarChart barChart;
    private RecyclerView recyclerRecentEntries, recyclerNotifications;
    private RecentEntryAdapter recentEntryAdapter;
    private FirebaseFirestore firestore;
    private SharedPreferences prefs;
    private String currentChartType = "Matang";
    private int selectedYear;
    private List<RecentEntryAdapter.BuahEntry> recentEntries = new ArrayList<>();
    private List<String> notifications = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_main);

        // Initialize views
        progressBar = findViewById(R.id.progressBar);
        txtTotalBuah = findViewById(R.id.txtTotalBuah);
        txtTotalBuahMatang = findViewById(R.id.txtTotalBuahMatang);
        txtTotalBuahMentah = findViewById(R.id.txtTotalBuahMentah);
        txtTotalBuahBusuk = findViewById(R.id.txtTotalBuahBusuk);
        txtTotalBuahLewatMatang = findViewById(R.id.txtTotalBuahLewatMatang);
        txtLolosBasik = findViewById(R.id.txtLolosBasik);
        txtAverageBuah = findViewById(R.id.txtAverageBuah);
        txtTodayStats = findViewById(R.id.txtTodayStats);
        txtWeeklyTotal = findViewById(R.id.txtWeeklyTotal);
        txtWeeklyLolos = findViewById(R.id.txtWeeklyLolos);
        txtWeeklyTopTruck = findViewById(R.id.txtWeeklyTopTruck);
        spinnerYear = findViewById(R.id.spinnerYear);
        btnToggleMatang = findViewById(R.id.btnToggleMatang);
        btnToggleMentah = findViewById(R.id.btnToggleMentah);
        btnToggleBusuk = findViewById(R.id.btnToggleBusuk);
        btnToggleLewatMatang = findViewById(R.id.btnToggleLewatMatang);
        btnViewReport = findViewById(R.id.btnViewReport);
        btnExport = findViewById(R.id.btnExport);
        barChart = findViewById(R.id.barChart);
        recyclerRecentEntries = findViewById(R.id.recyclerRecentEntries);
        recyclerNotifications = findViewById(R.id.recyclerNotifications);

        prefs = getSharedPreferences("DataBuahPrefs", MODE_PRIVATE);
        Calendar calendar = Calendar.getInstance();
        selectedYear = calendar.get(Calendar.YEAR);

        // Setup year spinner
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"2023", "2024", "2025"});
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);
        spinnerYear.setSelection(yearAdapter.getPosition(String.valueOf(selectedYear)));
        spinnerYear.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedYear = Integer.parseInt(parent.getItemAtPosition(position).toString());
                loadFirestoreData();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Setup chart toggle buttons
        btnToggleMatang.setOnClickListener(v -> setChartType("Matang"));
        btnToggleMentah.setOnClickListener(v -> setChartType("Mentah"));
        btnToggleBusuk.setOnClickListener(v -> setChartType("Busuk"));
        btnToggleLewatMatang.setOnClickListener(v -> setChartType("Lewat Matang"));
        updateToggleButtons();

        // Setup recent entries
        recyclerRecentEntries.setLayoutManager(new LinearLayoutManager(this));
        recentEntryAdapter = new RecentEntryAdapter(recentEntries);
        recyclerRecentEntries.setAdapter(recentEntryAdapter);

        // Setup notifications
        recyclerNotifications.setLayoutManager(new LinearLayoutManager(this));
        recyclerNotifications.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_notification, parent, false);
                return new RecyclerView.ViewHolder(view) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                TextView txtNotification = holder.itemView.findViewById(R.id.txtNotification);
                txtNotification.setText(notifications.get(position));
            }

            @Override
            public int getItemCount() {
                return Math.min(notifications.size(), 5);
            }
        });

        // Setup action buttons
        btnViewReport.setOnClickListener(v -> {
            startActivity(new Intent(this, RiwayatBuahActivity.class));
            finish();
        });
        btnExport.setOnClickListener(v -> Toast.makeText(this, "Pengembangan", Toast.LENGTH_SHORT).show());

        insertDummyData();
        setupBottomNav();
        loadFirestoreData();
    }

    private void setChartType(String type) {
        currentChartType = type;
        updateToggleButtons();
        loadFirestoreData();
    }

    private void updateToggleButtons() {
        btnToggleMatang.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                currentChartType.equals("Matang") ? 0xFF4CAF50 : 0xFFB0BEC5));
        btnToggleMentah.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                currentChartType.equals("Mentah") ? 0xFFFFC107 : 0xFFB0BEC5));
        btnToggleBusuk.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                currentChartType.equals("Busuk") ? 0xFFF44336 : 0xFFB0BEC5));
        btnToggleLewatMatang.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                currentChartType.equals("Lewat Matang") ? 0xFFFF9800 : 0xFFB0BEC5));
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
            return "[]";
        }
    }

    private void insertDummyData() {
        boolean isDummyDataInserted = prefs.getBoolean("isDummyDataInserted", false);
        if (isDummyDataInserted) {
            Log.d("MainActivity", "Dummy data already inserted, skipping.");
            return;
        }

        try {
            String jsonString = readDummyDataFromRaw();
            JSONArray dummyData = new JSONArray(jsonString);
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

                String documentId = buah.get("kodeTruck") + "_" + buah.get("tanggalInput") + "_" + buah.get("waktuInput");
                batch.set(firestore.collection("Buah").document(documentId), buah);
            }

            batch.commit()
                    .addOnSuccessListener(aVoid -> {
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
        progressBar.setVisibility(View.VISIBLE);

        AtomicLong totalBuah = new AtomicLong();
        AtomicLong totalMatang = new AtomicLong();
        AtomicLong totalMentah = new AtomicLong();
        AtomicLong totalBusuk = new AtomicLong();
        AtomicLong totalLewatMatang = new AtomicLong();
        AtomicLong todayWeight = new AtomicLong();
        AtomicLong todayMatang = new AtomicLong();
        AtomicLong weeklyTotal = new AtomicLong();
        AtomicLong weeklyMatang = new AtomicLong();
        Map<String, Integer> truckCounts = new HashMap<>();
        HashMap<Integer, Float> monthlyTotals = new HashMap<>();
        HashMap<Integer, Float> monthlyLolos = new HashMap<>();
        recentEntries.clear();
        notifications.clear();
        for (int i = 1; i <= 12; i++) {
            monthlyTotals.put(i, 0f);
            monthlyLolos.put(i, 0f);
        }

        Calendar calendar = Calendar.getInstance();
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        String weekStartDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

        List<HashMap<String, Object>> allEntries = new ArrayList<>();
        firestore.collection("Buah")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Long jumlahMatang = doc.getLong("jumlahMatang");
                        Long jumlahMentah = doc.getLong("jumlahMentah");
                        Long jumlahBusuk = doc.getLong("jumlahBusuk");
                        Long jumlahLewatMatang = doc.getLong("jumlahLewatMatang");
                        Double beratDatang = doc.getDouble("beratDatang");
                        String kodeTruck = doc.getString("kodeTruck");
                        String tanggalInput = doc.getString("tanggalInput");

                        jumlahMatang = jumlahMatang != null ? jumlahMatang : 0;
                        jumlahMentah = jumlahMentah != null ? jumlahMentah : 0;
                        jumlahBusuk = jumlahBusuk != null ? jumlahBusuk : 0;
                        jumlahLewatMatang = jumlahLewatMatang != null ? jumlahLewatMatang : 0;
                        beratDatang = beratDatang != null ? beratDatang : 0.0;

                        totalMatang.addAndGet(jumlahMatang);
                        totalMentah.addAndGet(jumlahMentah);
                        totalBusuk.addAndGet(jumlahBusuk);
                        totalLewatMatang.addAndGet(jumlahLewatMatang);
                        totalBuah.set(totalMatang.get() + totalMentah.get() + totalBusuk.get() + totalLewatMatang.get());

                        if (tanggalInput != null) {
                            try {
                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                Date date = dateFormat.parse(tanggalInput);
                                if (date != null) {
                                    calendar.setTime(date);
                                    int year = calendar.get(Calendar.YEAR);
                                    int month = calendar.get(Calendar.MONTH) + 1;

                                    // Monthly aggregation
                                    if (year == selectedYear) {
                                        float value = 0;
                                        switch (currentChartType) {
                                            case "Matang":
                                                value = jumlahMatang;
                                                break;
                                            case "Mentah":
                                                value = jumlahMentah;
                                                break;
                                            case "Busuk":
                                                value = jumlahBusuk;
                                                break;
                                            case "Lewat Matang":
                                                value = jumlahLewatMatang;
                                                break;
                                        }
                                        monthlyTotals.put(month, monthlyTotals.get(month) + value);

                                        // Lolos percentage for tooltip
                                        long total = jumlahMatang + jumlahMentah + jumlahBusuk + jumlahLewatMatang;
                                        float lolos = total > 0 ? (jumlahMatang * 100f) / total : 0;
                                        monthlyLolos.put(month, monthlyLolos.get(month) + lolos);
                                    }

                                    // Today's stats
                                    if (tanggalInput.equals(todayDate)) {
                                        todayWeight.addAndGet(beratDatang.longValue());
                                        todayMatang.addAndGet(jumlahMatang);
                                    }

                                    // Weekly stats
                                    if (tanggalInput.compareTo(weekStartDate) >= 0) {
                                        weeklyTotal.addAndGet(beratDatang.longValue());
                                        weeklyMatang.addAndGet(jumlahMatang);
                                        truckCounts.put(kodeTruck, truckCounts.getOrDefault(kodeTruck, 0) + 1);
                                    }
                                }
                            } catch (ParseException e) {
                                Log.e("MainActivity", "Error parsing date: " + tanggalInput, e);
                            }
                        }

                        // Recent entries
                        long total = jumlahMatang + jumlahMentah + jumlahBusuk + jumlahLewatMatang;
                        double lolosPercentage = total > 0 ? (jumlahMatang * 100.0) / total : 0;
                        boolean isLolos = lolosPercentage >= 90;
                        if (kodeTruck != null) {
                            recentEntries.add(new RecentEntryAdapter.BuahEntry(kodeTruck, lolosPercentage, isLolos));
                            // Notifications
                            if (!isLolos && tanggalInput != null && tanggalInput.equals(todayDate)) {
                                notifications.add(String.format("🚚 Truk %s hari ini gagal lolos karena banyak buah mentah", kodeTruck));
                            }
                        }

                        Long finalJumlahMatang = jumlahMatang;
                        Long finalJumlahMentah = jumlahMentah;
                        Long finalJumlahBusuk = jumlahBusuk;
                        Long finalJumlahLewatMatang = jumlahLewatMatang;
                        allEntries.add(new HashMap<String, Object>() {{
                            put("tanggalInput", tanggalInput);
                            put("jumlahMatang", finalJumlahMatang);
                            put("jumlahMentah", finalJumlahMentah);
                            put("jumlahBusuk", finalJumlahBusuk);
                            put("jumlahLewatMatang", finalJumlahLewatMatang);
                        }});
                    }

                    // Calculate averages
                    long days = 30; // Approximate
                    long months = 12;
                    double avgDaily = totalBuah.get() / (double) days;
                    double avgMonthly = totalBuah.get() / (double) months;

                    // Update TextViews
                    double lolosPercentage = totalBuah.get() > 0 ? (totalMatang.get() * 100.0) / totalBuah.get() : 0;
                    txtTotalBuah.setText(String.valueOf(totalBuah.get()));
                    txtTotalBuahMatang.setText(String.valueOf(totalMatang.get()));
                    txtTotalBuahMentah.setText(String.valueOf(totalMentah.get()));
                    txtTotalBuahBusuk.setText(String.valueOf(totalBusuk.get()));
                    txtTotalBuahLewatMatang.setText(String.valueOf(totalLewatMatang.get()));
                    txtLolosBasik.setText(String.format("Lolos Basik: %.1f%%", lolosPercentage));
                    txtLolosBasik.setTextColor(lolosPercentage >= 90 ? 0xFF4CAF50 : 0xFFF44336);
                    txtAverageBuah.setText(String.format("Harian: %.1f, Bulanan: %.1f", avgDaily, avgMonthly));
                    long todayTotal = todayWeight.get() + todayMatang.get();
                    double todayLolos = todayTotal > 0 ? (todayMatang.get() * 100.0) / todayTotal : 0;
                    txtTodayStats.setText(String.format("%.1f kg, Lolos: %s", todayWeight.get() / 1000.0, todayLolos >= 90 ? "✅" : "❌"));
                    txtTodayStats.setTextColor(todayLolos >= 90 ? 0xFF4CAF50 : 0xFFF44336);

                    // Weekly summary
                    double weeklyLolos = weeklyTotal.get() > 0 ? (weeklyMatang.get() * 100.0) / weeklyTotal.get() : 0;
                    String topTruck = truckCounts.entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(e -> String.format("%s (%dx)", e.getKey(), e.getValue()))
                            .orElse("-");
                    txtWeeklyTotal.setText(String.format("Total buah: %.1f kg", weeklyTotal.get() / 1000.0));
                    txtWeeklyLolos.setText(String.format("Rata-rata lolos: %.1f%%", weeklyLolos));
                    txtWeeklyTopTruck.setText("Truk terbanyak: " + topTruck);

                    // Notifications
                    if (!recentEntries.isEmpty()) {
                        long lastWeekTotal = allEntries.stream()
                                .filter(e -> {
                                    String date = (String) e.get("tanggalInput");
                                    try {
                                        return date != null && date.compareTo(weekStartDate) < 0 &&
                                                date.compareTo(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                                        .format(new Date(calendar.getTimeInMillis() - 7 * 24 * 3600 * 1000L))) >= 0;
                                    } catch (Exception ex) {
                                        return false;
                                    }
                                })
                                .mapToLong(e -> ((Number) e.get("jumlahMatang")).longValue() +
                                        ((Number) e.get("jumlahMentah")).longValue() +
                                        ((Number) e.get("jumlahBusuk")).longValue() +
                                        ((Number) e.get("jumlahLewatMatang")).longValue())
                                .sum();
                        double change = lastWeekTotal > 0 ? ((weeklyTotal.get() - lastWeekTotal) * 100.0) / lastWeekTotal : 0;
                        String trend = change >= 0 ? "naik" : "menurun";
                        notifications.add(String.format("📊 Total buah masuk minggu ini %s %.1f%% dibanding minggu lalu", trend, Math.abs(change)));
                    }

                    // Update RecyclerViews
                    recentEntryAdapter.notifyDataSetChanged();
                    recyclerNotifications.getAdapter().notifyDataSetChanged();

                    // Update bar chart
                    updateBarChart(monthlyTotals, monthlyLolos);

                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gagal memuat data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("MainActivity", "Firestore error: ", e);
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void updateBarChart(HashMap<Integer, Float> monthlyTotals, HashMap<Integer, Float> monthlyLolos) {
        ArrayList<BarEntry> barEntries = new ArrayList<>();
        ArrayList<Entry> lineEntries = new ArrayList<>();
        float sum = 0;
        int count = 0;

        for (int month = 1; month <= 12; month++) {
            float value = monthlyTotals.get(month);
            barEntries.add(new BarEntry(month - 1, value));
            sum += value;
            if (value > 0) count++;
            lineEntries.add(new Entry(month - 1, count > 0 ? sum / count : 0)); // Moving average for trend
        }

        BarDataSet barDataSet = new BarDataSet(barEntries, "Jumlah Buah " + currentChartType);
        barDataSet.setColor(Color.parseColor(currentChartType.equals("Matang") ? "#4CAF50" :
                currentChartType.equals("Mentah") ? "#FFC107" :
                        currentChartType.equals("Busuk") ? "#F44336" : "#FF9800"));

        LineDataSet lineDataSet = new LineDataSet(lineEntries, "Tren");
        lineDataSet.setColor(Color.BLUE);
        lineDataSet.setLineWidth(2f);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setDrawValues(false);

        BarData barData = new BarData(barDataSet);
        barData.setBarWidth(0.4f);
        LineData lineData = new LineData(lineDataSet);
        barChart.setData(barData);

        // Add trend line
        barChart.getData();
        barChart.setData(new CombinedData() {{
            setData(barData);
            setData(lineData);
        }}.getBarData());

        // X-Axis
        barChart.getXAxis().setGranularity(1f);
        barChart.getXAxis().setGranularityEnabled(true);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getXAxis().setDrawAxisLine(true);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setLabelCount(12);
        barChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                int index = (int) value;
                return index >= 0 && index < months.length ? months[index] : "";
            }
        });

        // Y-Axis and chart settings
        barChart.getAxisRight().setEnabled(false);
        barChart.getDescription().setEnabled(false);
        barChart.setFitBars(true);
        barChart.setScaleEnabled(false);

        // Tooltip
        barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int month = (int) e.getX() + 1;
                float value = e.getY();
                float lolos = monthlyLolos.get(month) / (monthlyTotals.get(month) > 0 ? monthlyTotals.get(month) : 1);
                Toast.makeText(MainActivity.this,
                        String.format("%s: %.0f buah (%.1f%% lolos)", getMonthName(month), value, lolos),
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected() {}
        });

        barChart.invalidate();
    }

    private String getMonthName(int month) {
        String[] months = {"", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        return month >= 1 && month <= 12 ? months[month] : "";
    }

    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setSelectedItemId(R.id.nav_home);

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_add) {
                tampilkanBottomSheetAdd("Main");
            } else if (id == R.id.nav_history) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    firestore.collection("Users").document(currentUser.getUid())
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