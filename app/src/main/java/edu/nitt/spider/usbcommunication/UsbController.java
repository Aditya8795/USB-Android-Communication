package edu.nitt.spider.usbcommunication;

        import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Iterator;

public class UsbController {

    private final Context mApplicationContext;
    private final UsbManager mUsbManager;
    private final IUsbConnectionHandler mConnectionHandler;
    private final int VID;
    private final int PID;
    protected static final String ACTION_USB_PERMISSION = "ch.serverbox.android.USB";

    /**
     * Activity is needed for onResult
     *
     * @param parentActivity
     */
    public UsbController(
            Activity parentActivity,
            IUsbConnectionHandler connectionHandler,
            int vid, int pid
    ) {
        mApplicationContext = parentActivity.getApplicationContext();
        mConnectionHandler = connectionHandler;
        mUsbManager = (UsbManager) mApplicationContext
                .getSystemService(Context.USB_SERVICE);
        VID = vid;
        PID = pid;
        init();
    }

    private void init() {
        enumerate(new IPermissionListener() {
            @Override
            public void onPermissionDenied(UsbDevice d) {
                // this gets a notification on your screen asking you to let the app access the USB
                UsbManager usbman = (UsbManager) mApplicationContext
                        .getSystemService(Context.USB_SERVICE);
                PendingIntent pi = PendingIntent.getBroadcast(
                        mApplicationContext, 0, new Intent(
                                ACTION_USB_PERMISSION), 0);
                mApplicationContext.registerReceiver(mPermissionReceiver,
                        new IntentFilter(ACTION_USB_PERMISSION));
                mApplicationContext.registerReceiver(mUsbReceiver,new IntentFilter());
                usbman.requestPermission(d, pi);
            }
        });
    }

    public void stop() {
        mStop = true;
        synchronized (sSendLock) {
            sSendLock.notify();
        }
        try {
            if(mUsbThread != null)
                mUsbThread.join();
        } catch (InterruptedException e) {
            e(e);
        }
        mStop = false;
        mLoop = null;
        mUsbThread = null;

        try{
            mApplicationContext.unregisterReceiver(mPermissionReceiver);
        }catch(IllegalArgumentException e){};//bravo
    }

    private UsbRunnable mLoop;
    private Thread mUsbThread;

    private void startHandler(UsbDevice d) {
        if (mLoop != null) {
            mConnectionHandler.onErrorLooperRunningAlready();
            return;
        }
        mLoop = new UsbRunnable(d);
        mUsbThread = new Thread(mLoop);
        mUsbThread.start();
    }

    public void send(byte data) {
        mData = data;
        synchronized (sSendLock) {
            sSendLock.notify();
        }
    }

    private void enumerate(IPermissionListener listener) {
        l("enumerating");
        HashMap<String, UsbDevice> devlist = mUsbManager.getDeviceList();
        Iterator<UsbDevice> deviter = devlist.values().iterator();
        while (deviter.hasNext()) {
            UsbDevice d = deviter.next();

            Toast.makeText(mApplicationContext,"Found device: "
                    + String.format("%04X:%04X", d.getVendorId(),
                    d.getProductId()),Toast.LENGTH_SHORT).show();

            l("Found device: "
                    + String.format("%04X:%04X", d.getVendorId(),
                    d.getProductId()));
            if (d.getVendorId() == VID && d.getProductId() == PID) {
                Toast.makeText(mApplicationContext,"Device under: " + d.getDeviceName(),Toast.LENGTH_SHORT).show();
                l("Device under: " + d.getDeviceName());
                if (!mUsbManager.hasPermission(d))
                    listener.onPermissionDenied(d);
                else{
                    startHandler(d);
                    return;
                }
                break;
            }
        }
        Toast.makeText(mApplicationContext,"no more devices found",Toast.LENGTH_SHORT).show();
        l("no more devices found");
        mConnectionHandler.onDeviceNotFound();
    }

    private class PermissionReceiver extends BroadcastReceiver {
        private final IPermissionListener mPermissionListener;

        public PermissionReceiver(IPermissionListener permissionListener) {
            mPermissionListener = permissionListener;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            mApplicationContext.unregisterReceiver(this);
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                if (!intent.getBooleanExtra(
                        UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    mPermissionListener.onPermissionDenied((UsbDevice) intent
                            .getParcelableExtra(UsbManager.EXTRA_DEVICE));
                } else {
                    l("Permission granted");
                    UsbDevice dev = (UsbDevice) intent
                            .getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (dev != null) {
                        if (dev.getVendorId() == VID
                                && dev.getProductId() == PID) {
                            startHandler(dev);// has new thread
                        }
                    } else {
                        e("device not present!");
                    }
                }
            }
        }

    }

    // MAIN LOOP
    private static final Object[] sSendLock = new Object[]{};//learned this trick from some google example :)
    //basically an empty array is lighter than an  actual new Object()...
    private boolean mStop = false;
    private byte mData = 0x00;

