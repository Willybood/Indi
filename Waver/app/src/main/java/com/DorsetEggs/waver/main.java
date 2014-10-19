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

import android.os.Bundle;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;

import java.util.Timer;
import java.util.TimerTask;

public class main extends ActionBarActivity {

    //TextView connectionStatus;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Intent intent = new Intent(this, communicatorService.class);
        this.startService(intent);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        //Show splash screen for 3 seconds
        Timer t = new Timer();
        t.schedule(new TimerTask() {

            @Override
            public void run() {
                finish();
            }
        }, 3000);
    }
}
