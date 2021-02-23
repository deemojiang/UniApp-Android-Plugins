package io.huiy.deemo.printer.thread;

import android.os.Handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import io.huiy.deemo.printer.helpers.Constant;


/**************************************
 * 监听WiFi连接线程2019.1.15
 **************************************/

public class CheckWifiConnThread extends Thread {
    private boolean runing=false;
    private String ip;
    private Handler handler;


    public CheckWifiConnThread(String ip, Handler handler){
        runing=true;
        this.ip=ip;
        this.handler=handler;
    }

    @Override
    public void run() {
        while (runing){
            try {
                sleep(2*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String str=Ping(ip);
            if(str.equals("success")) {
                //Ping通操作
                if (runing)
                    handler.obtainMessage(Constant.PING_SUCCESS).sendToTarget();

            }else {
                //Ping不通操作
                if (runing) {
                    handler.obtainMessage(Constant.PING_FAIL).sendToTarget();
                }

            }

        }
    }

    public String Ping(String str) {
        String result = "";
        Process p;
        try {
            p = Runtime.getRuntime().exec("ping -c 1 -w 3 " + str);
            InputStream input = p.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            StringBuffer buffer = new StringBuffer();
            String line = "";
            while ((line = in.readLine()) != null){
                buffer.append(line);
            }
            input.close();
            in.close();
//            LogUtil.i("ping wifi result" + buffer.toString());
            if(buffer.toString().indexOf("100%")!=-1||buffer.toString().equals("")){
                result = "fail";
            }  else{
                result = "success";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public void cancel(){
        runing=false;
    }

}
