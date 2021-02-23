**项目说明** 
- 本项目是uniApp插件工程集合
- **UniPlugin-Android** Android插件工程，插件源码。包括陀螺仪、扫码枪、标签打印机、身份证识别、售货机等插件
- **uniplugin_Demo** uniapp demo示例
- 参考资料：https://nativesupport.dcloud.net.cn/NativePlugin/course/android
<br>
 
 
**项目结构** 
```
uni-app_plug-in
├─uniplugin_Demo   uniApp插件实例工程
│    └─nativeplugins  插件存放路径
│        ├─Huiy-Card     身份证插件
│        ├─Huiy-Office   Office文件预览插件
│        ├─Huiy-Printer  打印机插件
│        ├─Huiy-Sale     售货机插件
│        ├─Huiy-Scanner  扫码枪插件
│        └─Huiy-Sensor   陀螺仪插件
│ 
├─UniPlugin-Android     Android插件工程，插件源码 
│    ├─app            App程序，可运行
│    ├─huiy_card      身份证组件module
│    ├─huiy_office    Office文件预览组件module
│    ├─huiy_printer   打印机组件module
│    ├─huiy_sale      售货机组件module
│    ├─huiy_scaner    扫码枪组件module
│    └─huiy_sensor    陀螺仪组件module
│   
```


<br>
 

**陀螺仪插件使用说明** 
- 将uniplugin_Demo\nativeplugins\Huiy-Sensor文件夹copy到 自己工程的 nativeplugins 下面
- 在manifest.json中，选择APP原生插件配置，引入插件
- 在项目中使用如下方法，引入module
```
        var testModule = uni.requireNativePlugin("Huiy-SensorModule")
```

- 使用**registerSensorAFunc**方法，注册陀螺仪监听，通过**ret**回调陀螺仪数据，并实时刷新
```
		testModule.registerSensorAFunc(
			(ret) => {
				this.degree = ret.degree;
				this.x = ret.x;
				this.y = ret.y;
				this.z = ret.z;
				
		})
```
返回参数包括 **degree,x,y,z**
<br>
**degree** 当前方向角度，取值范围**0~360**, 0-北 90-东 180-南 270-西
<br>
**x,y,z** 陀螺仪传感器返回的 x,y,z三轴的原始数据，取值范围为 **-PI~PI**
<br>
 
- 使用**unRegisterSensorFunc**方法，取消陀螺仪监听注册
```
        var ret = testModule.unRegisterSensorFunc(); `
```

- 使用**getSensorInfoFunc**方法，获取陀螺仪当前数据，返回结果和**registerSensorAFunc**相同
```
        var ret = testModule.getSensorInfoFunc();`
```
- 使用**toDegree180Func**方法，将陀螺仪原始坐标转换成角度，入参为**value**，通过**ret.degree**返回数据 取值范围 **-180~180**
```
		var ret = testModule.toDegree180Func({
				'value': -1
			});
		modal.toast({
			message: "toDegree360："+ret.degree,
			duration: 1.5
		});
```

- 使用**toDegree360Func**方法，将陀螺仪原始坐标转换成角度，入参为**value**，通过**ret.degree**返回数据 取值范围 **0~360**
```
		var ret = testModule.toDegree360Func({
				'value': -1
			});
		modal.toast({
			message: "toDegree360："+ret.degree,
			duration: 1.5
		});
```




<br> 
<br> 

**扫码枪插件使用说明** 
- 本插件适配**ZEBRA 斑马**品牌扫码枪，只支持**USB连接**，适配具体型号为**ZEBRA DS2208 条码扫描器**
- 将uniplugin_Demo\nativeplugins\Huiy-Scanner文件夹copy到 自己工程的 nativeplugins 下面
- 在manifest.json中，选择APP原生插件配置，引入插件
- 在项目中使用如下方法，引入module
```
var testModule = uni.requireNativePlugin("Huiy-ScannerModule")
```

- 在项目中使用如下方法，注册一个通知回调，用于接收扫码枪返回的消息
```
		var globalEvent = uni.requireNativePlugin('globalEvent');
			globalEvent.addEventListener('scannerEvent', function(e) {
				console.log("scannerEvent:" + that);
				that.i = e.barcode;
				modal.toast({
					message: "globalEvent收到：" + e.scannerId + " " + e.type + " " + e.msg + " " + e.barcode + " " + e.barType,
					duration: 1.5
				});
			});
```
其中 **scannerId**扫码枪id，**type** 消息类型，**msg** 消息内容，**barcode** 扫码内容，**barType**二维码类型
<br> 

- 使用**initScannerAFunc**方法，初始化组件
```
       testModule.initScannerAFunc(
			(ret) => {
				modal.toast({
					message: "init：" + JSON.stringify(ret),
					duration: 1.5
				});
		});`
```

- 使用**connectScannerAFunc**方法，连接扫码枪
```
       testModule.connectScannerAFunc(
			(ret) => {
				modal.toast({
					message: "init：" + JSON.stringify(ret),
					duration: 1.5
				});
		});`
```
此时使用扫码枪扫描二维码或条形码，扫描结果会通过上面注册的回调返回数据

