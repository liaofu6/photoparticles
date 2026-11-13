# Changelog

## 0.2.0 — first public release

- Photo Block and Video Block, both opened by right-clicking into an in-game file browser that can browse every drive.
- Photos are drawn as a dynamic texture on the block entity instead of particles. Up to 384 px on the longest side, 144 px per block, nearest-neighbour sampling so pixels stay sharp.
- Images are downscaled with area averaging, which keeps more detail than plain sampling.
- Video Block loops GIFs up to 224 px on the longest side, up to 60 frames, advancing every 2 game ticks.
- The GIF itself is uploaded to the server and stored in the block entity, so every player can see it playing, not just the one who picked the file.
- Both blocks render emissive, so they stay at full brightness at night and underground.
- Picture orientation follows the direction you face when placing the block.
- On runtimes without `java.desktop` (some Android launchers) image decoding is unavailable; the mod detects this and shows the plain block model instead of crashing.

## 0.1.0 — internal build

Local test build, never published. No separate changes.

---

# 更新日志

## 0.2.0 — 首个公开版本

- 新增照片方块和视频方块，右键打开游戏内文件浏览器，可以浏览所有磁盘。
- 照片不走粒子，改为方块实体上的动态贴图。最长边最多 384 像素，每格 144 像素，最近邻采样，像素不发虚。
- 缩图改成区域平均，比直接抽样保留更多细节。
- 视频方块循环播放 GIF，最长边最多 224 像素，最多 60 帧，每 2 游戏刻换一帧。
- GIF 本身会上传到服务器存进方块实体，服务器上所有玩家都能看到播放，不只是选文件的人。
- 两种方块都是自发光渲染，夜里和地下也不会变暗。
- 显示朝向由放置时面对的方向决定。
- 缺少 `java.desktop` 的运行环境（部分安卓启动器）无法解码图片，模组会检测并只显示方块模型，不会崩溃。

## 0.1.0 — 内部构建

本地测试用，未发布，没有独立改动。
