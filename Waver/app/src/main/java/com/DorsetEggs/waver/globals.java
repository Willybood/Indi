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

package com.DorsetEggs.waver;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import java.util.Vector;

import static android.database.DatabaseUtils.queryNumEntries;

public final class globals {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public globals() {
    }

    public static enum ServoTypes {DLARM, ULARM, DRARM, URARM, NUMOFSERVOS}

    private static final int MAX_SERVO_ROTATION = 180;

    //A container for incremented keyframes and timestamps. Useful for complex animations.
    private static class motorIncrementer {
        public int keyframe;
        public int lastTimeStamp;

        public motorIncrementer() {
            restart();
        }

        //Reset the log for a new animation
        public void restart() {
            keyframe = 0;
            lastTimeStamp = 0;
        }
    }

    private static Vector<motorIncrementer> kickstarterAnimations = new Vector<motorIncrementer>();

    //The different ways animations could be manipulated
    public static enum animOptions {
        CALL_RECIEVED,
        KICKSTARTER_VIDEO,
        NUM_OF_OPTIONS
    }

    //Values used for the menus
    public static enum animationReasons {
        ERROR,
        SET_CALL_RECIEVED_ANIMATION,
        DELETE_ANIMATION,
        NUM_OF_REASONS
    }

    public static final String EXTRA_MESSAGE = "com.DorsetEggs.waver.MESSAGE";

    // SQL related constants
    public static abstract class Animations implements BaseColumns {
        public static final String TABLE_NAME = "animations";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_KEYFRAME = "keyframe";
        public static final String COLUMN_NAME_MOTOR = "motor";
        public static final String COLUMN_NAME_TIME = "time";
        public static final String COLUMN_NAME_POSITION = "position";
    }

    public static abstract class AnimToActions implements BaseColumns {
        public static final String TABLE_NAME = "animToActions";
        public static final String COLUMN_NAME_ACTION = "action";
        public static final String COLUMN_NAME_ANIMATION = "animation";
    }

    //Global database helper class, is initialised when the app is in the splash screen.
    public static SQLDBHelper dbHelper;

    //Default animations to load up into database
    public static void checkDatabaseHasEntries() {
        // Gets the data repository in write mode
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Check if any action to animation database is empty
        // todo: Forced to rewrite for now, put back on release
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

            // Insert value and default for the Kickstarter video
            values = new ContentValues();
            values.put(AnimToActions.COLUMN_NAME_ACTION, animOptions.KICKSTARTER_VIDEO.ordinal());
            values.put(AnimToActions.COLUMN_NAME_ANIMATION, "Kickstarter");
            // Insert the new row, returning the primary key value of the new row
            db.insert(
                    AnimToActions.TABLE_NAME,
                    null,
                    values);
        }

