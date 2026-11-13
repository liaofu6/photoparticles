package com.photoparticles.client.video;

import com.photoparticles.client.PhotoParticlesClient;
import com.photoparticles.network.VideoSourcePayload;
import java.io.File;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * 后台解析视频/动图的任务。解析期间可以关掉方块界面去干别的，
 * 进度会用动作栏文字提示，结束后自动上传。
 */
public final class VideoJobs {
    public static final class Job {
        private final BlockPos pos;
        private final File file;
        private volatile float progress;
        private volatile String text = "准备解析";
        private volatile boolean done;
        private volatile String result;

        private Job(BlockPos pos, File file) {
            this.pos = pos;
            this.file = file;
        }

        public BlockPos pos() {
            return pos;
        }

        public File file() {
            return file;
        }

        public float progress() {
            return progress;
        }

        public String text() {
            return text;
        }

        public boolean done() {
            return done;
        }

        private void update(float fraction, String message) {
            this.progress = Math.max(0.0F, Math.min(1.0F, fraction));
            this.text = message;
        }

        private void finish(String message) {
            this.progress = 1.0F;
            this.result = message;
            this.done = true;
        }
    }

    private static volatile Job active;
    private static int ticks;

    private VideoJobs() {
    }

    public static Job active() {
        return active;
    }

    public static void start(BlockPos pos, File file) {
        if (active != null) {
            PhotoParticlesClient.notifyPlayer("已有视频在解析，等它结束");
            return;
        }
        Job job = new Job(pos, file);
        active = job;
        ticks = 0;
        PhotoParticlesClient.notifyPlayer("开始解析视频，可以关掉界面");
        PhotoParticlesClient.runAsync(() -> {
            try {
                byte[] blob = VideoTranscoder.transcode(file, job::update);
                if (blob == null) {
                    job.finish("解析失败，换个小一点的视频试试");
                    return;
                }
                if (blob.length > VideoSourcePayload.MAX_DATA) {
                    job.finish("视频数据超过上限，换短一点的视频");
                    return;
                }
                Minecraft.getInstance().execute(() -> {
                    if (Minecraft.getInstance().getConnection() == null) {
                        job.finish("连接已断开");
                        return;
                    }
                    ClientPlayNetworking.send(
                            new VideoSourcePayload(pos, file.getAbsolutePath(), blob));
                    job.finish("视频已上传 " + String.format("%.2f", blob.length / 1048576.0D) + " MB");
                });
            } catch (Throwable e) {
                job.finish("解析失败：" + e.getMessage());
            }
        });
    }

    /** 每 tick 调用，负责用动作栏刷新进度，客户端断开时清空任务。 */
    public static void tick() {
        Job job = active;
        if (job == null) {
            return;
        }
        if (job.done()) {
            if (job.result != null) {
                PhotoParticlesClient.notifyPlayer(job.result);
            }
            active = null;
            return;
        }
        if (++ticks % 10 == 0) {
            PhotoParticlesClient.notifyPlayer(
                    (int) (job.progress() * 100.0F) + "% " + job.text());
        }
    }
}
