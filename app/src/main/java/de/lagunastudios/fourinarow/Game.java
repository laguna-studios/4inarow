package de.lagunastudios.fourinarow;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        int[] game = new int[42];
        int pos = 0;
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 7; j++, pos++) {
                game[pos] = mGameLoop.chips[i][j];
            }
        }
        outState.putIntArray("game", game);
    }

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
        if (savedInstanceState != null && savedInstanceState.containsKey("game")) {
            int[] game = savedInstanceState.getIntArray("game");
            int pos = 0;
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < 7; j++, pos++) {
                    mGameLoop.chips[i][j] = (byte) game[pos];
                }
            }
        }

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


        mGameLoop.onInput(event);
        return true;
    }

    class GameLoop extends Thread implements TextureView.SurfaceTextureListener {
        private boolean mRunning = true;
        final private Object mLock = new Object();

        private SurfaceTexture mSurfaceTexture;
        int d;
        int mWidth;
        int mHeight;
        int xOffset;
        int yOffset;

        int winner = 0;

        public byte[][] chips = {
                {0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0},
        };

        public byte[][] getSequences() {
            final byte[][] sequences = new byte[25][7];
            int index = 0;

            for (int row = 0; row < chips.length; row++, index++) {
                sequences[index] = chips[row];
            }
            for (int column = 0; column < chips[0].length; column++, index++) {
                sequences[index] = new byte[7];
                for (int row = 0; row < chips.length; row++) {
                    sequences[index][row] = chips[row][column];
                }
            }

            byte[][] c = mGameLoop.chips;

            sequences[index++] = new byte[]{c[3][0], c[2][1], c[1][2], c[0][3]};
            sequences[index++] = new byte[]{c[4][0], c[3][1], c[2][2], c[1][3], c[0][4]};
            sequences[index++] = new byte[]{c[5][0], c[4][1], c[3][2], c[2][3], c[1][4], c[0][5]};
            sequences[index++] = new byte[]{c[5][1], c[4][2], c[3][3], c[2][4], c[1][5], c[0][6]};
            sequences[index++] = new byte[]{c[5][2], c[4][3], c[3][4], c[2][5], c[1][6]};
            sequences[index++] = new byte[]{c[5][3], c[4][4], c[3][5], c[2][6]};

            sequences[index++] = new byte[]{c[2][0], c[3][1], c[4][2], c[5][3]};
            sequences[index++] = new byte[]{c[1][0], c[2][1], c[3][2], c[4][3], c[5][4]};
            sequences[index++] = new byte[]{c[0][0], c[1][1], c[2][2], c[3][3], c[4][4], c[5][5]};

            sequences[index++] = new byte[]{c[0][1], c[1][2], c[2][3], c[3][4], c[4][5], c[5][6]};
            sequences[index++] = new byte[]{c[0][2], c[1][3], c[2][4], c[3][5], c[4][6]};
            sequences[index] = new byte[]{c[0][3], c[1][4], c[2][5], c[3][6]};


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

        void onInput(MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_UP) return;

            if (winner != 0) {
                winner = 0;
                for (int i = 0; i < chips.length; i++) {
                    for (int j = 0; j < chips[0].length; j++) {
                        chips[i][j] = 0;
                    }
                }
                return;
            }

            int x = (int) event.getX();

            if (x < xOffset || xOffset + d * 7 < x) return;
            x = (x - xOffset) / d;

            int y = 5;

            while (y >= 0 && chips[y][x] != 0) {
                y--;
            }
            if (y >= 0) {
                chips[y][x] = (byte) (you ? 1 : -1);
                you = !you;
            }
        }

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

                prepareUpdate();
            }
        }

        private void update(Canvas canvas) {

            d = mWidth / 7 < mHeight / 6 ? mWidth / 7 : mHeight / 6;
            xOffset = (mWidth - d*7) / 2;
            yOffset = (mHeight - d*6) / 2;

            canvas.drawRect(0,0, mWidth, mHeight, blue);


                for (int i = 0; i < 6; i++) {
                    for (int j = 0; j < 7; j++) {
                        canvas.drawCircle(xOffset + d/2 + (j*d),   yOffset + d/2 + (i*d), d/2-8, chips[i][j] == 0 ? white : chips[i][j] < 0 ? red : yellow);
                    }
                }



            if ((winner = getWinner()) != 0) {
                canvas.drawText(winner < 0 ? "Rot gewinnt" : "Gelb gewinnt", 50, 100, white);
            }
        }

        private void prepareUpdate() {
            final Surface surface;
            synchronized (mLock) {
                if (mSurfaceTexture == null) return;
                surface = new Surface(mSurfaceTexture);
            }

            while (true) {
                Canvas canvas;
                try {
                    canvas = surface.lockCanvas(null);
                    if (canvas == null) break;
                    update(canvas);
                    surface.unlockCanvasAndPost(canvas);
                } catch (IllegalArgumentException e) {
                    break;
                }
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
        public void onSurfaceTextureUpdated(SurfaceTexture s) {}
    }
}