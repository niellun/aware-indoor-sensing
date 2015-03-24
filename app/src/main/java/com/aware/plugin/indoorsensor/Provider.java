package com.aware.plugin.indoorsensor;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.util.Log;

import com.aware.Aware;
import com.aware.utils.DatabaseHelper;

public class Provider extends ContentProvider {

    public static final int DATABASE_VERSION = 10;

    /**
     * Provider authority: com.aware.plugin.lux_meter.provider.lux_meter
     */

    public static String AUTHORITY = "com.aware.plugin.indoorsensor.provider.indoorsensor";

    public static final String DATABASE_NAME = Environment.getExternalStorageDirectory() + "/AWARE/plugin_indoorsensor.db";

    private static final int INDOORSENSOR = 1;
    private static final int INDOORSENSOR_ID = 2;

    public static final String[] DATABASE_TABLES = {
            "plugin_indoorsensor"
    };

    public static final String[] TABLES_FIELDS = {
            Indoorsensor_Data._ID + " integer primary key autoincrement," +
                    Indoorsensor_Data.TIMESTAMP + " real default 0," +
                    Indoorsensor_Data.DEVICE_ID + " text default ''," +
                    Indoorsensor_Data.VALUE + " integer default -1," +
                    Indoorsensor_Data.REAL_VALUE + " integer default -1," +
                    Indoorsensor_Data.DIFF + " integer default 0," +
                    Indoorsensor_Data.SNR + " real default 0," +
                    Indoorsensor_Data.SAT + " real default 0," +
                    Indoorsensor_Data.LVL + " real default 0," +
                    "UNIQUE("+Indoorsensor_Data.TIMESTAMP+","+Indoorsensor_Data.DEVICE_ID+")"
    };

    public static final class Indoorsensor_Data implements BaseColumns {
        private Indoorsensor_Data(){};

        public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/plugin_indoorsensor");
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.aware.plugin.indoorsensor";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.aware.plugin.indoorsensor";

        public static final String _ID = "id";
        public static final String TIMESTAMP = "timestamp";
        public static final String DEVICE_ID = "device_id";
        public static final String VALUE = "value";
        public static final String REAL_VALUE = "real_vlue";
        public static final String DIFF = "diff";
        public static final String SNR = "snr";
        public static final String SAT = "sat";
        public static final String LVL = "lvl";
    }

    private static UriMatcher URIMatcher;
    private static HashMap<String, String> databaseMap;
    private static DatabaseHelper databaseHelper;
    private static SQLiteDatabase database;

    @Override
    public boolean onCreate() {

        AUTHORITY = getContext().getPackageName() + ".provider.indoorsensor";

        URIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        URIMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], INDOORSENSOR);
        URIMatcher.addURI(AUTHORITY, DATABASE_TABLES[0]+"/#", INDOORSENSOR_ID);

        databaseMap = new HashMap<String, String>();
        databaseMap.put(Indoorsensor_Data._ID, Indoorsensor_Data._ID);
        databaseMap.put(Indoorsensor_Data.TIMESTAMP, Indoorsensor_Data.TIMESTAMP);
        databaseMap.put(Indoorsensor_Data.DEVICE_ID, Indoorsensor_Data.DEVICE_ID);
        databaseMap.put(Indoorsensor_Data.VALUE, Indoorsensor_Data.VALUE);
        databaseMap.put(Indoorsensor_Data.REAL_VALUE, Indoorsensor_Data.REAL_VALUE);
        databaseMap.put(Indoorsensor_Data.DIFF, Indoorsensor_Data.DIFF);
        databaseMap.put(Indoorsensor_Data.SNR, Indoorsensor_Data.SNR);
        databaseMap.put(Indoorsensor_Data.SAT, Indoorsensor_Data.SAT);
        databaseMap.put(Indoorsensor_Data.LVL, Indoorsensor_Data.LVL);

        return true;
    }

    private boolean initializeDB() {
        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS );
        }
        if( databaseHelper != null && ( database == null || ! database.isOpen() )) {
            database = databaseHelper.getWritableDatabase();
        }
        return( database != null && databaseHelper != null);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return 0;
        }

        int count = 0;
        switch (URIMatcher.match(uri)) {
            case INDOORSENSOR:
                count = database.delete(DATABASE_TABLES[0], selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (URIMatcher.match(uri)) {
            case INDOORSENSOR:
                return Indoorsensor_Data.CONTENT_TYPE;
            case INDOORSENSOR_ID:
                return Indoorsensor_Data.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return null;
        }

        ContentValues values = (initialValues != null) ? new ContentValues(
                initialValues) : new ContentValues();

        switch (URIMatcher.match(uri)) {
            case INDOORSENSOR:
                long weather_id = database.insert(DATABASE_TABLES[0], Indoorsensor_Data.DEVICE_ID, values);

                if (weather_id > 0) {
                    Uri new_uri = ContentUris.withAppendedId(
                            Indoorsensor_Data.CONTENT_URI,
                            weather_id);
                    getContext().getContentResolver().notifyChange(new_uri,
                            null);
                    return new_uri;
                }
                throw new SQLException("Failed to insert row into " + uri);
            default:
                throw new IllegalArgumentException("Unknown URI "+URIMatcher.match(uri) + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return null;
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (URIMatcher.match(uri)) {
            case INDOORSENSOR:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(databaseMap);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs,
                    null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            if (Aware.DEBUG)
                Log.e(Aware.TAG, e.getMessage());

            return null;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return 0;
        }

        int count = 0;
        switch (URIMatcher.match(uri)) {
            case INDOORSENSOR:
                count = database.update(DATABASE_TABLES[0], values, selection,
                        selectionArgs);
                break;
            default:

                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}