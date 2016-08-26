package com.ak.jourknow;

import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.w3c.dom.Text;

public class ScriptActivity extends AppCompatActivity {

    private DbAdapter dbHelper;
    private int rowId;
    private String mScript;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script);

        Intent intent = getIntent();
        rowId = intent.getIntExtra(MainActivity.EXTRA_ID, -1);
        dbHelper = new DbAdapter(this);
        dbHelper.open();
        Cursor cursor = dbHelper.fetchScript(rowId);

        if(cursor != null) {
            TextView scriptView = (TextView) findViewById(R.id.textViewScript);
            mScript = cursor.getString(0);
            scriptView.setText(mScript);
            cursor.close();
        }
    }

}
