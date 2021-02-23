package io.huiy.deemo.scanner;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.zebra.scannercontrol.DCSSDKDefs;
import com.zebra.scannercontrol.DCSScannerInfo;
import com.zebra.scannercontrol.FirmwareUpdateEvent;
import com.zebra.scannercontrol.IDcsSdkApiDelegate;
import com.zebra.scannercontrol.SDKHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniModule;
import io.huiy.deemo.scanner.helpers.AvailableScanner;
import io.huiy.deemo.scanner.helpers.Barcode;
import io.huiy.deemo.scanner.helpers.Constants;
import io.huiy.deemo.scanner.helpers.ScannerAppEngine;

/**
 * 扫码枪插件
 * 可以用扫码枪识别条形码，二维码
 */
public class ScannerModule extends UniModule {

    String TAG = "ScannerModule";
    private Context mContext;
    public static SDKHandler sdkHandler;

    private static ArrayList<DCSScannerInfo> mScannerInfoList = new ArrayList<>();
    private static ArrayList<DCSScannerInfo> mOfflineScannerInfoList = new ArrayList<>();
    private static ArrayList<DCSScannerInfo> mSNAPIList = new ArrayList<>();

    public static boolean MOT_SETTING_NOTIFICATION_ACTIVE;
    public static ArrayList<Barcode> barcodeData = new ArrayList<>();
    public static int minScreenWidth = 360;

    public static boolean isAnyScannerConnected = false;    // 当前是否有连接的设备
    public static int currentConnectedScannerID = -1;       // 当前连接的设备id
    public static DCSScannerInfo currentConnectedScanner;   // 当前连接的设备
    public static DCSScannerInfo lastConnectedScanner;      // 上一次连接的设备
    public static AvailableScanner curAvailableScanner = null;

    private static final int PERMISSIONS_ACCESS_COARSE_LOCATION = 10;


    IDcsSdkApiDelegate sdkApiDelegate = new IDcsSdkApiDelegate() {
        @Override
        public void dcssdkEventScannerAppeared(DCSScannerInfo availableScanner) {
            // 扫描到设备
            Log.i(TAG, "dcssdkEventScannerAppeared --> " + availableScanner);
            dataHandler.obtainMessage(Constants.SCANNER_APPEARED, availableScanner).sendToTarget();
        }

        @Override
        public void dcssdkEventScannerDisappeared(int scannerID) {
            // 设备消失
            Log.i(TAG, "dcssdkEventScannerDisappeared --> " + scannerID);
            dataHandler.obtainMessage(Constants.SCANNER_DISAPPEARED, scannerID).sendToTarget();
        }

        @Override
        public void dcssdkEventCommunicationSessionEstablished(DCSScannerInfo activeScanner) {
            // 与设备建立连接
            Log.i(TAG, "dcssdkEventCommunicationSessionEstablished --> " + activeScanner);
            dataHandler.obtainMessage(Constants.SESSION_ESTABLISHED, activeScanner).sendToTarget();
        }

        @Override
        public void dcssdkEventCommunicationSessionTerminated(int scannerID) {
            // 与设备断开连接
            Log.i(TAG, "dcssdkEventCommunicationSessionTerminated --> " + scannerID);
            dataHandler.obtainMessage(Constants.SESSION_TERMINATED, scannerID).sendToTarget();
        }

        @Override
        public void dcssdkEventBarcode(byte[] barcodeData, int barcodeType, int fromScannerID) {
            // 扫描到条形码
            Barcode barcode = new Barcode(barcodeData, barcodeType, fromScannerID);
            Log.i(TAG, "dcssdkEventBarcode --> " + fromScannerID + " " + new String(barcodeData));
            dataHandler.obtainMessage(Constants.BARCODE_RECEIVED, barcode).sendToTarget();
        }

        @Override
        public void dcssdkEventImage(byte[] imageData, int fromScannerID) {
            Log.i(TAG, "dcssdkEventImage --> " + fromScannerID);
            dataHandler.obtainMessage(Constants.IMAGE_RECEIVED, imageData).sendToTarget();
        }

        @Override
        public void dcssdkEventVideo(byte[] videoFrame, int fromScannerID) {
            Log.i(TAG, "dcssdkEventVideo --> " + fromScannerID);
            dataHandler.obtainMessage(Constants.VIDEO_RECEIVED, videoFrame).sendToTarget();
        }

        @Override
        public void dcssdkEventBinaryData(byte[] binaryData, int fromScannerID) {
            Log.i(TAG, "dcssdkEventBinaryData --> " + fromScannerID);
            Log.d(TAG, "BinaryData Event received no.of bytes : " + binaryData.length + " for Scanner ID : " + fromScannerID);
        }

        @Override
        public void dcssdkEventFirmwareUpdate(FirmwareUpdateEvent firmwareUpdateEvent) {
            // 设备需要更新
        }

        @Override
        public void dcssdkEventAuxScannerAppeared(DCSScannerInfo newTopology, DCSScannerInfo auxScanner) {
            Log.i(TAG, "dcssdkEventAuxScannerAppeared --> " + auxScanner);
            dataHandler.obtainMessage(Constants.AUX_SCANNER_CONNECTED, auxScanner).sendToTarget();
        }
    };

