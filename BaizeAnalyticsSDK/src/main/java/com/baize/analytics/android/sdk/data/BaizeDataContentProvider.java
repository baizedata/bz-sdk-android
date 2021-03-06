/*
 * Created by wangzhuozhou on 2017/5/5.
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

package com.baize.analytics.android.sdk.data;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;

import com.baize.analytics.android.sdk.BzLog;
import com.baize.analytics.android.sdk.data.persistent.PersistentAppEndData;
import com.baize.analytics.android.sdk.data.persistent.PersistentAppPaused;
import com.baize.analytics.android.sdk.data.persistent.PersistentAppStartTime;
import com.baize.analytics.android.sdk.data.persistent.PersistentLoginId;
import com.baize.analytics.android.sdk.data.persistent.PersistentSessionIntervalTime;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

public class BaizeDataContentProvider extends ContentProvider {
    private final static int EVENTS = 1;
    private final static int ACTIVITY_START_COUNT = 2;
    private final static int APP_START_TIME = 3;
    private final static int APP_END_DATA = 4;
    private final static int APP_PAUSED_TIME = 5;
    private final static int SESSION_INTERVAL_TIME = 6;
    private final static int LOGIN_ID = 7;
    private static UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private BaizeDBHelper dbHelper;
    private ContentResolver contentResolver;
    private PersistentAppStartTime persistentAppStartTime;
    private PersistentAppEndData persistentAppEndData;
    private PersistentAppPaused persistentAppPaused;
    private PersistentSessionIntervalTime persistentSessionIntervalTime;
    private PersistentLoginId persistentLoginId;

    private boolean isDbWritable = true;
    private int startActivityCount = 0;

    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context != null) {
            //这里是为了使用 ProviderTestRule
            String packageName;
            try {
                packageName = context.getApplicationContext().getPackageName();
            } catch (UnsupportedOperationException e) {
                packageName = "com.baize.analytics.android.sdk.test";
            }
            String authority = packageName + ".BaizeDataContentProvider";
            contentResolver = context.getContentResolver();
            uriMatcher.addURI(authority, DbParams.TABLE_EVENTS, EVENTS);
            uriMatcher.addURI(authority, DbParams.TABLE_ACTIVITY_START_COUNT, ACTIVITY_START_COUNT);
            uriMatcher.addURI(authority, DbParams.TABLE_APP_START_TIME, APP_START_TIME);
            uriMatcher.addURI(authority, DbParams.TABLE_APP_END_DATA, APP_END_DATA);
            uriMatcher.addURI(authority, DbParams.TABLE_APP_END_TIME, APP_PAUSED_TIME);
            uriMatcher.addURI(authority, DbParams.TABLE_SESSION_INTERVAL_TIME, SESSION_INTERVAL_TIME);
            uriMatcher.addURI(authority, DbParams.TABLE_LOGIN_ID, LOGIN_ID);
            dbHelper = new BaizeDBHelper(context);

            /* 迁移数据，并删除老的数据库 */
            try {
                File oldDatabase = context.getDatabasePath(packageName);
                if (oldDatabase.exists()) {
                    OldBDatabaseHelper oldBDatabaseHelper = new OldBDatabaseHelper(context, packageName);

                    JSONArray oldEvents = oldBDatabaseHelper.getAllEvents();
                    for (int i = 0; i < oldEvents.length(); i++) {
                        JSONObject jsonObject = oldEvents.getJSONObject(i);
                        final ContentValues cv = new ContentValues();
                        cv.put(DbParams.KEY_DATA, jsonObject.getString(DbParams.KEY_DATA));
                        cv.put(DbParams.KEY_CREATED_AT, jsonObject.getString(DbParams.KEY_CREATED_AT));

                        try {
                            SQLiteDatabase database = dbHelper.getWritableDatabase();
                            database.insert(DbParams.TABLE_EVENTS, "_id", cv);
                        } catch (SQLiteException e) {
                            isDbWritable = false;
                            BzLog.printStackTrace(e);
                        }
                    }
                }
                if (isDbWritable) {
                    context.deleteDatabase(packageName);
                }
            } catch (Exception e) {
                BzLog.printStackTrace(e);
            }
            PersistentLoader.initLoader(context);
            persistentAppEndData = (PersistentAppEndData) PersistentLoader.loadPersistent(DbParams.TABLE_APP_END_DATA);
            persistentAppStartTime = (PersistentAppStartTime) PersistentLoader.loadPersistent(DbParams.TABLE_APP_START_TIME);
            persistentAppPaused = (PersistentAppPaused) PersistentLoader.loadPersistent(DbParams.TABLE_APP_END_TIME);
            persistentSessionIntervalTime = (PersistentSessionIntervalTime) PersistentLoader.loadPersistent(DbParams.TABLE_SESSION_INTERVAL_TIME);
            persistentLoginId = (PersistentLoginId) PersistentLoader.loadPersistent(DbParams.TABLE_LOGIN_ID);
        }
        return true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (!isDbWritable) {
            return 0;
        }
        int deletedCounts = 0;
        try {
            int code = uriMatcher.match(uri);
            if (EVENTS == code) {
                try {
                    SQLiteDatabase database = dbHelper.getWritableDatabase();
                    deletedCounts = database.delete(DbParams.TABLE_EVENTS, selection, selectionArgs);
                } catch (SQLiteException e) {
                    isDbWritable = false;
                    BzLog.printStackTrace(e);
                }
            }
            //目前逻辑不处理其他 Code
        } catch (Exception e) {
            BzLog.printStackTrace(e);
        }
        return deletedCounts;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // 不处理 values = null 或者 values 为空的情况
        if (!isDbWritable || values == null || values.size() == 0) {
            return uri;
        }
        try {
            int code = uriMatcher.match(uri);
            if (code == EVENTS) {
                SQLiteDatabase database;
                try {
                    database = dbHelper.getWritableDatabase();
                } catch (SQLiteException e) {
                    isDbWritable = false;
                    BzLog.printStackTrace(e);
                    return uri;
                }
                if (!values.containsKey(DbParams.KEY_DATA) || !values.containsKey(DbParams.KEY_CREATED_AT)) {
                    return uri;
                }
                long d = database.insert(DbParams.TABLE_EVENTS, "_id", values);
                return ContentUris.withAppendedId(uri, d);
            } else {
                insert(code, uri, values);
            }
            return uri;
        } catch (Exception e) {
            BzLog.printStackTrace(e);
        }
        return uri;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        if (!isDbWritable) {
            return 0;
        }
        int numValues;
        SQLiteDatabase database = null;
        try {
            try {
                database = dbHelper.getWritableDatabase();
            } catch (SQLiteException e) {
                isDbWritable = false;
                BzLog.printStackTrace(e);
                return 0;
            }
            database.beginTransaction();
            numValues = values.length;
            for (int i = 0; i < numValues; i++) {
                insert(uri, values[i]);
            }
            database.setTransactionSuccessful();
        } finally {
            if (database != null) {
                database.endTransaction();
            }
        }
        return numValues;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (!isDbWritable) {
            return null;
        }
        Cursor cursor = null;
        try {
            int code = uriMatcher.match(uri);
            if (code == EVENTS) {
                try {
                    cursor = dbHelper.getWritableDatabase().query(DbParams.TABLE_EVENTS, projection, selection, selectionArgs, null, null, sortOrder);
                } catch (SQLiteException e) {
                    isDbWritable = false;
                    BzLog.printStackTrace(e);
                }
            } else {
                cursor = query(code);
            }
        } catch (Exception e) {
            BzLog.printStackTrace(e);
        }
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    /**
     * insert 处理
     *
     * @param code Uri code
     * @param uri Uri
     * @param values ContentValues
     */
    private void insert(int code, Uri uri, ContentValues values) {
        switch (code) {
            case ACTIVITY_START_COUNT:
                startActivityCount = values.getAsInteger(DbParams.TABLE_ACTIVITY_START_COUNT);
                break;
            case APP_START_TIME:
                persistentAppStartTime.commit(values.getAsLong(DbParams.TABLE_APP_START_TIME));
                break;
            case APP_PAUSED_TIME:
                persistentAppPaused.commit(values.getAsLong(DbParams.TABLE_APP_END_TIME));
                break;
            case APP_END_DATA:
                persistentAppEndData.commit(values.getAsString(DbParams.TABLE_APP_END_DATA));
                break;
            case SESSION_INTERVAL_TIME:
                persistentSessionIntervalTime.commit(values.getAsInteger(DbParams.TABLE_SESSION_INTERVAL_TIME));
                contentResolver.notifyChange(uri, null);
                break;
            case LOGIN_ID:
                persistentLoginId.commit(values.getAsString(DbParams.TABLE_LOGIN_ID));
                break;
            default:
                break;
        }
    }

    /**
     * query 处理
     *
     * @param code Uri code
     * @return Cursor
     */
    private Cursor query(int code) {
        String column = null;
        Object data = null;
        switch (code) {
            case ACTIVITY_START_COUNT:
                data = startActivityCount;
                column = DbParams.TABLE_ACTIVITY_START_COUNT;
                break;
            case APP_START_TIME:
                data = persistentAppStartTime.get();
                column = DbParams.TABLE_APP_START_TIME;
                break;
            case APP_PAUSED_TIME:
                data = persistentAppPaused.get();
                column = DbParams.TABLE_APP_END_TIME;
                break;
            case APP_END_DATA:
                data = persistentAppEndData.get();
                column = DbParams.TABLE_APP_END_DATA;
                break;
            case SESSION_INTERVAL_TIME:
                data = persistentSessionIntervalTime.get();
                column = DbParams.TABLE_SESSION_INTERVAL_TIME;
                break;
            case LOGIN_ID:
                data = persistentLoginId.get();
                column = DbParams.TABLE_LOGIN_ID;
            default:
                break;
        }

        MatrixCursor matrixCursor = new MatrixCursor(new String[]{column});
        matrixCursor.addRow(new Object[]{data});
        return matrixCursor;
    }
}
