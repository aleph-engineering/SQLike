package com.yarieldis.sqlike.base;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.yarieldis.sqlike.annotation.IntegerField;
import com.yarieldis.sqlike.annotation.StringField;
import com.yarieldis.sqlike.annotation.Table;
import com.yarieldis.sqlike.annotation.TableField;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class DataSet<T extends Entity> {

    private static final String TAG = "DataSet";
    private DataContext mDataContext;
    private Class<T> mManagedType;

    public DataSet(Class<T> managedType, DataContext dataContext) {
        mDataContext = dataContext;
        mManagedType = managedType;
    }

    private String getTableName() {
        Table tableAnnotation = mManagedType.getAnnotation(Table.class);
        String tableName = tableAnnotation.name();
        if (tableName.equals("")) {
            tableName = mManagedType.getSimpleName();
        }
        return tableName;
    }

    private String[] getTableFields() {
        List<String> fields = new ArrayList<>();
        for (Field field : mManagedType.getDeclaredFields()) {
            if (field.isAnnotationPresent(IntegerField.class) || field.isAnnotationPresent(StringField.class)) {
                fields.add(field.getName());
            }
        }
        String[] toArray = new String[fields.size() + 1];
        toArray[0] = "id"; // FIXME id
        for (int i = 0, j = 1; i < fields.size(); i++, j++) {
            toArray[j] = fields.get(i);
        }
        return toArray;
    }

    public String getCreateScript() {
        String script = "CREATE TABLE " + getTableName() + " ( " +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, "; // FIXME id
        for (Field field : mManagedType.getDeclaredFields()) {
            if (field.isAnnotationPresent(IntegerField.class)) {
                String fieldName = "";
                if (field.isAnnotationPresent(TableField.class)) {
                    TableField fieldAnnotation = field.getAnnotation(TableField.class);
                    fieldName = fieldAnnotation.name();
                }
                if (fieldName.equals("")) {
                    fieldName = field.getName();
                }
                script += fieldName + " INTEGER, ";
            }
            if (field.isAnnotationPresent(StringField.class)) {
                String fieldName = "";
                if (field.isAnnotationPresent(TableField.class)) {
                    TableField fieldAnnotation = field.getAnnotation(TableField.class);
                    fieldName = fieldAnnotation.name();
                }
                if (fieldName.equals("")) {
                    fieldName = field.getName();
                }
                script += fieldName + " TEXT, ";
            }
        }
        return script.substring(0, script.length() - 2) + " )";
    }

    public T get(int id) {
        SQLiteDatabase db = mDataContext.getReadableDatabase();

        String[] tableFields = getTableFields();
        Cursor cursor =
                db.query(getTableName(), // a. table
                        tableFields, // b. column names
                        " id = ?", // FIXME id c. selections
                        new String[] { String.valueOf(id) }, // d. selections args
                        null, // e. group by
                        null, // f. having
                        null, // g. order by
                        null); // h. limit
        if (cursor != null) {
            cursor.moveToFirst();
            try {
                T tuple = mManagedType.newInstance();
                int cursorIndex = 0;
                Method idSetterMethod = tuple.getClass().getMethod(
                        "setId",
                        int.class
                );
                idSetterMethod.invoke(tuple, cursor.getInt(cursorIndex));
                cursorIndex++;
                for (Field field : mManagedType.getDeclaredFields()) {
                    if (field.isAnnotationPresent(IntegerField.class) || field.isAnnotationPresent(StringField.class)) {
                        Method setterMethod = tuple.getClass().getMethod(
                                String.format("set%s", Helper.capitalize(field.getName())),
                                field.getType()
                        );
                        if (field.isAnnotationPresent(IntegerField.class)) {
                            setterMethod.invoke(tuple, cursor.getInt(cursorIndex));
                            cursorIndex++;
                        }
                        if (field.isAnnotationPresent(StringField.class)) {
                            setterMethod.invoke(tuple, cursor.getString(cursorIndex));
                            cursorIndex++;
                        }
                    }
                }
                return tuple;
            } catch (Exception e) {
                Log.e(TAG, e.getLocalizedMessage());
                return null;
            } finally {
                cursor.close();
            }
        }

        return null;
    }

    public int add(T entity) {
        SQLiteDatabase db = mDataContext.getWritableDatabase();
        ContentValues values = new ContentValues();
        for (Field field : mManagedType.getDeclaredFields()) {
            try {
                if (field.isAnnotationPresent(IntegerField.class) || field.isAnnotationPresent(StringField.class)) {
                    Method getterMethod = entity.getClass().getMethod(
                            String.format("get%s", Helper.capitalize(field.getName()))
                    );
                    Object data = getterMethod.invoke(entity);
                    String fieldName = "";
                    if (field.isAnnotationPresent(TableField.class)) {
                        TableField fieldAnnotation = field.getAnnotation(TableField.class);
                        fieldName = fieldAnnotation.name();
                    }
                    if (fieldName.equals("")) {
                        fieldName = field.getName();
                    }
                    values.put(fieldName, String.valueOf(data));
                }
            } catch (Exception e) {
                Log.e(TAG, e.getLocalizedMessage());
                return 0;
            }
        }
        long id = db.insert(getTableName(), // table
                null, //nullColumnHack
                values); // key/value -> keys = column names/ values = column values
        db.close();

        return Long.valueOf(id).intValue();
    }

    public boolean isEmpty() {
        SQLiteDatabase db = mDataContext.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + getTableName(), null);
        if (cursor != null && cursor.moveToNext()) {
            cursor.close();
            return false;
        }
        return true;
    }

    public List<T> search(String where, String[] values) {
        SQLiteDatabase db = mDataContext.getReadableDatabase();
        Cursor cursor =
                db.query(getTableName(), // a. table
                        getTableFields(), // b. column names
                        where, // c. selections
                        values, // d. selections args
                        null, // e. group by
                        null, // f. having
                        null, // g. order by
                        null); // h. limit
        if (cursor != null) {
            List<T> list = new ArrayList<>();
            while (cursor.moveToNext()) {
                try {
                    T tuple = mManagedType.newInstance();
                    int cursorIndex = 0;
                    Method idSetterMethod = tuple.getClass().getMethod(
                            "setId",
                            int.class
                    );
                    idSetterMethod.invoke(tuple, cursor.getInt(cursorIndex));
                    cursorIndex++;
                    for (Field field : mManagedType.getDeclaredFields()) {
                        if (field.isAnnotationPresent(IntegerField.class) || field.isAnnotationPresent(StringField.class)) {
                            Method setterMethod = tuple.getClass().getMethod(
                                    String.format("set%s", Helper.capitalize(field.getName())),
                                    field.getType()
                            );
                            if (field.isAnnotationPresent(IntegerField.class)) {
                                setterMethod.invoke(tuple, cursor.getInt(cursorIndex));
                                cursorIndex++;
                            }
                            if (field.isAnnotationPresent(StringField.class)) {
                                setterMethod.invoke(tuple, cursor.getString(cursorIndex));
                                cursorIndex++;
                            }
                        }
                    }
                    list.add(tuple);
                } catch (Exception e) {
                    Log.e(TAG, e.getLocalizedMessage());
                }
            }
            cursor.close();
            return list;
        }
        return null;
    }

    public List<T> all() {
        SQLiteDatabase db = mDataContext.getReadableDatabase();
        String query = "SELECT * FROM " + getTableName();
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null) {
            List<T> list = new ArrayList<>();
            while (cursor.moveToNext()) {
                try {
                    T tuple = mManagedType.newInstance();
                    int cursorIndex = 0;
                    Method idSetterMethod = tuple.getClass().getMethod(
                            "setId",
                            int.class
                    );
                    idSetterMethod.invoke(tuple, cursor.getInt(cursorIndex));
                    cursorIndex++;
                    for (Field field : mManagedType.getDeclaredFields()) {
                        if (field.isAnnotationPresent(IntegerField.class) || field.isAnnotationPresent(StringField.class)) {
                            Method setterMethod = tuple.getClass().getMethod(
                                    String.format("set%s", Helper.capitalize(field.getName())),
                                    field.getType()
                            );
                            if (field.isAnnotationPresent(IntegerField.class)) {
                                setterMethod.invoke(tuple, cursor.getInt(cursorIndex));
                                cursorIndex++;
                            }
                            if (field.isAnnotationPresent(StringField.class)) {
                                setterMethod.invoke(tuple, cursor.getString(cursorIndex));
                                cursorIndex++;
                            }
                        }
                    }
                    list.add(tuple);
                } catch (Exception e) {
                    Log.e(TAG, e.getLocalizedMessage());
                }
            }
            cursor.close();
            return list;
        }
        return null;
    }

    public int update(T entity) {
        SQLiteDatabase db = mDataContext.getWritableDatabase();
        ContentValues values = new ContentValues();
        for (Field field : mManagedType.getDeclaredFields()) {
            try {
                if (field.isAnnotationPresent(IntegerField.class) || field.isAnnotationPresent(StringField.class)) {
                    Method getterMethod = entity.getClass().getMethod(
                            String.format("get%s", Helper.capitalize(field.getName()))
                    );
                    Object data = getterMethod.invoke(entity);
                    String fieldName = "";
                    if (field.isAnnotationPresent(TableField.class)) {
                        TableField fieldAnnotation = field.getAnnotation(TableField.class);
                        fieldName = fieldAnnotation.name();
                    }
                    if (fieldName.equals("")) {
                        fieldName = field.getName();
                    }
                    values.put(fieldName, String.valueOf(data));
                }
            } catch (Exception e) {
                Log.e(TAG, e.getLocalizedMessage());
                return 0;
            }
        }
        int i = db.update(
                getTableName(), //table
                values, // column/value
                "id = ?", // FIXME id selections
                new String[] { String.valueOf(entity.getId()) } //selection args
        );
        db.close();
        return i;
    }

    public void delete(T entity) {
        SQLiteDatabase db = mDataContext.getWritableDatabase();

        db.delete(
                getTableName(),
                "id = ?", // FIXME id
                new String[] { String.valueOf(entity.getId()) }
        );

        db.close();
    }
}
