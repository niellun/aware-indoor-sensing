package com.aware.plugin.indoorsensor;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ui.Stream_UI;
import com.aware.utils.Aware_Plugin;

import java.util.Timer;

import static com.aware.plugin.indoorsensor.Provider.Indoorsensor_Data.CONTENT_URI;
import static com.aware.plugin.indoorsensor.Provider.Indoorsensor_Data.DEVICE_ID;
import static com.aware.plugin.indoorsensor.Provider.Indoorsensor_Data.DIFF;
import static com.aware.plugin.indoorsensor.Provider.Indoorsensor_Data.LVL;
import static com.aware.plugin.indoorsensor.Provider.Indoorsensor_Data.REAL_VALUE;
import static com.aware.plugin.indoorsensor.Provider.Indoorsensor_Data.SAT;
import static com.aware.plugin.indoorsensor.Provider.Indoorsensor_Data.SNR;
import static com.aware.plugin.indoorsensor.Provider.Indoorsensor_Data.TIMESTAMP;
import static com.aware.plugin.indoorsensor.Provider.Indoorsensor_Data.VALUE;

public class Plugin extends Aware_Plugin {

    public static final String ACTION_DATA_REFRESH = "com.aware.plugin.indoorsensor.broadcast";

    private Leveler _leveler;

    @Override
    public void onCreate() {
        super.onCreate();
        if( DEBUG ) Log.d(TAG, "Plugin started");

        //Initialize our plugin's settings
        if( Aware.getSetting(this, Settings.STATUS_PLUGIN_TEMPLATE).length() == 0 ) {
            Aware.setSetting(this, Settings.STATUS_PLUGIN_TEMPLATE, true);
        }

        //Activate any sensors/plugins you need here
        _leveler = new Leveler(getApplicationContext());
        Timer tmr = new Timer("tmr1");
        tmr.schedule(new LocationTask(this, _leveler), 0, 8000);

        DbWriter writer = new DbWriter(getApplicationContext());
        //Tell Android that you'll monitor the stream statuses
        IntentFilter filter = new IntentFilter();
        filter.addAction(Plugin.ACTION_DATA_REFRESH);
        filter.addAction("switch_status");
        registerReceiver(writer, filter);


        //Any active plugin/sensor shares its overall context using broadcasts
        CONTEXT_PRODUCER = new ContextProducer() {
            @Override
            public void onContext() {
                //Broadcast your context here
            }
        };

        //To sync data to the server, you'll need to set this variables from your ContentProvider
        //DATABASE_TABLES =
        //TABLES_FIELDS =
        //CONTEXT_URIS = new Uri[]{ }

        DATABASE_TABLES = Provider.DATABASE_TABLES;
        TABLES_FIELDS = Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{ Provider.Indoorsensor_Data.CONTENT_URI };

        //Ask AWARE to apply your settings
        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //This function gets called every 5 minutes by AWARE to make sure this plugin is still running.
        TAG = "IndoorSensor";
        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if( DEBUG ) Log.d(TAG, "Plugin terminated");
        Aware.setSetting(this, Settings.STATUS_PLUGIN_TEMPLATE, false);

        //Deactivate any sensors/plugins you activated here
        //...

        //Ask AWARE to apply your settings
        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));
    }
}

class DbWriter extends BroadcastReceiver
{
    private int rres = 0;

    DbWriter(Context context) {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if( intent.getAction().equals("switch_status")){
            rres = intent.getIntExtra("value", -1);

            Log.d("switch", rres+" c");
        }

        if( intent.getAction().equals(Plugin.ACTION_DATA_REFRESH) ) {
            Bundle b = intent.getExtras();
            float sat = b.getFloat("sat");
            float snr = b.getFloat("snr");
            int val = b.getInt("value");
            int stat = b.getInt("stat");
            int res = -1;
            if(val == 1)
            {
                res = 1;
            }
            if(val == 2)
            {
                res = 2;
            }
            if((stat==1 || stat==2) && stat!=val)
            {
                res = 3;
            }

            int dif = 0;
            if(res!=3 && res!=rres)
            {
                dif = 1;
            }

            ContentValues data = new ContentValues();
            data.put(TIMESTAMP, System.currentTimeMillis());
            data.put(DEVICE_ID, Aware.getSetting(context, Aware_Preferences.DEVICE_ID));
            data.put(VALUE, res);
            data.put(REAL_VALUE, rres);
            data.put(DIFF, dif);
            data.put(SAT, sat);
            data.put(SNR, snr);
            data.put(LVL, b.getFloat("level"));
            context.getContentResolver().insert(CONTENT_URI, data);

            Log.d("switch", rres+" x");
        }
    }
}
