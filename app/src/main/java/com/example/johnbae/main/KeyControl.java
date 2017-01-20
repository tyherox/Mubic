package com.example.johnbae.main;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;
import android.os.Handler;


public class KeyControl extends AppCompatActivity {

    Button btnDis;
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    KeyController player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS); //receive the address of the bluetooth device

        //view of the KeyControl
        setContentView(R.layout.activity_key_control);

        //call the widgtes
        btnDis = (Button)findViewById(R.id.button4);

        //new ConnectBT().execute(); //Call the class to connect
        btnDis.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Disconnect(); //close connection
            }
        });

        Button btnPlayC = (Button)findViewById(R.id.button);
        btnPlayC.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                player.play("c");
            }
        });

        Button btnPlayA = (Button)findViewById(R.id.button2);
        btnPlayA.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                player.play("a");
            }
        });

        Button btnPlayBack = (Button)findViewById(R.id.button5);
        btnPlayBack.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                player.playBack();
            }
        });

        Button btnRecord = (Button)findViewById(R.id.button3);
        btnRecord.setOnClickListener(new View.OnClickListener()
        {
            boolean truthy = false;
            @Override
            public void onClick(View v)
            {
                player.setRecord(!truthy);
            }
        });

        Button octUp = (Button)findViewById(R.id.button6);
        octUp.setOnClickListener(new View.OnClickListener()
        {
            boolean truthy = false;
            @Override
            public void onClick(View v)
            {
                player.setOctave(1);
            }
        });

        Button octDown = (Button)findViewById(R.id.button7);
        octDown.setOnClickListener(new View.OnClickListener()
        {
            boolean truthy = false;
            @Override
            public void onClick(View v)
            {
                player.setOctave(-1);
            }
        });

        player = new KeyController();
    }

    private void Disconnect() {
        if (btSocket!=null) //If the btSocket is busy
        {
            try
            {
                btSocket.close(); //close connection
            }
            catch (IOException e)
            { msg("Error");}
        }
        finish(); //return to the first layout

    }

    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

    public void listen(){
        final Handler handler = new Handler();
        Thread workerThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() ) {
                    try {
                        final InputStream inputStream = btSocket.getInputStream();
                        int bytesAvailable = inputStream.available();

                        if(bytesAvailable!=0){
                            handler.post(new Runnable()
                            {
                                public void run()
                                {
                                    try {
                                        byte[] buffer = new byte[256];
                                        int bytes = inputStream.read(buffer);
                                        String key = new String(buffer, 0, bytes);

                                        new Thread(new Runnable() {
                                            public void run() {
                                                player.play("c");
                                            }
                                        }).start();

                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }


                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        workerThread.start();
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_led_control, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(KeyControl.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                 myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                 BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                 btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                 BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                 btSocket.connect();//start connection
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            }
            else
            {
                msg("Connected.");
                isBtConnected = true;
                listen();
                msg("Let's start Listening.");
            }
            progress.dismiss();
        }
    }

    public class KeyController {

        ArrayList<Note> notes = new ArrayList<>();
        ArrayList<String> recording = new ArrayList<>();
        int Octave = 0;

        boolean record = false;

        public void setOctave(int i){
            if(Octave + i >= 0 && Octave + i < 3) Octave += i;
        }

        public KeyController(){
            notes.add(new Note(R.raw.note0));
            notes.add(new Note(R.raw.note1));
            notes.add(new Note(R.raw.note2));
            notes.add(new Note(R.raw.note3));
            notes.add(new Note(R.raw.note4));
            notes.add(new Note(R.raw.note5));
            notes.add(new Note(R.raw.note6));
            notes.add(new Note(R.raw.note7));
            notes.add(new Note(R.raw.note8));
            notes.add(new Note(R.raw.note9));
            notes.add(new Note(R.raw.note10));
            notes.add(new Note(R.raw.note11));
            notes.add(new Note(R.raw.note12));
            notes.add(new Note(R.raw.note13));
            notes.add(new Note(R.raw.note14));
            notes.add(new Note(R.raw.note15));
            notes.add(new Note(R.raw.note16));
            notes.add(new Note(R.raw.note17));
            notes.add(new Note(R.raw.note18));
            notes.add(new Note(R.raw.note19));
            notes.add(new Note(R.raw.note20));
            notes.add(new Note(R.raw.note21));
            notes.add(new Note(R.raw.note22));
            notes.add(new Note(R.raw.note23));
            notes.add(new Note(R.raw.note24));
            notes.add(new Note(R.raw.note25));
            notes.add(new Note(R.raw.note26));
            notes.add(new Note(R.raw.note27));
            notes.add(new Note(R.raw.note28));
            notes.add(new Note(R.raw.note29));
            notes.add(new Note(R.raw.note30));
            notes.add(new Note(R.raw.note31));
            notes.add(new Note(R.raw.note32));
            notes.add(new Note(R.raw.note33));
            notes.add(new Note(R.raw.note34));
            notes.add(new Note(R.raw.note35));
            notes.add(new Note(R.raw.note35));
        }

        public void playBack(){
            setRecord(false);

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    for(int i = 0; i<recording.size(); i++){
                        final int index = i;
                        final String[] parts = recording.get(index).split("&");
                        final double rest = index!= 0 ? Double.parseDouble(parts[1]) - Double.parseDouble(recording.get(index-1).split("&")[1]) : 0;

                        try {
                            Thread.sleep((long)rest);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        play(parts[0]);
                    }
                }
            }, 100);
        }

        public void play(String key) {

            Log.d("!", "near recording:" + record);
            if(record){
                recording.add(key + "&" + System.currentTimeMillis());
                Log.d("!", "RECORDED KEY");
            }

            int base = Octave * 12;

            switch(key){
                case "c":  notes.get(0+base).play();
                    break;
                case "h":  notes.get(1+base).play();
                    break;
                case "d":  notes.get(2+base).play();
                    break;
                case "i":  notes.get(3+base).play();
                    break;
                case "e":  notes.get(4+base).play();
                    break;
                case "f":  notes.get(5+base).play();
                    break;
                case "j":  notes.get(6+base).play();
                    break;
                case "g":  notes.get(7+base).play();
                    break;
                case "k":  notes.get(8+base).play();
                    break;
                case "a":  notes.get(9+base).play();
                    break;
                case "l":  notes.get(10+base).play();
                    break;
                case "b":  notes.get(11+base).play();
                    break;
                case "C":  notes.get(12+base).play();
                    break;

            }

        }

        public void setRecord(boolean r) {
            record = r;
            Log.d("!", "Set Record:" + r);
            if(record) recording.clear();
        }

        public class Note{

            int Key;
            boolean playing = false;
            private MediaPlayer sound;

            public Note(int key){
                Key = key;
                sound = MediaPlayer.create(KeyControl.this, Key);
                sound.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        playing = false;
                    }
                });
                sound.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        playing = true;
                    }
                });
            }

            public void play(){
                sound.stop();
                try {
                    sound.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                sound.start();
            }

        }
    }
}
