package com.cele.diplomskibasura.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.ReadInputDiscretesRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputDiscretesResponse;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.net.TCPConnectionHandler;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;


public class SentronActivity extends Activity {


    TextView valueL1;
    TextView valueL2;
    TextView valueL3;
    Button btnConnect;
    Boolean isConnectedToSlave = false;
    SharedPreferences sharedPreferences;
    TCPMasterConnection conn;
    Timer tm;

    ModbusTCPTransaction trans = null; //the transaction
    ReadMultipleRegistersRequest regRequest= null;
    ReadMultipleRegistersResponse regResponse = null;

    


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sentron);
       sharedPreferences = getApplicationContext().getSharedPreferences(Constants.MY_PREFS, 0); // 0 - for private mode



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
                }).start();


            }


        });


    }


@Override
        protected void onPause(){

        closeConnection();
   super.onPause();
}


    @Override
            protected void onStop(){
        closeConnection();
        super.onStop();

    }
   void connectToDevice(){

        final String sentronIP = sharedPreferences.getString(Postavke.SENTRON_IP, Constants.DEFUALT_PAC_IP);
        final int sentronPort = sharedPreferences.getInt(Postavke.SENTRON_PORT, Constants.DEFUALT_PAC_PORT);

            try {


                InetAddress address = InetAddress.getByName(sentronIP);
                conn = new TCPMasterConnection(address);
                conn.setPort(sentronPort);
               // conn.setTimeout(1000);
                Log.d("cele", "Connecting...");
                conn.connect();

                if(conn.isConnected()){
                    Log.d("cele", "Connected");
                    readSentronRegisters();



                    isConnectedToSlave = true;
                }

                 }catch (UnknownHostException e) {
                Log.d("cele", "No host");
                Log.d("cele", e.getMessage());

                 } catch (Exception e) {
                Log.d("cele", "failed to Connect");
                    Log.d("cele", " " + e.getMessage());
                }


        }

    void closeConnection(){
    new Thread(new Runnable() {
        @Override
        public void run() {


            if(conn.isConnected()){
                conn.close();
                Log.d("cele", "Connection closed to " + conn.getAddress());

            }else{
                Log.d("cele", "Not connected");
                return;

            }


        }
    }).start();


    }


    void readSentronRegisters(){



        regRequest = new ReadMultipleRegistersRequest(49, 1);

        trans = new ModbusTCPTransaction(conn);
        trans.setRequest(regRequest);

        try {
            trans.execute();
        } catch (ModbusException e) {
            e.printStackTrace();
        }
        regResponse = (ReadMultipleRegistersResponse) trans.getResponse();
        for(int i = 0; i < regResponse.getWordCount(); i++){

            Log.d("cele", "Value is " + i + " :  " + regResponse.getRegisterValue(i));
        }


    }

    }



