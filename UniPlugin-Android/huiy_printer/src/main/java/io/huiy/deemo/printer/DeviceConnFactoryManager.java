package io.huiy.deemo.printer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.gprinter.io.BluetoothPort;
import com.gprinter.io.EthernetPort;
import com.gprinter.io.PortManager;
import com.gprinter.io.SerialPort;
import com.gprinter.io.UsbPort;

import java.io.IOException;
import java.util.Vector;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.huiy.deemo.printer.helpers.Constant;
import io.huiy.deemo.printer.thread.ThreadFactoryBuilder;
import io.huiy.deemo.printer.thread.ThreadPool;
import io.huiy.deemo.printer.utils.Utils;


/**
 * Created by Administrator
 *
 * @author 猿史森林
 * Time 2017/8/2
 */
public class DeviceConnFactoryManager {

    private static final String TAG = DeviceConnFactoryManager.class.getSimpleName();
    public PortManager mPort;
    public Constant.CONN_METHOD connMethod;

    private String ip;
    private int port;
    private String macAddress;
    private UsbDevice mUsbDevice;

    private String serialPortPath;
    private int baudrate;
    private int id;

    private static DeviceConnFactoryManager[] deviceConnFactoryManagers = new DeviceConnFactoryManager[4];
    private boolean isOpenPort;

    private byte[] sendCommand;
    private Constant.PrinterCommand currentPrinterCommand;   // 判断打印机所使用指令是否是ESC指令
    public PrinterReader reader;
    private int queryPrinterCommandFlag;

    public static DeviceConnFactoryManager[] getDeviceConnFactoryManagers() {
        return deviceConnFactoryManagers;
    }

    /**
     * 打开端口
     *
     * @return
     */
    public void openPort() {
        deviceConnFactoryManagers[id].isOpenPort = false;
        sendStateBroadcast(Constant.CONN_STATE_CONNECTING);
        switch (deviceConnFactoryManagers[id].connMethod) {
            case BLUETOOTH:
                mPort = new BluetoothPort(macAddress);
                isOpenPort = deviceConnFactoryManagers[id].mPort.openPort();
                break;
            case USB:
                mPort = new UsbPort(Utils.getApp(), mUsbDevice);
                isOpenPort = mPort.openPort();
                break;
            case WIFI:
                mPort = new EthernetPort(ip, port);
                isOpenPort = mPort.openPort();
                break;
            case SERIAL_PORT:
                mPort = new SerialPort(serialPortPath, baudrate, 0);
                isOpenPort = mPort.openPort();
                break;
            default:
                break;
        }

        //端口打开成功后，检查连接打印机所使用的打印机指令ESC、TSC
        if (isOpenPort) {
            queryCommand();
        } else {
            if (this.mPort != null) {
                this.mPort = null;
            }
            sendStateBroadcast(Constant.CONN_STATE_FAILED);
        }
    }

    /**
     * 查询当前连接打印机所使用打印机指令（ESC（EscCommand.java）、TSC（LabelCommand.java））
     */
    private void queryCommand() {
        //开启读取打印机返回数据线程
        reader = new PrinterReader();
        reader.start(); //读取数据线程
        //查询打印机所使用指令
        queryPrinterCommand(); //小票机连接不上  注释这行，添加下面那三行代码。使用ESC指令
//        sendCommand=esc;
//        currentPrinterCommand = PrinterCommand.ESC;
//        sendStateBroadcast(CONN_STATE_CONNECTED);
    }

    /**
     * 获取端口连接方式
     *
     * @return
     */
    public Constant.CONN_METHOD getConnMethod() {
        return connMethod;
    }

    /**
     * 获取端口打开状态（true 打开，false 未打开）
     *
     * @return
     */
    public boolean getConnState() {
        return isOpenPort;
    }

    /**
     * 获取连接蓝牙的物理地址
     *
     * @return
     */
    public String getMacAddress() {
        return macAddress;
    }

    /**
     * 获取连接网口端口号
     *
     * @return
     */
    public int getPort() {
        return port;
    }

    /**
     * 获取连接网口的IP
     *
     * @return
     */
    public String getIp() {
        return ip;
    }

    /**
     * 获取连接的USB设备信息
     *
     * @return
     */
    public UsbDevice usbDevice() {
        return mUsbDevice;
    }

    /**
     * 关闭端口
     */
    public void closePort(int id) {
        if (this.mPort != null) {
            if (reader != null) {
                reader.cancel();
                reader = null;
            }
            boolean b = this.mPort.closePort();
            if (b) {
                this.mPort = null;
                isOpenPort = false;
                currentPrinterCommand = null;
            }
        }
        sendStateBroadcast(Constant.CONN_STATE_DISCONNECT);
    }

