package de.lagunastudios.fourinarow;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

public class Game extends Activity implements View.OnTouchListener {

    GameLoop mGameLoop;
    TextureView mTextureView;
    Paint red = new Paint();
    Paint yellow = new Paint();
    Paint white = new Paint();
    Paint blue = new Paint();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        red.setColor(0xffff0000);
        yellow.setColor(0xffDFF24D);
        white.setColor(0xffffffff);
        blue.setColor(0xff0000ff);

        mGameLoop = new GameLoop();
        mGameLoop.start();

        mTextureView = new TextureView(this);
        mTextureView.setSurfaceTextureListener(mGameLoop);
        mTextureView.setOnTouchListener(this);

        setContentView(mTextureView);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return true;
    }

    class GameLoop extends Thread implements TextureView.SurfaceTextureListener {

        private boolean mRunning = true;
        final private Object mLock = new Object();

        private SurfaceTexture mSurfaceTexture;
        private int mWidth;
        private int mHeight;

        public byte[] chips = {
                0,0,0,0,0,0,0,
                1,1,-1,1,1,-1,1,
                -1,-1,-1,1,-1,-1,0,
                1,1,1,-1,1,1,1,
                -1,1,-1,1,-1,-1,-1,
                1,-1,1,0,1,-1,-1,
        };

        @Override
        public void run() {
            while (mRunning) {
                while (mSurfaceTexture == null) {
                    synchronized (mLock) {
                        try {
                            mLock.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        if (!mRunning) break;
                    }
                }

                update();
            }
        }

        private void update() {
            final Surface surface;
            synchronized (mLock) {
                if (mSurfaceTexture == null) return;
                surface = new Surface(mSurfaceTexture);
            }

            while (true) {
                final Canvas canvas = surface.lockCanvas(null);
                if (canvas == null) break;

                canvas.drawRect(0,0, mWidth, mHeight, blue);

                int d = mWidth / 7 < mHeight / 6 ? mWidth / 7 : mHeight / 6;

                for (int i = 0; i < 6; i++) {
                    for (int j = 0; j < 7; j++) {
                        canvas.drawCircle(d/2 + (j*d), 300 + d/2 + (i*d), d/2-8, chips[j + (i * 7)] == 0 ? white : chips[i * 7 + j] < 0 ? red : yellow);
                    }
                 }

                surface.unlockCanvasAndPost(canvas);
            }

            surface.release();
        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture s, int w, int h) {
            mWidth = w;
            mHeight = h;

            synchronized (mLock) {
                mSurfaceTexture = s;
                mLock.notify();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture s, int w, int h) {
            mWidth = w;
            mHeight = h;
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture s) {
            mRunning = false;
            synchronized (mLock) {
                mSurfaceTexture = null;
            }
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture s) {

        }
    }
}