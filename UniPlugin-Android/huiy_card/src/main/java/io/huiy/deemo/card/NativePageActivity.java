package io.huiy.deemo.card;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.huashi.bluetooth.HSBlueApi;
import com.huashi.bluetooth.HsInterface;
import com.huashi.bluetooth.IDCardInfo;
import com.huashi.bluetooth.Utility;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NativePageActivity extends Activity  {
    private static final String TAG = "MainActivity";

    private TextView tv_sam, tv_info;
    private ImageView iv;
    private Button btconn, btread, btclose, btsleep, btweak;
    String filepath = "";
    boolean isSlepp = false;

    private HSBlueApi api;
    private IDCardInfo ic;
    private boolean isConn = false;
    int ret;

    private View diaView;
    private TextView tv_ts;
    private ListView lv;
    private Button scanf;
    private MyAdapter adapter;
    private List<BluetoothDevice> bundDevices;
    private List<BluetoothDevice> notDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native_page);
        Log.e("UUUU", Environment.getExternalStorageDirectory().toString());
        initView();
        initData();
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

        }
    };

    private void initData() {
        adapter = new MyAdapter();
        api = new HSBlueApi(this,filepath);
        ret = api.init();
        if (ret == -1)
            Toast.makeText(this, "初始化失败", Toast.LENGTH_LONG).show();
        api.setmInterface(new HsInterface() {
            @Override
            public void reslut2Devices(Map<String, List<BluetoothDevice>> map) {
                bundDevices = map.get("bind");
                notDevices = map.get("notBind");
                adapter.notifyDataSetChanged();
            }
        });
        btconn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ret == -1)
                    return;
                showDialog();
//                "78:A5:04:72:EC:EA"
//                ret = api.connect("78:A5:04:72:EC:EA");
//                if (ret == 0){
//                    Toast.makeText(MainActivity.this,"已连接",Toast.LENGTH_LONG).show();
//                    String sam = api.Get_SAM(500);
//                    tv_sam.setText("SAM："+sam);
//                    isConn = true;
//                }else {
//                    Toast.makeText(MainActivity.this,"连接失败",Toast.LENGTH_LONG).show();
//                }
            }
        });

        btclose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (isSlepp)
//                    api.weak();
                ret = api.unInit();
                if (ret == 0) {
                    isConn = false;
//                    Toast.makeText(MainActivity.this,"已断开",Toast.LENGTH_LONG).show();
                    iv.setImageBitmap(null);
                    tv_info.setText("设备已断开");
                } else {
                    Toast.makeText(NativePageActivity.this, "断开失败", Toast.LENGTH_LONG).show();
                }
            }
        });

        btsleep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConn) {
                    api.sleep();
                    isSlepp = true;
                    iv.setImageBitmap(null);
                    tv_info.setText("设备已休眠");
                }
            }
        });

        btweak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConn) {
                    api.weak();
                    isSlepp = false;
                    iv.setImageBitmap(null);
                    tv_info.setText("设备已唤醒");
                }
            }
        });

        btread.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isConn)
                    return;
                api.aut();
                ret = api.Authenticate(500);
