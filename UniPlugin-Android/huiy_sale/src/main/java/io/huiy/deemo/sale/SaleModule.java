package io.huiy.deemo.sale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.Log;
import android.view.View;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.epton.sdk.callback.ADH812ResultListener;
import com.epton.sdk.callback.ADH812StateListener;
import com.epton.sdk.callback.DevicesStateListener;
import com.epton.sdk.callback.ResultCallBack;
import com.epton.sdk.port.ADH812Port;
import com.epton.sdk.port.ADH815Port;
import com.epton.sdk.port.PortController;
import com.taobao.weex.annotation.JSMethod;
import com.taobao.weex.bridge.JSCallback;

import java.util.Locale;

import io.dcloud.feature.uniapp.common.UniModule;

public class SaleModule extends UniModule {

    private static final String TAG = "SaleModule";
    private int targetAisle;    // 当前货道号

    private JSCallback devStateCallback;            // 设备状态监听，返回各个货柜是否断开连接
    private JSCallback adh812StateCallback;         // 812设备状态监听
    private JSCallback outGoodsCallback;            // 掉出货接口返回之后的监听，返回升降梯定位是否成功、是否出货成功
    private JSCallback readCoordinateCallback;      // 读取坐标，返回回调

    // 设备板卡状态监听
    private final DevicesStateListener devicesStateListener = new DevicesStateListener() {

        /**
         * 状态改变下触发，可实时获取板卡状态及温度值
         * @param state 815 状态数组，长度为 3，驱动板不启用时状态为-1；
         * @param temp 当前温度数组，长度为 3，驱动板不启用是为 -40。
         *  返回值：无
         */
        @Override
        public void onStateChanged(int[] state, byte[] temp) {
            // 815 状态改变
            JSONObject data = new JSONObject();
            try {
                data.put("code", 500);
                data.put("msg", state);
                data.put("temp", temp);

                data.put("815Temp", String.format(Locale.CHINA, "%.2f", ((float) ADH815Port.curTemp / 100)));
                data.put("sensorState", ADH815Port.sensorState);    // 主柜掉货检测 (升降) 【主柜光眼遮挡状态】
                data.put("sensorState2", ADH815Port.sensorState2);  // 副柜1掉货检测 （开门）
                data.put("sensorState3", ADH815Port.sensorState3);  // 副柜2掉货检测

                data.put("ADH815State", ADH815Port.ADH815State);    // 主控制板当前状态
                data.put("ADH815State2", ADH815Port.ADH815State2);  // 副柜 1 控制板当前状态
                data.put("ADH815State3", ADH815Port.ADH815State3);  // 副柜 2 控制板当前状态

                Log.d(TAG, "DevicesStateListener: onStateChanged  --> " + data);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            devStateCallback.invokeAndKeepAlive(data);
        }

        @Override
        public void onModeChanged() {

        }

        /**
         * 串口通讯中断时触
         * @param devNo 货柜编号
         */
        @Override
        public void onDisconnected(int devNo) {
            if (devNo == 1) {
                Log.e(TAG, "--> 主柜连接中断");
                devStateCallback.invokeAndKeepAlive(initJSONObject(1, "主柜连接中断"));
            } else if (devNo == 2) {
                Log.e(TAG, "--> 副柜1连接中断");
                devStateCallback.invokeAndKeepAlive(initJSONObject(2, "副柜1连接中断"));
            } else if (devNo == 3) {
                Log.e(TAG, "--> 副柜2连接中断");
                devStateCallback.invokeAndKeepAlive(initJSONObject(3, "副柜2连接中断"));
            }
        }
    };

    // ADH812 板卡状态监听器
    private final ADH812StateListener adh812StateListener = new ADH812StateListener() {

        /**
         * ADH812 状态回调函数，
         * 接收到此回调后可通过 ADH812Port 中的相关变量获取各开关的状态
         * @param state ADH812 板卡状态
         */
        @Override
        public void on812StateReturned(int state) {

            JSONObject data = new JSONObject();
            try {
                data.put("code", 800);
                data.put("msg", state);
                data.put("boardState", ADH812Port.boardState);  // ADH812 控制板状态
                data.put("commonError", ADH812Port.commonError);  // 普通故障字
                data.put("haltError", ADH812Port.haltError);      // 停机错误字
                data.put("curLayerCount", ADH812Port.curLayerCount); //升降梯当前所在层数

                data.put("812Temp", String.format(Locale.CHINA, "%.2f", ((float) ADH812Port.cur812Temp / 100))); // ADH812 当前温控采集温度
                data.put("doorCloseLimiter", ADH812Port.doorCloseLimiter);  //取货门关门限位开关
                data.put("topLimiter", ADH812Port.topLimiter); //上限位开关
                data.put("checkPickSensor1", ADH812Port.checkPickSensor1); // 掉货光眼 1
                data.put("checkPickSensor2", ADH812Port.checkPickSensor2); // 掉货光眼 2
                data.put("debugBuffer", ADH812Port.debugBuffer); //ADH812 状态机调试缓存
                Log.d(TAG, "ADH812StateListener: on812StateReturned  --> " + data);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            adh812StateCallback.invokeAndKeepAlive(data);
        }

        /**
         * ADH812 固件程序版本回调
         * @param isRegister:是否注册
         * @param version:ADH812 固件程序版本号
         */
        @Override
        public void onVersionReturn(boolean isRegister, String version) {

        }

        /**
         *  ADH812 层坐标返回数据监听（读取坐标）
         * @param coordinate:坐标数
         */
        @Override
        public void on812CoordinateReturned(int[] coordinate) {
            JSONObject data = new JSONObject();
            try {
                data.put("code", 1);
                data.put("msg", "层坐标返回成功");
                data.put("data", coordinate);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.e(TAG, "--> 层坐标返回成功");
            readCoordinateCallback.invokeAndKeepAlive(data);
        }

        /**
         * ADH812 通讯中断时触发。
         */
        @Override
        public void onADH812Disconnected() {

        }
    };


    // ADH812 板卡升降定位及运行结果监听器
    private final ADH812ResultListener adh812ResultListener = new ADH812ResultListener() {

        /**
         * 812升降梯出货成功
         * @param   aisleNum 货道
         */
        @Override
        public void on812Success(int aisleNum) {
            outGoodsCallback.invokeAndKeepAlive(initJSONObject(1, "升降梯定位成功"));
        }

        /**
         * 812升降梯出货失败
         * @param   aisleNum 货道
         */
        @Override
        public void on812Failure(int aisleNum) {
            outGoodsCallback.invokeAndKeepAlive(initJSONObject(0, "升降梯定位失败"));
        }

        /**
         * 升降梯已到达指定位置
         * 此状态需调用 PortController.outGoods() 控制 ADH815 出货。
         */
        @Override
        public void on812Located() {
            PortController.outGoods(targetAisle, new ResultCallBack() {
                @Override
                public void onSuccess(int i, int i1) {
                    outGoodsCallback.invokeAndKeepAlive(initJSONObject(3, "出货成功"));
                    PortController.outOver("01");
                }

                @Override
                public void onFailure(int i, String s, String s1) {
                    outGoodsCallback.invokeAndKeepAlive(initJSONObject(2, "出货失败"));
                    PortController.outOver("02");

                    int state = PortController.get815State(1);
                }
            });
        }
    };


    @JSMethod(uiThread = true)
    public void hideBottomUIMenu(JSCallback callback) {
        //隐藏底部导航栏
        final View decorView = ((Activity) mWXSDKInstance.getContext()).getWindow().getDecorView();
        @SuppressLint("InlinedApi") final int uiOption = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOption);

        // This code will always hide the navigation bar
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decorView.setSystemUiVisibility(uiOption);
                }
            }
        });
    }

    @JSMethod(uiThread = true)
    public void showBottomUIMenu(JSCallback callback) {
        //隐藏底部导航栏
        final View decorView = ((Activity) mWXSDKInstance.getContext()).getWindow().getDecorView();
        @SuppressLint("InlinedApi") final int uiOption = View.SYSTEM_UI_FLAG_VISIBLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOption);

        // This code will always hide the navigation bar
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decorView.setSystemUiVisibility(uiOption);
                }
            }
        });
    }

    /**
     * 初始化打开串口
     *
     * @param portType "ttyS", "ttyUSB"  串口类型
     * @param port     2 串口号
     * @param baud     38400 波特率
     * @param callback
     */
    @JSMethod(uiThread = true)
    public void initADP(String portType, int port, int baud, JSCallback callback) {
        PortController.enableLog(true);
        PortController.enableLoggers(mWXSDKInstance.getContext(), true);
        PortController.setAdh812ResultListener(adh812ResultListener);
        boolean success = PortController.init(mWXSDKInstance.getContext(), portType, port, baud);
        callback.invoke(initJSONObject(!success ? 0 : 1, !success ? "初始化失败" : "初始化成功"));
    }

    /**
     * 板卡状态监听
     * 1-主柜连接中断，2-副柜1连接中断，3-副柜2连接中断
     *
     * @param callback
     */
    @JSMethod(uiThread = true)
    public void setStateListener(JSCallback callback) {
        PortController.setStateListener(devicesStateListener);
        devStateCallback = callback;
    }

    /**
     * 获取各个板块当前状态
     *
     * @param index    板卡号， 1:主柜驱动板， 2:副柜1驱动板， 3:副柜2驱动板
     * @param callback code --> 0:空闲， 1:正在出货， 2:出货完成状态， -1：无板卡连接
     */
    @JSMethod(uiThread = true)
    public void get815State(int index, JSCallback callback) {
        int state = PortController.get815State(index);
        PortController.enableLoggers(mWXSDKInstance.getContext(), true);
        callback.invoke(initJSONObject(state, "板块状态"));
    }


    /**
     * 812状态监听
     *
     * @param callback
     */
    @JSMethod(uiThread = true)
    public void setAdh812StateListener(JSCallback callback) {
        PortController.setAdh812StateListener(adh812StateListener);
        adh812StateCallback = callback;
    }

    /**
     * 关闭串口
     *
     * @param callback
     */
    @JSMethod(uiThread = true)
    public void closeADP(JSCallback callback) {
        PortController.closeSerialPort();
    }

    /**
     * 启用副柜控制板，默认副柜关
     *
     * @param enable815_2
     * @param enable815_3
     * @param callback
     */
    @JSMethod(uiThread = true)
    public void enableSalve(boolean enable815_2, boolean enable815_3, JSCallback callback) {
        PortController.enableSalve(enable815_2, enable815_3);
    }

    /**
     * 设置815模式：关闭开启掉货检测
     *
     * @param board    主板号，1：主柜，2：副柜1,，3：副柜2；
     * @param mode     运行模式，“00”：三线电机关闭掉货检测；“80”：三线电机开启掉货检测
     * @param callback
     */
    @JSMethod(uiThread = true)
    public void set815Mode(int board, String mode, JSCallback callback) {
        PortController.set815Mode(board, mode);
    }

    /**
     * 815出货(副柜)
     *
     * @param aisle    货道号，从0开始
     * @param callback
     */
    @JSMethod(uiThread = true)
    public void outGoodsSalve(int aisle, final JSCallback callback) {
        if (ADH815Port.ADH815State == -1) {
            callback.invoke(initJSONObject(-1, "通讯异常,初始化未完成"));
            return;
        }
        PortController.outGoods(aisle, new ResultCallBack() {
            @Override
            public void onSuccess(int i, int i1) {
                callback.invoke(initJSONObject(1, "出货成功"));
            }

            @Override
            public void onFailure(int i, String s, String s1) {
                callback.invoke(initJSONObject(0, "出货失败"));
            }
        });
    }
