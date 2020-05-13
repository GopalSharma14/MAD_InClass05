package com.example.inclass05;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private TextView list_tv;

    private Button go_btn;
    private Button next_btn;
    private Button prev_btn;

    private ImageView display_iv;
    private ImageView next_iv;
    private ImageView prev_iv;

    private ProgressBar progressBar;

    private TextView load_tv;

    private int counter = 0;

    ArrayList<String> url_list = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (isConnected()) {
            Toast.makeText(MainActivity.this, "Internet is connected!!", Toast.LENGTH_SHORT).show();
        }

        go_btn = findViewById(R.id.go_btn);

        list_tv = findViewById(R.id.list_tv);
        load_tv = findViewById(R.id.load_tv);
        load_tv.setVisibility(TextView.INVISIBLE);

        display_iv = findViewById(R.id.pic_iv);
        next_iv = findViewById(R.id.next_iv);
        prev_iv = findViewById(R.id.prev_iv);

        progressBar = findViewById(R.id.progressbar);
        progressBar.setVisibility(ProgressBar.INVISIBLE);


        go_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new GetKeyWords().execute();
            }
        });

        if (url_list == null)
            next_iv.setAlpha(0.5f);

        if (url_list == null)
            prev_iv.setAlpha(0.5f);


        next_iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (url_list != null) {
                    new DownloadImageTask(display_iv).execute(url_list.get((++counter) % url_list.size()));
                }
            }
        });

        prev_iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (url_list != null) {
                    counter--;
                    if (counter <= 0) {
                        counter = url_list.size();
                    }
                    new DownloadImageTask(display_iv).execute(url_list.get((counter) % url_list.size()));
                }
            }
        });

    }


    private boolean isConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo == null || !networkInfo.isConnected() ||
                (networkInfo.getType() != ConnectivityManager.TYPE_WIFI
                        && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
            return false;
        }
        return true;
    }


    class GetKeyWords extends AsyncTask<Void, Void, String[]> {

        @Override
        protected String[] doInBackground(Void... voids) {

            StringBuilder stringBuilder = new StringBuilder();
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            String result = null;
            String[] keywords = new String[0];
            try {
                URL url = new URL("http://dev.theappsdr.com/apis/photos/keywords.php");
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line = "";
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line);
                    }
                    result = stringBuilder.toString();
                    keywords = result.split(";");
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return keywords;
        }

        @Override
        protected void onPostExecute(final String[] strings) {
            super.onPostExecute(strings);
            load_tv.setVisibility(TextView.INVISIBLE);
            progressBar.setVisibility(ProgressBar.INVISIBLE);
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Search")
                    .setItems(strings, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d("demo", "Clicked on " + strings[which]);
                            String keyword = strings[which];
                            list_tv.setText(strings[which]);
                            new GetImageURLs().execute(strings[which]);
                        }
                    });
            builder.create().show();
        }
    }

    class GetImageURLs extends AsyncTask<String, Void, ArrayList<String>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            counter = 0;
            display_iv.setImageDrawable(null);
            load_tv.setVisibility(TextView.VISIBLE);
            progressBar.setVisibility(ProgressBar.VISIBLE);
        }

        @Override
        protected ArrayList<String> doInBackground(String... strings) {
            String start_url = null;
            String result = null;
            ArrayList<String> results = new ArrayList<>();
            try {
                start_url = "http://dev.theappsdr.com/apis/photos/index.php?keyword=" + URLEncoder.encode(strings[0], "UTF-8");
                URL url = new URL(start_url);

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    result = IOUtils.toString(connection.getInputStream(), "UTF-8");

                    String[] sub_str = result.split("\n");

                    for (String str: sub_str) {
                        Log.d("demo", "Image URLs:\n" + str);
                        results.add(str);
                    }
                }

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return results;
        }

        @Override
        protected void onPostExecute(ArrayList<String> strings) {
            super.onPostExecute(strings);
            Log.d("demo", "Post Image URLs: " + strings.size() + strings);

            url_list = strings;
            load_tv.setVisibility(TextView.INVISIBLE);
            progressBar.setVisibility(ProgressBar.INVISIBLE);
            if (url_list != null && !url_list.get(0).isEmpty()) {
                new DownloadImageTask(display_iv).execute(url_list.get((counter) % url_list.size()));
                if (url_list.size() > 1) {
                    next_iv.setAlpha(1f);
                    next_iv.setClickable(true);

                    prev_iv.setAlpha(1f);
                    prev_iv.setClickable(true);

                }
                else {
                    next_iv.setAlpha(0.5f);
                    next_iv.setClickable(false);
                    prev_iv.setAlpha(0.5f);
                    prev_iv.setClickable(false);
                }
            }
            else {
                next_iv.setAlpha(0.5f);
                next_iv.setClickable(false);
                prev_iv.setAlpha(0.5f);
                prev_iv.setClickable(false);
            }
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;
        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            load_tv.setVisibility(TextView.VISIBLE);
            progressBar.setVisibility(ProgressBar.VISIBLE);
            bmImage.setImageDrawable(null);
        }

        protected Bitmap doInBackground(String... urls) {
            String url_image = urls[0];
            Bitmap bmp = null;
            try {
                InputStream in = new java.net.URL(url_image).openStream();
                bmp = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return bmp;
        }

        protected void onPostExecute(Bitmap result) {
            load_tv.setVisibility(TextView.INVISIBLE);
            progressBar.setVisibility(ProgressBar.INVISIBLE);
            if (result != null)
                bmImage.setImageBitmap(result);
            else
                new DownloadImageTask(display_iv).execute(url_list.get((++counter) % url_list.size()));
        }
    }
}
