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
import com.ghgande.j2mod.modbus.ModbusIOException;
import com.ghgande.j2mod.modbus.ModbusSlaveException;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.ReadInputDiscretesRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputDiscretesResponse;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.net.TCPConnectionHandler;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import com.ghgande.j2mod.modbus.util.ModbusUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;


public class SentronActivity extends Activity {


    TextView valueL1;
    TextView valueL2;
    TextView valueL3;
    Button btnConnect;
    Boolean isConnectedToSlave = false;
    SharedPreferences sharedPreferences;
    TCPMasterConnection conn;
    Timer tm;
    TimerTask readRegs;

    Handler handler = new Handler();
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
protected void onResume(){
        super.onResume();
    new Thread(new Runnable() {
        @Override
        public void run() {

            connectToDevice();

        }
    }).start();


}


@Override
        protected void onPause(){
    Log.d("cele", "Pause disconnect.");
        closeConnection();
   super.onPause();
}


    @Override
            protected void onStop(){
        Log.d("cele", "Stop disconnect.");
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

                if(!conn.isConnected()){
                    Log.d("cele", "Connecting...");
                    conn.connect();

                }else{
                    Log.d("cele", "Already connected");
                }


                if(conn.isConnected()){
                    Log.d("cele", "Connected");

                    tm = new Timer();
                    readRegs = new TimerTask() {
                        @Override
                        public void run() {
                            readSentronRegisters();
                        }
                    };

                    tm.scheduleAtFixedRate( readRegs,  (long)100, (long)1000);



                    isConnectedToSlave = true;
                }

                 }catch (UnknownHostException e) {
                Log.d("cele", "No host");
                Log.d("cele", e.getMessage());

                 }
            catch (Exception e) {
                e.printStackTrace();
                Log.d("cele", "failed to Connect");
                    Log.d("cele", " l" + e.getLocalizedMessage());
                }


        }

    void closeConnection(){
    new Thread(new Runnable() {
        @Override
        public void run() {

            //tm.purge();

            if(conn != null && conn.isConnected() ){
                tm.cancel();
                readRegs.cancel();
                conn.close();
                Log.d("cele", "Connection closed to " + conn.getAddress());

            }else{
                Log.d("cele", "Not connected");
                return;

            }


        }
    }).start();


    }




    void readSentronRegisters() {


        regRequest = new ReadMultipleRegistersRequest(49, 3);

        trans = new ModbusTCPTransaction(conn);
        trans.setRequest(regRequest);

        try {
            trans.execute();
        }

        catch (ModbusIOException e) {
            Log.d("cele", "IO error");

            e.printStackTrace();
        } catch (ModbusSlaveException e) {

            Log.d("cele", "Slave returned exception");
            e.printStackTrace();
        } catch (ModbusException e) {
            Log.d("cele", "Failed to execute request");

            e.printStackTrace();
        }


        regResponse = (ReadMultipleRegistersResponse) trans.getResponse();
        for (int i = 0; i < regResponse.getWordCount(); i++) {

            Log.d("cele", "Value is " + i + " :  " + regResponse.getRegisterValue(i));
        }

        handler.post(new Runnable() {
            @Override
            public void run() {

                refreshGUI();
            }
        });

    }

    void refreshGUI(){


                if(regResponse!= null){
                    
                    valueL1.setText(regResponse.getRegisterValue(0) + " V");
                    valueL2.setText(regResponse.getRegisterValue(1) + " V");
                    valueL3.setText(regResponse.getRegisterValue(2) + " V");

                }else{

                    Log.d("cele", "reg emtpy");
                }

            }


        }








