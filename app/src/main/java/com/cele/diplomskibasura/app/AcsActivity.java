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
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.ModbusIOException;
import com.ghgande.j2mod.modbus.ModbusSlaveException;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterRequest;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterResponse;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

import static com.cele.diplomskibasura.app.Utils.*;


public class AcsActivity extends Activity implements SeekBar.OnSeekBarChangeListener {


    Button reverse;
    Button startStop;
    TextView currentSpeedReference;
    TextView currentActualSpeed;
    TextView currentActualCurrent;
    TextView currentActualPower;
    TextView currentFaultCode;
    TextView currentWarningCode;
    SeekBar speedReference;
    ImageButton btnWarning;
    ImageButton btnFault;


    boolean isMotorRunning = false;
    boolean isWriting = false;
    boolean isFirstCommNeeded = false;
    boolean isLocalActive = false;

    boolean isReadyToSwitchOn = false;
    boolean isReadyToRun = false;
    boolean isReadyRef;
    boolean isFaulted;
    boolean isOffTwoInactive;
    boolean isOffThreeInactive;
    boolean isSwitchOnInhibited;
    boolean isWarningActive;
    boolean isAtSetpoint;
    boolean isRemoteActive;
    boolean isAboveLimit;
    boolean isExtRunEnabled;

    boolean isConnectedToSlave = false;
    SharedPreferences sharedPreferences;
    TCPMasterConnection conn;

    Timer tm;
    TimerTask readRegs;
    Handler handler = new Handler();
    volatile ModbusTCPTransaction trans = null; //the transaction
    ReadMultipleRegistersRequest regRequest = null;
    volatile ReadMultipleRegistersResponse regResponse = null;

