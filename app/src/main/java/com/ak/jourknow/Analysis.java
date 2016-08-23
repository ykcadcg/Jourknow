package com.ak.jourknow;

import com.ibm.watson.developer_cloud.tone_analyzer.v3.ToneAnalyzer;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.Tone;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneAnalysis;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneOptions;

public class Analysis {

    String Analyze(String script) {
        ToneAnalyzer service = new ToneAnalyzer(ToneAnalyzer.VERSION_DATE_2016_05_19);
        service.setUsernameAndPassword("1ddb6e92-d10f-4164-9e67-c13b669224ef", "fy0LwRZMMipp");

// Call the service and get the tone
        ToneAnalysis tone = service.getTone(script, null).execute();
        System.out.println(tone);
        return tone.toString();
    }
}
