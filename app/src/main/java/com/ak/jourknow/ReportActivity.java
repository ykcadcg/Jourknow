package com.ak.jourknow;

import android.database.Cursor;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.AxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;

public class ReportActivity extends AppCompatActivity {

    private DbAdapter dbHelper;
    private LineChart mChart1;
    private PieChart mChart2;
    private int mRowCnt;
    ArrayList<String> mXLables;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        dbHelper = new DbAdapter(this);
        dbHelper.open();
        initCharts();
        updateCharts();
    }

    private void initCharts() {
        initChart1();
        initChart2();
    }

    private void initChart1(){
        mChart1 = (LineChart) findViewById(R.id.chart1);
        mChart1.setDrawGridBackground(false);
        mChart1.setDescription(getString(R.string.emotionNumberDescription));
        mChart1.setDrawBorders(false);

        mChart1.getAxisRight().setEnabled(false);
        mChart1.getAxisLeft().setDrawAxisLine(false);
        mChart1.getXAxis().setDrawGridLines(false);
        mChart1.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        mChart1.setTouchEnabled(true);
        mChart1.setDragEnabled(true);
        mChart1.setScaleEnabled(true);
        mChart1.setPinchZoom(true);
        mChart1.getLegend().setEnabled(true);
        mChart1.getLegend().setPosition(Legend.LegendPosition.ABOVE_CHART_CENTER);

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
        mChart1.getXAxis().setValueFormatter(xFormatter);
    }

    private void initChart2(){
        mChart2 = (PieChart) findViewById(R.id.chart2);
        mChart2.setRotationEnabled(true);
        mChart2.setHighlightPerTapEnabled(true);
        mChart2.setDescription("");
        mChart2.setEntryLabelColor(getResources().getColor(R.color.colorPrimaryDark));
        mChart2.setEntryLabelTextSize(12f);
        mChart2.setTouchEnabled(true);
        //mChart2.getLegend().setEnabled(true);
        //mChart2.getLegend().setPosition(Legend.LegendPosition.ABOVE_CHART_CENTER);
    }

    private void updateCharts() {
        updateChart1();
        updateChart2();
    }

    private boolean updateChart1(){
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
        mChart1.setData(data);
        //mChart1.animateX(200);
        mChart1.invalidate();
        return true;
    }

    private boolean updateChart2(){
        Cursor cursor = dbHelper.avgJasdf();
        if(cursor == null || (mRowCnt = cursor.getCount()) == 0)
            return false;

        ArrayList<PieEntry> entries = new ArrayList<PieEntry>();
        int[] colors = new int[5];
        for (int i = 0; i < 5; i++) {
            entries.add(new PieEntry(cursor.getFloat(i), getResources().getStringArray(R.array.emotions)[i]));
            colors[i] = Color.parseColor(getResources().getStringArray(R.array.jasdfColors)[i * 2]);
        }
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        //data.setValueFormatter(new PercentFormatter());
        data.setValueTextSize(11f);
        data.setValueTextColor(getResources().getColor(R.color.colorPrimaryDark));
        mChart2.setData(data);

        mChart2.invalidate();
        return true;
    }
}
