package io.huiy.deemo.printer;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.Spinner;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.common.UniModule;
import io.huiy.deemo.printer.entity.Label;
import io.huiy.deemo.printer.entity.PrintInfo;
import io.huiy.deemo.printer.helpers.Constant;
import io.huiy.deemo.printer.helpers.PrintContent;
import io.huiy.deemo.printer.thread.CheckWifiConnThread;
import io.huiy.deemo.printer.thread.ThreadFactoryBuilder;
import io.huiy.deemo.printer.thread.ThreadPool;
import io.huiy.deemo.printer.utils.Utils;

import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED;
import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED;


/**
 * 传感器插件
 * 用于监听方位（东南西北），并实时返回角度
 */
public class PrinterModule extends UniModule {

    private final String TAG = "PrinterModule";

    private UsbManager usbManager;
    private int counts;
    private Context mContext;

    private String usbName;
    private ThreadPool threadPool;

    //判断打印机所使用指令是否是ESC指令
    private int id = 0;
    private EditText etPrintCounts;
    private Spinner mode_sp;
    private int printcount = 0;
    private boolean continuityprint = false;

    //wifi连接线程监听CheckWifiConnThread
    private CheckWifiConnThread checkWifiConnThread;

    private PendingIntent mPermissionIntent;
    private String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH
    };

    /**
     * 初始化打印机
     */
    private void initPrinter() {
        if (mContext == null || usbManager == null) {
            mContext = mUniSDKInstance.getContext();
            usbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
            initBroadcast();
        }
    }

    /**
     * 获取USB设备列表
     *
     * @return
     */
    public List<Object> getUsbDeviceList() {
        List<Object> devList = new ArrayList<>();
        initPrinter();
        // Get the list of attached devices
        HashMap<String, UsbDevice> devMap = usbManager.getDeviceList();
        Set<String> keys = devMap.keySet();
        Log.d(TAG, "UsbDevice --> count:" + devMap.size());
        if (devMap.size() == 0) {
            Log.d(TAG, "noDevices ---> " + "没有USB设备");
            return devList;
        }

        for (String key : keys) {
            UsbDevice dev = devMap.get(key);
            devList.add(dev.getDeviceName());
            Log.d(TAG, "UsbDevice --> key:" + key + " value:" + dev.toString());
        }
//        Log.v(TAG, "getDevListJson --> " + devList.toString());
        return devList;

    }


    private void connectUsbDevice(String usbName) {
        initPrinter();
        closeport();

        //获取USB设备名
        HashMap<String, UsbDevice> usbDeviceList = usbManager.getDeviceList();
        UsbDevice usbDevice = null;

        if (!TextUtils.isEmpty(usbName)) {
            // 通过USB设备名找到USB设备
            usbDevice = usbDeviceList.get(usbName);
        }

        if (usbDevice == null) {
            for (String key : usbDeviceList.keySet()) {
                Log.d(TAG, "connectUsbDevice  --> key:" + key);
                usbDevice = usbDeviceList.get(key);
            }
        }
        // 判断USB设备是否有权限
        if (usbManager.hasPermission(usbDevice)) {
            Log.d(TAG, "connectUsbDevice ---> devName" + usbDevice.getDeviceName());
            usbConn(usbDevice);
        } else {//请求权限
            mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(Constant.ACTION_USB_PERMISSION), 0);
            usbManager.requestPermission(usbDevice, mPermissionIntent);
        }
    }


    /**
     * 打印标签
     *
     * @param json
     */
    private void printerLabel(final String json) {
        threadPool = ThreadPool.getInstantiation();
        threadPool.addSerialTask(new Runnable() {
            @Override
            public void run() {
                if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null ||
                        !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState()) {
                    mHandler.obtainMessage(Constant.CONN_PRINTER).sendToTarget();
                    return;
                }
                if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == Constant.PrinterCommand.TSC) {
                    if (TextUtils.isEmpty(json)) {
                        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately(PrintContent.getLabel());
                    } else {
                        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately(PrintContent.parseJsonLabel(json));
                    }
                } else {
                    mHandler.obtainMessage(Constant.PRINTER_COMMAND_ERROR).sendToTarget();
                }
            }
        });
    }

    private String getJsonStr() {
        Label label = new Label();

        label.getPrintInfoList().add(PrintInfo.createTextPrint(60, 10, 0, "悠悠奶茶", "TSS24.BF2", 2, 2));
        label.getPrintInfoList().add(PrintInfo.createTextPrint(20, 70, 0, "品名：招牌奶茶", "TSS24.BF2", 1, 1));
        label.getPrintInfoList().add(PrintInfo.createTextPrint(20, 100, 0, "规格：大杯", "TSS24.BF2", 1, 1));
        label.getPrintInfoList().add(PrintInfo.createTextPrint(20, 130, 0, "价格：16元", "TSS24.BF2", 1, 1));

        label.getPrintInfoList().add(PrintInfo.createQrCodePrint(200, 75, 0, "12345678900", "L", 3));
        label.getPrintInfoList().add(PrintInfo.create1DBarcodePrint(30, 160, 0, "12345678900", 1, 50, "128"));
        return JSON.toJSONString(label);
    }


    /**
     * 重新连接回收上次连接的对象，避免内存泄漏
     */
    private void closeport() {
        if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] != null && DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].mPort != null) {
            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].reader.cancel();
            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].mPort.closePort();
            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].mPort = null;
        }
    }

    /**
     * 注册广播
     * Registration broadcast
     */
    private void initBroadcast() {
        IntentFilter filter = new IntentFilter(Constant.ACTION_USB_PERMISSION);//USB访问权限广播
        filter.addAction(Constant.ACTION_CONN_STATE);//与打印机连接状态
        filter.addAction(Constant.ACTION_QUERY_PRINTER_STATE);//查询打印机缓冲区状态广播，用于一票一控
        filter.addAction(ACTION_USB_DEVICE_DETACHED);//USB线拔出
        filter.addAction(ACTION_USB_DEVICE_ATTACHED);//USB线插入
        mContext.registerReceiver(receiver, filter);
    }

    /**
     * usb连接
     *
     * @param usbDevice
     */
    private void usbConn(UsbDevice usbDevice) {
        new DeviceConnFactoryManager.Build()
                .setId(id)
                .setConnMethod(Constant.CONN_METHOD.USB)
                .setUsbDevice(usbDevice)
                .setContext(mContext)
                .build();
        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].openPort();
    }

    /**
     * 获取当前连接设备信息
     *
     * @return
     */
    private String getConnDeviceInfo() {
        String str = "";
        DeviceConnFactoryManager deviceConnFactoryManager = DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id];
        if (deviceConnFactoryManager != null
                && deviceConnFactoryManager.getConnState()) {
            if ("USB".equals(deviceConnFactoryManager.getConnMethod().toString())) {
                str += "USB\n";
                str += "USB Name: " + deviceConnFactoryManager.usbDevice().getDeviceName();
            } else if ("WIFI".equals(deviceConnFactoryManager.getConnMethod().toString())) {
                str += "WIFI\n";
                str += "IP: " + deviceConnFactoryManager.getIp() + "\t";
                str += "Port: " + deviceConnFactoryManager.getPort();
                checkWifiConnThread = new CheckWifiConnThread(deviceConnFactoryManager.getIp(), mHandler);//开启监听WiFi线程
                checkWifiConnThread.start();
            } else if ("BLUETOOTH".equals(deviceConnFactoryManager.getConnMethod().toString())) {
                str += "BLUETOOTH\n";
                str += "MacAddress: " + deviceConnFactoryManager.getMacAddress();
            } else if ("SERIAL_PORT".equals(deviceConnFactoryManager.getConnMethod().toString())) {
                str += "SERIAL_PORT\n";
                str += "Path: " + deviceConnFactoryManager.getSerialPortPath() + "\t";
                str += "Baudrate: " + deviceConnFactoryManager.getBaudrate();
            }
        }
        return str;
    }

    private void sendContinuityPrint() {
        ThreadPool.getInstantiation().addSerialTask(new Runnable() {
            @Override
            public void run() {
                if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] != null
                        && DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState()) {
                    ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder("MainActivity_sendContinuity_Timer");
                    ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1, threadFactoryBuilder);
                    scheduledExecutorService.schedule(threadFactoryBuilder.newThread(new Runnable() {
                        @Override
                        public void run() {
                            counts--;
                            if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == Constant.PrinterCommand.ESC) {
                                DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately(PrintContent.getReceipt());
                            } else if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == Constant.PrinterCommand.TSC) {
                                DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately(PrintContent.getLabel());
                            } else {
                                DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately(PrintContent.getCPCL());
                            }
                        }
                    }), 1000, TimeUnit.MILLISECONDS);
                }
            }
        });
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                //USB请求访问权限
                case Constant.ACTION_USB_PERMISSION:
                    synchronized (this) {
                        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (device != null) {//用户点击授权
                                usbConn(device);
                            }
                        } else {//用户点击不授权,则无权限访问USB
                            Log.e(TAG, "No access to USB");
                        }
                    }
                    break;
                //Usb连接断开广播
                case ACTION_USB_DEVICE_DETACHED:
                    UsbDevice usbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
