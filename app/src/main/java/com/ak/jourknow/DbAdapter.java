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
    public static final String KEY_WORDCNT      = "wordCnt";
    public static final String KEY_TEXT         = "text";
    public static final String KEY_ANALYSISRAW  = "analysisRaw";
    public static final String KEY_JOY          = "joy";
    public static final String KEY_ANGER        = "anger";
    public static final String KEY_SADNESS      = "sadness";
    public static final String KEY_DISGUST      = "disgust";
    public static final String KEY_FEAR         = "fear";
    public static final String KEY_TOPEMOTION   = "topEmotion";


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
                    KEY_WORDCNT + "," +
                    KEY_TEXT + "," +
                    KEY_ANALYSISRAW + "," +
                    KEY_JOY + "," +
                    KEY_ANGER + "," +
                    KEY_SADNESS + "," +
                    KEY_DISGUST + "," +
                    KEY_FEAR + "," +
                    KEY_TOPEMOTION + "," +
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

    public long insert(NoteActivity.NoteData a) {

        ContentValues args = new ContentValues();
        args.put(KEY_CALENDARMS   , a.time.getTimeInMillis());
        args.put(KEY_CALENDARSTR  , a.time.toString());
        args.put(KEY_DATE         , Integer.toString((a.time.get(Calendar.MONTH) + 1)) + "/" + a.time.get(Calendar.DAY_OF_MONTH));
        args.put(KEY_WORDCNT      , a.wordCnt);
        args.put(KEY_TEXT         , a.text);
        args.put(KEY_ANALYSISRAW  , a.analysisRaw);
        args.put(KEY_JOY          , a.jasdf[0]);
        args.put(KEY_ANGER        , a.jasdf[1]);
        args.put(KEY_SADNESS      , a.jasdf[2]);
        args.put(KEY_DISGUST      , a.jasdf[3]);
        args.put(KEY_FEAR         , a.jasdf[4]);
        args.put(KEY_TOPEMOTION   , a.topEmotion);
        return mDb.insert(SQLITE_TABLE, null, args);
    }

    public int delete(long _id) {
        return mDb.delete(SQLITE_TABLE, KEY_ROWID + "=" + _id, null);
    }

    public boolean update(long _id, NoteActivity.NoteData a) {
        ContentValues args = new ContentValues();
        args.put(KEY_CALENDARMS   , a.time.getTimeInMillis());
        args.put(KEY_CALENDARSTR  , a.time.toString());
        args.put(KEY_DATE         , Integer.toString((a.time.get(Calendar.MONTH) + 1)) + "/" + a.time.get(Calendar.DAY_OF_MONTH));
        args.put(KEY_WORDCNT       , a.wordCnt);
        args.put(KEY_TEXT       , a.text);
        args.put(KEY_ANALYSISRAW  , a.analysisRaw);
        args.put(KEY_JOY          , a.jasdf[0]);
        args.put(KEY_ANGER        , a.jasdf[1]);
        args.put(KEY_SADNESS      , a.jasdf[2]);
        args.put(KEY_DISGUST      , a.jasdf[3]);
        args.put(KEY_FEAR         , a.jasdf[4]);
        args.put(KEY_TOPEMOTION   , a.topEmotion);

        return mDb.update(SQLITE_TABLE, args, KEY_ROWID + "=" + _id, null) > 0;
    }

/*
    public boolean deleteAllRecords() {

        int doneDelete = 0;
        doneDelete = mDb.delete(SQLITE_TABLE, null , null);
        ;//Log.w(TAG, Integer.toString(doneDelete));
        return doneDelete > 0;

    }
    */


    public Cursor queryById(long _id) throws SQLException {
        Cursor cursor = mDb.query(true, SQLITE_TABLE, null,
                    KEY_ROWID + " = " + _id, null,
                    null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
   }

    public Cursor fetchRecordsList() {
        Cursor cursor = mDb.query(SQLITE_TABLE, new String[] {KEY_ROWID,
                        KEY_TEXT, KEY_WORDCNT, KEY_DATE, KEY_TOPEMOTION},
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
