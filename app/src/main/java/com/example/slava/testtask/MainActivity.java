package com.example.slava.testtask;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaExtractor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    public static final int MY_PERMISSION_REQUEST = 1;
    private static final String TAG = "tag";
    private static AudioManager audioManager;
    SeekBar seekBar;

    ArrayList<String> arrayList;
    ListView listView;
    ArrayAdapter<String> adapter;
    Button button_one;
    Button button_two;
    Button button_play;



    public MediaPlayer song0;
    public MediaPlayer song1;
    private ScheduledExecutorService scheduledExecutorService;




    private boolean needToCheck = false;
    private boolean isPlaingNow = false;
    private static int FADE;


    private MediaPlayer currentSong;
    private MediaPlayer nextSong;
    private int seekValue;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST);
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST);
            }
        }

        seekBar = findViewById(R.id.seekBar);


        button_one = findViewById(R.id.btn_song_one);
        button_one.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: 1");
                Log.d(TAG, "doStuff: ");
                listView = (ListView) findViewById(R.id.lv_songs);
                arrayList = new ArrayList<>();
                getMusic();
                adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, arrayList);
                listView.setAdapter(adapter);
                listView.setVisibility(View.VISIBLE);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        String string;
                        string = arrayList.get(position);
                        Uri uri = Uri.parse(string);
                        song1 = MediaPlayer.create(MainActivity.this, uri);
                        Log.d(TAG, "onItemClick: " + string);
                        try {
                            song1.prepare();
                            song1.seekTo(0);
                        } catch (Throwable t) {
//                            Toast.makeText(MainActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                        }

                        listView.setVisibility(View.INVISIBLE);


                    }
                });


            }
        });

        button_two = findViewById(R.id.btn_song_two);
        button_two.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "doStuff: ");

                listView.setVisibility(View.VISIBLE);
                listView = (ListView) findViewById(R.id.lv_songs);
                arrayList = new ArrayList<>();
                getMusic();
                adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, arrayList);
                listView.setAdapter(adapter);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        String string;
                        string = arrayList.get(position);
                        Uri uri = Uri.parse(string);
                        song0 = MediaPlayer.create(MainActivity.this, uri);
                        Log.d(TAG, "onItemClick: " + string);
                        try {
                            song0.prepare();
                            song0.seekTo(0);
                        } catch (Throwable t) {
//                            Toast.makeText(MainActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                        }

                        listView.setVisibility(View.INVISIBLE);
                    }
                });
            }
        });
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        button_play = findViewById(R.id.btn_play);
        button_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seekValue = seekBar.getProgress();
                FADE = (seekValue + 2) *1000;
                Log.d(TAG, "Fade: "+ FADE);
                if ((song1.getDuration()<FADE)||(song0.getDuration()<FADE) ){
                    Toast.makeText(MainActivity.this,"Аудиофалы короче кроссфейда",Toast.LENGTH_SHORT).show();
                }
                else
                {
                    try {
                        if (!isPlaingNow) {
                            currentSong = song1;
                            button_play.setText("STOP");
                            needToCheck = true;
                            isPlaingNow = true;
                            Log.d(TAG, "onClick: play");
                            startPlay();
                        } else {
                            button_play.setText("PLAY");
                            Log.d(TAG, "onClick: stop");
                            isPlaingNow = false;
                            needToCheck = false;
                            currentSong.stop();
                        }
                    } catch (Exception ex) {
                        Toast.makeText(MainActivity.this, "Выберите песни" + ex, Toast.LENGTH_SHORT).show();
                    }}


            }
        });
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

    }


    private void startPlay() {
        currentSong.start();
        checkForNext();
    }

    private void checkForNext() {
        Thread checkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (needToCheck) {
                    if ((currentSong.getDuration() - currentSong.getCurrentPosition()) <= FADE && currentSong.isPlaying()) { //  затухание началось
                        Log.d(TAG, "run: Началось затухание");
                        needToCheck = false;
                        pause(currentSong, FADE);

                        startNextSong();
                    }
                }
            }
        });
        Log.d(TAG, "checkForNext: вышли");
        checkThread.start();
    }

    private void startNextSong() {
        if (currentSong == song1) {
            nextSong = currentSong;
            currentSong = song0;
            Log.d(TAG, "startNextSong: song0");
        } else {
            nextSong = currentSong;
            currentSong = song1;
            Log.d(TAG, "startNextSong: song1");
        }
        play(currentSong,FADE);
        needToCheck = true;
    }



    public void getMusic() {

        ContentResolver contentResolver = getContentResolver();
        Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor songCursor = contentResolver.query(songUri, null, null, null, null);
        if (songCursor != null && songCursor.moveToFirst()) {
//            int songTitle = songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
//            int songArtist = songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int songDirectory = songCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            Log.d(TAG, "getMusic: ");
            do {
//                String currentTitle = songCursor.getString(songTitle);
//                String currentArtist = songCursor.getString(songArtist);
                String currentDirectory = songCursor.getString(songDirectory);
                arrayList.add(currentDirectory);
                Log.d(TAG, "getMusic: " + arrayList.toString());
            } while (songCursor.moveToNext());
        }
    }

    //
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Предоставте доступ к файлам", Toast.LENGTH_SHORT).show();
                    finish();
                }
                return;
            }

        }
    }



    private int iVolume;
    private int gVolume;

    private final static int INT_VOLUME_MAX = 100;
    private final static int INT_VOLUME_MIN = 0;
    private final static float FLOAT_VOLUME_MAX = 1;
    private final static float FLOAT_VOLUME_MIN = 0;




    public void play(final MediaPlayer mediaPlayer, int fadeDuration) {

        // Set current volume, depending on fade or not
        if (fadeDuration > 0)
            gVolume = INT_VOLUME_MIN;
        else
            gVolume = INT_VOLUME_MAX;

        updateVolume(mediaPlayer, 0);

        // Play music
        if (!mediaPlayer.isPlaying())
            mediaPlayer.start();

        // Start increasing volume in increments
        if (fadeDuration > 0) {
            final Timer timer = new Timer(true);
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    updateVolume1(mediaPlayer, 1);
                    if (gVolume == INT_VOLUME_MAX) {
                        timer.cancel();
                        timer.purge();
                    }
                }
            };

            // calculate delay, cannot be zero, set to 1 if zero
            int delay = fadeDuration / INT_VOLUME_MAX;
            if (delay == 0)
                delay = 1;

            timer.schedule(timerTask, delay, delay);
        }
    }

    public void pause(final MediaPlayer mediaPlayer, int fadeDuration) {
        Log.d(TAG, "pause: pause");
        // Set current volume, depending on fade or not
        if (fadeDuration > 0)
            iVolume = INT_VOLUME_MAX;
        else
            iVolume = INT_VOLUME_MIN;

        updateVolume(mediaPlayer, 0);

        // Start increasing volume in increments
        if (fadeDuration > 0) {
            final Timer timer = new Timer(true);
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    updateVolume(mediaPlayer, -1);
                    if (iVolume == INT_VOLUME_MIN) {

                        Log.d(TAG, "run: Затухание закончилось");

                        timer.cancel();
                        timer.purge();
                    }
                }
            };

            // calculate delay, cannot be zero, set to 1 if zero
            int delay = fadeDuration / INT_VOLUME_MAX;
            if (delay == 0)
                delay = 1;

            timer.schedule(timerTask, delay, delay);
        }
    }

    private void updateVolume(MediaPlayer mediaPlayer, int change) {
        // increment or decrement depending on type of fade
        iVolume = iVolume + change;

        // ensure iVolume within boundaries
        if (iVolume < INT_VOLUME_MIN)
            iVolume = INT_VOLUME_MIN;
        else if (iVolume > INT_VOLUME_MAX)
            iVolume = INT_VOLUME_MAX;

        // convert to float value
        float fVolume = 1 - ((float) Math.log(INT_VOLUME_MAX - iVolume) / (float) Math.log(INT_VOLUME_MAX));

        // ensure fVolume within boundaries
        if (fVolume < FLOAT_VOLUME_MIN)
            fVolume = FLOAT_VOLUME_MIN;
        else if (fVolume > FLOAT_VOLUME_MAX)
            fVolume = FLOAT_VOLUME_MAX;

        mediaPlayer.setVolume(fVolume, fVolume);
    }
    private void updateVolume1(MediaPlayer mediaPlayer, int change) {
        // increment or decrement depending on type of fade
        gVolume = gVolume + change;

        // ensure iVolume within boundaries
        if (gVolume < INT_VOLUME_MIN)
            gVolume = INT_VOLUME_MIN;
        else if (gVolume > INT_VOLUME_MAX)
            gVolume = INT_VOLUME_MAX;

        // convert to float value
        float cVolume = 1 - ((float) Math.log(INT_VOLUME_MAX - gVolume) / (float) Math.log(INT_VOLUME_MAX));

        // ensure fVolume within boundaries
        if (cVolume < FLOAT_VOLUME_MIN)
            cVolume = FLOAT_VOLUME_MIN;
        else if (cVolume > FLOAT_VOLUME_MAX)
            cVolume = FLOAT_VOLUME_MAX;

        mediaPlayer.setVolume(cVolume, cVolume);
    }


}
