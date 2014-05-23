package com.cele.diplomskibasura.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ghgande.j2mod.modbus.net.TCPConnectionHandler;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;

import java.net.InetAddress;
import java.net.UnknownHostException;


public class SentronActivity extends Activity {


    TextView valueL1;
    TextView valueL2;
    TextView valueL3;
    Button btnConnect;
    Boolean isConnectedToSlave = false;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sentron);
        final SharedPreferences sharedPreferences1= getApplicationContext().getSharedPreferences(Constants.MY_PREFS, 0); // 0 - for private mode


        valueL1 = (TextView) findViewById(R.id.txt_sentron_l1);
        valueL2 = (TextView) findViewById(R.id.txt_sentron_l2);
        valueL3 = (TextView) findViewById(R.id.txt_sentron_l3);
        btnConnect = (Button) findViewById(R.id.btn_sentron_connect);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Toast.makeText(getApplicationContext(), "cliked", Toast.LENGTH_SHORT).show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        connectToDevice();
                    }
                });


            }


        });


    }





   void connectToDevice(){

        final String sentronIP = "192.168.0.1";//sharedPreferences.getString(Postavke.SENTRON_IP, Constants.DEFUALT_PAC_IP);
        final int sentronPort = 502;//sharedPreferences.getInt(Postavke.SENTRON_PORT, Constants.DEFUALT_PAC_PORT);

            try {


                InetAddress address = InetAddress.getByName(sentronIP);
                TCPMasterConnection conn = new TCPMasterConnection(address);
                conn.setPort(sentronPort);
                conn.setTimeout(1000);
                Log.d("cele", "Connecting...");
                conn.connect();

                if(conn.isConnected()){
                    Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                    isConnectedToSlave = true;
                }

                 }catch (UnknownHostException e) {
                Log.d("cele", "No host");

                 } catch (Exception e) {
                Log.d("cele", "failed to coinn");
                    e.printStackTrace();
                }


        }


    }



