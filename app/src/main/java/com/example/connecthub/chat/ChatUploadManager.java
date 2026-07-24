package com.example.connecthub.chat;

import android.content.Context;
import android.net.Uri;

import com.example.connecthub.network.CloudinaryUploader;

public class ChatUploadManager {

    public interface UploadListener {

        void onStart();

        void onSuccess(String imageUrl);

        void onFailure(Exception e);

    }

    public static void uploadImage(
            Context context,
            Uri imageUri,
            UploadListener listener
    ) {

        listener.onStart();

        CloudinaryUploader.uploadImage(
                context,
                imageUri,
                new CloudinaryUploader.UploadCallback() {

                    @Override
                    public void onSuccess(String imageUrl) {
                        listener.onSuccess(imageUrl);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        listener.onFailure(e);
                    }

                });

    }

}