    final int controlWordAdr = 0;
    final int statusWordAdr = 50;
    final int speedRefInAdr = 51;
    final int speedRefOutAdr = 1;
    final int dataInOffset = 52;
    int powerInAdr;
    int currentInAdr;
    int speedEstInAdr;
    int readSpeedRef = 0;
    float currentSpeed;
    int starStopWriteValue = 1150;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acs);

        sharedPreferences = getApplicationContext().getSharedPreferences(Constants.MY_PREFS, 0); // 0 - for private mode

        currentSpeedReference = (TextView) findViewById(R.id.txt_acs_reference_value);
        currentActualCurrent = (TextView) findViewById(R.id.txt_acs_current_current_value);
        currentActualSpeed = (TextView) findViewById(R.id.txt_acs_speed_current_value);
        currentActualPower = (TextView) findViewById(R.id.txt_acs_current_power_value);
        currentFaultCode = (TextView) findViewById(R.id.txt_acs_current_fault);
        currentWarningCode = (TextView) findViewById(R.id.txt_acs_current_warning);

        btnFault = (ImageButton) findViewById(R.id.btn_acs_fault);
        btnWarning = (ImageButton) findViewById(R.id.btn_acs_warning);

        btnFault.setVisibility(View.INVISIBLE);
        btnWarning.setVisibility(View.INVISIBLE);

        currentFaultCode.setText(" ");
        currentWarningCode.setText(" ");

        startStop = (Button) findViewById(R.id.btn_acs_start_stop);
        startStop.setBackgroundColor(Color.parseColor("#ff2280d7"));
        reverse = (Button) findViewById(R.id.btn_acs_reverziranje);

        powerInAdr = sharedPreferences.getInt(Postavke.ACS_POWER_READ, Constants.DEFUALT_ACS_POWER_READ_ADR) + dataInOffset;
        currentInAdr = sharedPreferences.getInt(Postavke.ACS_CURRENT_READ, Constants.DEFUALT_ACS_CURRENT_READ_ADR) + dataInOffset;

        speedEstInAdr = sharedPreferences.getInt(Postavke.ACS_SPEED_EST_READ, Constants.DEFUALT_ACS_SPEED_EST_READ_ADR);
        speedReference = (SeekBar) findViewById(R.id.seek_acs_speed_reference);

        speedReference.setOnSeekBarChangeListener(this);
        speedReference.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                                      @Override
                                                      public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                                                      }

                                                      @Override
                                                      public void onStartTrackingTouch(SeekBar seekBar) {
                                                      }

                                                      @Override
                                                      public void onStopTrackingTouch(final SeekBar seekBar) {

                                                          final int temp = seekBar.getProgress();

                                                          final AlertDialog.Builder promjenaBrzine = new AlertDialog.Builder(AcsActivity.this);

                                                          promjenaBrzine.setTitle("Promjena brzine");
                                                          promjenaBrzine.setMessage("Postaviti brzinu na " + temp / 200 + "%");

                                                          promjenaBrzine.setPositiveButton("Potvrda", new DialogInterface.OnClickListener() {
                                                              @Override
                                                              public void onClick(DialogInterface dialogInterface, int i) {

                                                                  Toast.makeText(getApplicationContext(), "Value is " + temp, Toast.LENGTH_SHORT).show();
                                                                  writeToACS(temp, speedRefOutAdr );

                                                              }
                                                          });

                                                          promjenaBrzine.setNegativeButton("Odustani", new DialogInterface.OnClickListener() {
                                                              @Override
                                                              public void onClick(DialogInterface dialogInterface, int i) {

                                                              }
                                                          });
                                                            promjenaBrzine.show();
                                                      }
                                                  });

                startStop.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        Log.d("cele", "start pressed");

                        final AlertDialog.Builder stopMotor = new AlertDialog.Builder(AcsActivity.this);

                        stopMotor.setTitle(startStop.getText());
                        stopMotor.setMessage("Da li ste sigurni?");

                        stopMotor.setPositiveButton("Potvrdi", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                writeToACS(starStopWriteValue, controlWordAdr);

                            }
                        });

                        stopMotor.setNegativeButton("Odustani", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        });
                        stopMotor.show();
                    }
                });


        reverse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

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

                            writeToACS(acsTransparentToInt((readSpeedRef * (-1))), speedRefOutAdr);

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

                tm.scheduleAtFixedRate(readRegs, (long) 500, (long) 500);
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

            /*    if(regResponse != null) {
                    for (int i = 50; i < regResponse.getWordCount(); i++) {

                        Log.d("cele", "Value is " + i + " :  " + regResponse.getRegisterValue(i));
                    }
                }
*/
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

                SimpleRegister sr;
                sr = new SimpleRegister(value);

                Log.d("cele", "reg created");

                WriteSingleRegisterRequest mulitpleRequest = new WriteSingleRegisterRequest(register, sr);

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
              //  isWriting = true;
                trans.setRequest(mulitpleRequest);
                try {
                    Log.d("cele", "Writing " + sr + " to " + register);
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



    }


    void refreshGUI() {

        if (regResponse != null) {

            //Log.d("cele", "Values refreshed");
            readSpeedRef = oneIntToTransparent(regResponse.getRegisterValue(speedRefInAdr));
            int tempSpeedRef;

            if (readSpeedRef >= 0) {
                tempSpeedRef = readSpeedRef;
            } else {
                tempSpeedRef = readSpeedRef * (-1);
            }

            speedReference.setProgress(tempSpeedRef);
            currentActualCurrent.setText(
                    twoIntsToACSTransparent(
                            regResponse.getRegisterValue(currentInAdr),
                            regResponse.getRegisterValue(currentInAdr + 1),
                            100) + " A");

            currentSpeedReference.setText(
                    oneIntToTransparent(
                            regResponse.getRegisterValue(speedRefInAdr)) + " ");

            currentSpeed = twoIntsToACSTransparent(
                    regResponse.getRegisterValue(speedEstInAdr + dataInOffset),
                    regResponse.getRegisterValue(speedEstInAdr + 1 + dataInOffset), 100);

            currentActualSpeed.setText(currentSpeed + " 1/min");

            currentActualPower.setText(
                    twoIntsToACSTransparent(
                            regResponse.getRegisterValue(powerInAdr),
                            regResponse.getRegisterValue(powerInAdr + 1),
                            100) + " kW");

            int statusWord = regResponse.getRegisterValue(statusWordAdr);

            isReadyToSwitchOn = getBitState(0, statusWord);
            Log.d("cele", " isReadyToSwitch on " + isReadyToSwitchOn);

            isReadyToRun = getBitState(1, statusWord);
            Log.d("cele", " isReadyToRun" + isReadyToRun);

            isReadyRef = getBitState(2, statusWord);
            Log.d("cele", " isReadyRef " + isReadyRef);

            isFaulted = getBitState(3, statusWord);
            Log.d("cele", " isFaulted " + isFaulted);

            isOffTwoInactive = getBitState(4, statusWord);
            Log.d("cele", " isOff 2 inactive " + isOffTwoInactive);

            isOffThreeInactive = getBitState(5, statusWord);
            Log.d("cele", " iisOff 3 inactive " + isOffThreeInactive);

            isSwitchOnInhibited = getBitState(6, statusWord);
            Log.d("cele", " isSwitchOn Inhibited " + isSwitchOnInhibited);

            isWarningActive = getBitState(7, statusWord);
            Log.d("cele", " isWarning active " + isWarningActive);

            isAtSetpoint = getBitState(8, statusWord);
            Log.d("cele", " isAt setpoint " + isAtSetpoint);

            isRemoteActive = getBitState(9, statusWord);
            Log.d("cele", " isRemote active" + isRemoteActive);

            isAboveLimit = getBitState(10, statusWord);
            Log.d("cele", " isAbove limit " + isAboveLimit);

            isExtRunEnabled = getBitState(12, statusWord);
            Log.d("cele", " Remote run " + isExtRunEnabled);


            if(isFaulted){
                btnFault.setVisibility(View.VISIBLE);
            }else{
               btnFault.setVisibility(View.INVISIBLE);
            }

            if(isWarningActive){
                btnWarning.setVisibility(View.VISIBLE);
            }else{
                btnWarning.setVisibility(View.INVISIBLE);
            }

            switch (statusWord) {


                case 754:

                    isMotorRunning = false;
                    isFirstCommNeeded = true;
                    isLocalActive = false;
                    startStop.setText("PRIPREMA");
                    startStop.setBackgroundColor(Color.DKGRAY);
                    startStop.setClickable(true);
                    starStopWriteValue = 1150;
                    break;

                case 695:

                    isMotorRunning = true;
                    isFirstCommNeeded = false;
                    isLocalActive = false;
                    startStop.setText("STOP");
                    startStop.setBackgroundColor(Color.RED);
                    startStop.setClickable(true);
                    starStopWriteValue = 1150;
                    break;

                case 689:

                    isMotorRunning = false;
                    isFirstCommNeeded = false;
                    isLocalActive = false;
                    startStop.setText("START");
                    startStop.setBackgroundColor(Color.GREEN);
                    startStop.setClickable(true);
                    starStopWriteValue = 1151;
                    break;

                case 691:

                    isMotorRunning = false;
                    isFirstCommNeeded = true;
                    isLocalActive = false;
                    startStop.setText("PRIPREMA");
                    startStop.setBackgroundColor(Color.DKGRAY);
                    starStopWriteValue = 1150;
                    break;

                default:

                    if(statusWord < 513) {
                        isLocalActive = true;
                        isMotorRunning = false;
                        isFirstCommNeeded = false;
                        startStop.setText("LOKALNI MOD");
                        startStop.setBackgroundColor(Color.LTGRAY);
                        startStop.setClickable(false);
                        break;
                    }

            }

        } else {
            startStop.setText("GRESKA");
            startStop.setBackgroundColor(Color.parseColor("#ff2280d7"));
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









}
