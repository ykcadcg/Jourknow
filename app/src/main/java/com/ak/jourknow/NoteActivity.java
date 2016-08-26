package com.ak.jourknow;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.ak.jourknow.speech.util.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;


public class NoteActivity extends AppCompatActivity implements View.OnClickListener {
    private static String TAG = NoteActivity.class.getSimpleName();

    private SpeechRecognizer mIat;
    private RecognizerDialog mIatDialog;
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
    private Toast mToast;
    private EditText mEditText;
    private final boolean mIsShowDialog = true;
    private String mLanguage = "en_us"; //en_us, mandarin
    private String mEngineType = SpeechConstant.TYPE_CLOUD;
    private final String mVadbos = "5000";
    private final String mVadeos = "30000";
    private final String mPunc = "1";
    private static Analyzer mAnalyzer = new Analyzer();
    private DbAdapter mDbHelper;
    private int mRowId;

    public class NoteData{
        Calendar time;
        String text;
        String analysis;
        int wordCnt;
        NoteData(){
            time = Calendar.getInstance();
        }
    };
    private NoteData mNoteData;

    boolean recordGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        //speech
        SpeechUtility.createUtility(NoteActivity.this, "appid=" + getString(R.string.app_id));
        mIat = SpeechRecognizer.createRecognizer(NoteActivity.this, mInitListener);
        mIatDialog = new RecognizerDialog(NoteActivity.this, mInitListener);

        loadSettings();

        //UI
        findViewById(R.id.speak).setOnClickListener(NoteActivity.this);
        findViewById(R.id.analyze).setOnClickListener(NoteActivity.this);
        //mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        mEditText = ((EditText) findViewById(R.id.iat_text));
        mNoteData = new NoteData();

        requestRecordPermission();

        Intent intent = getIntent();
        mRowId = intent.getIntExtra(MainActivity.EXTRA_ID, -1);
        mDbHelper = new DbAdapter(this);

        //entering an existing note
        if(mRowId != -1){
            mDbHelper.open();
            Cursor cursor = mDbHelper.fetchScript(mRowId);
            if(cursor != null) {
                String text = cursor.getString(0);
                mEditText.setText(text);
                cursor.close();
            }
            mDbHelper.close();
        }
    }

    public static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    public void requestRecordPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            recordGranted = false;
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(NoteActivity.this, "You have denied my Microphone access. To record, grant me Microphone access first.", Toast.LENGTH_SHORT).show();
            }
            //else
            {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
            }
        } else {
            recordGranted = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    recordGranted = true;
                } else {
                    recordGranted = false;
                }
                return;
            }
        }
    }

    @Override
    public void onClick(View view) {
        int ret = 0; // 函数调用返回值
        switch (view.getId()) {
            // 开始听写
            // 如何判断一次听写结束：OnResult isLast=true 或者 onError
            case R.id.speak:
                if (!recordGranted) {
                    requestRecordPermission();
                    return;
                }
                mIatResults.clear();

                if (mIsShowDialog) {
                    // 显示听写对话框
                    mIatDialog.setListener(mRecognizerDialogListener);
                    mIatDialog.show();

                } else {
                    // 不显示听写对话框
                    ret = mIat.startListening(mRecognizerListener);
                    if (ret != ErrorCode.SUCCESS) {
                        showTip(getString(R.string.errorCode) + ret);
                    } else {
                        showTip(getString(R.string.pleaseSpeak));
                    }
                }
                break;

            case R.id.analyze:
                if(mIat.isListening())
                    mIat.stopListening();

                if (mEditText.getText().length() <= 0) {
                    break;
                }
                String analysis = mAnalyzer.Analyze(mEditText.getText().toString());
                ((TextView)findViewById(R.id.analysis)).setText(analysis);
                //updateRecord();
                break;

            default:
                break;
        }
    }

    @Override
    public void onPause() {
        if(mIat.isListening())
            mIat.stopListening();

        updateRecord();
        super.onPause();
    }

    private void updateRecord(){
        if(mEditText.getText().length() <= 0)
            return;
        mNoteData.text = mEditText.getText().toString();
        mNoteData.wordCnt = mEditText.getText().toString().split("\\s+").length;
        mDbHelper.open();
        if(mRowId == -1)
            mDbHelper.addRecord(mNoteData);
        else
            mDbHelper.update(mRowId, mNoteData);
        mDbHelper.close();
    }

    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            //Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip(getString(R.string.errorCode) + code);
            }
        }
    };

    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            //showTip(getString(R.string.pleaseSpeak));
        }

        @Override
        public void onError(SpeechError error) {
            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            // 如果使用本地功能（语记）需要提示用户开启语记的录音权限。
            showTip(error.getPlainDescription(true));
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            //showTip("结束说话");
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d(TAG, results.getResultString());
            printResult(results);

            if (isLast) {
                // TODO 最后的结果
            }
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            //showTip("当前正在说话，音量大小：" + volume);
            //Log.d(TAG, "返回音频数据："+data.length);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };

    private void printResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());
        if(text.length() <= 0) return;
        if(!mLanguage.equalsIgnoreCase("mandarin")) {
            //for non-Chinese: remove leading ','; skip single '.' case; uppercase first letter; add ending '. '.
            if (text.charAt(0) == ',') {
                text = text.substring(1);
            }
            text = text.trim();
            if((text.length() <= 0) || text.equalsIgnoreCase("."))
                return;
            text = text.substring(0, 1).toUpperCase() + text.substring(1) + ". ";
        }
        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mIatResults.put(sn, text);

        //yk why do this?
//        StringBuffer resultBuffer = new StringBuffer();
//        for (String key : mIatResults.keySet()) {
//            resultBuffer.append(mIatResults.get(key));
//        }
//        mEditText.setText(resultBuffer.toString());

        mEditText.getText().insert(mEditText.getSelectionStart(), text);
        mEditText.setSelection(mEditText.length());
    }

    //听写UI监听器
    private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
        public void onResult(RecognizerResult results, boolean isLast) {
            printResult(results);
        }
        //识别回调错误.
        public void onError(SpeechError error) {
            showTip(error.getPlainDescription(true));
        }
    };

    private void showTip(final String str) {
        mToast.setText(str);
        mToast.show();
    }

    public void loadSettings() {
        mIatDialog.setUILanguage(new Locale("en", "US"));

        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);
        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");
        String lag = mLanguage;
        if (lag.equals("en_us")) {
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");
        } else {
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
            // 设置语言区域
            mIat.setParameter(SpeechConstant.ACCENT, lag);
        }
        mIat.setParameter(SpeechConstant.VAD_BOS, mVadbos);
        mIat.setParameter(SpeechConstant.VAD_EOS, mVadeos);
        mIat.setParameter(SpeechConstant.ASR_PTT, mPunc);
        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        //mIat.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
        //mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/iat.wav");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 退出时释放连接
        mIat.cancel();
        mIat.destroy();
    }

}
