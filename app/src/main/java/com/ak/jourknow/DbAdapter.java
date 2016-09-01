package com.ak.jourknow;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.Calendar;

/**
 * Created by kyang on 6/28/2016. based on http://www.mysamplecode.com/2012/07/android-listview-cursoradapter-sqlite.html
 */
public class DbAdapter {
    //Notes table
    public static final String KEY_ROWID        = "_id";
    public static final String KEY_CALENDARMS   = "calendarMs";
    public static final String KEY_DATE         = "date";
    public static final String KEY_WORDCNT      = "wordCnt";
    public static final String KEY_TEXT         = "text";
    public static final String KEY_ANALYSISRAW  = "analysisRaw";
    public static final String KEY_JOY          = "joy";
    public static final String KEY_ANGER        = "anger";
    public static final String KEY_SADNESS      = "sadness";
    public static final String KEY_DISGUST      = "disgust";
    public static final String KEY_FEAR         = "fear";
    public static final String KEY_TOPEMOIDX    = "topEmotionIdx";
    public static final String KEY_TOPSCORE     = "topScore";
    public static final String KEY_ANALYZED     = "analyzed";

    //Sentences table
    public static final String KEY_NOTEID   = "noteId";
    public static final String KEY_SENTENCEID   = "sentenceId";
    public static final String KEY_INPUTFROM   = "inputFrom";
    public static final String KEY_INPUTTO   = "inputTo";


    private static final String TAG = "DbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    private static final String DATABASE_NAME = "Jourknow";
    private static final String TABLE_NOTES = "Notes";
    private static final String TABLE_SENTENCES = "Sentences";
    private static final int DATABASE_VERSION = 1;

    private final Context mCtx;

    private static final String CREATE_TABLE_NOTES =
            "CREATE TABLE if not exists " + TABLE_NOTES + " (" +
                    KEY_ROWID + " integer PRIMARY KEY autoincrement," +
                    KEY_CALENDARMS + "," +
                    KEY_DATE + "," +
                    KEY_WORDCNT + "," +
                    KEY_TEXT + "," +
                    KEY_ANALYZED + "," +
                    KEY_ANALYSISRAW + "," +
                    KEY_JOY + "," +
                    KEY_ANGER + "," +
                    KEY_SADNESS + "," +
                    KEY_DISGUST + "," +
                    KEY_FEAR + "," +
                    KEY_TOPEMOIDX + "," +
                    KEY_TOPSCORE + "," +
    " UNIQUE (" + KEY_CALENDARMS +"));";

    //multiple tables: learned from http://www.androidhive.info/2013/09/android-sqlite-database-with-multiple-tables/
    private static final String CREATE_TABLE_SENTENCES =
            "CREATE TABLE if not exists " + TABLE_SENTENCES + " (" +
                    KEY_ROWID + " integer PRIMARY KEY autoincrement," +
                    KEY_NOTEID + "," +
                    KEY_SENTENCEID + "," +
                    KEY_TOPEMOIDX + "," +
                    KEY_TOPSCORE + "," +
                    KEY_TEXT + "," +
                    KEY_INPUTFROM + "," +
                    KEY_INPUTTO + "," +
                    "FOREIGN KEY(" + KEY_NOTEID + ") REFERENCES " + TABLE_NOTES + " (" + KEY_ROWID + "));"
            ;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            ;//Log.w(TAG, DATABASE_CREATE);
            db.execSQL(CREATE_TABLE_NOTES);
            db.execSQL(CREATE_TABLE_SENTENCES);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            ;//Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SENTENCES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
            onCreate(db);
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            super.onOpen(db);
            if (!db.isReadOnly()) {
                // Enable foreign key constraints
                db.execSQL("PRAGMA foreign_keys=ON;");
            }
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

    public long insertNote(NoteActivity.NoteData a) {
        ContentValues args = new ContentValues();
        args.put(KEY_CALENDARMS   , a.time.getTimeInMillis());
        args.put(KEY_DATE         , Integer.toString((a.time.get(Calendar.MONTH) + 1)) + "/" + a.time.get(Calendar.DAY_OF_MONTH));
        args.put(KEY_WORDCNT      , a.wordCnt);
        args.put(KEY_TEXT         , a.text);
        args.put(KEY_ANALYZED     , a.analyzed);
        if(a.analyzed) {
            args.put(KEY_ANALYSISRAW, a.analysisRaw);
            args.put(KEY_JOY, a.jasdf[0]);
            args.put(KEY_ANGER, a.jasdf[1]);
            args.put(KEY_SADNESS, a.jasdf[2]);
            args.put(KEY_DISGUST, a.jasdf[3]);
            args.put(KEY_FEAR, a.jasdf[4]);
            args.put(KEY_TOPEMOIDX, a.topEmotionIdx);
            args.put(KEY_TOPSCORE, a.topScore);

        }
        long noteId = mDb.insert(TABLE_NOTES, null, args);

        if(a.analyzed) {
            for (NoteActivity.sentenceEmotion s : a.sentences) {
                insertSentence(noteId, s);
            }
        }
        return noteId;
    }

