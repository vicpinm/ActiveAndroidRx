package com.activeandroid.rx;


import android.database.Cursor;
import android.text.TextUtils;

import com.activeandroid.Cache;
import com.activeandroid.Model;
import com.activeandroid.TableInfo;
import com.activeandroid.rxschedulers.AndroidSchedulers;
import com.activeandroid.sqlbrite.SqlBrite;
import com.activeandroid.util.SQLiteUtils;

import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by Victor on 30/10/2015.
 */
public class RxSelect<T> {

    private Class type;
    private String where;

    public RxSelect<T> from(Class type) {
        this.type = type;
        return this;
    }

    public RxSelect<T> where(String where) {
        this.where = where;
        return this;
    }

    public rx.Observable<T> execute() {

        TableInfo tableInfo = new TableInfo(type);

        String sql = "SELECT * FROM " + tableInfo.getTableName();

        if (!TextUtils.isEmpty(where)) {
            sql += " WHERE " + where;
        }

        return Cache.openDatabase().createQuery(tableInfo.getTableName(), sql)
                .subscribeOn(Schedulers.io())
                .map(new Func1<SqlBrite.Query, T>() {
                    @Override
                    public T call(SqlBrite.Query query) {
                        try {
                            Cursor cursor = query.run();
                            return (T) SQLiteUtils.processCursor(type, cursor);

                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                        return null;

                    }
                })
                .observeOn(AndroidSchedulers.mainThread());

    }

    public rx.Observable<T> executeSingle() {

        TableInfo tableInfo = new TableInfo(type);

        String sql = "SELECT * FROM " + tableInfo.getTableName();

        if (!TextUtils.isEmpty(where)) {
            sql += " WHERE " + where;
        }

        return Cache.openDatabase().createQuery(tableInfo.getTableName(), sql)
                .subscribeOn(Schedulers.io())
                .map(new Func1<SqlBrite.Query, T>() {
                    @Override
                    public T call(SqlBrite.Query query) {
                        try {
                            Cursor cursor = query.run();
                            if (cursor != null && cursor.getCount() > 0) {
                                cursor.moveToFirst();
                                Model model = (Model) type.newInstance();
                                model.loadFromCursor(cursor);
                                return (T)model;
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                        return null;

                    }
                })
                .observeOn(AndroidSchedulers.mainThread());

    }
}
