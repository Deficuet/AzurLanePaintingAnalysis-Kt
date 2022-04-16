# AzurLanePaintingAnalysis
通过分析AssetBundle自动计算并将多张立绘和差分表情组合成一个完整的立绘图片

### 目录

- [启动方法](#启动方法)
- [改动及新增](#改动及新增)
  - [控制保存图片时的压缩等级](#控制保存图片时的压缩等级)
  - [解析AssetBundle](#%E8%A7%A3%E6%9E%90assetbundle)
  - [坐标微调](#坐标微调)
  - [差分接头的局部预览](#差分接头的局部预览)
- [食用方法](#食用方法)
  - [差分接头](#差分接头)

### 立绘合并
![image](https://user-images.githubusercontent.com/36525579/163659659-baebddf8-b848-45ee-8154-b46daea25081.png)

### 差分表情接头
![image](https://user-images.githubusercontent.com/36525579/163659667-325deefe-2280-4523-9459-8caa688757f7.png)


基本布局和用法与[AzurLanePaintingAnalyzer](https://github.com/Deficuet/AzurLanePaintingAnalyzer)相同

## 启动方法
下载最新的[Release](https://github.com/Deficuet/AzurLanePaintingAnalysis-Kt/releases)后，解压所有文件运行bat即可

<b>启动需要openjdk-11环境</b>，编写时使用的是openjdk-11.0.10_9

自带一套JavaFX SDK 11.0.2，从官网[JavaFX SDK](https://gluonhq.com/products/javafx/)直接下载获取，bat里已经配置好可以直接用

也可以编辑bat文件自定义启动

## 改动及新增
- ### 控制保存图片时的压缩等级
![image](https://user-images.githubusercontent.com/36525579/163660015-59cb2b4c-4055-4e13-aa92-2021dc260ac1.png)

对PNG图片的无损压缩。一般用7级就行。不在意占用空间的话也可以调低，最低至0以获取更快的保存速度。最高9级

<b>不推荐使用8级乃至9级。</b>只比7级小一点点的同时要花费数倍的时间

简略测试了一下：

| 压缩等级 | 耗时 |
| ------- | ---- |
|   7级   |  7秒 |
|   8级   | 15秒 |
|   9级   | 37秒 |

- ### 解析AssetBundle
不再使用Unity Tools的WebExtractor以及binary2text。现在解析AssetBundle使用的是我自己写的[UnityKt](https://github.com/Deficuet/UnityKt)，基于[AssetStudio](https://github.com/Perfare/AssetStudio)，免去了大量的文件IO。同时所有`Object`都是惰性加载，只有访问其属性时才会加载，节省了大量时间

- ### 坐标微调
为立绘合并和差分表情接头都提供了粘贴坐标微调的功能。
![image](https://user-images.githubusercontent.com/36525579/163660673-c7406669-f57a-48c5-b6ed-52b0594b20ee.png)
![image](https://user-images.githubusercontent.com/36525579/163660720-69962908-226c-4a16-8479-2af10d8b6591.png)

调整后需要点击`重新计算`调整才会生效

合并立绘时每张立绘都可以独立微调，当然第一张作为基底是调不了的
![image](https://user-images.githubusercontent.com/36525579/163660931-806ca73e-def3-49f7-ac7b-a33a3dc812e8.png)

- ### 差分接头的局部预览
![image](https://user-images.githubusercontent.com/36525579/163661090-7a2d4588-59c8-4389-ad5e-adaa3a380f60.png)

方便检查并微调

差分接头的`总体预览`和`局部预览`展示的图片全部采用惰性加载，减少导入差分表情后的等待时间
## 食用方法
之前用过py写的[AzurLanePaintingAnalyzer](https://github.com/Deficuet/AzurLanePaintingAnalyzer)可以选择略过

![image](https://user-images.githubusercontent.com/36525579/163661419-df0c3f6d-65b4-4827-b1b2-7c646615ace7.png)

导入文件只需要无_tex后缀的文件，例如`pangpeimagenuo`, `xiefeierde_4`, `ruoye_2_n`, `diliyasite_2_rw`，不需要合并或无法读取文件等情况都会报错。文件来自瓜游游戏文件的`painting`文件夹

![image](https://user-images.githubusercontent.com/36525579/163661590-0e1f4415-749e-411e-81f7-5d7475c9ae0b.png)

之后一个一个往里导入立绘的png图片即可。**不提供把立绘碎片拼在一起的功能。**

![image](https://user-images.githubusercontent.com/36525579/163661671-dcb12c8d-0c81-4e05-bc00-954dc15997f3.png)

每导入一个立绘表格会自动选取下一个，一直点导入就行。

所有的都导入完了之后就可以保存了。如果需要微调的话记得点击`重新计算`再保存，一般是不需要微调的

### 差分接头
![image](https://user-images.githubusercontent.com/36525579/163661853-1588a80e-bf7d-4cab-bbb1-f39030d1e397.png)

导入主立绘后开放后两个按钮。导入差分文件需要的是`pantingface`文件夹里的，导入后可以在列表内选择切换预览
![image](https://user-images.githubusercontent.com/36525579/163662005-23338c17-161b-45ad-8b0a-e70385dc4ef4.png)

导入差分图片就是简简单单选个图片然后粘贴。

立绘与差分都导入之后就可以保存了。需要微调的话同样记得先点`重新计算`
