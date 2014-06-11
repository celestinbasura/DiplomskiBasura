package com.cele.diplomskibasura.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.ModbusIOException;
import com.ghgande.j2mod.modbus.ModbusSlaveException;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.WriteMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;


public class SentronActivity extends Activity {


    TextView valueL1;
    TextView valueL2;
    TextView valueL3;
    TextView valueI1;
    TextView valueI2;
    TextView valueI3;
    TextView avgVoltage;
    TextView frequency;


    Boolean isConnectedToSlave = false;
    SharedPreferences sharedPreferences;
    TCPMasterConnection conn;
    Timer tm;
    TimerTask readRegs;
    Handler hanler = new Handler();
    Handler handler = new Handler();
    volatile ModbusTCPTransaction trans = null; //the transaction
    ReadMultipleRegistersRequest regRequest = null;
    volatile ReadMultipleRegistersResponse regResponse = null;


    Boolean isWriting = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sentron);
        sharedPreferences = getApplicationContext().getSharedPreferences(Constants.MY_PREFS, 0); // 0 - for private mode
        valueL1 = (TextView) findViewById(R.id.txt_sentron_l1);
        valueL2 = (TextView) findViewById(R.id.txt_sentron_l2);
        valueL3 = (TextView) findViewById(R.id.txt_sentron_l3);
        frequency = (TextView) findViewById(R.id.txt_sentron_freq);
        valueI1 = (TextView) findViewById(R.id.txt_sentron_i1);
        valueI2 = (TextView) findViewById(R.id.txt_sentron_i2);
        valueI3 = (TextView) findViewById(R.id.txt_sentron_i3);
        avgVoltage = (TextView) findViewById(R.id.txt_sentron_avg_v_n);


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

    void connectToDevice() {

        final String sentronIP = sharedPreferences.getString(Postavke.SENTRON_IP, Constants.DEFUALT_PAC_IP);
        final int sentronPort = sharedPreferences.getInt(Postavke.SENTRON_PORT, Constants.DEFUALT_PAC_PORT);

        try {


            InetAddress address = InetAddress.getByName(sentronIP);
            conn = new TCPMasterConnection(address);
            conn.setPort(sentronPort);
            // conn.setTimeout(1000);

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
                        readSentronRegisters();
                    }
                };

                tm.scheduleAtFixedRate(readRegs, (long) 100, (long) 500);


                isConnectedToSlave = true;
            }

        } catch (UnknownHostException e) {
            Log.d("cele", "No host");
            Log.d("cele", e.getMessage());

        } catch (Exception e) {
            e.printStackTrace();
            Log.d("cele", "failed to Connect");
            Log.d("cele", " l" + e.getLocalizedMessage());
        }


    }

    void closeConnection() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                //tm.purge();

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


    void writeToSentronTest() {

        new Thread(new Runnable() {
            @Override
            public void run() {


                SimpleRegister[] sr = new SimpleRegister[1];
                sr[0] = new SimpleRegister((int) (Math.random() * 1000));
                Log.d("cele", "reg created");

                WriteMultipleRegistersRequest singleRequest = new WriteMultipleRegistersRequest(69, sr);
                // WriteMultipleRegisterRequest singleRequest = new WriteSingleRegisterRequest(8, sr);
                // WriteMultipleRegisterResponse singleResponse = null;
                WriteMultipleRegistersResponse singleResponse = null;


                //WriteMultipleRegistersRequest writeRequest = new WriteMultipleRegistersRequest(sr);
                Log.d("cele", "request set");
                if (!(conn != null && conn.isConnected())) {


                    hanler.post(new Runnable() {
                        @Override
                        public void run() {

                            Toast.makeText(getBaseContext(), "Not connected to server", Toast.LENGTH_SHORT).show();
                        }
                    });

                    return;


                }
                trans.setRequest(singleRequest);
                try {

                    trans.execute();
                    trans.getResponse();
                    Log.d("cele", "executed");
                } catch (ModbusException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {

                    e.printStackTrace();
                }
                isWriting = false;
                readSentronRegisters();

            }


        }
        ).start();


        isWriting = true;

    }

    void readSentronRegisters() {

        if (isWriting) {
            return;
        }

        regRequest = new ReadMultipleRegistersRequest(1, 75);

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
            valueL1.setText((String.format("%.3f V",
                    twoIntsToFloat(
                            regResponse.getRegisterValue(0),
                            regResponse.getRegisterValue(1))
            )));


            valueL2.setText((String.format("%.3f V",
                    twoIntsToFloat(
                            regResponse.getRegisterValue(2),
                            regResponse.getRegisterValue(3))
            )));

            valueL3.setText((String.format("%.3f V",
                    twoIntsToFloat(
                            regResponse.getRegisterValue(4),
                            regResponse.getRegisterValue(5))
            )));

            frequency.setText((String.format("%.3f Hz",
                    // regResponse.getRegisterValue(68))));

                    twoIntsToFloat(
                            regResponse.getRegisterValue(54),
                            regResponse.getRegisterValue(55))
            )));


            valueI1.setText((String.format("%.3f A",
                    twoIntsToFloat(
                            regResponse.getRegisterValue(12),
                            regResponse.getRegisterValue(13))
            )));

            valueI2.setText((String.format("%.3f A",
                    twoIntsToFloat(
                            regResponse.getRegisterValue(14),
                            regResponse.getRegisterValue(15))
            )));

            valueI3.setText((String.format("%.3f A",
                    twoIntsToFloat(
                            regResponse.getRegisterValue(16),
                            regResponse.getRegisterValue(17))
            )));

            avgVoltage.setText((String.format("%.3f V",
                    twoIntsToFloat(
                            regResponse.getRegisterValue(56),
                            regResponse.getRegisterValue(57))
            )));

        } else {

            Log.d("cele", "reg emtpy");
        }

    }


    public static float twoIntsToFloat(int reg1, int reg2) {

        byte[] b1 = ByteBuffer.allocate(4).putInt(reg1).array();
        byte[] b2 = ByteBuffer.allocate(4).putInt(reg2).array();

        byte[] b32bit = {b1[2], b1[3], b2[2], b2[3]};

        return ByteBuffer.wrap(b32bit).getFloat();
    }


}