- 使用**disconnectScannerAFunc**方法，断开连接
```
       testModule.disconnectScannerAFunc(
			(ret) => {
				modal.toast({
					message: "init：" + JSON.stringify(ret),
					duration: 1.5
				});
		});`
```
<br> 
<br> 


**条码打印机插件使用说明** 
- 本插件适配**佳博 Gainscha**品牌条码打印机，只支持**USB连接**，适配具体型号为**佳博 GP-1324D 热敏条码打印机**
- 将uniplugin_Demo\nativeplugins\Huiy-Printer文件夹copy到 自己工程的 nativeplugins 下面
- 在manifest.json中，选择APP原生插件配置，引入插件
- 在项目中使用如下方法，引入module
```
var testModule = uni.requireNativePlugin("Huiy-PrinterModule")
```

- 在项目中使用如下方法，注册一个通知回调，用于接收打印机是否连接成功的消息
```
		globalEvent.addEventListener('myEvent', function(e) {
				console.log("globalEvent:" + that);
				that.i = e.degree;
				modal.toast({
					message: "myEvent收到：" + e.type + " " + e.msg,
					duration: 1.5
				});
		});
```
其中 **type** 消息类型，**msg** 消息内容
<br> 

- 使用**getUsbDevListFunc**方法，获取设备列表
```
       var ret = testModule.getUsbDevListFunc();
```

- 使用**connectUsbDev**方法，连接设备，其中**name**为设备列表中获取的设备名称
```
       var ret = testModule.connectUsbDevFunc({
			'name': this.conDev
		});
```

- 使用**printerLabel**方法，打印标签
```
       var ret = testModule.printerLabel(json);
```
其中**json**为json报文，用于描述打印标签的内容，具体举例如下：

```
       {
		    "direction": 0,
			"gap": 20,
			"height": 30,
			"mirror": 0,
			"width": 40,
			"x": 0,
			"y": 0,
			"printInfoList": [{
				"cellWidth": 5,
				"font": "TSS24.BF2",
				"height": 100,
				"readable": 0,
				"rotation": 0,
				"scaleX": 2,
				"scaleY": 2,
				"text": "悠悠奶茶",
				"type": 1,
				"x": 60,
				"y": 10
			}, {
				"cellWidth": 3,
				"height": 100,
				"level": "L",
				"readable": 0,
				"rotation": 0,
				"scaleX": 0,
				"scaleY": 0,
				"text": "barcode1234567",
				"type": 2,
				"x": 200,
				"y": 75
			}, {
				"barType": "128",
				"cellWidth": 5,
				"height": 50,
				"readable": 1,
				"rotation": 0,
				"scaleX": 0,
				"scaleY": 0,
				"text": "7654321",
				"type": 3,
				"x": 30,
				"y": 160
			}]
	    }
```
最上方**direction、gap、width、height、mirror、x、y** 用于描述标签信息，具体参数说明如下：
<br> 
```
    public int width = 40;      // 标签尺寸（宽）
    public int height = 30;     // 标签尺寸（高）
    public int gap = 20;        // 标签之间的间距

    public int x;               // 打印起始坐标（x）
    public int y;               // 打印起始坐标（y）
    public int direction;       // 打印方向类型：正向打印(0),反方向打印，从下往上(1);
    public int mirror;          // 是否镜像类型：正常(0), 开启镜像(1);
```


**printInfoList**中的内容用于描述打印内容，具体参数说明如下：
```
    // 公共参数
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
```

- 具体参数说明也可参考：\TscCommand说明_Android.pdf
<br> 

<br> 
<br> 


**Office文件预览插件使用说明**
- 本插件使用**TBS腾讯浏览服务**实现office文档预览功能，使用时需要联网，会自动下载安装腾讯浏览器内核。具体参考：https://x5.tencent.com/
- 将uniplugin_Demo\nativeplugins\Huiy-Office文件夹copy到 自己工程的 nativeplugins 下面
- 在manifest.json中，选择APP原生插件配置，引入插件
- 在项目中使用如下方法，使用控件
```
<officeFrame ref="officeView" officeUrl="/storage/emulated/0/excel001.xlsx" style="width:1260;height:700" @onInit="onInit" @click="officeClick"></officeFrame>
```
可以通过**width、height** 属性控制控件大小

- 在项目中使用如下方法，获取腾讯浏览器内核加载情况
```
		onInit(e) {
			this.name = "onInit --> "+e.detail.type + " "+ e.detail.msg+ " "+ e.detail.progress+ " "+ e.detail.isSuccess;		
		},
```
其中 
<br>
**type** 回调类型 1.内核下载中 2. 内核下载完成 3.内核安装完成 4.控件初始化完成（需根据isSuccess判断是否成功），
<br>
**msg** 当前状态描述
<br>
**progress** 下载进度
<br>
**isSuccess** 控件是否加载成功


- 使用**loadFile**方法，加载Android本地文件
```
        officeClick(e) {
		    this.name = "loadFile"
		    this.$refs.officeView.loadFile("/sdcard/ppt001.pptx");
        }，
```






