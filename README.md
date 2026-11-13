# Photo Particles

**English** | [简体中文](README.zh-CN.md)

A Minecraft Fabric mod that adds two blocks: the Photo Block and the Video Block. Right-click a block to open an in-game file browser, pick an image or a GIF, and it is displayed as a backlit picture above the block.

The picture is drawn as a dynamic texture on a block entity instead of being assembled from particles, so edges stay clean and there is no line-by-line sweep.

## The two blocks

**Photo Block**: right-click to pick an image. Supports png, jpg, jpeg, bmp and gif. The image is downscaled so its longest side is at most 384 pixels, and it is drawn at 144 pixels per block with nearest-neighbour sampling to keep pixels sharp.

**Video Block**: right-click to pick a GIF or an MP4 and loop it. The file is parsed on the client into up to 40 frames at 192 px on the longest side, then uploaded to the server and stored in the block, so every player on the map can see it playing. Parsing runs on a background thread with a progress readout, and the block screen can be closed while it works.

Both blocks render emissive, so they never get darker at night, underground or in dim rooms. The picture faces the direction you were facing when you placed the block.

## Requirements

- Minecraft 26.2

- Fabric Loader 0.19.5 or newer

- Fabric API

- Java 25

## Installation

Drop `build/libs/photoparticles-0.3.0.jar` into `.minecraft/mods`. Fabric API must be there as well.

## Building

```
gradlew build
```

Requires JDK 25 on Windows. The output is not remapped, so it can be dropped straight into a Fabric installation.

## Known limitations

- Video supports GIF and MP4 only. MP4 decoding is pure Java (JCodec bundled), so it handles H.264; other codecs are not supported.
- Parsed video data must be 8 MB or smaller, since it is uploaded to the server and saved with the world. Longer or larger clips are sampled down to 40 frames first.

- Photo pixels are stored in the block entity and saved with the world. One image is at most 384×384, and many of them will increase save size.

- Runtimes without `java.desktop` (some Android launchers such as FCL) cannot decode images. The mod detects this and falls back to the plain block model instead of crashing, but no picture is shown.

## License

MIT, see [LICENSE](LICENSE). Version history is in [CHANGELOG.md](CHANGELOG.md).
