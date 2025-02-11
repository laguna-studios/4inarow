package de.lagunastudios.fourinarow;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import java.util.ArrayList;

public class Game extends Activity implements View.OnTouchListener {

    int id;
    GameLoop mGameLoop;
    TextureView mTextureView;
    Paint red = new Paint();
    Paint yellow = new Paint();
    Paint white = new Paint();
    Paint blue = new Paint();

    boolean you = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        red.setColor(0xffff0000);
        yellow.setColor(0xffDFF24D);
        white.setColor(0xffffffff);
        white.setTextSize(100);
        blue.setColor(0xff0000ff);

        id = getPreferences(0).getInt("id", -1);
        if (id == -1) {
            getPreferences(0).edit().putInt("id", 1).apply();
        }

        mGameLoop = new GameLoop();
        mGameLoop.start();

        mTextureView = new TextureView(this);
        mTextureView.setSurfaceTextureListener(mGameLoop);
        mTextureView.setOnTouchListener(this);

        setContentView(mTextureView);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        /*Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "https://lagunastudios.de/4inarow/123");
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, null);
        startActivity(shareIntent);
        return true;
        */


        if (mGameLoop.winner != null) return true;

        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        int x = (int) (event.getX() / (mGameLoop.mWidth / 7));
        int y = 5;

        while (y >= 0 && mGameLoop.chips[y][x] != 0) {
            y--;
        }
        if (y >= 0) {
            mGameLoop.chips[y][x] = (byte) (you ? 1 : -1);
            you = !you;

            if (getWinner() != 0) {
                mGameLoop.winner = String.format("Player: %s won", getWinner());
            }
        }
        return true;
    }

    public byte[][] getSequences() {
        final byte[][] sequences = new byte[25][7];
        int index = 0;

        for (int row = 0; row < mGameLoop.chips.length; row++, index++) {
            sequences[index] = mGameLoop.chips[row];
        }
        for (int column = 0; column < mGameLoop.chips[0].length; column++, index++) {
            sequences[index] = new byte[7];
            for (int row = 0; row < mGameLoop.chips.length; row++) {
                sequences[index][row] = mGameLoop.chips[row][column];
            }
        }


        sequences[index++] = new byte[]{mGameLoop.chips[3][0], mGameLoop.chips[2][1], mGameLoop.chips[1][2], mGameLoop.chips[0][3]};
        sequences[index++] = new byte[]{mGameLoop.chips[4][0], mGameLoop.chips[3][1], mGameLoop.chips[2][2], mGameLoop.chips[1][3], mGameLoop.chips[0][4]};
        sequences[index++] = new byte[]{mGameLoop.chips[5][0], mGameLoop.chips[4][1], mGameLoop.chips[3][2], mGameLoop.chips[2][3], mGameLoop.chips[1][4], mGameLoop.chips[0][5]};
        sequences[index++] = new byte[]{mGameLoop.chips[5][1], mGameLoop.chips[4][2], mGameLoop.chips[3][3], mGameLoop.chips[2][4], mGameLoop.chips[1][5], mGameLoop.chips[0][6]};
        sequences[index++] = new byte[]{mGameLoop.chips[5][2], mGameLoop.chips[4][3], mGameLoop.chips[3][4], mGameLoop.chips[2][5], mGameLoop.chips[1][6]};
        sequences[index++] = new byte[]{mGameLoop.chips[5][3], mGameLoop.chips[4][4], mGameLoop.chips[3][5], mGameLoop.chips[2][6]};

        sequences[index++] = new byte[]{mGameLoop.chips[2][0], mGameLoop.chips[3][1], mGameLoop.chips[4][2], mGameLoop.chips[5][3]};
        sequences[index++] = new byte[]{mGameLoop.chips[1][0], mGameLoop.chips[2][1], mGameLoop.chips[3][2], mGameLoop.chips[4][3], mGameLoop.chips[5][4]};
        sequences[index++] = new byte[]{mGameLoop.chips[0][0], mGameLoop.chips[1][1], mGameLoop.chips[2][2], mGameLoop.chips[3][3], mGameLoop.chips[4][4], mGameLoop.chips[5][5]};

        sequences[index++] = new byte[]{mGameLoop.chips[0][1], mGameLoop.chips[1][2], mGameLoop.chips[2][3], mGameLoop.chips[3][4], mGameLoop.chips[4][5], mGameLoop.chips[5][6]};
        sequences[index++] = new byte[]{mGameLoop.chips[0][2], mGameLoop.chips[1][3], mGameLoop.chips[2][4], mGameLoop.chips[3][5], mGameLoop.chips[4][6]};
        sequences[index] = new byte[]{mGameLoop.chips[0][3], mGameLoop.chips[1][4], mGameLoop.chips[2][5], mGameLoop.chips[3][6]};


        return sequences;
    }

    public int getWinner() {

        byte[][] sequences = getSequences();

        for (byte[] seq : sequences) {
            byte player = 0;
            byte distance = 0;
            for (byte f : seq) {
                if (f == player) {
                    distance++;
                } else {
                    player = f;
                    distance = 1;
                }
                if (distance >= 4 && player != 0) return player;
            }
        }

        return 0;
    }

    class GameLoop extends Thread implements TextureView.SurfaceTextureListener {

        private boolean mRunning = true;
        final private Object mLock = new Object();

        private SurfaceTexture mSurfaceTexture;
        int mWidth;
        int mHeight;

        String winner;

        public byte[][] chips = {
                {0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0},
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
                        canvas.drawCircle(d/2 + (j*d), 300 + d/2 + (i*d), d/2-8, chips[i][j] == 0 ? white : chips[i][j] < 0 ? red : yellow);
                    }
                 }

                if (winner != null) {
                    canvas.drawText(winner, 50, 100, white);
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