//                    if (usbDevice.equals(DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].usbDevice())) {
                        mHandler.obtainMessage(Constant.CONN_STATE_DISCONN).sendToTarget();
//                    }
                    break;
                //连接状态
                case Constant.ACTION_CONN_STATE:
                    int state = intent.getIntExtra(Constant.STATE, -1);
                    int deviceId = intent.getIntExtra(Constant.DEVICE_ID, -1);
                    switch (state) {
                        case Constant.CONN_STATE_DISCONNECT:
                            if (id == deviceId) {
                                Log.e(TAG, "CONN_STATE_DISCONNECT -->  连接状态：未连接");
                                globalCallback(1,"连接状态：未连接");
//                                tvConnState.setText(mContext.getString(R.string.str_conn_state_disconnect));
                            }
                            break;
                        case Constant.CONN_STATE_CONNECTING:
                            Log.e(TAG, "CONN_STATE_CONNECTING -->  连接状态：连接中");
                            globalCallback(2,"连接状态：连接中");
//                            tvConnState.setText(mContext.getString(R.string.str_conn_state_connecting));
                            break;
                        case Constant.CONN_STATE_CONNECTED:
                            Log.e(TAG, "CONN_STATE_CONNECTED -->  连接状态：已连接");
                            globalCallback(3,"连接状态：已连接"+ "\n" + getConnDeviceInfo());
//                            tvConnState.setText(mContext.getString(R.string.str_conn_state_connected) + "\n" + getConnDeviceInfo());
                            break;
                        case Constant.CONN_STATE_FAILED:
                            Log.e(TAG, "CONN_STATE_CONNECTING --> 连接失败！");
                            globalCallback(4,"连接失败！");
                            Utils.showLong("连接失败！");
                            //wificonn=false;
//                            tvConnState.setText(mContext.getString(R.string.str_conn_state_disconnect));
                            break;
                        default:
                            break;
                    }
                    break;
                //连续打印，一票一控，防止打印机乱码
                case Constant.ACTION_QUERY_PRINTER_STATE:
                    if (counts >= 0) {
                        if (continuityprint) {
                            printcount++;
                            Utils.showLong("连续打印: " + printcount);
                        }
                        if (counts != 0) {
                            sendContinuityPrint();
                        } else {
                            continuityprint = false;
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            handleMsg(msg);
        }
    };

    private void handleMsg(Message msg) {
        switch (msg.what) {
            case Constant.CONN_STATE_DISCONN://断开连接
                DeviceConnFactoryManager deviceConnFactoryManager = DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id];
                if (deviceConnFactoryManager != null && deviceConnFactoryManager.getConnState()) {
                    DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].closePort(id);
                    Utils.showLong("成功断开连接");
                }
                break;
            case Constant.PRINTER_COMMAND_ERROR://打印机指令错误
                Utils.showLong("请选择正确的打印机指令");
                break;
            case Constant.CONN_PRINTER://未连接打印机
                Utils.showLong("请先连接打印机");
                break;
            case Constant.MESSAGE_UPDATE_PARAMETER:
                String strIp = msg.getData().getString("Ip");
                String strPort = msg.getData().getString("Port");
                //初始化端口信息
                new DeviceConnFactoryManager.Build()
                        //设置端口连接方式
                        .setConnMethod(Constant.CONN_METHOD.WIFI)
                        //设置端口IP地址
                        .setIp(strIp)
                        //设置端口ID（主要用于连接多设备）
                        .setId(id)
                        //设置连接的热点端口号
                        .setPort(Integer.parseInt(strPort))
                        .build();
                threadPool = ThreadPool.getInstantiation();
                threadPool.addSerialTask(new Runnable() {
                    @Override
                    public void run() {
                        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].openPort();
                    }
                });
                break;
            case Constant.PING_SUCCESS://WIfi连接成功\
                Log.e(TAG, "wifi connect success!");
                break;
            case Constant.PING_FAIL://WIfI断开连接
                Log.e(TAG, "wifi connect fail!");
                Utils.showLong("断开连接");
                checkWifiConnThread.cancel();
                checkWifiConnThread = null;
                mHandler.obtainMessage(Constant.CONN_STATE_DISCONN).sendToTarget();
                break;
            case Constant.tip:
                String str = (String) msg.obj;
                Utils.showLong(str);
                break;
            default:
                new DeviceConnFactoryManager.Build()
                        //设置端口连接方式
                        .setConnMethod(Constant.CONN_METHOD.WIFI)
                        //设置端口IP地址
                        .setIp("192.168.2.227")
                        //设置端口ID（主要用于连接多设备）
                        .setId(id)
                        //设置连接的热点端口号
                        .setPort(9100)
                        .build();
                threadPool.addSerialTask(new Runnable() {
                    @Override
                    public void run() {
                        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].openPort();
                    }
                });
                break;
        }
    }

