package com.cele.diplomskibasura.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;


public class Splash extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);


        new Handler().postDelayed(new Runnable() {

            // Using handler with postDelayed called runnable run method

            @Override
            public void run() {
                Intent i = new Intent(Splash.this, MainActivity.class);
                startActivity(i);

                // close this activity
                finish();
            }
        }, 3 * 1000); // wait for 3 seconds

    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

    }


}

