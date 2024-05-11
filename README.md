# AzurLanePaintingAnalysis

通过分析AssetBundle自动计算辅助将多张立绘和差分表情组合成一个完整的立绘图片

**不提供从素材文件还原立绘的功能，使用前提是已经有还原好的各部分立绘。**

例如马耶布雷泽原皮被拆成了四部分，需要四张还原好的各部分立绘

由Python项目[AzurLanePaintingAnalyzer](https://github.com/Deficuet/AzurLanePaintingAnalyzer)重构并多次迭代

### 目录

- [使用方法](#使用方法)
  - [设置](#设置)
  - [操作](#操作)
  - [保存](#保存)
- [启动方法](#启动方法)

![image](https://github.com/Deficuet/AzurLanePaintingAnalysis-Kt/assets/36525579/7437ec70-54c1-4f0f-bc87-7035b61dd923)

# 使用方法

## 设置

> #### 保存图片时的压缩等级

![image](https://github.com/Deficuet/AzurLanePaintingAnalysis-Kt/assets/36525579/95b1cbd6-e383-439f-963a-071f5c840ce6)

- 对PNG图片的无损压缩等级。一般推荐5~7级。不在意占用空间的话也可以调低，最低至0以获取更快的保存速度。最高9级

- **不推荐使用8级及以上**。只比7级小一点点的同时要花费数倍的时间

> #### 素材文件根目录

![image](https://github.com/Deficuet/AzurLanePaintingAnalysis-Kt/assets/36525579/e71218a0-7e41-40f2-baaf-a43b2b25ac40)

第一次使用几乎所有交互都被禁用了，必须先设置导入`素材文件根目录`。

- <b>该目录下必须包含`painting`，`paintingface`文件夹以及`dependencies`文件。</b>

例如在Android文件系统里碧蓝航线的文件结构是
```
/sdcard/Android/data/com.bilibili.azurlane/files/AssetBundles
  - char/
  - ...
  - painting/
  - paintingface/
  - ...
  - dependencies
```
那么素材文件根目录就是`/sdcard/Android/data/com.bilibili.azurlane/files/AssetBundles`

>#### 自动导入

![image](https://github.com/Deficuet/AzurLanePaintingAnalysis-Kt/assets/36525579/a4611dff-0bf8-4229-a8a5-a0c332d185b5)

- 自动导入只涉及导入`立绘图像`和`差分表情的AssetBundle`。`差分表情图像`不支持自动导入。
- 可任选只自动导入其一或者两个都执行自动导入

![image](https://github.com/Deficuet/AzurLanePaintingAnalysis-Kt/assets/36525579/d5edd2a8-d2c8-4eaa-9b52-47d94e77ecac)

- 可在使用之前设置导入路径，避免第一次使用时不得不手动导入的情况。文本框内的路径会即时更新
- 筛选文件格式分别由配置文件内的通配符`painting.imageNamePattern`与`face.bundleNamePattern`控制
  - 通配符内使用`{name}`表示立绘名称，使用时会被替换
- 当自动导入某部件出错时，该部件会被跳过

## 操作
> #### 导入文件

![image](https://github.com/Deficuet/AzurLanePaintingAnalysis-Kt/assets/36525579/89ab7af4-e2da-4031-851d-1bca81560609)

- 初次使用或者当原有路径不存在时，对话框会打开`素材文件根目录`下的`painting`文件夹
- 导入文件只需要无_tex后缀的文件，例如`pangpeimagenuo`, `xiefeierde_4`, `ruoye_2_n`, `lumang_idol_n`。文件来自素材根目录下的`painting`文件夹

  ![image](https://user-images.githubusercontent.com/36525579/163661590-0e1f4415-749e-411e-81f7-5d7475c9ae0b.png)

- 如果出现无法读取文件等情况都会中断分析过程并给出信息。导入文件后会自动加载依赖项文件，用来分析导入立绘时使用的筛选文件名，找不到依赖项时也会显示错误信息并标红
- <b>右键该按钮自动重载当前任务</b>。
  - 例如：当自动导入出错找不到文件，可在更改立绘/差分表情根目录后右键按钮重载当前任务并重新执行自动导入

> #### 导入图像

![image](https://github.com/Deficuet/AzurLanePaintingAnalysis-Kt/assets/36525579/19975069-dc4a-4d20-a0de-626c0d5d48c4)

- 列表内会列出所需部件名称。`$face$`指示该位置为差分表情
  - 未进行过导入操作的部件名称是黑色
  - 在自动导入过程中出错的部件名称会变为红色
  - 成功导入的部件名称会变为蓝色
- 所有立绘都会有`$face$`部分，即便该立绘实际上没有配套差分表情

---

#### 添加图像
- 为列表里被选中的一项导入图像。如果要导入差分表情的图像需先选中`$face$`
  - 立绘的文件筛选格式由配置文件通配符`painting.imageNamePattern`控制
  - 差分表情图像的文件筛选格式由配置文件通配符`face.imageNamePattern`
  - 选择立绘图像时只能单选，而选择差分表情图像时**可以多选**
- 导入立绘图像后，文件夹路径会被同步设置为`立绘根目录`
- 成功导入后列表会自动选取下一项

#### 添加差分表情文件
- 可直接执行而不需要先选中`$face`
- 文件筛选格式由通配符`face.bundleNamePattern`控制
- 初次使用或者原有路径不存在时，对话框会打开`素材文件根目录`下的`paintingface`文件夹
- 导入文件后，文件夹路径会被同步设置为`差分表情根目录`
- 成功导入后，如果当前`$face$`恰好被选中，则列表会自动选取下一项
- 如果某个立绘没有差分表情部分，该按钮不会开放

---

- 每个成功导入的部件都会在右侧预览创建一个新标签页。标签页内包含坐标微调的功能
  - 差分表情的标签页为局部预览，并且额外包含一个可切换选择表情的列表
- 当重新导入时，如果出错则已有内容不会被替换

---

#### 坐标微调
![image](https://github.com/Deficuet/AzurLanePaintingAnalysis-Kt/assets/36525579/d4ba2a0c-5e8f-4b2c-979e-1a58f4998a91)
![image](https://github.com/Deficuet/AzurLanePaintingAnalysis-Kt/assets/36525579/af7bc16e-c117-4e0f-931d-4a0e58140846)

- 横向每+1，**往右**移动一个像素；纵向每+1，**往上**移动一个像素。
- 每个部件都可以独立微调
- <b>调整后需要点击`重新合并`调整才会生效</b>
- 所有差分表情共享坐标及缩放

---

- 切换至`总体预览`及`$face$`标签页，以及选择不同表情时会触发惰性生成预览，所需时间取决于立绘规模

## 保存

![image](https://github.com/Deficuet/AzurLanePaintingAnalysis-Kt/assets/36525579/7ad64647-c3a8-49dc-aa8a-4b07dbb6b213)

- 导入成功任意一个部件即可保存

> #### 普通保存

- 保存当前已合并的立绘。保存路径与`立绘根目录`相同，采取策略为直接覆盖
- 使用设置的压缩等级对立绘图像进行无损压缩
- 如果没有导入过差分表情，则文件名后缀为`_group.png`
- 如果有差分表情，会为**当前被选中的**差分表情保存。文件名后缀为`_exp.png`

> #### 为所有表情保存

- 当且仅当导入了差分表情后开放
- 为**每一个**差分表情导出一张**完整立绘**。保存路径与`立绘根目录`相同，文件名后缀为`_exp_<差分名称>.png`，会自动重命名避免覆盖已有文件
- 使用设置的压缩等级进行压缩，但仍可能会占用大量空间
- 会以蓝字显示保存进度

# 启动方法

- 下载最新的[Release](https://github.com/Deficuet/AzurLanePaintingAnalysis-Kt/releases)后，解压所有文件运行bat即可
- 启动环境：
  - Java 11
  - JavaFX 17.0.11
- 自带一套JavaFX SDK 17.0.11，从官网[JavaFX SDK](https://gluonhq.com/products/javafx/)直接下载获取，bat里已经配置好可以直接用，也可以编辑bat文件自定义启动
- 启动之后自动生成一个配置文件，使用时会自动更新保存

# 成品预览
放个新泽西婚纱

![image](https://github.com/Deficuet/AzurLanePaintingAnalysis-Kt/assets/36525579/f8e53eae-ccd7-4d9c-b37e-791ab80c3868)


