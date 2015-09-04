package edu.nitt.spider.usbcommunication;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;

public class TransferHub extends AppCompatActivity {

    private static final int VID = 0x2341;
    private static final int PID = 0x0043;//I believe it is 0x0000 for the Arduino Megas


    private final IUsbConnectionHandler mConnectionHandler = new IUsbConnectionHandler() {
        @Override
        public void onUsbStopped() {
            Log.e("Activity","Usb stopped!");
        }

        @Override
        public void onErrorLooperRunningAlready() {
            Log.e("Activity","Looper already running!");
        }

        @Override
        public void onDeviceNotFound() {
            if(sUsbController != null){
                sUsbController.stop();
                sUsbController = null;
            }
        }
    };

    private static UsbController sUsbController;
    public static Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_hub);

        if(sUsbController == null){
            sUsbController = new UsbController(this, mConnectionHandler, VID, PID);
        }
        ((SeekBar)(findViewById(R.id.seekBar1))).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                if (fromUser) {
                    if (sUsbController != null) {
                        sUsbController.send((byte) (progress & 0xFF));
                    }
                }
            }
        });
        ((Button)findViewById(R.id.buttonEnum)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sUsbController == null)
                    sUsbController = new UsbController(TransferHub.this, mConnectionHandler, VID, PID);
                else {
                    sUsbController.stop();
                    sUsbController = new UsbController(TransferHub.this, mConnectionHandler, VID, PID);
                }
            }
        });
        ((Button)findViewById(R.id.button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText data = (EditText)findViewById(R.id.editText);
                //UsbController.send((byte) (d & 0xFF));
            }
        });

        mContext = this;
    }

    public void sendData(View v){

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_transfer_hub, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
