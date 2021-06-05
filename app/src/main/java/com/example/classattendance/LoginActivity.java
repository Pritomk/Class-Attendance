package com.example.classattendance;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.classattendance.ui.DatabaseHelper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    EditText emailText,passText;
    Button regBtn,logBtn;
    FirebaseAuth firebaseAuth;
    FirebaseDatabase database;
    DatabaseReference reference;
    DatabaseHelper dbhelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailText = findViewById(R.id.emailTextId);
        passText = findViewById(R.id.passTextId);
        regBtn = findViewById(R.id.regBtn);
        logBtn = findViewById(R.id.logBtn);

        firebaseAuth = FirebaseAuth.getInstance();
        dbhelper = new DatabaseHelper(this);
        database = FirebaseDatabase.getInstance();

        logBtn.setOnClickListener(v -> logFunc());
        regBtn.setOnClickListener(v->regFunc());
    }

    private void regFunc() {
        String email = emailText.getText().toString();
        String pass = passText.getText().toString();

        if (!pass.isEmpty()) {
            if (!email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                firebaseAuth.createUserWithEmailAndPassword(email,pass)
                        .addOnSuccessListener(authResult -> {
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            Toast.makeText(LoginActivity.this, "Registered successfully", Toast.LENGTH_LONG);
                        }).addOnFailureListener(e -> Toast.makeText(LoginActivity.this, "Register Problem", Toast.LENGTH_LONG));
            }

        } else {
            passText.setError("Enter password");
        }
    }

    private void logFunc() {
        String email = emailText.getText().toString();
        String pass = passText.getText().toString();

        if (!pass.isEmpty()) {
            if (!email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                firebaseAuth.signInWithEmailAndPassword(email,pass)
                        .addOnSuccessListener(authResult -> {
                            loadData();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            Toast.makeText(LoginActivity.this, "Registered successfully", Toast.LENGTH_LONG);
                        }).addOnFailureListener(e -> Toast.makeText(LoginActivity.this, "Register Problem", Toast.LENGTH_LONG));
            }

        } else {
            passText.setError("Enter password");
        }
    }

    // Data fetch from Firebase to SQLite
    private void loadData() {
        reference = database.getReference().child(firebaseAuth.getCurrentUser().getUid()+"");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot classShot : snapshot.getChildren()) {
                    String className = classShot.child("className").getValue().toString();
                    String subName = classShot.child("subName").getValue().toString();
                    long cid = dbhelper.addClass(className,subName);

                    Log.e("firebasedata","Class and sub name "+className+subName);

                    for (DataSnapshot studentShot : classShot.child("student").getChildren()) {
                        String studentName = studentShot.child("studentName").getValue().toString();
                        String roll = studentShot.child("roll").getValue().toString();
                        long sid = dbhelper.addStudent(cid,Integer.parseInt(roll),studentName);

                        Log.e("firebasedata","student name and roll "+studentName+roll);

                        for (DataSnapshot statusShot : studentShot.getChildren()) {
                            String statusDate = statusShot.getKey();
                            String status = statusShot.getValue().toString();
                            if (!statusDate.equals("studentName") && !statusDate.equals("roll")) {
                                dbhelper.addStatus(sid,cid,changeDateFormat(statusDate),status);

                                Log.e("firebasedata","status data "+statusDate+status);
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getApplicationContext(),"Something what's wrong",Toast.LENGTH_SHORT);
            }
        });
    }


    private String changeDateFormat(String date) {
        String res = "";
        for (int i = 0;i < date.length();i++) {
            if (date.charAt(i) == '-') {
                res += '.';
                continue;
            }
            res += date.charAt(i);
        }
        return res;
    }
}