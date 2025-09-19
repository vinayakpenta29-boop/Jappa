package com.example.datacapture;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int FRONT_CAMERA_REQUEST = 2101;
    private static final int BACK_CAMERA_REQUEST  = 2102;
    private static final int FRONT_GALLERY_REQUEST = 2103;
    private static final int BACK_GALLERY_REQUEST  = 2104;
    private static final int CAMERA_PERMISSION_CODE = 1001;

    private ImageView imageView;
    private RecyclerView tableView;
    private TableAdapter tableAdapter;
    private ArrayList<TableModel> tableData = new ArrayList<>();
    private OCRManager ocrManager;
    private Bitmap frontImage = null;
    private Bitmap backImage = null;

    private SharedPreferences prefs;
    private final String TABLE_PREF_KEY = "persisted_table";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.imageView);

        // Two buttons for Front and Back
        Button frontBtn = findViewById(R.id.frontBtn);
        Button backBtn  = findViewById(R.id.backBtn);

        // Setup RecyclerView and persistent table data
        tableView = findViewById(R.id.tableView);
        tableView.setLayoutManager(new LinearLayoutManager(this));
        tableAdapter = new TableAdapter(tableData);
        tableView.setAdapter(tableAdapter);

        prefs = getSharedPreferences("tablepref", Context.MODE_PRIVATE);
        loadTableData();

        ocrManager = new OCRManager(this, new OCRManager.OCRResultListener() {
            @Override
            public void onOCRResult(TableModel result) {
                // Keep Sr. number incrementing
                result.no = tableData.size() + 1;
                tableData.add(result);
                tableAdapter.notifyDataSetChanged();
                saveTableData();
                // Clear previews for next input
                frontImage = null;
                backImage = null;
                imageView.setImageDrawable(null);
            }
        });

        frontBtn.setOnClickListener(v -> {
            selectImage(true);
        });

        backBtn.setOnClickListener(v -> {
            selectImage(false);
        });
    }

    private void selectImage(boolean isFront) {
        String[] options = {"Camera", "Gallery"};
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(isFront ? "Select Front Side" : "Select Back Side")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Camera
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                        } else {
                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            startActivityForResult(intent, isFront ? FRONT_CAMERA_REQUEST : BACK_CAMERA_REQUEST);
                        }
                    } else {
                        // Gallery
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(intent, isFront ? FRONT_GALLERY_REQUEST : BACK_GALLERY_REQUEST);
                    }
                }).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission just granted, notify user to retry
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bitmap photo = null;

        if (resultCode == RESULT_OK && data != null) {
            if ((requestCode == FRONT_CAMERA_REQUEST || requestCode == BACK_CAMERA_REQUEST) && data.getExtras() != null) {
                photo = (Bitmap) data.getExtras().get("data");
            } else if ((requestCode == FRONT_GALLERY_REQUEST || requestCode == BACK_GALLERY_REQUEST) && data.getData() != null) {
                try {
                    photo = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                } catch (Exception e) { e.printStackTrace(); }
            }
        }

        if (photo != null) {
            imageView.setImageBitmap(photo);
            if (requestCode == FRONT_CAMERA_REQUEST || requestCode == FRONT_GALLERY_REQUEST) {
                frontImage = photo;
            } else if (requestCode == BACK_CAMERA_REQUEST || requestCode == BACK_GALLERY_REQUEST) {
                backImage = photo;
            }
            // If both provided, run extraction
            if (frontImage != null && backImage != null) {
                ocrManager.setFrontImage(frontImage);
                ocrManager.setBackImage(backImage);
                ocrManager.processBoth();
            }
        }
    }

    // ----------- Persistence using Gson and SharedPreferences ------------

    private void saveTableData() {
        Gson gson = new Gson();
        String json = gson.toJson(tableData);
        prefs.edit().putString(TABLE_PREF_KEY, json).apply();
    }

    private void loadTableData() {
        Gson gson = new Gson();
        String json = prefs.getString(TABLE_PREF_KEY, null);
        if (json != null) {
            try {
                ArrayList<TableModel> saved = gson.fromJson(json, new TypeToken<ArrayList<TableModel>>(){}.getType());
                if (saved != null) {
                    tableData.clear();
                    tableData.addAll(saved);
                }
            } catch (Exception ignored) { }
        }
    }
}
