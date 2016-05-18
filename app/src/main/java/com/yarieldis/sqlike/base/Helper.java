package com.yarieldis.sqlike.base;


import android.util.Log;

import java.lang.reflect.Type;

/*

Thanks to ADA framework developers

 */
public class Helper {

    private static final String TAG = "Helper";

    public static boolean extendsOf(Type pType, Class<?> pClass) {
        boolean returnedValue = false;
        try {
            if (pType == pClass) {
                returnedValue = true;
            } else {
                Class<?> superClass = ((Class<?>)pType).getSuperclass();
                while(superClass != null && superClass != Object.class) {
                    if (superClass == pClass) {
                        returnedValue = true;
                        break;
                    }
                    superClass = superClass.getSuperclass();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        return returnedValue;
    }

    public static String capitalize(String pValue) {
        if (pValue.length() <= 1) return pValue;
        return pValue.substring(0, 1).toUpperCase() + pValue.substring(1);
    }
}
