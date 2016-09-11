package com.ak.jourknow;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVOSCloud;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.SaveCallback;
import com.avos.avoscloud.feedback.FeedbackAgent;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DbAdapter dbHelper;
    private BkgdCursorAdapter customAdapter;
    private ListView listView;
    public final static String EXTRA_ID = "com.ak.jourknow.id";
    public final static String packageName = "com.ak.jourknow";
    public String mAndroidId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        AVOSCloud.initialize(this, "w1fcAvOcr0HliArshK92BCgP-9Nh9j0Va", "gQHn93R585cPzOXnaY6rzIY6");
        //yk: not working yet: AVOSCloud.useAVCloudUS();
//        // 测试 SDK 是否正常工作的代码
//        AVObject testObject = new AVObject("TestObject");
//        testObject.put("words","Hello US2 World!");
//        testObject.saveInBackground(new SaveCallback() {
//            @Override
//            public void done(AVException e) {
//                if(e == null){
//                    Log.d("saved","success!");
//                }
//            }
//        });
        //mFeedbackAgent.sync();
    }

    public void newSession(View view) {
        Intent intent = new Intent(this, NoteActivity.class);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        else if (id == R.id.action_share) {
            Intent i1 = new Intent(android.content.Intent.ACTION_SEND);
            i1.setType("text/plain");
            i1.putExtra(android.content.Intent.EXTRA_SUBJECT, getResources().getString(R.string.recommendation_subject));
            i1.putExtra(android.content.Intent.EXTRA_TEXT, getResources().getString(R.string.recommendation_body));
            startActivity(Intent.createChooser(i1, getResources().getText(R.string.shareWith)));
        }
        else if (id == R.id.action_feedback) {
            //below leancloud feedback form gives error
//            FeedbackAgent agent = new FeedbackAgent(getApplicationContext());
//            agent.startDefaultThreadActivity();
            Intent Email = new Intent(Intent.ACTION_SEND);
            Email.setType("message/rfc822");
            Email.putExtra(Intent.EXTRA_EMAIL, new String[] { "jourknow@gmail.com" });
            Email.putExtra(Intent.EXTRA_SUBJECT, "Feedback");
            Email.putExtra(Intent.EXTRA_TEXT, "Dear Jourknow team, " + "");
            try {
                startActivity(Intent.createChooser(Email, "Send Feedback:"));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
            }
        }
        else if (id == R.id.action_follow) {
            final Uri uri = Uri.parse("http://twitter.com/myJourknow");
            final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            if (getPackageManager().queryIntentActivities(intent, 0).size() > 0)
            {
                startActivity(intent);
            }
            else
            {
                Toast.makeText(this, "The device has no way to handle the url.", Toast.LENGTH_SHORT).show();
            }
        }
        else if (id == R.id.action_report) {
            Intent intent = new Intent(this, ReportActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_quotes) {
            Intent intent = new Intent(this, QuotesActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
/*
        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }
*/
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onResume() {
        dbHelper = new DbAdapter(this);
        dbHelper.open();
//        if (prefs.getBoolean("firstrun", true)) {
//            //loadSampleNotes();
//            prefs.edit().putBoolean("firstrun", false).commit();
//        }
        displayListView();

        super.onResume();
    }


    private int displayListView() {
        dbHelper.open();

        Cursor cursor = dbHelper.fetchNotesList();
        int rowCnt = 0;
        if(cursor != null)
            rowCnt = cursor.getCount();

        customAdapter = new BkgdCursorAdapter(this, cursor, 0);

        listView = (ListView) findViewById(R.id.listView);
        // Assign adapter to ListView
        listView.setAdapter(customAdapter);

        // cursor.close(); //can't close, o/w error android.database.StaleDataException: Attempting to access a closed CursorWindow.Most probable cause: cursor is deactivated prior to calling this method. Can close in onPause in future.

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> listView, View view,
                                    int position, long id) {
                // Get the cursor, positioned to the corresponding row in the result set
                Cursor cursor = (Cursor) listView.getItemAtPosition(position);

                Intent intent = new Intent(MainActivity.this, NoteActivity.class);
                int rowId = cursor.getInt(cursor.getColumnIndex("_id"));
                intent.putExtra(EXTRA_ID, rowId);
                startActivity(intent);
            }
        });

        return rowCnt;

    }

    //to be able to change color for each listview item: learned from https://coderwall.com/p/fmavhg/android-cursoradapter-with-custom-layout-and-how-to-use-it
    public class BkgdCursorAdapter extends CursorAdapter {
        private LayoutInflater cursorInflater;
        public BkgdCursorAdapter(Context context, Cursor cursor, int flags) {
            super(context, cursor, flags);
            cursorInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        SimpleDateFormat dateFormatter = new SimpleDateFormat("MM/dd");

        public void bindView(View view, Context context, Cursor c) {
            TextView tvDate = (TextView)view.findViewById(R.id.textViewDate);
            long calendarMs = c.getLong(c.getColumnIndexOrThrow(DbAdapter.KEY_CALENDARMS));
            tvDate.setText(dateFormatter.format(new Date(calendarMs)));
            TextView tvSnippet = (TextView)view.findViewById(R.id.textViewSnippet);
            tvSnippet.setText(c.getString(c.getColumnIndexOrThrow(DbAdapter.KEY_TEXT)));
            boolean analyzed = c.getInt(c.getColumnIndexOrThrow(DbAdapter.KEY_ANALYZED)) > 0;
            float score = c.getFloat(c.getColumnIndexOrThrow(DbAdapter.KEY_TOPSCORE));
            tvSnippet.setBackgroundColor(Color.WHITE);
            if(analyzed && (score >= NoteActivity.noteEmotionThreshold)){
                int colorIdx = c.getInt(c.getColumnIndexOrThrow(DbAdapter.KEY_TOPEMOIDX));
                if(colorIdx >= 0) {
                    tvSnippet.setBackgroundColor(Color.parseColor(getResources().getStringArray(R.array.jasdfColors)[colorIdx]));
                }
            }
        }

        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return cursorInflater.inflate(R.layout.listview_item, parent, false);
        }
    }

}
