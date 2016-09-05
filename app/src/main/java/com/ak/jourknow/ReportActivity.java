package com.ak.jourknow;

import android.database.Cursor;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.github.mikephil.charting.charts.BarChart;
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
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ReportActivity extends AppCompatActivity {

    private DbAdapter dbHelper;
    private BarChart mChart1;
    private PieChart mChart2, mChart3, mChart4;
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

        mChart2 = (PieChart) findViewById(R.id.chart2);
        initChart(mChart2);

        mChart3 = (PieChart) findViewById(R.id.chart3);
        initChart(mChart3);

        mChart4 = (PieChart) findViewById(R.id.chart4);
        initChart(mChart4);
    }


    private void initChart1(){
        mChart1 = (BarChart) findViewById(R.id.chart1);
        mChart1.setDrawGridBackground(false);
        mChart1.setDescription(getString(R.string.emotionNumberDescription));
        mChart1.setDescriptionTextSize(14f);
        mChart1.setDrawBorders(false);
        mChart1.setDrawBarShadow(false);

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

    private void initChart(PieChart chart) {
        chart.setRotationEnabled(true);
        chart.setHighlightPerTapEnabled(true);
        chart.setDescription("");
        chart.setEntryLabelColor(getResources().getColor(R.color.colorPrimaryDark));
        chart.setEntryLabelTextSize(12f);
        chart.setTouchEnabled(true);
        //chart.getLegend().setEnabled(true);
        //chart.getLegend().setPosition(Legend.LegendPosition.ABOVE_CHART_CENTER);
    }
    
    private void updateCharts() {
        updateChart1();
        updateChart2();
        updateChart3();
        updateChart4();
    }

    private boolean updateChart1(){
        String[] columns = new String[]{dbHelper.KEY_CALENDARMS, dbHelper.KEY_ANALYZED, dbHelper.KEY_JOY, dbHelper.KEY_ANGER, dbHelper.KEY_SADNESS, dbHelper.KEY_DISGUST, dbHelper.KEY_FEAR};
        Cursor cursor = dbHelper.fetchNotesByColumns(columns, false);
        if(cursor == null || cursor.getCount() == 0)
            return false;

        mXLables = new ArrayList<>();
        ArrayList<ArrayList<BarEntry>> valueSets = new ArrayList<>();
        for (int emo = 0; emo < 5; emo++) {
            valueSets.add(new ArrayList<BarEntry>());
        }

        SimpleDateFormat dateFormatter = new SimpleDateFormat("MM/dd");
        int entryId = 0;
        do{
            if(cursor.getInt(cursor.getColumnIndex(dbHelper.KEY_ANALYZED)) == 0) //not analyzed
                continue;

            long calendarMs = cursor.getLong(cursor.getColumnIndexOrThrow(DbAdapter.KEY_CALENDARMS));
            String date = dateFormatter.format(new Date(calendarMs));
            mXLables.add(date);
            for (int emo = 0; emo < 5; emo++) {
                float score = cursor.getFloat(cursor.getColumnIndex(columns[emo + 2])); //from KEY_JOY
                valueSets.get(emo).add(new BarEntry(entryId, score));
            }
            entryId++;
        }
        while (cursor.moveToNext());

        ArrayList<IBarDataSet> dataSets = new ArrayList<>();
        for (int emo = 0; emo < 5; emo++) {
            BarDataSet d = new BarDataSet(valueSets.get(emo), getResources().getStringArray(R.array.emotions)[emo]);
            int color = Color.parseColor(getResources().getStringArray(R.array.jasdfColors)[emo * 2]);
            d.setColor(color);
            d.setDrawValues(false);
            dataSets.add(d);
        }

        BarData data = new BarData(dataSets);
        mChart1.setData(data);
        //mChart1.animateX(200);
        mChart1.invalidate();
        return true;
    }

    private boolean updateChart2(){
        Cursor cursor = dbHelper.avgJasdf();
        if(cursor == null || cursor.getCount() == 0)
            return false;

        ArrayList<PieEntry> entries = new ArrayList<PieEntry>();
        int[] colors = new int[5];

        for (int i = 0; i < 5; i++) {
            entries.add(new PieEntry(cursor.getFloat(i), getResources().getStringArray(R.array.emotions)[i]));
            colors[i] = Color.parseColor(getResources().getStringArray(R.array.jasdfColors)[i * 2]);
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        return configPieChart(mChart2, dataSet, colors);
    }

    private boolean updateChart4(){
        Cursor cursor = dbHelper.totalJasdf();
        if(cursor == null || cursor.getCount() == 0)
            return false;

        ArrayList<PieEntry> entries = new ArrayList<PieEntry>();
        int[] colors = new int[5];

        for (int i = 0; i < 5; i++) {
            entries.add(new PieEntry(cursor.getFloat(i), getResources().getStringArray(R.array.emotions)[i]));
            colors[i] = Color.parseColor(getResources().getStringArray(R.array.jasdfColors)[i * 2]);
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        return configPieChart(mChart4, dataSet, colors);
    }

    private boolean updateChart3(){
        Cursor cursor = dbHelper.emotionalQuotesCnt();
        if(cursor == null || cursor.getCount() == 0)
            return false;

        ArrayList<PieEntry> entries = new ArrayList<PieEntry>();
        int[] colors = new int[5];

        int entryId = 0;
        do{
            int emotion = cursor.getInt(cursor.getColumnIndexOrThrow("emotion"));
            int cnt = cursor.getInt(cursor.getColumnIndexOrThrow("cnt"));
            entries.add(new PieEntry(cnt, getResources().getStringArray(R.array.emotions)[emotion]));
            
            colors[entryId] = Color.parseColor(getResources().getStringArray(R.array.jasdfColors)[emotion * 2]);

            entryId++;
        }
        while (cursor.moveToNext());


        PieDataSet dataSet = new PieDataSet(entries, "");
        //chart3's formatter
        ValueFormatter intFormatter = new ValueFormatter () {
            @Override
            public String getFormattedValue(float value, Entry e, int i, ViewPortHandler h) {
                return "" + ((int) value);
            }
        };
        dataSet.setValueFormatter(intFormatter);

        return configPieChart(mChart3, dataSet, colors);
    }

    private boolean configPieChart(PieChart chart, PieDataSet dataSet, int[] colors){
        if(dataSet.getEntryCount() == 0)
            return false;

        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        //data.setValueFormatter(new PercentFormatter());
        data.setValueTextSize(11f);
        data.setValueTextColor(getResources().getColor(R.color.colorPrimaryDark));
        chart.setData(data);

        chart.invalidate();
        return true;
    }

}
