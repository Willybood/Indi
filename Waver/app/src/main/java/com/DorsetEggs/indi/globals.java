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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Log;

import java.io.IOException;
import java.util.Vector;

import static android.database.DatabaseUtils.queryNumEntries;

public final class globals {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public globals() {
    }

    public static final String TAG = "IndiActivity"; // TAG is used to debug in Android logcat console

    public static enum ServoTypes {DLARM, ULARM, DRARM, URARM, NUMOFSERVOS}

    private static final int MAX_SERVO_ROTATION = 180;

    //A container for incremented keyframes and timestamps. Useful for complex animations.
    public static class motorIncrementer {
        public int keyframe;
        public long lastTimeStamp;

        public motorIncrementer() {
            restart();
        }

        //Reset the log for a new animation
        public void restart() {
            keyframe = 0;
            lastTimeStamp = 0;
        }
    }

    //The different ways animations could be manipulated
    public static enum animOptions {
        CALL_RECIEVED,
        KICKSTARTER_1_1,
        KICKSTARTER_2_1,
        KICKSTARTER_2_2,
        KICKSTARTER_2_3,
        KICKSTARTER_3_1,
        NUM_OF_OPTIONS
    }

    //Values used for the menus
    public static enum animationReasons {
        ERROR,
        SET_CALL_RECIEVED_ANIMATION,
        DELETE_ANIMATION,
        NUM_OF_REASONS
    }

    public static final String EXTRA_MESSAGE = "com.DorsetEggs.indi.MESSAGE";

    // SQL related constants
    public static abstract class Animations implements BaseColumns {
        public static final String TABLE_NAME = "animations";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final int COLUMN_INDEX_TITLE = 0;
        public static final String COLUMN_NAME_KEYFRAME = "keyframe";
        public static final int COLUMN_INDEX_KEYFRAME = 1;
        public static final String COLUMN_NAME_MOTOR = "motor";
        public static final int COLUMN_INDEX_MOTOR = 2;
        public static final String COLUMN_NAME_TIME = "time";
        public static final int COLUMN_INDEX_TIME = 3;
        public static final String COLUMN_NAME_POSITION = "position";
        public static final int COLUMN_INDEX_POSITION = 4;
    }

    public static abstract class AnimToActions implements BaseColumns {
        public static final String TABLE_NAME = "animToActions";
        public static final String COLUMN_NAME_ACTION = "action";
        public static final int COLUMN_INDEX_ACTION = 0;
        public static final String COLUMN_NAME_ANIMATION = "animation";
        public static final int COLUMN_INDEX_ANIMATION = 1;
    }

    //Global database helper class, is initialised when the app is in the splash screen.
    public static SQLDBHelper dbHelper;

    //Default animations to load up into database
    public static void checkDatabaseHasEntries() {
        // Gets the data repository in write mode
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Check if any action to animation database is empty
        // todo: Forced to rewrite for now, put back on release
        db.delete(AnimToActions.TABLE_NAME, null, null);
        //if(queryNumEntries(db, AnimToActions.TABLE_NAME) == 0)
        {
            // Insert value and default for a call event
            ContentValues
                    values = new ContentValues();
            values.put(AnimToActions.COLUMN_NAME_ACTION, animOptions.CALL_RECIEVED.ordinal());
            values.put(AnimToActions.COLUMN_NAME_ANIMATION, "Two hand wave");
            // Insert the new row, returning the primary key value of the new row
            db.insert(
                    AnimToActions.TABLE_NAME,
                    null,
                    values);

            // todo: Remove this for the proper release
            // Insert value and default for a Kickstarter video
            values = new ContentValues();
            values.put(AnimToActions.COLUMN_NAME_ACTION, animOptions.KICKSTARTER_1_1.ordinal());
            values.put(AnimToActions.COLUMN_NAME_ANIMATION, "1-1");
            // Insert the new row, returning the primary key value of the new row
            db.insert(
                    AnimToActions.TABLE_NAME,
                    null,
                    values);
            // Insert value and default for a Kickstarter video
            values = new ContentValues();
            values.put(AnimToActions.COLUMN_NAME_ACTION, animOptions.KICKSTARTER_2_1.ordinal());
            values.put(AnimToActions.COLUMN_NAME_ANIMATION, "2-1");
            // Insert the new row, returning the primary key value of the new row
            db.insert(
                    AnimToActions.TABLE_NAME,
                    null,
                    values);
            // Insert value and default for a Kickstarter video
            values = new ContentValues();
            values.put(AnimToActions.COLUMN_NAME_ACTION, animOptions.KICKSTARTER_2_2.ordinal());
            values.put(AnimToActions.COLUMN_NAME_ANIMATION, "2-2");
            // Insert the new row, returning the primary key value of the new row
            db.insert(
                    AnimToActions.TABLE_NAME,
                    null,
                    values);
            // Insert value and default for a Kickstarter video
            values = new ContentValues();
            values.put(AnimToActions.COLUMN_NAME_ACTION, animOptions.KICKSTARTER_2_3.ordinal());
            values.put(AnimToActions.COLUMN_NAME_ANIMATION, "2-3");
            // Insert the new row, returning the primary key value of the new row
            db.insert(
                    AnimToActions.TABLE_NAME,
                    null,
                    values);
            // Insert value and default for a Kickstarter video
            values = new ContentValues();
            values.put(AnimToActions.COLUMN_NAME_ACTION, animOptions.KICKSTARTER_3_1.ordinal());
            values.put(AnimToActions.COLUMN_NAME_ANIMATION, "3-1");
            // Insert the new row, returning the primary key value of the new row
            db.insert(
                    AnimToActions.TABLE_NAME,
                    null,
                    values);
        }

        // Check if the animation database is empty
        // todo: Forced to rewrite for now, put back on release
        db.delete(Animations.TABLE_NAME, null, null);
        //if(queryNumEntries(db, Animations.TABLE_NAME) == 0)
        {
            // If it is, write some default entries
            // A simple two handed wave...
            for (int i = 0; i < ServoTypes.NUMOFSERVOS.ordinal(); ++i) {
                insertKeyframe(db, "Two hand wave", 0, 1000, 90, i);
                insertKeyframe(db, "Two hand wave", 1, 1000, 0, i);
            }

            // And a slightly simpler one handed wave
            for (int i = 0; i <= ServoTypes.ULARM.ordinal(); ++i) {
                insertKeyframe(db, "One hand wave", 0, 1000, 90, i);
                insertKeyframe(db, "One hand wave", 1, 1000, 0, i);
            }
        }

        setKickstarterAnimation(db);
    }

