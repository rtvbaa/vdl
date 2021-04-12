package com.github.engatec.vdl.model.preferences.youtubedl;

import java.util.prefs.Preferences;

public class NoPartConfigItem extends YoutubeDlConfigItem<Boolean> {

    @Override
    protected String getName() {
        return "noPart";
    }

    @Override
    public Boolean getValue(Preferences prefs) {
        return prefs.getBoolean(getKey(), false);
    }

    @Override
    public void setValue(Preferences prefs, Boolean value) {
        prefs.putBoolean(getKey(), value);
    }
}
