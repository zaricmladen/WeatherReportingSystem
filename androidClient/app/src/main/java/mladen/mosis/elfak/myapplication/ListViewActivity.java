package mladen.mosis.elfak.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ListViewActivity extends AppCompatActivity {
    ListView mListView;
    ArrayAdapter<String> mArrAdapter;
    ArrayList<String> mArrList;
    HashMap<String, ArrayList<DataRecord>> dataRecords;
    Thread backgroundThread;
    boolean keepRunning;
    boolean firstStart;
    boolean sortAsc;
    boolean currentDateSelected;
    InfluxDBManagement mDbManager;
    Spinner spinner;
    Button btnSort, btnDate;
    DatePickerDialog mDatePickerDialog;
    int mDay,mMonth,mYear;
    private ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view);
        mListView = findViewById(R.id.listView);
        btnSort = findViewById(R.id.btnSort);
        btnDate = findViewById(R.id.btnDate);
        progressBar = (ProgressBar)findViewById(R.id.progressBar1);
        progressBar.setVisibility(View.VISIBLE);
        keepRunning = true;
        firstStart = true;
        currentDateSelected=true;
        sortAsc = true;
        mArrList = new ArrayList<>();
        dataRecords = new HashMap<>();
        dataRecords.put("Temperature", new ArrayList<DataRecord>());
        dataRecords.put("Humidity", new ArrayList<DataRecord>());
        mArrAdapter = new ArrayAdapter<String>(this, R.layout.list_item,mArrList) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                textView.setHeight(150);
                textView.setMinimumHeight(150);
                return view;
            }
        };

        final Calendar cldr = Calendar.getInstance();
        mDay = cldr.get(Calendar.DAY_OF_MONTH);
        mMonth = cldr.get(Calendar.MONTH);
        mYear = cldr.get(Calendar.YEAR);
        btnDate.setText(String.valueOf(mDay) + "/" +String.valueOf(mMonth+1) + "/" +String.valueOf(mYear));

        btnSort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(sortAsc) {
                    btnSort.setBackgroundResource(R.drawable.baseline_north_24);
                    sortAsc=false;
                }
                else {
                    btnSort.setBackgroundResource(R.drawable.baseline_south_24);
                    sortAsc=true;
                }
                Collections.reverse(mArrList);
                mArrAdapter.notifyDataSetChanged();
            }
        });
        btnDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // date picker dialog
                mDatePickerDialog = new DatePickerDialog(ListViewActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                btnDate.setText(String.valueOf(dayOfMonth) + "/" +String.valueOf(monthOfYear+1)+ "/" +String.valueOf(year));
                                firstStart=true;
                                mDay=dayOfMonth;
                                mMonth = monthOfYear;
                                mYear = year;

                                mArrList.clear();
                                final Calendar cldr = Calendar.getInstance();
                                if(mDay == cldr.get(Calendar.DAY_OF_MONTH) && mMonth == (cldr.get(Calendar.MONTH)) && mYear == cldr.get(Calendar.YEAR))
                                    currentDateSelected=true;
                                else
                                    currentDateSelected=false;
                                progressBar.setVisibility(View.VISIBLE);

                            }
                        }, mYear, mMonth, mDay);
                mDatePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
                mDatePickerDialog.show();
            }
        });
        mListView.setAdapter(mArrAdapter);
        mDbManager = new InfluxDBManagement();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.plot_settings:
                startPlotView();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startPlotView() {
        Intent intent = new Intent(ListViewActivity.this, PlotActivity.class);
        intent.putExtra("isLive", currentDateSelected);
        intent.putExtra("day", mDay);
        intent.putExtra("month", mMonth);
        intent.putExtra("year", mYear);
        intent.putParcelableArrayListExtra("temperatureData", dataRecords.get("Temperature"));
        intent.putParcelableArrayListExtra("humidData", dataRecords.get("Humidity"));
        String lastRead;
        if(mDbManager.getLastRead() != null)
            lastRead = mDbManager.getLastRead().toString();
        else
            lastRead = "noData";
        intent.putExtra("lastRead", lastRead);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        keepRunning=true;
        initRead();
        super.onResume();
    }

    @Override
    protected void onPause() {
        keepRunning = false;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void initRead() {
        backgroundThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while(keepRunning) {
                        if(firstStart) {
                            dataRecords = mDbManager.QueryDBLogger(buildRangeString(mDay,mMonth,mYear));
                            if(dataRecords.get("Temperature").size() > 0 && dataRecords.get("Humidity").size() > 0)
                                showDataRecords();
                            firstStart=false;
                        }
                        else if(currentDateSelected) {
                            DataRecord[] tmp = mDbManager.getLastValue("-2m");
                            Log.d("lastvalue: ", tmp[0] + " " +tmp[1]);
                            if(!(tmp[0].getTimestamp().equals("err") || tmp[1].getTimestamp().equals("err"))) {
                               addSingleDataRecord(tmp);
                            }
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mArrAdapter.notifyDataSetChanged();
                                progressBar.setVisibility(View.GONE);
                            }
                        });
                        Thread.sleep(1000);
                    }
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
        });
        backgroundThread.start();
    }

    private String[] buildRangeString(int d, int m, int y) {
        String[] ret = new String[2];
        String month;
        String day;
        LocalDateTime startlocalDateTime = LocalDateTime.of(y,m+1,d,00,00,01);
        ZonedDateTime startzonedDateTime = localtoUTC(startlocalDateTime);
        LocalDateTime stoplocalDateTime = LocalDateTime.of(y,m+1,d,23,59, 59);
        ZonedDateTime stopzonedDateTime = localtoUTC(stoplocalDateTime);
        Log.d("localDateTime", startlocalDateTime.toString());
        Log.d("ZonedDateTime", startzonedDateTime.toString());
        ret[0] = startzonedDateTime.toString();
        ret[1] = stopzonedDateTime.toString();
        return ret;
    }

    private ZonedDateTime localtoUTC(LocalDateTime localDateTime) {
        ZonedDateTime ldtZoned,utcZoned;
        ldtZoned = localDateTime.atZone(ZoneId.systemDefault());
        utcZoned = ldtZoned.withZoneSameInstant(ZoneOffset.UTC);
        return utcZoned;
    }

    private void showDataRecords() {
        int tmpInd=0;
        for(DataRecord dr : dataRecords.get("Temperature")) {
            mArrList.add(dr.getTimestamp() + "\n" + "| Temperature: " + String.valueOf(dr.getValue()) + "\u2103" + " |");
        }
        for(DataRecord dr : dataRecords.get("Humidity")) {
            mArrList.set(tmpInd, mArrList.get(tmpInd) + " Humidity: " + String.valueOf(dr.getValue()) + "% |");
            tmpInd++;
        }
    }

    private void addSingleDataRecord(DataRecord[] dr) {
        dataRecords.get("Temperature").add(dr[0]);
        dataRecords.get("Humidity").add(dr[1]);

        String tmp = dr[0].getTimestamp() + "\n" + "| Temperature: " + String.valueOf(dr[0].getValue()) + "\u2103" + " |";
        tmp += " Humidity: " + String.valueOf(dr[1].getValue()) + "% |";

        if(sortAsc)
            mArrList.add(tmp);
        else
            mArrList.add(0,tmp);

    }
}