    // This is the animation for the kickstarter video.
    private static void setKickstarterAnimation(SQLiteDatabase db) {
        //Starts at 0:90 of video 1
        globals.sendDebugMessage("Setting Kickstarter animations");
        insertKeyframe(db, "1-1", 0, 1000, 60, ServoTypes.ULARM.ordinal());
        insertKeyframe(db, "1-1", 0, 1000, 60, ServoTypes.URARM.ordinal());
        insertKeyframe(db, "1-1", 0, 1000, 20, ServoTypes.DLARM.ordinal());
        insertKeyframe(db, "1-1", 0, 1000, 20, ServoTypes.DRARM.ordinal());
        insertKeyframe(db, "1-1", 1, 1000, 0, ServoTypes.ULARM.ordinal());
        insertKeyframe(db, "1-1", 1, 1000, 0, ServoTypes.URARM.ordinal());
        insertKeyframe(db, "1-1", 1, 1000, 0, ServoTypes.DLARM.ordinal());
        insertKeyframe(db, "1-1", 1, 1000, 0, ServoTypes.DRARM.ordinal());

        //Starts at 3:50 of video 2
        /*insertKeyframe(db, "2-1", 0, 1000, 10, ServoTypes.ULARM.ordinal());
        insertKeyframe(db, "2-1", 0, 1000, 0, ServoTypes.URARM.ordinal());
        insertKeyframe(db, "2-1", 0, 1000, 20, ServoTypes.DLARM.ordinal());
        insertKeyframe(db, "2-1", 0, 1000, 0, ServoTypes.DRARM.ordinal());
        insertKeyframe(db, "2-1", 1, 500, 0, ServoTypes.ULARM.ordinal());
        insertKeyframe(db, "2-1", 1, 500, 0, ServoTypes.URARM.ordinal());
        insertKeyframe(db, "2-1", 1, 500, 0, ServoTypes.DLARM.ordinal());
        insertKeyframe(db, "2-1", 1, 500, 0, ServoTypes.DRARM.ordinal());

        //Starts at 8:00 of video 2
        insertKeyframe(db, "2-2", 0, 1000, 0, ServoTypes.ULARM.ordinal());
        insertKeyframe(db, "2-2", 0, 1000, 10, ServoTypes.URARM.ordinal());
        insertKeyframe(db, "2-2", 0, 1000, 0, ServoTypes.DLARM.ordinal());
        insertKeyframe(db, "2-2", 0, 1000, 20, ServoTypes.DRARM.ordinal());
        insertKeyframe(db, "2-2", 1, 500, 0, ServoTypes.ULARM.ordinal());
        insertKeyframe(db, "2-2", 1, 500, 0, ServoTypes.URARM.ordinal());
        insertKeyframe(db, "2-2", 1, 500, 0, ServoTypes.DLARM.ordinal());
        insertKeyframe(db, "2-2", 1, 500, 0, ServoTypes.DRARM.ordinal());*/

        //Starts at 17:00 of video 2
        insertKeyframe(db, "2-3", 0, 750, 60, ServoTypes.ULARM.ordinal());
        insertKeyframe(db, "2-3", 0, 750, 60, ServoTypes.URARM.ordinal());
        insertKeyframe(db, "2-3", 0, 750, 40, ServoTypes.DLARM.ordinal());
        insertKeyframe(db, "2-3", 0, 750, 40, ServoTypes.DRARM.ordinal());
        insertKeyframe(db, "2-3", 1, 500, 20, ServoTypes.ULARM.ordinal());
        insertKeyframe(db, "2-3", 1, 500, 20, ServoTypes.URARM.ordinal());
        insertKeyframe(db, "2-3", 1, 500, 20, ServoTypes.DLARM.ordinal());
        insertKeyframe(db, "2-3", 1, 500, 20, ServoTypes.DRARM.ordinal());
        insertKeyframe(db, "2-3", 2, 500, 70, ServoTypes.ULARM.ordinal());
        insertKeyframe(db, "2-3", 2, 500, 70, ServoTypes.URARM.ordinal());
        insertKeyframe(db, "2-3", 2, 500, 40, ServoTypes.DLARM.ordinal());
        insertKeyframe(db, "2-3", 2, 500, 40, ServoTypes.DRARM.ordinal());
        insertKeyframe(db, "2-3", 3, 500, 70, ServoTypes.ULARM.ordinal());
        insertKeyframe(db, "2-3", 3, 500, 70, ServoTypes.URARM.ordinal());
        insertKeyframe(db, "2-3", 3, 500, 40, ServoTypes.DLARM.ordinal());
        insertKeyframe(db, "2-3", 3, 500, 40, ServoTypes.DRARM.ordinal());
        insertKeyframe(db, "2-3", 4, 500, 15, ServoTypes.ULARM.ordinal());
        insertKeyframe(db, "2-3", 4, 500, 15, ServoTypes.URARM.ordinal());
        insertKeyframe(db, "2-3", 4, 500, 15, ServoTypes.DLARM.ordinal());
        insertKeyframe(db, "2-3", 4, 500, 15, ServoTypes.DRARM.ordinal());
        insertKeyframe(db, "2-3", 5, 500, 65, ServoTypes.ULARM.ordinal());
        insertKeyframe(db, "2-3", 5, 500, 65, ServoTypes.URARM.ordinal());
        insertKeyframe(db, "2-3", 5, 500, 45, ServoTypes.DLARM.ordinal());
        insertKeyframe(db, "2-3", 5, 500, 45, ServoTypes.DRARM.ordinal());
        insertKeyframe(db, "2-3", 6, 750, 0, ServoTypes.ULARM.ordinal());
        insertKeyframe(db, "2-3", 6, 750, 0, ServoTypes.URARM.ordinal());
        insertKeyframe(db, "2-3", 6, 750, 0, ServoTypes.DLARM.ordinal());
        insertKeyframe(db, "2-3", 6, 750, 0, ServoTypes.DRARM.ordinal());

        //Starts at 2.25 of video 3
        insertKeyframe(db, "3-1", 0, 750, 10, ServoTypes.ULARM.ordinal());
        insertKeyframe(db, "3-1", 0, 750, 10, ServoTypes.URARM.ordinal());
        insertKeyframe(db, "3-1", 1, 750, 0, ServoTypes.ULARM.ordinal());
        insertKeyframe(db, "3-1", 1, 750, 0, ServoTypes.URARM.ordinal());
    }