    private class UsbRunnable implements Runnable {
        private final UsbDevice mDevice;

        UsbRunnable(UsbDevice dev) {
            mDevice = dev;
        }

        @Override
        public void run() {

            //here the main USB functionality is implemented
            //Toast.makeText(mApplicationContext,"Run started",Toast.LENGTH_SHORT).show();
            Log.i(TAG,"inside run of USB controller");
            UsbDeviceConnection conn = mUsbManager.openDevice(mDevice);
            if (!conn.claimInterface(mDevice.getInterface(1), true)) {
                //Toast.makeText(mApplicationContext,"allow me to access the USB",Toast.LENGTH_SHORT).show();
                return;
            }
            // Arduino Serial usb Conv
           // conn.controlTransfer(0x21, 34, 0, 0, null, 0, 0);
           // conn.controlTransfer(0x21, 32, 0, 0, new byte[] { (byte) 0x80, 0x25, 0x00, 0x00, 0x00, 0x00, 0x08 }, 7, 0);

            conn.controlTransfer(0x40, 0, 0, 0, null, 0, 0);// reset
            // mConnection.controlTransfer(0×40,
            // 0, 1, 0, null, 0,
            // 0);//clear Rx
            conn.controlTransfer(0x40, 0, 2, 0, null, 0, 0);// clear Tx
            conn.controlTransfer(0x40, 0x02, 0x0000, 0, null, 0, 0);// flow
            // control
            // none
            conn.controlTransfer(0x40, 0x03, 0x0034, 0, null, 0, 0);// baudrate
            // 57600
            conn.controlTransfer(0x40, 0x04, 0x0008, 0, null, 0, 0);// data bit
            // 8, parity
            // none,
            // stop bit
            // 1, tx off

            //Toast.makeText(mApplicationContext,"control transfer sent",Toast.LENGTH_SHORT).show();

            UsbEndpoint epIN = null;
            UsbEndpoint epOUT = null;

            UsbInterface usbIf = mDevice.getInterface(1);
            for (int i = 0; i < usbIf.getEndpointCount(); i++) {
                if (usbIf.getEndpoint(i).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (usbIf.getEndpoint(i).getDirection() == UsbConstants.USB_DIR_IN)
                        epIN = usbIf.getEndpoint(i);
                    else
                        epOUT = usbIf.getEndpoint(i);
                }
            }

            //Toast.makeText(mApplicationContext,"End points set",Toast.LENGTH_SHORT).show();

            for (;;) {// this is the main loop for transferring
                synchronized (sSendLock) {//ok there should be a OUT queue, no guarantee that the byte is sent actually
                    try {
                        sSendLock.wait();
                    } catch (InterruptedException e) {
                        if (mStop) {
                            mConnectionHandler.onUsbStopped();
                            return;
                        }
                        e.printStackTrace();
                    }
                }

                //Toast.makeText(mApplicationContext,"before bulk transfer",Toast.LENGTH_SHORT).show();

                //conn.bulkTransfer(epOUT, new byte[]{mData}, 1, 0);

                // This is where it is meant to receive
                byte[] buffer = new byte[1];

                //StringBuilder str = new StringBuilder();
                Log.i(TAG,"Before bulk transfer");

                while (conn.bulkTransfer(epIN, buffer, 0,1, 0) >= 0) {
                    String temp = String.valueOf(buffer[0]);
                    Log.i(TAG,buffer[0]+ " " + temp);
                   /*for (int i = 0; i < 1; i++) {
                        Log.i(TAG, "" + buffer[i]);
                        //String s1 = String.format("%8s", Integer.toBinaryString(buffer[i] & 0xFF)).replace(' ', '0');
                        //Log.i(TAG," "+s1+" ");
                        if (buffer[i] != 0) {
                            Log.i(TAG," "+buffer[i]+" ");
                        } else {
                            Log.i(TAG,"Buffer is 0");
                            Log.i(TAG," "+buffer[i]+" ");
                            //break;
                        }
                    }*/

                }

                //Toast.makeText(mApplicationContext,"Eureka",Toast.LENGTH_SHORT).show();


                if (mStop) {
                    mConnectionHandler.onUsbStopped();
                    return;
                }
            }
        }
    }

    // END MAIN LOOP
    private BroadcastReceiver mPermissionReceiver = new PermissionReceiver(
            new IPermissionListener() {
                @Override
                public void onPermissionDenied(UsbDevice d) {
                    l("Permission denied on " + d.getDeviceId());
                }
            });

    private static interface IPermissionListener {
        void onPermissionDenied(UsbDevice d);
    }

    public final static String TAG = "USBController";

    private void l(Object msg) {
        Log.d(TAG, ">==< " + msg.toString() + " >==<");
    }

    private void e(Object msg) {
        Log.e(TAG, ">==< " + msg.toString() + " >==<");
    }

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    // call your method that cleans up and closes communication with the device
                    mConnectionHandler.onUsbStopped();
                }
            }
        }
    };
}