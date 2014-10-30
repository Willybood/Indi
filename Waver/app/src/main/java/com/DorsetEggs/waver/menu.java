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

public class menu extends ListActivity {

    private enum menuOptions {
        SET_ANIMATIONS,
        SPECIAL_THANKS,
        CREATE_ANIMATION,
        DELETE_ANIMATIONS,
        NUM_OF_OPTIONS
    }
    final String [] menuValues = new String[] {
            // todo: Put this back when we have a proper editor for animations
            "Set animations",
            "Special thanks"
            //"Create animation",
            //"Delete animations"
    };

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, menuValues);
        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if(menuOptions.CREATE_ANIMATION.ordinal() == position)
        {
            Intent intent = new Intent(this, animationEditor.class);
            startActivity(intent);
        }
        else if(menuOptions.SPECIAL_THANKS.ordinal() == position)
        {
            Intent intent = new Intent(this, specialThanks.class);
            startActivity(intent);
        }
        else if(menuOptions.SET_ANIMATIONS.ordinal() == position)
        {
            Intent intent = new Intent(this, selectActivity.class);
            startActivity(intent);
        }
        else if(menuOptions.DELETE_ANIMATIONS.ordinal() == position)
        {
            Intent intent = new Intent(this, selectAnimation.class);
            startActivity(intent);
            intent.putExtra(globals.EXTRA_MESSAGE, globals.animationReasons.DELETE_ANIMATION.ordinal());
        }
        else
        {
            String item = (String) getListAdapter().getItem(position);
            Toast.makeText(this, item + " not found", Toast.LENGTH_LONG).show();
        }
    }

    //If back is hit on the main menu, quit the app
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
