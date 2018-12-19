package projects.pers.sbp.ardcon;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

public class Ardcon extends AppCompatActivity {

    public final static String MODULE_MAC = "98:D3:34:90:6F:A1";
    public final static int REQUEST_ENABLE_BT = 1;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    BluetoothAdapter bta;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    ConnectedThread btt = null;
    Button switchLight, switchRelay;
    TextView response;
    boolean lightflag = false;
    boolean relayFlag = true;
    public Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ardcon);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Log.i("[BLUETOOTH]", "Creating listeners");
        response = (TextView) findViewById(R.id.response);
        switchRelay = (Button) findViewById(R.id.relay);
        switchLight = (Button) findViewById(R.id.switchlight);
        switchLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("[BLUETOOTH]", "Attempting to send data");
                if (mmSocket.isConnected() && btt != null) { //if we have connection to the bluetoothmodule
                    if (!lightflag) {
                        String sendtxt = "LY";
                        btt.write(sendtxt.getBytes());
                        lightflag = true;
                    } else {
                        String sendtxt = "LN";
                        btt.write(sendtxt.getBytes());
                        lightflag = false;
                    }
                } else {
                    Toast.makeText(Ardcon.this, "Something went wrong", Toast.LENGTH_LONG).show();
                }
            }
        });
        switchRelay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("[BLUETOOTH]", "Attempting to send data");
                if (mmSocket.isConnected() && btt != null) { //if we have connection to the bluetoothmodule
                    if(relayFlag){
                        String sendtxt = "RY";
                        btt.write(sendtxt.getBytes());
                        relayFlag = false;
                    }else{
                        String sendtxt = "RN";
                        btt.write(sendtxt.getBytes());
                        relayFlag = true;
                    }

                    //disable the button and wait for 4 seconds to enable it again
                    switchRelay.setEnabled(false);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                Thread.sleep(4000);
                            }catch(InterruptedException e){
                                return;
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    switchRelay.setEnabled(true);
                                }
                            });

                        }
                    }).start();
                } else {
                    Toast.makeText(Ardcon.this, "Something went wrong", Toast.LENGTH_LONG).show();
                }
            }
        });

        bta = BluetoothAdapter.getDefaultAdapter();

        //if bluetooth is not enabled then create Intent for user to turn it on
        if(!bta.isEnabled()){
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
        }else{
            initiateBluetoothProcess();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK && requestCode == REQUEST_ENABLE_BT){
            initiateBluetoothProcess();
        }
    }

    public void initiateBluetoothProcess(){

        if(bta.isEnabled()){

            //attempt to connect to bluetooth module
            BluetoothSocket tmp = null;
            mmDevice = bta.getRemoteDevice(MODULE_MAC);

            //create socket
            try {
                tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
                mmSocket = tmp;
                mmSocket.connect();
                Log.i("[BLUETOOTH]","Connected to: "+mmDevice.getName());
            }catch(IOException e){
                try{mmSocket.close();}catch(IOException c){return;}
            }

            Log.i("[BLUETOOTH]", "Creating handler");
            mHandler = new Handler(Looper.getMainLooper()){
                @Override
                public void handleMessage(Message msg) {
                    //super.handleMessage(msg);
                    if(msg.what == ConnectedThread.RESPONSE_MESSAGE){
                        String txt = (String)msg.obj;
                        if(response.getText().toString().length() >= 30){
                            response.setText("");
                            response.append(txt);
                        }else{
                            response.append("\n" + txt);
                        }
                    }
                }
            };

            Log.i("[BLUETOOTH]", "Creating and running Thread");
            btt = new ConnectedThread(mmSocket,mHandler);
            btt.start();


        }
    }

}