    ScannerAppEngine.IScannerAppEngineDevConnectionsDelegate mDevConnDelegate = new ScannerAppEngine.IScannerAppEngineDevConnectionsDelegate() {
        @Override
        public boolean scannerHasAppeared(int scannerID) {
            // 扫码枪出现时触发
            Log.d(TAG, "scannerHasAppeared  --> " + scannerID);
            globalCallback(scannerID, Constants.SCANNER_APPEARED, "发现新设备", "", -1);
            return false;
        }

        @Override
        public boolean scannerHasDisappeared(int scannerID) {
            // 扫码枪消失时触发
            Log.d(TAG, "scannerHasDisappeared  --> " + scannerID);
            globalCallback(scannerID, Constants.SCANNER_DISAPPEARED, "设备消失", "", -1);
            return false;
        }

        @Override
        public boolean scannerHasConnected(int scannerID) {
            // 扫码枪已连接（返回设备id）
            Log.d(TAG, "scannerHasConnected  --> " + scannerID);
            ArrayList<DCSScannerInfo> activeScanners = new ArrayList<DCSScannerInfo>();
            sdkHandler.dcssdkGetActiveScannersList(activeScanners);
            for (DCSScannerInfo scannerInfo : mScannerInfoList) {
                if (scannerInfo.getScannerID() == scannerID) {
                    isAnyScannerConnected = true;
                    currentConnectedScannerID = scannerID;
                    currentConnectedScanner = scannerInfo;
                    lastConnectedScanner = currentConnectedScanner;
                }
            }

            // 处理连接方法的回调
            if (callbackMap.get(Constants.CALL_BACK_CONNECT) != null) {
                JSONObject data = new JSONObject();
                data.put("code", "success");
                data.put("action", "connect");
                data.put("msg", "设备已连接");
                callbackMap.get(Constants.CALL_BACK_CONNECT).invoke(data);
            }

            // 回调通知uniApp
            globalCallback(scannerID, Constants.SESSION_ESTABLISHED, "设备已连接", "", -1);
            return true;
        }

        @Override
        public boolean scannerHasDisconnected(int scannerID) {
            // 扫码枪已断开连接（返回设备id）
            Log.d(TAG, "scannerHasDisconnected  --> " + scannerID);
            isAnyScannerConnected = false;
            currentConnectedScannerID = -1;
            lastConnectedScanner = currentConnectedScanner;
            currentConnectedScanner = null;

            // 处理连接方法的回调
            if (callbackMap.get(Constants.CALL_BACK_DISCONNECT) != null) {
                JSONObject data = new JSONObject();
                data.put("code", "success");
                data.put("action", "disconnect");
                data.put("msg", "设备已断开连接");
                callbackMap.get(Constants.CALL_BACK_DISCONNECT).invoke(data);
            }

            globalCallback(scannerID, Constants.SESSION_TERMINATED, "设备断开连接", "", -1);
            return true;
        }
    };

