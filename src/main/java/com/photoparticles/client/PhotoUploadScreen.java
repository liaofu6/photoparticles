package com.photoparticles.client;

import com.photoparticles.client.render.PhotoBlockRenderer;
import com.photoparticles.network.UploadPhotoPayload;
import com.photoparticles.network.VideoSourcePayload;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class PhotoUploadScreen extends Screen {
    private static final int MAX_SIDE = 256;
    private static final int MAX_PREVIEW = 220;
    private static final int ROW_HEIGHT = 12;
    private static final int MAX_ENTRIES = 200;
    private static final String[] IMAGE_EXTENSIONS = {".png", ".jpg", ".jpeg", ".bmp", ".gif"};
    private static final String[] VIDEO_EXTENSIONS = {".gif"};

    private final BlockPos pos;
    private final boolean video;
    private File currentDir;
    private final List<Row> rows = new ArrayList<>();
    private int scroll;
    private File selectedFile;
    private int imageWidth;
    private int imageHeight;
    private int[] pixels;
    private Component status = Component.literal("点击左侧文件选择图片");    private Button sendButton;

    private int listX;
    private int listTop;
    private int listWidth;
    private int listHeight;
    private int previewX;
    private int previewY;
    private int previewSize;
    private int contentBottom;

    public PhotoUploadScreen(BlockPos pos, boolean video) {
        super(Component.literal(video ? "视频方块" : "照片方块"));
        this.pos = pos;
        this.video = video;
        if (video) {
            this.status = Component.literal("选择 GIF 作为循环播放的视频");
        }
    }

    @Override
    protected void init() {
        File home = new File(System.getProperty("user.home", "."));
        File pictures = new File(home, "Pictures");
        this.currentDir = (pictures.isDirectory() ? pictures : home).getAbsoluteFile();
        this.refresh();

        int margin = 20;
        this.listX = margin;
        this.listTop = 52;
        this.contentBottom = this.height - 52;
        int contentHeight = Math.max(80, this.contentBottom - this.listTop);
        this.listHeight = contentHeight;
        this.listWidth = Math.min(280, Math.max(160, (int) (this.width * 0.36D)));

        int columnX = this.listX + this.listWidth + margin;
        int columnWidth = Math.max(80, this.width - columnX - margin);
        this.previewSize = Math.min(MAX_PREVIEW, Math.min(columnWidth, contentHeight - 18));
        this.previewX = columnX + (columnWidth - this.previewSize) / 2;
        this.previewY = this.listTop + (contentHeight - 18 - this.previewSize) / 2;

        int buttonY = this.height - 34;
        int buttonWidth = 110;
        int gap = 10;
        int startX = this.width / 2 - (buttonWidth * 3 + gap * 2) / 2;

        this.addRenderableWidget(Button.builder(Component.literal("上级目录"), b -> this.goUp())
                .bounds(startX, buttonY, buttonWidth, 20)
                .build());

        this.sendButton = Button.builder(Component.literal("发送到方块"), b -> this.send())
                .bounds(startX + buttonWidth + gap, buttonY, buttonWidth, 20)
                .build();
        this.addRenderableWidget(this.sendButton);

        this.addRenderableWidget(Button.builder(Component.literal("关闭"), b -> this.onClose())
                .bounds(startX + (buttonWidth + gap) * 2, buttonY, buttonWidth, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (this.sendButton != null) {
            this.sendButton.active = this.video ? this.selectedFile != null : this.pixels != null;
        }

        graphics.centeredText(this.font, this.title, this.width / 2, 16, 0xFFFFFFFF);
        graphics.text(this.font, this.currentDir == null ? "此电脑" : this.currentDir.getAbsolutePath(),
                this.listX, this.listTop - 12, 0xFF9A9A9A);

        // 左侧文件列表：外边框 + 内底色
        graphics.fill(this.listX - 2, this.listTop - 2, this.listX + this.listWidth + 2,
                this.listTop + this.listHeight + 2, 0xFF4A4A4A);
        graphics.fill(this.listX, this.listTop, this.listX + this.listWidth,
                this.listTop + this.listHeight, 0xFF141414);

        int visibleRows = Math.max(1, this.listHeight / ROW_HEIGHT);
        for (int i = 0; i < visibleRows; i++) {
            int index = this.scroll + i;
            if (index >= this.rows.size()) {
                break;
            }
            Row row = this.rows.get(index);
            int rowY = this.listTop + i * ROW_HEIGHT;
            boolean hovered = mouseX >= this.listX && mouseX < this.listX + this.listWidth
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            if (hovered) {
                graphics.fill(this.listX, rowY, this.listX + this.listWidth, rowY + ROW_HEIGHT, 0xFF333333);
            } else if (row.file.equals(this.selectedFile)) {
                graphics.fill(this.listX, rowY, this.listX + this.listWidth, rowY + ROW_HEIGHT, 0xFF26412A);
            }
            String label = row.directory ? "[目录] " + row.file.getName() : row.file.getName();
            String shown = this.font.plainSubstrByWidth(label, this.listWidth - 8);
            graphics.text(this.font, shown, this.listX + 4, rowY + 2, row.directory ? 0xFFE0C060 : 0xFFE8E8E8);
        }

        // 列表滚动条
        if (this.rows.size() > visibleRows) {
            int trackX = this.listX + this.listWidth - 3;
            int trackHeight = this.listHeight;
            int thumbHeight = Math.max(10, trackHeight * visibleRows / this.rows.size());
            int maxScroll = this.rows.size() - visibleRows;
            int thumbY = this.listTop + (trackHeight - thumbHeight) * this.scroll / Math.max(1, maxScroll);
            graphics.fill(trackX, this.listTop, trackX + 2, this.listTop + trackHeight, 0xFF2A2A2A);
            graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, 0xFF8A8A8A);
        }

        // 右侧预览框
        graphics.fill(this.previewX - 2, this.previewY - 2, this.previewX + this.previewSize + 2,
                this.previewY + this.previewSize + 2, 0xFF4A4A4A);
        graphics.fill(this.previewX, this.previewY, this.previewX + this.previewSize,
                this.previewY + this.previewSize, 0xFF141414);
        if (this.pixels != null) {
            int cell = Math.max(1, Math.min(this.previewSize / this.imageWidth, this.previewSize / this.imageHeight));
            int drawWidth = cell * this.imageWidth;
            int drawHeight = cell * this.imageHeight;
            int originX = this.previewX + (this.previewSize - drawWidth) / 2;
            int originY = this.previewY + (this.previewSize - drawHeight) / 2;
            for (int y = 0; y < this.imageHeight; y++) {
                for (int x = 0; x < this.imageWidth; x++) {
                    int color = this.pixels[y * this.imageWidth + x];
                    if ((color >>> 24) == 0) {
                        continue;
                    }
                    int px = originX + x * cell;
                    int py = originY + y * cell;
                    graphics.fill(px, py, px + cell, py + cell, color);
                }
            }
        }

        int statusY = this.previewY + this.previewSize + 6;
        graphics.centeredText(this.font, this.status, this.previewX + this.previewSize / 2,
                Math.min(statusY, this.contentBottom - 10), 0xFFD0D0D0);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (super.mouseClicked(event, doubled)) {
            return true;
        }
        if (event.button() != 0) {
            return false;
        }
        double mouseX = event.x();
        double mouseY = event.y();
        if (mouseX < this.listX || mouseX >= this.listX + this.listWidth
                || mouseY < this.listTop || mouseY >= this.listTop + this.listHeight) {
            return false;
        }
        int visibleRows = Math.max(1, this.listHeight / ROW_HEIGHT);
        int index = this.scroll + (int) ((mouseY - this.listTop) / ROW_HEIGHT);
        if (index < 0 || index >= this.rows.size() || index >= this.scroll + visibleRows) {
            return false;
        }
        Row row = this.rows.get(index);
        if (row.directory) {
            this.currentDir = row.file;
            this.scroll = 0;
            this.refresh();
        } else {
            this.selectedFile = row.file;
            this.loadImage(row.file);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= this.listX && mouseX < this.listX + this.listWidth
                && mouseY >= this.listTop && mouseY < this.listTop + this.listHeight) {
            int visibleRows = Math.max(1, this.listHeight / ROW_HEIGHT);
            int maxScroll = Math.max(0, this.rows.size() - visibleRows);
            this.scroll = Math.max(0, Math.min(maxScroll, this.scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void goUp() {
        if (this.currentDir == null) {
            return;
        }
        File parent = this.currentDir.getParentFile();
        this.currentDir = parent;
        this.scroll = 0;
        this.refresh();
    }

    private void refresh() {
        this.rows.clear();
        if (this.currentDir == null) {
            // 第一层显示所有磁盘分区
            for (File root : File.listRoots()) {
                this.rows.add(new Row(root, true));
            }
            this.status = Component.literal("选择磁盘");
            return;
        }
        File[] entries = this.currentDir.listFiles();
        if (entries == null) {
            this.status = Component.literal("无法访问该目录");
            return;
        }
        Arrays.sort(entries, (a, b) -> {
            if (a.isDirectory() != b.isDirectory()) {
                return a.isDirectory() ? -1 : 1;
            }
            return a.getName().compareToIgnoreCase(b.getName());
        });
        for (File entry : entries) {
            if (this.rows.size() >= MAX_ENTRIES) {
                break;
            }
            if (entry.isDirectory()) {
                this.rows.add(new Row(entry, true));
            } else if (isImage(entry)) {
                this.rows.add(new Row(entry, false));
            }
        }
        this.status = Component.literal("共 " + this.rows.size() + " 项");
    }

    private boolean isImage(File file) {
        String name = file.getName().toLowerCase();
        String[] extensions = this.video ? VIDEO_EXTENSIONS : IMAGE_EXTENSIONS;
        for (String extension : extensions) {
            if (name.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private void loadImage(File file) {
        if (!PhotoBlockRenderer.imageSupportAvailable()) {
            this.status = Component.literal("当前运行环境缺少图片解码能力，只能显示方块模型");
            return;
        }
        BufferedImage source;
        try {
            source = ImageIO.read(file);
        } catch (Throwable e) {
            this.status = Component.literal("图片读取失败");
            return;
        }
        if (source == null) {
            this.status = Component.literal("不支持的图片格式");
            return;
        }
        int srcWidth = source.getWidth();
        int srcHeight = source.getHeight();
        double scale = Math.min(1.0D, (double) MAX_SIDE / Math.max(srcWidth, srcHeight));
        int newWidth = Math.max(1, (int) Math.round(srcWidth * scale));
        int newHeight = Math.max(1, (int) Math.round(srcHeight * scale));
        int[] result = new int[newWidth * newHeight];
        for (int y = 0; y < newHeight; y++) {
            int srcY = Math.min(srcHeight - 1, y * srcHeight / newHeight);
            for (int x = 0; x < newWidth; x++) {
                int srcX = Math.min(srcWidth - 1, x * srcWidth / newWidth);
                result[y * newWidth + x] = source.getRGB(srcX, srcY);
            }
        }
        this.imageWidth = newWidth;
        this.imageHeight = newHeight;
        this.pixels = result;
        this.status = Component.literal("已解析 " + newWidth + " x " + newHeight + " 像素");
    }

    private void send() {
        if (this.video) {
            if (this.selectedFile == null) {
                this.status = Component.literal("请先在左侧选择 GIF 文件");
                return;
            }
            ClientPlayNetworking.send(new VideoSourcePayload(this.pos, this.selectedFile.getAbsolutePath()));
            this.onClose();
            return;
        }
        if (this.pixels == null) {
            this.status = Component.literal("请先在左侧选择图片");
            return;
        }
        ClientPlayNetworking.send(new UploadPhotoPayload(this.pos, this.imageWidth, this.imageHeight, this.pixels));
        this.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Row(File file, boolean directory) {
    }
}
