package com.example.connecthub.activities;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.connecthub.R;

public class ImageViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        ImageView photoView = findViewById(R.id.photoView);

        String imageUrl = getIntent().getStringExtra("image");

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(photoView);

        photoView.setOnClickListener(v -> finish());
    }
}