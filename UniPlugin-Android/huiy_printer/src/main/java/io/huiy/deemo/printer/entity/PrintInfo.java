package io.huiy.deemo.printer.entity;

import com.gprinter.command.LabelCommand;

/**
 * 打印内容描述，可以打印文字、二维码、条形码
 */
public class PrintInfo {

    public int type;       // 打印内容的类型： 打印文字(1),打印二维码(2),打印条形码(3)
    public int x;          // 打印起始坐标x
    public int y;          // 打印起始坐标y
    public int rotation;   // 旋转角度：0 90 150 270
    public String text;    // 打印文字的内容

    // 打印文字相关
    public String font;     // 字体类型：1-10、简体中文("TSS24.BF2"),繁体中文("TST24.BF2"), 韩语("K");
    public int scaleX;      // x轴放大系数： 1-10
    public int scaleY;      // y轴放大系数： 1-10

    // 打印二维码相关
    public String level;    // 纠错级别：L M Q H
    public int cellWidth = 5;

    // 打印条形码相关
    public int readable;     //是否打印可识别字符  DISABLE(0),EANBEL(1);
    public int height = 100; // 条形码高度
    public String barType;   // 条形码编码类型： CODE128 ..


    public LabelCommand.ROTATION parseRotaEnum() {
        switch (rotation) {
            case 0:
                return LabelCommand.ROTATION.ROTATION_0;
            case 90:
                return LabelCommand.ROTATION.ROTATION_90;
            case 180:
                return LabelCommand.ROTATION.ROTATION_180;
            case 270:
                return LabelCommand.ROTATION.ROTATION_270;
        }
        return LabelCommand.ROTATION.ROTATION_0;
    }

    public LabelCommand.FONTTYPE parseFontEnum() {
        switch (font) {
            case "1":
                return LabelCommand.FONTTYPE.FONT_1;
            case "2":
                return LabelCommand.FONTTYPE.FONT_2;
            case "3":
                return LabelCommand.FONTTYPE.FONT_3;
            case "4":
                return LabelCommand.FONTTYPE.FONT_4;
            case "5":
                return LabelCommand.FONTTYPE.FONT_5;
            case "6":
                return LabelCommand.FONTTYPE.FONT_6;
            case "7":
                return LabelCommand.FONTTYPE.FONT_7;
            case "8":
                return LabelCommand.FONTTYPE.FONT_8;
            case "9":
                return LabelCommand.FONTTYPE.FONT_9;
            case "10":
                return LabelCommand.FONTTYPE.FONT_10;
            case "TSS24.BF2":
                return LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE;
            case "TST24.BF2":
                return LabelCommand.FONTTYPE.TRADITIONAL_CHINESE;
            case "K":
                return LabelCommand.FONTTYPE.KOREAN;
        }
        return LabelCommand.FONTTYPE.FONT_1;
    }

    public LabelCommand.FONTMUL parseScaleXEnum() {
        switch (scaleX) {
            case 1:
                return LabelCommand.FONTMUL.MUL_1;
            case 2:
                return LabelCommand.FONTMUL.MUL_2;
            case 3:
                return LabelCommand.FONTMUL.MUL_3;
            case 4:
                return LabelCommand.FONTMUL.MUL_4;
            case 5:
                return LabelCommand.FONTMUL.MUL_5;
            case 6:
                return LabelCommand.FONTMUL.MUL_6;
            case 7:
                return LabelCommand.FONTMUL.MUL_7;
            case 8:
                return LabelCommand.FONTMUL.MUL_8;
            case 9:
                return LabelCommand.FONTMUL.MUL_9;
            case 10:
                return LabelCommand.FONTMUL.MUL_10;
        }
        return LabelCommand.FONTMUL.MUL_1;
    }

    public LabelCommand.FONTMUL parseScaleYEnum() {
        switch (scaleY) {
            case 1:
                return LabelCommand.FONTMUL.MUL_1;
            case 2:
                return LabelCommand.FONTMUL.MUL_2;
            case 3:
                return LabelCommand.FONTMUL.MUL_3;
            case 4:
                return LabelCommand.FONTMUL.MUL_4;
            case 5:
                return LabelCommand.FONTMUL.MUL_5;
            case 6:
                return LabelCommand.FONTMUL.MUL_6;
            case 7:
                return LabelCommand.FONTMUL.MUL_7;
            case 8:
                return LabelCommand.FONTMUL.MUL_8;
            case 9:
                return LabelCommand.FONTMUL.MUL_9;
            case 10:
                return LabelCommand.FONTMUL.MUL_10;
        }
        return LabelCommand.FONTMUL.MUL_1;
    }

