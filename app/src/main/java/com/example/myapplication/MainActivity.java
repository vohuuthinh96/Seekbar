package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    RangeSeekBarView seekbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String filePath= "/storage/emulated/0/Demo.mp4";
        Handler handler1 = new Handler();
        Runnable runnable1 = new Runnable() {
            @Override
            public void run() {
                if (!seekbar.isPaused){
                    seekbar.setCurrentPosition(seekbar.getCurPosition()+10);
                    handler1.postDelayed(this::run, 100);
                }
            }
        };
        seekbar= findViewById(R.id.seekbar);
        findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                seekbar.isPaused = false;
                handler1.postDelayed(runnable1,0);
            }
        });

        findViewById(R.id.pause).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handler1.removeCallbacks(runnable1);
                seekbar.isPaused = true;
                seekbar.invalidate();
            }
        });

        File file=new File(filePath);
        if (file.exists()){
            long duration= getDuration(filePath);
            seekbar.setDuration(duration);
            seekbar.setEndPosition(duration-1000 );
            seekbar.setStartPosition(1000);
            seekbar.setCurrentPosition(1000);
            getBitmapFromVideo(filePath);

            seekbar.setOnRangeSeekBarChangeListener(new RangeSeekBarView.OnRangeSeekBarChangeListener() {
                @Override
                public void onProgressChanged(RangeSeekBarView seekBar, long progress) {

                }

                @Override
                public void onStartTrackingTouch(RangeSeekBarView seekBar) {

                }

                @Override
                public void onStopTrackingTouch(RangeSeekBarView seekBar) {

                }

                @Override
                public void onRangeChanged(RangeSeekBarView seekBar, long start, long end, boolean leftChanged) {

                }
            });
        }

        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                seekbar.setCurrentPosition(seekbar.getCurPosition()+10);
                handler.removeCallbacks(this);
                handler.postDelayed(this,100);
            }
        };
        seekbar.setStartPosition(seekbar.getCurPosition());
        handler.postDelayed(runnable,4000);

    }
    public void getBitmapFromVideo(final String path) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int maxPreview = seekbar.getMaxPreview();
                long step = getDuration(path) / maxPreview;
                for (long ms = 0; ms < getDuration(path); ms += step) {
                    Bitmap bitmap = getBitmapAt(path, ms);
                    if (Utils.isBitmapExist(bitmap))
                        seekbar.addPreviewBitmap(bitmap);
                }
            }
        }).start();
    }

    private Bitmap getBitmapAt(String pathVideo, long ms) {
        MediaMetadataRetriever mediaMetadataRetriever = null;
        try {
            mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(MainActivity.this, Uri.parse(pathVideo));
            return mediaMetadataRetriever.getFrameAtTime(ms * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        } catch (Exception ex) {
            Log.e("loadThumb", "run: " + ex);
        } finally {
            if (mediaMetadataRetriever != null)
                mediaMetadataRetriever.release();
        }

        return null;
    }

    public long getDuration(String path){
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(MainActivity.this, Uri.parse(path));
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long timeInMillisec = Long.parseLong(time);
            retriever.release();
            return timeInMillisec;
        } catch (Exception ex) {
            Log.e("thinh.vh", "getDuration: " + ex.getMessage());
        }
        return 0;
    }
}