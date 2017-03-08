package dazi.com.uart;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import dazi.com.reboot.DevConnector;

public class MainActivity extends AppCompatActivity {

    DevConnector connector = null;
    public SharedPreferences sharePrefSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharePrefSettings = getSharedPreferences("UARTLBPref", 0);
        connector = new DevConnector(this, sharePrefSettings);

        Button btnLink = (Button) findViewById(R.id.btnLink);
        Button btnSend = (Button) findViewById(R.id.btnSend);
        Button btnScan = (Button) findViewById(R.id.btnScan);
        Button btnSleep = (Button) findViewById(R.id.btnSleep);
        TextView shwoText = (TextView) findViewById(R.id.showText);

               /* handle write click */
        btnSend.setOnClickListener(new View.OnClickListener() {

            // @Override
            public void onClick(View v) {
                if (connector.wakeupDev()) {
                    msgToast("设备唤醒成功", Toast.LENGTH_LONG);
                }
            }
        });

                       /* handle write click */
        btnScan.setOnClickListener(new View.OnClickListener() {

            // @Override
            public void onClick(View v) {
                if (connector.scan()) {
                    msgToast("设备扫描成功", Toast.LENGTH_LONG);
                }
            }
        });

                       /* handle write click */
        btnSleep.setOnClickListener(new View.OnClickListener() {

            // @Override
            public void onClick(View v) {
                if (connector.sleepDev()) {
                    msgToast("设备休眠成功", Toast.LENGTH_LONG);
                }
            }
        });


    }


    void msgToast(String str, int showTime)
    {
        Toast.makeText(this, str, showTime).show();
    }

    @Override
    protected void onResume() {
        // Ideally should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onResume();
        connector.ResumeAccessory();
    }


    @Override
    protected void onDestroy() {
        connector.DestroyAccessory();
        super.onDestroy();
    }
}
