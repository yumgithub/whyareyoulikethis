package com.project.jb.YuM;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.project.jb.YuM.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    private static final String[] PERMISSIONS_STORAGE = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final String[] PERMISSIONS_INTERNET = {Manifest.permission.INTERNET};

    List<String> links;
    List<String> titles1;

    private DownloadManager downloadManager;
    private long Music_DownloadId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        links = new ArrayList<>();
        titles1 = new ArrayList<>();
        getSupportActionBar().hide();

        verifyPermissions();

        Button buttondownload = (Button) findViewById(R.id.convertbutton);

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(downloadReceiver, filter);


        buttondownload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText urltext = (EditText) findViewById(R.id.urltext);
                String stringurl = urltext.getText().toString();
                links.clear();
                titles1.clear();
                makeJsonObjectRequest(Utility.getUrl(stringurl));
            }
        });
    }

    private void verifyPermissions() {

        int permissionExt = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionInternet = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.INTERNET);

        if (permissionExt != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_STORAGE, 1);
        }
        if (permissionExt != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_INTERNET, 1);
        }
    }


    private void makeJsonObjectRequest(String urlJsonObj) {

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                urlJsonObj, null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                if (response.has("error")) {
                    try {
                        Toast.makeText(MainActivity.this, response.getString("error"), Toast.LENGTH_LONG).show();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else

                {
                    try {
                        JSONObject info = response.getJSONObject("info");
                        String titles = info.getString("title");

                        JSONArray formats = info.getJSONArray("formats");
                        for (int i = 0; i < formats.length(); i++) {
                            JSONObject tempObject = formats.getJSONObject(i);
                            String extension = tempObject.getString("ext");
                            if (extension.equals("m4a")) {
                                Log.e("ARRAY", tempObject.toString());

                                links.add(tempObject.getString("url"));
                                titles1.add(titles);
                            }
                        }
                        Log.e("URL For Audio", links.get(0)); {

                            Uri music_uri = Uri.parse(links.get(0));
                            Music_DownloadId = DownloadData(music_uri);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(),
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d("TAG", "Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        "Invalid URL",  Toast.LENGTH_SHORT).show();
            }
        });

        AppController.getInstance().addToRequestQueue(jsonObjReq);
    }


    private long DownloadData(Uri uri) {

        final long downloadReference;
        final AtomicBoolean downloading = new AtomicBoolean(false);
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.probar);
        final TextView viewname = (TextView) findViewById(R.id.viewname);
        final Button cancel = (Button) findViewById(R.id.buttoncancel);
        cancel.setEnabled(false);

        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        final DownloadManager.Request request = new DownloadManager.Request(uri);

        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        request.setTitle(titles1.get(0) + "downloading.mp3");
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, titles1.get(0) + "finished.mp3");

        downloadReference = downloadManager.enqueue(request);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    downloading.set(false);
                    downloadManager.remove(downloadReference);
                    cancel.setEnabled(false);
                    viewname.setText(null);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        });

        viewname.setText(titles1.get(0));

        new Thread(new Runnable() {
            @Override
            public void run() {
                downloading.set(true);
                cancel.setEnabled(true);
                while (downloading.get()) {

                    DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(downloadReference);
                    Cursor c = downloadManager.query(q);
                    c.moveToFirst();

                    int bytes_downloaded = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    int bytes_total = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                    if (c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                        downloading.set(false);
                    }

                    final int dl_progress = (int) ((bytes_downloaded * 100l) / bytes_total);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setProgress((int) dl_progress);
                        }
                    });
                }
                { progressBar.setProgress(0);
                }
            }
        }).start();

        return downloadReference;
    }


    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

            if (referenceId == Music_DownloadId) {

                    File file1 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), titles1.get(0) + "finished.mp3");
                    File file2 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), titles1.get(0) + ".mp3");

                    file1.renameTo(file2);

                    Toast toast = Toast.makeText(MainActivity.this,
                            "Music Download Complete", Toast.LENGTH_LONG);
                    toast.show();
                }
            }
    };



}