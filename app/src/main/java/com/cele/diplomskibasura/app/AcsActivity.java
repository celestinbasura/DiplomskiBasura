package com.cele.diplomskibasura.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.ModbusIOException;
import com.ghgande.j2mod.modbus.ModbusSlaveException;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;


public class AcsActivity extends Activity implements SeekBar.OnSeekBarChangeListener{


    Button reverse;
    Button startStop;
    TextView currentSpeedReference;
    TextView currentActualSpeed;
    TextView currentActualCurrent;
    SeekBar speedReference;


    boolean isReady = false;
    boolean isMotorRunning = false;
    boolean isWriting = false;

    Boolean isConnectedToSlave = false;
    SharedPreferences sharedPreferences;
    TCPMasterConnection conn;

    Timer tm;
    TimerTask readRegs;
    Handler handler = new Handler();
    volatile ModbusTCPTransaction trans = null; //the transaction
    ReadMultipleRegistersRequest regRequest = null;
    volatile ReadMultipleRegistersResponse regResponse = null;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acs);

        sharedPreferences = getApplicationContext().getSharedPreferences(Constants.MY_PREFS, 0); // 0 - for private mode

        currentSpeedReference = (TextView) findViewById(R.id.txt_acs_reference_value);
        currentActualCurrent = (TextView) findViewById(R.id.txt_acs_current_current_value);
        currentActualSpeed = (TextView) findViewById(R.id.txt_acs_speed_current_text);

        startStop = (Button) findViewById(R.id.btn_acs_start_stop);
        reverse = (Button) findViewById(R.id.btn_acs_reverziranje);

        speedReference = (SeekBar) findViewById(R.id.seek_acs_speed_reference);

        startStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                speedReference.setProgress(1000);
                if(isReady){

                    if(isMotorRunning){

                        writeToACS(5, 5);

                    }else{

                        writeToACS(2, 5);
                    }


                }else{

                    Toast.makeText(getApplicationContext(), "Pretvarac nije spreman", Toast.LENGTH_SHORT);
                }

            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        new Thread(new Runnable() {
            @Override
            public void run() {

                connectToDevice();

            }
        }).start();


    }


    @Override
    protected void onPause() {
        Log.d("cele", "Pause disconnect.");
        closeConnection();
        super.onPause();
    }


    @Override
    protected void onStop() {
        Log.d("cele", "Stop disconnect.");
        closeConnection();
        super.onStop();

    }






    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }


    void writeToACS(int value, int address){






    }


    void connectToDevice() {

        final String AcsIP = sharedPreferences.getString(Postavke.ACS_IP, Constants.DEFUALT_ACS_IP);
        final int AcsPort = sharedPreferences.getInt(Postavke.ACS_PORT, Constants.DEFUALT_ACS_PORT);


        try {

            InetAddress address = InetAddress.getByName(AcsIP);
            conn = new TCPMasterConnection(address);
            conn.setPort(AcsPort);

            if (!conn.isConnected()) {
                Log.d("cele", "Connecting...");
                conn.connect();

            } else {
                Log.d("cele", "Already connected");
            }


            if (conn.isConnected()) {
                Log.d("cele", "Connected");

                tm = new Timer();
                readRegs = new TimerTask() {
                    @Override
                    public void run() {
                        readACSRegisters();
                    }
                };

                tm.scheduleAtFixedRate(readRegs, (long) 500, (long) 1000);
                isConnectedToSlave = true;
            }

        } catch (UnknownHostException e) {
            Log.d("cele", "No host");
            Log.d("cele", e.getMessage());

        } catch (Exception e) {
            e.printStackTrace();
            Log.d("cele", "failed to Connect");
            Log.d("cele", " " + e.getLocalizedMessage());
        }


    }

    void closeConnection() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                if (conn != null && conn.isConnected()) {
                    tm.cancel();
                    readRegs.cancel();
                    conn.close();
                    Log.d("cele", "Connection closed to " + conn.getAddress());

                } else {
                    Log.d("cele", "Not connected");
                    return;

                }
            }
        }).start();

    }

    void readACSRegisters() {

        if (isWriting) {
            return;
        }

        regRequest = new ReadMultipleRegistersRequest(0, 20);

        trans = new ModbusTCPTransaction(conn);
        trans.setRequest(regRequest);

        try {

            trans.execute();

        } catch (ModbusIOException e) {
            Log.d("cele", "IO error");

            e.printStackTrace();
        } catch (ModbusSlaveException e) {

            Log.d("cele", "Slave returned exception");
            e.printStackTrace();
        } catch (ModbusException e) {
            Log.d("cele", "Failed to execute request");

            e.printStackTrace();
        } catch (NullPointerException e) {

            e.printStackTrace();
        }

        try {
            if (trans.getResponse() instanceof WriteMultipleRegistersResponse) {

                Log.d("cele", " response is write");
            }
            regResponse = (ReadMultipleRegistersResponse) trans.getResponse();

        } catch (ClassCastException e) {
            trans.setRequest(regRequest);
            e.printStackTrace();
        }


        //     for (int i = 0; i < regResponse.getWordCount(); i++) {
//
        //          Log.d("cele", "Value is " + i + " :  " + regResponse.getRegisterValue(i));
        //    }


        handler.post(new Runnable() {
            @Override
            public void run() {

                refreshGUI();
            }
        });

    }

    void refreshGUI() {


        if (regResponse != null) {

            Log.d("cele", "Values refreshed");

            //TODO: Gui refreshing


        } else {

            Log.d("cele", "reg emtpy");
        }

    }


}
