package com.datacapture;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_REQUEST = 1888;
    private ImageView imageView;
    private RecyclerView tableView;
    private TableAdapter tableAdapter;
    private ArrayList<TableModel> tableData = new ArrayList<>();
    private OCRManager ocrManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        Button cameraBtn = findViewById(R.id.cameraBtn);

        tableView = findViewById(R.id.tableView);
        tableAdapter = new TableAdapter(tableData);
        tableView.setLayoutManager(new LinearLayoutManager(this));
        tableView.setAdapter(tableAdapter);

        ocrManager = new OCRManager(this, results -> {
            tableData.clear();
            tableData.addAll(results);
            tableAdapter.notifyDataSetChanged();
        });

        cameraBtn.setOnClickListener(v -> {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, CAMERA_REQUEST);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(photo);
            ocrManager.processImage(photo);
        }
    }
}
