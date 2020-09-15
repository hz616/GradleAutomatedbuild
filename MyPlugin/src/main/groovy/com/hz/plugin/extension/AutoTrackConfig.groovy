package com.hz.plugin.extension


public class AutoTrackConfig {

    /**
     * 名称
     */
    String name = "自动埋点工具"
    /**
     * 需要指定插入代码的全路径
     */
    String hookMethod
    /**
     * 是否打卡自动化埋点
     */
    boolean isOpenAutoTrack = true
    /**
     * 用于日志的打印
     */
    boolean isDebug = true
}