    public LabelCommand.EEC parseLevelEnum() {
        switch (level) {
            case "L":
                return LabelCommand.EEC.LEVEL_L;
            case "M":
                return LabelCommand.EEC.LEVEL_M;
            case "Q":
                return LabelCommand.EEC.LEVEL_Q;
            case "H":
                return LabelCommand.EEC.LEVEL_H;

        }
        return LabelCommand.EEC.LEVEL_L;
    }

    public LabelCommand.READABEL parseReadableEnum() {
        return readable == 0 ? LabelCommand.READABEL.DISABLE : LabelCommand.READABEL.EANBEL;
    }

    public LabelCommand.BARCODETYPE parseBarTypeEnum() {
        switch (barType) {
            case "128":
                return LabelCommand.BARCODETYPE.CODE128;
            case "128M":
                return LabelCommand.BARCODETYPE.CODE128M;
            case "EAN128":
                return LabelCommand.BARCODETYPE.EAN128;
            case "25":
                return LabelCommand.BARCODETYPE.ITF25;
            case "25C":
                return LabelCommand.BARCODETYPE.ITF25C;
            case "39":
                return LabelCommand.BARCODETYPE.CODE39;
            case "39C":
                return LabelCommand.BARCODETYPE.CODE39C;
            // todo 这里需要补充完整
        }
        return LabelCommand.BARCODETYPE.CODE128;
    }

    public PrintInfo() {

    }

    public static PrintInfo createTextPrint(int x, int y, int rotation, String text, String font, int scaleX, int scaleY) {
        PrintInfo info = new PrintInfo();
        info.type = 1;
        info.x = x;
        info.y = y;
        info.rotation = rotation;
        info.text = text;
        info.font = font;
        info.scaleX = scaleX;
        info.scaleY = scaleY;
        return info;
    }

    public static PrintInfo createQrCodePrint(int x, int y, int rotation, String text, String level, int cellWidth) {
        PrintInfo info = new PrintInfo();
        info.type = 2;
        info.x = x;
        info.y = y;
        info.rotation = rotation;
        info.text = text;
        info.level = level;
        info.cellWidth = cellWidth;
        return info;
    }

    public static PrintInfo create1DBarcodePrint(int x, int y, int rotation, String text, int readable, int height, String barType) {
        PrintInfo info = new PrintInfo();
        info.type = 3;
        info.x = x;
        info.y = y;
        info.rotation = rotation;
        info.text = text;
        info.readable = readable;
        info.height = height;
        info.barType = barType;
        return info;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getFont() {
        return font;
    }

    public void setFont(String font) {
        this.font = font;
    }

    public int getScaleX() {
        return scaleX;
    }

    public void setScaleX(int scaleX) {
        this.scaleX = scaleX;
    }

    public int getScaleY() {
        return scaleY;
    }

    public void setScaleY(int scaleY) {
        this.scaleY = scaleY;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public int getCellWidth() {
        return cellWidth;
    }

    public void setCellWidth(int cellWidth) {
        this.cellWidth = cellWidth;
    }

    public int getReadable() {
        return readable;
    }

    public void setReadable(int readable) {
        this.readable = readable;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getBarType() {
        return barType;
    }

    public void setBarType(String barType) {
        this.barType = barType;
    }

    @Override
    public String toString() {
        return "PrintInfo{" +
                "type=" + type +
                ", x=" + x +
                ", y=" + y +
                ", rotation=" + rotation +
                ", text='" + text + '\'' +
                ", font='" + font + '\'' +
                ", scaleX=" + scaleX +
                ", scaleY=" + scaleY +
                ", level='" + level + '\'' +
                ", cellWidth=" + cellWidth +
                ", readable=" + readable +
                ", barType='" + barType + '\'' +
                '}';
    }
}
