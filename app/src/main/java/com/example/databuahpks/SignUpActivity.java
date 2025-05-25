package com.example.databuahpks;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class SignUpActivity extends AppCompatActivity {
    private EditText edtUsername, edtPassword;
    private RadioGroup radioGroupRole;
    private RadioButton radioPekerja, radioMandor;
    private Button btnSignUp;
    private TextView txtSignIn;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        radioGroupRole = findViewById(R.id.radioGroupRole);
        radioPekerja = findViewById(R.id.radioPekerja);
        radioMandor = findViewById(R.id.radioMandor);
        btnSignUp = findViewById(R.id.btnSignUp);
        txtSignIn = findViewById(R.id.txtSignIn);
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        btnSignUp.setOnClickListener(v -> {
            String username = edtUsername.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();
            int selectedRoleId = radioGroupRole.getCheckedRadioButtonId();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Harap isi semua field", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedRoleId == -1) {
                Toast.makeText(this, "Pilih role (Pekerja atau Mandor)", Toast.LENGTH_SHORT).show();
                return;
            }

            String role = selectedRoleId == R.id.radioPekerja ? "pekerja" : "mandor";
            String email = username + "@databuahpks.com";

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        String uid = authResult.getUser().getUid();
                        HashMap<String, Object> userData = new HashMap<>();
                        userData.put("username", username);
                        userData.put("uid", uid);
                        userData.put("role", role);

                        firestore.collection("Users").document(uid)
                                .set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("SignUpActivity", "User registered: " + username + ", role: " + role);
                                    Toast.makeText(this, "Berhasil mendaftar", Toast.LENGTH_SHORT).show();
                                    auth.signOut();
                                    startActivity(new Intent(this, SignInActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("SignUpActivity", "Failed to save user data: " + e.getMessage());
                                    Toast.makeText(this, "Gagal menyimpan data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("SignUpActivity", "Sign-up failed: " + e.getMessage());
                        Toast.makeText(this, "Gagal mendaftar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        txtSignIn.setOnClickListener(v -> {
            startActivity(new Intent(this, SignInActivity.class));
        });
    }
}