//                if (ret == 1) {
                ic = new IDCardInfo();
                ret = api.Read_Card(ic, 2000);
                if (ret == 1) {
                    Toast.makeText(getApplicationContext(), "读卡成功", Toast.LENGTH_SHORT).show();
                    SimpleDateFormat df = new SimpleDateFormat("yyyy年MM月dd日");// 设置日期格式


                    byte[] fp = new byte[1024];
                    fp = ic.getFpDate();
                    String m_FristPFInfo = "";
                    String m_SecondPFInfo = "";

                    if (fp[4] == (byte)0x01) {
                        m_FristPFInfo = String.format("指纹  信息：第一枚指纹注册成功。指位：%s。指纹质量：%d ", Utility.GetFPcode(fp[5]), fp[6]);
                    } else {
                        m_FristPFInfo = "身份证无指纹 \n";
                    }
                    if (fp[512 + 4] == (byte)0x01) {
                        m_SecondPFInfo = String.format("指纹  信息：第二枚指纹注册成功。指位：%s。指纹质量：%d ", Utility.GetFPcode(fp[512 + 5]),
                                fp[512 + 6]);
                    } else {
                        m_SecondPFInfo = "身份证无指纹 \n";
                    }
                    if (ic.getcertType() == " ") {
                        tv_info.setText("证件类型：身份证\n" + "姓名："
                                + ic.getPeopleName() + "\n" + "性别：" + ic.getSex()
                                + "\n" + "民族：" + ic.getPeople() + "\n" + "出生日期："
                                + df.format(ic.getBirthDay()) + "\n" + "地址："
                                + ic.getAddr() + "\n" + "身份号码：" + ic.getIDCard()
                                + "\n" + "签发机关：" + ic.getDepartment() + "\n"
                                + "有效期限：" + ic.getStrartDate() + "-"
                                + ic.getEndDate() + "\n" + m_FristPFInfo + "\n"
                                + m_SecondPFInfo);
                    } else {
                        if(ic.getcertType() == "J")
                        {
                            tv_info.setText("证件类型：港澳台居住证（J）\n"
                                    + "姓名：" + ic.getPeopleName() + "\n" + "性别："
                                    + ic.getSex() + "\n"
                                    + "签发次数：" + ic.getissuesNum() + "\n"
                                    + "通行证号码：" + ic.getPassCheckID() + "\n"
                                    + "出生日期：" + df.format(ic.getBirthDay())
                                    + "\n" + "地址：" + ic.getAddr() + "\n" + "身份号码："
                                    + ic.getIDCard() + "\n" + "签发机关："
                                    + ic.getDepartment() + "\n" + "有效期限："
                                    + ic.getStrartDate() + "-" + ic.getEndDate() + "\n"
                                    + m_FristPFInfo + "\n" + m_SecondPFInfo);
                        }
                        else{
                            if(ic.getcertType() == "I")
                            {
                                tv_info.setText("证件类型：外国人永久居留证（I）\n"
                                        + "英文名称：" + ic.getPeopleName() + "\n"
                                        + "中文名称：" + ic.getstrChineseName() + "\n"
                                        + "性别：" + ic.getSex() + "\n"
                                        + "永久居留证号：" + ic.getIDCard() + "\n"
                                        + "国籍：" + ic.getstrNationCode() + "\n"
                                        + "出生日期：" + df.format(ic.getBirthDay())
                                        + "\n" + "证件版本号：" + ic.getstrCertVer() + "\n"
                                        + "申请受理机关：" + ic.getDepartment() + "\n"
                                        + "有效期限："+ ic.getStrartDate() + "-" + ic.getEndDate() + "\n"
                                        + m_FristPFInfo + "\n" + m_SecondPFInfo);
                            }
                        }

                    }


//                    try {
//                        ret = api.Unpack(filepath, ic.getwltdata());// 照片解码
//                        if (ret != 0) {// 读卡失败
//                            Toast.makeText(getApplicationContext(), "头像解码失败", Toast.LENGTH_SHORT).show();
//                            return;
//                        }
//                        FileInputStream fis = new FileInputStream(filepath + "/zp.bmp");
//                        Bitmap bmp = BitmapFactory.decodeStream(fis);
//                        iv.setImageBitmap(bmp);
//                        fis.close();
//                    } catch (FileNotFoundException e) {
//                        Toast.makeText(getApplicationContext(), "头像不存在！", Toast.LENGTH_SHORT).show();
//                    } catch (IOException e) {
//                        // TODO 自动生成的 catch 块
//                        Toast.makeText(getApplicationContext(), "头像读取错误", Toast.LENGTH_SHORT).show();
//                    } catch (Exception e) {
//                        Toast.makeText(getApplicationContext(), "异常", Toast.LENGTH_SHORT).show();
//                    }
                } else {
                    tv_info.setText("读卡错误");
                }