        // Check if the animation database is empty
        // todo: Forced to rewrite for now, put back on release
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
        // kickstarterAnimations[] is coupled with 'addKickstarterFrame'. Not great, but hey ho.
        // It's only temp code anyway...
        kickstarterAnimations.clear();
        for (int i = 0; i < ServoTypes.NUMOFSERVOS.ordinal(); ++i) {
            kickstarterAnimations.add(new motorIncrementer());
        }
        addKickstarterFrame(db, 730, 30, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 730, 5, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 900, 5, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 900, 5, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 900, 80, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 900, 80, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 1192, 20, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 1192, 0, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 1192, 20, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 1192, 0, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 1307, 45, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 1307, 30, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 1307, 45, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 1307, 30, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 1725, 45, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 1725, 30, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 1725, 45, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 1725, 30, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 1932, 5, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 1932, 0, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 1932, 5, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 1932, 0, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 3307, 5, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 3307, 0, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 3307, 5, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 3307, 0, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 3750, 30, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 3750, 5, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 3750, 5, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 3750, 0, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 4025, 30, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 4025, 5, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 4025, 5, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 4025, 0, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 4500, 35, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 4500, 30, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 4500, 35, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 4500, 30, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 5000, 10, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 5000, 10, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 5000, 10, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 5000, 10, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 5500, 5, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 5500, 0, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 5500, 5, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 5500, 0, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 6750, 5, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 6750, 0, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 6750, 5, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 6750, 0, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 6692, 0, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 6692, 0, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 6692, 0, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 6692, 0, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 7150, 10, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 7150, 10, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 7150, 10, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 7150, 10, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 7292, 10, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 7292, 10, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 7292, 10, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 7292, 10, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 7350, 0, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 7350, 0, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 7350, 0, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 7350, 0, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 7575, 0, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 7575, 0, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 7575, 0, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 7575, 0, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 7625, 10, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 7625, 0, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 7625, 10, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 7625, 0, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 7875, 2, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 7875, 1, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 7875, 2, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 7875, 1, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 7925, 30, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 7925, 90, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 7925, 30, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 7925, 90, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 8125, 0, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 8125, 0, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 8125, 0, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 8125, 0, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 8325, 15, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 8325, 5, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 8325, 15, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 8325, 5, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 8682, 5, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 8682, 0, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 8682, 5, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 8682, 0, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 8917, 30, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 8917, 15, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 8917, 30, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 8917, 15, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 9125, 5, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 9125, 0, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 9125, 5, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 9125, 0, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 9532, 10, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 9532, 0, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 9532, 25, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 9532, 15, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 9925, 25, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 9925, 60, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 10107, 10, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 10107, 0, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 10107, 10, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 10107, 0, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 10300, 15, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 10300, 0, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 10300, 15, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 10300, 0, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 10600, 10, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 10600, 0, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 10600, 0, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 10600, 0, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 10792, 20, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 10792, 20, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 10792, 20, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 10792, 20, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 10942, 10, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 10942, 0, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 10942, 10, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 10942, 0, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 11075, 10, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 11075, 0, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 11075, 10, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 11075, 0, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 10942, 10, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 10942, 0, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 10942, 10, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 10942, 0, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 11250, 15, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 11250, 5, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 11250, 15, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 11250, 5, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 11392, 65, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 11392, 40, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 11392, 65, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 11392, 40, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 11492, 40, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 11492, 5, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 11492, 40, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 11492, 5, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 11567, 70, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 11567, 50, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 11567, 70, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 11567, 50, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 11650, 40, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 11650, 5, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 11650, 40, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 11650, 5, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 11742, 65, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 11742, 45, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 11742, 65, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 11742, 45, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 11817, 40, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 11817, 5, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 11817, 40, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 11817, 5, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 11900, 70, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 11900, 55, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 11900, 70, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 11900, 55, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 12050, 10, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 12050, 0, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 12050, 10, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 12050, 0, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 12875, 10, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 12875, 0, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 12875, 10, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 12875, 0, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 13100, 15, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 13100, 15, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 13100, 5, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 13100, 0, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 13400, 20, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 13400, 5, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 13400, 20, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 13400, 5, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 13617, 25, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 13617, 5, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 13617, 25, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 13617, 5, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 13975, 25, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 13975, 5, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 13975, 25, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 13975, 5, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 14207, 25, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 14207, 0, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 14207, 25, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 14207, 0, ServoTypes.DRARM.ordinal());

        addKickstarterFrame(db, 15742, 25, ServoTypes.ULARM.ordinal());
        addKickstarterFrame(db, 15742, 0, ServoTypes.DLARM.ordinal());
        addKickstarterFrame(db, 15742, 25, ServoTypes.URARM.ordinal());
        addKickstarterFrame(db, 15742, 0, ServoTypes.DRARM.ordinal());
    }

    private static void addKickstarterFrame(SQLiteDatabase db, int timeStamp, int position, int motor) {
        final String name = "Kickstarter";
        motorIncrementer incrementer = kickstarterAnimations.get(motor);
        insertKeyframe(db,
                name, incrementer.keyframe, timeStamp - incrementer.lastTimeStamp, position, motor);
        incrementer.keyframe = incrementer.keyframe + 1;
        incrementer.lastTimeStamp = timeStamp;
        kickstarterAnimations.set(motor, incrementer);
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
        db.insert(
                Animations.TABLE_NAME,
                null,
                values);
    }
}
