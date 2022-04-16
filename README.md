# AzurLanePaintingAnalysis
通过分析AssetBundle自动计算并将多张立绘和差分表情组合成一个完整的立绘图片

### 目录

- [启动方法](#启动方法)
- [改动及新增](https://github.com/Deficuet/AzurLanePaintingAnalysis-Kt/edit/main/README.md#%E6%94%B9%E5%8A%A8%E5%8F%8A%E6%96%B0%E5%A2%9E)

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
### 控制保存图片时的压缩等级
![image](https://user-images.githubusercontent.com/36525579/163660015-59cb2b4c-4055-4e13-aa92-2021dc260ac1.png)

对PNG图片的无损压缩。一般用7级就行。不在意占用空间的话也可以调低，最低至0以获取更快的保存速度。最高9级

<b>不推荐使用8级乃至9级。</b>只比7级小一点点的同时要花费数倍的时间

简略测试了一下：

| 压缩等级 | 耗时 |
| ------- | ---- |
|   7级   |  7秒 |
|   8级   | 15秒 |
|   9级   | 37秒 |
