package com.cele.diplomskibasura.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.ghgande.j2mod.modbus.net.TCPMasterConnection;

import java.util.Map;
import java.util.Set;


public class Postavke extends Activity {

    Button btnSave;
    EditText acsIp;
    EditText acsPort;
    EditText sentronIP;
    EditText sentronPort;

    EditText acsCntrIn;
    EditText acsCntrOut;
    EditText acsSpeedRefIn;
    EditText acsSpeedRefOut;
    EditText acsPowerIn;
    EditText acsCurrentIn;



    // User name (make variable public to access from outside)
    public static final String ACS_IP = "acsIP";

    public static final String ACS_PORT = "acsPort";

    // Email address (make variable public to access from outside)
    public static final String SENTRON_IP = "acsIP";

    public static final String SENTRON_PORT = "sentronPort";

    public static final String ACS_CNTR_WORD_NAME = "acsControl";

    public static final String ACS_STS_WORD_NAME = "acsStatus";

    public static final String ACS_SPEED_REF_READ = "acsSpeedRefRead";

    public static final String ACS_SPEED_REF_WRITE = "acsSpeedRefWrite";

    public static final String ACS_POWER_READ = "acsPower";

    public static final String ACS_CURRENT_READ = "acsCurrent";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_postavke);


        final SharedPreferences pref = getApplicationContext().getSharedPreferences(Constants.MY_PREFS, 0); // 0 - for private mode


        acsIp = (EditText) findViewById(R.id.edt_acs880_ip);
        acsPort = (EditText) findViewById(R.id.edt_acs880_port);

        sentronIP = (EditText) findViewById(R.id.edt_sentron_ip);
        sentronPort = (EditText) findViewById(R.id.edt_sentron_port);

        btnSave = (Button) findViewById(R.id.btn_save_settings);

        acsCntrIn = (EditText) findViewById(R.id.edt_acs_contr_word_adr);
        acsCntrOut = (EditText) findViewById(R.id.edt_acs_status_wrd_adr);
        acsSpeedRefIn = (EditText) findViewById(R.id.edt_acs_speedref_read_adr);
        acsSpeedRefOut = (EditText) findViewById(R.id.edt_acs_speedref_set_adr);
        acsPowerIn = (EditText) findViewById(R.id.edt_acs_power_read_adr);
        acsCurrentIn = (EditText) findViewById(R.id.edt_acs_current_read_adr);

        acsIp.setText(pref.getString(ACS_IP, Constants.DEFUALT_ACS_IP));
        acsPort.setText(pref.getInt(ACS_PORT, Constants.DEFUALT_ACS_PORT) + "");

        sentronIP.setText(pref.getString(SENTRON_IP, Constants.DEFUALT_PAC_IP));
        sentronPort.setText(pref.getInt(SENTRON_PORT, Constants.DEFUALT_PAC_PORT) + "");

        acsCntrIn.setText(pref.getInt(ACS_CNTR_WORD_NAME, Constants.DEFUALT_ACS_CNTR_WRD_ADR) + "");
        acsCntrOut.setText(pref.getInt(ACS_STS_WORD_NAME, Constants.DEFUALT_ACS_STS_WRD_ADR) + "");
        acsSpeedRefIn.setText(pref.getInt(ACS_SPEED_REF_READ, Constants.DEFUALT_ACS_SPEED_REF_READ_ADR) + "");
        acsSpeedRefOut.setText(pref.getInt(ACS_SPEED_REF_WRITE, Constants.DEFUALT_ACS_SPEED_REF_WRT_ADR) + "");
        acsPowerIn.setText(pref.getInt(ACS_POWER_READ, Constants.DEFUALT_ACS_POWER_READ_ADR) + "");
        acsCurrentIn.setText(pref.getInt(ACS_CURRENT_READ, Constants.DEFUALT_ACS_CURRENT_READ_ADR) + "");





        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                SharedPreferences.Editor editor = pref.edit();


                editor.putString(ACS_IP, acsIp.getText().toString());
                editor.putInt(ACS_PORT, Integer.valueOf(acsPort.getText().toString()));

                editor.putString(SENTRON_IP, sentronIP.getText().toString());
                editor.putInt(SENTRON_PORT, Integer.valueOf(sentronPort.getText().toString()));

                editor.putInt(ACS_CNTR_WORD_NAME, Integer.valueOf(acsCntrIn.getText().toString()));

                editor.putInt(ACS_STS_WORD_NAME, Integer.valueOf(acsCntrOut.getText().toString()));

                editor.putInt(ACS_SPEED_REF_READ, Integer.valueOf(acsSpeedRefIn.getText().toString()));

                editor.putInt(ACS_SPEED_REF_WRITE, Integer.valueOf(acsSpeedRefOut.getText().toString()));

                editor.putInt(ACS_POWER_READ, Integer.valueOf(acsPowerIn.getText().toString()));

                editor.putInt(ACS_CURRENT_READ, Integer.valueOf(acsCurrentIn.getText().toString()));

                editor.commit();
                Toast.makeText(getBaseContext(), "Saved", Toast.LENGTH_SHORT).show();

                Intent i = new Intent(Postavke.this, MainActivity.class);
                // Closing all the Activities
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);


                // Staring Login Activity
                startActivity(i);
            }
        });


    }

    @Override
    protected void onResume() {

        super.onResume();


    }

}