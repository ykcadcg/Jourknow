package com.ak.jourknow;

import android.database.Cursor;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.AxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;

public class ReportActivity extends AppCompatActivity {

    private DbAdapter dbHelper;
    private LineChart mChart;
    private int mRowCnt;
    ArrayList<String> mXLables;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        dbHelper = new DbAdapter(this);
        dbHelper.open();
        initChart();
        updateChart();
    }

    private void initChart(){
        mChart = (LineChart) findViewById(R.id.chart1);
        mChart.setDrawGridBackground(false);
        mChart.setDescription("");
        mChart.setDrawBorders(false);

        mChart.getAxisRight().setEnabled(false);
        mChart.getAxisLeft().setDrawAxisLine(false);
        mChart.getXAxis().setDrawGridLines(false);
        mChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        mChart.setTouchEnabled(true);
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setPinchZoom(true);
        mChart.getLegend().setEnabled(true);
        mChart.getLegend().setPosition(Legend.LegendPosition.ABOVE_CHART_CENTER);

        AxisValueFormatter xFormatter = new AxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                if((int)value < 0)  //for case of only 1 point: left/ right are -1, 1
                    return mXLables.get(0);
                if((int)value >= mXLables.size())
                    return mXLables.get(mXLables.size() - 1);
                return mXLables.get((int)value);
            }
            @Override
            public int getDecimalDigits() {
                return 0;
            }
        };
        mChart.getXAxis().setValueFormatter(xFormatter);
    }

    private boolean updateChart(){

        String[] columns = new String[]{dbHelper.KEY_DATE, dbHelper.KEY_ANALYZED, dbHelper.KEY_JOY, dbHelper.KEY_ANGER, dbHelper.KEY_SADNESS, dbHelper.KEY_DISGUST, dbHelper.KEY_FEAR};
        Cursor cursor = dbHelper.fetchNotesByColumns(columns);
        if(cursor == null || (mRowCnt = cursor.getCount()) == 0)
            return false;

        mXLables = new ArrayList<>();
        ArrayList<ArrayList<Entry>> valueSets = new ArrayList<>();
        for (int emo = 0; emo < 5; emo++) {
            valueSets.add(new ArrayList<Entry>());
        }

        int entryId = 0;
        do{
            if(cursor.getInt(cursor.getColumnIndex(dbHelper.KEY_ANALYZED)) == 0) //not analyzed
                continue;
            String date = cursor.getString(cursor.getColumnIndex(dbHelper.KEY_DATE));
            mXLables.add(date);
            for (int emo = 0; emo < 5; emo++) {
                float score = cursor.getFloat(cursor.getColumnIndex(columns[emo + 2])); //from KEY_JOY
                valueSets.get(emo).add(new Entry(entryId, score));
            }
            entryId++;
        }
        while (cursor.moveToNext());

        ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        for (int emo = 0; emo < 5; emo++) {
            LineDataSet d = new LineDataSet(valueSets.get(emo), getResources().getStringArray(R.array.emotions)[emo]);
            d.setLineWidth(2.5f);
            d.setCircleRadius(4f);
            int color = Color.parseColor(getResources().getStringArray(R.array.jasdfColors)[emo * 2]);
            d.setColor(color);
            d.setCircleColor(color);
            d.setDrawValues(false);
            if(emo == 0)
                d.setDrawFilled(true);
            dataSets.add(d);
        }

        LineData data = new LineData(dataSets);
        mChart.setData(data);
        //mChart.animateX(200);
        mChart.invalidate();
        return true;
    }
}