    /**
     * 获取串口号
     *
     * @return
     */
    public String getSerialPortPath() {
        return serialPortPath;
    }

    /**
     * 获取波特率
     *
     * @return
     */
    public int getBaudrate() {
        return baudrate;
    }

    public static void closeAllPort() {
        for (DeviceConnFactoryManager deviceConnFactoryManager : deviceConnFactoryManagers) {
            if (deviceConnFactoryManager != null) {
                Log.e(TAG, "cloaseAllPort() id -> " + deviceConnFactoryManager.id);
                deviceConnFactoryManager.closePort(deviceConnFactoryManager.id);
                deviceConnFactoryManagers[deviceConnFactoryManager.id] = null;
            }
        }
    }

    private DeviceConnFactoryManager(Build build) {
        this.connMethod = build.connMethod;
        this.macAddress = build.macAddress;
        this.port = build.port;
        this.ip = build.ip;
        this.mUsbDevice = build.usbDevice;
        this.serialPortPath = build.serialPortPath;
        this.baudrate = build.baudrate;
        this.id = build.id;
        deviceConnFactoryManagers[id] = this;
    }

    /**
     * 获取当前打印机指令
     *
     * @return PrinterCommand
     */
    public Constant.PrinterCommand getCurrentPrinterCommand() {
        return deviceConnFactoryManagers[id].currentPrinterCommand;
    }

    public static final class Build {
        private String ip;
        private String macAddress;
        private UsbDevice usbDevice;
        private int port;
        private Constant.CONN_METHOD connMethod;
        private Context context;
        private String serialPortPath;
        private int baudrate;
        private int id;

        public DeviceConnFactoryManager.Build setIp(String ip) {
            this.ip = ip;
            return this;
        }

        public DeviceConnFactoryManager.Build setMacAddress(String macAddress) {
            this.macAddress = macAddress;
            return this;
        }

        public DeviceConnFactoryManager.Build setUsbDevice(UsbDevice usbDevice) {
            this.usbDevice = usbDevice;
            return this;
        }

        public DeviceConnFactoryManager.Build setPort(int port) {
            this.port = port;
            return this;
        }

        public DeviceConnFactoryManager.Build setConnMethod(Constant.CONN_METHOD connMethod) {
            this.connMethod = connMethod;
            return this;
        }

        public DeviceConnFactoryManager.Build setContext(Context context) {
            this.context = context;
            return this;
        }

        public DeviceConnFactoryManager.Build setId(int id) {
            this.id = id;
            return this;
        }

        public DeviceConnFactoryManager.Build setSerialPort(String serialPortPath) {
            this.serialPortPath = serialPortPath;
            return this;
        }

        public DeviceConnFactoryManager.Build setBaudrate(int baudrate) {
            this.baudrate = baudrate;
            return this;
        }

        public DeviceConnFactoryManager build() {
            return new DeviceConnFactoryManager(this);
        }
    }

    public void sendDataImmediately(final Vector<Byte> data) {
        if (this.mPort == null) {
            return;
        }
        try {
            this.mPort.writeDataImmediately(data, 0, data.size());
        } catch (Exception e) {//异常中断发送
            mHandler.obtainMessage(Constant.abnormal_Disconnection).sendToTarget();
//            e.printStackTrace();

        }
    }

    public void sendByteDataImmediately(final byte[] data) {
        if (this.mPort == null) {
            return;
        } else {
            Vector<Byte> datas = new Vector<Byte>();
            for (int i = 0; i < data.length; ++i) {
                datas.add(Byte.valueOf(data[i]));
            }
            try {
                this.mPort.writeDataImmediately(datas, 0, datas.size());
            } catch (IOException e) {//异常中断发送
//                e.printStackTrace();
                mHandler.obtainMessage(Constant.abnormal_Disconnection).sendToTarget();
            }
        }
    }

    public int readDataImmediately(byte[] buffer) throws IOException {
        return this.mPort.readData(buffer);
    }

