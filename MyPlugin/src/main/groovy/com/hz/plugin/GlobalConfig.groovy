package com.hz.plugin


@Singleton
public class GlobalConfig {

    private String autoTrackBase = ""

    private boolean isOpenAutoTrack = true

    String getAutoTrackBase() {
        return autoTrackBase
    }

    void setAutoTrackBase(String autoTrackBase) {
        this.autoTrackBase = autoTrackBase
    }

    boolean getIsOpenAutoTrack() {
        return isOpenAutoTrack
    }

    void setIsOpenAutoTrack(boolean isOpenAutoTrack) {
        this.isOpenAutoTrack = isOpenAutoTrack
    }
}