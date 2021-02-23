package io.huiy.deemo.printer.entity;

import com.gprinter.command.LabelCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * 标签
 */
public class Label {

    public int width = 40;      // 标签尺寸（宽）
    public int height = 30;     // 标签尺寸（高）
    public int gap = 20;        // 标签之间的间距

    public int x;               // 打印起始坐标（x）
    public int y;               // 打印起始坐标（y）
    public int direction;       // 打印方向类型：正向打印(0),反方向打印，从下往上(1);
    public int mirror;          // 是否镜像类型：正常(0), 开启镜像(1);

    public List<PrintInfo> printInfoList = new ArrayList<>();

    public LabelCommand.DIRECTION parseDirEnum() {
        return direction == 0 ? LabelCommand.DIRECTION.FORWARD : LabelCommand.DIRECTION.BACKWARD;
    }

    public LabelCommand.MIRROR parseMirEnum() {
        return mirror == 0 ? LabelCommand.MIRROR.NORMAL : LabelCommand.MIRROR.MIRROR;
    }


    public Label() {

    }

    public Label(int width, int height, int x, int y, int gap, int direction, int mirror) {
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
        this.gap = gap;
        this.direction = direction;
        this.mirror = mirror;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getGap() {
        return gap;
    }

    public void setGap(int gap) {
        this.gap = gap;
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

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public int getMirror() {
        return mirror;
    }

    public void setMirror(int mirror) {
        this.mirror = mirror;
    }

    public List<PrintInfo> getPrintInfoList() {
        return printInfoList;
    }

    public void setPrintInfoList(List<PrintInfo> printInfoList) {
        this.printInfoList = printInfoList;
    }

    @Override
    public String toString() {
        return "Label{" +
                "width=" + width +
                ", height=" + height +
                ", x=" + x +
                ", y=" + y +
                ", gap=" + gap +
                ", direction=" + direction +
                ", mirror=" + mirror +
                ", printInfoList=" + printInfoList +
                '}';
    }
}
