package com.project.jb.newyum;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {


    List<String> links;
    List<String> titles1;

    private DownloadManager downloadManager;

    android.widget.SearchView searchView;
    private long Music_DownloadId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        links = new ArrayList<>();
        titles1 = new ArrayList<>();

        final Button convert = (Button) findViewById(R.id.convertbutton);


        //set filter to only when download is complete and register broadcast receiver
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(downloadReceiver, filter);



        convert.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText urltext = (EditText) findViewById(R.id.urltext);
                String blabla = urltext.getText().toString();
                links.clear();
                titles1.clear();
                makeJsonObjectRequest(Utility.getUrl(blabla));

            }
        });
    }

    private long DownloadData(Uri uri) {

        long downloadReference;

        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(uri);

        //Setting title of request
        request.setTitle(titles1.get(0));

        //Setting description of request
        request.setDescription("Android Data download using DownloadManager.");

        //Set the local destination for the downloaded file to a path within the application's external files directory
        request.setDestinationInExternalPublicDir((Environment.DIRECTORY_MUSIC), titles1.get(0)+".mp3");


        //Enqueue download and save the referenceId
        downloadReference = downloadManager.enqueue(request);


        return downloadReference;
    }

    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) { 

            //check if the broadcast message is for our Enqueued download
            long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

            if (referenceId == Music_DownloadId) {

                Toast toast = Toast.makeText(MainActivity.this,
                        "Music Download Complete", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP, 25, 400);
                toast.show();
            }

        }
    };

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

                    //Log.e("TAG", response.toString());
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
                            // TextView testtext = (TextView) findViewById(R.id.testtext);
                            //testtext.setText("CONVERTED NICE");

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
                        error.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });
        AppController.getInstance().addToRequestQueue(jsonObjReq);
    }
}