    private long insertSentence(long noteId, NoteActivity.sentenceEmotion a) {
        ContentValues args = new ContentValues();
        args.put(KEY_NOTEID         , noteId);
        args.put(KEY_SENTENCEID     , a.sentenceId);
        args.put(KEY_TOPEMOIDX      , a.topEmotionIdx);
        args.put(KEY_TOPSCORE       , a.topScore);
        args.put(KEY_TEXT           , a.text);
        args.put(KEY_INPUTFROM      , a.input_from);
        args.put(KEY_INPUTTO        , a.input_to);

        long sentenceId = mDb.insert(TABLE_SENTENCES, null, args);
        return sentenceId;
    }

    private long deleteSentencesByNoteId(long noteId) {
        return mDb.delete(TABLE_SENTENCES, KEY_NOTEID + "=" + noteId, null);
    }

    public int deleteNote(long _id) {
        mDb.delete(TABLE_SENTENCES, KEY_NOTEID + "=" + _id, null); //delete all sentences first
        return mDb.delete(TABLE_NOTES, KEY_ROWID + "=" + _id, null);
    }

    public boolean updateNote(long _id, NoteActivity.NoteData a) {
        ContentValues args = new ContentValues();
        args.put(KEY_CALENDARMS   , a.time.getTimeInMillis());
        args.put(KEY_DATE         , Integer.toString((a.time.get(Calendar.MONTH) + 1)) + "/" + a.time.get(Calendar.DAY_OF_MONTH));
        args.put(KEY_WORDCNT       , a.wordCnt);
        args.put(KEY_TEXT       , a.text);
        args.put(KEY_ANALYZED     , a.analyzed);
        if(a.analyzed) {
            args.put(KEY_ANALYSISRAW, a.analysisRaw);
            args.put(KEY_JOY, a.jasdf[0]);
            args.put(KEY_ANGER, a.jasdf[1]);
            args.put(KEY_SADNESS, a.jasdf[2]);
            args.put(KEY_DISGUST, a.jasdf[3]);
            args.put(KEY_FEAR, a.jasdf[4]);
            args.put(KEY_TOPEMOIDX, a.topEmotionIdx);
            args.put(KEY_TOPSCORE, a.topScore);

            //update sentences
            deleteSentencesByNoteId(_id);
            for (NoteActivity.sentenceEmotion s : a.sentences) {
                insertSentence(_id, s);
            }
        }
        return mDb.update(TABLE_NOTES, args, KEY_ROWID + "=" + _id, null) > 0;
    }

/*
    public boolean deleteAllNotes() {

        int doneDelete = 0;
        doneDelete = mDb.delete(TABLE_NOTES, null , null);
        ;//Log.w(TAG, Integer.toString(doneDelete));
        return doneDelete > 0;

    }
    */


    public Cursor queryNoteById(long _id) throws SQLException {
        Cursor cursor = mDb.query(true, TABLE_NOTES, null,
                    KEY_ROWID + " = " + _id, null,
                    null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
   }

    public Cursor querySentencesByNoteId(long _id) throws SQLException {
        Cursor cursor = mDb.query(true, TABLE_SENTENCES, null,
                KEY_NOTEID + " = " + _id, null,
                null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor querySentencesByScore(float scoreThreshold) throws SQLException {
        Cursor cursor = mDb.query(TABLE_SENTENCES, null,
                KEY_TOPSCORE + " >= " + scoreThreshold,
                null, null, null, KEY_TOPSCORE + " DESC ", null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor fetchNotesList() {
        Cursor cursor = mDb.query(TABLE_NOTES, new String[] {KEY_ROWID,
                        KEY_TEXT, KEY_DATE, KEY_TOPEMOIDX, KEY_TOPSCORE, KEY_ANALYZED},
                null, null, null, null,  KEY_ROWID + " DESC", null);

        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor fetchNotesByColumns(String[] columns) {
        Cursor cursor = mDb.query(TABLE_NOTES, columns, null, null, null, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

}