//
//    /**
//     * 升降梯定位
//     *
//     * @param aisle    货道号，从0开始
//     * @param callback
//     */
//    @JSMethod(uiThread = true)
//    public void liftLocation(int aisle, final JSCallback callback) {
//        if (ADH815Port.ADH815State == -1) {
//            callback.invoke(initJSONObject(-1, "通讯异常,初始化未完成"));
//            return;
//        }
//        targetAisle = aisle;
//        if (ADH815Port.ADH815State != 0 || ADH812Port.boardState != 0) {
//            callback.invoke(initJSONObject(-2, "设备正忙"));
//            return;
//        }
//
//        callbackLiftLocation = callback;
//        PortController.liftLocation(aisle);
//    }

    /**
     * 815出货
     *
     * @param aisle    货道号，从0开始
     * @param callback
     */
    @JSMethod(uiThread = true)
    public void outGoods(int aisle, final JSCallback callback) {
        if (ADH815Port.ADH815State == -1) {
            callback.invoke(initJSONObject(-1, "通讯异常,初始化未完成"));
            return;
        }
        targetAisle = aisle;
        if (ADH815Port.ADH815State != 0 || ADH812Port.boardState != 0) {
            callback.invoke(initJSONObject(-2, "设备正忙"));
            return;
        }

        outGoodsCallback = callback;
        PortController.liftLocation(aisle);
    }

    /**
     * 启用812升降梯控制板通讯
     *
     * @param isEnable        开启关闭812
     * @param totalLayerCount 货道总层数
     * @param callback
     */
    @JSMethod(uiThread = true)
    public void enable812(boolean isEnable, int totalLayerCount, JSCallback callback) {
        PortController.enable812(isEnable, totalLayerCount);
    }

    /**
     * 初始化运行812升降梯
     *
     * @param callback
     */
    @JSMethod(uiThread = true)
    public void run(JSCallback callback) {
        PortController.run();
    }

    /**
     * 清除故障
     *
     * @param callback
     */
    @JSMethod(uiThread = true)
    public void clearError(JSCallback callback) {
        PortController.clearError();
    }

    /**
     * 开启或关闭调试模式
     *
     * @param isEnable
     * @param callback
     */
    @JSMethod(uiThread = true)
    public void debugMode(boolean isEnable, JSCallback callback) {
        PortController.debugMode(isEnable);
    }

    /**
     * 设置升降梯各层坐标
     *
     * @param layerNum   带设置坐标的层号（从上到下1到10层）
     * @param coordinate 坐标值（0~65535）
     * @param callback
     */
    @JSMethod(uiThread = true)
    public void setCoordinate(int layerNum, int coordinate, JSCallback callback) {
        PortController.setCoordinate(layerNum, coordinate);
    }

    /**
     * 读取坐标
     *
     * @param callback
     */
    @JSMethod(uiThread = true)
    public void readCoordinate(JSCallback callback) {
        readCoordinateCallback = callback;
        PortController.readCoordinate();
    }

    /**
     * 设置主副柜控制板温控模式及温度
     *
     * @param type     0-主柜，1-副柜1,2-副柜2
     * @param tempMode 温控模式，0-常温，1-制冷，2-制热
     * @param tempVal  温度值
     */
    @JSMethod(uiThread = true)
    public void setTemperature(int type, int tempMode, int tempVal, JSCallback callback) {
        if (type == 0) {
            PortController.setTemperature(tempMode, tempVal);
        } else if (type == 1) {
            PortController.setTemperature2(tempMode, tempVal);
        } else if (type == 2) {
            PortController.setTemperature3(tempMode, tempVal);
        }
    }

    /**
     * @param state -2(设备正忙)  -1(通讯异常,初始化未完成)  0(正常)
     * @param msg
     * @return
     */
    private JSONObject initJSONObject(int state, String msg) {
        JSONObject data = new JSONObject();
        try {
            data.put("code", state);
            data.put("msg", msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return data;
    }


}

