package com.ak.jourknow;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class QuotesActivity extends AppCompatActivity {
    private DbAdapter dbHelper;
    private ListView listView;
    private EditText mEdit;
    private FilterCursorAdapter cursorAdapters[];
    private int mEmotionSelected = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quotes);

        cursorAdapters = new FilterCursorAdapter[5];
        dbHelper = new DbAdapter(this);
        dbHelper.open();
        displayListView();
    }

    @Override
    public void onPause() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("actionPlan", String.valueOf(mEdit.getText().toString()));
        editor.commit();
        super.onPause();
    }

    private boolean displayListView() {
        if(cursorAdapters == null || dbHelper == null)
            return false;

        dbHelper.open();

        for(int emo = 0; emo < 5; ++emo) {
            Cursor c = dbHelper.querySentencesByScore(NoteActivity.sentenceEmotionThreshold, emo);
            cursorAdapters[emo] = new FilterCursorAdapter(this, c, 0);
        }

        //default: Joy
        listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(cursorAdapters[0]);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> listView, View view,
                                    int position, long id) {
                // Get the cursor, positioned to the corresponding row in the result set
                Cursor cursor = (Cursor) listView.getItemAtPosition(position);

                Intent intent = new Intent(QuotesActivity.this, NoteActivity.class);
                int rowId = cursor.getInt(cursor.getColumnIndex(DbAdapter.KEY_NOTEID));
                intent.putExtra(MainActivity.EXTRA_ID, rowId);
                startActivity(intent);
            }
        });

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String actionPlan = sharedPref.getString("actionPlan", null);
        mEdit = (EditText)findViewById(R.id.actionPlan);
        mEdit.setText(actionPlan);
        return true;
    }

    //borrowed from MainActivity, also based on http://stackoverflow.com/questions/4973175/hide-an-element-of-listviews-item-depending-on-cursors-column-value
    public class FilterCursorAdapter extends CursorAdapter {
        private LayoutInflater cursorInflater;
        public FilterCursorAdapter(Context context, Cursor cursor, int flags) {
            super(context, cursor, flags);
            cursorInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        private int interpolateColorsCompact (int a, int b, float lerp)
        {
            int MASK1 = 0xff00ff, MASK2 = 0x00ff00;
            int f2 = (int)(256 * lerp);
            int f1 = 256 - f2;
            return   ((((( a & MASK1 ) * f1 ) + ( ( b & MASK1 ) * f2 )) >> 8 ) & MASK1 )
                    | ((((( a & MASK2 ) * f1 ) + ( ( b & MASK2 ) * f2 )) >> 8 ) & MASK2 );
        }

        public void bindView(View view, Context context, Cursor c) {
            float score = c.getFloat(c.getColumnIndexOrThrow(DbAdapter.KEY_TOPSCORE));
            TextView tvScore = (TextView)view.findViewById(R.id.textViewScore);
            tvScore.setText(String.format("%.1f", score));

            TextView textViewArrow = (TextView)view.findViewById(R.id.textViewArrow);
            RelativeLayout parentLayout = (RelativeLayout) view.findViewById(R.id.layout);

            int emotionIdx = c.getInt(c.getColumnIndexOrThrow(DbAdapter.KEY_TOPEMOIDX));
            TextView tvQuote = (TextView)view.findViewById(R.id.quote);
            tvQuote.setText("\"" + c.getString(c.getColumnIndexOrThrow(DbAdapter.KEY_TEXT)) + "\"");
            int strong = NoteActivity.jasdfColors[mEmotionSelected * 2];
            int moderate = NoteActivity.jasdfColors[mEmotionSelected * 2 + 1];
            int color = interpolateColorsCompact(moderate, strong, (score - NoteActivity.sentenceEmotionThreshold) / (1.f- NoteActivity.sentenceEmotionThreshold));
            parentLayout.setBackgroundColor(0xff000000 + color);
        }

        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return cursorInflater.inflate(R.layout.listview_item_quote, parent, false);
        }
    }

    public void onButtonClicked(View view) {
        switch(view.getId()) {
            case R.id.joy:
                mEmotionSelected = 0;
                break;
            case R.id.anger:
                mEmotionSelected = 1;
                break;
            case R.id.sadness:
                mEmotionSelected = 2;
                break;
            case R.id.disgust:
                mEmotionSelected = 3;
                break;
            case R.id.fear:
                mEmotionSelected = 4;
            break;
            default:
                break;
        }
        listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(cursorAdapters[mEmotionSelected]);

        //listView.setBackgroundColor(Color.parseColor(getResources().getStringArray(R.array.jasdfColors)[mEmotionSelected * 2]));
        listView.invalidateViews();
    }

}
