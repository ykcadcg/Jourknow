package com.ak.jourknow;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Calendar;

/**
 * Created by kyang on 6/28/2016. based on http://www.mysamplecode.com/2012/07/android-listview-cursoradapter-sqlite.html
 */
public class DbAdapter {

    public static final String KEY_ROWID        = "_id";
    public static final String KEY_CALENDARMS   = "calendarMs";
    public static final String KEY_CALENDARSTR  = "calendarStr";
    public static final String KEY_DATE         = "date";
    public static final String KEY_LENGTH       = "length";
    public static final String KEY_LENGTHMIN    = "lengthMin";
    public static final String KEY_LENGTHSEC    = "lengthSec";
    public static final String KEY_SCRIPT       = "script";

    private static final String TAG = "DbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    private static final String DATABASE_NAME = "Jourknow";
    private static final String SQLITE_TABLE = "Notes";
    private static final int DATABASE_VERSION = 1;

    private final Context mCtx;

    private static final String DATABASE_CREATE =
            "CREATE TABLE if not exists " + SQLITE_TABLE + " (" +
                    KEY_ROWID + " integer PRIMARY KEY autoincrement," +
                    KEY_CALENDARMS + "," +
                    KEY_CALENDARSTR + "," +
                    KEY_DATE + "," +
                    KEY_LENGTH + "," +
                    KEY_LENGTHMIN + "," +
                    KEY_LENGTHSEC + "," +
                    KEY_SCRIPT + "," +
                    " UNIQUE (" + KEY_CALENDARMS +"));";

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            ;//Log.w(TAG, DATABASE_CREATE);
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            ;//Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + SQLITE_TABLE);
            onCreate(db);
        }
    }

    public DbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    public DbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        if (mDbHelper != null) {
            mDbHelper.close();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        this.close();
        super.finalize();
    }

    public long addRecord(AddActivity.Result a) {

        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_CALENDARMS   , a.rightNow.getTimeInMillis());
        initialValues.put(KEY_CALENDARSTR  , a.rightNow.toString());
        initialValues.put(KEY_DATE         , Integer.toString((a.rightNow.get(Calendar.MONTH) + 1)) + "/" + a.rightNow.get(Calendar.DAY_OF_MONTH));
        initialValues.put(KEY_LENGTH       , a.lengthMin + ":" + a.lengthSec);
        initialValues.put(KEY_LENGTHMIN    , a.lengthMin);
        initialValues.put(KEY_LENGTHSEC    , a.lengthSec);
        initialValues.put(KEY_SCRIPT       , a.script);

        return mDb.insert(SQLITE_TABLE, null, initialValues);
    }

    public int delete(long _id) {
        return mDb.delete(SQLITE_TABLE, KEY_ROWID + "=" + _id, null);
    }
/*
    public boolean deleteAllRecords() {

        int doneDelete = 0;
        doneDelete = mDb.delete(SQLITE_TABLE, null , null);
        ;//Log.w(TAG, Integer.toString(doneDelete));
        return doneDelete > 0;

    }
    */


    public Cursor fetchScript(long _id) throws SQLException {
        Cursor cursor = mDb.query(true, SQLITE_TABLE, new String[] {KEY_SCRIPT},
                    KEY_ROWID + " = " + _id, null,
                    null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
   }

    public Cursor fetchRecordsList() {
        Cursor cursor = mDb.query(SQLITE_TABLE, new String[] {KEY_ROWID,
                        KEY_SCRIPT, KEY_LENGTH, KEY_DATE},
                null, null, null, null,  KEY_ROWID + " DESC", null);

        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor fetchNoArg(String[] columns) {
        Cursor cursor = mDb.query(SQLITE_TABLE, columns, null, null, null, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

}
