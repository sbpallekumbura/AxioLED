package axio.com.axioled;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.view.View;
import android.content.Intent;
import android.widget.ToggleButton;
import android.content.Context;
import android.widget.Toast;
import android.view.Gravity;
import android.graphics.drawable.Drawable;
import android.app.AlertDialog;

import axio.com.axioled.BLEConnectionServices.BluetoothLeService;
import axio.com.axioled.CommonUtils.Logger;
import axio.com.axioled.CommonUtils.Utils;

public class MainActivity extends Activity {

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,-1);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        set_bluetoothStatus(false);
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                       // setButtonText("Turning Bluetooth off...");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        set_bluetoothStatus(true);
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                       // setButtonText("Turning Bluetooth on...");
                        break;
                }
            }
        }
    };

    private BluetoothAdapter _bluetoothAdapter;
    private boolean _bluetoothStatus;

    private static final int ENABLE_BT_REQUEST_CODE = 1;

    public boolean is_bluetoothStatus() {
        return _bluetoothStatus;
    }

    public void set_bluetoothStatus(boolean _bluetoothStatus) {
        this._bluetoothStatus = _bluetoothStatus;
        setBluetoothStatusforBtnBluetooth(_bluetoothStatus);
    }

    public void set_bluetoothStatus()
    {
        if(!_bluetoothAdapter.isEnabled())
        {
            set_bluetoothStatus(false);
        }
        else {
            set_bluetoothStatus(true);
        }
    }


    SeekBar _seekBar;
    Button _btnSettings;
    Button _btnZero,_btnHalf,_btnFull,_btnBluetooth;
    ToggleButton _btnPower;
    TextView _seekBarValue;
    Toast toast=null;
    Context context;
    boolean _appState;
    int prev_progress,curr_progress;

    Boolean _connected;

    private AxioLEDApplication mApplication;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();

        _seekBar = (SeekBar)findViewById(R.id.seekbar);
        _seekBarValue = (TextView)findViewById(R.id.seekbarvalue);

        _btnBluetooth=(Button) findViewById(R.id.BtnBluetooth);
        _btnPower =(ToggleButton) findViewById(R.id.BtnPower);
        _btnZero = (Button) findViewById(R.id.BtnZero);
        _btnHalf = (Button) findViewById(R.id.BtnHalf);
        _btnFull = (Button) findViewById(R.id.BtnFull);

        Enabled(false);

        _appState=false;
        app_state(_appState);

        _bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            _seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                // TODO Auto-generated method stub
                if(curr_progress!=0) {
                    prev_progress = curr_progress;
                }
                curr_progress=progress;

                _seekBarValue.setText(String.valueOf(progress) + "%");
                change_state_btnExposure(progress);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }});

                _btnSettings = (Button) findViewById(R.id.BtnSettings);
                _btnSettings.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent=new Intent(MainActivity.this,Settings.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivity(intent);
                    }
                });

            _btnPower.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    _appState=_btnPower.isChecked();
                    app_state(_appState);
                    if(_appState) {
                        change_state_btnExposure(_seekBar.getProgress());
                    }
                    onOffLED();
                    String text = String.valueOf((_appState) ? "LED Switch on" : "LED Switch off");
                    show_msg(text);
                }
            });

             _btnZero.setOnClickListener(new View.OnClickListener() {
            @Override
                public void onClick(View v) {
                show_msg("Contrast set to 25%");
                    _seekBar.setProgress(25);
                    change_state_btnExposure(25);
                }
             });
            _btnHalf.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    show_msg("Contrast set to 50%");
                    _seekBar.setProgress(50);
                    change_state_btnExposure(50);

                }
            });
            _btnFull.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    show_msg("Contrast set to 100%");
                    _seekBar.setProgress(100);
                    change_state_btnExposure(100);
                }
            });

            prev_progress=_seekBar.getProgress();
        }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        /*if (id == R.id.action_exit) {
            exitApp();
            return true;
        }*/

        return super.onOptionsItemSelected(item);
    }

    public void app_state(boolean state)
    {
        _btnZero.setEnabled(state);
        _btnHalf.setEnabled(state);
        _btnFull.setEnabled(state);
        _seekBarValue.setEnabled(state);
       _seekBar.setEnabled(state);
        if (!state) {
//            _btnZero.setBackgroundResource(R.drawable.zero_off);
//            _btnHalf.setBackgroundResource(R.drawable.half_off);
//            _btnFull.setBackgroundResource(R.drawable.full_off);
            change_state_btnExposure(_seekBar.getProgress());
            Drawable thumb = getResources().getDrawable(R.drawable.contrast_disabled);
            _seekBar.setThumb(thumb);
        } else {
            Drawable thumb = getResources().getDrawable( R.drawable.contrast);
            _seekBar.setThumb(thumb);
        }
    }

    public void change_state_btnExposure(int progress)
    {
        _btnZero.setBackgroundResource(R.drawable.zero_off);
        _btnHalf.setBackgroundResource(R.drawable.half_off);
        _btnFull.setBackgroundResource(R.drawable.full_off);

        chage_LED_Britness();

        if (_appState) {
            if (progress == 25) {
                _btnZero.setBackgroundResource(R.drawable.zero_on);
            } else if (progress == 50) {
                _btnHalf.setBackgroundResource(R.drawable.half_on);
            } else if (progress == 100) {
                _btnFull.setBackgroundResource(R.drawable.full_on);
            }
        } else {
            if (progress == 25) {
                _btnZero.setBackgroundResource(R.drawable.zero_off_d);
            } else if (progress == 50) {
                _btnHalf.setBackgroundResource(R.drawable.half_off_d);
            } else if (progress == 100) {
                _btnFull.setBackgroundResource(R.drawable.full_off_d);
            }
        }
    }

    public void chage_LED_Britness()
    {
        int value=getIncrement();
        int steps=Math.abs(value)/10;
        if(Math.signum(value)==1)
        {
            increaseBritness(steps);
        }
        else if(Math.signum(value)==-1)
        {
            decreaseBritness(steps);
        }
    }

    public int getIncrement()
    {
       return curr_progress-prev_progress;
    }

    public void show_msg(String msg) {
    /*    if (toast != null) {
            toast.cancel();
        }
        int duration = Toast.LENGTH_SHORT;

        toast = Toast.makeText(context,msg, duration);
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 150);
        toast.show();*/
    }

    @Override
    public void onBackPressed() {
        exitApp();
    }

    public void freezeApp(){
        Log.d("CDA", "onBackPressed Called");
        Intent setIntent = new Intent(Intent.ACTION_MAIN);
        setIntent.addCategory(Intent.CATEGORY_HOME);
        setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(setIntent);
    }

    public  void exitApp(){

        AlertDialog.Builder builder = new AlertDialog.Builder(
                MainActivity.this);
        builder.setMessage(
                getResources().getString(R.string.alert_message_exit))
                .setCancelable(false)
                .setTitle(getResources().getString(R.string.app_name))
                .setPositiveButton(
                        getResources()
                                .getString(R.string.alert_message_exit_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Finish the current activity
                                //MainActivity.this.finish();
                                finishAffinity();
                                Intent gattServiceIntent = new Intent(getApplicationContext(),
                                        BluetoothLeService.class);
                                stopService(gattServiceIntent);

                            }
                        })
                .setNegativeButton(
                        getResources().getString(
                                R.string.alert_message_exit_cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Cancel the dialog box
                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();

    }

    public void Enabled(boolean result)
    {
        _btnPower.setEnabled(result);
    }

    public void setBluetoothStatusforBtnBluetooth(boolean s){
            _btnBluetooth.setEnabled(s);
    }
    @Override
    protected void onResume() {
        set_bluetoothStatus();
        // Register the BroadcastReceiver
        registerBroadcastReceiver();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            _connected = extras.getBoolean("connected");
            if(_connected)
            {
                app_state(true);
            }
        }

        super.onResume();
    }

    @Override
    protected void onPause() {
        this.unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    protected void registerBroadcastReceiver()
    {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        this.registerReceiver(broadcastReceiver, intentFilter);
    }




    private BluetoothGattCharacteristic mReadCharacteristic;

    public void writeOnDevice(String result) {
        byte[] convertedBytes = Utils.convertingTobyteArray(result);
        writeCharaValue(convertedBytes);
    }

    /**
     * Method to write the byte value to the characteristic
     * @param value
     */
    private void writeCharaValue(byte[] value) {

        mApplication=(AxioLEDApplication)getApplication();
        mReadCharacteristic = mApplication.getBluetoothgattcharacteristic();
        ////displayTimeandDate();
        // Writing the hexValue to the characteristic
        try {
            BluetoothLeService.writeCharacteristicGattDb(mReadCharacteristic,
                    value);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void onOffLED(){
        writeOnDevice("0x5");
    }
    public void increaseBritness(int steps)
    {
        for(int count=0;count<steps;count++) {
            writeOnDevice("0x4");
        }
    }
    public void decreaseBritness(int steps)
    {
        for(int count=0;count<steps;count++) {
            writeOnDevice("0x2");
        }

    }
}
