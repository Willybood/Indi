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

import static android.database.DatabaseUtils.queryNumEntries;

public final class globals {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public globals() {}

    //The different ways animations could be manipulated
    public static enum animOptions {
        CALL_RECIEVED,
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
    public static void checkDatabaseHasEntries()
    {
        // Gets the data repository in write mode
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Check if the action to animation database is empty
        if(queryNumEntries(db, AnimToActions.TABLE_NAME) == 0)
        {
            ContentValues values = new ContentValues();
            values.put(AnimToActions.COLUMN_NAME_ACTION, animOptions.CALL_RECIEVED.ordinal());
            values.put(AnimToActions.COLUMN_NAME_ANIMATION, "Two hand wave");
            // Insert the new row, returning the primary key value of the new row
            db.insert(
                    AnimToActions.TABLE_NAME,
                    null,
                    values);
        }

        // Check if the animation database is empty
        if(queryNumEntries(db, Animations.TABLE_NAME) == 0) {
            // If it is, write some default entries
            // A simple two handed wave...
            for (int i = 0; i < communicatorService.ServoTypes.NUMOFSERVOS.ordinal(); ++i) {
                ContentValues values = new ContentValues();
                values.put(Animations.COLUMN_NAME_TITLE, "Two hand wave");
                values.put(Animations.COLUMN_NAME_KEYFRAME, 0);
                values.put(Animations.COLUMN_NAME_TIME, 1000);
                values.put(Animations.COLUMN_NAME_POSITION, 90);
                values.put(Animations.COLUMN_NAME_MOTOR, i);
                // Insert the new row, returning the primary key value of the new row
                db.insert(
                        Animations.TABLE_NAME,
                        null,
                        values);
                values.put(Animations.COLUMN_NAME_KEYFRAME, 1);
                values.put(Animations.COLUMN_NAME_TIME, 1000);
                values.put(Animations.COLUMN_NAME_POSITION, 0);
                values.put(Animations.COLUMN_NAME_MOTOR, i);
                // Insert the new row, returning the primary key value of the new row
                db.insert(
                        Animations.TABLE_NAME,
                        null,
                        values);
            }

            // And a slightly simpler one handed wave
            for (int i = 0; i <= communicatorService.ServoTypes.ULARM.ordinal(); ++i) {
                ContentValues values = new ContentValues();
                values.put(Animations.COLUMN_NAME_TITLE, "One hand wave");
                values.put(Animations.COLUMN_NAME_KEYFRAME, 0);
                values.put(Animations.COLUMN_NAME_TIME, 1000);
                values.put(Animations.COLUMN_NAME_POSITION, 90);
                values.put(Animations.COLUMN_NAME_MOTOR, i);
                // Insert the new row, returning the primary key value of the new row
                db.insert(
                        Animations.TABLE_NAME,
                        null,
                        values);
                values.put(Animations.COLUMN_NAME_KEYFRAME, 1);
                values.put(Animations.COLUMN_NAME_TIME, 1000);
                values.put(Animations.COLUMN_NAME_POSITION, 0);
                values.put(Animations.COLUMN_NAME_MOTOR, i);
                // Insert the new row, returning the primary key value of the new row
                db.insert(
                        Animations.TABLE_NAME,
                        null,
                        values);
            }
            for (int i = communicatorService.ServoTypes.DRARM.ordinal(); i <= communicatorService.ServoTypes.NUMOFSERVOS.ordinal(); ++i) {
                ContentValues values = new ContentValues();
                values.put(Animations.COLUMN_NAME_TITLE, "One hand wave");
                values.put(Animations.COLUMN_NAME_KEYFRAME, 2);
                values.put(Animations.COLUMN_NAME_TIME, 1000);
                values.put(Animations.COLUMN_NAME_POSITION, 0);
                values.put(Animations.COLUMN_NAME_MOTOR, i);
                // Insert the new row, returning the primary key value of the new row
                db.insert(
                        Animations.TABLE_NAME,
                        null,
                        values);
                values.put(Animations.COLUMN_NAME_KEYFRAME, 3);
                values.put(Animations.COLUMN_NAME_TIME, 1000);
                values.put(Animations.COLUMN_NAME_POSITION, 0);
                values.put(Animations.COLUMN_NAME_MOTOR, i);
                // Insert the new row, returning the primary key value of the new row
                db.insert(
                        Animations.TABLE_NAME,
                        null,
                        values);
            }
        }
    }
}
