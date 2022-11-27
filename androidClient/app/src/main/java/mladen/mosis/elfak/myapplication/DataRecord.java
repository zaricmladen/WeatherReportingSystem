package mladen.mosis.elfak.myapplication;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class DataRecord implements Parcelable {
    private long value;
    private String timestamp;

    public DataRecord() {

    }

    public DataRecord(long val, String ts) {
        this.value=val;
        this.timestamp=ts;
    }

    protected DataRecord(Parcel in) {
        value = in.readLong();
        timestamp = in.readString();
    }

    public static final Creator<DataRecord> CREATOR = new Creator<DataRecord>() {
        @Override
        public DataRecord createFromParcel(Parcel in) {
            return new DataRecord(in);
        }

        @Override
        public DataRecord[] newArray(int size) {
            return new DataRecord[size];
        }
    };

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public String getTimestamp() {
        return timestamp;
    }
    public String getFormattedTimestamp() {
        return timestamp.substring(0,5);
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeLong(value);
        parcel.writeString(timestamp);
    }
}
