package axio.com.axioled;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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

import axio.com.axioled.CommonUtils.Logger;

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


    SeekBar _seekBar;
    Button _btnSettings;
    Button _btnZero,_btnHalf,_btnFull,_btnBluetooth;
    ToggleButton _btnPower;
    TextView _seekBarValue;
    Toast toast=null;
    Context context;
    boolean _appState;


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
        _appState=false;
        app_state(_appState);

        _bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

      /*  if (!_bluetoothAdapter.isEnabled()) {
            // A dialog will appear requesting user permission to enable Bluetooth
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, ENABLE_BT_REQUEST_CODE);
        }*/


            _seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                // TODO Auto-generated method stub
                _seekBarValue.setText(String.valueOf(progress)+"%");
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
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_exit) {
            exitApp();
            return true;
        }

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

    public void show_msg(String msg) {
        if (toast != null) {
            toast.cancel();
        }
        int duration = Toast.LENGTH_SHORT;

        toast = Toast.makeText(context,msg, duration);
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 150);
        toast.show();
    }

    @Override
    public void onBackPressed() {
        Log.d("CDA", "onBackPressed Called");
        Intent setIntent = new Intent(Intent.ACTION_MAIN);
        setIntent.addCategory(Intent.CATEGORY_HOME);
        setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(setIntent);
    }

    public  void exitApp(){

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                context);

        // set title
        alertDialogBuilder.setTitle("Exit");

        // set dialog message
        alertDialogBuilder
                .setMessage("Click yes to exit!")
                .setCancelable(false)
                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        // if this button is clicked, close
                        // current activity
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        dialog.cancel();
                    }
                });

       /* // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();*/
    }

    public void setBluetoothStatusforBtnBluetooth(boolean s){
            _btnBluetooth.setEnabled(s);
    }
    @Override
    protected void onResume() {
        // Register the BroadcastReceiver
        registerBroadcastReceiver();
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
}