    private static void insertKeyframe(SQLiteDatabase db,
                                       String name, int keyframe, int time, int position, int motor) {
        ContentValues values = new ContentValues();
        values.put(Animations.COLUMN_NAME_TITLE, name);
        values.put(Animations.COLUMN_NAME_KEYFRAME, keyframe);
        values.put(Animations.COLUMN_NAME_TIME, time);
        values.put(Animations.COLUMN_NAME_POSITION, position);
        values.put(Animations.COLUMN_NAME_MOTOR, motor);
        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(
                Animations.TABLE_NAME,
                null,
                values);
        if(-1 == newRowId)
        {
            sendDebugMessage("Error inserting keyframe for animation " + name);
        }
    }

    public static final boolean D = BuildConfig.DEBUG; // This is automatically set when building
    public static void sendDebugMessage(String text) {
        if (D) {
            Log.d(globals.TAG, text + " - " +
                               Thread.currentThread().getStackTrace()[3].toString() + " - " +
                               (System.currentTimeMillis() / 1000)); // The current time since the unix epoch in seconds
        }
    }
    public static void sendErrorMessage(String text, Throwable exception) {
        Log.e(globals.TAG, text + " - " + Thread.currentThread().getStackTrace()[3].toString(), exception);
    }
    public static void sendErrorMessage(String text) {
        Log.e(globals.TAG, text + " - " + Thread.currentThread().getStackTrace()[3].toString());
    }
}
