package com.github.engatec.vdl.model.preferences.youtubedl;

import java.util.prefs.Preferences;

import org.apache.commons.lang3.StringUtils;

public class ProxyUrlConfigItem extends YoutubeDlConfigItem<String> {

    @Override
    protected String getName() {
        return "proxyUrl";
    }

    @Override
    public String getValue(Preferences prefs) {
        return prefs.get(getKey(), StringUtils.EMPTY);
    }

    @Override
    public void setValue(Preferences prefs, String value) {
        prefs.put(getKey(), value);
    }
}
