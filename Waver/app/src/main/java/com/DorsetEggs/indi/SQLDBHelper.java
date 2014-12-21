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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLDBHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Indi.db";

    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ANIMATION_ENTRIES =
            "CREATE TABLE " + globals.Animations.TABLE_NAME + " (" +
                    globals.Animations.COLUMN_NAME_TITLE + TEXT_TYPE + COMMA_SEP +
                    globals.Animations.COLUMN_NAME_KEYFRAME + INTEGER_TYPE + COMMA_SEP +
                    globals.Animations.COLUMN_NAME_MOTOR + INTEGER_TYPE + COMMA_SEP +
                    globals.Animations.COLUMN_NAME_TIME + INTEGER_TYPE + COMMA_SEP +
                    globals.Animations.COLUMN_NAME_POSITION + INTEGER_TYPE +
                    " )";
    private static final String SQL_CREATE_ANIMTOACTIONS_ENTRIES =
            "CREATE TABLE " + globals.AnimToActions.TABLE_NAME + " (" +
                    globals.AnimToActions.COLUMN_NAME_ACTION + INTEGER_TYPE + COMMA_SEP +
                    globals.AnimToActions.COLUMN_NAME_ANIMATION + TEXT_TYPE +
                    " )";

    private static final String SQL_ANIMATION_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + globals.Animations.TABLE_NAME;
    private static final String SQL_ANIMTOACTIONS_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + globals.AnimToActions.TABLE_NAME;

    public SQLDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ANIMATION_ENTRIES);
        db.execSQL(SQL_CREATE_ANIMTOACTIONS_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_ANIMATION_DELETE_ENTRIES);
        db.execSQL(SQL_ANIMTOACTIONS_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}