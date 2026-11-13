# Changelog

## 0.2.0

- Photos are now decoded up to 384 px on the longest side and drawn at 144 px per block, so the same picture is sharper without getting bigger in the world.

- Downscaling switched to area averaging, which keeps more detail than picking single pixels.

- Video Block now plays GIFs up to 224 px on the longest side, up to 60 frames.

- The GIF is uploaded to the server and stored in the block entity. Every player on the map can see it playing, not just the one who picked the file. GIFs are limited to 1 MB.

## 0.1.0 — first release

- Photo Block and Video Block, both opened by right-clicking into an in-game file browser that can browse every drive.

- Photos are drawn as a dynamic texture on the block entity instead of particles, up to 256 px on the longest side at 96 px per block.

- Video Block loops GIFs up to 160 px on the longest side, reading the local file on the client.

- Both blocks render emissive, so they stay at full brightness at night and underground.

- Picture orientation follows the direction you face when placing the block.

- On runtimes without `java.desktop` (some Android launchers) image decoding is unavailable; the mod detects this and shows the plain block model instead of crashing.

***

# 更新日志

## 0.2.0

- 照片解析上限提到最长边 384 像素，显示密度 96 提升到每格 144 像素，同一张图更清晰，世界里的尺寸不变。

- 缩图算法改成区域平均，比直接抽点保留更多细节。

- 视频方块最长边提到 224 像素，最多 60 帧。

- GIF 本身会上传到服务器存进方块实体，服务器上所有玩家都能看到播放，不只是选文件的人。单个 GIF 限制 1 MB。

## 0.1.0 — 首个版本

- 新增照片方块和视频方块，右键打开游戏内文件浏览器，可以浏览所有磁盘。

- 照片不走粒子，改为方块实体上的动态贴图，最长边 256 像素，每格 96 像素。

- 视频方块循环播放 GIF，最长边 160 像素，读取的是客户端本地文件。

- 两种方块都是自发光渲染，夜里和地下也不会变暗。

- 显示朝向由放置时面对的方向决定。

- 缺少 `java.desktop` 的运行环境（部分安卓启动器）无法解码图片，模组会检测并只显示方块模型，不会崩溃。

