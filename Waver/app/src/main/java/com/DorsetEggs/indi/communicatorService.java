/**
 *
 * Copyright (c) 2014 Billy Wood
 * This file is part of Indi.
 *
 * Indi is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Indi is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Indi.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.DorsetEggs.indi;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;

import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Vector;

public class communicatorService extends IntentService {
    //Notification variables
    NotificationCompat.Builder mBuilder;
    int mId = 29;
    NotificationManager mNotificationManager;

    class KeyframePacket {
        public byte/*int8_t*/ servoToApplyTo;
        public short/*int16_t*/ degreesToReach; // 0 to 180, 0 is straight down
        //The max amount of time that an animation keyframe can take is 65 seconds.
        //We can up it later if we feel like it by upping these variables to uint32_t
        public int/*int32_t*/ timeToPosition;//Time in ms to reach this point
    }

    //2D vector [Servo][Keyframe]
    Vector<Vector<KeyframePacket>> servoAnimations = new Vector<Vector<KeyframePacket>>();
    Vector<KeyframePacket> defaultServoPositions = new Vector<KeyframePacket>(); // The default positions for the motors
    byte[] servoAnimationInPlay = new byte[globals.ServoTypes.NUMOFSERVOS.ordinal()];

    private static final String ACTION_USB_PERMISSION = "com.DorsetEggs.arduino.indi.robot.USB_PERMISSION";

    UsbAccessory mAccessory;
    ParcelFileDescriptor mFileDescriptor;
    FileOutputStream mOutputStream;
    private UsbManager mUsbManager;
    private PendingIntent mPermissionIntent;
    private boolean mPermissionRequestPending;
    boolean callOngoing;
    boolean sendReset = false;
    private long[] outgoneMotorAnimation;
    private boolean[] motorPlayingAnimation;
    int timeBetweenProcessing = 0;
    long start = 0;

    SQLiteDatabase db;

    /**
     * A constructor is required, and must call the super IntentService(String)
     * constructor with a name for the worker thread.
     */
    public communicatorService() {
        super("communicatorService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Connect this function to the film player so it can recieve a message when its ready
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("start-kickstarter-anim"));
        globals.sendDebugMessage("CommunicatorService is up");
        outgoneMotorAnimation = new long[globals.ServoTypes.NUMOFSERVOS.ordinal()];
        motorPlayingAnimation = new boolean[globals.ServoTypes.NUMOFSERVOS.ordinal()];
    }

    /**
     * Handles the message from the film player to start the animation
     */
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int message = intent.getIntExtra("message", 0);
            if(0 == message)
            {
                globals.sendErrorMessage("Broadcast error, 0 sent");
            }
            globals.sendDebugMessage("Starting Kickstarter animation");
            sendAnimation(message);
        }
    };

    /**
     * The IntentService calls this method from the default worker thread with
     * the intent that started the service. When this method returns, IntentService
     * stops the service, as appropriate.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        callOngoing = false;
        globals.sendDebugMessage("Waiting for connection");

        instantiateAnimation();

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

        IntentFilter usbFilter = new IntentFilter(ACTION_USB_PERMISSION);
        usbFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(mUsbReceiver, usbFilter);

        IntentFilter callFilter = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        registerReceiver(mCallReceiver, callFilter);

        if (mAccessory != null) {
            setConnectionStatus(true);
            return;
        }

        UsbAccessory[] accessories = mUsbManager.getAccessoryList();
        UsbAccessory accessory = (accessories == null ? null : accessories[0]);
        if (accessory != null) {
            globals.sendDebugMessage("USB ready");
            if (mUsbManager.hasPermission(accessory)) {
                openAccessory(accessory);
                launchNotification(mId, "Indi connected");
                //Initialise the database
                db = globals.dbHelper.getWritableDatabase();
                resetMotors();
            } else {
                globals.sendDebugMessage("No USB permission");
                setConnectionStatus(false);
                synchronized (mUsbReceiver) {
                    if (!mPermissionRequestPending) {
                        mUsbManager.requestPermission(accessory, mPermissionIntent);
                        mPermissionRequestPending = true;
                    }
                }
            }
        } else {
            globals.sendDebugMessage("mAccessory is null");
            setConnectionStatus(false);
        }
        // Play here until the phone is disconnected
        while (true)
        {
            checkAnimation();
            // Used for debugging USB errors, we activate this and see exactly where it breaks.
            //transmitSingleByte();
        }
    }

    private void checkAnimation()
    {
        long currentTime = SystemClock.elapsedRealtime();
        timeBetweenProcessing = (int)(currentTime - start);
        start = currentTime;
        for(byte i = 0; i < globals.ServoTypes.NUMOFSERVOS.ordinal(); ++i) {
            if(true == motorPlayingAnimation[i]) {
                outgoneMotorAnimation[i] += timeBetweenProcessing;
                if (outgoneMotorAnimation[i] > servoAnimations.get(i).get(servoAnimationInPlay[i]).timeToPosition) {
                    animationComplete(i);
                    outgoneMotorAnimation[i] = 0;
                }
            }
        }
    }

    private void launchNotification(int id, String notificationText) {
        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(notificationText);
        NotificationCompat.InboxStyle inboxStyle =
                new NotificationCompat.InboxStyle();
        mBuilder.setStyle(inboxStyle);
        mNotificationManager.notify(id, mBuilder.build());
    }

    //Wrapper class for the below 'transmitKeyFrame'
    private void sendKeyframe(byte servo, int keyframe) {
        transmitKeyFrame(servoAnimations.get(servo).get(keyframe));
        globals.sendDebugMessage("Keyframe sent - servo == " + servo + " - keyframe == " + keyframe  + " - time to == " + servoAnimations.get(servo).get(keyframe).timeToPosition + " - degrees == " + servoAnimations.get(servo).get(keyframe).degreesToReach);
    }

    private void transmitKeyFrame(KeyframePacket keyframePacket) {
        if (mOutputStream != null) {
            try {
                byte[] byteArray = toByteArray(keyframePacket);
                mOutputStream.write(byteArray);
            } catch (IOException e) {
                //globals.sendErrorMessage("write failed", e);
                globals.sendErrorMessage("write failed, probably an ENODEV");
            }
        }
    }

    // Used for debugging USB errors
    private void transmitSingleByte() {
        if (mOutputStream != null) {
            try {
                mOutputStream.write(5);
            } catch (IOException e) {
                //globals.sendErrorMessage("write failed", e);
                globals.sendErrorMessage("write failed when transmitting single byte, probably an ENODEV");
                setConnectionStatus(false);
            }
        }
    }

    private void instantiateAnimation() {
        //Load the default position
        for (byte i = 0; i < globals.ServoTypes.NUMOFSERVOS.ordinal(); ++i) {
            KeyframePacket keyframe = new KeyframePacket();
            keyframe.servoToApplyTo = i;
            keyframe.degreesToReach = 0;
            keyframe.timeToPosition = 1000;
            defaultServoPositions.add(keyframe);
        }
    }

    public static byte[] toByteArray(KeyframePacket obj) throws IOException {
        ByteBuffer bytes = ByteBuffer.allocate(7); // The size of a keyframe packet
        bytes.order(ByteOrder.LITTLE_ENDIAN);
        bytes.put(obj.servoToApplyTo);
        bytes.putShort(obj.degreesToReach);
        bytes.putInt(obj.timeToPosition);
        return bytes.array();
    }

    private void animationComplete(byte servo) {
        if (servo >= globals.ServoTypes.NUMOFSERVOS.ordinal() )
        {
            globals.sendErrorMessage("Data recieve error");
        }
        else if (true/* == callOngoing*/) { // Put back when done!
            servoAnimationInPlay[servo] += 1;
            if (servoAnimationInPlay[servo] >= servoAnimations.get(servo).size()) {
                // Put back when done with the Kickstarter version, stops the animations repeating
                //servoAnimationInPlay[servo] = 0;
                // And remove this below!
                resetMotor(servo);
                globals.sendDebugMessage("Animation finished");
                motorPlayingAnimation[servo] = false;

            }
            if(true == motorPlayingAnimation[servo]) {
                sendKeyframe(servo, servoAnimationInPlay[servo]);
            }
            sendReset = true;
        } else if (sendReset) //If the call is disconnected, send a signal to reset the motors once
        {
            globals.sendDebugMessage("Animation finishing");
            resetMotor(servo);
            sendReset = false;
            globals.sendDebugMessage("Animation finished");
            motorPlayingAnimation[servo] = false;
        }
        else
        {
            globals.sendErrorMessage("State machine error on disconnected call");
        }
    }

    public void resetMotors() {
        globals.sendDebugMessage("Resetting motors");
        //Make sure the servos are in the default position
        for (byte i = 0; i < globals.ServoTypes.NUMOFSERVOS.ordinal(); ++i) {
            resetMotor(i);
        }
    }

    public void resetMotor(byte servo) {
        //globals.sendDebugMessage("Resetting motor " + servo);
        //Make sure the servos are in the default position
        transmitKeyFrame(defaultServoPositions.elementAt(servo));
    }

    @Override
    public void onDestroy() {
        globals.sendDebugMessage("USB communicator closing");
        closeAccessory();
        unregisterReceiver(mUsbReceiver);
        unregisterReceiver(mCallReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
        globals.sendDebugMessage("USB communicator closed");
    }

    private void setConnectionStatus(boolean connected) {
        //exit when disconnected
        if (connected == false) {
            globals.sendDebugMessage("Exiting due to disconnection");
            if (mNotificationManager != null) {
                mNotificationManager.cancel(mId);
                globals.sendDebugMessage("Closing notification");
            }
            stopSelf();
        }
    }

    private void openAccessory(UsbAccessory accessory) {
        mFileDescriptor = mUsbManager.openAccessory(accessory);
        if (mFileDescriptor != null) {
            mAccessory = accessory;
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            mOutputStream = new FileOutputStream(fd);

            setConnectionStatus(true);

            globals.sendDebugMessage("Accessory opened");
        } else {
            globals.sendDebugMessage("Accessory open failed");
            setConnectionStatus(false);
        }
    }

    private void closeAccessory() {
        globals.sendDebugMessage("USB accessory closing");
        setConnectionStatus(false);

        // Close all streams
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

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        //Usb broadcast event
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbAccessory accessory = intent.getParcelableExtra
                            (UsbManager.EXTRA_ACCESSORY);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        openAccessory(accessory);
                    } else {
                        globals.sendDebugMessage("Permission denied for accessory " + accessory);
                    }
                    mPermissionRequestPending = false;
                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra
                        (UsbManager.EXTRA_ACCESSORY);
                if (accessory != null && accessory.equals(mAccessory)) {
                    globals.sendDebugMessage("Accessory detached");
                    closeAccessory();
                }
            }
        }
    };

    private final BroadcastReceiver mCallReceiver = new BroadcastReceiver() {
        @Override
        //Call broadcast event
        public void onReceive(Context context, Intent intent) {
            //Call broadcast event
            if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                // This code will execute when the phone has an incoming call
                // get the phone number
                String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                globals.sendDebugMessage("Call from:" + incomingNumber);
                callOngoing = true;
                sendAnimation(globals.animOptions.CALL_RECIEVED.ordinal());

            } else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(
                    TelephonyManager.EXTRA_STATE_IDLE)
                    || intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(
                    TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                globals.sendDebugMessage("Detected call hangup event");
                callOngoing = false;
                resetMotors();
            }
        }
    };

    private void sendAnimation(int animOption) {
        //First, load the animation to use
        String[] projectionAnimToActions = {
                globals.AnimToActions.COLUMN_NAME_ACTION,
                globals.AnimToActions.COLUMN_NAME_ANIMATION,
        };
        String sortOrderAnimToActions =
                globals.AnimToActions.COLUMN_NAME_ACTION + " DESC";
        Cursor cAnimToActions = db.query(
                globals.AnimToActions.TABLE_NAME,                              // The table to query
                projectionAnimToActions,                                       // The columns to return
                globals.AnimToActions.COLUMN_NAME_ACTION + " = " + animOption, // The columns for the WHERE clause
                null,                                                          // The values for the WHERE clause
                null,                                                          // don't group the rows
                null,                                                          // don't filter by row groups
                sortOrderAnimToActions                                         // The sort order
        );

        if (cAnimToActions.getColumnCount() == 0) {
            globals.sendDebugMessage("Animation not found for action");
        }

        //Next load the selected animation
        String[] projectionAnimations = {
                globals.Animations.COLUMN_NAME_POSITION,
                globals.Animations.COLUMN_NAME_TIME,
        };
        String sortOrderAnimations =
                globals.Animations.COLUMN_NAME_KEYFRAME + " ASC";

        //Finally place the animation into the array for later processing
        cAnimToActions.moveToFirst();
        for (Integer i = 0; i < globals.ServoTypes.NUMOFSERVOS.ordinal(); ++i) {
            String whereClause = globals.Animations.COLUMN_NAME_TITLE + " = \"" + cAnimToActions.getString(globals.AnimToActions.COLUMN_INDEX_ANIMATION) + "\"" +
                    " AND " + globals.Animations.COLUMN_NAME_MOTOR + " = " + i;
            //globals.sendDebugMessage("Loading animation " + cAnimToActions.getString(globals.AnimToActions.COLUMN_INDEX_ANIMATION) + " for motor " + i);

            Cursor cAnimations = db.query(
                    globals.Animations.TABLE_NAME, // The table to query
                    projectionAnimations,          // The columns to return
                    whereClause,                   // The columns for the WHERE clause
                    null,                          // The values for the WHERE clause
                    null,                          // don't group the rows
                    null,                          // don't filter by row groups
                    sortOrderAnimations            // The sort order
            );
            if (cAnimations.getColumnCount() == 0) {
                globals.sendDebugMessage("Animation not found");
            }

            if (cAnimations != null) {
                if (cAnimations.moveToFirst()) {
                    int positionColumn = cAnimations.getColumnIndex(globals.Animations.COLUMN_NAME_POSITION);
                    int timeColumn = cAnimations.getColumnIndex(globals.Animations.COLUMN_NAME_TIME);
                    Vector<KeyframePacket> servoAnimation = new Vector<KeyframePacket>();
                    do {
                        KeyframePacket keyframe = new KeyframePacket();
                        keyframe.servoToApplyTo = i.byteValue();
                        keyframe.degreesToReach = (byte) cAnimations.getInt(positionColumn);
                        keyframe.timeToPosition = cAnimations.getInt(timeColumn);
                        servoAnimation.add(keyframe);
                        //globals.sendDebugMessage("Servo animation added to motor " + keyframe.servoToApplyTo + ", degrees to reach == " + keyframe.degreesToReach + ", time to position == " + keyframe.timeToPosition);
                    } while (cAnimations.moveToNext());
                    servoAnimations.add(i, servoAnimation);
                    //globals.sendDebugMessage("Complete servo animation added to motor " + i);
                }
                else
                {
                    globals.sendErrorMessage("moveToFirst failed, cursor empty");
                    globals.sendDebugMessage("Stored animations ==");
                    String[] title = {
                            globals.Animations.COLUMN_NAME_TITLE
                    };
                    String sortOrderTitle =
                            globals.Animations.COLUMN_NAME_TITLE + " DESC";
                    Cursor cColumnNames = db.query(
                            globals.Animations.TABLE_NAME, // The table to query
                            title,                         // The columns to return
                            null,                          // The columns for the WHERE clause
                            null,                          // The values for the WHERE clause
                            null,                          // don't group the rows
                            null,                          // don't filter by row groups
                            sortOrderTitle                 // The sort order
                    );
                    cColumnNames.moveToFirst();
                    do {
                        globals.sendDebugMessage("\t" + cColumnNames.getString(globals.Animations.COLUMN_INDEX_TITLE));
                    } while (cColumnNames.moveToNext());
                }
            }
            else
            {
                globals.sendDebugMessage("cAnimations == null");
            }
        }

        for (byte i = 0; i < globals.ServoTypes.NUMOFSERVOS.ordinal(); ++i) {
            servoAnimationInPlay[i] = 0;
            sendKeyframe(i, servoAnimationInPlay[i]);
            motorPlayingAnimation[i] = true;
        }
        start = SystemClock.elapsedRealtime();
    }
}
