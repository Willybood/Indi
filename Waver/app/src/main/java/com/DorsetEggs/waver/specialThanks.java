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
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class specialThanks extends ListActivity {
    private class Backer {
        public String name;
        public String plug;
        public String link;

        public Backer(String nameInput, String plugInput, String linkInput) {
            name = nameInput;
            plug = plugInput;
            link = linkInput;
        }
    }

    private ArrayList<Backer> backers;

    private ArrayList<Backer> setUpBackerList() {
        ArrayList<Backer> backerList = new ArrayList<Backer>();

        backerList.add(new Backer("Moll Teaser", "@Malty", "https://twitter.com/Malty"));
        backerList.add(new Backer("Toby LaRone", "AddictiveTriangle.com", "http://www.AddictiveTriangle.com"));
        backerList.add(new Backer("Noah Zark", "facebook.com/noahzark", "https://www.facebook.com/noahzark"));
        backerList.add(new Backer("Faye Dinaway", "transparentShirts.co.uk", "http://www.transparentShirts.co.uk"));
        backerList.add(new Backer("Klaus Shave", "@CloserStill", "https://twitter.com/CloserStill"));
        backerList.add(new Backer("Jack Pott", "The gambling machine for your home - Kickstarter", "https://www.kickstarter.com/projects/hop/gamblor"));
        backerList.add(new Backer("Horace Cope", "@YourFutureBaby", "https://twitter.com/YourFutureBaby"));
        backerList.add(new Backer("Rhoda Hoarse", "facebook.com/rhodahoarse", "https://www.facebook.com/rhodahoarse"));
        backerList.add(new Backer("Rhoda Hoarse", "facebook.com/rhodahoarse", "https://www.facebook.com/rhodahoarse"));
        backerList.add(new Backer("Carley Wurly", "@TheMarathon", "https://twitter.com/TheMarathon"));
        backerList.add(new Backer("Russell Sprout", "DisappointingChristmas.com", "www.DisappointingChristmas.com"));
        backerList.add(new Backer("Laura Wood", "@worldsofnote", "https://twitter.com/worldsofnote"));

        return backerList;
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        backers = setUpBackerList();
        ArrayAdapter adapter = new ArrayAdapter(
                this, android.R.layout.simple_list_item_2, android.R.id.text1, backers) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                text1.setText(backers.get(position).name);
                text2.setText(backers.get(position).plug);
                return view;
            }
        };

        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(backers.get(position).link));
        startActivity(browserIntent);
    }
}
