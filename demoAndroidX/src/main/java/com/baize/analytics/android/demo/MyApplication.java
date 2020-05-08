/*
 * Created by wangzhuozhou on 2016/11/12.
 * Copyright 2015－2020 Hong Yu Inc.
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
import com.baize.analytics.android.sdk.BzConfigOptions;

import java.util.ArrayList;
import java.util.List;

public class MyApplication extends Application {
    /**
     * 采集数据的地址
     */
    private final static String SERVER_URL = "http://192.168.1.177:8432/logger/bz";

    @Override
    public void onCreate() {
        super.onCreate();
        initBaizeAPI();
    }

    /**
     * 初始化 SDK
     */
    private void initBaizeAPI() {
        BaizeAPI.sharedInstance(this, new BzConfigOptions(SERVER_URL));
        BaizeAPI.sharedInstance(this).setAppId("3");
        // 打开自动采集, 并指定追踪哪些 AutoTrack 事件
        List<BaizeAPI.AutoTrackEventType> eventTypeList = new ArrayList<>();
        eventTypeList.add(BaizeAPI.AutoTrackEventType.APP_START);
        eventTypeList.add(BaizeAPI.AutoTrackEventType.APP_END);
        eventTypeList.add(BaizeAPI.AutoTrackEventType.APP_VIEW_SCREEN);
        eventTypeList.add(BaizeAPI.AutoTrackEventType.APP_CLICK);
        BaizeAPI.sharedInstance(this).enableAutoTrack(eventTypeList);
        BaizeAPI.sharedInstance(this).trackFragmentAppViewScreen();
        BaizeAPI.sharedInstance(this).trackAppCrash();

        BaizeAPI.sharedInstance().login("91");
    }

}
