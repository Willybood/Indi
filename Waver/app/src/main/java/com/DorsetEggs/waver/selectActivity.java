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
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class selectActivity extends ListActivity {
    final String [] setAnimValues = new String[] {
            "Call recieved"
    };

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, setAnimValues);
        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if(globals.animOptions.CALL_RECIEVED.ordinal() == position)
        {
            Intent intent = new Intent(this, selectAnimation.class);
            intent.putExtra(globals.EXTRA_MESSAGE, globals.animationReasons.SET_CALL_RECIEVED_ANIMATION.ordinal());
            startActivity(intent);
        }
        else
        {
            String item = (String) getListAdapter().getItem(position);
            Toast.makeText(this, item + " not found", Toast.LENGTH_LONG).show();
        }
    }
}
