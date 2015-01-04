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
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.VideoView;


public class filmPlayer extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        setContentView(R.layout.activity_film_player);

        final VideoView videoView =
                (VideoView) findViewById(R.id.videoView);

        Uri kickstarterVideo =
                /*
                // video 1
                Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.face_video_part_one);
                */
                // video 2
                Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.face_video_part_two);
                /*
                // video 3
                Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.face_video_part_three);
                */
        videoView.setVideoURI(kickstarterVideo);

        Thread thread = new Thread() {
            int currentAnimation = 0;
            @Override
            public void run() {
                try {
                    while(true) {
                        sleep(250); // Every 1/4 a second
                        // getCurrentPosition returns the current time in ms, its being used to return the correct animation for each position
                        /*
                        // video 1
                        if ((videoView.getCurrentPosition() > 1000) && (0 == currentAnimation)) {
                            sendMessage(globals.animOptions.KICKSTARTER_1_1.ordinal());
                            currentAnimation++;
                        }
                        */
                        // video 2
                        if((videoView.getCurrentPosition() > 0) && (0 == currentAnimation)) {
                            sendMessage(globals.animOptions.KICKSTARTER_2_1.ordinal());
                            currentAnimation++;
                        }
                        if((videoView.getCurrentPosition() > 3500) && (1 == currentAnimation)) {
                            sendMessage(globals.animOptions.KICKSTARTER_2_2.ordinal());
                            currentAnimation++;
                        }
                        if((videoView.getCurrentPosition() > 8000) && (2 == currentAnimation)) {
                            sendMessage(globals.animOptions.KICKSTARTER_2_3.ordinal());
                            currentAnimation++;
                        }
                        if((videoView.getCurrentPosition() > 17000) && (3 == currentAnimation)) {
                            sendMessage(globals.animOptions.KICKSTARTER_2_4.ordinal());
                            currentAnimation++;
                        }
                        /*
                        // video 3
                        if((videoView.getCurrentPosition() > 2250) && (0 == currentAnimation)) {
                            sendMessage(globals.animOptions.KICKSTARTER_3_1.ordinal());
                            currentAnimation++;
                        }
                        */
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer vmp) {
                globals.sendDebugMessage("Ending film");
            }
        });

        videoView.start();
        globals.sendDebugMessage("Starting film");
    }

    private void sendMessage(int animation) {
        Intent intent = new Intent("start-kickstarter-anim");
        intent.putExtra("message", animation);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
