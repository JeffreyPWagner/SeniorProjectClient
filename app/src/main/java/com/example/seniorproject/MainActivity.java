package com.example.seniorproject;

import android.os.AsyncTask;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button connectButton;

    private static Socket socket;
    private static DataOutputStream dataOutputStream;
    private static ObjectInputStream objectInputStream;
    private static StandardDeviation standardDeviation;
    private static String ip = "192.168.0.109";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        standardDeviation = new StandardDeviation();
        connectButton = findViewById(R.id.connectButton);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectToServer();
            }
        });
    }

    public void connectToServer () {
        serverTask task = new serverTask();
        task.execute();
    }

    class serverTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                socket = new Socket(ip, 5000);
                objectInputStream = new ObjectInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());

                while(true) {
                    List<Double> gene = (List<Double>) objectInputStream.readObject();
                    Double tStat = getTStat(gene);
                    List<Double> permutationTStats = new ArrayList<>();

                    for(int i=0; i<100; i++) {
                        Collections.shuffle(gene);
                        permutationTStats.add(getTStat(gene));
                    }

                    double[] permutationTStatsArray = permutationTStats.toArray(new double[0]);
                    Double permutationStandardDeviation = standardDeviation.evaluate(permutationTStats);
                }




            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    private Double getTStat(List<Double> gene) {
        return 0.09;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
