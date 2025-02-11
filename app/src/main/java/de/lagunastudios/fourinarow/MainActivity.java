package de.lagunastudios.fourinarow;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;

public class MainActivity extends Activity {

    LinearLayout root;

    TextView tv;
    TextView tv2;
    Button button;

    ArrayList<Button> grid = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        tv = new TextView(this);
        tv2 = new TextView(this);
        button = new Button(this);

        for (int i = 0; i < 36; i++) {
            final Button b = new Button(this);
            b.setWidth(20);
            b.setBackgroundColor(0xff000000 + i * 11111);

            grid.add(b);
        }

        tv.setText("Hallo");
        tv2.setText("Duda2");

        button.setOnClickListener(v -> onClick());

        final GridLayout gridLayout = new GridLayout(this);

        gridLayout.setColumnCount(6);
        root.addView(tv);
        root.addView(tv2);
        root.addView(button);
        root.addView(gridLayout);

        for (Button b : grid) {
            gridLayout.addView(b);
        }

        setContentView(root);
    }

    public void onClick() {
        new Thread(this::networkCall).start();
    }

    public void networkCall() {
        Socket client = null;
        try {
            client = new Socket("lagunastudios.de", 9090);

            BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String result = reader.readLine();

            //OutputStreamWriter writer = new OutputStreamWriter(new BufferedOutputStream(client.getOutputStream()));
            //writer.write("Hallo from small app\n");
            //writer.flush();

            runOnUiThread(() -> tv2.setText(result));

            client.close();
        } catch (IOException e) {
            runOnUiThread(() -> tv.setText("Error"));
        }
    }


}