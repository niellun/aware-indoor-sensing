package com.aware.plugin.indoorsensor;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by niellune on 18.03.15.
 */
public class Leveler {

    public static final String APP_PREFS_NAME = "com.aware.plugin.indoorsensor.settings";

    private static final String LEVEL_VALUE = "level";

    private SharedPreferences _settings;

    private SharedPreferences.Editor _editor;

    private float _value;

    public Leveler(Context context) {
        _settings = context.getSharedPreferences(APP_PREFS_NAME, 0);
        _editor = _settings.edit();
        _value = _settings.getFloat(LEVEL_VALUE, -1);
        if (_value < 0)
            SetValue(25);
        Log.d("leveler", "Created with value "+_value);
    }

    public void SetValue(float value) {
        _value = value;
        _editor.putFloat(LEVEL_VALUE, _value);
        _editor.apply();
        Log.d("leveler", "New value is "+_value);
    }

    public float GetValue() {
        return _value;
    }

    public void Adjust(float value) {
        float res = _value+value;
        if(res<30 && res >20 ) {
            SetValue(_value + value);
        }
    }
}
