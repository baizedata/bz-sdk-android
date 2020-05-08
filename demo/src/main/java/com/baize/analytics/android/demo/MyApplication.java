/*
 * Created by wangzhuozhou on 2016/11/12.
 * Copyright 2015－2020 Sensors Data Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baize.analytics.android.demo;

import android.app.Application;

import com.baize.analytics.android.sdk.BaizeAPI;
import com.baize.analytics.android.sdk.BaizeAnalyticsAutoTrackEventType;
import com.baize.analytics.android.sdk.BzConfigOptions;

public class MyApplication extends Application {
    /**
     * Baize采集数据的地址
     */
    private final static String SERVER_URL = "http://olomobi.com/logger/bz";

    @Override
    public void onCreate() {
        super.onCreate();
        initBaizeAPI();
    }

    /**
     * 初始化 Baize SDK
     */
    private void initBaizeAPI() {
        BzConfigOptions configOptions = new BzConfigOptions(SERVER_URL);
        // 打开自动采集, 并指定追踪哪些 AutoTrack 事件
        configOptions.setAutoTrackEventType(BaizeAnalyticsAutoTrackEventType.APP_START |
                BaizeAnalyticsAutoTrackEventType.APP_END |
                BaizeAnalyticsAutoTrackEventType.APP_VIEW_SCREEN |
                BaizeAnalyticsAutoTrackEventType.APP_CLICK);
        // 打开 crash 信息采集
        configOptions.enableTrackAppCrash();
        //传入 BzConfigOptions 对象，初始化神策 SDK
        BaizeAPI.sharedInstance(this, configOptions);
        BaizeAPI.sharedInstance(this).setAppId("3");
    }
}
