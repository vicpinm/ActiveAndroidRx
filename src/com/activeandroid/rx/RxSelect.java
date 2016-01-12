package com.activeandroid.rx;


import android.database.Cursor;
import android.text.TextUtils;

import com.activeandroid.Cache;
import com.activeandroid.Model;
import com.activeandroid.rxschedulers.AndroidSchedulers;
import com.activeandroid.sqlbrite.SqlBrite;
import com.activeandroid.util.Log;
import com.activeandroid.util.SQLiteUtils;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by Victor on 30/10/2015.
 */
public class RxSelect<T extends Model> {

    private Class<T> mType;
    private String mAlias;
    private StringBuilder mWhere = new StringBuilder();
    private String mLimit;
    private String mGroupBy;
    private String mHaving;
    private String mOrderBy;
    private String mOffset;
    private List<Object> mArguments;

    private RxSelect(Class<T> type) {
        this.mType = type;
        mArguments = new ArrayList<>();
    }

    public static <T extends Model> RxSelect<T> from(Class<T> type) {
        return new RxSelect<>(type);
    }

    public RxSelect<T> as(String alias) {
        mAlias = alias;
        return this;
    }

    public RxSelect<T> groupBy(String groupBy) {
        mGroupBy = groupBy;
        return this;
    }

    public RxSelect<T> where(String clause) {
        // Chain conditions if a previous condition exists.
        if (mWhere.length() > 0) {
            mWhere.append(" AND ");
        }
        mWhere.append(clause);
        return this;
    }

    public RxSelect<T> where(String clause, Object... args) {
        where(clause).addArguments(args);
        return this;
    }

    public RxSelect<T> orderBy(String orderBy) {
        mOrderBy = orderBy;
        return this;
    }

    public RxSelect<T> limit(int limit) {
        return limit(String.valueOf(limit));
    }

    public RxSelect<T> limit(String limit) {
        mLimit = limit;
        return this;
    }

    public RxSelect<T> offset(int offset) {
        return offset(String.valueOf(offset));
    }

    public RxSelect<T> offset(String offset) {
        mOffset = offset;
        return this;
    }

    public RxSelect<T> having(String having) {
        mHaving = having;
        return this;
    }

    public <T extends Model> rx.Observable<List<T>> execute() {

        String sql = buildSql();

        return Cache.openDatabase().createQuery(Cache.getTableName(mType), sql, getArguments())
                .subscribeOn(Schedulers.io())
                .map(new Func1<SqlBrite.Query, List<T>>() {
                    @Override
                    public List<T> call(SqlBrite.Query query) {
                        try {
                            Cursor cursor = query.run();
                            return SQLiteUtils.processCursor(mType, cursor);

                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                        return null;

                    }
                })
                .observeOn(AndroidSchedulers.mainThread());
    }

    public rx.Observable<T> executeSingle() {

        String sql = buildSql();

        return Cache.openDatabase().createQuery(Cache.getTableName(mType), sql, getArguments())
                .subscribeOn(Schedulers.io())
                .map(new Func1<SqlBrite.Query, T>() {
                    @Override
                    public T call(SqlBrite.Query query) {
                        try {
                            Cursor cursor = query.run();
                            if (cursor != null && cursor.getCount() > 0) {
                                cursor.moveToFirst();
                                Model model = (Model) mType.newInstance();
                                model.loadFromCursor(cursor);
                                return (T) model;
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                        return null;

                    }
                })
                .observeOn(AndroidSchedulers.mainThread());
    }

    private String buildSql() {
        StringBuilder sql = new StringBuilder();

        buildSelect(sql);
        addFrom(sql);
        addWhere(sql);
        addGroupBy(sql);
        addHaving(sql);
        addOrderBy(sql);
        addLimit(sql);
        addOffset(sql);

        return sqlString(sql);
    }

    public String toCountSql() {

        final StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) ");

        addFrom(sql);
        addWhere(sql);
        addGroupBy(sql);
        addHaving(sql);
        addLimit(sql);
        addOffset(sql);

        return sqlString(sql);
    }

    private void buildSelect(final StringBuilder sql) {
        sql.append("SELECT * ");
    }

    private void addFrom(final StringBuilder sql) {
        sql.append("FROM ");
        sql.append(Cache.getTableName(mType)).append(" ");

        if (mAlias != null) {
            sql.append("AS ");
            sql.append(mAlias);
            sql.append(" ");
        }
    }

    private void addWhere(final StringBuilder sql) {
        if (!TextUtils.isEmpty(mWhere)) {
            sql.append("WHERE ");
            sql.append(mWhere);
            sql.append(" ");
        }
    }

    private void addGroupBy(final StringBuilder sql) {
        if (mGroupBy != null) {
            sql.append("GROUP BY ");
            sql.append(mGroupBy);
            sql.append(" ");
        }
    }

    private void addHaving(final StringBuilder sql) {
        if (mHaving != null) {
            sql.append("HAVING ");
            sql.append(mHaving);
            sql.append(" ");
        }
    }

    private void addOrderBy(final StringBuilder sql) {
        if (mOrderBy != null) {
            sql.append("ORDER BY ");
            sql.append(mOrderBy);
            sql.append(" ");
        }
    }

    private void addLimit(final StringBuilder sql) {
        if (!TextUtils.isEmpty(mLimit)) {
            sql.append("LIMIT ");
            sql.append(mLimit);
            sql.append(" ");
        }
    }

    private void addOffset(final StringBuilder sql) {
        if (mOffset != null) {
            sql.append("OFFSET ");
            sql.append(mOffset);
            sql.append(" ");
        }
    }

    /**
     * Gets the number of rows returned by the query.
     */
    public Observable<Integer> count() {
        return Cache.openDatabase().createQuery(Cache.getTableName(mType), toCountSql(), getArguments())
                .subscribeOn(Schedulers.io())
                .map(new Func1<SqlBrite.Query, Integer>() {
                    @Override public Integer call(SqlBrite.Query query) {
                        Cursor cursor = query.run();
                        if (cursor.moveToFirst()) {
                            return cursor.getInt(cursor.getColumnIndex(cursor.getColumnName(0)));
                        }
                        return 0;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread());
    }

    private String sqlString(final StringBuilder sql) {
        return sql.toString().trim();
    }

    void addArguments(Object[] args) {
        for(Object arg : args) {
            if (arg.getClass() == boolean.class || arg.getClass() == Boolean.class) {
                arg = (arg.equals(true) ? 1 : 0);
            }
            mArguments.add(arg);
        }
    }

    public String[] getArguments() {
        final int size = mArguments.size();
        final String[] args = new String[size];

        for (int i = 0; i < size; i++) {
            args[i] = mArguments.get(i).toString();
        }

        return args;
    }
}