//                } else {
//                    tv_info.setText("卡认证失败");
//                    return;
//                }
            }
        });
    }

    private void initView() {
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        filepath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/wltlib";// 授权目录
        checkPermission();

        bundDevices = new ArrayList<>();
        notDevices = new ArrayList<>();
        setContentView(R.layout.main);
        iv = (ImageView) findViewById(R.id.iv);
        tv_info = (TextView) findViewById(R.id.tv);
        tv_sam = (TextView) findViewById(R.id.sam);
        btconn = (Button) findViewById(R.id.btconn);
        btread = (Button) findViewById(R.id.btread);
        btclose = (Button) findViewById(R.id.btclose);
        btsleep = (Button) findViewById(R.id.btsleep);
        btweak = (Button) findViewById(R.id.btweak);
    }

    private void checkPermission() {

        if (Build.VERSION.SDK_INT >= 23) {
            int write = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int read = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            if (write != PackageManager.PERMISSION_GRANTED || read != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 300);
            } else {
                filepath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/wltlib";// 授权目录

                File dirFile = new File(filepath);
                Log.d("dirFile", "" + dirFile);
                if (!dirFile.exists()) {
                    boolean mkdirs = dirFile.mkdirs();
                    if (!mkdirs) {
                        Log.e("TAG", "文件夹创建失败");
                    } else {
                        Log.e("TAG", "文件夹创建成功");
                        Log.e("TAG", filepath);
                    }
                }
            }
        } else {
            Log.i("wytings", "------------- Build.VERSION.SDK_INT < 23 ------------");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 300) {
            Log.i("wytings", "--------------requestCode == 300->" + requestCode + "," + permissions.length + "," + grantResults.length);
            filepath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/wltlib";// 授权目录

            File dirFile = new File(filepath);
            Log.d("dirFile", "" + dirFile);
            if (!dirFile.exists()) {
                boolean mkdirs = dirFile.mkdirs();
                if (!mkdirs) {
                    Log.e("TAG", "文件夹创建失败");
                } else {
                    Log.e("TAG", "文件夹创建成功");
                    Log.e("TAG", filepath);
                }
            }
        } else {
            Log.i("wytings", "--------------requestCode != 300->" + requestCode + "," + permissions + "," + grantResults);
        }
    }

    private void showDialog() {
        if (isConn){
            Toast.makeText(NativePageActivity.this,"已连接设备", Toast.LENGTH_LONG).show();
            return;
        }
        if (bundDevices != null && notDevices != null) {
            bundDevices.clear();
            notDevices.clear();
        }
        final AlertDialog dialog;
        diaView = View.inflate(NativePageActivity.this, R.layout.test, null);
        tv_ts = (TextView) diaView.findViewById(R.id.tv_ts);
        lv = (ListView) diaView.findViewById(R.id.lv);
        lv.setAdapter(adapter);
        scanf = (Button) diaView.findViewById(R.id.scanf);
        AlertDialog.Builder builder = new AlertDialog.Builder(NativePageActivity.this);
        builder.setView(diaView);
//        builder.setCancelable(false);
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0 || position == bundDevices.size() + 1) {
                    return;
                }
                Log.i(TAG, "onItemClick: 正在连接");
                dialog.dismiss();
                BluetoothDevice d = null;
                if (position < bundDevices.size() + 1) {
                    d = bundDevices.get(position - 1);
                } else {
                    d = notDevices.get(position - 2 - bundDevices.size());
                }
                int ret = api.connect(d.getAddress());
                if (ret == 0){
                    Toast.makeText(NativePageActivity.this,"已连接",Toast.LENGTH_LONG).show();
                    String sam = api.Get_SAM(500);
                    tv_sam.setText("SAM："+sam);
                    isConn = true;
                }else {
                    Toast.makeText(NativePageActivity.this,"连接失败",Toast.LENGTH_LONG).show();
                }
            }
        });
        scanf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                api.scanf();
                tv_ts.setVisibility(View.INVISIBLE);
                scanf.setVisibility(View.INVISIBLE);
                lv.setVisibility(View.VISIBLE);
            }
        });
    }

    class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
//            if (notDevices == null){
//                return bundDevices.size()+1;
//            }
            return bundDevices.size() + notDevices.size() + 2;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @SuppressLint("MissingPermission")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (position == 0) {
                TextView tv = new TextView(NativePageActivity.this);
                tv.setBackgroundResource(R.color.app_item);
                tv.setTextColor(Color.WHITE);
                tv.setText("绑定设备");
                return tv;
            }
            if (position == bundDevices.size() + 1) {
                TextView tv = new TextView(NativePageActivity.this);
                tv.setBackgroundResource(R.color.app_item);
                tv.setTextColor(Color.WHITE);
                tv.setText("其他设备");
                return tv;
            }
            View v = null;
            ViewHodler hodler;
            BluetoothDevice device = null;
            if (position < bundDevices.size() + 1) {
                device = bundDevices.get(position - 1);
            } else {
                device = notDevices.get(position - 2 - bundDevices.size());
            }
            if (convertView != null && convertView instanceof LinearLayout) {
                v = convertView;
                hodler = (ViewHodler) convertView.getTag();
            } else {
                v = View.inflate(NativePageActivity.this, R.layout.dialog, null);
                hodler = new ViewHodler();
                hodler.tv_name = (TextView) v.findViewById(R.id.name);
                hodler.tv_address = (TextView) v.findViewById(R.id.mac);
                convertView = v;
                convertView.setTag(hodler);
            }
            hodler.tv_name.setText(device.getName());
            hodler.tv_address.setText(device.getAddress());
            return v;
        }

        class ViewHodler {
            private TextView tv_name, tv_address;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isConn){
            api.unInit();
            isConn = false;
        }
    }
}

