package com.DorsetEggs.waver;

//TODO: Test throoughly!

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import java.util.Vector;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

public class main extends ActionBarActivity {
    //TODO: The following definitions are shared with the device. It may be better to have this established with some initialization communications.
    public enum ServoTypes {DLARM, ULARM, DRARM, URARM, NUMOFSERVOS}
    private static final int MAX_SERVO_ROTATION = 180;

    class KeyframePacket {
        public byte/*uint8_t*/ servoToApplyTo;
        public byte/*uint8_t*/ degreesToReach; // 0 to 180, 0 is straight down
        //The max amount of time that an animation keyframe can take is 65 seconds.
        //We can up it later if we feel like it by upping these variables to uint32_t
        public short/*uint16_t*/ timeToPosition;//Time in ms to reach this point
    };
    //2D vector [Servo][Keyframe]
    Vector<Vector<KeyframePacket>> servoAnimations = new Vector<Vector<KeyframePacket>>();
    byte[] servoAnimationInPlay = new byte[ServoTypes.NUMOFSERVOS.ordinal()];

    public static final boolean D = BuildConfig.DEBUG; // This is automatically set when building
    private static final String TAG = "WaverActivity"; // TAG is used to debug in Android logcat console
    private static final String ACTION_USB_PERMISSION = "com.DorsetEggs.arduino.waver.robot.USB_PERMISSION";

    UsbAccessory mAccessory;
    ParcelFileDescriptor mFileDescriptor;
    FileInputStream mInputStream;
    FileOutputStream mOutputStream;
    private UsbManager mUsbManager;
    private PendingIntent mPermissionIntent;
    private boolean mPermissionRequestPending;
    TextView connectionStatus;
    boolean callOngoing;