    ScannerAppEngine.IScannerAppEngineDevEventsDelegate mDevEventsDelegate = new ScannerAppEngine.IScannerAppEngineDevEventsDelegate() {
        @Override
        public void scannerBarcodeEvent(byte[] barcodeData, int barcodeType, int scannerID) {
            // 扫描到二维码时
            Log.i(TAG, "scannerBarcodeEvent --> scannerID:" + scannerID +
                    " barcodeType:" + barcodeType + " barcodeData:" + new String(barcodeData));
            globalCallback(scannerID, Constants.BARCODE_RECEIVED, "扫描到二维码", new String(barcodeData), barcodeType);
        }

        @Override
        public void scannerFirmwareUpdateEvent(FirmwareUpdateEvent firmwareUpdateEvent) {
            // 软件有更新时
            Log.i(TAG, "scannerFirmwareUpdateEvent --> ");
        }

        @Override
        public void scannerImageEvent(byte[] imageData) {
            // 扫描到图片时
            Log.i(TAG, "scannerImageEvent --> " + imageData.length);
        }

        @Override
        public void scannerVideoEvent(byte[] videoData) {
            // 扫描到视频时
            Log.i(TAG, "scannerVideoEvent --> " + videoData.length);
        }
    };

    ScannerAppEngine.IScannerAppEngineDevListDelegate mDevListDelegate = new ScannerAppEngine.IScannerAppEngineDevListDelegate() {
        @Override
        public boolean scannersListHasBeenUpdated() {
            // 设备列表有更新时
            Log.i(TAG, "scannersListHasBeenUpdated --> ");
//            globalCallback(0, Constants.SCANNERS_HAS_UPDATED, "设备列表有更新", "", -1);
            return false;
        }
    };


    /**
     * Receiver to handle the events about RFID Reader
     * 接收器处理有关RFID读取器的事件
     */
    private BroadcastReceiver onNotification = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent i) {

            //Since the application is in foreground, show a dialog.
            Toast.makeText(ctxt, i.getStringExtra(Constants.NOTIFICATIONS_TEXT), Toast.LENGTH_SHORT).show();

            //Abort the broadcast since it has been handled.
            abortBroadcast();
        }
    };


    private void initScanner() {
        mContext = mUniSDKInstance.getContext();
        // onCreate
        if (sdkHandler == null) {
            sdkHandler = new SDKHandler(mContext, true);
        }
        sdkHandler.dcssdkSetDelegate(sdkApiDelegate);

        int notifications_mask = 0;
        notifications_mask |= (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_ESTABLISHMENT.value | DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_TERMINATION.value);
        notifications_mask |= (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_APPEARANCE.value | DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_DISAPPEARANCE.value);
        notifications_mask |= (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BARCODE.value);
        notifications_mask |= (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_IMAGE.value);
        notifications_mask |= (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_VIDEO.value);
        notifications_mask |= (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BINARY_DATA.value);
        sdkHandler.dcssdkSubsribeForEvents(notifications_mask);

        mSNAPIList.clear();
        updateScannersList();
        for (DCSScannerInfo device : mScannerInfoList) {
            if (device.getConnectionType() == DCSSDKDefs.DCSSDK_CONN_TYPES.DCSSDK_CONNTYPE_USB_SNAPI) {
                mSNAPIList.add(device);
            }
        }

        // onResume
        IntentFilter filter = new IntentFilter(Constants.ACTION_SCANNER_CONNECTED);
        filter.addAction(Constants.ACTION_SCANNER_DISCONNECTED);
        filter.addAction(Constants.ACTION_SCANNER_AVAILABLE);
        filter.addAction(Constants.ACTION_SCANNER_CONN_FAILED);

        //Use a positive priority
        filter.setPriority(2);
        mContext.registerReceiver(onNotification, filter);
        TAG = getClass().getSimpleName();

        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // No explanation needed, we can request the permission.
            // 没有权限，去请求权限
            ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_ACCESS_COARSE_LOCATION);
        } else {
            initialize();
        }
    }

    private void initialize() {
        sdkHandler.dcssdkEnableAvailableScannersDetection(true);
        sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_BT_NORMAL);
        sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_SNAPI);
        sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_BT_LE);

