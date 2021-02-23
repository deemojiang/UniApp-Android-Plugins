package io.huiy.deemo.printer.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.gprinter.command.CpclCommand;
import com.gprinter.command.EscCommand;
import com.gprinter.command.LabelCommand;

import java.util.Vector;

import io.huiy.deemo.printer.entity.Label;
import io.huiy.deemo.printer.entity.PrintInfo;

/**
 * Created by Administrator on 2018/4/16.
 */

public class PrintContent {
    /**
     * 票据打印测试页
     *
     * @return
     */
    public static Vector<Byte> getReceipt() {
        EscCommand esc = new EscCommand();
        //初始化打印机
        esc.addInitializePrinter();
        //打印走纸多少个单位
        esc.addPrintAndFeedLines((byte) 3);
        // 设置打印居中
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
        // 设置为倍高倍宽
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF);
        // 打印文字
        esc.addText("票据测试\n");
        //打印并换行
        esc.addPrintAndLineFeed();
        // 取消倍高倍宽
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
        // 设置打印左对齐
        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
        // 打印文字
        esc.addText("打印文字测试:\n");
        // 打印文字
        esc.addText("欢迎使用打印机!\n");
        esc.addPrintAndLineFeed();
        esc.addText("打印对齐方式测试:\n");
        // 设置打印左对齐
        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
        esc.addText("居左");
        esc.addPrintAndLineFeed();
        // 设置打印居中对齐
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
        esc.addText("居中");
        esc.addPrintAndLineFeed();
        // 设置打印居右对齐
        esc.addSelectJustification(EscCommand.JUSTIFICATION.RIGHT);
        esc.addText("居右");
        esc.addPrintAndLineFeed();
        esc.addPrintAndLineFeed();
        // 设置打印左对齐
        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
        esc.addText("打印Bitmap图测试:\n");
//            Bitmap b = BitmapFactory.decodeResource(App.getContext().getResources(), R.drawable.gprinter);
        // 打印图片  光栅位图  384代表打印图片像素  0代表打印模式
        // 58mm打印机 可打印区域最大点数为 384 ，80mm 打印机 可打印区域最大点数为 576
//            esc.addRastBitImage(b, 384, 0);
        esc.addPrintAndLineFeed();
        // 打印文字
        esc.addText("打印条码测试:\n");
        esc.addSelectPrintingPositionForHRICharacters(EscCommand.HRI_POSITION.BELOW);
        // 设置条码可识别字符位置在条码下方
        // 设置条码高度为60点
        esc.addSetBarcodeHeight((byte) 60);
        // 设置条码宽窄比为2
        esc.addSetBarcodeWidth((byte) 2);
        // 打印Code128码
        esc.addCODE128(esc.genCodeB("barcode128"));
        esc.addPrintAndLineFeed();
        /*
         * QRCode命令打印 此命令只在支持QRCode命令打印的机型才能使用。 在不支持二维码指令打印的机型上，则需要发送二维条码图片
         */
        esc.addText("打印二维码测试:\n");
        // 设置纠错等级
        esc.addSelectErrorCorrectionLevelForQRCode((byte) 0x31);
        // 设置qrcode模块大小
        esc.addSelectSizeOfModuleForQRCode((byte) 4);
        // 设置qrcode内容
        esc.addStoreQRCodeData("www.smarnet.cc");
        // 打印QRCode
        esc.addPrintQRCode();
        //打印并走纸换行
        esc.addPrintAndLineFeed();
        // 设置打印居中对齐
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
        //打印fontB文字字体
        esc.addSelectCharacterFont(EscCommand.FONT.FONTB);
        esc.addText("测试完成!\r\n");
        //打印并换行
        esc.addPrintAndLineFeed();
        //打印走纸n个单位
        esc.addPrintAndFeedLines((byte) 4);
        // 开钱箱
        esc.addGeneratePlus(LabelCommand.FOOT.F2, (byte) 255, (byte) 255);
        //开启切刀
        esc.addCutPaper();
        //添加缓冲区打印完成查询
        byte[] bytes = {0x1D, 0x72, 0x01};
        //添加用户指令
        esc.addUserCommand(bytes);
        Vector<Byte> datas = esc.getCommand();
        return datas;
    }


    public static Vector<Byte> parseJsonLabel(String json) {
        LabelCommand tsc = new LabelCommand();
        Label label = JSON.parseObject(json, Label.class);
        Log.d("PrintContent", "parseJsonLabel --> " + label);

        tsc.addSize(label.width, label.height); // 实际尺寸*8 （320*240）
        // 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0 单位mm
        tsc.addGap(label.gap);
        // 设置打印方向
        tsc.addDirection(label.parseDirEnum(), label.parseMirEnum());
        // 开启带Response的打印，用于连续打印
        tsc.addQueryPrinterStatus(LabelCommand.RESPONSE_MODE.ON);
        // 设置原点坐标
        tsc.addReference(label.x, label.y);
        //设置浓度
        tsc.addDensity(LabelCommand.DENSITY.DNESITY4);
        // 撕纸模式开启
        tsc.addTear(EscCommand.ENABLE.ON);
        // 清除打印缓冲区
        tsc.addCls();

        for (PrintInfo info : label.getPrintInfoList()) {
            switch (info.type) {
                case 1:
                    tsc.addText(info.x, info.y, info.parseFontEnum(), info.parseRotaEnum(), info.parseScaleXEnum(), info.parseScaleYEnum(), info.text);
                    break;
                case 2:
                    tsc.addQRCode(info.x, info.y,info.parseLevelEnum(), info.cellWidth, info.parseRotaEnum(), info.text);
                    break;
                case 3:
                    tsc.add1DBarcode(info.x, info.y, info.parseBarTypeEnum(), info.height, info.parseReadableEnum(), info.parseRotaEnum(), info.text);
                    break;
            }
        }
        // 打印标签
        tsc.addPrint(1, 1);
        // 打印标签后 蜂鸣器响
        tsc.addSound(2, 100);
        //开启钱箱
        tsc.addCashdrwer(LabelCommand.FOOT.F5, 255, 255);
        Vector<Byte> datas = tsc.getCommand();
        // 发送数据
        return datas;

    }


    /**
     * 标签打印测试页
     *
     * @return
     */
    public static Vector<Byte> getLabel() {
        LabelCommand tsc = new LabelCommand();
        // 设置标签尺寸宽高，按照实际尺寸设置 单位mm
        tsc.addSize(40, 30); // 实际尺寸*8 （320*240）
        // 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0 单位mm
        tsc.addGap(20);
        // 设置打印方向
        tsc.addDirection(LabelCommand.DIRECTION.BACKWARD, LabelCommand.MIRROR.NORMAL);
        // 开启带Response的打印，用于连续打印
        tsc.addQueryPrinterStatus(LabelCommand.RESPONSE_MODE.ON);
        // 设置原点坐标
        tsc.addReference(0, 0);
        //设置浓度
        tsc.addDensity(LabelCommand.DENSITY.DNESITY4);
        // 撕纸模式开启
        tsc.addTear(EscCommand.ENABLE.ON);
        // 清除打印缓冲区
        tsc.addCls();

//            tsc.addHome();

        // 悠悠奶茶标签模板
        // 绘制简体中文
        tsc.addText(60, 10, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_2, LabelCommand.FONTMUL.MUL_2,
                "悠悠奶茶");
        tsc.addText(20, 70, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                "品名：招牌奶茶");
        tsc.addText(20, 100, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                "规格：大杯");
        tsc.addText(20, 130, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                "价格：16元");

        //绘制二维码
        tsc.addQRCode(200, 75, LabelCommand.EEC.LEVEL_L, 3, LabelCommand.ROTATION.ROTATION_0, " 12345678900");

        // 绘制一维条码
        tsc.add1DBarcode(30, 160, LabelCommand.BARCODETYPE.CODE128, 50, LabelCommand.READABEL.EANBEL, LabelCommand.ROTATION.ROTATION_0, "12345678900");


//
//            // 绘制简体中文
//            tsc.addText(20, 0, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0,
//                    LabelCommand.FONTMUL.MUL_2, LabelCommand.FONTMUL.MUL_2, "欢迎使用Printer");
////            //打印繁体
////            tsc.addUnicodeText(20,30, LabelCommand.FONTTYPE.TRADITIONAL_CHINESE, LabelCommand.ROTATION.ROTATION_0,
////                    LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,"BIG5碼繁體中文字元","BIG5");
////            //打印韩文
////            tsc.addUnicodeText(0,20, LabelCommand.FONTTYPE.KOREAN, LabelCommand.ROTATION.ROTATION_0,
////                    LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,"Korean 지아보 하성","EUC_KR");
////            Bitmap b = BitmapFactory.decodeResource(App.getContext().getResources(), R.drawable.gprinter);
////            // 绘制图片
////            tsc.addBitmap(20, 10, LabelCommand.BITMAP_MODE.OVERWRITE, 300, b);
//            //绘制二维码
////            tsc.addQRCode(20,30, LabelCommand.EEC.LEVEL_L, 5, LabelCommand.ROTATION.ROTATION_0, " www.smarnet.cc");
////            // 绘制一维条码
//            tsc.add1DBarcode(20, 30, LabelCommand.BARCODETYPE.CODE128,
//                    100, LabelCommand.READABEL.EANBEL, LabelCommand.ROTATION.ROTATION_0, "SMARNET");
        // 打印标签
        tsc.addPrint(1, 1);
        // 打印标签后 蜂鸣器响
        tsc.addSound(2, 100);
        //开启钱箱
        tsc.addCashdrwer(LabelCommand.FOOT.F5, 255, 255);
        Vector<Byte> datas = tsc.getCommand();
        // 发送数据
        return datas;
    }

    /**
     * 标签打印长图
     *
     * @param bitmap
     * @return
     */
    public static Vector<Byte> printViewPhoto(Bitmap bitmap) {
        LabelCommand labelCommand = new LabelCommand();
        /**
         * 参数说明
         * 0：打印图片x轴
         * 0：打印图片Y轴
         * 576：打印图片宽度  纸张可打印宽度  72 *8
         * bitmap:图片
         */
        labelCommand.addZLibNoTrembleBitmapheight(0, 0, 576, bitmap);
        return labelCommand.getCommand();
    }

    /**
     * 面单打印测试页
     *
     * @return
     */
    public static Vector<Byte> getCPCL() {
        CpclCommand cpcl = new CpclCommand();
        cpcl.addInitializePrinter(1130, 1);
        cpcl.addJustification(CpclCommand.ALIGNMENT.CENTER);
        cpcl.addSetmag(1, 1);
        cpcl.addText(CpclCommand.TEXT_FONT.FONT_4, 0, 30, "Sample");
        cpcl.addSetmag(0, 0);
        cpcl.addJustification(CpclCommand.ALIGNMENT.LEFT);
        cpcl.addText(CpclCommand.TEXT_FONT.FONT_4, 0, 65, "Print text");
        cpcl.addText(CpclCommand.TEXT_FONT.FONT_4, 0, 95, "Welcom to use SMARNET printer!");
        cpcl.addText(CpclCommand.TEXT_FONT.FONT_13, 0, 135, "佳博智匯標籤打印機");
        cpcl.addText(CpclCommand.TEXT_FONT.FONT_4, 0, 195, "智汇");
        cpcl.addJustification(CpclCommand.ALIGNMENT.CENTER);
        cpcl.addText(CpclCommand.TEXT_FONT.FONT_4, 0, 195, "网络");
        cpcl.addJustification(CpclCommand.ALIGNMENT.RIGHT);
        cpcl.addText(CpclCommand.TEXT_FONT.FONT_4, 0, 195, "设备");
        cpcl.addJustification(CpclCommand.ALIGNMENT.LEFT);
        cpcl.addText(CpclCommand.TEXT_FONT.FONT_4, 0, 230, "Print bitmap!");
//            Bitmap bitmap = BitmapFactory.decodeResource(App.getContext().getResources(), R.drawable.gprinter);
//            cpcl.addEGraphics(0, 255, 385, bitmap);
        cpcl.addText(CpclCommand.TEXT_FONT.FONT_4, 0, 645, "Print code128!");
        cpcl.addBarcodeText(5, 2);
        cpcl.addBarcode(CpclCommand.COMMAND.BARCODE, CpclCommand.CPCLBARCODETYPE.CODE128, 50, 0, 680, "SMARNET");
        cpcl.addText(CpclCommand.TEXT_FONT.FONT_4, 0, 775, "Print QRcode");
        cpcl.addBQrcode(0, 810, "QRcode");
        cpcl.addJustification(CpclCommand.ALIGNMENT.CENTER);
        cpcl.addText(CpclCommand.TEXT_FONT.FONT_4, 0, 1010, "Completed");
        cpcl.addJustification(CpclCommand.ALIGNMENT.LEFT);
        cpcl.addPrint();
        Vector<Byte> datas = cpcl.getCommand();
        return datas;
    }

    /**
     * 获取图片
     *
     * @param mcontext
     * @return
     */
    public static Bitmap getBitmap(Context mcontext) {
//            View v = View.inflate(App.getContext(), R.layout.pj, null);
//            TableLayout tableLayout = (TableLayout) v.findViewById(R.id.li);
//            TextView jine = (TextView) v.findViewById(R.id.jine);
//            TextView pep = (TextView) v.findViewById(R.id.pep);
//            tableLayout.addView(ctv(mcontext, "红茶\n加热\n加糖", 8, 3));
//            tableLayout.addView(ctv(mcontext, "绿茶", 109, 899));
//            tableLayout.addView(ctv(mcontext, "咖啡", 15, 4));
//            tableLayout.addView(ctv(mcontext, "红茶", 8, 3));
//            tableLayout.addView(ctv(mcontext, "绿茶", 10, 8));
//            tableLayout.addView(ctv(mcontext, "咖啡", 15, 4));
//            tableLayout.addView(ctv(mcontext, "红茶", 8, 3));
//            tableLayout.addView(ctv(mcontext, "绿茶", 10, 8));
//            tableLayout.addView(ctv(mcontext, "咖啡", 15, 4));
//            tableLayout.addView(ctv(mcontext, "红茶", 8, 3));
//            tableLayout.addView(ctv(mcontext, "绿茶", 10, 8));
//            tableLayout.addView(ctv(mcontext, "咖啡", 15, 4));
//            tableLayout.addView(ctv(mcontext, "红茶", 8, 3));
//            tableLayout.addView(ctv(mcontext, "绿茶", 10, 8));
//            tableLayout.addView(ctv(mcontext, "咖啡", 15, 4));
//            jine.setText("998");
//            pep.setText("张三");
//            final Bitmap bitmap = convertViewToBitmap(v);
//            return bitmap;
        return null;
    }

    /**
     * mxl转bitmap图片
     *
     * @param view
     * @return
     */
    public static Bitmap convertViewToBitmap(View view) {
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        view.buildDrawingCache();
        Bitmap bitmap = view.getDrawingCache();
        return bitmap;
    }

    public static TableRow ctv(Context context, String name, int k, int n) {
        TableRow tb = new TableRow(context);
        tb.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));
        TextView tv1 = new TextView(context);
        tv1.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
        tv1.setText(name);
        tv1.setTextColor(Color.BLACK);
        tv1.setTextSize(30);
        tb.addView(tv1);
        TextView tv2 = new TextView(context);
        tv2.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
        tv2.setText(k + "");
        tv2.setTextColor(Color.BLACK);
        tv2.setTextSize(30);
        tb.addView(tv2);
        TextView tv3 = new TextView(context);
        tv3.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
        tv3.setText(n + "");
        tv3.setTextColor(Color.BLACK);
        tv3.setTextSize(30);
        tb.addView(tv3);
        return tb;
    }

    /**
     * 打印矩阵二维码
     *
     * @return
     */
    public static Vector<Byte> getNewCommandToPrintQrcode() {
        LabelCommand tsc = new LabelCommand();
        // 设置标签尺寸，按照实际尺寸设置
        tsc.addSize(80, 80);
        // 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0
        tsc.addGap(0);
        // 设置打印方向
        tsc.addDirection(LabelCommand.DIRECTION.FORWARD, LabelCommand.MIRROR.NORMAL);
        // 设置原点坐标
        tsc.addReference(0, 0);
        // 撕纸模式开启
        tsc.addTear(EscCommand.ENABLE.ON);
        // 清除打印缓冲区
        tsc.addCls();
        //添加矩阵打印二维码  旋转
        /**
         * 参数 说明  x横坐标打印起始点   y 纵坐标打印起始点   width  打印宽度 height 打印高度  ROTATION：旋转   content：内容
         */
        tsc.addDMATRIX(10, 10, 400, 400, LabelCommand.ROTATION.ROTATION_90, "DMATRIX EXAMPLE 1");
        /**
         * 参数 说明  x横坐标打印起始点   y 纵坐标打印起始点   width  打印宽度 height 打印高度  content：内容
         */
        tsc.addDMATRIX(110, 10, 200, 200, "DMATRIX EXAMPLE 1");
        /**
         * 参数 说明  x横坐标打印起始点   y 纵坐标打印起始点   width  打印宽度 height 打印高度  Xzoom：放大倍数   content：内容
         */
        tsc.addDMATRIX(210, 10, 400, 400, 6, "DMATRIX EXAMPLE 2");
        /**
         * 参数 说明  x横坐标打印起始点   y 纵坐标打印起始点   width  打印宽度 height 打印高度  c：ASCLL码  Xzomm：放大倍数 content：内容
         */
        tsc.addDMATRIX(10, 200, 100, 100, 126, 6, "~1010465011125193621Gsz9YC24xBbQD~12406404~191ffd0~192Ypg+oU9uLHdR9J5ms0UlqzSPEW7wYQbknUrwOehbz+s+a+Nfxk8JlwVhgItknQEZyfG4Al26Rs/Ncj60ubNCWg==");
        tsc.addPrint(1, 1);
        // 打印标签后 蜂鸣器响
        tsc.addSound(2, 100);
        Vector<Byte> datas = tsc.getCommand();
        // 发送数据
        return datas;
    }

}
