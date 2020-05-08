/*
 * Created by dengshiwei on 2019/09/12.
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
package com.baize.analytics.android.sdk.internal;

import android.content.Context;
import android.text.TextUtils;

import com.baize.analytics.android.sdk.util.BaizeUtils;
import com.baize.analytics.android.sdk.BzLog;
import com.baize.analytics.android.sdk.BaizeIgnoreTrackAppViewScreen;
import com.baize.analytics.android.sdk.BaizeIgnoreTrackAppViewScreenAndAppClick;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class FragmentAPI implements IFragmentAPI {
    /* $AppViewScreen 事件是否支持 Fragment*/
    private boolean mTrackFragmentAppViewScreen;
    private Set<Integer> mAutoTrackFragments;
    private Set<Integer> mAutoTrackIgnoredFragments;

    public FragmentAPI(Context context) {
        ArrayList<String> autoTrackFragments = BaizeUtils.getAutoTrackFragments(context);
        if (autoTrackFragments.size() > 0) {
            mAutoTrackFragments = new CopyOnWriteArraySet<>();
            for (String fragment : autoTrackFragments) {
                mAutoTrackFragments.add(fragment.hashCode());
            }
        }
    }

    @Override
    public void trackFragmentAppViewScreen() {
        this.mTrackFragmentAppViewScreen = true;
    }

    @Override
    public boolean isTrackFragmentAppViewScreenEnabled() {
        return this.mTrackFragmentAppViewScreen;
    }

    @Override
    public void enableAutoTrackFragment(Class<?> fragment) {
        try {
            if (fragment == null) {
                return;
            }

            if (mAutoTrackFragments == null) {
                mAutoTrackFragments = new CopyOnWriteArraySet<>();
            }

            String canonicalName = fragment.getCanonicalName();
            if (!TextUtils.isEmpty(canonicalName)) {
                mAutoTrackFragments.add(canonicalName.hashCode());
            }
        } catch (Exception ex) {
            BzLog.printStackTrace(ex);
        }
    }

    @Override
    public void enableAutoTrackFragments(List<Class<?>> fragmentsList) {
        if (fragmentsList == null || fragmentsList.size() == 0) {
            return;
        }

        if (mAutoTrackFragments == null) {
            mAutoTrackFragments = new CopyOnWriteArraySet<>();
        }

        try {
            String canonicalName;
            for (Class fragment : fragmentsList) {
                canonicalName = fragment.getCanonicalName();
                if (!TextUtils.isEmpty(canonicalName)) {
                    mAutoTrackFragments.add(canonicalName.hashCode());
                }
            }
        } catch (Exception e) {
            BzLog.printStackTrace(e);
        }
    }

    @Override
    public boolean isFragmentAutoTrackAppViewScreen(Class<?> fragment) {
        if (fragment == null) {
            return false;
        }
        try {
            if (mAutoTrackFragments != null && mAutoTrackFragments.size() > 0) {
                String canonicalName = fragment.getCanonicalName();
                if (!TextUtils.isEmpty(canonicalName)) {
                    return mAutoTrackFragments.contains(canonicalName.hashCode());
                }
            }

            if (fragment.getAnnotation(BaizeIgnoreTrackAppViewScreen.class) != null) {
                return false;
            }

            if (fragment.getAnnotation(BaizeIgnoreTrackAppViewScreenAndAppClick.class) != null) {
                return false;
            }

            if (mAutoTrackIgnoredFragments != null && mAutoTrackIgnoredFragments.size() > 0) {
                String canonicalName = fragment.getCanonicalName();
                if (!TextUtils.isEmpty(canonicalName)) {
                    return !mAutoTrackIgnoredFragments.contains(canonicalName.hashCode());
                }
            }
        } catch (Exception e) {
            BzLog.printStackTrace(e);
        }

        return true;
    }

    @Override
    public void ignoreAutoTrackFragments(List<Class<?>> fragmentList) {
        try {
            if (fragmentList == null || fragmentList.size() == 0) {
                return;
            }

            if (mAutoTrackIgnoredFragments == null) {
                mAutoTrackIgnoredFragments = new CopyOnWriteArraySet<>();
            }

            for (Class<?> fragment : fragmentList) {
                if (fragment != null) {
                    String canonicalName = fragment.getCanonicalName();
                    if (!TextUtils.isEmpty(canonicalName)) {
                        mAutoTrackIgnoredFragments.add(canonicalName.hashCode());
                    }
                }
            }
        } catch (Exception ex) {
            BzLog.printStackTrace(ex);
        }
    }

    @Override
    public void ignoreAutoTrackFragment(Class<?> fragment) {
        try {
            if (fragment == null) {
                return;
            }

            if (mAutoTrackIgnoredFragments == null) {
                mAutoTrackIgnoredFragments = new CopyOnWriteArraySet<>();
            }

            String canonicalName = fragment.getCanonicalName();
            if (!TextUtils.isEmpty(canonicalName)) {
                mAutoTrackIgnoredFragments.add(canonicalName.hashCode());
            }
        } catch (Exception ex) {
            BzLog.printStackTrace(ex);
        }
    }

    @Override
    public void resumeIgnoredAutoTrackFragments(List<Class<?>> fragmentList) {
        try {
            if (fragmentList == null || fragmentList.size() == 0 ||
                    mAutoTrackIgnoredFragments == null) {
                return;
            }

            for (Class fragment : fragmentList) {
                if (fragment != null) {
                    String canonicalName = fragment.getCanonicalName();
                    if (!TextUtils.isEmpty(canonicalName)) {
                        mAutoTrackIgnoredFragments.remove(canonicalName.hashCode());
                    }
                }
            }
        } catch (Exception ex) {
            BzLog.printStackTrace(ex);
        }
    }

    @Override
    public void resumeIgnoredAutoTrackFragment(Class<?> fragment) {
        try {
            if (fragment == null || mAutoTrackIgnoredFragments == null) {
                return;
            }

            String canonicalName = fragment.getCanonicalName();
            if (!TextUtils.isEmpty(canonicalName)) {
                mAutoTrackIgnoredFragments.remove(canonicalName.hashCode());
            }
        } catch (Exception ex) {
            BzLog.printStackTrace(ex);
        }
    }
}
