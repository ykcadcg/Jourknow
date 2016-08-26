package com.ak.jourknow;

import com.ibm.watson.developer_cloud.tone_analyzer.v3.ToneAnalyzer;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.Tone;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneAnalysis;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneOptions;

public class Analyzer {
    static ToneAnalyzer mService = new ToneAnalyzer(ToneAnalyzer.VERSION_DATE_2016_05_19);

    Analyzer(){
        mService.setUsernameAndPassword("1ddb6e92-d10f-4164-9e67-c13b669224ef", "fy0LwRZMMipp");
    }

    String Analyze(String script) {
        ToneAnalysis tone = mService.getTone(script, null).execute();
        //System.out.println(tone);
        return tone.toString();
    }
}
