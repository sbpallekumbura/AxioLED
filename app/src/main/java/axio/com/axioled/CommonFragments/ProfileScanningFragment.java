/*
 * Copyright Cypress Semiconductor Corporation, 2014-2015 All rights reserved.
 * 
 * This software, associated documentation and materials ("Software") is
 * owned by Cypress Semiconductor Corporation ("Cypress") and is
 * protected by and subject to worldwide patent protection (UnitedStates and foreign), United States copyright laws and international
 * treaty provisions. Therefore, unless otherwise specified in a separate license agreement between you and Cypress, this Software
 * must be treated like any other copyrighted material. Reproduction,
 * modification, translation, compilation, or representation of this
 * Software in any other form (e.g., paper, magnetic, optical, silicon)
 * is prohibited without Cypress's express written permission.
 * 
 * Disclaimer: THIS SOFTWARE IS PROVIDED AS-IS, WITH NO WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO,
 * NONINFRINGEMENT, IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE. Cypress reserves the right to make changes
 * to the Software without notice. Cypress does not assume any liability
 * arising out of the application or use of Software or any product or
 * circuit described in the Software. Cypress does not authorize its
 * products for use as critical components in any products where a
 * malfunction or failure may reasonably be expected to result in
 * significant injury or death ("High Risk Product"). By including
 * Cypress's product in a High Risk Product, the manufacturer of such
 * system or application assumes all risk of such use and in doing so
 * indemnifies Cypress against all liability.
 * 
 * Use of this Software may be limited by and subject to the applicable
 * Cypress software license agreement.
 * 
 * 
 */

