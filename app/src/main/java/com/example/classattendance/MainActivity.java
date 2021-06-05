package com.example.classattendance;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.database.Cursor;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;


import com.example.classattendance.ui.ClassAdapter;
import com.example.classattendance.ui.ClassItem;
import com.example.classattendance.ui.DatabaseHelper;
import com.example.classattendance.ui.MyDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    FloatingActionButton addBtn;
    ClassAdapter classAdapter;
    RecyclerView.LayoutManager layoutManager;
    ArrayList<ClassItem> classItems = new ArrayList<>();
    Toolbar toolbar;
    DatabaseHelper dbhelper;
    FirebaseAuth firebaseAuth;
    ImageButton loginBtn;
    FirebaseDatabase database;
    DatabaseReference reference;
    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginBtn = findViewById(R.id.iconSave);
//        loginBtn.setVisibility(View.INVISIBLE);
//        loginBtn.setBackgroundResource(R.drawable.signout_icon);

        firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this,LoginActivity.class));
        } else {
            loginBtn.setOnClickListener(v -> {
                firebaseAuth.signOut();
                startActivity(new Intent(MainActivity.this,LoginActivity.class));
            });
        }

        uid = firebaseAuth.getCurrentUser().getUid();

        database = FirebaseDatabase.getInstance();


        dbhelper = new DatabaseHelper(this);

        addBtn = findViewById(R.id.addBtn);
        addBtn.setOnClickListener(v -> showDialog());

        loadData();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        classAdapter = new ClassAdapter(this,classItems);
        recyclerView.setAdapter(classAdapter);
        classAdapter.setOnItemClickListener(position -> goToItemActivity(position));
        setToolbar();
//        Log.e("pritom",dbhelper.getClassTable().getString(1)+" yo");
        classAdapter.notifyDataSetChanged();
    }

    private void loadData() {
        Cursor cursor = dbhelper.getClassTable();

        classItems.clear();
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndex(dbhelper.C_ID));
            String className = cursor.getString(cursor.getColumnIndex(dbhelper.CLASS_NAME_KEY));
            String subjectName = cursor.getString(cursor.getColumnIndex(dbhelper.SUBJECT_NAME_KEY));
//            Log.e("pritom",id+className+subjectName+"           ");
            classItems.add(new ClassItem(id,className,subjectName));
        }
    }

    public void setToolbar() {
        toolbar = findViewById(R.id.toolbar);
        TextView title = findViewById(R.id.title_toolbar);
        TextView subTitle = findViewById(R.id.subtitle_toolbar);
        ImageButton back = findViewById(R.id.backButton);

        title.setText("Attendance");
        title.setTextSize(25f);
        subTitle.setVisibility(View.GONE);
        back.setVisibility(View.INVISIBLE);
    }

    private void goToItemActivity(int position) {
        Intent intent = new Intent(this, AttendanceActivity.class);
        intent.putExtra("className",classItems.get(position).getClassName());
        intent.putExtra("subjectName",classItems.get(position).getSubjectName());
        intent.putExtra("position",position);
        intent.putExtra("cid",classItems.get(position).getCid());
        intent.putExtra("fire_cid",classItems.get(position).getClassName()+classItems.get(position).getSubjectName());
        intent.putExtra("uid",uid);
        startActivity(intent);
    }

    public void showDialog() {
        MyDialog dialog = new MyDialog();
        dialog.show(getSupportFragmentManager(),MyDialog.CLASS_ADD_DIALOG);
        dialog.setListener((className,subName)->addClass(className,subName));

    }

    private void addClass(String className,String subName) {
        long cid = dbhelper.addClass(className,subName);
        ClassItem classItem = new ClassItem(cid,className,subName);

        //Firebase data add
        reference = database.getReference(uid+"").child(className+subName+"");
        Map<String,Object> map = new HashMap<>();
        map.put("className",className);
        map.put("subName",subName);

        reference.updateChildren(map);

        classItems.add(classItem);
        classAdapter.notifyDataSetChanged();

    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case 0 :
                showUpdateDialog(item.getGroupId());
                break;
            case 1 :
                deleteClass(item.getGroupId());
                break;
        }
        return super.onContextItemSelected(item);
    }

    public void showUpdateDialog(int position) {
        MyDialog dialog = new MyDialog();
        dialog.show(getSupportFragmentManager(),MyDialog.CLASS_UPDATE_DIALOG);
        dialog.setListener((className,subjectName)->updateClass(position,className,subjectName));
    }

    private void updateClass(int position, String className, String subjectName) {
        long cid = classItems.get(position).getCid();

        dbhelper.updateClass(cid,className,subjectName);
        classItems.get(position).setClassName(className);
        classItems.get(position).setSubjectName(subjectName);

        //Firebase data update
        reference = database.getReference(uid+"").child(cid+"");
        Map<String,Object> map = new HashMap<>();
        map.put("className",className);
        map.put("subName",subjectName);
        reference.updateChildren(map);

        classAdapter.notifyDataSetChanged();
    }

    private void deleteClass(int groupId) {
        dbhelper.deleteClass(classItems.get(groupId).getCid());

        //Firebase data update
        reference = database.getReference(uid+"").child(classItems.get(groupId).getCid()+"");
        reference.setValue(null);

        classItems.remove(groupId);
        classAdapter.notifyDataSetChanged();

    }

}