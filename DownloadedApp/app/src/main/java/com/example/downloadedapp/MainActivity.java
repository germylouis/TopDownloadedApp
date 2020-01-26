package com.example.downloadedapp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private String feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml";
    private int feedLimit = 10;
    private String feedCacheUrl = "INVALIDATED";
    public static final String STATE_URL = "feedUrl";
    public static final String STATE_LIMIT = "feedlimit";
    private ListView listApps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        listApps = (ListView) findViewById( R.id.xmlListView );

        if (savedInstanceState != null){
            feedUrl = savedInstanceState.getString( STATE_URL );
            feedLimit = savedInstanceState.getInt( STATE_LIMIT );
        }
        downloadUrl( String.format( feedUrl, feedLimit ) );

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate( R.menu.feeds_menu, menu );
        if (feedLimit == 10) {
            menu.findItem( R.id.mnu10 ).setChecked( true );
        } else {
            menu.findItem( R.id.mnu25 ).setChecked( true );
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.mnuFree:
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml";
                break;
            case R.id.mnuPaid:
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/toppaidapplications/limit=%d/xml";
                break;
            case R.id.mnuSongs:
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/limit=%d/xml";
                break;
            case R.id.mnu10:
            case R.id.mnu25:
                if (!item.isChecked()) {
                    item.setChecked( true );
                    feedLimit = 35 - feedLimit;
                    Log.d( TAG, "onOptionsItemSelected: " + item.getTitle() + " setting feedLimit to " + feedLimit );
                } else {
                    Log.d( TAG, "onOptionsItemSelected: " + item.getTitle() + " feedLimit changed" );
                }
                break;
            case R.id.mnuRefresh:
                feedCacheUrl = "INVALIDATED";
                break;
            default:

                break;
        }
        downloadUrl( String.format( feedUrl, feedLimit ) );
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString( STATE_URL, feedUrl );
        outState.putInt( STATE_LIMIT,  feedLimit);
        super.onSaveInstanceState( outState );
    }

    private void downloadUrl(String feedUrl) {
        if (!feedUrl.equals( feedCacheUrl )) {
            Log.d( TAG, "onCreate: Starting Async Tasks" );
            DownloadData downloadData = new DownloadData();
            downloadData.execute( feedUrl );
            feedCacheUrl = feedUrl;
            Log.d( TAG, "onCreate: Done" );
        } else {
            Log.d( TAG, "downloadUrl: URL not changed" );
        }
    }

    private class DownloadData extends AsyncTask<String, Void, String> {
        private static final String TAG = "DownloadData";

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute( s );
            // Log.d( TAG, "onPostExecute: parameter is " + s );
            ParseApplications parseApplications = new ParseApplications();
            parseApplications.parse( s );

//            ArrayAdapter<FeedEntry> arrayAdapter = new ArrayAdapter<FeedEntry>(
//                    MainActivity.this, R.layout.list_item, parseApplications.getAppplications() );
//            listApps.setAdapter( arrayAdapter );
            FeedAdapter feedAdapter = new FeedAdapter( MainActivity.this, R.layout.list_record,
                    parseApplications.getAppplications() );
            listApps.setAdapter( feedAdapter );
        }

        @Override
        protected String doInBackground(String... strings) {
            Log.d( TAG, "doInBackground: " + strings[0] );
            String rssFeed = downloadXML( strings[0] );
            if (rssFeed == null) {
                Log.e( TAG, "doInBackground: Error downloading" );
            }
            return rssFeed;
        }

        private String downloadXML(String urlPath) {
            StringBuilder xmlResult = new StringBuilder();

            try {

                URL url = new URL( urlPath );
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                int response = connection.getResponseCode();
                Log.e( TAG, "downloadXML: the response code was : " + response );
                BufferedReader reader = new BufferedReader( new InputStreamReader( connection.getInputStream() ) );

                int charsRead;
                char[] inputBuffer = new char[500];
                while (true) {
                    charsRead = reader.read( inputBuffer );
                    if (charsRead < 0) {
                        break;
                    }
                    if (charsRead > 0) {
                        xmlResult.append( String.valueOf( inputBuffer, 0, charsRead ) );
                    }
                }
                reader.close();
                return xmlResult.toString();
            } catch (MalformedURLException e) {

                Log.e( TAG, "downloadXML: " + e.getMessage() );

            } catch (IOException e) {

                Log.e( TAG, "downloadXML: IO Exception reading data " + e.getMessage() );

            } catch (SecurityException e) {
                Log.e( TAG, "downloadXML: Security exception needs permission? " + e.getMessage() );
            }
            return null;
        }
    }
}
