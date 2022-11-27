package mladen.mosis.elfak.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.androidplot.Plot;
import com.androidplot.ui.widget.TextLabelWidget;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.CatmullRomInterpolator;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PanZoom;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class PlotActivity extends AppCompatActivity {
    private class MyPlotUpdater implements Observer {
        Plot plot;
        public MyPlotUpdater(Plot plot) {
            this.plot = plot;
        }
        @Override
        public void update(Observable observable, Object o) {
            plot.redraw();
        }
    }
    private Thread backgroundThread;

    private XYPlot dynamicPlot;
    private MyPlotUpdater plotUpdater;
    SampleDynamicXYDatasource data;
    SampleDynamicSeries tempSeries;
    SampleDynamicSeries humidSeries;
    private Thread myThread;
    private boolean liveData;
    private boolean showTemp, showHumid;
    private ArrayList<String> range;
    private int mDay, mMonth, mYear;
    private ArrayList<DataRecord> mArrTemp, mArrHumid;
    private LocalDateTime lastRead;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu2, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.t_settings:
                tempPlot();
                return true;
            case R.id.h_settings:
                humidPlot();
                return true;
            case R.id.th_settings:
                thPlot();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void humidPlot() {
        if(showTemp)
            dynamicPlot.removeSeries(tempSeries);
        if(!showHumid) {
            LineAndPointFormatter formatter2 =
                    new LineAndPointFormatter(Color.rgb(0, 0, 200), null, null, null);
            formatter2.getLinePaint().setStrokeWidth(10);
            formatter2.getLinePaint().setStrokeJoin(Paint.Join.ROUND);
            dynamicPlot.addSeries(humidSeries, formatter2);
        }

        showTemp=false;
        showHumid=true;
        dynamicPlot.setRangeStepValue(5);
        dynamicPlot.setRangeBoundaries(0, 100, BoundaryMode.FIXED);
        dynamicPlot.redraw();
    }

    private void tempPlot() {
        if(showHumid)
            dynamicPlot.removeSeries(humidSeries);
        if(!showTemp) {
            LineAndPointFormatter formatter1 = new LineAndPointFormatter(
                    Color.rgb(0, 200, 0), null, null, null);
            formatter1.getLinePaint().setStrokeJoin(Paint.Join.ROUND);
            formatter1.getLinePaint().setStrokeWidth(10);
            dynamicPlot.addSeries(tempSeries, formatter1);
        }

        showTemp=true;
        showHumid=false;
        dynamicPlot.setRangeStepValue(2);
        dynamicPlot.setRangeBoundaries(0, 50, BoundaryMode.FIXED);
        dynamicPlot.redraw();
    }

    private void thPlot() {
        if(!showTemp) {
            LineAndPointFormatter formatter1 = new LineAndPointFormatter(
                    Color.rgb(0, 200, 0), null, null, null);
            formatter1.getLinePaint().setStrokeJoin(Paint.Join.ROUND);
            formatter1.getLinePaint().setStrokeWidth(10);
            dynamicPlot.addSeries(tempSeries, formatter1);
        }

        if(!showHumid) {
            LineAndPointFormatter formatter2 =
                    new LineAndPointFormatter(Color.rgb(0, 0, 200), null, null, null);
            formatter2.getLinePaint().setStrokeWidth(10);
            formatter2.getLinePaint().setStrokeJoin(Paint.Join.ROUND);
            dynamicPlot.addSeries(humidSeries, formatter2);
        }
        showTemp=true;
        showHumid=true;
        dynamicPlot.setRangeStepMode(StepMode.INCREMENT_BY_VAL);
        dynamicPlot.setRangeStepValue(5);
        dynamicPlot.setRangeBoundaries(0, 100, BoundaryMode.FIXED);
        dynamicPlot.redraw();
    }

    @Override
    protected void onResume() {
        myThread = new Thread(data);
        myThread.start();
        super.onResume();
    }

    @Override
    protected void onPause() {
        data.stopThread();
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.xy_plot);
        dynamicPlot = (XYPlot) findViewById(R.id.plot);
        mArrTemp = this.getIntent().getParcelableArrayListExtra("temperatureData");
        mArrHumid = this.getIntent().getParcelableArrayListExtra("humidData");
        Log.d("HumidData cnt:", String.valueOf(mArrHumid.size()));
        Log.d("TempData cnt: ", String.valueOf(mArrTemp.size()));
        liveData = this.getIntent().getBooleanExtra("isLive", true);
        showHumid = true;
        showTemp = true;
        Log.d("isLive", String.valueOf(liveData));
        mDay = this.getIntent().getIntExtra("day", 0);
        mMonth = this.getIntent().getIntExtra("month", 0);
        mYear = this.getIntent().getIntExtra("year", 0);
        Log.d("date", String.valueOf(mDay) + "/" + String.valueOf(mMonth));
        dynamicPlot.setTitle(String.valueOf(mDay) + "/" +String.valueOf(mMonth+1) + "/" +String.valueOf(mYear));
        String lastR = this.getIntent().getStringExtra("lastRead");
        if(lastR.equals("noData"))
            lastRead = LocalDateTime.of(1997, 2, 18, 19,05);
        else
            lastRead = LocalDateTime.parse(lastR);
        Log.d("lastRead", lastR.toString());
        createDynamicPlot();
        PanZoom.attach(dynamicPlot);

    }


    private void createDynamicPlot() {
        Log.d("antra", "th_sett");
        plotUpdater = new MyPlotUpdater(dynamicPlot);

        // getInstance and position datasets:
        data = new SampleDynamicXYDatasource();
        tempSeries = new SampleDynamicSeries(data, 0, "\u00B0" + "C" + " Temperature");
        humidSeries = new SampleDynamicSeries(data, 1, "  % Humidity");

        // only display whole numbers in domain labels
        dynamicPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).
                setFormat(new NumberFormat() {
                    @NonNull
                    @Override
                    public StringBuffer format(double v, @NonNull StringBuffer stringBuffer, @NonNull FieldPosition fieldPosition) {
                        if(data.mapValues.get(0).size() > 0)
                            return new StringBuffer(data.mapValues.get(0).get((int)v).getFormattedTimestamp());
                        else return new StringBuffer("no data");
                    }

                    @NonNull
                    @Override
                    public StringBuffer format(long l, @NonNull StringBuffer stringBuffer, @NonNull FieldPosition fieldPosition) {
                        return null;
                    }

                    @Nullable
                    @Override
                    public Number parse(@NonNull String s, @NonNull ParsePosition parsePosition) {
                        return null;
                    }
                });

        LineAndPointFormatter formatter1 = new LineAndPointFormatter(
                Color.rgb(0, 200, 0), null, null, null);
        formatter1.getLinePaint().setStrokeJoin(Paint.Join.ROUND);
        formatter1.getLinePaint().setStrokeWidth(10);
        dynamicPlot.addSeries(tempSeries,
                formatter1);

        LineAndPointFormatter formatter2 =
                new LineAndPointFormatter(Color.rgb(0, 0, 200), null, null, null);
        formatter2.getLinePaint().setStrokeWidth(10);
        formatter2.getLinePaint().setStrokeJoin(Paint.Join.ROUND);

        //formatter2.getFillPaint().setAlpha(220);
        dynamicPlot.addSeries(humidSeries, formatter2);

        // hook up the plotUpdater to the data model:
        data.addObserver(plotUpdater);

        // thin out domain tick labels so they dont overlap each other:
        dynamicPlot.setDomainStepMode(StepMode.INCREMENT_BY_VAL);
        dynamicPlot.setDomainStepValue(5);

        dynamicPlot.setRangeStepMode(StepMode.INCREMENT_BY_VAL);
        dynamicPlot.setRangeStepValue(5);

        dynamicPlot.getGraph().getLineLabelStyle(
                XYGraphWidget.Edge.LEFT).setFormat(new DecimalFormat("###.#"));

        // uncomment this line to freeze the range boundaries:
        dynamicPlot.setRangeBoundaries(0, 100, BoundaryMode.FIXED);

        // create a dash effect for domain and range grid lines:
        DashPathEffect dashFx = new DashPathEffect(
                new float[] {PixelUtils.dpToPix(3), PixelUtils.dpToPix(3)}, 0);
        dynamicPlot.getGraph().getDomainGridLinePaint().setPathEffect(dashFx);
        dynamicPlot.getGraph().getRangeGridLinePaint().setPathEffect(dashFx);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    class SampleDynamicXYDatasource implements Runnable {

        // encapsulates management of the observers watching this datasource for update events:
        class MyObservable extends Observable {
            @Override
            public void notifyObservers() {
                setChanged();
                super.notifyObservers();
            }
        }

        private int SAMPLE_SIZE = 2;
        private MyObservable notifier;
        private boolean keepRunning = false;
        private HashMap<Integer,List<DataRecord>> mapValues;
        private InfluxDBManagement InfluxDBManager;

        {
            notifier = new MyObservable();
            mapValues = new HashMap<>();
            mapValues.put(0, mArrTemp);
            mapValues.put(1, mArrHumid);
            InfluxDBManager = new InfluxDBManagement();
            InfluxDBManager.setLastRead(lastRead);

        }

        void stopThread() {
            keepRunning = false;
        }

        //@Override
        public void run() {
            try {
                keepRunning = true;
                while (keepRunning) {
                    if(mapValues.get(0) != null && mapValues.get(1) != null && liveData) {
                        DataRecord[] tmp = InfluxDBManager.getLastValue("-2m");
                        if(!(tmp[0].getTimestamp().equals("err") || tmp[1].getTimestamp().equals("err"))) {
                            mapValues.get(0).add(tmp[0]);
                            mapValues.get(1).add(tmp[1]);
                        }
                    }
                    if(!liveData)
                        keepRunning=false;
                    SAMPLE_SIZE = mapValues.get(0).size();
                    Log.d("Samplesize:", String.valueOf(SAMPLE_SIZE));
                    notifier.notifyObservers();
                    Thread.sleep(10000); // decrease or remove to speed up the refresh rate.
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        int getItemCount(int series) {
            return SAMPLE_SIZE;
        }

        Number getX(int series, int index) {
            if (index >= SAMPLE_SIZE) {
                throw new IllegalArgumentException();
            }

            return index;
        }

        Number getY(int series, int index) {
            if (index >= SAMPLE_SIZE) {
                throw new IllegalArgumentException();
            }
            if(mapValues.get(0).size() > 1 && mapValues.get(1).size() > 1) {
                DataRecord rec = mapValues.get(series).get(index);
                return rec.getValue();
            }
            return 0;
        }

        void addObserver(Observer observer) {
            notifier.addObserver(observer);
        }

        public void removeObserver(Observer observer) {
            notifier.deleteObserver(observer);
        }

    }
    class SampleDynamicSeries implements XYSeries {
        private SampleDynamicXYDatasource datasource;
        private int seriesIndex;
        private String title;

        SampleDynamicSeries(SampleDynamicXYDatasource datasource, int seriesIndex, String title) {
            this.datasource = datasource;
            this.seriesIndex = seriesIndex;
            this.title = title;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public int size() {
            return datasource.getItemCount(seriesIndex);
        }

        @Override
        public Number getX(int index) {
            return datasource.getX(seriesIndex, index);
        }

        @Override
        public Number getY(int index) {
            return datasource.getY(seriesIndex, index);
        }
    }

}