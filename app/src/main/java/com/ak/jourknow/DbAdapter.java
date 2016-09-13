package com.ak.jourknow;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.util.Log;

import com.avos.avoscloud.AVCloudQueryResult;
import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVQuery;
import com.avos.avoscloud.CloudQueryCallback;
import com.avos.avoscloud.SaveCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

/**
 * Created by kyang on 6/28/2016. based on http://www.mysamplecode.com/2012/07/android-listview-cursoradapter-sqlite.html
 */
public class DbAdapter {
    public final static boolean firstRunAttachDB = true;
    private String mAndroidId;

    //Notes table
    public static final String KEY_ROWID        = "_id";
    public static final String KEY_CALENDARMS   = "calendarMs";
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
    public static final String KEY_REFLECTION     = "reflection";
    public static final String KEY_1            = "reserved1"; //reserved
    public static final String KEY_2            = "reserved2";
    public static final String KEY_3            = "reserved3";
    public static final String KEY_4            = "reserved4";

    //Sentences table
    public static final String KEY_NOTEID   = "noteId";
    public static final String KEY_SENTENCEID   = "sentenceId";
    public static final String KEY_INPUTFROM   = "inputFrom";
    public static final String KEY_INPUTTO   = "inputTo";

    //Server backend
    public static final String KEY_ANDROIDID    = "androidId";
    public static final String KEY_NOTEOBJECTID    = "noteObjectId";


    private static final String TAG = "DbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    private static String DB_PATH = "/data/data/" + MainActivity.packageName + "/databases/";
    private static final String DATABASE_NAME = "Jourknow.db";
    private static final String TABLE_NOTES = "Notes";
    private static final String TABLE_SENTENCES = "Sentences";
    private static final int DATABASE_VERSION = 1;

    private final Context mCtx;

    private static final String CREATE_TABLE_NOTES =
            "CREATE TABLE if not exists " + TABLE_NOTES + " (" +
                    KEY_ROWID + " integer PRIMARY KEY autoincrement," +
                    KEY_CALENDARMS + "," +
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
                    KEY_REFLECTION + "," +
                    KEY_1 + "," +
                    KEY_2 + "," +
                    KEY_3 + "," +
                    KEY_4 + "," +
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
                    KEY_1 + "," +
                    KEY_2 + "," +
                    KEY_3 + "," +
                    KEY_4 + "," +
                    "FOREIGN KEY(" + KEY_NOTEID + ") REFERENCES " + TABLE_NOTES + " (" + KEY_ROWID + "));"
            ;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            ;//Log.w(TAG, DATABASE_CREATE);
            if(!firstRunAttachDB) {
                db.execSQL(CREATE_TABLE_NOTES);
                db.execSQL(CREATE_TABLE_SENTENCES);
            }
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
        mAndroidId = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public DbAdapter open() throws SQLException {
        attachDBIfNotExist();
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    void attachDBIfNotExist(){
        if(!firstRunAttachDB)
            return;

        String dbPathName = DB_PATH + DATABASE_NAME;
        if(!(new File(dbPathName)).exists()) { //DB not exists
            try {
                File f = new File(DB_PATH);
                if (!f.exists()) {
                    f.mkdir();
                }
                InputStream myInput = mCtx.getAssets().open(DATABASE_NAME);
                OutputStream myOutput = new FileOutputStream(dbPathName);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = myInput.read(buffer)) > 0) {
                    myOutput.write(buffer, 0, length);
                }
                myOutput.flush();
                myOutput.close();
                myInput.close();
            } catch (Exception e) {
                throw new Error("Unable to create database");
            }
        }
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


