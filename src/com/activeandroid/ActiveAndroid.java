package com.activeandroid;

/*
 * Copyright (C) 2010 Michael Pardo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.database.Cursor;

import com.activeandroid.rxschedulers.AndroidSchedulers;
import com.activeandroid.sqlbrite.BriteDatabase;
import com.activeandroid.sqlbrite.SqlBrite;
import com.activeandroid.util.Log;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public final class ActiveAndroid {
    //////////////////////////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    //////////////////////////////////////////////////////////////////////////////////////

    public static void initialize(Context context) {
        initialize(new Configuration.Builder(context).create());
    }

    public static void initialize(Configuration configuration) {
        initialize(configuration, false);
    }

    public static void initialize(Context context, boolean loggingEnabled) {
        initialize(new Configuration.Builder(context).create(), loggingEnabled);
    }

    public static void initialize(Configuration configuration, boolean loggingEnabled) {
        // Set logging enabled first
        setLoggingEnabled(loggingEnabled);
        Cache.initialize(configuration);
    }

    public static void clearCache() {
        Cache.clear();
    }

    public static void dispose() {
        Cache.dispose();
    }

    public static void setLoggingEnabled(boolean enabled) {
        Log.setEnabled(enabled);
    }

    public static BriteDatabase getDatabase() {
        return Cache.openDatabase();
    }

    public static BriteDatabase.Transaction beginTransaction() {
        return Cache.openDatabase().newTransaction();
    }

    public static void endTransaction(BriteDatabase.Transaction transaction) {
        transaction.end();
    }

    public static void setTransactionSuccessful(BriteDatabase.Transaction transaction) {
        transaction.markSuccessful();
    }


    public static void execSQL(String sql) {
        Cache.openDatabase().execute(sql);
    }

    public static void execSQL(String sql, Object[] bindArgs) {
        Cache.openDatabase().execute(sql, bindArgs);
    }

    public static Observable<SqlBrite.Query> query(String table, String sql) {
        return Cache.openDatabase().createQuery(table, sql);
    }

    public static Observable<Cursor> queryCursor(Class<? extends Model> clase, String sql) {

        TableInfo tableInfo = new TableInfo(clase);
        return Cache.openDatabase().createQuery(tableInfo.getTableName(), sql)
                .subscribeOn(Schedulers.io())
                .map(new Func1<SqlBrite.Query, Cursor>() {
                    @Override
                    public Cursor call(SqlBrite.Query query) {
                        return query.run();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread());
    }

    public static Observable<List<? extends Model>> queryObject(final Class<? extends Model> clase,String sql) {

        TableInfo tableInfo = new TableInfo(clase);

        return Cache.openDatabase().createQuery(tableInfo.getTableName(), sql)
                .subscribeOn(Schedulers.io())
                .map(new Func1<SqlBrite.Query, List<? extends Model>>() {
                    @Override
                    public List<? extends Model> call(SqlBrite.Query query) {
                        try {
                            Cursor cursor = query.run();
                            List<Model> data = new ArrayList<Model>();
                            if (cursor != null && cursor.getCount() > 0) {
                                while (cursor.moveToNext()) {
                                    Model model = clase.newInstance();
                                    model.loadFromCursor(cursor);
                                    data.add(model);
                                }
                            }

                            return data;
                        } catch(Exception ex){
                            ex.printStackTrace();
                        }

                        return null;

                    }
                })
                .observeOn(AndroidSchedulers.mainThread());
    }



}
