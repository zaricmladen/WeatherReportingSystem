package mladen.mosis.elfak.myapplication;
import android.util.Log;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InfluxDBManagement {

    private static char[] token = ("z1UJveRLL4uQ-u8aNEh3Fme3HJZYCVIFMCeNkAZuSE-9yADkcNSjXBIT7adXtaSbBW8r3dZXLQKG3RKR6_BW6Q==").toCharArray();
    private static String org = "mladen019@gmail.com";
    private static String bucket = "DataBucket";
    private static String url = "https://eu-central-1-1.aws.cloud2.influxdata.com";
    private InfluxDBClient influxClient;
    private QueryApi queryApi;
    private LocalDateTime lastRead;


    public InfluxDBManagement() {
        influxClient = InfluxDBClientFactory.create(url, token, org,bucket);
        queryApi = influxClient.getQueryApi();
        lastRead = null;
    }

    public HashMap<String, ArrayList<DataRecord>> QueryDBLogger(String[] range) {
        String base = "from(bucket:\"DataBucket\") |> range(start: " + range[0] + ", stop: " + range[1] + ")" + "|> filter(fn: (r) => r._field==";
        String tempQuery = base + "\"Temperature\"" + ")";
        String humidQuery = base + "\"Humidity\"" + ")";
        Log.d("string", base);

        HashMap<String, ArrayList<DataRecord>> retMap = new HashMap<>();
        retMap.put("Temperature", new ArrayList<DataRecord>());
        retMap.put("Humidity", new ArrayList<DataRecord>());
        List<FluxTable> arrTables = queryApi.query(tempQuery);
        if(influxClient!=null && queryApi !=null) {
            if(arrTables.size() > 0) {
                FluxTable tTable = arrTables.get(0);
                FluxTable hTable = queryApi.query(humidQuery).get(0);
                List<FluxRecord> recT = tTable.getRecords();
                List<FluxRecord> recH = hTable.getRecords();
                for(FluxRecord fluxRecord : recT) {
                    LocalDateTime dateTime = LocalDateTime.ofInstant(fluxRecord.getTime(), ZoneId.of(ZoneId.systemDefault().toString()));
                    String formatted = DateTimeFormatter.ofPattern("HH:mm:ss").format(dateTime);
                    retMap.get("Temperature").add(new DataRecord((long)fluxRecord.getValueByKey("_value"),formatted));
                    Log.d("187", fluxRecord.getTime() + ": " + fluxRecord.getValueByKey("_value"));
                    setLastRead(dateTime);
                }
                for(FluxRecord fluxRecord : recH) {
                    LocalDateTime dateTime = LocalDateTime.ofInstant(fluxRecord.getTime(), ZoneId.of(ZoneId.systemDefault().toString()));
                    String formatted = DateTimeFormatter.ofPattern("HH:mm:ss").format(dateTime);
                    retMap.get("Humidity").add(new DataRecord((long)fluxRecord.getValueByKey("_value"),formatted));
                    Log.d("187", fluxRecord.getTime() + ": " + fluxRecord.getValueByKey("_value"));
                }
            }
        }

        return retMap;
    }

    public ArrayList<String> queryDBStrings(String[] range) {
        String base = "from(bucket:\"DataBucket\") |> range(start: " + range[0] + ", stop: " + range[1] + ")" + "|> filter(fn: (r) => r._field==";
        String tempQuery = base + "\"Temperature\"" + ")";
        String humidQuery = base + "\"Humidity\"" + ")";
        Log.d("string", base);
        Log.d("tempquery", tempQuery);
        ArrayList<String> retList = new ArrayList<>();
        List<FluxTable> arrTables = queryApi.query(tempQuery);
        Log.d("prvi", String.valueOf(arrTables.size()));
        if(influxClient!=null && queryApi !=null) {
            if(arrTables.size() > 0)
            {
                FluxTable tTable = arrTables.get(0);
                FluxTable hTable = queryApi.query(humidQuery).get(0);
                if(tTable != null && hTable!=null) {
                    List<FluxRecord> recT = tTable.getRecords();
                    List<FluxRecord> recH = hTable.getRecords();
                    int tmpInd=0;
                    for(FluxRecord fluxRecord : recT) {
                        LocalDateTime dateTime = LocalDateTime.ofInstant(fluxRecord.getTime(),ZoneId.of(ZoneId.systemDefault().toString()));
                        Log.d("ldt", dateTime.toString());
                        String formatted = DateTimeFormatter.ofPattern("HH:mm:ss").format(dateTime);
                        retList.add(formatted + "\n" + "| Temperature: " + fluxRecord.getValueByKey("_value").toString() + "\u2103" + " |");
                        setLastRead(dateTime);
                    }
                    for(FluxRecord fluxRecord : recH) {
                        retList.set(tmpInd, retList.get(tmpInd) + " Humidity: " + fluxRecord.getValueByKey("_value").toString() + "% |");
                        tmpInd++;
                    }
            }

            }

        }
        return retList;
    }

    public int countPoints(String range) {
        String base = "from(bucket:\"DataBucket\") |> range(start: " + range + ")" + "|> filter(fn: (r) => r._field==";
        String tempQuery = base + "\"Temperature\"" + ")";
        tempQuery += "|> count()";
        int retValue=-1;
        if(influxClient != null && queryApi != null) {
            FluxTable tTable = queryApi.query(tempQuery).get(0);
            if(tTable != null) {
                List<FluxRecord> recT = tTable.getRecords();
                for(FluxRecord fr : recT) {
                    Log.d("count", fr.getValueByKey("_value").toString());
                    retValue = Integer.parseInt(fr.getValueByKey("_value").toString());
                }
            }
        }
        return retValue;
    }

    public DataRecord[] getLastValue(String range) {
        DataRecord[] ret = new DataRecord[2];
        ret[0] = new DataRecord();
        ret[1] = new DataRecord();
        String base = "from(bucket:\"DataBucket\") |> range(start: " + range + ")" + "|> filter(fn: (r) => r._field==";
        String tempQuery = base + "\"Temperature\"" + ")";
        String humidQuery = base + "\"Humidity\"" + ")";
        tempQuery += "|> last()";
        humidQuery += "|> last()";
        ret[0].setTimestamp("err");
        ret[1].setTimestamp("err");
        if (influxClient != null && queryApi != null) {
            List<FluxTable> arrTables = queryApi.query(tempQuery);
            Log.d("arSize", String.valueOf(arrTables.size()));
            if((arrTables.size() > 0)) {
                FluxTable tTable = arrTables.get(0);
                FluxTable hTable = queryApi.query(humidQuery).get(0);
                if(tTable!=null && hTable !=null) {
                    List<FluxRecord> recT = tTable.getRecords();
                    List<FluxRecord> recH = hTable.getRecords();
                    for (FluxRecord fluxRecord : recT) {
                        LocalDateTime dateTime = LocalDateTime.ofInstant(fluxRecord.getTime(), ZoneId.of(ZoneId.systemDefault().toString()));
                        String formatted = DateTimeFormatter.ofPattern("HH:mm:ss").format(dateTime);
                        if(dateTime.isAfter(getLastRead())) {
                            ret[0].setValue((long)fluxRecord.getValueByKey("_value"));
                            ret[0].setTimestamp(formatted);
                        }
                        Log.d("lastRead", getLastRead().toString());
                        Log.d("actRead", dateTime.toString());
                    }
                    for (FluxRecord fluxRecord : recH) {
                        LocalDateTime dateTime = LocalDateTime.ofInstant(fluxRecord.getTime(), ZoneId.of(ZoneId.systemDefault().toString()));
                        String formatted = DateTimeFormatter.ofPattern("HH:mm:ss").format(dateTime);
                        if(dateTime.isAfter(getLastRead())) {
                            ret[1].setValue((long)fluxRecord.getValueByKey("_value"));
                            ret[1].setTimestamp(formatted);
                        }
                        setLastRead(dateTime);
                    }
                }
            }

        }
        return ret;
    }
    public void closeConnection() {
        if(influxClient!=null)
            influxClient.close();
    }

    public LocalDateTime getLastRead() {
        return lastRead;
    }

    public void setLastRead(LocalDateTime lastRead) {
        this.lastRead = lastRead;
    }
}
