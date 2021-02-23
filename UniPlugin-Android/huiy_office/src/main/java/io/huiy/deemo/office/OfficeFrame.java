package io.huiy.deemo.office;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.tencent.smtt.sdk.QbSdk;
import com.tencent.smtt.sdk.TbsListener;
import com.tencent.smtt.sdk.TbsReaderView;

import java.util.HashMap;
import java.util.Map;

import io.dcloud.feature.uniapp.UniSDKInstance;
import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.ui.action.AbsComponentData;
import io.dcloud.feature.uniapp.ui.component.AbsVContainer;
import io.dcloud.feature.uniapp.ui.component.UniComponent;
import io.dcloud.feature.uniapp.ui.component.UniComponentProp;

public class OfficeFrame extends UniComponent<FrameLayout> implements TbsReaderView.ReaderCallback {

    private static final String TAG = "OfficeFrame";
    private TbsReaderView tbsReaderView;
    private Context mContext;
    private FrameLayout mFrame;

    private boolean mSuccess = false;
    private String mOfficeUrl = "";
    private String tbsReaderTemp = Environment.getExternalStorageDirectory() + "/TbsReaderTemp";

    public OfficeFrame(UniSDKInstance instance, AbsVContainer parent, AbsComponentData basicComponentData) {
        super(instance, parent, basicComponentData);
    }

    @Override
    protected FrameLayout initComponentHostView(Context context) {
        mContext = context;
        mFrame = new FrameLayout(context);
//        tbsReaderView = new TbsReaderView(context, this);
//        mFrame.addView(tbsReaderView);
        initQbSdk();
        return mFrame;
    }

    private void initQbSdk() {
        Log.d(TAG, " initQbSdk is ");

        // 增加下载过程的监听
        QbSdk.setTbsListener(new TbsListener() {
            @Override
            public void onDownloadProgress(int i) {
                Log.d(TAG, "  --> onDownloadProgress is " + i);
                initCallback(1, "DownloadProgress", i, false);
            }

            @Override
            public void onDownloadFinish(int i) {
                Log.d(TAG, "  --> onDownloadFinish is " + i);
                initCallback(2, "DownloadFinish", i, false);
            }

            @Override
            public void onInstallFinish(int i) {
                Log.d(TAG, "  --> onInstallFinish is " + i);
                initCallback(3, "InstallFinish", i, false);
            }

        });

        //x5内核初始化接口
        QbSdk.initX5Environment(mContext.getApplicationContext(), new QbSdk.PreInitCallback() {

            @Override
            public void onViewInitFinished(boolean isSuccess) {
                // TODO Auto-generated method stub
                //x5內核初始化完成的回调，为true表示x5内核加载成功，否则表示x5内核加载失败，会自动切换到系统内核。
                Log.d(TAG, "  --> onViewInitFinished is " + isSuccess);
                initCallback(4, "ViewInitFinished", 0, isSuccess);
                if (isSuccess) {
                    mSuccess = true;
                }else {
                    // todo 处理控件初始化失败的情况
                }
            }

            @Override
            public void onCoreInitFinished() {
                Log.d(TAG, "  --> onCoreInitFinished  ");
                // TODO Auto-generated method stub
            }
        });
    }

    @UniComponentProp(name = "officeUrl")
    public void setOfficeUrl(String officeUrl) {
//        Toast.makeText(mContext," " + officeUrl,Toast.LENGTH_LONG).show();
        Log.d(TAG, "  --> onViewInitFinished is " + officeUrl);
        mOfficeUrl = officeUrl;
//        if (mSuccess){
//            loadFile();
//        }
    }

    @UniJSMethod
    public void loadFile(String filePath) {
//        getHostView().setText("");
//        officeUrl = "/sdcard/ppt001.pptx";

        mOfficeUrl = filePath;
        Log.d(TAG, " --> loadFile ");
        Bundle localBundle = new Bundle();
        localBundle.putString("filePath", mOfficeUrl);
        localBundle.putString("tempPath", tbsReaderTemp);
        resetTbsView();
        boolean result = tbsReaderView.preOpen(getFileType(mOfficeUrl), false);
        if (result) {
            tbsReaderView.openFile(localBundle);
        }
    }

    /**
     * 内核加载状态的回调
     * @param type 回调类型 1.内核下载中 2. 内核下载完成 3.内核安装完成 4.控件初始化完成（需根据isSuccess判断是否成功）
     * @param msg 当前状态描述
     * @param progress 下载进度
     * @param isSuccess 控件是否加载成功
     */
    private void initCallback(int type, String msg, int progress, boolean isSuccess) {
        //原生触发fireEvent 自定义事件onTel
        Map<String, Object> params = new HashMap<>();
        Map<String, Object> notify = new HashMap<>();
        notify.put("type", type);
        notify.put("msg", msg);
        notify.put("progress", progress);
        notify.put("isSuccess", isSuccess);

        //目前uni限制 参数需要放入到"detail"中 否则会被清理
        params.put("detail", notify);
        fireEvent("onInit", params);
    }

    private void resetTbsView() {
        if (null != tbsReaderView) {
            tbsReaderView.onStop();
            mFrame.removeView(tbsReaderView);
            tbsReaderView = null;
        }
        tbsReaderView = new TbsReaderView(mContext, this);
        mFrame.addView(tbsReaderView);
    }


    public static String getFileType(String filePath) {
        String str = "";

        if (TextUtils.isEmpty(filePath)) {
            Log.d(TAG, "filePath --> null");
            return str;
        }
        Log.d(TAG, "filePath --> " + filePath);
        int i = filePath.lastIndexOf('.');
        if (i <= -1) {
            Log.d(TAG, "i <= -1");
            return str;
        }

        str = filePath.substring(i + 1);
        Log.d(TAG, "filePath.substring(i + 1) --> " + str);
        return str;
    }

    @Override
    public void onActivityResume() {
        super.onActivityResume();

    }

    @Override
    public void onActivityPause() {
        super.onActivityPause();
    }

    @Override
    public void onActivityDestroy() {
        super.onActivityDestroy();
        tbsReaderView.onStop();
        mFrame.removeView(tbsReaderView);
        tbsReaderView = null;
    }

    @Override
    public void onCallBackAction(Integer integer, Object o, Object o1) {

    }
}
