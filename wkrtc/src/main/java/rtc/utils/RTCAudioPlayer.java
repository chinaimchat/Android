package rtc.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;

import java.io.IOException;

/**
 * 5/8/21 12:10 PM
 */
public class RTCAudioPlayer {
    private RTCAudioPlayer() {

    }

    private static class LiMAudioPlayerBinder {
        static final RTCAudioPlayer player = new RTCAudioPlayer();
    }

    public static RTCAudioPlayer getInstance() {
        return LiMAudioPlayerBinder.player;
    }

    MediaPlayer player;

    public void play(Context context, String fileName, boolean isLoop) {
        stopPlay();
        player = new MediaPlayer();
        try {
            AssetManager assetManager = context.getResources().getAssets();
            AssetFileDescriptor fileDescriptor = assetManager.openFd(fileName);
            player.setDataSource(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(), fileDescriptor.getStartOffset());
            player.setOnPreparedListener(mp -> player.start());
            player.prepareAsync();
            player.setLooping(isLoop);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void stopPlay() {
        if (player != null && player.isPlaying()) {
            player.stop();
            player.release();
            player = null;
        }
    }
}
