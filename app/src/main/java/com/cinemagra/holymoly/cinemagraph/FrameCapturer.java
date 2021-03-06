package com.cinemagra.holymoly.cinemagraph;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.AndroidUtil;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by jeffjeong on 2017. 11. 19..
 * Modified by sujin.kim
 * https://github.com/nbadal/android-gif-encoder/blob/master/GifEncoder.java
 */
public class FrameCapturer {
    private String videoPathName;
    private ArrayList<Bitmap> bitmapArrayList;
    private Thread thread;

    private Handler handler;

    public void run(String pathName, int[] position, final Handler handler, int width, int height) throws IOException, JCodecException {
        this.handler = handler;

        videoPathName = pathName;
        // 추출할 bitmap 을 담을 array 생성
        bitmapArrayList = new ArrayList<>();

        File file = new File(videoPathName);
        FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file));
        Picture picture;
        Bitmap backgroundBitmap = null;
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        while (null != (picture = grab.getNativeFrame())) {
            Bitmap bitmap = AndroidUtil.toBitmap(picture);// picture의 높이가 넓이보다 크면 90도 로테이션되서 bitmap에 담김
            if(height > width) //그래서 높이가 넓이보다 크면 90도 회전해서 다시 저장
                bitmap = Bitmap.createBitmap(bitmap , 0, 0, bitmap.getWidth(), bitmap .getHeight(), matrix, true);
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, Math.round(width / 2), Math.round(height / 2), true);
            if (backgroundBitmap == null) {
                backgroundBitmap = Bitmap.createBitmap(resized);
                position[0] = Math.round(position[0] / 2);
                position[1] = (int) Math.floor(position[1] / 2);
                position[2] = Math.round(position[2] / 2);
                position[3] = (int) Math.floor(position[3] / 2);
            }
            Bitmap modified = Bitmap.createBitmap(backgroundBitmap);
            for (int y = position[2]; y < position[3]; y++) {
                for (int x = position[0]; x < position[1]; x++) {
                    modified.setPixel(x, y, resized.getPixel(x, y));
                }
            }
            bitmapArrayList.add(modified);
        }

        // Thread start
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                convertGIF(bitmapArrayList);
                handler.sendMessage(Message.obtain(handler, MainActivity.MyHandler.MSG_FINISHED));
            }
        });

        handler.sendMessage(Message.obtain(handler, MainActivity.MyHandler.MSG_STARTED));
        thread.start();
    }

    private void convertGIF(ArrayList<Bitmap> bitmapArrayList) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        AnimatedGifEncoder encoder = new AnimatedGifEncoder();
        encoder.start(bos);

        for (Bitmap bitmap : bitmapArrayList) {
            sendProgress(bitmapArrayList.size() - 1, bitmapArrayList.indexOf(bitmap));
            encoder.addFrame(bitmap);
        }
        encoder.finish();

        FileOutputStream outStream = null;

        String folder = Environment.getExternalStorageDirectory().toString();
        File saveFolder = new File(folder + "/");
        if (!saveFolder.exists()) {
            saveFolder.mkdirs();
        }

        try {
            outStream = new FileOutputStream(saveFolder.getAbsolutePath() + "/test.gif");
            outStream.write(bos.toByteArray());
            outStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendProgress(int total, int current) {
        handler.sendMessage(
                handler.obtainMessage(
                        MainActivity.MyHandler.MSG_PROGRESS, total, current));
    }
}