    public boolean isConnected() {
        ConnectivityManager manager = (ConnectivityManager)mCtx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = manager.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnectedOrConnecting());
    }

    public long insertNote(NoteActivity.NoteData a) {
        ContentValues args = new ContentValues();
        args.put(KEY_CALENDARMS   , a.time.getTimeInMillis());
        args.put(KEY_WORDCNT      , a.wordCnt);
        args.put(KEY_TEXT         , a.text);
        args.put(KEY_ANALYZED     , a.analyzed);
        args.put(KEY_REFLECTION     , a.reflection);
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
        if(isConnected() && noteId != -1) {
            insertNoteOnServer(noteId, a);
        }
        return noteId;
    }

    public boolean insertNoteOnServer(final long noteId, final NoteActivity.NoteData a) {
        boolean ret = false;
        final AVObject args = new AVObject(TABLE_NOTES);

        args.put(KEY_ANDROIDID, mAndroidId);
        args.put(KEY_NOTEID, noteId);
        //
        args.put(KEY_CALENDARMS, a.time.getTimeInMillis());
        args.put(KEY_WORDCNT      , a.wordCnt);
        args.put(KEY_TEXT         , a.text);
        args.put(KEY_ANALYZED     , a.analyzed);
        args.put(KEY_REFLECTION     , a.reflection);
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
        args.saveInBackground(new SaveCallback() {
            @Override
            public void done(AVException e) {
                if(e == null){
                    String noteObjectId = args.getObjectId();
                    if (a.analyzed) {
                        for (NoteActivity.sentenceEmotion s : a.sentences) {
                            insertSentenceOnServer(noteId, noteObjectId, s);
                        }
                    }
                }
                else{
                    Log.e(TAG, "error: insertNoteOnServer", e);
                }
            }
        });
        return ret;
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

    private void insertSentenceOnServer(long noteId, String noteObjectId, NoteActivity.sentenceEmotion a) {
        AVObject args = new AVObject(TABLE_SENTENCES);
        args.put(KEY_NOTEID, noteId);
        args.put(KEY_NOTEOBJECTID, noteObjectId);
        args.put(KEY_SENTENCEID, a.sentenceId);
        args.put(KEY_TOPEMOIDX, a.topEmotionIdx);
        args.put(KEY_TOPSCORE, a.topScore);
        args.put(KEY_TEXT, a.text);
        args.put(KEY_INPUTFROM, a.input_from);
        args.put(KEY_INPUTTO, a.input_to);

        args.saveInBackground(new SaveCallback() {
            @Override
            public void done(AVException e) {
                if (e != null) {
                    Log.e(TAG, "error: insertSentenceOnServer", e);
                }
            }
        });
    }

    private long deleteSentencesByNoteId(long noteId) {
        return mDb.delete(TABLE_SENTENCES, KEY_NOTEID + "=" + noteId, null);
    }

    public int deleteNote(long _id) {
        mDb.delete(TABLE_SENTENCES, KEY_NOTEID + "=" + _id, null); //delete all sentences first
        int ret = mDb.delete(TABLE_NOTES, KEY_ROWID + "=" + _id, null);

        if(isConnected()){
            deleteNoteOnServer(_id);
        }
        return ret;
    }

    private void deleteNoteOnServer(long noteId){
        //yk: there's currently no way to delete a note without knowing its noteObjectId. same limitation caused we can't do updateNoteOnServer.
//        //delete all sentences first
//        String query = "DELETE FROM " + TABLE_SENTENCES + " WHERE " + KEY_NOTEID + " = " + noteId;
//        AVQuery.doCloudQueryInBackground(query, new CloudQueryCallback<AVCloudQueryResult>() {
//            @Override
//            public void done(AVCloudQueryResult avCloudQueryResult, AVException e) {
//                if (e != null) {
//                    Log.e(TAG, "error: deleteSentencesByNoteIdOnServer", e);
//                }
//            }
//        });
//        //delete the note
//        query = "DELETE FROM " + TABLE_NOTES + " WHERE " + KEY_NOTEID + " = " + noteId;
//        AVQuery.doCloudQueryInBackground(query, new CloudQueryCallback<AVCloudQueryResult>() {
//            @Override
//            public void done(AVCloudQueryResult avCloudQueryResult, AVException e) {
//                if (e != null) {
//                    Log.e(TAG, "error: deleteNoteOnServer", e);
//                }
//            }
//        });
    }

    public boolean updateNote(long _id, NoteActivity.NoteData a) {
        ContentValues args = new ContentValues();
        //args.put(KEY_CALENDARMS   , a.time.getTimeInMillis());
        args.put(KEY_WORDCNT       , a.wordCnt);
        args.put(KEY_TEXT       , a.text);
        args.put(KEY_ANALYZED     , a.analyzed);
        args.put(KEY_REFLECTION     , a.reflection);
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
        //                if(isConnected()) {
//                 //yk todo   mDbHelper.updateNoteOnServer(mRowId, mNoteData);
//                }

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

    public Cursor querySentencesByScore(float scoreThreshold, int emotion) throws SQLException {
        Cursor cursor = mDb.query(TABLE_SENTENCES, null,
                KEY_TOPSCORE + " >= " + scoreThreshold + " AND " + KEY_TOPEMOIDX + "/2 == " + emotion,
                null, null, null, KEY_TOPSCORE + " DESC ", null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor fetchNotesList() {
        Cursor cursor = mDb.query(TABLE_NOTES, new String[] {KEY_ROWID, KEY_CALENDARMS,
                        KEY_TEXT, KEY_TOPEMOIDX, KEY_TOPSCORE, KEY_ANALYZED},
                null, null, null, null,  KEY_CALENDARMS + " DESC", null);

        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor fetchNotesByColumns(String[] columns, boolean Desc) {
        String order = Desc ? " DESC" : " ASC";
        Cursor cursor = mDb.query(TABLE_NOTES, columns, null, null, null, null, KEY_CALENDARMS + order, null);

        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor avgJasdf() {
        Cursor cursor = mDb.rawQuery("SELECT AVG(joy), AVG(anger), AVG(sadness), AVG(disgust), AVG(fear) FROM " + TABLE_NOTES, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor totalJasdf() {
        Cursor cursor = mDb.rawQuery("SELECT SUM(joy), SUM(anger), SUM(sadness), SUM(disgust), SUM(fear) FROM " + TABLE_NOTES, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor emotionalQuotesCnt() {
        Cursor cursor = mDb.rawQuery("SELECT (" + KEY_TOPEMOIDX + "/2) emotion, count(*) cnt FROM " + TABLE_SENTENCES + " WHERE " + KEY_TOPSCORE + " >= " + NoteActivity.sentenceEmotionThreshold + " GROUP BY emotion", null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

}
