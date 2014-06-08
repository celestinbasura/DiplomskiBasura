package com.cele.diplomskibasura.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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
import com.ghgande.j2mod.modbus.msg.WriteMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterResponse;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;


public class AcsActivity extends Activity implements SeekBar.OnSeekBarChangeListener {


    Button reverse;
    Button startStop;
    TextView currentSpeedReference;
    TextView currentActualSpeed;
    TextView currentActualCurrent;
    TextView currentActualPower;
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


    int controlWordAdr;
    int statusWordAdr;
    int speedRefInAdr;
    int speedRefOutAdr;
    int powerInAdr;
    int currentInAdr;
    int readSpeedRef = 0;
    int dataInOffset = 54;
    int dataOutOffset = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acs);

        sharedPreferences = getApplicationContext().getSharedPreferences(Constants.MY_PREFS, 0); // 0 - for private mode

        currentSpeedReference = (TextView) findViewById(R.id.txt_acs_reference_value);
        currentActualCurrent = (TextView) findViewById(R.id.txt_acs_current_current_value);
        currentActualSpeed = (TextView) findViewById(R.id.txt_acs_speed_current_value);
        currentActualPower = (TextView) findViewById(R.id.txt_acs_current_power_value);
        //currentActualPower.setText("2,23 kW");

        startStop = (Button) findViewById(R.id.btn_acs_start_stop);
        startStop.setBackgroundColor(Color.parseColor("#ff2280d7"));
        reverse = (Button) findViewById(R.id.btn_acs_reverziranje);

        controlWordAdr = sharedPreferences.getInt(Postavke.ACS_CNTR_WORD_NAME, Constants.DEFUALT_ACS_CNTR_WRD_ADR) + dataOutOffset;
        statusWordAdr = sharedPreferences.getInt(Postavke.ACS_STS_WORD_NAME, Constants.DEFUALT_ACS_STS_WRD_ADR) + dataInOffset;
        speedRefInAdr = sharedPreferences.getInt(Postavke.ACS_SPEED_REF_READ, Constants.DEFUALT_ACS_SPEED_REF_READ_ADR) + dataInOffset;
        speedRefOutAdr = sharedPreferences.getInt(Postavke.ACS_SPEED_REF_WRITE, Constants.DEFUALT_ACS_SPEED_REF_WRT_ADR) + dataOutOffset;
        powerInAdr = sharedPreferences.getInt(Postavke.ACS_POWER_READ, Constants.DEFUALT_ACS_POWER_READ_ADR) + dataInOffset;
        currentInAdr = sharedPreferences.getInt(Postavke.ACS_CURRENT_READ, Constants.DEFUALT_ACS_CURRENT_READ_ADR) + dataInOffset;

        speedReference = (SeekBar) findViewById(R.id.seek_acs_speed_reference);

        speedReference.setOnSeekBarChangeListener(this);
        speedReference.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                                      @Override
                                                      public void onProgressChanged(SeekBar seekBar, int i, boolean b) {


                                                          //Log.d("cele", "started progressracking");
                                                      }

                                                      @Override
                                                      public void onStartTrackingTouch(SeekBar seekBar) {

                                                          Log.d("cele", "started starttracking");
                                                      }

                                                      @Override
                                                      public void onStopTrackingTouch(final SeekBar seekBar) {



                                                          Log.d("cele", "started stoptracking");
                                                          final AlertDialog.Builder promjenaBrzine = new AlertDialog.Builder(AcsActivity.this);

                                                          promjenaBrzine.setTitle("Promjena brzine");
                                                          promjenaBrzine.setMessage("Da li ste sigurni da zelite brzinu postaviti na " + seekBar.getProgress() / 20 + "%");

                                                          promjenaBrzine.setPositiveButton("Da", new DialogInterface.OnClickListener() {
                                                              @Override
                                                              public void onClick(DialogInterface dialogInterface, int i) {

                                                                  Toast.makeText(getApplicationContext(), "Value is " + seekBar.getProgress(), Toast.LENGTH_SHORT).show();
                                                                  writeToACS(speedRefOutAdr, seekBar.getProgress());//TODO: vrijednost za zaustavljanje motora

                                                              }
                                                          });

                                                          promjenaBrzine.setNegativeButton("Odustani", new DialogInterface.OnClickListener() {
                                                              @Override
                                                              public void onClick(DialogInterface dialogInterface, int i) {

                                                              }
                                                          });
                                                            promjenaBrzine.show();

                                                          //TODO: Write correct value as speed reference
                                                      }
                                                  });

                startStop.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        Log.d("cele", "start pressed");

                        if (isReady) {
                            final AlertDialog.Builder stopMotor = new AlertDialog.Builder(AcsActivity.this);

                            if (isMotorRunning) {


                                stopMotor.setTitle("Zaustavi motor");
                                stopMotor.setMessage("Da li ste sigurni da zelite zaustaviti motor");

                                stopMotor.setPositiveButton("Da", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {

                                        setExactBit(false, 3, controlWordAdr);

                                     //  writeToACS(controlWordAdr, 5);//TODO: vrijednost za zaustavljanje motora

                                    }
                                });

                                stopMotor.setNegativeButton("Odustani", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {

                                    }
                                });
                                stopMotor.show();

                            } else {


                                stopMotor.setTitle("Pokreni motor");
                                stopMotor.setMessage("Da li ste sigurni da zelite pokrenuti motor");

                                stopMotor.setPositiveButton("Da", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {

                                        setExactBit(true, 3, controlWordAdr);

                                        //  writeToACS(controlWordAdr, 5);//TODO: vrijednost za zaustavljanje motora

                                    }
                                });

                                stopMotor.setNegativeButton("Odustani", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {

                                    }
                                });

                                stopMotor.show();
                                //setExactBit(true, 3, controlWordAdr);
                               // writeToACS(2, 5); //TODO: vrijednost za pokretanje motora
                            }


                        } else {

                            Toast.makeText(getApplicationContext(), "Pretvarac nije spreman", Toast.LENGTH_SHORT).show();
                        }



                    }
                });





        reverse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                Log.d("cele", "Rev pressed");

                if(!isMotorRunning){
                    Toast.makeText(getApplicationContext(), "Motor nije pokrenut", Toast.LENGTH_SHORT).show();
                    return;
                }else {
                    final AlertDialog.Builder promjenaSmjera = new AlertDialog.Builder(AcsActivity.this);
                    promjenaSmjera.setTitle("Reverziranje");
                    promjenaSmjera.setMessage("Da li ste sigurni da zelite reverzirati smjer vrtnje?");

                    promjenaSmjera.setPositiveButton("Da", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            writeToACS(readSpeedRef * (-1), speedRefOutAdr);

                            //  writeToACS(controlWordAdr, 5);//TODO: vrijednost za zaustavljanje motora

                        }
                    });

                    promjenaSmjera.setNegativeButton("Odustani", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });

                    promjenaSmjera.show();
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

        regRequest = new ReadMultipleRegistersRequest(0, 75);

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
            if (trans.getResponse() instanceof WriteSingleRegisterResponse) {

                Log.d("cele", " response is write");
            }
            regResponse = (ReadMultipleRegistersResponse) trans.getResponse();

        } catch (ClassCastException e) {
            trans.setRequest(regRequest);
            e.printStackTrace();
        }

                if(regResponse != null) {
                    for (int i = 50; i < regResponse.getWordCount(); i++) {

                        Log.d("cele", "Value is " + i + " :  " + regResponse.getRegisterValue(i));
                    }
                }

        handler.post(new Runnable() {
            @Override
            public void run() {

                refreshGUI();
            }
        });

    }


    void writeToACS(final int value, final int register) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                SimpleRegister[] sr = new SimpleRegister[2];
                sr[0] = new SimpleRegister(value);
                sr[1] = new SimpleRegister(value); //convert to two values
                Log.d("cele", "reg created");

                WriteMultipleRegistersRequest mulitpleRequest = new WriteMultipleRegistersRequest(register, sr);

                Log.d("cele", "request set");
                if (!(conn != null && conn.isConnected())) {

                    handler.post(new Runnable() {
                        @Override
                        public void run() {

                            Toast.makeText(getBaseContext(), "Not connected to server", Toast.LENGTH_SHORT).show();
                        }
                    });

                    return;

                }
                trans.setRequest(mulitpleRequest);
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
                readACSRegisters();
                }


        }
        ).start();

         isWriting = true;

    }


    void refreshGUI() {


        if (regResponse != null) {

            Log.d("cele", "Values refreshed");

            readSpeedRef = (int) twoIntsToACSTransparent(regResponse.getRegisterValue(speedRefInAdr), regResponse.getRegisterValue(speedRefInAdr + 1), 1);


            int tempSpeedRef;

            if(readSpeedRef >= 0){
                tempSpeedRef = readSpeedRef;
            }else{

                tempSpeedRef = readSpeedRef * (-1);
            }
            speedReference.setProgress(tempSpeedRef);

            currentActualCurrent.setText(twoIntsToACSTransparent(regResponse.getRegisterValue(currentInAdr), regResponse.getRegisterValue(currentInAdr + 1), 100) + " A"); // Scales to current (ACS 880 = 100)

            currentActualSpeed.setText(twoIntsToACSTransparent(regResponse.getRegisterValue(speedRefInAdr), regResponse.getRegisterValue(speedRefInAdr + 1), 100) + " 1/min");

            currentActualPower.setText(twoIntsToACSTransparent(regResponse.getRegisterValue(powerInAdr), regResponse.getRegisterValue(powerInAdr + 1), 100) + " kW");



            isReady = getBitState(1, statusWordAdr);
            isMotorRunning = getBitState(2, statusWordAdr);


            if(isReady){//TODO: Find real bit addresses to use

                if(isMotorRunning){

                    startStop.setText("START");
                    startStop.setBackgroundColor(Color.RED);
                }else{

                    startStop.setText("STOP");
                    startStop.setBackgroundColor(Color.GREEN);

                }

            }else{
                startStop.setText("Nije spremno");
                startStop.setBackgroundColor(Color.parseColor("#ff2280d7"));

            }
            // TODO: Gui refreshing,
            // TODO: getting bits for READY and start/stop,
            // TODO: getting values to write to ACS


        } else {

            Log.d("cele", "reg emtpy");
        }

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


    public static float twoIntsToACSTransparent(int reg1, int reg2, int scaleValue) {

        int numberHelper;


        byte[] b1 = ByteBuffer.allocate(4).putInt(reg1).array();
        byte[] b2 = ByteBuffer.allocate(4).putInt(reg2).array();


        byte[] b32bit = {b1[2], b1[3], b2[2], b2[3]};



        numberHelper = ByteBuffer.wrap(b32bit).getInt();



       String helper = Integer.toBinaryString(  numberHelper  );

       return parseUnsignedInt(helper, 2) / scaleValue;



    }

    public void setExactBit(boolean state, int bitOffset, int regAddress){

        String binary = Integer.toBinaryString(regResponse.getRegisterValue(regAddress));

        char[] binaryArray = binary.toCharArray();

        if(state){
            binaryArray[bitOffset] = '1';

        }else{

            binaryArray[bitOffset] = '0';
        }

        String editedBinary = binaryArray.toString();

        int temp = Integer.parseInt(editedBinary);

        writeToACS(temp, regAddress + 0);


    }


    public boolean getBitState(int offset, int regAddress){

        String binary = Integer.toBinaryString(regResponse.getRegisterValue(regAddress));

        char[] binaryArray = binary.toCharArray();

        if(binaryArray[offset] == 0){
            return false;
        }else {

            return true;
        }


    }


    public static int parseUnsignedInt(String s, int radix)
            throws NumberFormatException {
        if (s == null)  {
            throw new NumberFormatException("null");
        }

        int len = s.length();
        if (len > 0) {
            char firstChar = s.charAt(0);
            if (firstChar == '-') {
                throw new
                        NumberFormatException(String.format("Illegal leading minus sign " +
                        "on unsigned string %s.", s));
            } else {
                if (len <= 5 || // Integer.MAX_VALUE in Character.MAX_RADIX is 6 digits
                        (radix == 10 && len <= 9) ) { // Integer.MAX_VALUE in base 10 is 10 digits
                    return Integer.parseInt(s, radix);
                } else {
                    long ell = Long.parseLong(s, radix);
                    if ((ell & 0xffffffff00000000L) == 0) {
                        return (int) ell;
                    } else {
                        throw new
                                NumberFormatException(String.format("String value %s exceeds " +
                                "range of unsigned int.", s));
                    }
                }
            }
        } else {
            throw new NumberFormatException(" String is wrong");
        }
    }


}
