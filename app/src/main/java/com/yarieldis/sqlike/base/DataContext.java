package com.yarieldis.sqlike.base;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.lang.reflect.Field;

public class DataContext {

    private static final String TAG = "DataContext";

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "database.db";

    private Context mContext;
    private String mDatabaseName;

    private DataBaseHelper mDataBaseHelper;

    public DataContext(Context context) {
        mContext = context;
        mDataBaseHelper = new DataBaseHelper(this);
        mDatabaseName = DATABASE_NAME;
    }

    public DataContext(Context context, String databaseName) {
        mContext = context;
        mDataBaseHelper = new DataBaseHelper(this);
        mDatabaseName = databaseName;
    }

    public SQLiteDatabase getWritableDatabase() {
        return mDataBaseHelper.getWritableDatabase();
    }

    public SQLiteDatabase getReadableDatabase() {
        return mDataBaseHelper.getReadableDatabase();
    }

    private class DataBaseHelper extends SQLiteOpenHelper {

        private DataContext mDataContext;

        private DataBaseHelper(DataContext dataContext) {
            super(mContext, mDatabaseName, null, DATABASE_VERSION);
            mDataContext = dataContext;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            for (Field field : mDataContext.getClass().getDeclaredFields()) {
                if (Helper.extendsOf(field.getType(), DataSet.class)) {
                    try {
                        DataSet dataSet = (DataSet) field.get(mDataContext);
                        String scriptToCreate = dataSet.getCreateScript();
                        Log.d(TAG, "onCreate: " + scriptToCreate);
                        db.execSQL(scriptToCreate);
                    } catch (Exception e) {
                        Log.e(TAG, e.getLocalizedMessage());
                    }
                }
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO drop databases and recreate them.
        }
    }
}
