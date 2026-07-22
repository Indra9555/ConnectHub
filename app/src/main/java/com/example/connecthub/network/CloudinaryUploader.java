package com.example.connecthub.network;


import android.content.Context;
import android.net.Uri;

import com.example.connecthub.utils.CloudinaryConfig;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;



public class CloudinaryUploader {


    public interface UploadCallback{

        void onSuccess(String imageUrl);

        void onFailure(Exception e);

    }



    public static void uploadImage(
            Context context,
            Uri imageUri,
            UploadCallback callback
    ){


        new Thread(() -> {


            try{


                String urlString =
                        "https://api.cloudinary.com/v1_1/"
                                + CloudinaryConfig.CLOUD_NAME
                                + "/image/upload";



                URL url = new URL(urlString);


                HttpURLConnection connection =
                        (HttpURLConnection) url.openConnection();



                connection.setRequestMethod("POST");

                connection.setDoOutput(true);



                String boundary =
                        "----ConnectHubBoundary";



                connection.setRequestProperty(
                        "Content-Type",
                        "multipart/form-data; boundary="
                                + boundary
                );



                DataOutputStream output =
                        new DataOutputStream(
                                connection.getOutputStream()
                        );



                output.writeBytes(
                        "--" + boundary + "\r\n"
                );


                output.writeBytes(
                        "Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n"
                );


                output.writeBytes(
                        CloudinaryConfig.UPLOAD_PRESET
                                + "\r\n"
                );



                InputStream inputStream =
                        context.getContentResolver()
                                .openInputStream(imageUri);



                output.writeBytes(
                        "--" + boundary + "\r\n"
                );


                output.writeBytes(
                        "Content-Disposition: form-data; name=\"file\"; filename=\"image.jpg\"\r\n\r\n"
                );



                byte[] buffer = new byte[1024];

                int bytes;



                while((bytes=inputStream.read(buffer))!=-1){

                    output.write(buffer,0,bytes);

                }



                output.writeBytes(
                        "\r\n--" + boundary + "--\r\n"
                );



                output.flush();


                output.close();



                InputStream response =
                        connection.getInputStream();



                StringBuilder result =
                        new StringBuilder();



                while((bytes=response.read(buffer))!=-1){

                    result.append(
                            new String(buffer,0,bytes)
                    );

                }



                String json=result.toString();



                String imageUrl =
                        json.split("\"secure_url\":\"")[1]
                                .split("\"")[0];



                callback.onSuccess(imageUrl);



            }
            catch(Exception e){


                callback.onFailure(e);


            }



        }).start();



    }



}