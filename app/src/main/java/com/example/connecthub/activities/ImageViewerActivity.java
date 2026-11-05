package com.example.connecthub.activities;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.connecthub.R;

public class ImageViewerActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Dark backdrop behind the system bars as well
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));

        setContentView(R.layout.activity_image_viewer);

        String imageUrl = getIntent().getStringExtra("image");

        if (imageUrl == null || imageUrl.isEmpty()) {
            finish();
            return;
        }

        ImageView imgFull = findViewById(R.id.imgFull);
        View progressBar = findViewById(R.id.progressBar);

        Glide.with(this)
                .load(imageUrl)
                .error(R.drawable.ic_image)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<Drawable> target, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                                                   Target<Drawable> target,
                                                   DataSource dataSource, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(imgFull);

        imgFull.setOnClickListener(v -> finish());
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
    }
}
