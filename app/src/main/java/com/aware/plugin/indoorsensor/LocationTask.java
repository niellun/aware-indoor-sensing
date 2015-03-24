package com.aware.plugin.indoorsensor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import java.util.Iterator;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;

/**
 * Created by niellune on 18.03.15.
 */

public class LocationTask extends TimerTask implements GpsStatus.Listener, LocationListener {

    private final Leveler _leveler;
    private Plugin _plugin;
    private LocationManager _locationManager;
    private volatile boolean _locker;

    private float _snrSum;
    private float _satSum;
    private float _count;
    private int _warmUp;

    private float _curSnr;
    private float _curCount;
    private int _prevStatus;
    private int _curStatus;
    private int _state;

    public LocationTask(Plugin plugin, Leveler leveler) {
        _plugin = plugin;
        _leveler = leveler;

        _locationManager = (LocationManager) plugin.getSystemService(Context.LOCATION_SERVICE);
        _locationManager.addGpsStatusListener(this);
        _locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, this);

        _state = 0;
        _curCount = 0;
        _curSnr = 0;
        _prevStatus = 0;
        _curStatus = 0;
    }

    public void Destroy() {
        _locationManager.removeUpdates(this);
    }

    @Override
    public void run() {
        synchronized (this) {
            _locker = true;
        }

        float cnt = _count == 0 ? 0 : _satSum / _count;
        float snr = _count == 0 ? 0 : _snrSum / _count;
        _count = 0;
        _satSum = 0;
        _snrSum = 0;
        _locker = false;

        if(cnt<3)
        {
            snr = snr/3;
        }

        int newStatus = snr > _leveler.GetValue() ? 2 : 1;

        if(newStatus != _curStatus)
        {
            if(_curStatus == 1 || _curStatus==2)
            {
                // Change of status -> go to transition
                newStatus = 3;
            }

            if(_curStatus == 3 && newStatus == _prevStatus)
            {
                // Make previous transitional same as statuses;
                _curStatus = newStatus;
                if(Math.abs(_curSnr - _leveler.GetValue()) < 2.0)
                {
                    if(_curStatus == 1)
                        _leveler.Adjust((float) 0.5);
                    if(_curStatus == 2)
                        _leveler.Adjust((float) -0.5);
                }
            }
        }

        if(_curStatus == _prevStatus && _curStatus==newStatus && Math.abs(_curSnr - _leveler.GetValue()) < 2.0)
        {
            if(_curStatus == 1)
                _leveler.Adjust((float) 0.1);
            if(_curStatus == 2)
                _leveler.Adjust((float) -0.1);
        }

        if(_curStatus == newStatus && _curStatus!=3 && _curStatus!=0)
        {
            _state = _curStatus;
        }

        _prevStatus = _curStatus;
        _curStatus = newStatus;
        _curSnr = snr;
        _curCount = cnt;

        Intent intent = new Intent();
        intent.setAction(Plugin.ACTION_DATA_REFRESH);
        intent.putExtra("sat", (float) (((int) (100.0 * cnt)) / 100.0));
        intent.putExtra("snr", (float)(((int) (100.0 * snr)) / 100.0));
        intent.putExtra("stat", _curStatus);
        intent.putExtra("value", _state);
        intent.putExtra("level", _leveler.GetValue());
        _plugin.sendBroadcast(intent);
    }

    @Override
    public void onGpsStatusChanged(int event) {
        int count = 0;
        float strength = 0;

        float strengthTop = 0;
        int countTop = 0;

        GpsStatus gpsStatus = _locationManager.getGpsStatus(null);
        if (gpsStatus != null) {
            Iterable<GpsSatellite> satellites = gpsStatus.getSatellites();
            Iterator<GpsSatellite> sat = satellites.iterator();
            while (sat.hasNext()) {
                strength += sat.next().getSnr();
                count++;
            }

            Log.d("location 1", count+"  "+strength / count);

            Iterator<GpsSatellite> sat2 = satellites.iterator();
            while (sat2.hasNext()) {
                float str = sat2.next().getSnr();
                if (str > strength / count) {
                    countTop++;
                    strengthTop += str;
                }
            }
        }

        synchronized (this) {
            if (!_locker) {
                _snrSum += strengthTop / countTop;
                _satSum += countTop;
                _count++;
                Log.d("location 2", countTop+"  "+strengthTop / countTop);
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
