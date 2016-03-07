package axio.com.axioled;

import axio.com.axioled.BLEConnectionServices.BluetoothLeService;
import axio.com.axioled.CommonUtils.Logger;
import axio.com.axioled.CommonFragments.NavigationDrawerFragment;
import axio.com.axioled.CommonFragments.AboutFragment;
import axio.com.axioled.CommonFragments.ProfileScanningFragment;
import axio.com.axioled.CommonUtils.Utils;
import axio.com.axioled.CommonUtils.Constants;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.widget.DrawerLayout;
import android.widget.FrameLayout;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;


public class Settings extends FragmentActivity implements
        NavigationDrawerFragment.NavigationDrawerCallbacks {

    private static final int DRAWER_BLE = 0;
    private static final int DRAWER_ABOUT = 2;
    public static FrameLayout mContainerView;
    public static Boolean mApplicationInBackground = false;
    /**
     * Used to manage connections of the Blue tooth LE Device
     */
    private static BluetoothLeService mBluetoothLeService;
    private static DrawerLayout mParentView;
    /**
     * Code to manage Service life cycle.
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
                    .getService();
            // Initializing the service
            if (!mBluetoothLeService.initialize()) {
                Logger.d("Service not initialized");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
    String attachmentFileName = "attachment.cyacd";
    private boolean BLUETOOTH_STATUS_FLAG = true;
    private String Paired;
    private String Unpaired;
    // progress dialog variable
    private ProgressDialog mpdia;
    private AlertDialog mAlert;
    //Upgrade file catch
    private InputStream attachment = null;
    /**
     * Fragment managing the behaviors, interactions and presentation of the
     * navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;
    /**
     * Broadcast receiver for getting the bonding information
     */
    private BroadcastReceiver mBondStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            //Received when the bond state is changed
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {

                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                final int previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);

                if (state == BluetoothDevice.BOND_BONDING) {
                    // Bonding...
                    String dataLog2 = getResources().getString(R.string.dl_commaseparator)
                            + "[" + ProfileScanningFragment.mDeviceName + "|"
                            + ProfileScanningFragment.mDeviceAddress + "] " +
                            getResources().getString(R.string.dl_connection_pairing_request);
                    Logger.datalog(dataLog2);
                    Utils.bondingProgressDialog(Settings.this, mpdia, true);
                } else if (state == BluetoothDevice.BOND_BONDED) {
                    Logger.e("HomepageActivity--->Bonded");
                    Utils.stopDialogTimer();
                    // Bonded...
                    if (ProfileScanningFragment.mPairButton != null) {
                        ProfileScanningFragment.mPairButton.setText(Paired);
                        if(bondState == BluetoothDevice.BOND_BONDED && previousBondState == BluetoothDevice.BOND_BONDING) {
                            Toast.makeText(Settings.this, getResources().getString(R.string.toast_paired), Toast.LENGTH_SHORT).show();
                        }
                    }
                    String dataLog = getResources().getString(R.string.dl_commaseparator)
                            + "[" + ProfileScanningFragment.mDeviceName + "|"
                            + ProfileScanningFragment.mDeviceAddress + "] " +
                            getResources().getString(R.string.dl_connection_paired);
                    Logger.datalog(dataLog);
                    Utils.bondingProgressDialog(Settings.this, mpdia, false);

                } else if (state == BluetoothDevice.BOND_NONE) {
                    // Not bonded...
                    Logger.e("SettingsActivity--->Not Bonded");
                    Utils.stopDialogTimer();
                    if (ProfileScanningFragment.mPairButton != null) {
                        ProfileScanningFragment.mPairButton.setText(Unpaired);
                        if(bondState == BluetoothDevice.BOND_NONE && previousBondState == BluetoothDevice.BOND_BONDED) {
                            Toast.makeText(Settings.this, getResources().getString(R.string.toast_unpaired), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(Settings.this,
                                    getResources().getString(R.string.dl_connection_pairing_unsupported),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                    String dataLog = getResources().getString(R.string.dl_commaseparator)
                            + "[" + ProfileScanningFragment.mDeviceName + "|"
                            + ProfileScanningFragment.mDeviceAddress + "] " +
                            getResources().getString(R.string.dl_connection_pairing_unsupported);
                    Logger.datalog(dataLog);
                    Utils.bondingProgressDialog(Settings.this, mpdia, false);
                }else{
                    Logger.e("Error received in pair-->"+state);
                }
            }
            else  if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                Logger.i("BluetoothAdapter.ACTION_STATE_CHANGED.");
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) ==
                        BluetoothAdapter.STATE_OFF) {
                    Logger.i("BluetoothAdapter.STATE_OFF");
                    if (BLUETOOTH_STATUS_FLAG) {
                        connectionLostBluetoothalertbox(true);
                    }

                }
                else if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) ==
                        BluetoothAdapter.STATE_ON) {
                    Logger.i("BluetoothAdapter.STATE_ON");
                    if (BLUETOOTH_STATUS_FLAG) {
                        connectionLostBluetoothalertbox(false);
                    }

                }

            }
            else if(action.equals(BluetoothLeService.ACTION_PAIR_REQUEST)){
                Logger.e("Pair request received");
                Logger.e("SettingActivity--->Pair Request");
                Utils.stopDialogTimer();
            }

        }
    };

    /**
     * Method to detect whether the device is phone or tablet
     */
    private static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        if (isTablet(this)) {
            Logger.d("Tablet");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        } else {
            Logger.d("Phone");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        setContentView(R.layout.activity_settings);

        Paired = getResources().getString(R.string.bluetooth_pair);
        Unpaired = getResources().getString(R.string.bluetooth_unpair);

        mParentView = (DrawerLayout) findViewById(R.id.drawer_layout);
        mContainerView = (FrameLayout) findViewById(R.id.container);

        mpdia = new ProgressDialog(this);
        mpdia.setCancelable(false);

        mAlert = new AlertDialog.Builder(this).create();
        mAlert.setMessage(getResources().getString(
                R.string.alert_message_bluetooth_reconnect));
        mAlert.setCancelable(false);
        mAlert.setTitle(getResources().getString(R.string.app_name));
        mAlert.setButton(Dialog.BUTTON_POSITIVE, getResources().getString(
                R.string.alert_message_exit_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intentActivity = getIntent();
                finish();
                overridePendingTransition(
                        R.anim.slide_left, R.anim.push_left);
                startActivity(intentActivity);
                overridePendingTransition(
                        R.anim.slide_right, R.anim.push_right);
            }
        });
        mAlert.setCanceledOnTouchOutside(false);

        getTitle();

        Intent gattServiceIntent = new Intent(getApplicationContext(),
                BluetoothLeService.class);
        startService(gattServiceIntent);

        /**
         * Attaching the profileScanning fragment to start scanning for nearby
         * devices
         */

        ProfileScanningFragment profileScanningFragment = new ProfileScanningFragment();
        displayView(profileScanningFragment,
                Constants.PROFILE_SCANNING_FRAGMENT_TAG);

    }

    public void connectionLostBluetoothalertbox(Boolean status) {
        //Disconnected
        if (status) {
            mAlert.show();
        } else {
            if (mAlert != null && mAlert.isShowing())
                mAlert.dismiss();
        }

    }

    @Override
    protected void onPause() {
        getIntent().setData(null);
        // Getting the current active fragment
        Fragment currentFragment = getSupportFragmentManager()
                .findFragmentById(R.id.container);
        if (currentFragment instanceof ProfileScanningFragment || currentFragment instanceof
                AboutFragment) {
            Intent gattServiceIntent = new Intent(getApplicationContext(),
                    BluetoothLeService.class);
            stopService(gattServiceIntent);
        }
        mApplicationInBackground = true;
        BLUETOOTH_STATUS_FLAG = false;
        unregisterReceiver(mBondStateReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        Logger.e("onResume-->activity");
        try {
            catchUpgradeFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mApplicationInBackground = false;
        BLUETOOTH_STATUS_FLAG = true;
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_PAIR_REQUEST);
        registerReceiver(mBondStateReceiver, intentFilter);
        super.onResume();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Logger.e("newIntent");
        super.onNewIntent(intent);
        setIntent(intent);
    }

    /**
     * Handling the back pressed actions
     */
    @Override
    public void onBackPressed() {


    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        Logger.e("onNavigationDrawerItemSelected " + position);
        /**
         * Update the main content by replacing fragments with user selected
         * option
         */
        switch (position) {
            case DRAWER_BLE:
                /**
                 * BLE Devices
                 */
                if (BluetoothLeService.getConnectionState() == 2 ||
                        BluetoothLeService.getConnectionState() == 1 ||
                        BluetoothLeService.getConnectionState() == 4) {
                    BluetoothLeService.disconnect();
                }
                Intent intent = getIntent();
                finish();
                overridePendingTransition(R.anim.slide_left, R.anim.push_left);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_right, R.anim.push_right);

                break;
            case DRAWER_ABOUT:
                /**
                 * About
                 */
                AboutFragment aboutFragment = new AboutFragment();
                displayView(aboutFragment, Constants.ABOUT_FRAGMENT_TAG);

                break;
            default:
                break;
        }

    }

    /**
     * Used for replacing the main content of the view with provided fragments
     *
     * @param fragment
     * @param tag
     */
    void displayView(Fragment fragment, String tag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, fragment, tag).commit();
    }

    // Get intent, action and MIME type
    private void catchUpgradeFile() throws IOException, NullPointerException {
        Intent intent = getIntent();
        String action = intent.getAction();
        Uri data = intent.getData();

        File targetLocationparent = new File("/storage/emulated/0/CySmart");

        if (Intent.ACTION_VIEW.equalsIgnoreCase(action) && data != null) {
            if (intent.getScheme().compareTo("content") == 0) {
                try {
                    Cursor c = getContentResolver().query(
                            intent.getData(), null, null, null, null);
                    c.moveToFirst();
                    final int fileNameColumnId = c.getColumnIndex(
                            MediaStore.MediaColumns.DISPLAY_NAME);
                    if (fileNameColumnId >= 0)
                        attachmentFileName = c.getString(fileNameColumnId);
                    Logger.e("Filename>>>" + attachmentFileName);
                    // Fetch the attachment
                    attachment = getContentResolver().openInputStream(data);
                    if (attachment == null) {
                        Logger.e("onCreate" + "cannot access mail attachment");
                    } else {
                        if (fileExists(attachmentFileName, targetLocationparent)) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(Settings.this);
                            builder.setMessage(getResources().getString(R.string.alert_message_file_copy))
                                    .setCancelable(false)
                                    .setTitle(getResources().getString(R.string.app_name))
                                    .setPositiveButton(
                                            getResources()
                                                    .getString(R.string.alert_message_yes),
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    try {
                                                        FileOutputStream tmp = new FileOutputStream("/storage/emulated/0/CySmart" + File.separator + attachmentFileName);
                                                        byte[] buffer = new byte[1024];
                                                        int bytes = 0;
                                                        while ((bytes = attachment.read(buffer)) > 0)
                                                            tmp.write(buffer, 0, bytes);
                                                        tmp.close();
                                                        attachment.close();
                                                        getIntent().setData(null);
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            })
                                    .setNegativeButton(
                                            getResources().getString(
                                                    R.string.alert_message_no),
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    // Cancel the dialog box
                                                    dialog.cancel();
                                                    getIntent().setData(null);
                                                }
                                            });
                            AlertDialog alert = builder.create();
                            alert.show();
                        } else {
                            try {
                                FileOutputStream tmp = new FileOutputStream("/storage/emulated/0/CySmart" + File.separator + attachmentFileName);
                                byte[] buffer = new byte[1024];
                                int bytes = 0;
                                while ((bytes = attachment.read(buffer)) > 0)
                                    tmp.write(buffer, 0, bytes);
                                tmp.close();
                                attachment.close();
                                Toast.makeText(this, getResources().getString(R.string.toast_file_copied), Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                String sourcePath = data.getPath();
                Logger.e("Action>>>" + action + "Uri" + data.toString() + "Source path>>" + sourcePath);

                final File sourceLocation = new File(sourcePath);
                String sourceFileName = sourceLocation.getName();

                final File targetLocation = new File("/storage/emulated/0/CySmart" + File.separator + sourceFileName);

                if (fileExists(sourceFileName, targetLocationparent)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(
                            Settings.this);
                    builder.setMessage(getResources().getString(R.string.alert_message_file_copy))
                            .setCancelable(false)
                            .setTitle(getResources().getString(R.string.app_name))
                            .setPositiveButton(
                                    getResources()
                                            .getString(R.string.alert_message_yes),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            try {
                                                copyDirectory(sourceLocation, targetLocation);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    })
                            .setNegativeButton(
                                    getResources().getString(
                                            R.string.alert_message_no),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            // Cancel the dialog box
                                            dialog.cancel();
                                            getIntent().setData(null);
                                        }
                                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                } else {
                    try {
                        copyDirectory(sourceLocation, targetLocation);
                        Toast.makeText(this, getResources().getString(R.string.toast_file_copied), Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /*
    Checks whether a file exists in the folder specified
     */
    public boolean fileExists(String name, File file) {
        File[] list = file.listFiles();
        if (list != null)
            for (File fil : list) {
                if (fil.isDirectory()) {
                    fileExists(name, fil);
                } else if (name.equalsIgnoreCase(fil.getName())) {
                    Logger.e("File>>" + fil.getName());
                    return true;
                }
            }
        return false;
    }

    // If targetLocation does not exist, it will be created.
    public void copyDirectory(File sourceLocation, File targetLocation)
            throws IOException {
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }

            String[] children = sourceLocation.list();
            for (int i = 0; i < children.length; i++) {
                copyDirectory(new File(sourceLocation, children[i]),
                        new File(targetLocation, children[i]));
            }
        } else {

            InputStream in = new FileInputStream(sourceLocation.getAbsolutePath());
            OutputStream out = new FileOutputStream(targetLocation.getAbsolutePath());

            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            getIntent().setData(null);
        }
    }


    /**
     * Method to create an alert before user exit from the application
     */
    void alertbox() {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                Settings.this);
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
                                Settings.this.finish();
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

    //For UnPairing
    private void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);

        } catch (Exception e) {
            e.printStackTrace();
        }

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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void showWarningMessage() {
        AlertDialog alert;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.alert_message_clear_cache))
                .setTitle(getString(R.string.alert_title_clear_cache))
                .setCancelable(false)
                .setPositiveButton(getString(
                                R.string.alert_message_exit_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                if (BluetoothLeService.mBluetoothGatt != null)
                                    BluetoothLeService.refreshDeviceCache(BluetoothLeService.mBluetoothGatt);
                                BluetoothLeService.disconnect();
                                Toast.makeText(getBaseContext(),
                                        getString(R.string.alert_message_bluetooth_disconnect),
                                        Toast.LENGTH_SHORT).show();
                                Intent homePage = getIntent();
                                finish();
                                overridePendingTransition(R.anim.slide_right, R.anim.push_right);
                                startActivity(homePage);
                                overridePendingTransition(R.anim.slide_left, R.anim.push_left);

                            }
                        })
                .setNegativeButton(getString(
                        R.string.alert_message_exit_cancel), null);
        alert = builder.create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }

}


