package com.aware.plugin.indoorsensor;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ui.Stream_UI;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.IContextCard;

import static com.aware.plugin.indoorsensor.Provider.Indoorsensor_Data.*;

public class ContextCard extends BroadcastReceiver implements IContextCard {

    private boolean Active = false;

    private static String _text;

    //You may use sContext on uiChanger to do queries to databases, etc.
    private Context sContext;

    //Declare here all the UI elements you'll be accessing
    private View card;
    private TextView counter_txt;

    //Used to load your context card
    private LayoutInflater sInflater;

    //Empty constructor used to instantiate this card
    public ContextCard(){}

    @Override
    public void onReceive(Context context, Intent intent) {
        if( intent.getAction().equals(Stream_UI.ACTION_AWARE_STREAM_OPEN) ) {
            Log.d("test", "Stream open");
            Active = true;
        }
        if( intent.getAction().equals(Stream_UI.ACTION_AWARE_STREAM_CLOSED) ) {
            Log.d("test", "Stream closed");
            Active = false;
        }
        if( intent.getAction().equals(Plugin.ACTION_DATA_REFRESH) ) {
            Log.d("test", "Data");
            Bundle b = intent.getExtras();
            float sat = b.getFloat("sat");
            float snr = b.getFloat("snr");
            int val = b.getInt("value");
            int stat = b.getInt("stat");
            String txt = "STATUS: UNKNOWN";
            if(val == 1)
            {
                txt = "STATUS: INDOORS";
            }
            if(val == 2)
            {
                txt = "STATUS: OUTDOORS";
            }
            if((stat==1 || stat==2) && stat!=val)
            {
                txt = "STATUS: TRANSITION";
            }

            SharedPreferences settings = sContext.getSharedPreferences(Leveler.APP_PREFS_NAME, 0);
            boolean indoors = settings.getBoolean("indoors_status", false);

            _text = txt+ "\r\n\r\nSAT "+sat+" SNR "+snr+"\r\n"+"STAT "+stat+" RES "+val+"\r\nLVL "+b.getFloat("level")+"\r\n SWTCH "+indoors;
            counter_txt.setText(_text);
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public View getContextCard(Context context) {
        sContext = context;

        //Tell Android that you'll monitor the stream statuses
        IntentFilter filter = new IntentFilter();
        filter.addAction(Stream_UI.ACTION_AWARE_STREAM_OPEN);
        filter.addAction(Stream_UI.ACTION_AWARE_STREAM_CLOSED);
        filter.addAction(Plugin.ACTION_DATA_REFRESH);
        context.registerReceiver(this, filter);

        //Load card information to memory
        sInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        card = sInflater.inflate(R.layout.card, null);

        //Initialize UI elements from the card
        counter_txt = (TextView) card.findViewById(R.id.counter);
        counter_txt.setText(_text);

        SharedPreferences settings = sContext.getSharedPreferences(Leveler.APP_PREFS_NAME, 0);
        boolean indoors = settings.getBoolean("indoors_status", false);
        Intent intent = new Intent("switch_status");
        intent.putExtra("value", indoors?1:2);
        sContext.sendBroadcast(intent);

        Switch swtch = (Switch) card.findViewById(R.id.switch1);
        swtch.setChecked(indoors);
        swtch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences settings = sContext.getSharedPreferences(Leveler.APP_PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("indoors_status", isChecked);
                editor.apply();
                Intent intent = new Intent("switch_status");
                intent.putExtra("value", isChecked?1:2);
                sContext.sendBroadcast(intent);
            }
        });

        //Return the card to AWARE/apps
        return card;
    }
}
