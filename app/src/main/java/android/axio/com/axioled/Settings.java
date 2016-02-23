package android.axio.com.axioled;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.net.wifi.ScanResult;

import java.util.List;


public class Settings extends ActionBarActivity {


    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // Whenever a remote Bluetooth device is found
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                arrayAdapterBT.add(bluetoothDevice.getName() + "\n"
                        + bluetoothDevice.getAddress());
            }
        }
    };

    private final BroadcastReceiver  wifiReciever=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                List<ScanResult> wifiScanResultList =(List<ScanResult>)  wifiManager.getScanResults();
                for(int i = 0; i < wifiScanResultList.size(); i++){
                    String hotspot = (wifiScanResultList.get(i)).toString();
                    arrayAdapterWIFI.add(hotspot);
                }
            }
        }
    };

    private BluetoothAdapter bluetoothAdapter;
    private ToggleButton toggleButtonBT;
    private ListView listviewBT;
    private ArrayAdapter arrayAdapterBT;
    private static final int ENABLE_BT_REQUEST_CODE = 1;
    private static final int DISCOVERABLE_BT_REQUEST_CODE = 2;
    private static final int DISCOVERABLE_DURATION = 300;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        //BT
        toggleButtonBT = (ToggleButton) findViewById(R.id.toggleButtonBT);
        listviewBT = (ListView) findViewById(R.id.listViewBT);
        arrayAdapterBT = new ArrayAdapter
                (this,android.R.layout.simple_list_item_1);
        listviewBT.setAdapter(arrayAdapterBT);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //WIFI
        toggleButtonWIFI = (ToggleButton) findViewById(R.id.toggleButtonWIFI);
        listviewWIFI = (ListView) findViewById(R.id.listViewWIFI);
        arrayAdapterWIFI = new ArrayAdapter
                (this,android.R.layout.simple_list_item_1);
        listviewWIFI.setAdapter(arrayAdapterWIFI);

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

    }

    public void onToggleClickedBT(View view) {

        arrayAdapterBT.clear();

        ToggleButton toggleButton = (ToggleButton) view;

        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(getApplicationContext(), "Oop! Your device does not support Bluetooth",
                    Toast.LENGTH_SHORT).show();
            toggleButton.setChecked(false);
        }
        else {

            if (toggleButton.isChecked()){ // to turn on bluetooth
                if (!bluetoothAdapter.isEnabled()) {
                    // A dialog will appear requesting user permission to enable Bluetooth
                    Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBluetoothIntent, ENABLE_BT_REQUEST_CODE);
                } else {
                    Toast.makeText(getApplicationContext(), "Your device has already been enabled." +
                                    "\n" + "Scanning for remote Bluetooth devices...",
                            Toast.LENGTH_SHORT).show();
                    // To discover remote Bluetooth devices
                    discoverDevices();
                    // Make local device discoverable by other devices
                    makeDiscoverable();
                }
            } else { // Turn off bluetooth

                bluetoothAdapter.disable();
                arrayAdapterBT.clear();
                Toast.makeText(getApplicationContext(), "Your device is now disabled.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == ENABLE_BT_REQUEST_CODE) {

            // Bluetooth successfully enabled!
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(getApplicationContext(), "Ha! Bluetooth is now enabled." +
                                "\n" + "Scanning for remote Bluetooth devices...",
                        Toast.LENGTH_SHORT).show();

                // Make local device discoverable by other devices
                makeDiscoverable();

                // To discover remote Bluetooth devices
                discoverDevices();

            } else { // RESULT_CANCELED as user refused or failed to enable Bluetooth
                Toast.makeText(getApplicationContext(), "Bluetooth is not enabled.",
                        Toast.LENGTH_SHORT).show();

                // Turn off togglebutton
                toggleButtonBT.setChecked(false);
            }
        } else if (requestCode == DISCOVERABLE_BT_REQUEST_CODE){

            if (resultCode == DISCOVERABLE_DURATION){
                Toast.makeText(getApplicationContext(), "Your device is now discoverable by other devices for " +
                                DISCOVERABLE_DURATION + " seconds",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Fail to enable discoverability on your device.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    protected void discoverDevices(){
        // To scan for remote Bluetooth devices
        if (bluetoothAdapter.startDiscovery()) {
            Toast.makeText(getApplicationContext(), "Discovering other bluetooth devices...",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Discovery failed to start.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    protected void makeDiscoverable(){
        // Make local device discoverable
        Intent discoverableIntent = new
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_DURATION);
        startActivityForResult(discoverableIntent, DISCOVERABLE_BT_REQUEST_CODE);
    }

    private WifiManager wifiManager;
    private ToggleButton toggleButtonWIFI;
    private ListView listviewWIFI;
    private ArrayAdapter arrayAdapterWIFI;


    public void onToggleClickedWIFI(View view) {

        arrayAdapterBT.clear();

        ToggleButton toggleButton = (ToggleButton) view;

        if (wifiManager == null) {
            // Device does not support WIFI
            Toast.makeText(getApplicationContext(), "Oop! Your device does not support WIFI",
                    Toast.LENGTH_SHORT).show();
            toggleButton.setChecked(false);
        }
        else {

            if (toggleButton.isChecked()){ // to turn on bluetooth
                    wifiManager.setWifiEnabled(true); // To turn on the Wi-Fi
                    Toast.makeText(getApplicationContext(), "Your device is now WIFI enabled." +
                                    "\n" + "Scanning for remote WIFI devices...",
                            Toast.LENGTH_SHORT).show();
                    // To discover remote WIFI devices
                    discoverWIFIDevices();
                }
            else { // Turn off bluetooth

                wifiManager.setWifiEnabled(false); // To turn on the Wi-Fi
                arrayAdapterWIFI.clear();
                Toast.makeText(getApplicationContext(), "Your device is now WIFI disabled.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    protected void discoverWIFIDevices(){
        // To scan for remote Bluetooth devices
        if (wifiManager.startScan()) {
            Toast.makeText(getApplicationContext(), "Scan other WIFI devices...",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Scan WIFI failed to start.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register the BroadcastReceiver for ACTION_FOUND
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(broadcastReceiver, filter);

        IntentFilter filterWIFI = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiReciever, filterWIFI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(broadcastReceiver);
        this.unregisterReceiver(wifiReciever);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
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
            finish();
            System.exit(0);
            return true;
        }

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (Integer.parseInt(android.os.Build.VERSION.SDK) > 5
                && keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            Log.d("CDA", "onKeyDown Called");
            onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


/*    @Override
    public void onBackPressed() {
        Log.d("CDA", "onBackPressed Called");
        Toast.makeText(getApplicationContext(), "onBackPressed Called.",
                Toast.LENGTH_SHORT).show();
        Intent setIntent = new Intent(Intent.ACTION_MAIN);
        setIntent.addCategory(Intent.CATEGORY_HOME);
        setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(setIntent);
    }*/

/*    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }*/

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }
}