    ConnectedThread mConnectedThread;
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        //Usb broadcast event
        public void onReceive(Context context, Intent intent) {
            instantiateAnimation();
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbAccessory accessory = UsbManager.getAccessory(intent);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                        openAccessory(accessory);
                    else {
                        if (D)
                            Log.d(TAG, "Permission denied for accessory " + accessory);
                        printDebugMessage("Permission denied for accessory " + accessory);
                    }
                    mPermissionRequestPending = false;
                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                UsbAccessory accessory = UsbManager.getAccessory(intent);
                if (accessory != null && accessory.equals(mAccessory))
                    closeAccessory();
            }
        }
    };

    private void sendKeyframe(byte servo, int keyframe)
    {
        if (mOutputStream != null) {
            try {
                servoAnimations.get(servo).get(keyframe);
                mOutputStream.write(toByteArray(servoAnimations.get(servo).get(keyframe)));
            } catch (IOException e) {
                if (D)
                    Log.e(TAG, "write failed", e);
            }
        }
    }

    public static byte[] toByteArray(Object obj) throws IOException {
        byte[] bytes = null;
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;
        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            bytes = bos.toByteArray();
        } finally {
            if (oos != null) {
                oos.close();
            }
            if (bos != null) {
                bos.close();
            }
        }
        return bytes;
    }

    private void animationCompleteReceived(byte servo)
    {
        if(true == callOngoing) {
            servoAnimationInPlay[servo] += 1;
            if(servoAnimationInPlay[servo] >= servoAnimations.get(servo).size())
            {
                servoAnimationInPlay[servo] = 0;
            }
            sendKeyframe(servo, servoAnimationInPlay[servo]);
        }
    }

    // TODO: Test the call receiver functionality fully.
    private final BroadcastReceiver mCallReceiver = new BroadcastReceiver() {
        @Override
        //Call broadcast event
        public void onReceive(Context context, Intent intent) {
            //Call broadcast event
            if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                // This code will execute when the phone has an incoming call
                // get the phone number
                String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                printCallReceivedMessage("Call from:" + incomingNumber);
                callOngoing = true;
                for(byte i = 0; i < ServoTypes.NUMOFSERVOS.ordinal(); ++i)
                {
                    sendKeyframe(i, 0);
                }

            } else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(
                    TelephonyManager.EXTRA_STATE_IDLE)
                    || intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(
                    TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                printCallDroppedMessage("Detected call hangup event");
                callOngoing = false;
                resetServoAnimations();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        callOngoing = false;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        connectionStatus = (TextView) findViewById(R.id.connectionStatus);

        mUsbManager = UsbManager.getInstance(this);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

        IntentFilter usbFilter = new IntentFilter(ACTION_USB_PERMISSION);
        usbFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(mUsbReceiver, usbFilter);

        IntentFilter callFilter = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        registerReceiver(mCallReceiver, callFilter);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mAccessory != null) {
            setConnectionStatus(true);
            return;
        }

        UsbAccessory[] accessories = mUsbManager.getAccessoryList();
        UsbAccessory accessory = (accessories == null ? null : accessories[0]);
        if (accessory != null) {
            if (mUsbManager.hasPermission(accessory))
                openAccessory(accessory);
            else {
                setConnectionStatus(false);
                synchronized (mUsbReceiver) {
                    if (!mPermissionRequestPending) {
                        mUsbManager.requestPermission(accessory, mPermissionIntent);
                        mPermissionRequestPending = true;
                    }
                }
            }
        } else {
            setConnectionStatus(false);
            if (D)
                Log.d(TAG, "mAccessory is null");
            printDebugMessage("mAccessory is null");

        }
    }

    @Override
    public void onBackPressed() {
        if (mAccessory != null) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Closing Activity")
                    .setMessage("Are you sure you want to close this application?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        } else
            finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeAccessory();
        unregisterReceiver(mUsbReceiver);
        unregisterReceiver(mCallReceiver);
    }

    private void openAccessory(UsbAccessory accessory) {
        mFileDescriptor = mUsbManager.openAccessory(accessory);
        if (mFileDescriptor != null) {
            mAccessory = accessory;
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            mInputStream = new FileInputStream(fd);
            mOutputStream = new FileOutputStream(fd);

            mConnectedThread = new ConnectedThread(this);
            mConnectedThread.start();

            setConnectionStatus(true);

            if (D)
                Log.d(TAG, "Accessory opened");
            printDebugMessage("Accessory opened");
        } else {
            setConnectionStatus(false);
            if (D)
                Log.d(TAG, "Accessory open failed");
            printDebugMessage("Accessory open failed");
        }
    }

    private void setConnectionStatus(boolean connected) {
        connectionStatus.setText(connected ? "Connected" : "Disconnected");
    }

    private void closeAccessory() {
        setConnectionStatus(false);

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Close all streams
        try {
            if (mInputStream != null)
                mInputStream.close();
        } catch (Exception ignored) {
        } finally {
            mInputStream = null;
        }
        try {
            if (mOutputStream != null)
                mOutputStream.close();
        } catch (Exception ignored) {
        } finally {
            mOutputStream = null;
        }
        try {
            if (mFileDescriptor != null)
                mFileDescriptor.close();
        } catch (IOException ignored) {
        } finally {
            mFileDescriptor = null;
            mAccessory = null;
        }
    }

    private class ConnectedThread extends Thread {
        Activity activity;
        TextView mTextView;
        byte[] buffer = new byte[1024];
        boolean running;

        ConnectedThread(Activity activity) {
            this.activity = activity;
            mTextView = (TextView) findViewById(R.id.textView);
            running = true;
        }

        public void run() {
            while (running) {
                try {
                    int bytes = mInputStream.read(buffer);
                    if (bytes > 0) { // The message is 1 bytes long
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                byte servo = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).get();
                                animationCompleteReceived(servo);
                            }
                        });
                    }
                } catch (Exception ignore) {
                }
            }
        }

        public void cancel() {
            running = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void printDebugMessage(String output) {
        TextView mDebugOutput;
        mDebugOutput = (TextView) findViewById(R.id.debugOutput);
        mDebugOutput.setText(output);
    }

    private void printCallDroppedMessage(String output) {
        TextView mCallDroppedInfo;
        mCallDroppedInfo = (TextView) findViewById(R.id.callDroppedInfo);
        mCallDroppedInfo.setText(output);
    }

    private void printCallReceivedMessage(String output) {
        TextView mCallRecievedInfo;
        mCallRecievedInfo = (TextView) findViewById(R.id.callRecievedInfo);
        mCallRecievedInfo.setText(output);
    }

    private void instantiateAnimation()
    {
        for(byte i = 0; i < ServoTypes.NUMOFSERVOS.ordinal(); ++i) {
            Vector<KeyframePacket> servoAnimation = new Vector<KeyframePacket>();
            KeyframePacket keyframe1 = new KeyframePacket();
            KeyframePacket keyframe2 = new KeyframePacket();

            keyframe1.servoToApplyTo = i;
            keyframe1.degreesToReach = 90;
            keyframe1.timeToPosition = 1000;
            servoAnimation.add(keyframe1);
            keyframe2.servoToApplyTo = i;
            keyframe2.degreesToReach = 0;
            keyframe2.timeToPosition = 1000;
            servoAnimation.add(keyframe2);
            servoAnimations.add(i, servoAnimation);
        }
        resetServoAnimations();
    }

    private void resetServoAnimations()
    {
        for(byte i = 0; i < ServoTypes.NUMOFSERVOS.ordinal(); ++i) {
            servoAnimationInPlay[i] = 0;
        }
    }
}