    /**
     * 查询打印机当前使用的指令（ESC、CPCL、TSC、）
     */
    private void queryPrinterCommand() {
        queryPrinterCommandFlag = Constant.ESC;
        ThreadPool.getInstantiation().addSerialTask(new Runnable() {
            @Override
            public void run() {
                //开启计时器，隔2000毫秒没有没返回值时发送查询打印机状态指令，先发票据，面单，标签
                final ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder("Timer");
                final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1, threadFactoryBuilder);
                scheduledExecutorService.scheduleAtFixedRate(threadFactoryBuilder.newThread(new Runnable() {
                    @Override
                    public void run() {
                        if (currentPrinterCommand == null && queryPrinterCommandFlag > Constant.TSC) {
                            if (getConnMethod() == Constant.CONN_METHOD.USB) {//三种状态查询，完毕均无返回值，默认票据（针对凯仕、盛源机器USB查询指令没有返回值，导致连不上）
                                currentPrinterCommand = Constant.PrinterCommand.ESC;
                                sendStateBroadcast(Constant.CONN_STATE_CONNECTED);
                                sendCommand = Constant.ESC_COMMAND;
                                mHandler.sendMessage(mHandler.obtainMessage(Constant.DEFAUIT_COMMAND, ""));
                                scheduledExecutorService.shutdown();
                            } else {
                                if (reader != null) {//三种状态，查询无返回值，发送连接失败广播
                                    reader.cancel();
                                    mPort.closePort();
                                    isOpenPort = false;
                                    sendStateBroadcast(Constant.CONN_STATE_FAILED);
                                    scheduledExecutorService.shutdown();
                                }
                            }
                        }
                        if (currentPrinterCommand != null) {
                            if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
                                scheduledExecutorService.shutdown();
                            }
                            return;
                        }
                        switch (queryPrinterCommandFlag) {
                            case Constant.ESC:
                                //发送ESC查询打印机状态指令
                                sendCommand = Constant.ESC_COMMAND;
                                break;
                            case Constant.TSC:
                                //发送ESC查询打印机状态指令
                                sendCommand = Constant.TSC_COMMAND;
                                break;
                            case Constant.CPCL:
                                //发送CPCL查询打印机状态指令
                                sendCommand = Constant.CPCL_COMMAND;
                                break;
                            default:
                                break;
                        }
                        Vector<Byte> data = new Vector<>(sendCommand.length);
                        for (int i = 0; i < sendCommand.length; i++) {
                            data.add(sendCommand[i]);
                        }
                        sendDataImmediately(data);
                        queryPrinterCommandFlag++;
                    }
                }), 1500, 1500, TimeUnit.MILLISECONDS);
            }
        });
    }

    class PrinterReader extends Thread {
        private boolean isRun = false;

        private byte[] buffer = new byte[100];

        public PrinterReader() {
            isRun = true;
        }

        @Override
        public void run() {
            try {
                while (isRun) {
                    //读取打印机返回信息,打印机没有返回纸返回-1
                    Log.e(TAG, "wait read ");
                    int len = readDataImmediately(buffer);
                    Log.e(TAG, " read " + len);
                    if (len > 0) {
                        Message message = Message.obtain();
                        message.what = Constant.READ_DATA;
                        Bundle bundle = new Bundle();
                        bundle.putInt(Constant.READ_DATA_CNT, len); //数据长度
                        bundle.putByteArray(Constant.READ_BUFFER_ARRAY, buffer); //数据
                        message.setData(bundle);
                        mHandler.sendMessage(message);
                    }
                }
            } catch (Exception e) {//异常断开
                if (deviceConnFactoryManagers[id] != null) {
                    closePort(id);
                    mHandler.obtainMessage(Constant.abnormal_Disconnection).sendToTarget();
                }
            }
        }

        public void cancel() {
            isRun = false;
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            handleMsg(msg);
        }
    };

    private void handleMsg(Message msg) {
        switch (msg.what) {
            case Constant.abnormal_Disconnection://异常断开连接
//                Utils.showLong(R.string.str_disconnect);
                Log.d(TAG, " mHandler --> 断开连接");
                break;
            case Constant.DEFAUIT_COMMAND://默认模式
//                Utils.showLong(R.string.default_mode);
                Log.d(TAG, " mHandler --> 默认模式，小票模式");
                break;
            case Constant.READ_DATA:
                int cnt = msg.getData().getInt(Constant.READ_DATA_CNT); //数据长度 >0;
                byte[] buffer = msg.getData().getByteArray(Constant.READ_BUFFER_ARRAY);  //数据
                //这里只对查询状态返回值做处理，其它返回值可参考编程手册来解析
                if (buffer == null) {
                    return;
                }
                int result = judgeResponseType(buffer[0]); //数据右移
//                    String status = App.getContext().getString(R.string.str_printer_conn_normal);
                String status = "打印机连接正常";
                Log.d(TAG, " mHandler --> 打印机连接正常");
                if (sendCommand == Constant.ESC_COMMAND) {
                    //设置当前打印机模式为ESC模式
                    if (currentPrinterCommand == null) {
                        currentPrinterCommand = Constant.PrinterCommand.ESC;
                        sendStateBroadcast(Constant.CONN_STATE_CONNECTED);
//                            Utils.toast(App.getContext(),App.getContext().getString(R.string.str_escmode));
                        Log.d(TAG, " mHandler --> 票据模式");
                    } else {//查询打印机状态
                        if (result == 0) {//打印机状态查询
                            Intent intent = new Intent(Constant.ACTION_QUERY_PRINTER_STATE);
                            intent.putExtra(Constant.DEVICE_ID, id);
                            // todo 需要修改
                            Utils.getApp().sendBroadcast(intent);
                        } else if (result == 1) {//查询打印机实时状态
                            if ((buffer[0] & Constant.ESC_STATE_PAPER_ERR) > 0) {
                                status += " 打印机缺纸";
                            }
                            if ((buffer[0] & Constant.ESC_STATE_COVER_OPEN) > 0) {
                                status += " 打印机开盖";
                            }
                            if ((buffer[0] & Constant.ESC_STATE_ERR_OCCURS) > 0) {
                                status += " 打印机出错";
                            }
                            System.out.println("状态：" + status);
                            String mode = "打印模式:ESC";
                            Log.d(TAG, " mHandler --> " + mode + " " + status);
//                                Utils.toast(App.getContext(), mode+" "+status);
                        }
                    }
                }
                else if (sendCommand == Constant.TSC_COMMAND) {
                    //设置当前打印机模式为TSC模式
                    if (currentPrinterCommand == null) {
                        currentPrinterCommand = Constant.PrinterCommand.TSC;
                        sendStateBroadcast(Constant.CONN_STATE_CONNECTED);
//                            Utils.toast(App.getContext(),App.getContext().getString(R.string.str_tscmode));
                        Log.d(TAG, " mHandler --> 标签模式");
                    } else {
                        if (cnt == 1) {//查询打印机实时状态
                            if ((buffer[0] & Constant.ESC_STATE_PAPER_ERR) > 0) {
                                status += " 打印机缺纸";
                            }
                            if ((buffer[0] & Constant.ESC_STATE_COVER_OPEN) > 0) {
                                status += " 打印机开盖";
                            }
                            if ((buffer[0] & Constant.ESC_STATE_ERR_OCCURS) > 0) {
                                status += " 打印机出错";
                            }
                            System.out.println("状态：" + status);
                            String mode = "打印模式:TSC";
                            Log.d(TAG, " mHandler --> " + mode + " " + status);

//                                System.out.println(App.getContext().getString(R.string.str_state) + status);
//                                String mode=App.getContext().getString(R.string.str_printer_printmode_tsc);
//                                Utils.toast(App.getContext(), mode+" "+status);
                        } else {//打印机状态查询
                            Intent intent = new Intent(Constant.ACTION_QUERY_PRINTER_STATE);
                            intent.putExtra(Constant.DEVICE_ID, id);
                            Utils.getApp().sendBroadcast(intent);
                        }
                    }
                }
                else if (sendCommand == Constant.CPCL_COMMAND) {
                    if (currentPrinterCommand == null) {
                        currentPrinterCommand = Constant.PrinterCommand.CPCL;
                        sendStateBroadcast(Constant.CONN_STATE_CONNECTED);
//                            Utils.toast(App.getContext(),App.getContext().getString(R.string.str_cpclmode));
                        Log.d(TAG, " mHandler --> 面单模式");
                    } else {
                        if (cnt == 1) {
                            System.out.println("状态：" + status);
                            if ((buffer[0] & Constant.ESC_STATE_PAPER_ERR) > 0) {
                                status += " 打印机缺纸";
                            }
                            if ((buffer[0] & Constant.ESC_STATE_COVER_OPEN) > 0) {
                                status += " 打印机开盖";
                            }
                            String mode = "打印模式:CPCL";
                            Log.d(TAG, " mHandler --> " + mode + " " + status);
//                                String mode=App.getContext().getString(R.string.str_printer_printmode_cpcl);
//                                Utils.toast(App.getContext(), mode+" "+status);
                        } else {//打印机状态查询
                            Intent intent = new Intent(Constant.ACTION_QUERY_PRINTER_STATE);
                            intent.putExtra(Constant.DEVICE_ID, id);
                            Utils.getApp().sendBroadcast(intent);
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * 发送广播
     *
     * @param state
     */
    private void sendStateBroadcast(int state) {
        Intent intent = new Intent(Constant.ACTION_CONN_STATE);
        intent.putExtra(Constant.STATE, state);
        intent.putExtra(Constant.DEVICE_ID, id);
        //此处若报空指针错误，需要在清单文件application标签里注册此类，参考demo
        Utils.getApp().sendBroadcast(intent);
    }

    /**
     * 判断是实时状态（10 04 02）还是查询状态（1D 72 01）
     */
    private int judgeResponseType(byte r) {
        return (byte) ((r & Constant.FLAG) >> 4);
    }


}