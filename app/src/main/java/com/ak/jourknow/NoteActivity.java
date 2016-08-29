package com.ak.jourknow;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import com.github.mikephil.charting.charts.BubbleChart;
import com.github.mikephil.charting.data.BubbleData;
import com.github.mikephil.charting.data.BubbleDataSet;
import com.github.mikephil.charting.data.BubbleEntry;
import com.github.mikephil.charting.interfaces.datasets.IBubbleDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.ToneAnalyzer;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.SentenceTone;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.Tone;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneAnalysis;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneCategory;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneOptions;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneScore;
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
    private TextView mColoredText;
    private final boolean mIsShowDialog = true;
    private String mLanguage = "en_us"; //en_us, mandarin
    private String mEngineType = SpeechConstant.TYPE_CLOUD;
    private final String mVadbos = "5000";
    private final String mVadeos = "30000";
    private final String mPunc = "1";
    private DbAdapter mDbHelper;
    private int mRowId;
    private NoteData mNoteData;
    boolean recordGranted = false;

    //AI
    static ToneAnalyzer mToneAnalyzer = new ToneAnalyzer(ToneAnalyzer.VERSION_DATE_2016_05_19);

    //UI
    public final String[] jasdfColors = new String[]{ //strong/ median for each emotion
            "#FFD629", "#FFF173",//joy
            "#E80521", "#FFA197",//anger
            "#086DB2", "#69C3E2",//sadness
            "#592684", "#A779D8", //disgust
            "#325E2B", "#7DB258"//fear
    };

    private BubbleChart mChart;

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
        mColoredText = ((TextView) findViewById(R.id.coloredText));
        mNoteData = new NoteData();

        requestRecordPermission();

        Intent intent = getIntent();
        mRowId = intent.getIntExtra(MainActivity.EXTRA_ID, -1);
        mDbHelper = new DbAdapter(this);

        initChart();
        //entering an existing note
        if(mRowId != -1){
            loadNoteData(mRowId);
            mChart.setEnabled(false);
        }
        else {
            startListen();
        }
    }

    void loadNoteData(int rowId){
        mDbHelper.open();
        Cursor cursor = mDbHelper.queryById(mRowId);
        if(cursor != null) {
            //load note
            mNoteData.text = cursor.getString(cursor.getColumnIndex(mDbHelper.KEY_TEXT));
            mNoteData.analysisRaw = cursor.getString(cursor.getColumnIndex(mDbHelper.KEY_ANALYSISRAW));
            mNoteData.wordCnt =  cursor.getInt(cursor.getColumnIndex(mDbHelper.KEY_WORDCNT));
            mNoteData.jasdf[0] = cursor.getFloat(cursor.getColumnIndex(mDbHelper.KEY_JOY));
            mNoteData.jasdf[1] = cursor.getFloat(cursor.getColumnIndex(mDbHelper.KEY_ANGER));
            mNoteData.jasdf[2] = cursor.getFloat(cursor.getColumnIndex(mDbHelper.KEY_SADNESS));
            mNoteData.jasdf[3] = cursor.getFloat(cursor.getColumnIndex(mDbHelper.KEY_DISGUST));
            mNoteData.jasdf[4] = cursor.getFloat(cursor.getColumnIndex(mDbHelper.KEY_FEAR));
            mNoteData.topEmotion = cursor.getInt(cursor.getColumnIndex(mDbHelper.KEY_TOPEMOTION));
            mEditText.setText(mNoteData.text);

            cursor.close();
        }
        mDbHelper.close();

        updateChart();
        updateColoredText();
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
        switch (view.getId()) {
            // 开始听写
            // 如何判断一次听写结束：OnResult isLast=true 或者 onError
            case R.id.speak:
                startListen();
                break;

            case R.id.analyze:
                if(mIat.isListening())
                    mIat.stopListening();

                if (mEditText.getText().length() <= 0) {
                    break;
                }

                mNoteData.Analyze(mEditText.getText().toString());
                updateChart();
                updateColoredText();
                break;

            default:
                break;
        }
    }

    void startListen(){
        int ret = 0; // 函数调用返回值

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
            mDbHelper.insert(mNoteData);
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
        mToneAnalyzer.setUsernameAndPassword("1ddb6e92-d10f-4164-9e67-c13b669224ef", "fy0LwRZMMipp");

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

    private final float sentenceEmotionThreshold = 0.5f;
    private final float strongEmotionThreshold = 0.75f;
    public class sentenceEmotion{
        sentenceEmotion(int id, int topEmo, float s, String t, int from, int to){
            sentenceId = id;
            topEmotion = topEmo;
            score = s;
            text = t;
            input_from = from;
            input_to = to;
        }
        int sentenceId;
        int topEmotion; //0-9, emotionIdx*2 + 1(if moderate). Default -1.
        float score;
        String text;
        int input_from;
        int input_to;
    }

    public int emotionIdx(String emotion){
        switch (emotion) {
            case "joy":
                return 0;
            case "anger":
                return 1;
            case "sadness":
                return 2;
            case "disgust":
                return 3;
            case "fear":
                return 4;
            default:
                return -1;
        }
    }

    public class NoteData{
        Calendar time;
        String text;
        String analysisRaw;
        int wordCnt;
        float[] jasdf = {0,0,0,0,0}; //scores: joy, anger, sadness, disgust, fear   //{.9f, .8f, .7f, .6f, .5f};//
        int topEmotion; //0-9, emotionIdx*2 + 1(if moderate). Default -1.
        ArrayList<sentenceEmotion> sentences;

        NoteData(){
            time = Calendar.getInstance();
            sentences = new ArrayList<>();
        }

        void Analyze(String text){
            if(text.length() <= 0)
                return;
            mNoteData.text = text;

            ToneOptions options = new ToneOptions.Builder().addTone(Tone.EMOTION).build();
            ToneAnalysis tone = mToneAnalyzer.getTone(text, options).execute();
            Log.v("analysis: ", tone.toString());
            analysisRaw = tone.toString();

            ToneCategory emotionTone = tone.getDocumentTone().getTones().get(0);
            if(!emotionTone.getId().equalsIgnoreCase("emotion_tone")) {
                Log.e("analysis: ", "Failed parsing emotion_tone");
                return;
            }
            List<ToneScore> toneScores = emotionTone.getTones();
            float maxScore = 0;
            int topEmo = -1;
            for (ToneScore s : toneScores) {
                float score = s.getScore().floatValue();
                int idx = emotionIdx(s.getId());
                if(idx != -1){
                    mNoteData.jasdf[idx] = score;
                    if(score > maxScore){
                        maxScore = score;
                        topEmo = idx * 2;
                        if(maxScore < strongEmotionThreshold)
                            topEmo += 1; //moderate emotion
                    }
                }
            }
            mNoteData.topEmotion = topEmo;

            //update emotional sentences
            List<SentenceTone> sentenceTones = tone.getSentencesTone();
            for(SentenceTone t : sentenceTones){
                ToneCategory c = t.getTones().get(0);
                if(!c.getId().equalsIgnoreCase("emotion_tone")) {
                    Log.e("analysis: ", "Failed parsing emotion_tone for sentence " + t.getId());
                    continue;
                }
                List<ToneScore> scores = c.getTones();
                //find max score that's >threshold
                float maxScoreS = 0;
                int topEmoS = -1;
                for(ToneScore s: scores){
                    float score = s.getScore().floatValue();
                    int idx = emotionIdx(s.getId());
                    if(idx != -1) {
                        if (score > maxScoreS) {
                            maxScoreS = score;
                            topEmoS = idx * 2;
                            if(maxScoreS < strongEmotionThreshold)
                                topEmoS += 1; //moderate emotion
                        }
                    }
                }
                //if(maxScoreS > sentenceEmotionThreshold)
                mNoteData.sentences.add(new sentenceEmotion(t.getId(), topEmoS, maxScoreS, t.getText(), t.getInputFrom(), t.getInputTo()));
            }
        }
    };

    private void initChart(){
        //init
        mChart = (BubbleChart) findViewById(R.id.chart);
        //mChart.setDescription("");
        mChart.setDrawGridBackground(false);
        mChart.setTouchEnabled(true);
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setMaxVisibleValueCount(5);
        mChart.setPinchZoom(true);
        mChart.getXAxis().setEnabled(false);
        mChart.getAxisLeft().setEnabled(false);
        mChart.getAxisRight().setEnabled(false);
        mChart.setDescription("");
        //setBorderColor
        mChart.getXAxis().setAxisMinValue(-0.5f);
        mChart.getXAxis().setAxisMaxValue(5.5f);
        mChart.setDrawBorders(true);
        mChart.setBorderColor(getResources().getColor(R.color.colorPrimary));
    }

    private void updateChart(){
        if(mEditText.getText().length() <= 0)
            return;
        ArrayList<IBubbleDataSet> dataSets = new ArrayList<>();
        ArrayList<BubbleEntry> vals = new ArrayList<>();
        int[] colors = new int[5];
        for(int i = 0, pos = 0; i < 5; ++i) {
//            if(mNoteData.jasdf[i] < noteEmotionThreshold)
//                continue;
            BubbleEntry entry = new BubbleEntry(pos, 0, mNoteData.jasdf[i]); //x, y, size
            vals.add(entry);
            colors[pos] = Color.parseColor(jasdfColors[i * 2]);
            ++pos;
        }
        //        if(dataSets.size() == 0){ //no emotion detected
        //            mChart.setVisibility(0);
        //            return;
        //        }
        mChart.setEnabled(true);
        BubbleDataSet set = new BubbleDataSet(vals, "");

        set.setColors(colors);
        set.setDrawValues(false);
        dataSets.add(set);

        BubbleData data = new BubbleData(dataSets);
        data.setDrawValues(false);
        data.setHighlightCircleWidth(1.5f);

        mChart.setData(data);

        //mChart.setBackground()
        //mChart.animateX(200);
        mChart.invalidate();
    }

    private void updateColoredText(){
        if((mEditText.getText().length() <= 0) || (mNoteData.text == null) || (mNoteData.text.length() <= 0))
            return;
        //spannable: see http://blog.csdn.net/harvic880925/article/details/38984705
        SpannableString spanText = new SpannableString(mNoteData.text);
        BackgroundColorSpan[] spans = new BackgroundColorSpan[10];
        for (int i = 0; i < 10; ++i)
        {
            spans[i] = new BackgroundColorSpan(Color.parseColor(jasdfColors[i]));
        }

        for(sentenceEmotion sen : mNoteData.sentences){
            if(sen.score > sentenceEmotionThreshold){
                int spanIdx = sen.topEmotion;
                if(spanIdx >= 0 && spanIdx < spans.length)
                    spanText.setSpan(spans[spanIdx], sen.input_from, sen.input_to, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        mColoredText.setText(spanText);
    }
}
