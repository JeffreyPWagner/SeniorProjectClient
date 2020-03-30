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

/**
 * The main activity that contains the client app's functionality
 */
public class MainActivity extends AppCompatActivity {

    // The connect button on the UI
    Button connectButton;

    // The disconnect button on the UI
    Button disconnectButton;

    // The calculation counter on the UI
    private TextView counter;

    // The count of calculations completed
    private static Integer count;

    // Apache Commons Math object for calculating standard deviation
    private static StandardDeviation standardDeviation;

    // Apache Commons Math object for calculating mean
    private static Mean mean;

    // Is the app connected and ready to calculate
    private static boolean running;

    /**
     * Lifecycle method that represents the launch of the app
     * @param savedInstanceState the last saved app state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Load state and create UI
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set the app to running and instantiate class variables
        running = true;
        count = 0;
        mean = new Mean();
        standardDeviation = new StandardDeviation();

        // Create connect button and set listener
        connectButton = findViewById(R.id.connectButton);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectToServer();
            }
        });

        // Create disconnect button and set listener
        disconnectButton = findViewById(R.id.disconnect);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disconnect();
            }
        });

        // Create counter and set current count
        counter = findViewById(R.id.counter);
        counter.setText(count.toString());

    }

    /**
     * Set the app to run and execute a new server task
     */
    private void connectToServer () {
        running = true;
        serverTask task = new serverTask();
        task.execute();
    }

    /**
     * Inner class that allows the app to perform gene calculations in the background
     */
    private class serverTask extends AsyncTask<Void, Void, Void> {

        /**
         * Connect to the central server and process the genes it sends
         */
        @Override
        protected Void doInBackground(Void... voids) {
            try {

                // The IP of the central server, hardcoded to my machine in this case
                String ip = "192.168.0.109";

                // The TCP socket connecting to the server
                Socket socket = new Socket(ip, 5000);

                // A stream to take in the gene data
                ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());

                // A stream to output the result
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());


                while(running) {

                    // Get the gene data from the server
                    double[] gene = (double[]) objectInputStream.readObject();

                    // Calculate the t-stat of the original data
                    double tStat = getTStat(gene);
                    double[] permutationTStats = new double[10000];

                    // Shuffle the data and record the t-stat of the newly ordered data x times
                    for(int i=0; i<10000; i++) {
                        shuffleArray(gene);
                        permutationTStats[i] = getTStat(gene);
                    }

                    // Calculate the d-score of the gene
                    double permutationMean = mean.evaluate(permutationTStats);
                    double permutationStandardDeviation = standardDeviation.evaluate(permutationTStats);
                    double dScore = Math.abs(tStat - permutationMean) / permutationStandardDeviation;

                    // Send the d-score back to the central server
                    dataOutputStream.writeDouble(dScore);
                    dataOutputStream.flush();

                    // Increase the calculation count on the UI
                    count++;
                    counter.setText(count.toString());
                }

                // Close the streams and socket when done running
                objectInputStream.close();
                dataOutputStream.close();
                socket.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        /**
         * Helper method to calculate the t-stat of the given gene
         * @param gene the gene to calculate the t-stat for
         * @return the gene's t-stat
         */
        private double getTStat(double[] gene) {

            // Record statistics of the first 8 patients, who have renal cancer in the non-shuffled data
            double[] first = Arrays.copyOfRange(gene, 0, 8);
            double firstMean = mean.evaluate(first);
            double firstStandardDeviation = standardDeviation.evaluate(first);

            // Record statistics of the last 52 patients, who do not have renal cancer in the non-shuffled data
            double[] last = Arrays.copyOfRange(gene, 8, gene.length);
            double lastMean = mean.evaluate(last);
            double lastStandardDeviation = standardDeviation.evaluate(last);

            // Calculate and return the t-stat
            return (firstMean - lastMean) / Math.sqrt((Math.pow(firstStandardDeviation, 2) / 8) + Math.pow(lastStandardDeviation, 2) / 52);
        }

        /**
         * Helper method to shuffle the gene data.
         * @param array the array to shuffle
         */
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

    /**
     * Cease calculations and disconnect from the central server
     */
    private void disconnect() {
        running = false;
    }
}
