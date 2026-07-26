package com.example.connecthub.chat;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class VoiceUploadManager {

    public interface UploadListener {
        void onStart();
        void onSuccess(String voiceUrl);
        void onFailure(Exception e);
    }

    // Cloudinary Details
    private static final String CLOUD_NAME = "okxussox";
    private static final String UPLOAD_PRESET = "connecthub_upload";

    public static void uploadVoice(File voiceFile, UploadListener listener) {

        new Thread(() -> {

            listener.onStart();

            HttpURLConnection connection = null;
            DataOutputStream outputStream = null;

            try {

                String boundary = "===" + System.currentTimeMillis() + "===";
                String lineEnd = "\r\n";
                String twoHyphens = "--";

                URL url = new URL(
                        "https://api.cloudinary.com/v1_1/"
                                + CLOUD_NAME
                                + "/auto/upload"
                );

                connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setUseCaches(false);
                connection.setRequestMethod("POST");

                connection.setRequestProperty(
                        "Content-Type",
                        "multipart/form-data; boundary=" + boundary
                );

                outputStream = new DataOutputStream(connection.getOutputStream());

                // upload preset
                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes(
                        "Content-Disposition: form-data; name=\"upload_preset\""
                                + lineEnd
                );
                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(UPLOAD_PRESET + lineEnd);

                // voice file
                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes(
                        "Content-Disposition: form-data; name=\"file\"; filename=\""
                                + voiceFile.getName()
                                + "\""
                                + lineEnd
                );

                outputStream.writeBytes("Content-Type: audio/mp4" + lineEnd);
                outputStream.writeBytes(lineEnd);

                FileInputStream fileInputStream = new FileInputStream(voiceFile);

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                fileInputStream.close();

                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                outputStream.flush();

                int responseCode = connection.getResponseCode();

                Scanner scanner;

                if (responseCode == HttpURLConnection.HTTP_OK) {

                    scanner = new Scanner(connection.getInputStream());

                    StringBuilder response = new StringBuilder();

                    while (scanner.hasNextLine()) {
                        response.append(scanner.nextLine());
                    }

                    scanner.close();

                    JSONObject json = new JSONObject(response.toString());

                    listener.onSuccess(json.getString("secure_url"));

                } else {

                    scanner = new Scanner(connection.getErrorStream());

                    StringBuilder error = new StringBuilder();

                    while (scanner.hasNextLine()) {
                        error.append(scanner.nextLine());
                    }

                    scanner.close();

                    listener.onFailure(
                            new Exception(error.toString())
                    );
                }

            } catch (Exception e) {

                listener.onFailure(e);

            } finally {

                try {
                    if (outputStream != null)
                        outputStream.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }

        }).start();
    }
}