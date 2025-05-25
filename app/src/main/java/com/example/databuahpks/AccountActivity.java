package com.example.databuahpks;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AccountActivity extends AppCompatActivity {
    private TextView txtUsername;
    private Button btnLogout;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        setupBottomNav();

        txtUsername = findViewById(R.id.txtUsername);
        btnLogout = findViewById(R.id.btnLogout);
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            // No user logged in, redirect to SignInActivity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }

        // Load username from Firestore
        String uid = currentUser.getUid();
        firestore.collection("Users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        txtUsername.setText("Username: " + (username != null ? username : "Unknown"));
                        Log.d("AccountActivity", "Loaded username: " + username);
                    } else {
                        txtUsername.setText("Username: Unknown");
                        Log.w("AccountActivity", "User document not found for UID: " + uid);
                    }
                })
                .addOnFailureListener(e -> {
                    txtUsername.setText("Username: Error");
                    Log.e("AccountActivity", "Failed to load username: " + e.getMessage());
                    Toast.makeText(this, "Gagal memuat username: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            Toast.makeText(this, "Berhasil keluar", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SignInActivity.class));
            finish();
        });
    }

    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setSelectedItemId(R.id.nav_profile); // Assuming nav_account exists in bottom_nav_menu

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(this, InputTruckActivity.class));
            } else if (id == R.id.nav_history) {
                tampilkanBottomSheetHistory();
            } else if (id == R.id.nav_profile) {
                // Already in AccountActivity
            }
            return true;
        });
    }

    private void tampilkanBottomSheetHistory() {
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_history, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);

        Button btnRiwayatBuah = view.findViewById(R.id.btnRiwayatBuah);
        Button btnRiwayatTruck = view.findViewById(R.id.btnRiwayatTruck);

        btnRiwayatBuah.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, RiwayatBuahActivity.class));
        });

        btnRiwayatTruck.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, RiwayatTruckActivity.class));
        });

        dialog.show();
    }
}