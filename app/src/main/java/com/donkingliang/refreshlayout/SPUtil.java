package com.donkingliang.refreshlayout;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Depiction: 用于储蓄列表的最后刷新时间
 * Author:lry
 * Date: 2018/8/17
 */
class SPUtil {

    private static volatile SharedPreferences mSharedPreferences;

    public static synchronized SharedPreferences initSharedPreferences(Context context) {
        if (mSharedPreferences == null) {
            mSharedPreferences = context.getSharedPreferences("refresh_time", Context.MODE_PRIVATE);
        }
        return mSharedPreferences;
    }

    /**
     * 对全局变量指定写入一个long值.
     *
     * @param key   KEY
     * @param value 值
     */
    public static void writeRefreshTime(final String key, final long value) {
        final SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    public static Long getRefreshTime(final String key) {
        return mSharedPreferences.getLong(key, 0);
    }
}
