package io.huiy.deemo.sensor;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniModule;

import static android.content.Context.SENSOR_SERVICE;
import static java.lang.Math.PI;


/**
 * Android陀螺仪插件
 * <p>
 * 用于监听方位（东南西北），并实时返回角度
 */
public class SensorModule extends UniModule {

    String TAG = "SensorModule";
    public UniJSCallback call;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagneticField;

    private float[] mAccelerometerValues = new float[3];
    private float[] mMagneticFieldValues = new float[3];
    private float[] mValues = new float[3];
    private float[] mMatrix = new float[9];
    private float degree = 0;

    private SensorEventListener listener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mAccelerometerValues = event.values;
            }
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                mMagneticFieldValues = event.values;
            }

            // 调用getRotaionMatrix获得变换矩阵mMatrix[]
            SensorManager.getRotationMatrix(mMatrix, null, mAccelerometerValues, mMagneticFieldValues);
            SensorManager.getOrientation(mMatrix, mValues);

            // 经过SensorManager.getOrientation(R, values);得到的values值为弧度
            // values[0]  ：azimuth 方向角，但用（磁场+加速度）得到的数据范围是（-180～180）,
            // 也就是说，0表示正北，90表示正东，180/-180表示正南，-90表示正西。
            // 而直接通过方向感应器数据范围是（0～359）360/0表示正北，90表示正东，180表示正南，270表示正西。

            callBackSensorInfo();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    /**
     * 注册陀螺仪数据监听
     */
    private void registerSensor() {
        if (mSensorManager == null) {
            mSensorManager = (SensorManager) mUniSDKInstance.getContext().getSystemService(SENSOR_SERVICE);
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }

        mSensorManager.registerListener(listener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(listener, mMagneticField, SensorManager.SENSOR_DELAY_NORMAL);

    }

    /**
     * 取消注册陀螺仪数据监听
     */
    private void unRegisterSensor() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(listener);
        }
        call = null;
    }


    /**
     * 注册陀螺仪监听
     *
     * @param callback 回调，陀螺仪数据会通过此回调返回
     *                 callback 返回参数包括 degree,x,y,z
     *                 degree 当前方向角度, 取值范围 0~360, 0-北 90-东 180-南 270-西
     *                 x,y,z 陀螺仪传感器返回的 x,y,z三轴的原始数据，取值范围为 -PI~PI
     *                 可以通过 toDegree180Func 和 toDegree360Func 方法转换正角度
     */
    @UniJSMethod(uiThread = true)
    public void registerSensorAFunc(UniJSCallback callback) {
        Log.e(TAG, "registerSensor -->");
        registerSensor();
        call = callback;

        if (callback != null) {
            JSONObject data = new JSONObject();
            data.put("code", "success");
            data.put("action", "registerSensor");
            data.put("degree", degree);
            data.put("x", mValues[0]);
            data.put("y", mValues[1]);
            data.put("z", mValues[2]);
//            callback.invoke(data);  // 只能回调一次，回调一次后销毁
            callback.invokeAndKeepAlive(data);  // 可以多次回调，一直保活
        }
    }

    /**
     * 取消陀螺仪注册
     *
     * @return json
     */
    @UniJSMethod(uiThread = false)
    public JSONObject unRegisterSensorFunc() {
        Log.e(TAG, "unRegisterSensor-->");
        unRegisterSensor();
        JSONObject data = new JSONObject();
        data.put("code", "success");
        data.put("action", "unRegisterSensor");
        return data;
    }

    /**
     * 获取当前陀螺仪数据
     *
     * @return json 包含陀螺仪数据 degree,x,y,z
     */
    @UniJSMethod(uiThread = false)
    public JSONObject getSensorInfoFunc() {
        Log.e(TAG, "getSensorInfo -->");
        JSONObject data = new JSONObject();
        data.put("degree", degree);
        data.put("x", mValues[0]);
        data.put("y", mValues[1]);
        data.put("z", mValues[2]);
        return data;
    }

    /**
     * 将陀螺仪原始坐标转换成角度
     *
     * @param json 陀螺仪原始数据，从 value中取
     * @return degree 角度，取值范围 -180~180
     */
    @UniJSMethod(uiThread = false)
    public JSONObject toDegree180Func(JSONObject json) {
        Log.e(TAG, "toDegree180Func -->" + json);
        float value = json.getFloatValue("value");
        JSONObject data = new JSONObject();
        data.put("degree", value * 180.0 / PI);
        return data;
    }

    /**
     * 将陀螺仪原始坐标转换成角度
     *
     * @param json 陀螺仪原始数据，从 value中取
     * @return degree 角度，取值范围 0~360
     */
    @UniJSMethod(uiThread = false)
    public JSONObject toDegree360Func(JSONObject json) {
        Log.e(TAG, "toDegree360Func -->" + json);
        float value = json.getFloatValue("value");
        JSONObject data = new JSONObject();
        if (value > 0) {
            data.put("degree", value * 180.0 / PI);
        } else {
            data.put("degree", value * 180.0 / PI + 360);
        }
        return data;
    }

    /**
     * 陀螺仪返回数据的回调
     */
    private void callBackSensorInfo() {
        if (call == null) {
            return;
        }

        if (mUniSDKInstance == null || mUniSDKInstance.getContext() == null) {
            unRegisterSensor();
            return;
        }

        // 计算方位角度 0-360 （0-北 90-东 180-南 270-西）
        degree = (float) Math.toDegrees(mValues[0]);
        if (degree < 0) {
            degree = degree + 360;
        }
        // Log.v("degree  -->", degree + "");


        // 回调陀螺仪数据
        ((Activity) mUniSDKInstance.getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                Log.v("runOnUiThread -->", degree + "");
//
//                Map<String, Object> params = new HashMap<>();
//                params.put("key", "value");
//                params.put("degree", degree);
//
//                Log.v("Callback ==> ", "fireGlobalEventCallback  degree:" + degree + " instances:" + mUniSDKInstance);
//                mUniSDKInstance.fireGlobalEventCallback("myEvent", params);
                JSONObject data = new JSONObject();
                data.put("code", "success");
                data.put("x", mValues[0]);
                data.put("y", mValues[1]);
                data.put("z", mValues[2]);
                data.put("action", "onSensorCallback");
                data.put("degree", degree);
                call.invokeAndKeepAlive(data);
            }
        });

    }


}
