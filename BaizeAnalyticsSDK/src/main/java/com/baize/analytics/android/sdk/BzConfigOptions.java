/*
 * Created by dengshiwei on 2019/03/11.
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

package com.baize.analytics.android.sdk;

import com.baize.analytics.android.sdk.util.ChannelUtils;

public final class BzConfigOptions extends AbstractSAConfigOptions {
    /**
     * 是否设置点击图开关
     */
    boolean mInvokeHeatMapEnabled;

    /**
     * 是否设置点击图对话框
     */
    boolean mInvokeHeatMapConfirmDialog;

    /**
     * 是否设置点击图证书检查
     */
    boolean mInvokeHeatMapSSLCheck;

    /**
     * 是否设置可视化全埋点开关
     */
    boolean mInvokeVisualizedEnabled;

    /**
     * 是否设置可视化全埋点对话框
     */
    boolean mInvokeVisualizedConfirmDialog;

    /**
     * 是否设置点击图证书检查
     */
    boolean mInvokeVisualizedSSLCheck;

    /**
     * 是否设置打印日志
     */
    boolean mInvokeLog;

    /**
     * 私有构造函数
     */
    private BzConfigOptions() {
    }

    /**
     * 获取 SAOptionsConfig 实例
     *
     * @param serverUrl，数据上报服务器地址
     */
    public BzConfigOptions(String serverUrl) {
        this.mServerUrl = serverUrl;
    }

    /**
     * 设置远程配置请求地址
     *
     * @param remoteConfigUrl，远程配置请求地址
     * @return SAOptionsConfig
     */
    public BzConfigOptions setRemoteConfigUrl(String remoteConfigUrl) {
        this.mRemoteConfigUrl = remoteConfigUrl;
        return this;
    }

    /**
     * 设置数据上报地址
     *
     * @param serverUrl，数据上报地址
     * @return SAOptionsConfig
     */
    public BzConfigOptions setServerUrl(String serverUrl) {
        this.mServerUrl = serverUrl;
        return this;
    }

    /**
     * 设置 AutoTrackEvent 的类型，可通过 '|' 进行连接
     *
     * @param autoTrackEventType 开启的 AutoTrack 类型
     * @return SAOptionsConfig
     */
    public BzConfigOptions setAutoTrackEventType(int autoTrackEventType) {
        this.mAutoTrackEventType = autoTrackEventType;
        return this;
    }

    /**
     * 设置是否开启 AppCrash 采集，默认是关闭的
     *
     * @return SAOptionsConfig
     */
    public BzConfigOptions enableTrackAppCrash() {
        this.mEnableTrackAppCrash = true;
        return this;
    }

    /**
     * 设置两次数据发送的最小时间间隔，最小值 5 秒
     *
     * @param flushInterval 时间间隔，单位毫秒
     * @return SAOptionsConfig
     */
    public BzConfigOptions setFlushInterval(int flushInterval) {
        this.mFlushInterval = Math.max(5 * 1000, flushInterval);
        return this;
    }

    /**
     * 设置本地缓存日志的最大条目数
     *
     * @param flushBulkSize 缓存数目
     * @return SAOptionsConfig
     */
    public BzConfigOptions setFlushBulkSize(int flushBulkSize) {
        this.mFlushBulkSize = Math.max(50, flushBulkSize);
        return this;
    }

    /**
     * 设置本地缓存上限值，单位 byte，默认为 32MB：32 * 1024 * 1024，最小 16MB：16 * 1024 * 1024，若小于 16MB，则按 16MB 处理。
     *
     * @param maxCacheSize 单位 byte
     * @return SAOptionsConfig
     */
    public BzConfigOptions setMaxCacheSize(long maxCacheSize) {
        this.mMaxCacheSize = Math.max(16 * 1024 * 1024, maxCacheSize);
        return this;
    }

    /**
     * 设置远程配置请求最小间隔时长
     *
     * @param minRequestInterval 最小时长间隔，单位：小时，默认 24
     * @return SAOptionsConfig
     */
    public BzConfigOptions setMinRequestInterval(int minRequestInterval) {
        this.mMinRequestInterval = minRequestInterval > 0 ? minRequestInterval : mMinRequestInterval;
        return this;
    }

    /**
     * 设置远程配置请求最大间隔时长
     *
     * @param maxRequestInterval 最大时长间隔，单位：小时，默认 48
     * @return SAOptionsConfig
     */
    public BzConfigOptions setMaxRequestInterval(int maxRequestInterval) {
        this.mMaxRequestInterval = maxRequestInterval > 0 ? maxRequestInterval : mMaxRequestInterval;
        return this;
    }

    /**
     * 禁用分散请求远程配置
     *
     * @return SAOptionsConfig
     */
    public BzConfigOptions disableRandomTimeRequestRemoteConfig() {
        this.mDisableRandomTimeRequestRemoteConfig = true;
        return this;
    }

    /**
     * 设置点击图是否可用
     *
     * @param enableHeatMap 点击图是否可用
     * @return SAOptionsConfig
     */
    public BzConfigOptions enableHeatMap(boolean enableHeatMap) {
        this.mHeatMapEnabled = enableHeatMap;
        this.mInvokeHeatMapEnabled = true;
        return this;
    }

    /**
     * 设置点击图提示对话框是否可用
     *
     * @param enableDialog 对话框状态是否可用
     * @return SAOptionsConfig
     */
    public BzConfigOptions enableHeatMapConfirmDialog(boolean enableDialog) {
        this.mHeatMapConfirmDialogEnabled = enableDialog;
        this.mInvokeHeatMapConfirmDialog = true;
        return this;
    }

    /**
     * 设置可视化全埋点是否可用
     *
     * @param enableVisualizedAutoTrack 可视化全埋点是否可用
     * @return SAOptionsConfig
     */
    public BzConfigOptions enableVisualizedAutoTrack(boolean enableVisualizedAutoTrack) {
        this.mVisualizedEnabled = enableVisualizedAutoTrack;
        this.mInvokeVisualizedEnabled = true;
        return this;
    }

    /**
     * 设置可视化全埋点提示对话框是否可用
     *
     * @param enableDialog 对话框状态是否可用
     * @return SAOptionsConfig
     */
    public BzConfigOptions enableVisualizedAutoTrackConfirmDialog(boolean enableDialog) {
        this.mVisualizedConfirmDialogEnabled = enableDialog;
        this.mInvokeVisualizedConfirmDialog = true;
        return this;
    }

    /**
     * 是否打印日志
     *
     * @param enableLog 是否开启打印日志
     * @return SAOptionsConfig
     */
    public BzConfigOptions enableLog(boolean enableLog) {
        this.mLogEnabled = enableLog;
        this.mInvokeLog = true;
        return this;
    }

    /**
     * 是否开启 RN 数据采集
     *
     * @param enableRN 是否开启 RN 采集
     * @return SAOptionsConfig
     */
    public BzConfigOptions enableReactNativeAutoTrack(boolean enableRN) {
        this.mRNAutoTrackEnabled = enableRN;
        return this;
    }

    /**
     * 是否开启屏幕方向采集
     *
     * @param enableScreenOrientation 是否开启屏幕方向采集
     * @return SAOptionsConfig
     */
    public BzConfigOptions enableTrackScreenOrientation(boolean enableScreenOrientation) {
        this.mTrackScreenOrientationEnabled = enableScreenOrientation;
        return this;
    }

    /**
     * 设置数据的网络上传策略
     *
     * @param networkTypePolicy 数据的网络上传策略
     * @return SAOptionsConfig
     */
    public BzConfigOptions setNetworkTypePolicy(int networkTypePolicy) {
        this.mNetworkTypePolicy = networkTypePolicy;
        return this;
    }

    /**
     * 设置匿名 ID
     *
     * @param anonymousId 匿名 ID
     * @return SAOptionsConfig
     */
    public BzConfigOptions setAnonymousId(String anonymousId) {
        this.mAnonymousId = anonymousId;
        return this;
    }

    /**
     * 是否开启多进程
     *
     * @param enableMultiProcess 是否开启多进程
     * @return BzConfigOptions
     */
    public BzConfigOptions enableMultiProcess(boolean enableMultiProcess) {
        this.mEnableMultiProcess = enableMultiProcess;
        return this;
    }

    /**
     * 设置是否保存 utm 属性
     *
     * @param enableSave boolean 默认 false 不保存
     * @return BzConfigOptions
     */
    public BzConfigOptions enableSaveDeepLinkInfo(boolean enableSave) {
        this.mEnableSaveDeepLinkInfo = enableSave;
        return this;
    }

    /**
     * 用户需采集渠道信息自定义属性 key 值，可传多个。
     *
     * @param channels 渠道信息自定义属性 key 值
     * @return BzConfigOptions
     */
    public BzConfigOptions setSourceChannels(String... channels) {
        ChannelUtils.setSourceChannelKeys(channels);
        return this;
    }
}