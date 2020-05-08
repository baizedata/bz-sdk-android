package com.baize.analytics.android.sdk.util;

import android.Manifest;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import com.baize.analytics.android.sdk.BaizeAPI;
import com.baize.analytics.android.sdk.BzConfigOptions;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class BzDeviceUtilsTest {
    @Rule
    public GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_PHONE_STATE);

    @Before
    public void initBaizeDataAPI() {
        Context context = ApplicationProvider.getApplicationContext();
        BaizeAPI.sharedInstance(context, new BzConfigOptions("").enableLog(true));
    }

    /**
     * 需集成 oaid 的 aar 包
     */
    @Test
    public void getOAID() {
        try {
            String oaid = BzDeviceUtils.getOAID(ApplicationProvider.getApplicationContext());
            assertNull(oaid);
            BaizeAPI.sharedInstance().trackInstallation("AppInstall");
        } catch (Exception ex) {
            //ignore
        }
    }
}
