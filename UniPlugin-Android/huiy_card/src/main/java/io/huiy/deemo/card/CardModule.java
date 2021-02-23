package io.huiy.deemo.card;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.huashi.bluetooth.HSBlueApi;
import com.huashi.bluetooth.HsInterface;
import com.huashi.bluetooth.IDCardInfo;
import com.huashi.bluetooth.Utility;
import com.taobao.weex.annotation.JSMethod;
import com.taobao.weex.bridge.JSCallback;
import com.taobao.weex.common.WXModule;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.dcloud.feature.uniapp.common.UniModule;


public class CardModule extends UniModule {
    private static final String TAG = "CardModule";
    String filepath = "";
    private HSBlueApi api;
    int ret;

    private List<BluetoothDevice> bundDevices;
    private List<BluetoothDevice> notDevices;


    private IDCardInfo ic;
    private boolean isConn = false;

    private Handler mHandler;
    private Handler mHandlerBluetooth;
    private int mTime;


    private void CopyAssets(String assetDir, String dir) {
        String[] files;
        try {
            files = mWXSDKInstance.getContext().getResources().getAssets().list(assetDir);
        } catch (IOException e1) {
            return;
        }
        File mWorkingPath = new File(dir);
        //if this directory does not exists, make one.
        if (!mWorkingPath.exists()) {
            if (!mWorkingPath.mkdirs()) {

            }
        }
        for (int i = 0; i < files.length; i++) {
            try {
                String fileName = files[i];
                //we make sure file name not contains '.' to be a folder.
                if (!fileName.contains(".")) {
                    if (0 == assetDir.length()) {
                        CopyAssets(fileName, dir + fileName + "/");
                    } else {
                        CopyAssets(assetDir + "/" + fileName, dir + fileName + "/");
                    }
                    continue;
                }
                File outFile = new File(mWorkingPath, fileName);
                if (outFile.exists())
                    outFile.delete();
                InputStream in = null;
                if (0 != assetDir.length())
                    in = mWXSDKInstance.getContext().getAssets().open(assetDir + "/" + fileName);
                else
                    in = mWXSDKInstance.getContext().getAssets().open(fileName);
                OutputStream out = new FileOutputStream(outFile);

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private JSONObject scrollCVR() {
        int code = 0;//0-失败，1-成功
        String msg = "读卡失败";

        String cvr_result = "读卡错误";//读卡结果
        String cvr_type = null;//证件类型
        String cvr_name = null;//姓名
        int cvr_head = 0;//头像获取 0-失败，1-成功
        String cvr_head_address = null;//头像地址
        String cvr_head_str = null;//头像
        String cvr_gender = null;//性别
        String cvr_nation = null;//民族
        String cvr_date_of_birth = null;//出生日期
        String cvr_address = null;//地址
        String cvr_id_number = null;//身份号码
        String cvr_signing = null;//签发机关
        String cvr_term_of_validity = null;//有效期限
        String cvr_fingerprint_one = null;//指纹信息1
        String cvr_fingerprint_two = null;//指纹信息2

        if (!isConn)
            return null;
        api.aut();
        ret = api.Authenticate(500);
//                if (ret == 1) {
        ic = new IDCardInfo();
        ret = api.Read_Card(ic, 2000);
        if (ret == 1) {
            code = 1;
            msg = "读取成功";
            cvr_result = "读卡成功";
            SimpleDateFormat df = new SimpleDateFormat("yyyy年MM月dd日");// 设置日期格式

            byte[] fp = new byte[1024];
            fp = ic.getFpDate();
            String m_FristPFInfo = "";
            String m_SecondPFInfo = "";

            if (fp[4] == (byte) 0x01) {
                m_FristPFInfo = String.format("指纹信息：第一枚指纹注册成功。指位：%s。指纹质量：%d ", Utility.GetFPcode(fp[5]), fp[6]);
            } else {
                m_FristPFInfo = "身份证无指纹 \n";
            }
            if (fp[512 + 4] == (byte) 0x01) {
                m_SecondPFInfo = String.format("指纹信息：第二枚指纹注册成功。指位：%s。指纹质量：%d ", Utility.GetFPcode(fp[512 + 5]),
                        fp[512 + 6]);
            } else {
                m_SecondPFInfo = "身份证无指纹 \n";
            }
            if (ic.getcertType() == " ") {
                cvr_type = "身份证";
                cvr_name = ic.getPeopleName();
                cvr_gender = ic.getSex();
                cvr_nation = ic.getPeople();
                cvr_date_of_birth = df.format(ic.getBirthDay());
                cvr_address = ic.getAddr();
                cvr_id_number = ic.getIDCard();
                cvr_signing = ic.getDepartment();
                cvr_term_of_validity = ic.getStrartDate() + "-" + ic.getEndDate();
                cvr_fingerprint_one = m_FristPFInfo;
                cvr_fingerprint_two = m_SecondPFInfo;
            } else {
                if (ic.getcertType() == "J") {
                    cvr_result = "证件类型：港澳台居住证（J）\n"
                            + "姓名：" + ic.getPeopleName() + "\n" + "性别："
                            + ic.getSex() + "\n"
                            + "签发次数：" + ic.getissuesNum() + "\n"
                            + "通行证号码：" + ic.getPassCheckID() + "\n"
                            + "出生日期：" + df.format(ic.getBirthDay())
                            + "\n" + "地址：" + ic.getAddr() + "\n" + "身份号码："
                            + ic.getIDCard() + "\n" + "签发机关："
                            + ic.getDepartment() + "\n" + "有效期限："
                            + ic.getStrartDate() + "-" + ic.getEndDate() + "\n"
                            + m_FristPFInfo + "\n" + m_SecondPFInfo;
                    cvr_type = "港澳台居住证（J）";
                } else if (ic.getcertType() == "I") {
                    cvr_result = "证件类型：外国人永久居留证（I）\n"
                            + "英文名称：" + ic.getPeopleName() + "\n"
                            + "中文名称：" + ic.getstrChineseName() + "\n"
                            + "性别：" + ic.getSex() + "\n"
                            + "永久居留证号：" + ic.getIDCard() + "\n"
                            + "国籍：" + ic.getstrNationCode() + "\n"
                            + "出生日期：" + df.format(ic.getBirthDay())
                            + "\n" + "证件版本号：" + ic.getstrCertVer() + "\n"
                            + "申请受理机关：" + ic.getDepartment() + "\n"
                            + "有效期限：" + ic.getStrartDate() + "-" + ic.getEndDate() + "\n"
                            + m_FristPFInfo + "\n" + m_SecondPFInfo;
                    cvr_type = "外国人永久居留证（I）";
                } else {
                    cvr_result = "身份证信息有误";
                    cvr_type = "未知";
                }
                cvr_name = null;
                cvr_gender = null;
                cvr_nation = null;
                cvr_date_of_birth = null;
                cvr_address = null;
                cvr_id_number = null;
                cvr_signing = null;
                cvr_term_of_validity = null;
                cvr_fingerprint_one = null;
                cvr_fingerprint_two = null;
            }

            Log.d(TAG,"filepath --> "+ filepath);
            ret = api.Unpack(filepath, ic.getwltdata());// 照片解码
            Log.d(TAG,"filepath  --> 结果" + ret);
            if (ret != 0) {// 读卡失败
                cvr_head = 0;
                cvr_head_address = null;
                cvr_head_str = null;
            } else {
                cvr_head = 1;
                cvr_head_address = filepath + "/zp.bmp";

                try {
                    FileInputStream fis = new FileInputStream(filepath + "/zp.bmp");
                    Bitmap bmp = BitmapFactory.decodeStream(fis);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, bos);//参数100表示不压缩
                    byte[] bytes = bos.toByteArray();
                    cvr_head_str = Base64.encodeToString(bytes, Base64.NO_WRAP);

                    fis.close();
                } catch (FileNotFoundException e) {
                    Toast.makeText(mWXSDKInstance.getContext(), "头像不存在！", Toast.LENGTH_SHORT).show();
                    cvr_head_str = null;
                } catch (IOException e) {
                    Toast.makeText(mWXSDKInstance.getContext(), "头像读取错误", Toast.LENGTH_SHORT).show();
                    cvr_head_str = null;
                } catch (Exception e) {
                    Toast.makeText(mWXSDKInstance.getContext(), "异常", Toast.LENGTH_SHORT).show();
                    cvr_head_str = null;
                }
            }
        } else {
            code = 0;
            msg = "读取失败";
            cvr_result = "读卡错误";
        }
        JSONObject data = new JSONObject();
        try {
            data.put("code", code);//0-失败，1-成功
            data.put("msg", msg);
            JSONObject result = new JSONObject();
            data.put("cvr_result", cvr_result);//读卡结果
            data.put("cvr_type", cvr_type);//证件类型
            data.put("cvr_name", cvr_name);//姓名
            data.put("cvr_head", cvr_head);//头像获取
            data.put("cvr_head_address", cvr_head_address);//头像地址
            data.put("cvr_head_str", cvr_head_str);//头像地址
            data.put("cvr_gender", cvr_gender);//性别
            data.put("cvr_nation", cvr_nation);//民族
            data.put("cvr_date_of_birth", cvr_date_of_birth);//出生日期
            data.put("cvr_address", cvr_address);//地址
            data.put("cvr_id_number", cvr_id_number);//身份号码
            data.put("cvr_signing", cvr_signing);//签发机关
            data.put("cvr_term_of_validity", cvr_term_of_validity);//有效期限
            data.put("cvr_fingerprint_one", cvr_fingerprint_one);//指纹信息1
            data.put("cvr_fingerprint_two", cvr_fingerprint_two);//指纹信息2
            data.put("result", result);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mHandler.sendEmptyMessageDelayed(1, mTime * 1000);
        return data;
    }


    @JSMethod(uiThread = true)
    public void initCVR(JSCallback callback) {
        Log.d(TAG, "initCVR  --> 初始化");
        JSONObject data = new JSONObject();
        int code = 0;//0-初始化成功，1-初始化失败
        if (mWXSDKInstance != null) {
            filepath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/wltlib";// 授权目录

            CopyAssets("wltlib", filepath);

            bundDevices = new ArrayList<>();
            notDevices = new ArrayList<>();

            api = new HSBlueApi(mWXSDKInstance.getContext(), filepath);
            ret = api.init();
            api.setmInterface(new HsInterface() {
                @Override
                public void reslut2Devices(Map<String, List<BluetoothDevice>> map) {
                    bundDevices = map.get("bind");
                    notDevices = map.get("notBind");
                }
            });
            if (ret == -1) {
                code = 0;
            } else {
                code = 1;
            }
        } else {
            code = 0;
        }
        try {
            data.put("code", code);
            data.put("msg", code == 0 ? "初始化失败" : "初始化成功");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        callback.invoke(data);
    }

    @JSMethod(uiThread = true)
    public void getBluetoothDeviceCVR(int time, final JSCallback callback) {
        Log.d(TAG,"getBluetoothDeviceCVR   --> 获取蓝牙设备");
        if (ret == -1)
            return;

        if (bundDevices != null) {
            bundDevices.clear();
        }
        if (notDevices != null) {
            notDevices.clear();
        }
        api.scanf();

        mHandlerBluetooth = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 2:
                        JSONObject data = new JSONObject();
                        try {
                            data.put("code", 1);
                            data.put("msg", "获取蓝牙设备成功");
                            JSONArray jsonArray = new JSONArray();
                            for (int i = 0; i < bundDevices.size(); i++) {
                                JSONObject object = new JSONObject();
                                object.put("name", bundDevices.get(i).getName());
                                object.put("address", bundDevices.get(i).getAddress());
                                jsonArray.add(object);
                            }
                            for (int i = 0; i < notDevices.size(); i++) {
                                JSONObject object = new JSONObject();
                                object.put("name", notDevices.get(i).getName());
                                object.put("address", notDevices.get(i).getAddress());
                                jsonArray.add(object);
                            }
                            data.put("result", jsonArray);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        callback.invoke(data);
                        break;
                }
            }
        };

        mHandlerBluetooth.sendEmptyMessageDelayed(2, time * 1000);

    }

    @JSMethod(uiThread = true)
    public void connectBluetoothCVR(String address, JSCallback callback) {
        Log.d(TAG,"connectBluetoothCVR --> 连接蓝牙设备");

        if (bundDevices != null && notDevices != null) {
            bundDevices.clear();
            notDevices.clear();
        }
        api.scanf();

        JSONObject data = new JSONObject();
        int code = 0;
        String msg = null;
        if (isConn) {
            code = 0;
            msg = "已有蓝牙设备连接";
        } else {
            if (!TextUtils.isEmpty(address)) {
                int ret = api.connect(address);
                if (ret == 0) {
                    isConn = true;
                    code = 1;
                    msg = "连接成功";
                } else {
                    code = 0;
                    msg = "连接失败";
                }
            } else {
                code = 0;
                msg = "没有蓝牙设备";
            }
        }

        try {
            data.put("code", code);
            data.put("msg", msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        callback.invoke(data);
    }

    @JSMethod(uiThread = true)
    public void loadCVR(JSCallback callback) {
        Log.d(TAG,"loadCVR --> 读卡invoke");
        int code = 0;//0-失败，1-成功
        String msg = "读卡失败";

        String cvr_result = "读卡错误";//读卡结果
        String cvr_type = null;//证件类型
        String cvr_name = null;//姓名
        int cvr_head = 0;//头像获取 0-失败，1-成功
        String cvr_head_address = null;//头像地址
        String cvr_head_str = null;//头像
        String cvr_gender = null;//性别
        String cvr_nation = null;//民族
        String cvr_date_of_birth = null;//出生日期
        String cvr_address = null;//地址
        String cvr_id_number = null;//身份号码
        String cvr_signing = null;//签发机关
        String cvr_term_of_validity = null;//有效期限
        String cvr_fingerprint_one = null;//指纹信息1
        String cvr_fingerprint_two = null;//指纹信息2


        if (!isConn)
            return;
        api.aut();
        ret = api.Authenticate(500);
//                if (ret == 1) {
        ic = new IDCardInfo();
        ret = api.Read_Card(ic, 2000);
        if (ret == 1) {
            code = 1;
            msg = "读取成功";
            cvr_result = "读卡成功";
            SimpleDateFormat df = new SimpleDateFormat("yyyy年MM月dd日");// 设置日期格式

            byte[] fp = new byte[1024];
            fp = ic.getFpDate();
            String m_FristPFInfo = "";
            String m_SecondPFInfo = "";

            if (fp[4] == (byte) 0x01) {
                m_FristPFInfo = String.format("指纹信息：第一枚指纹注册成功。指位：%s。指纹质量：%d ", Utility.GetFPcode(fp[5]), fp[6]);
            } else {
                m_FristPFInfo = "身份证无指纹 \n";
            }
            if (fp[512 + 4] == (byte) 0x01) {
                m_SecondPFInfo = String.format("指纹信息：第二枚指纹注册成功。指位：%s。指纹质量：%d ", Utility.GetFPcode(fp[512 + 5]),
                        fp[512 + 6]);
            } else {
                m_SecondPFInfo = "身份证无指纹 \n";
            }
            if (ic.getcertType() == " ") {
                cvr_type = "身份证";
                cvr_name = ic.getPeopleName();
                cvr_gender = ic.getSex();
                cvr_nation = ic.getPeople();
                cvr_date_of_birth = df.format(ic.getBirthDay());
                cvr_address = ic.getAddr();
                cvr_id_number = ic.getIDCard();
                cvr_signing = ic.getDepartment();
                cvr_term_of_validity = ic.getStrartDate() + "-" + ic.getEndDate();
                cvr_fingerprint_one = m_FristPFInfo;
                cvr_fingerprint_two = m_SecondPFInfo;
            } else {
                if (ic.getcertType() == "J") {
                    cvr_result = "证件类型：港澳台居住证（J）\n"
                            + "姓名：" + ic.getPeopleName() + "\n" + "性别："
                            + ic.getSex() + "\n"
                            + "签发次数：" + ic.getissuesNum() + "\n"
                            + "通行证号码：" + ic.getPassCheckID() + "\n"
                            + "出生日期：" + df.format(ic.getBirthDay())
                            + "\n" + "地址：" + ic.getAddr() + "\n" + "身份号码："
                            + ic.getIDCard() + "\n" + "签发机关："
                            + ic.getDepartment() + "\n" + "有效期限："
                            + ic.getStrartDate() + "-" + ic.getEndDate() + "\n"
                            + m_FristPFInfo + "\n" + m_SecondPFInfo;
                    cvr_type = "港澳台居住证（J）";
                } else if (ic.getcertType() == "I") {
                    cvr_result = "证件类型：外国人永久居留证（I）\n"
                            + "英文名称：" + ic.getPeopleName() + "\n"
                            + "中文名称：" + ic.getstrChineseName() + "\n"
                            + "性别：" + ic.getSex() + "\n"
                            + "永久居留证号：" + ic.getIDCard() + "\n"
                            + "国籍：" + ic.getstrNationCode() + "\n"
                            + "出生日期：" + df.format(ic.getBirthDay())
                            + "\n" + "证件版本号：" + ic.getstrCertVer() + "\n"
                            + "申请受理机关：" + ic.getDepartment() + "\n"
                            + "有效期限：" + ic.getStrartDate() + "-" + ic.getEndDate() + "\n"
                            + m_FristPFInfo + "\n" + m_SecondPFInfo;
                    cvr_type = "外国人永久居留证（I）";
                } else {
                    cvr_result = "身份证信息有误";
                    cvr_type = "未知";
                }
                cvr_name = null;
                cvr_gender = null;
                cvr_nation = null;
                cvr_date_of_birth = null;
                cvr_address = null;
                cvr_id_number = null;
                cvr_signing = null;
                cvr_term_of_validity = null;
                cvr_fingerprint_one = null;
                cvr_fingerprint_two = null;
            }


            Log.d(TAG,"filepath --> "+ filepath);
            ret = api.Unpack(filepath, ic.getwltdata());// 照片解码
            Log.d(TAG,"filepath --> 结果:" + ret);
            if (ret != 0) {// 读卡失败
                cvr_head = 0;
                cvr_head_address = null;
                cvr_head_str = null;
            } else {
                cvr_head = 1;
                cvr_head_address = filepath + "/zp.bmp";
                try {
                    FileInputStream fis = new FileInputStream(filepath + "/zp.bmp");
                    Bitmap bmp = BitmapFactory.decodeStream(fis);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, bos);//参数100表示不压缩
                    byte[] bytes = bos.toByteArray();
                    cvr_head_str = Base64.encodeToString(bytes, Base64.NO_WRAP);

                    fis.close();
                } catch (FileNotFoundException e) {
                    Toast.makeText(mWXSDKInstance.getContext(), "头像不存在！", Toast.LENGTH_SHORT).show();
                    cvr_head_str = null;
                } catch (IOException e) {
                    Toast.makeText(mWXSDKInstance.getContext(), "头像读取错误", Toast.LENGTH_SHORT).show();
                    cvr_head_str = null;
                } catch (Exception e) {
                    Toast.makeText(mWXSDKInstance.getContext(), "异常", Toast.LENGTH_SHORT).show();
                    cvr_head_str = null;
                }
            }
        } else {
            code = 0;
            msg = "读取失败";
            cvr_result = "读卡错误";
        }
        JSONObject data = new JSONObject();
        try {
            data.put("code", code);//0-失败，1-成功
            data.put("msg", msg);
            JSONObject result = new JSONObject();
            data.put("cvr_result", cvr_result);//读卡结果
            data.put("cvr_type", cvr_type);//证件类型
            data.put("cvr_name", cvr_name);//姓名
            data.put("cvr_head", cvr_head);//头像获取
            data.put("cvr_head_address", cvr_head_address);//头像地址
            data.put("cvr_head_str", cvr_head_str);//头像地址
            data.put("cvr_gender", cvr_gender);//性别
            data.put("cvr_nation", cvr_nation);//民族
            data.put("cvr_date_of_birth", cvr_date_of_birth);//出生日期
            data.put("cvr_address", cvr_address);//地址
            data.put("cvr_id_number", cvr_id_number);//身份号码
            data.put("cvr_signing", cvr_signing);//签发机关
            data.put("cvr_term_of_validity", cvr_term_of_validity);//有效期限
            data.put("cvr_fingerprint_one", cvr_fingerprint_one);//指纹信息1
            data.put("cvr_fingerprint_two", cvr_fingerprint_two);//指纹信息2
            data.put("result", result);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        callback.invoke(data);
    }

    @JSMethod(uiThread = true)
    public void loadAutoHandlerCVR(int time, final JSCallback callback) {
        Log.d(TAG,"loadAutoHandlerCVR --> 循环读卡");
        mTime = time;
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 1:
                        Log.e("loadAutoHandlerCVR", "循环读卡");
                        callback.invokeAndKeepAlive(scrollCVR());
                        break;
                }
            }
        };

        mHandler.sendEmptyMessageDelayed(1, mTime * 1000);
    }

    @JSMethod(uiThread = true)
    public void stopAutoCVR() {
        Log.d(TAG,"stopAutoCVR  --> 停止读卡");
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        if (mHandlerBluetooth != null) {
            mHandlerBluetooth.removeCallbacksAndMessages(null);
            mHandlerBluetooth = null;
        }
    }

    @JSMethod(uiThread = true)
    public void closeCVR(JSCallback callback) {
        Log.d(TAG,"closeCVR --> 断开连接");
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        if (mHandlerBluetooth != null) {
            mHandlerBluetooth.removeCallbacksAndMessages(null);
            mHandlerBluetooth = null;
        }

        JSONObject data = new JSONObject();
        int code = 0;
        String msg = null;
        ret = api.unInit();
        if (ret == 0) {
            isConn = false;
            code = 1;
            msg = "断开成功";
        } else {
            code = 0;
            msg = "断开失败";
        }
        try {
            data.put("code", code);
            data.put("msg", msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        callback.invoke(data);
    }

    @JSMethod(uiThread = true)
    public void gotoNativePage() {
        if (mWXSDKInstance != null && mWXSDKInstance.getContext() instanceof Activity) {
            Intent intent = new Intent(mWXSDKInstance.getContext(), NativePageActivity.class);
            ((Activity) mWXSDKInstance.getContext()).startActivity(intent);
        }
    }

}

