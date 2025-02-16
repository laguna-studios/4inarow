package de.lagunastudios.fourinarow;

import android.app.Activity;
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class Game extends Activity implements View.OnTouchListener {

    Multiplayer mMultiplayer;
    GameLoop mGameLoop;
    TextureView mTextureView;
    Paint red = new Paint();
    Paint yellow = new Paint();
    Paint white = new Paint();
    Paint blue = new Paint();

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
        red.setTextSize(100);
        red.setTextAlign(Paint.Align.CENTER);
        yellow.setColor(0xffDFF24D);
        yellow.setTextSize(100);
        yellow.setTextAlign(Paint.Align.CENTER);
        white.setColor(0xffffffff);
        white.setTextSize(100);
        white.setTextAlign(Paint.Align.CENTER);
        blue.setColor(0xff0000ff);

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

        if (getIntent().getData() != null) {
            mMultiplayer = new Multiplayer();
            mMultiplayer.start();
            mGameLoop.state = 1;
        }


        mTextureView = new TextureView(this);
        mTextureView.setSurfaceTextureListener(mGameLoop);
        mTextureView.setOnTouchListener(this);

        setContentView(mTextureView);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mGameLoop.onInput(event);
        return true;
    }

    class GameLoop extends Thread implements TextureView.SurfaceTextureListener {
        private int state = 0;

        private boolean mRunning = true;
        final private Object mLock = new Object();

        private SurfaceTexture mSurfaceTexture;
        int d;
        int mWidth;
        int mHeight;
        int xOffset;
        int yOffset;

        int winner = 0;
        boolean red = true;

        public byte[][] chips = {
                {0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0},
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

        void onMultiplayerInput(MotionEvent event) {
            if (winner != 0) return;

            int x = (int) event.getX();

            if (x < xOffset || xOffset + d * 7 < x) return;
            x = (x - xOffset) / d;

            mMultiplayer.makeMove(x);
        }

        void onInput(MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_UP) return;

            if (state == 0) {
                if (mHeight / 3 * 1.4f + 100 < event.getY() && event.getY() < mHeight / 3 * 1.4f + 200) {
                    mMultiplayer = new Multiplayer();
                    mMultiplayer.start();
                    return;
                } else if (mHeight / 3 * 1.4f - 100 < event.getY() && event.getY() < mHeight / 3 * 1.4f) {
                    state = 1;
                    return;
                }
            }

            if (mMultiplayer != null && mMultiplayer.inGame) {
                onMultiplayerInput(event);
                return;
            }

            if (winner != 0) {
                winner = 0;
                for (int i = 0; i < chips.length; i++) {
                    for (int j = 0; j < chips[0].length; j++) {
                        chips[i][j] = 0;
                    }
                }
                state = 0;
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
                chips[y][x] = (byte) (red ? -1 : 1);
                red = !red;
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

            if (state == 0) {
                canvas.drawRect(0, 0, mWidth, mHeight, blue);
                white.setTextSize(200);
                canvas.drawText("4 In A Row", mWidth/2, mHeight / 3, white);
                white.setTextSize(100);
                canvas.drawText("Local Game", mWidth/2, mHeight / 3 * 1.4f, white);
                canvas.drawText("Online Game", mWidth/2, mHeight / 3 * 1.4f + 200, white);
                return;
            }

            d = mWidth / 7 < mHeight / 6 ? mWidth / 7 : mHeight / 6;
            xOffset = (mWidth - d * 7) / 2;
            yOffset = (mHeight - d * 6) / 2;

            canvas.drawRect(0, 0, mWidth, mHeight, blue);


            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < 7; j++) {
                    canvas.drawCircle(xOffset + d / 2 + (j * d), yOffset + d / 2 + (i * d), d / 2 - 8, chips[i][j] == 0 ? white : chips[i][j] < 0 ? Game.this.red : yellow);
                }
            }


            if ((winner = getWinner()) != 0) {
                if (mMultiplayer != null && mMultiplayer.inGame) canvas.drawText(winner == mMultiplayer.color ? "You win" : "You lose", mWidth/2, 100, white);
                else canvas.drawText(winner == -1 ? "Red wins" : "Yellow wins", mWidth/2, 100, white);
            }

            if (mMultiplayer != null && mMultiplayer.myTurn != null) {
                canvas.drawText(mMultiplayer.myTurn ? "My Turn" : "Opponent's turn", mWidth/2, mHeight-white.descent(), (mMultiplayer.myTurn && mMultiplayer.color == -1) || (!mMultiplayer.myTurn && mMultiplayer.color == 1) ? Game.this.red : yellow);
            } else {
                canvas.drawText(red ? "Red's Turn" : "Yellow's turn", mWidth/2, mHeight-white.descent(), red ? Game.this.red : yellow);

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
        public void onSurfaceTextureUpdated(SurfaceTexture s) {
        }
    }

    class Multiplayer extends Thread {

        private final Object lock = new Object();
        boolean inGame = false;
        Socket socket;
        Integer move;
        String playerId;
        String gameId;
        Boolean myTurn;
        Integer color;

        BufferedReader reader;
        BufferedWriter writer;

        @Override
        public void run() {
            try {
                socket = new Socket("4iar.lagunastudios.de", 9090);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                helloOrRegister();

                if (getIntent().getData() == null) {
                    create();
                    return;
                }
                gameId = getIntent().getData().getPath().split("/")[1];

                game();
                while (true) {
                    if (!myTurn) {
                        listen(reader.readLine());
                    } else if (move == null) {
                        sleep(500);
                    } else {
                        update();
                    }
                }


            } catch (Exception e) {
                Log.d("FLO", e.toString());
                throw new RuntimeException(e);
            }

        }

        private void update() throws IOException {
            synchronized (lock) {
                writer.write(String.format("update:%s:%s\n", gameId, move.toString()));
                writer.flush();
                myTurn = false;
                move = null;
            }
        }

        private void create() throws IOException {
                writer.write("create\n");
                writer.flush();
                String gameId = reader.readLine();

                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, String.format("https://4iar.lagunastudios.de/%s", gameId));
                sendIntent.setType("text/plain");
                Intent shareIntent = Intent.createChooser(sendIntent, null);
                startActivity(shareIntent);
        }

        private void helloOrRegister() throws IOException {
            playerId = getPreferences(0).getString("id", null);
            if (playerId == null) {
                register();
            } else {
                hello();
            }

        }

        private void register() throws IOException {
            writer.write("register\n");
            writer.flush();
            playerId = reader.readLine();
            getPreferences(0).edit().putString("id", playerId).apply();
            Log.d("FLO", String.format("Registered: %s", playerId));
        }

        private void hello() throws IOException {
            writer.write("hello:"+playerId+"\n");
            writer.flush();
        }

        private void game() throws IOException {
            writer.write("game:" + gameId + "\n");
            writer.flush();
            listen(reader.readLine());
        }

        private void listen(String msg) {
            Log.d("FLO", msg);
            String[] parts = msg.split(":");
            String[] board = parts[3].substring(1, parts[3].length()-1).split(",");
            int pos = 0;
            for (int y = 0; y < mGameLoop.chips.length; y++) {
                for (int x = 0; x < mGameLoop.chips[0].length; x++) {
                    mGameLoop.chips[y][x] = Byte.parseByte(board[pos++].strip());
                }
            }
            inGame = true;
            color = parts[0].equals(playerId) ? -1 : 1;
            myTurn = parts[Integer.parseInt(parts[2])].equals(playerId);
        }

        public void makeMove(int x) {
            if (myTurn == null || !myTurn) return;
            synchronized (lock) {
                move = x;
            }
        }
    }
}