//        llBarcode = (FrameLayout) findViewById(R.id.scan_to_connect_barcode);
        broadcastSCAisListening();

        if (callbackMap.get(Constants.CALL_BACK_INIT) != null) {
            JSONObject data = new JSONObject();
            data.put("code", "success");
            data.put("action", "initialize");
            callbackMap.get(Constants.CALL_BACK_INIT).invoke(data);
        }
    }

    private void broadcastSCAisListening() {
        Intent intent = new Intent();
        intent.setAction("com.zebra.scannercontrol.LISTENING_STARTED");
        mContext.sendBroadcast(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_ACCESS_COARSE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 请求获取权限成功，去初始化
                initialize();
            } else {
                // 没有权限
                globalCallback(-1, Constants.NO_PERMISSIONS, "没有权限！", "", -1);
            }
        }
    }

//    public void addDevConnectionsDelegate(ScannerAppEngine.IScannerAppEngineDevConnectionsDelegate delegate) {
//        if (mDevConnDelegates == null)
//            mDevConnDelegates = new ArrayList<>();
//        mDevConnDelegates.add(delegate);
//        Log.w(TAG, "addDevConnectionsDelegate --> " + mDevListDelegates);
//    }

    public void updateScannersList() {
        if (sdkHandler != null && mScannerInfoList != null) {
            mScannerInfoList.clear();
            ArrayList<DCSScannerInfo> scannerTreeList = new ArrayList<DCSScannerInfo>();
            sdkHandler.dcssdkGetAvailableScannersList(scannerTreeList);
            sdkHandler.dcssdkGetActiveScannersList(scannerTreeList);
            createFlatScannerList(scannerTreeList);
        }
    }

    private void createFlatScannerList(ArrayList<DCSScannerInfo> scannerTreeList) {
        for (DCSScannerInfo s : scannerTreeList) {
            addToScannerList(s);
        }
    }

    private void addToScannerList(DCSScannerInfo s) {
        mScannerInfoList.add(s);
        if (s.getAuxiliaryScanners() != null) {
            for (DCSScannerInfo aux : s.getAuxiliaryScanners().values()) {
                addToScannerList(aux);
            }
        }
    }

    public DCSSDKDefs.DCSSDK_RESULT connect(int scannerId) {
        if (sdkHandler != null) {
            if (curAvailableScanner != null) {
                // 优先连接可用的设备
                sdkHandler.dcssdkTerminateCommunicationSession(curAvailableScanner.getScannerId());
            }
            // 再连接选中的设备
            return sdkHandler.dcssdkEstablishCommunicationSession(scannerId);
        } else {
            return DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE;
        }
    }

    public void disconnect(int scannerId) {
        if (sdkHandler != null) {
            DCSSDKDefs.DCSSDK_RESULT ret = sdkHandler.dcssdkTerminateCommunicationSession(scannerId);
            curAvailableScanner = null;
            updateScannersList();
        }
    }

    public DCSSDKDefs.DCSSDK_RESULT setAutoReconnectOption(int scannerId, boolean enable) {
        DCSSDKDefs.DCSSDK_RESULT ret;
        if (sdkHandler != null) {
            ret = sdkHandler.dcssdkEnableAutomaticSessionReestablishment(enable, scannerId);
            return ret;
        }
        return DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_FAILURE;
    }


    boolean notificaton_processed = false;
    boolean result = false;
    boolean found = false;
    protected Handler dataHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            handleMsg(msg);
        }
    };

    private void handleMsg(Message msg) {
        switch (msg.what) {
            case Constants.IMAGE_RECEIVED:
                Log.d(TAG, "Image Received");
                byte[] imageData = (byte[]) msg.obj;
                //Barcode barcode=(Barcode)msg.obj;
                //barcodeData.add(barcode);
                if (mDevEventsDelegate != null) {
                    Log.d(TAG, "Show Image Received");
                    mDevEventsDelegate.scannerImageEvent(imageData);
                }
                break;
            case Constants.VIDEO_RECEIVED:
                Log.d(TAG, "Video Received");
                byte[] videoEvent = (byte[]) msg.obj;
                if (mDevEventsDelegate != null) {
                    Log.d(TAG, "Show Video Received");
                    mDevEventsDelegate.scannerVideoEvent(videoEvent);
                }
                //Toast.makeText(getApplicationContext(),"Image event received 000000000",Toast.LENGTH_SHORT).show();
                break;
            case Constants.FW_UPDATE_EVENT:
                Log.d(TAG, "FW_UPDATE_EVENT Received. Client count = ");
//                    Log.d(TAG, "FW_UPDATE_EVENT Received. Client count = " + mDevEventsDelegates.size());
                FirmwareUpdateEvent firmwareUpdateEvent = (FirmwareUpdateEvent) msg.obj;
                if (mDevEventsDelegate != null) {
                    Log.d(TAG, "Show FW_UPDATE_EVENT Received");
                    mDevEventsDelegate.scannerFirmwareUpdateEvent(firmwareUpdateEvent);
                }
                break;
            case Constants.BARCODE_RECEIVED:
                Log.d(TAG, "--> Barcode Received");
                Barcode barcode = (Barcode) msg.obj;
                barcodeData.add(barcode);
                if (mDevEventsDelegate != null) {
                    Log.d(TAG, "--> Show Barcode Received");
                    mDevEventsDelegate.scannerBarcodeEvent(barcode.getBarcodeData(), barcode.getBarcodeType(), barcode.getFromScannerID());
                }
                break;
            case Constants.SESSION_ESTABLISHED:
                Log.d(TAG, "--> SESSION ESTABLISHED");
                DCSScannerInfo activeScanner = (DCSScannerInfo) msg.obj;
                notificaton_processed = false;
                curAvailableScanner = new AvailableScanner(activeScanner);
                curAvailableScanner.setConnected(true);
                DCSSDKDefs.DCSSDK_RESULT dcssdk_result = setAutoReconnectOption(activeScanner.getScannerID(), true);
                /* notify connections delegates */
                if (mDevConnDelegate != null) {
                    result = mDevConnDelegate.scannerHasConnected(activeScanner.getScannerID());
                }
                /* update dev list */
                found = false;
                if (mScannerInfoList != null) {
                    for (DCSScannerInfo ex_info : mScannerInfoList) {
                        if (ex_info.getScannerID() == activeScanner.getScannerID()) {
                            mScannerInfoList.remove(ex_info);
                            barcodeData.clear();
                            found = true;
                            break;
                        }
                    }
                }

                if (mOfflineScannerInfoList != null) {
                    for (DCSScannerInfo off_info : mOfflineScannerInfoList) {
                        if (off_info.getScannerID() == activeScanner.getScannerID()) {
                            mOfflineScannerInfoList.remove(off_info);
                            break;
                        }
                    }
                }

                if (mScannerInfoList != null)
                    mScannerInfoList.add(activeScanner);

                /* notify dev list delegates */
                if (mDevListDelegate != null) {
                    result = mDevListDelegate.scannersListHasBeenUpdated();
                }
                break;
            case Constants.SESSION_TERMINATED:
                Log.d(TAG, "--> SESSION  TERMINATED");
                int scannerID = (Integer) msg.obj;
                String scannerName = "";

                /* notify connections delegates */
                if (mDevConnDelegate != null) {
                    result = mDevConnDelegate.scannerHasDisconnected(scannerID);
                }

                DCSScannerInfo scannerInfo = getScannerByID(scannerID);
                mOfflineScannerInfoList.add(scannerInfo);
                if (scannerInfo != null) {
                    scannerName = scannerInfo.getScannerName();
                    curAvailableScanner = null;
                }
                updateScannersList();

                /* notify dev list delegates */
                if (mDevListDelegate != null) {
                    result = mDevListDelegate.scannersListHasBeenUpdated();
                }
                break;
            case Constants.SCANNER_APPEARED:
                Log.d(TAG, "-->   SCANNER_APPEARED");
            case Constants.AUX_SCANNER_CONNECTED:
                Log.d(TAG, "-->   AUX_SCANNER_CONNECTED");
                notificaton_processed = false;
                DCSScannerInfo availableScanner = (DCSScannerInfo) msg.obj;

                /* notify connections delegates */
                if (mDevConnDelegate != null) {
                    result = mDevConnDelegate.scannerHasAppeared(availableScanner.getScannerID());
                }

                /* update dev list */
                for (DCSScannerInfo ex_info : mScannerInfoList) {
                    if (ex_info.getScannerID() == availableScanner.getScannerID()) {
                        mScannerInfoList.remove(ex_info);
                        break;
                    }
                }

                mScannerInfoList.add(availableScanner);

                /* notify dev list delegates */
                if (mDevListDelegate != null) {
                    result = mDevListDelegate.scannersListHasBeenUpdated();
                }
                break;
            case Constants.SCANNER_DISAPPEARED:
                Log.d(TAG, "-->   SCANNER_DISAPPEARED");
                notificaton_processed = false;
                scannerID = (Integer) msg.obj;
                scannerName = "";
                /* notify connections delegates */
                if (mDevConnDelegate != null) {
                    result = mDevConnDelegate.scannerHasDisappeared(scannerID);
                }

                /* update dev list */
                found = false;
                for (DCSScannerInfo ex_info : mScannerInfoList) {
                    if (ex_info.getScannerID() == scannerID) {
                        /* find scanner with ID in dev list */
                        mScannerInfoList.remove(ex_info);
                        scannerName = ex_info.getScannerName();
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    for (DCSScannerInfo off_info : mOfflineScannerInfoList) {
                        if (off_info.getScannerID() == scannerID) {
                            scannerName = off_info.getScannerName();
                            break;
                        }
                    }
                    Log.d(TAG, "ScannerAppEngine:dcssdkEventScannerDisappeared: scanner is not in list");
                }

                /* notify dev list delegates */
                if (mDevListDelegate != null) {
                    result = mDevListDelegate.scannersListHasBeenUpdated();
                }
                if ((curAvailableScanner != null) && (scannerID == curAvailableScanner.getScannerId())) {
                    curAvailableScanner = null;
                }
                break;
        }
    }

    public DCSScannerInfo getScannerByID(int scannerId) {
        if (mScannerInfoList != null) {
            for (DCSScannerInfo scannerInfo : mScannerInfoList) {
                if (scannerInfo != null && scannerInfo.getScannerID() == scannerId)
                    return scannerInfo;
            }
        }
        return null;
    }

    private Map<String, UniJSCallback> callbackMap = new HashMap<>();


    @UniJSMethod(uiThread = true)
    public void initScannerAFunc(UniJSCallback callback) {
        Log.e(TAG, "initScannerFunc -->" );
        callbackMap.put(Constants.CALL_BACK_INIT, callback);
        initScanner();
    }

    @UniJSMethod(uiThread = true)
    public void connectScannerAFunc(UniJSCallback callback) {
        Log.e(TAG, "connect ScannerFunc -->" );
        callbackMap.put(Constants.CALL_BACK_CONNECT, callback);
        if (mScannerInfoList != null && mScannerInfoList.size() > 0) {
            connect(mScannerInfoList.get(0).getScannerID());
        } else {
            if (callbackMap.get(Constants.CALL_BACK_CONNECT) != null) {
                JSONObject data = new JSONObject();
                data.put("code", "fail");
                data.put("action", "connect");
                data.put("msg", "Scanner list is empty");
                callbackMap.get(Constants.CALL_BACK_CONNECT).invoke(data);
            }
        }
    }

    @UniJSMethod(uiThread = true)
    public void disconnectScannerAFunc(UniJSCallback callback) {
        Log.e(TAG, "disconnect ScannerFunc -->");
        callbackMap.put(Constants.CALL_BACK_DISCONNECT, callback);
        if (currentConnectedScannerID >= 0) {
            disconnect(currentConnectedScannerID);
        } else {
            if (callbackMap.get(Constants.CALL_BACK_DISCONNECT) != null) {
                JSONObject data = new JSONObject();
                data.put("code", "fail");
                data.put("action", "connect");
                data.put("msg", "no Scanner connected");
                callbackMap.get(Constants.CALL_BACK_CONNECT).invoke(data);
            }
        }
    }

    /**
     * 回调通知 UniApp
     *
     * @param scannerID 设备id
     * @param type      消息类型
     * @param msg       消息内容
     * @param barcode   扫码内容
     * @param barType   二维码类型
     */
    private void globalCallback(int scannerID, int type, String msg, String barcode, int barType) {
        Map<String, Object> params = new HashMap<>();
        params.put("scannerId", scannerID);
        params.put("type", type);
        params.put("msg", msg);
        params.put("barcode", barcode);
        params.put("barType", barType);
        Log.v("globalCallback --> ", " barcode:" + barcode);
        mUniSDKInstance.fireGlobalEventCallback("scannerEvent", params);
    }

}
