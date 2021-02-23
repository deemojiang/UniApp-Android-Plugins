package io.huiy.deemo.printer.helpers;

/**
 * Created by Administrator
 *
 * @author 猿史森林
 *         Date: 2017/10/14
 *         Class description:
 */
public class Constant {
    public static final String SERIALPORTPATH = "SerialPortPath";
    public static final String SERIALPORTBAUDRATE = "SerialPortBaudrate";
    public static final String WIFI_CONFIG_IP = "wifi config ip";
    public static final String WIFI_CONFIG_PORT = "wifi config port";
    public static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    public static final int BLUETOOTH_REQUEST_CODE = 0x001;
    public static final int USB_REQUEST_CODE = 0x002;
    public static final int WIFI_REQUEST_CODE = 0x003;
    public static final int SERIALPORT_REQUEST_CODE = 0x006;
    public static final int CONN_STATE_DISCONN = 0x007;
    public static final int MESSAGE_UPDATE_PARAMETER = 0x009;
    public static final int tip=0x010;
    public static final int abnormal_Disconnection=0x011;//异常断开

    public static final int PING_SUCCESS=0xa1;
    public static final int PING_FAIL=0xa2;

    public static final String WIFI_DEFAULT_IP = "192.168.123.100"; // wifi 默认ip
    public static final int WIFI_DEFAULT_PORT = 9100;               // wifi 默认端口号


    // ESC查询打印机实时状态指令
    public static final int ESC = 1;
    public static final byte[] ESC_COMMAND = {0x10, 0x04, 0x02};
    public static final int ESC_STATE_PAPER_ERR = 0x20;        // ESC查询打印机实时状态 缺纸状态
    public static final int ESC_STATE_COVER_OPEN = 0x04;       // ESC指令查询打印机实时状态 打印机开盖状态
    public static final int ESC_STATE_ERR_OCCURS = 0x40;       // ESC指令查询打印机实时状态 打印机报错状态

    // CPCL查询打印机状态指令
    public static final int CPCL = 2;
    public static final byte[] CPCL_COMMAND = {0x1b, 0x68};
    public static final int CPCL_STATE_PAPER_ERR = 0x01;       // CPCL指令查询打印机实时状态 打印机缺纸状态
    public static final int CPCL_STATE_COVER_OPEN = 0x02;      // CPCL指令查询打印机实时状态 打印机开盖状态

    // TSC查询打印机状态指令
    public static final int TSC = 3;
    public static final byte[] TSC_COMMAND = {0x1b, '!', '?'};
    public static final int TSC_STATE_PAPER_ERR = 0x04;    // TSC指令查询打印机实时状态 打印机缺纸状态
    public static final int TSC_STATE_COVER_OPEN = 0x01;   // TSC指令查询打印机实时状态 打印机开盖状态
    public static final int TSC_STATE_ERR_OCCURS = 0x80;   // TSC指令查询打印机实时状态 打印机出错状态


    public static final byte FLAG = 0x10;
    public static final int READ_DATA = 10000;
    public static final int DEFAUIT_COMMAND = 20000;
    public static final String READ_DATA_CNT = "read_data_cnt";
    public static final String READ_BUFFER_ARRAY = "read_buffer_array";
    public static final String ACTION_CONN_STATE = "action_connect_state";
    public static final String ACTION_QUERY_PRINTER_STATE = "action_query_printer_state";
    public static final String STATE = "state";
    public static final String DEVICE_ID = "id";
    public static final int CONN_STATE_DISCONNECT = 0x90;
    public static final int CONN_STATE_CONNECTING = CONN_STATE_DISCONNECT << 1;
    public static final int CONN_STATE_FAILED = CONN_STATE_DISCONNECT << 2;
    public static final int CONN_STATE_CONNECTED = CONN_STATE_DISCONNECT << 3;


    public static final int REQUEST_CODE = 0x004;
    public static final int PRINTER_COMMAND_ERROR = 0x008;     // 使用打印机指令错误
    public static final int CONN_MOST_DEVICES = 0x11;
    public static final int CONN_PRINTER = 0x12;

    public enum CONN_METHOD {
        //蓝牙连接
        BLUETOOTH("BLUETOOTH"),
        //USB连接
        USB("USB"),
        //wifi连接
        WIFI("WIFI"),
        //串口连接
        SERIAL_PORT("SERIAL_PORT");

        private String name;

        private CONN_METHOD(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    public enum PrinterCommand {
        /**
         * ESC指令
         */
        ESC,
        /**
         * TSC指令
         */
        TSC,
        /**
         * CPCL指令
         */
        CPCL
    }

}