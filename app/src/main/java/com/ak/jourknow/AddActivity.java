package com.ak.jourknow;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.StrictMode;
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


public class AddActivity extends AppCompatActivity implements View.OnClickListener {
    private static String TAG = AddActivity.class.getSimpleName();

    private SpeechRecognizer mIat;
    private RecognizerDialog mIatDialog;
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
    private String mEngineType = SpeechConstant.TYPE_CLOUD;
    private Toast mToast;
    private EditText mResultText;
    private final boolean mIsShowDialog = true;
    private String mLanguage = "mandarin"; //en_us, mandarin
    private final String mVadbos = "4000";
    private final String mVadeos = "30000";
    private final String mPunc = "1";
    public class Result{
        Calendar rightNow;
        int lengthMin, lengthSec;
        long startTime;
        String script;
        Analysis mAnalysis;
        Result() {
            mAnalysis = new Analysis();
        }
    };
    static Result mResult;
    boolean recordGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);


        SpeechUtility.createUtility(AddActivity.this, "appid=" + getString(R.string.app_id));

        initLayout();
        mIat = SpeechRecognizer.createRecognizer(AddActivity.this, mInitListener);
        mIatDialog = new RecognizerDialog(AddActivity.this, mInitListener);
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        mResultText = ((EditText) findViewById(R.id.iat_text));

        requestRecordPermission();
    }


    public static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    public void requestRecordPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            recordGranted = false;

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {

                Toast.makeText(AddActivity.this, "您拒绝了应用使用麦克风。为了正常使用，请允许应用使用麦克风。", Toast.LENGTH_SHORT).show();
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            }
            //else
            {

                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
            }
        } else {
            recordGranted = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    recordGranted = true;
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {
                    recordGranted = false;
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void initLayout() {
        findViewById(R.id.iat_recognize).setOnClickListener(AddActivity.this);
        findViewById(R.id.iat_stop).setOnClickListener(AddActivity.this);
        findViewById(R.id.iat_language).setOnClickListener(AddActivity.this);
        findViewById(R.id.iat_analyze).setOnClickListener(AddActivity.this);
    }

    int ret = 0; // 函数调用返回值

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            // 开始听写
            // 如何判断一次听写结束：OnResult isLast=true 或者 onError
            case R.id.iat_recognize:
                mResultText.setText(null);// 清空显示内容
                mIatResults.clear();
                // 设置参数
                setParam();
                boolean isShowDialog = mIsShowDialog;
                boolean started = false;

                if (!recordGranted) {
                    requestRecordPermission();
                    return;
                }

                if (isShowDialog) {
                    // 显示听写对话框
                    mIatDialog.setListener(mRecognizerDialogListener);
                    mIatDialog.show();
                    showTip(getString(R.string.text_begin));
                    started = true;
                } else {
                    // 不显示听写对话框
                    ret = mIat.startListening(mRecognizerListener);
                    if (ret != ErrorCode.SUCCESS) {
                        showTip("听写失败,错误码：" + ret);
                    } else {
                        showTip(getString(R.string.text_begin));
                        started = true;
                    }
                }
                if (started) {
                    mResult = new Result();
                    mResult.startTime = System.currentTimeMillis();
                    mResult.rightNow = Calendar.getInstance();
                }
                break;

            case R.id.iat_stop:
                if(mIat.isListening())
                {
                    mIat.stopListening();
                    showTip("停止听写");
                    if (mResult != null){
                        long millis = System.currentTimeMillis() - mResult.startTime;
                        int seconds = (int) (millis / 1000);
                        mResult.lengthMin = seconds / 60;
                        mResult.lengthSec = seconds % 60;
                        mResult.script = mResultText.getText().toString();
                        DbAdapter dbHelper = new DbAdapter(this);
                        dbHelper.open();
                        dbHelper.addRecord(mResult);
                    }
                }

                break;

            case R.id.iat_language:
                //mIat.cancel();
                mLanguage = (mLanguage == "mandarin" ? "en_us": "mandarin");
                mIat.setParameter(SpeechConstant.LANGUAGE, mLanguage);
                showTip("Language: "+ mLanguage);
                break;

            case R.id.iat_analyze:
                if (mResult != null && mResultText.getText().length()> 0) {
                    TextView analysisView = (TextView)findViewById(R.id.analysis);
                    String script = mResultText.getText().toString();
                    String analysis = mResult.mAnalysis.Analyze(script);
                    analysisView.setText(analysis);
                }
                break;

            default:
                break;
        }
    }

    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败，错误码：" + code);
            }
        }
    };

    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            showTip("开始说话");
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
            showTip("结束说话");
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
            showTip("当前正在说话，音量大小：" + volume);
            Log.d(TAG, "返回音频数据："+data.length);
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

        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mIatResults.put(sn, text);

        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }

        mResultText.setText(resultBuffer.toString());
        mResultText.setSelection(mResultText.length());
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

    /**
     * 参数设置
     *
     * @param param
     * @return
     */
    public void setParam() {
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
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/iat.wav");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 退出时释放连接
        mIat.cancel();
        mIat.destroy();
    }

}
