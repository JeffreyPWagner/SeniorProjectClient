package com.example.seniorproject;

import android.os.AsyncTask;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;

import android.widget.Button;
import android.widget.TextView;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import java.io.DataOutputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    Button connectButton;
    Button disconnectButton;
    private TextView counter;
    private static Integer count;

    private static Socket socket;
    private static DataOutputStream dataOutputStream;
    private static ObjectInputStream objectInputStream;
    private static StandardDeviation standardDeviation;
    private static Mean mean;
    private static String ip = "192.168.0.109";
    private static boolean running;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        running = true;
        count = 0;
        mean = new Mean();
        standardDeviation = new StandardDeviation();

        connectButton = findViewById(R.id.connectButton);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectToServer();
            }
        });

        disconnectButton = findViewById(R.id.disconnect);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disconnect();
            }
        });

        counter = findViewById(R.id.counter);
        counter.setText(count.toString());

    }

    private void connectToServer () {
        running = true;
        serverTask task = new serverTask();
        task.execute();
    }

    private class serverTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                socket = new Socket(ip, 5000);
                objectInputStream = new ObjectInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());

                while(running) {
                    double[] gene = (double[]) objectInputStream.readObject();
                    double tStat = getTStat(gene);
                    double[] permutationTStats = new double[1000];

                    for(int i=0; i<1000; i++) {
                        shuffleArray(gene);
                        permutationTStats[i] = getTStat(gene);
                    }

                    double permutationMean = mean.evaluate(permutationTStats);
                    double permutationStandardDeviation = standardDeviation.evaluate(permutationTStats);

                    double dScore = Math.abs(tStat - permutationMean) / permutationStandardDeviation;

                    dataOutputStream.writeDouble(dScore);
                    dataOutputStream.flush();

                    count++;
                    counter.setText(count.toString());
                }

                objectInputStream.close();
                dataOutputStream.close();
                socket.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        private double getTStat(double[] gene) {
            double[] first = Arrays.copyOfRange(gene, 0, 8);
            double firstMean = mean.evaluate(first);
            double firstStandardDeviation = standardDeviation.evaluate(first);

            double[] last = Arrays.copyOfRange(gene, 8, gene.length);
            double lastMean = mean.evaluate(last);
            double lastStandardDeviation = standardDeviation.evaluate(last);

            return (firstMean - lastMean) / Math.sqrt((Math.pow(firstStandardDeviation, 2) / 8) + Math.pow(lastStandardDeviation, 2) / 52);
        }

        // helper method to shuffle permutations
        private void shuffleArray(double[] array) {
            int index;
            double temp;
            Random random = new Random();
            for (int i = array.length - 1; i > 0; i--) {
                index = random.nextInt(i + 1);
                temp = array[index];
                array[index] = array[i];
                array[i] = temp;
            }
        }
    }

    private void disconnect() {
        running = false;
    }
}
