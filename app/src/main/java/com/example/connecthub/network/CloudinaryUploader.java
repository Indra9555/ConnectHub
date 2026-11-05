package com.example.connecthub.network;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.example.connecthub.utils.CloudinaryConfig;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Uploads images / audio to Cloudinary (unsigned preset).
 *
 * Improvements over the previous version:
 *  - images are down-scaled + re-encoded as JPEG before upload (phone photos are
 *    often 5-12 MB; they are now ~200-500 KB)
 *  - EXIF rotation is respected
 *  - proper JSON parsing, error messages from Cloudinary are surfaced
 *  - connect/read time-outs, streams are always closed
 *  - callbacks are ALWAYS delivered on the main thread
 */
public class CloudinaryUploader {

    public interface UploadCallback {
        void onSuccess(String url);

        void onFailure(Exception e);
    }

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static final int MAX_IMAGE_SIDE = 1600;
    private static final int JPEG_QUALITY = 85;

    private CloudinaryUploader() {
    }

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    public static void uploadImage(
            Context context,
            Uri imageUri,
            UploadCallback callback
    ) {

        Context appContext = context.getApplicationContext();

        EXECUTOR.execute(() -> {
            try {

                byte[] jpeg = compressImage(appContext, imageUri);

                String url = upload(jpeg, "image.jpg", "image/jpeg", "image");

                deliverSuccess(callback, url);

            } catch (Exception e) {
                deliverFailure(callback, e);
            }
        });
    }

    public static void uploadAudio(File file, UploadCallback callback) {

        EXECUTOR.execute(() -> {
            try {

                byte[] data = readFile(file);

                String url = upload(data, file.getName(), "audio/mp4", "auto");

                deliverSuccess(callback, url);

            } catch (Exception e) {
                deliverFailure(callback, e);
            }
        });
    }

    // ---------------------------------------------------------------------
    // Upload
    // ---------------------------------------------------------------------

    private static String upload(
            byte[] data,
            String fileName,
            String mimeType,
            String resourceType
    ) throws Exception {

        String boundary = "----ConnectHub" + UUID.randomUUID().toString().replace("-", "");

        URL url = new URL(
                "https://api.cloudinary.com/v1_1/"
                        + CloudinaryConfig.CLOUD_NAME
                        + "/" + resourceType + "/upload"
        );

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {

            connection.setConnectTimeout(15000);
            connection.setReadTimeout(60000);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setChunkedStreamingMode(0);
            connection.setRequestProperty(
                    "Content-Type",
                    "multipart/form-data; boundary=" + boundary
            );

            try (OutputStream out = connection.getOutputStream()) {

                writeField(out, boundary, "upload_preset", CloudinaryConfig.UPLOAD_PRESET);

                out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                out.write((
                        "Content-Disposition: form-data; name=\"file\"; filename=\""
                                + fileName + "\"\r\n"
                                + "Content-Type: " + mimeType + "\r\n\r\n"
                ).getBytes(StandardCharsets.UTF_8));
                out.write(data);
                out.write("\r\n".getBytes(StandardCharsets.UTF_8));

                out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            }

            int code = connection.getResponseCode();

            InputStream stream = code >= 200 && code < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            String body = readStream(stream);

            if (code < 200 || code >= 300) {
                throw new IOException(extractError(body, code));
            }

            String secureUrl = new JSONObject(body).optString("secure_url", "");

            if (secureUrl.isEmpty()) {
                throw new IOException("Upload succeeded but no URL was returned");
            }

            return secureUrl;

        } finally {
            connection.disconnect();
        }
    }

    private static void writeField(
            OutputStream out,
            String boundary,
            String name,
            String value
    ) throws IOException {

        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write((
                "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                        + value + "\r\n"
        ).getBytes(StandardCharsets.UTF_8));
    }

    private static String extractError(String body, int code) {
        try {
            JSONObject error = new JSONObject(body).optJSONObject("error");
            if (error != null) {
                String message = error.optString("message", "");
                if (!message.isEmpty()) {
                    return "Upload failed: " + message;
                }
            }
        } catch (Exception ignored) {
        }
        return "Upload failed (HTTP " + code + ")";
    }

    // ---------------------------------------------------------------------
    // Image compression
    // ---------------------------------------------------------------------

    private static byte[] compressImage(Context context, Uri uri) throws IOException {

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;

        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("Cannot open image");
            BitmapFactory.decodeStream(in, null, bounds);
        }

        int longest = Math.max(bounds.outWidth, bounds.outHeight);

        int sample = 1;
        while (longest / (sample * 2) >= MAX_IMAGE_SIDE) {
            sample *= 2;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;

        Bitmap bitmap;

        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("Cannot open image");
            bitmap = BitmapFactory.decodeStream(in, null, options);
        }

        if (bitmap == null) {
            throw new IOException("This file is not a supported image");
        }

        // Scale to the exact maximum side
        int side = Math.max(bitmap.getWidth(), bitmap.getHeight());
        float scale = side > MAX_IMAGE_SIDE ? (float) MAX_IMAGE_SIDE / side : 1f;

        int rotation = readRotation(context, uri);

        if (scale != 1f || rotation != 0) {

            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);
            if (rotation != 0) matrix.postRotate(rotation);

            Bitmap transformed = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            if (transformed != bitmap) {
                bitmap.recycle();
            }

            bitmap = transformed;
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output);
        bitmap.recycle();

        return output.toByteArray();
    }

    private static int readRotation(Context context, Uri uri) {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {

            if (in == null) return 0;

            android.media.ExifInterface exif = new android.media.ExifInterface(in);

            int orientation = exif.getAttributeInt(
                    android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL
            );

            switch (orientation) {
                case android.media.ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case android.media.ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case android.media.ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }

        } catch (Exception e) {
            return 0;
        }
    }

    // ---------------------------------------------------------------------
    // IO helpers
    // ---------------------------------------------------------------------

    private static byte[] readFile(File file) throws IOException {
        try (FileInputStream in = new FileInputStream(file);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int read;

            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

            return out.toByteArray();
        }
    }

    private static String readStream(InputStream stream) throws IOException {

        if (stream == null) {
            return "";
        }

        try (InputStream in = stream;
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int read;

            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

            return out.toString("UTF-8");
        }
    }

    // ---------------------------------------------------------------------
    // Callback delivery (main thread)
    // ---------------------------------------------------------------------

    private static void deliverSuccess(UploadCallback callback, String url) {
        if (callback != null) {
            MAIN.post(() -> callback.onSuccess(url));
        }
    }

    private static void deliverFailure(UploadCallback callback, Exception e) {
        if (callback != null) {
            MAIN.post(() -> callback.onFailure(e));
        }
    }
}
