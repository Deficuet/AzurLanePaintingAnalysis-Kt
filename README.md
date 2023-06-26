# AzurLanePaintingAnalysis
通过分析AssetBundle自动计算辅助将多张立绘和差分表情组合成一个完整的立绘图片

**不提供从素材文件还原立绘的功能，使用前提是用户已经有还原好的各部分立绘。**

例如马耶布雷泽原皮被拆成了四部分，需要四张还原好的各部分立绘


重构了之前写的Python项目[AzurLanePaintingAnalyzer](https://github.com/Deficuet/AzurLanePaintingAnalyzer)

### 目录

- [启动方法](#启动方法)
- [改动及新增](#改动及新增)
  - [控制保存图片时的压缩等级](#控制保存图片时的压缩等级)
  - [坐标微调](#坐标微调)
  - [差分接头的局部预览](#差分接头的局部预览)
  - [一键导出所有差分表情立绘](#一键导出所有差分表情立绘)  ([#2](https://github.com/Deficuet/AzurLanePaintingAnalysis-Kt/issues/2))
- [食用方法](#食用方法)
  - [立绘合并](#立绘合并)
  - [差分接头](#差分接头)

### 立绘合并
![image](https://github.com/Deficuet/AzurLanePaintingAnalysis-Kt/assets/36525579/3b934f96-d8e0-411b-8523-3d33c1acd836)

### 差分表情接头
![image](https://github.com/Deficuet/AzurLanePaintingAnalysis-Kt/assets/36525579/bfd3410d-7198-457f-b728-f1d8cf33254c)

基本布局和用法与[AzurLanePaintingAnalyzer](https://github.com/Deficuet/AzurLanePaintingAnalyzer)相同

## 启动方法
下载最新的[Release](https://github.com/Deficuet/AzurLanePaintingAnalysis-Kt/releases)后，解压所有文件运行bat即可

<b>启动需要openjdk-11环境</b>，编写时使用的是openjdk-11.0.17+8

自带一套JavaFX SDK 11.0.2，从官网[JavaFX SDK](https://gluonhq.com/products/javafx/)直接下载获取，bat里已经配置好可以直接用

也可以编辑bat文件自定义启动

启动之后自动生成一个配置文件，一般不需要修改，使用时会自动更新保存

## 改动及新增
- ### 控制保存图片时的压缩等级
![image](https://user-images.githubusercontent.com/36525579/163660015-59cb2b4c-4055-4e13-aa92-2021dc260ac1.png)

对PNG图片的无损压缩。一般用7级就行。不在意占用空间的话也可以调低，最低至0以获取更快的保存速度。最高9级

<b>不推荐使用8级乃至9级。</b>只比7级小一点点的同时要花费数倍的时间

- ### 自动寻找立绘并导入
![image](https://user-images.githubusercontent.com/36525579/209576558-75ed5654-b567-4e25-bb6e-9a4e24728063.png)

只对立绘合并功能有用。分析完AssetBundle后自动在设置的立绘文件夹下寻找、导入及合并。找不到立绘将会打断自动导入的流程，此时需要手动导入。

- ### 坐标微调
为立绘合并和差分表情接头都提供了粘贴坐标微调的功能。

横向每+1，**往右**移动一个像素；纵向每+1，**往上**移动一个像素。
![image](https://user-images.githubusercontent.com/36525579/163660673-c7406669-f57a-48c5-b6ed-52b0594b20ee.png)
![image](https://user-images.githubusercontent.com/36525579/163660720-69962908-226c-4a16-8479-2af10d8b6591.png)

调整后需要点击`重新合并`调整才会生效

合并立绘时每张立绘都可以独立微调，当然第一张作为基底是调不了的
![image](https://user-images.githubusercontent.com/36525579/163660931-806ca73e-def3-49f7-ac7b-a33a3dc812e8.png)

- ### 差分接头的局部预览
![image](https://user-images.githubusercontent.com/36525579/163661090-7a2d4588-59c8-4389-ad5e-adaa3a380f60.png)

方便检查并微调。展示区域是差分表情图片的矩形区域往四周拓展32个像素

差分接头的`总体预览`和`局部预览`展示的图片全部采用惰性加载，减少导入差分表情后的等待时间

- ### 一键导出所有差分表情立绘
![image](https://github.com/Deficuet/AzurLanePaintingAnalysis-Kt/assets/36525579/47f7637b-3891-4353-ba0c-924f98312de8)

点击`保存所有`会为**每一个**差分表情导出一张**完整立绘**。尽管会自动按照设置的png压缩等级进行压缩，但仍可能会占用大量空间。会自动重命名文件避免覆盖。

## 食用方法

之前用过py写的[AzurLanePaintingAnalyzer](https://github.com/Deficuet/AzurLanePaintingAnalyzer)可以选择略过

### 立绘合并
![image](https://user-images.githubusercontent.com/36525579/163661419-df0c3f6d-65b4-4827-b1b2-7c646615ace7.png)

导入文件只需要无_tex后缀的文件，例如`pangpeimagenuo`, `xiefeierde_4`, `ruoye_2_n`, `diliyasite_2_rw`。文件来自瓜游游戏文件的`painting`文件夹

![image](https://user-images.githubusercontent.com/36525579/163661590-0e1f4415-749e-411e-81f7-5d7475c9ae0b.png)

不需要合并或无法读取文件等情况都会报错。导入文件后会加载依赖项文件用来分析导入立绘时使用的筛选文件名，同一文件夹下找不到依赖项也会报错。

使用的文件名是依赖项文件内的`Texture2D`对象的名字，而不是依赖项文件本身的文件名（以香槟婚纱的文件举例）
![image](https://github.com/Deficuet/AzurLanePaintingAnalysis-Kt/assets/36525579/44078c2b-bf42-4d45-857a-031580e5413f)

![image](https://github.com/Deficuet/AzurLanePaintingAnalysis-Kt/assets/36525579/2e77a5d3-f7b0-4ed6-8739-d4ca1a583262)


之后一个一个往里导入立绘的png图片即可。**不提供把立绘碎片拼在一起的功能。通配符设置可以在config.yml内修改，差分接头同理**

![image](https://user-images.githubusercontent.com/36525579/163661671-dcb12c8d-0c81-4e05-bc00-954dc15997f3.png)

每导入一个立绘表格会自动选取下一个，一直点导入就行。

所有的都导入完了之后就可以保存了。如果需要微调的话记得点击`重新合并`再保存，一般是不需要微调的

### 差分接头
现在会自动判断主立绘需不需要合并，并显示在UI界面上。因此也需要检查依赖项
![image](https://user-images.githubusercontent.com/36525579/163661853-1588a80e-bf7d-4cab-bbb1-f39030d1e397.png)

导入主立绘后开放后两个按钮。导入差分文件需要的是`pantingface`文件夹里的，导入后可以在列表内选择切换预览
![image](https://user-images.githubusercontent.com/36525579/163662005-23338c17-161b-45ad-8b0a-e70385dc4ef4.png)

导入差分图片就是简简单单选个图片然后粘贴。

立绘与差分都导入之后就可以保存了。需要微调的话同样记得先点`重新合并`

## 成品预览
放个埃吉尔
![image](https://github.com/Deficuet/AzurLanePaintingAnalysis-Kt/assets/36525579/0983157c-8f4f-4d59-bc60-d57c63fd8cb4)

其他立绘人像和背景缩放比例不一样还能理解，这人像本身切得稀碎不知道图个啥