package axio.com.axioled.CommonFragments;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import axio.com.axioled.AxioLEDApplication;
import axio.com.axioled.BLEConnectionServices.BluetoothLeService;
import axio.com.axioled.CommonUtils.Constants;
import axio.com.axioled.CommonUtils.Logger;
import axio.com.axioled.CommonUtils.Utils;
import axio.com.axioled.CommonUtils.UUIDDatabase;
import axio.com.axioled.CommonUtils.GattAttributes;
import axio.com.axioled.MainActivity;
import axio.com.axioled.R;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class ProfileScanningFragment extends Fragment {

    // Stops scanning after 2 seconds.
    private static final long SCAN_PERIOD_TIMEOUT = 2000;
    private Timer mScanTimer;
    private boolean mScanning;

    // Connection time out after 10 seconds.
    private static final long CONNECTION_TIMEOUT = 10000;
    private Timer mConnectTimer;
    private boolean mConnectTimerON=false;

    // Activity request constant
    private static final int REQUEST_ENABLE_BT = 1;

    // device details
    public static String mDeviceName = "name";
    public static String mDeviceAddress = "address";

    //Pair status button and variables
    public static Button mPairButton;

    //Bluetooth adapter
    private static BluetoothAdapter mBluetoothAdapter;

    // Devices list variables
    private static ArrayList<BluetoothDevice> mLeDevices;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private SwipeRefreshLayout mSwipeLayout;
    private Map<String, Integer> mDevRssiValues;

    //GUI elements
    private ListView mProfileListView;
    private TextView mRefreshText;
    private ProgressDialog mProgressdialog;

    //  Flags
    private boolean mSearchEnabled = false;
    public static boolean isInFragment = false;

    //Delay Time out
    private static final long DELAY_PERIOD = 500;


    /**
     * Call back for BLE Scan
     * This call back is called when a BLE device is found near by.
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             byte[] scanRecord) {
            Activity mActivity = getActivity();
            if (mActivity != null) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!mSearchEnabled) {
                            mLeDeviceListAdapter.addDevice(device, rssi);
                            try {
                                mLeDeviceListAdapter.notifyDataSetChanged();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }

        }
    };

    /**
     * BroadcastReceiver for receiving the GATT communication status
     */
    private final BroadcastReceiver mGattConnectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            // Status received when connected to GATT Server
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mProgressdialog.setMessage(getString(R.string.alert_message_bluetooth_connect));
                if (mScanning) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mScanning = false;
                }
                mProgressdialog.dismiss();
                mLeDevices.clear();
                if(mConnectTimer!=null)
                mConnectTimer.cancel();
                mConnectTimerON=false;
                clearmBluetoothAdapter();
                startServiceDiscovery();

            }else if(BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)){
                /**
                 * Disconnect event.When the connect timer is ON,Reconnect the device
                 * else show disconnect message
                 */
                if(mConnectTimerON){
                    BluetoothLeService.reconnect();
                }else{
                    Toast.makeText(getActivity(),
                            R.string.profile_cannot_connect_message,
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    public void clearmBluetoothAdapter(){
        if (mLeDeviceListAdapter != null) {
            mLeDeviceListAdapter.clear();
            try {
                mLeDeviceListAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void startServiceDiscovery(){
        Handler delayHandler = new Handler();
        delayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Logger.e("Discover service called");
                if(BluetoothLeService.getConnectionState()==BluetoothLeService.STATE_CONNECTED)
                    BluetoothLeService.discoverServices();
            }
        }, DELAY_PERIOD);
    }
    /**
     * Textwatcher for filtering the list devices
     */
    private TextWatcher textWatcher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
            mLeDeviceListAdapter.notifyDataSetInvalidated();
            mLeDeviceListAdapter.getFilter().filter(s.toString());
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mApplication=(AxioLEDApplication) getActivity().getApplication();

       View mrootView = inflater.inflate(R.layout.fragment_profile_scan, container,
                false);
        mDevRssiValues = new HashMap<String, Integer>();
        mSwipeLayout = (SwipeRefreshLayout) mrootView
                .findViewById(R.id.swipe_container);
        mSwipeLayout.setColorScheme(R.color.dark_blue, R.color.medium_blue,
                R.color.light_blue, R.color.faint_blue);
        mProfileListView = (ListView) mrootView
                .findViewById(R.id.listView_profiles);
        mRefreshText = (TextView) mrootView.findViewById(R.id.no_dev);
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        mProfileListView.setAdapter(mLeDeviceListAdapter);
        mProfileListView.setTextFilterEnabled(true);
        setHasOptionsMenu(true);

        mProgressdialog = new ProgressDialog(getActivity());
        mProgressdialog.setCancelable(false);

        checkBleSupportAndInitialize();
        prepareList();

        /**
         * Swipe listener,initiate a new scan on refresh. Stop the swipe refresh
         * after 5 seconds
         */
        mSwipeLayout
                .setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

                    @Override
                    public void onRefresh() {
                        if (!mScanning) {
                            // Prepare list view and initiate scanning
                            if (mLeDeviceListAdapter != null) {
                                mLeDeviceListAdapter.clear();
                                try {
                                    mLeDeviceListAdapter.notifyDataSetChanged();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            scanLeDevice(true);
                            mScanning = true;
                            mSearchEnabled = false;
                            mRefreshText.setText(getResources().getString(
                                    R.string.profile_control_device_scanning));
                        }

                    }

                });


        /**
         * Creating the dataLogger file and
         * updating the datalogger history
         */
        Logger.createDataLoggerFile(getActivity());

        mProfileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (mLeDeviceListAdapter.getCount() > 0) {
                    final BluetoothDevice device = mLeDeviceListAdapter
                            .getDevice(position);
                    if (device != null) {
                        scanLeDevice(false);
                        connectDevice(device,true);
                    }
                }
            }
        });

        return mrootView;
    }

    private void checkBleSupportAndInitialize() {
        // Use this check to determine whether BLE is supported on the device.
        if (!getActivity().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(getActivity(), R.string.device_ble_not_supported,
                    Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }
        // Initializes a Blue tooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getActivity()
                .getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            // Device does not support Blue tooth
            Toast.makeText(getActivity(),
                    R.string.device_bluetooth_not_supported, Toast.LENGTH_SHORT)
                    .show();
            getActivity().finish();
        }
    }

    /**
     * Method to connect to the device selected. The time allotted for having a
     * connection is 8 seconds. After 8 seconds it will disconnect if not
     * connected and initiate scan once more
     *
     * @param device
     */

    private void connectDevice(BluetoothDevice device,boolean isFirstConnect) {
        mDeviceAddress = device.getAddress();
        mDeviceName = device.getName();
        // Get the connection status of the device
        if (BluetoothLeService.getConnectionState() == BluetoothLeService.STATE_DISCONNECTED) {
            Logger.v("BLE DISCONNECTED STATE");
            // Disconnected,so connect
            BluetoothLeService.connect(mDeviceAddress, mDeviceName, getActivity());
            showConnectAlertMessage(mDeviceName, mDeviceAddress);
        }
        else {
            Logger.v("BLE OTHER STATE-->" + BluetoothLeService.getConnectionState());
            // Connecting to some devices,so disconnect and then connect
            BluetoothLeService.disconnect();
            Handler delayHandler = new Handler();
            delayHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    BluetoothLeService.connect(mDeviceAddress, mDeviceName, getActivity());
                    showConnectAlertMessage(mDeviceName, mDeviceAddress);
                }
            }, DELAY_PERIOD);

        }
        if(isFirstConnect){
            startConnectTimer();
            mConnectTimerON=true;
        }

    }

    private void showConnectAlertMessage(String devicename,String deviceaddress) {
        mProgressdialog.setTitle(getResources().getString(
                R.string.alert_message_connect_title));
        mProgressdialog.setMessage(getResources().getString(
                R.string.alert_message_connect)
                + "\n"
                + devicename
                + "\n"
                + deviceaddress
                + "\n"
                + getResources().getString(R.string.alert_message_wait));

        if (!getActivity().isDestroyed() && mProgressdialog != null) {
            mProgressdialog.show();
        }
    }

    /**
     * Method to scan BLE Devices. The status of the scan will be detected in
     * the BluetoothAdapter.LeScanCallback
     *
     * @param enable
     */
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            if (!mScanning) {
                startScanTimer();
                mScanning = true;
                mRefreshText.setText(getResources().getString(
                        R.string.profile_control_device_scanning));
                mBluetoothAdapter.startLeScan(mLeScanCallback);
                mSwipeLayout.setRefreshing(true);
            }
        } else {
            mScanning = false;
            mSwipeLayout.setRefreshing(false);
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }

    }

    /**
     * Preparing the BLE Devicelist
     */
    public void prepareList() {
        // Initializes ActionBar as required
        setUpActionBar();
        // Prepare list view and initiate scanning
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        mProfileListView.setAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
        mSearchEnabled = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        Logger.e("Scanning onResume");
        isInFragment = true;
        if(checkBluetoothStatus()){
            prepareList();
        }
        Logger.e("Registering receiver in Profile scannng");
        getActivity().registerReceiver(mGattConnectReceiver,
                Utils.makeGattUpdateIntentFilter());
        Logger.e("Registering Service Discovery Listner");
        getActivity().registerReceiver(mServiceDiscoveryListner,
                Utils.makeGattUpdateIntentFilter());

    }

    @Override
    public void onPause() {
        Logger.e("Scanning onPause");
        isInFragment = false;
        if (mProgressdialog != null && mProgressdialog.isShowing()) {
            mProgressdialog.dismiss();
        }
        Logger.e("UN Registering receiver in Profile scannng");
        getActivity().unregisterReceiver(mGattConnectReceiver);
        Logger.e("UN Registering receiver in Service Discovery Listner");
        getActivity().unregisterReceiver(mServiceDiscoveryListner);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
            scanLeDevice(false);
            isInFragment = false;
            if (mLeDeviceListAdapter != null)
                mLeDeviceListAdapter.clear();
            if (mLeDeviceListAdapter != null) {
                try {
                    mLeDeviceListAdapter.notifyDataSetChanged();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mSwipeLayout.setRefreshing(false);
    }

    private void updateWithNewFragment() {
        if (mLeDeviceListAdapter != null) {
            mLeDeviceListAdapter.clear();
            try {
                mLeDeviceListAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
      //  getActivity().unregisterReceiver(mGattConnectReceiver);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable BlueTooth.
        if (requestCode == REQUEST_ENABLE_BT
                && resultCode == Activity.RESULT_CANCELED) {
            getActivity().finish();
        } else {
            // Check which request we're responding to
            if (requestCode == REQUEST_ENABLE_BT) {

                // Make sure the request was successful
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(
                            getActivity(),
                            getResources().getString(
                                    R.string.device_bluetooth_on),
                            Toast.LENGTH_SHORT).show();
                    mLeDeviceListAdapter = new LeDeviceListAdapter();
                    mProfileListView.setAdapter(mLeDeviceListAdapter);
                    scanLeDevice(true);
                } else {
                    getActivity().finish();
                }
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    /**
     * Setting up the ActionBar
     */
    void setUpActionBar() {
        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setIcon(new ColorDrawable(getResources().getColor(
                    android.R.color.transparent)));
        }
        if (actionBar != null) {
            //actionBar.setTitle(R.string.profile_scan_fragment);
        }
    }

    //For Pairing
    private void pairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("createBond", (Class[]) null);
            m.invoke(device, (Object[]) null);

        } catch (Exception e) {
            if (mProgressdialog != null && mProgressdialog.isShowing()) {
                mProgressdialog.dismiss();
            }
        }

    }

    //For UnPairing
    private void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);

        } catch (Exception e) {
            if (mProgressdialog != null && mProgressdialog.isShowing()) {
                mProgressdialog.dismiss();
            }
        }

    }

    public boolean checkBluetoothStatus() {
        /**
         * Ensures Blue tooth is enabled on the device. If Blue tooth is not
         * currently enabled, fire an intent to display a dialog asking the user
         * to grant permission to enable it.
         */
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return false;
        }
        return true;
    }


    /**
     * Holder class for the list view view widgets
     */
    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRssi;
        Button pairStatus;
    }

    /**
     * Connect Timer
     */
    private void startConnectTimer(){
        mConnectTimer=new Timer();
        mConnectTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mProgressdialog.dismiss();
                Logger.v("CONNECTION TIME OUT");
                mConnectTimerON=false;
                BluetoothLeService.disconnect();
                if(getActivity()!=null){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(),
                                    R.string.profile_cannot_connect_message,
                                    Toast.LENGTH_SHORT).show();
                            if (mLeDeviceListAdapter != null)
                                mLeDeviceListAdapter.clear();
                            if (mLeDeviceListAdapter != null) {
                                try {
                                    mLeDeviceListAdapter.notifyDataSetChanged();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            scanLeDevice(true);
                            mScanning = true;
                        }
                    });
                }

            }
        }, CONNECTION_TIMEOUT);
    }
    /**
     * Swipe refresh timer
     */
    public void startScanTimer(){
        mScanTimer=new Timer();
        mScanTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mRefreshText.post(new Runnable() {
                    @Override
                    public void run() {
                        mRefreshText.setText(getResources().getString(
                                R.string.profile_control_no_device_message));
                    }
                });
                mSwipeLayout.setRefreshing(false);
                scanLeDevice(false);
            }
        },SCAN_PERIOD_TIMEOUT);
    }

    /**
     * List Adapter for holding devices found through scanning.
     */
    private class LeDeviceListAdapter extends BaseAdapter implements Filterable {

        ArrayList<BluetoothDevice> mFilteredDevices = new ArrayList<BluetoothDevice>();
        private LayoutInflater mInflator;
        private int rssiValue;
        private ItemFilter mFilter = new ItemFilter();

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = getActivity().getLayoutInflater();
        }

        private void addDevice(BluetoothDevice device, int rssi) {
            this.rssiValue = rssi;
            // New device found
            if (!mLeDevices.contains(device)) {
                mDevRssiValues.put(device.getAddress(), rssi);
                mLeDevices.add(device);
            } else {
                mDevRssiValues.put(device.getAddress(), rssi);
            }
        }

        public int getRssiValue() {
            return rssiValue;
        }

        /**
         * Getter method to get the blue tooth device
         *
         * @param position
         * @return BluetoothDevice
         */
        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        /**
         * Clearing all values in the device array list
         */
        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }


        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }


        @Override
        public View getView(final int position, View view, ViewGroup viewGroup) {
            final ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, viewGroup,
                        false);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view
                        .findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view
                        .findViewById(R.id.device_name);
                viewHolder.deviceRssi = (TextView) view
                        .findViewById(R.id.device_rssi);
                viewHolder.pairStatus = (Button) view.findViewById(R.id.btn_pair);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            /**
             * Setting the name and the RSSI of the BluetoothDevice. provided it
             * is a valid one
             */
            final BluetoothDevice device = mLeDevices.get(position);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0) {
                try {
                    viewHolder.deviceName.setText(deviceName);
                    viewHolder.deviceAddress.setText(device.getAddress());
                    byte rssival = (byte) mDevRssiValues.get(device.getAddress())
                            .intValue();
                    if (rssival != 0) {
                        viewHolder.deviceRssi.setText(String.valueOf(rssival));
                    }
                    String pairStatus = (device.getBondState() == BluetoothDevice.BOND_BONDED) ? getActivity().getResources().getString(R.string.bluetooth_pair) : getActivity().getResources().getString(R.string.bluetooth_unpair);
                    viewHolder.pairStatus.setText(pairStatus);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                viewHolder.deviceName.setText(R.string.device_unknown);
                viewHolder.deviceName.setSelected(true);
                viewHolder.deviceAddress.setText(device.getAddress());
            }
            viewHolder.pairStatus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mPairButton = (Button) view;
                    mDeviceAddress = device.getAddress();
                    mDeviceName = device.getName();
                    String status = mPairButton.getText().toString();
                    if (status.equalsIgnoreCase(getResources().getString(R.string.bluetooth_pair))) {
                        unpairDevice(device);
                    } else {
                        pairDevice(device);
                    }

                }
            });
            return view;
        }

        @Override
        public Filter getFilter() {
            return mFilter;
        }

        private class ItemFilter extends Filter {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                String mFilterString = constraint.toString().toLowerCase();

                FilterResults mResults = new FilterResults();

                final ArrayList<BluetoothDevice> list = mLeDevices;

                int count = list.size();
                final ArrayList<BluetoothDevice> nlist = new ArrayList<BluetoothDevice>(count);

                for (int i = 0; i < count; i++) {
                    if (list.get(i).getName() != null && list.get(i).getName().toLowerCase().contains(mFilterString)) {
                        nlist.add(list.get(i));
                    }
                }

                mResults.values = nlist;
                mResults.count = nlist.size();
                return mResults;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mFilteredDevices = (ArrayList<BluetoothDevice>) results.values;
                clear();
                int count = mFilteredDevices.size();
                for (int i = 0; i < count; i++) {
                    BluetoothDevice mDevice = mFilteredDevices.get(i);
                    mLeDeviceListAdapter.addDevice(mDevice, mLeDeviceListAdapter.getRssiValue());
                    notifyDataSetChanged(); // notifies the data with new filtered values
                }
            }
        }
    }

    /******************************New********************************************/
    // Application
    private AxioLEDApplication mApplication;
    private Timer mTimer;
    private TextView mNoserviceDiscovered;

    // UUID key
    private static final String LIST_UUID = "UUID";

    // Gatt Service Data
    static ArrayList<HashMap<String, BluetoothGattService>> mGattServiceData =
            new ArrayList<HashMap<String, BluetoothGattService>>();
    static ArrayList<HashMap<String, BluetoothGattService>> mGattServiceFindMeData =
            new ArrayList<HashMap<String, BluetoothGattService>>();
    static ArrayList<HashMap<String, BluetoothGattService>> mGattServiceProximityData =
            new ArrayList<HashMap<String, BluetoothGattService>>();
    private static ArrayList<HashMap<String, BluetoothGattService>> mGattdbServiceData =
            new ArrayList<HashMap<String, BluetoothGattService>>();
    private static ArrayList<HashMap<String, BluetoothGattService>> mGattServiceMasterData =
            new ArrayList<HashMap<String, BluetoothGattService>>();

    private static ArrayList<HashMap<String, BluetoothGattService>> mModifiedServiceData;

    // BluetoothGattService
    private static BluetoothGattService mService;

    // GattCharacteristics list
    private static List<BluetoothGattCharacteristic> mGattCharacteristics;

    private final BroadcastReceiver mServiceDiscoveryListner=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
                    .equals(action)) {
                Logger.e("Service discovered");
                if(mTimer!=null)
                    mTimer.cancel();
                prepareGattServices(BluetoothLeService.getSupportedGattServices());

                /*
                / Changes the MTU size to 512 in case LOLLIPOP and above devices
                */
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    BluetoothLeService.exchangeGattMtu(512);
                }
            } else if (BluetoothLeService.ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL
                    .equals(action)) {
               //// mProgressDialog.dismiss();
                if(mTimer!=null)
                    mTimer.cancel();
               //// showNoServiceDiscoverAlert();
            }
        }
    };

    private void prepareGattServices(List<BluetoothGattService> gattServices) {
        // Optimization code for Sensor HUb
            prepareData(gattServices);
    }

    private void prepareData(List<BluetoothGattService> gattServices) {
        boolean mFindmeSet = false;
        boolean mProximitySet = false;
        boolean mGattSet = false;
        if (gattServices == null)
            return;
        // Clear all array list before entering values.
        mGattServiceData.clear();
        mGattServiceFindMeData.clear();
        mGattServiceMasterData.clear();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, BluetoothGattService> currentServiceData = new HashMap<String, BluetoothGattService>();
            UUID uuid = gattService.getUuid();
            // Optimization code for FindMe Profile
            if (uuid.equals(UUIDDatabase.UUID_IMMEDIATE_ALERT_SERVICE)) {
                currentServiceData.put(LIST_UUID, gattService);
                mGattServiceMasterData.add(currentServiceData);
                if (!mGattServiceFindMeData.contains(currentServiceData)) {
                    mGattServiceFindMeData.add(currentServiceData);
                }
                if (!mFindmeSet) {
                    mFindmeSet = true;
                    mGattServiceData.add(currentServiceData);
                }

            }
            // Optimization code for Proximity Profile
            else if (uuid.equals(UUIDDatabase.UUID_LINK_LOSS_SERVICE)
                    || uuid.equals(UUIDDatabase.UUID_TRANSMISSION_POWER_SERVICE)) {
                currentServiceData.put(LIST_UUID, gattService);
                mGattServiceMasterData.add(currentServiceData);
                if (!mGattServiceProximityData.contains(currentServiceData)) {
                    mGattServiceProximityData.add(currentServiceData);
                }
                if (!mProximitySet) {
                    mProximitySet = true;
                    mGattServiceData.add(currentServiceData);
                }

            }// Optimization code for GATTDB
            else if (uuid.equals(UUIDDatabase.UUID_GENERIC_ACCESS_SERVICE)
                    || uuid.equals(UUIDDatabase.UUID_GENERIC_ATTRIBUTE_SERVICE)) {
                currentServiceData.put(LIST_UUID, gattService);
                mGattdbServiceData.add(currentServiceData);
                if (!mGattSet) {
                    mGattSet = true;
                    mGattServiceData.add(currentServiceData);
                }

            } //Optimization code for HID
            else if (uuid.equals(UUIDDatabase.UUID_HID_SERVICE)){
                /**
                 * Special handling for KITKAT devices
                 */
                if (android.os.Build.VERSION.SDK_INT < 21) {
                    Logger.e("Kitkat RDK device found");
                    List<BluetoothGattCharacteristic> allCharacteristics=
                            gattService.getCharacteristics();
                    List<BluetoothGattCharacteristic> RDKCharacteristics=new
                            ArrayList<BluetoothGattCharacteristic>();
                    List<BluetoothGattDescriptor> RDKDescriptors=new
                            ArrayList<BluetoothGattDescriptor>();


                    //Find all Report characteristics
                    for(BluetoothGattCharacteristic characteristic:allCharacteristics){
                        if(characteristic.getUuid().equals(UUIDDatabase.UUID_REP0RT)){
                            RDKCharacteristics.add(characteristic);
                        }
                    }

                    //Find all Report descriptors
                    for(BluetoothGattCharacteristic rdkcharacteristic:RDKCharacteristics){
                        List<BluetoothGattDescriptor> descriptors = rdkcharacteristic.
                                getDescriptors();
                        for(BluetoothGattDescriptor descriptor:descriptors){
                            RDKDescriptors.add(descriptor);
                        }
                    }
                    /**
                     * Wait for all  descriptors to receive
                     */
                    if(RDKDescriptors.size()==RDKCharacteristics.size()*2){

                        for(int pos=0,descPos=0;descPos<RDKCharacteristics.size();pos++,descPos++){
                            BluetoothGattCharacteristic rdkcharacteristic=
                                    RDKCharacteristics.get(descPos);
                            //Mapping the characteristic and descriptors
                            Logger.e("Pos-->"+pos);
                            Logger.e("Pos+1-->"+(pos+1));
                            BluetoothGattDescriptor clientdescriptor=RDKDescriptors.get(pos);
                            BluetoothGattDescriptor reportdescriptor=RDKDescriptors.get(pos+1);
                            if(!rdkcharacteristic.getDescriptors().contains(clientdescriptor))
                                rdkcharacteristic.addDescriptor(clientdescriptor);
                            if(!rdkcharacteristic.getDescriptors().contains(reportdescriptor))
                                rdkcharacteristic.addDescriptor(reportdescriptor);
                            pos++;
                        }
                    }
                    currentServiceData.put(LIST_UUID, gattService);
                    mGattServiceMasterData.add(currentServiceData);
                    mGattServiceData.add(currentServiceData);
                }else{
                    currentServiceData.put(LIST_UUID, gattService);
                    mGattServiceMasterData.add(currentServiceData);
                    mGattServiceData.add(currentServiceData);
                }

            }else {
                currentServiceData.put(LIST_UUID, gattService);
                mGattServiceMasterData.add(currentServiceData);
                mGattServiceData.add(currentServiceData);
            }

        }

        // Preparing list data
        // GAP and GATT attributes are not displayed
        mModifiedServiceData = new ArrayList<HashMap<String, BluetoothGattService>>();
        for (int i = 0; i < mGattServiceData.size(); i++) {
            if (!(mGattServiceData.get(i).get("UUID").getUuid()
                    .equals(UUIDDatabase.UUID_GENERIC_ATTRIBUTE_SERVICE) || mGattServiceData
                    .get(i).get("UUID").getUuid()
                    .equals(UUIDDatabase.UUID_GENERIC_ACCESS_SERVICE))) {
                mModifiedServiceData.add(mGattServiceData.get(i));
            }
        }

        //mApplication.setGattServiceMasterData(mGattServiceMasterData);
        if(mModifiedServiceData.size()>0){
            try {
                ConnectToService(0);
            }catch(Exception e){
                Logger.e(e.getMessage());
            }
        }else{
            showNoServiceDiscoverAlert();
        }


    }

    private void showNoServiceDiscoverAlert() {
        if(mNoserviceDiscovered!=null)
            mNoserviceDiscovered.setVisibility(View.VISIBLE);
    }

    public void ConnectToService(int pos)
    {
        mService = mModifiedServiceData.get(pos).get("UUID");
        mGattCharacteristics = mService.getCharacteristics();

        mApplication.setGattCharacteristics(mGattCharacteristics);

        if(mGattCharacteristics.size()>0){
            try {
                ConnectToCharacteristic(0);
            }catch(Exception e){
                Logger.e(e.getMessage());
            }
        }else{
            //showNoServiceDiscoverAlert();
        }

    }

    public void ConnectToCharacteristic(int pos){
        mApplication.setBluetoothgattcharacteristic(mGattCharacteristics
                .get(pos));

       // Go to main UI
            goToMain();
    }

    public void goToMain() {

        // Getting the current active fragment
        Intent intent = new Intent(this.getActivity(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("connected",true);
        startActivity(intent);
    }

}
