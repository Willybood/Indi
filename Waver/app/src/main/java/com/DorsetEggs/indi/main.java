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

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.content.Intent;
import android.os.Handler;

public class main extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Reset the services and start the SQL service
        //Intent SQLIntent = new Intent(this, SQLService.class);
        //SQLIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Clears all threads and starts this one
        //this.startService(SQLIntent);
        globals.dbHelper = new SQLDBHelper(this.getBaseContext());
        globals.checkDatabaseHasEntries();

        // Start the communicator service
        Intent commIntent = new Intent(this, communicatorService.class);
        this.startService(commIntent);
    }

    @Override
    public void onStart() {
        super.onStart();

        //Show splash screen for 3 seconds
        Handler handler = new Handler();
        handler.postDelayed(runnable, 3000);
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            // Put back for the non-kickstarter version
            //checkUSB();
            globals.sendDebugMessage("Launching video player");
            videoGo();
        }
    };

    private void videoGo()
    {
        Intent filmPlayerIntent = new Intent(this, filmPlayer.class);
        startActivity(filmPlayerIntent);
        // The kickstarter animation will start when the confirmation is recieved from
        // filmPlayer
    }

    private void checkUSB() {
        // If connected, end the app
        // Otherwise, goto the menu
        if (!isMyServiceRunning(communicatorService.class)) {
            Intent intent = new Intent(this, menu.class);
            startActivity(intent);
        }
        finish();
    }

    // Taken from 'http://stackoverflow.com/questions/600207/how-to-check-if-a-service-is-running-in-android'
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.equals(service.service)) {
                return true;
            }
        }
        return false;
    }
}
