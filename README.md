# Photo Particles

**English** | [简体中文](README.zh-CN.md)

A Minecraft Fabric mod that adds two blocks: the Photo Block and the Video Block. Right-click a block to open an in-game file browser, pick an image or a GIF, and it is displayed as a backlit picture above the block.

The picture is drawn as a dynamic texture on a block entity instead of being assembled from particles, so edges stay clean and there is no line-by-line sweep.

## The two blocks

**Photo Block**: right-click to pick an image. Supports png, jpg, jpeg, bmp and gif. The image is downscaled so its longest side is at most 256 pixels, and it is drawn at 96 pixels per block with nearest-neighbour sampling to keep pixels sharp.

**Video Block**: right-click to pick a GIF and loop it. It is scaled so its longest side is at most 160 pixels, up to 100 frames are read, and the frame advances every 2 game ticks.

Both blocks render emissive, so they never get darker at night, underground or in dim rooms. The picture faces the direction you were facing when you placed the block.

## Requirements

- Minecraft 26.2

- Fabric Loader 0.19.5 or newer

- Fabric API

- Java 25

## Installation

Drop `build/libs/photoparticles-0.1.0.jar` into `.minecraft/mods`. Fabric API must be there as well.

## Building

```
gradlew build
```

Requires JDK 25 on Windows. The output is not remapped, so it can be dropped straight into a Fabric installation.

## Known limitations

- Only GIF is supported for video. MP4 would need external ffmpeg transcoding, which is not implemented.

- A GIF must be 1 MB or smaller, since it is uploaded to the server and saved with the world.

- Photo pixels are stored in the block entity and saved with the world. One image is at most 384×384, and many of them will increase save size.

- Runtimes without `java.desktop` (some Android launchers such as FCL) cannot decode images. The mod detects this and falls back to the plain block model instead of crashing, but no picture is shown.

## License

MIT, see [LICENSE](LICENSE).
