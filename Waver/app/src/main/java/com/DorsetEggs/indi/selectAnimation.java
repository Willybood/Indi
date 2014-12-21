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

import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Vector;

public class selectAnimation extends ListActivity {
    int reasonForCall = globals.animationReasons.ERROR.ordinal();
    SQLiteDatabase db;
    Vector<String> results = new Vector<String>();

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Intent intent = getIntent();
        reasonForCall = intent.getIntExtra(globals.EXTRA_MESSAGE, globals.animationReasons.ERROR.ordinal());

        //Access the list of animations
        db = globals.dbHelper.getWritableDatabase();
        String[] projection = {
                globals.Animations.COLUMN_NAME_TITLE
        };
        String sortOrder =
                globals.Animations.COLUMN_NAME_TITLE + " DESC";
        Cursor namesOfAnimations = db.query(
                true,                          // Whether to return distinct entries
                globals.Animations.TABLE_NAME, // The table to query
                projection,                    // The columns to return
                null,                              // The columns for the WHERE clause
                null,                              // The values for the WHERE clause
                null,                              // don't group the rows
                null,                              // don't filter by row groups
                sortOrder,                         // The sort order
                null                               // The cancellation signal

        );
        if (namesOfAnimations != null) {
            if (namesOfAnimations.moveToFirst()) {
                do {
                    results.add(namesOfAnimations.getString(namesOfAnimations.getColumnIndex(globals.Animations.COLUMN_NAME_TITLE)));
                } while (namesOfAnimations.moveToNext());
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, results.toArray(new String[results.size()]));
        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (reasonForCall == globals.animationReasons.SET_CALL_RECIEVED_ANIMATION.ordinal()) {
            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(globals.AnimToActions.COLUMN_NAME_ACTION,
                    globals.animOptions.CALL_RECIEVED.ordinal());
            values.put(globals.AnimToActions.COLUMN_NAME_ANIMATION,
                    results.elementAt(position));

            // Insert the new row, returning the primary key value of the new row
            db.insert(
                    globals.AnimToActions.TABLE_NAME,
                    null,
                    values);
        } else if (reasonForCall == globals.animationReasons.DELETE_ANIMATION.ordinal()) {
            int numDeleted = db.delete(globals.Animations.TABLE_NAME,
                    globals.Animations.COLUMN_NAME_TITLE + " = " +
                            results.elementAt(position), null);

            if (numDeleted > 0) {
                Toast.makeText(this, "Animation " + reasonForCall + " deleted",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Animation not found", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Reason " + reasonForCall + " not found",
                    Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