//    @UniJSMethod(uiThread = false)
//    public JSONObject initPrinterFunc() {
//        Log.e(TAG, "initPrinterFunc --> ");
//        initPrinter();
//
//        JSONObject data = new JSONObject();
//        data.put("code", "success");
//        data.put("action", "initPrinterFunc");
//        return data;
//    }

    @UniJSMethod(uiThread = false)
    public JSONObject getUsbDevListFunc() {
        Log.e(TAG, "getUsbDevicesFunc --> ");
        List<Object> devNames = getUsbDeviceList();

        JSONObject data = new JSONObject();
        data.put("code", "success");
        data.put("action", "getUsbDevicesFunc");
        JSONArray arr = new JSONArray(devNames);
        data.put("list", arr);
        return data;
    }

    @UniJSMethod(uiThread = false)
    public JSONObject connectUsbDevFunc(JSONObject options) {
        Log.e(TAG, "connectUsbDevFunc -->" + options);
        connectUsbDevice(options.getString("name"));

        JSONObject data = new JSONObject();
        data.put("code", "success");
        data.put("action", "connectUsbDeviceAFunc");
        return data;
    }

    @UniJSMethod(uiThread = false)
    public JSONObject printerLabelFunc(JSONObject options) {
        Log.e(TAG, "printerLabelFunc -->" + options);
        printerLabel(options.toString());

        JSONObject data = new JSONObject();
        data.put("code", "success");
        data.put("action", "printerLabelFunc");
        return data;
    }

    private void globalCallback(int type, String msg) {
        Map<String, Object> params = new HashMap<>();
        params.put("type", type);
        params.put("msg", msg);
        Log.v("globalCallback --> ", " msg:" + msg);
        mUniSDKInstance.fireGlobalEventCallback("printerEvent", params);
    }


}
