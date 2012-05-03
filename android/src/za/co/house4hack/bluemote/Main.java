package za.co.house4hack.bluemote;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class Main extends Activity {
   private static final String BLUEMOTE_NAME_PREFIX = "BluMote";

   // Debugging
   private static final String TAG = "BlueMote";
   private static final boolean D = true;

   // Intent request codes
   private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
   private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
   private static final int REQUEST_ENABLE_BT = 3;

   // Array adapter for the conversation thread
   private BluetoothAdapter mBluetoothAdapter = null;
   // Member object for the chat services
   private BluetoothAdapter mBtAdapter;

   private ProgressDialog pd = null;
   private boolean pairing = false;

   private BluetoothService mService = null;
   public static boolean mIsBound;
   private ServiceConnection mConnection = new ServiceConnection() {
      public void onServiceConnected(ComponentName className, IBinder service) {
         BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
         mService = binder.getService();
      }

      public void onServiceDisconnected(ComponentName className) {
         mService = null;
      }
   };

   void doBindService() {
      // Establish a connection with the service. We use an explicit
      // class name because we want a specific service implementation that
      // we know will be running in our own process (and thus won't be
      // supporting component replacement by other applications).
      bindService(new Intent(Main.this, BluetoothService.class), mConnection, Context.BIND_AUTO_CREATE);
      mIsBound = true;
   }

   void doUnbindService() {
      if (mIsBound) {
         // Detach our existing connection.
         unbindService(mConnection);
         mIsBound = false;
      }
   }

   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);

      // Get local Bluetooth adapter
      mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

      // If the adapter is null, then Bluetooth is not supported
      if (mBluetoothAdapter == null) {
         Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
         finish();
         return;
      }
   }

   @Override
   protected void onStart() {
      // TODO Auto-generated method stub
      super.onStart();

      // If BT is not on, request that it be enabled.
      // setupChat() will then be called during onActivityResult
      if (!mBluetoothAdapter.isEnabled()) {
         Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
         startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
         // Otherwise, setup the chat session
      } else {
         if (mService == null) doBindService();
      }
   }
//
//   @Override
//   protected void onResume() {
//      // TODO Auto-generated method stub
//      super.onResume();
//
//      // Performing this check in onResume() covers the case in which BT was
//      // not enabled during onStart(), so we were paused to enable it...
//      // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
//      if (mService != null) {
//         // Only if the state is STATE_NONE, do we know that we haven't started
//         // already
//         if (mService.getState() == BluetoothService.STATE_NONE) {
//            // Start the Bluetooth chat services
//            mService.start();
//         }
//      }
//   }

   @Override
   protected void onDestroy() {
      // TODO Auto-generated method stub
      super.onDestroy();
      // Stop the Bluetooth chat services
      // if (mService != null) mService.stop();
      doUnbindService();
      
      try {
         unregisterReceiver(mReceiver);
      } catch (Exception e) {
      }
   }

   public void onActivityResult(int requestCode, int resultCode, Intent data) {
      if (D) Log.d(TAG, "onActivityResult " + resultCode);
      switch (requestCode) {
         case REQUEST_CONNECT_DEVICE_SECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
               // connectDevice(data, true);
            }
            break;
         case REQUEST_CONNECT_DEVICE_INSECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
               // connectDevice(data, false);
            }
            break;
         case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
               // Bluetooth is now enabled, so set up a chat session
               doBindService();
            } else {
               // User did not enable Bluetooth or an error occurred
               Log.d(TAG, "BT not enabled");
               Toast.makeText(this, R.string.msg_bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
               finish();
            }
      }
   }

   public void connectDevice(final BluetoothDevice device, final boolean secure) {
      if (mService != null) {
         mService.connect(device, secure);
      } else {
         Log.d(TAG, "mService null, can't connect to " + device.getName());
      }
   }

   /**
    * User wants to connect to a BlueMote device
    * 
    * @param v
    */
   public void onConnect(View v) {
      new AlertDialog.Builder(this).setTitle("Connect BlueMotes").setMessage("Please press button 1 and 2 on your BlueMotes to pair them")
               .setPositiveButton("Ready", new OnClickListener() {

                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                     dialog.dismiss();
                     pairing = true;

                     // Register for broadcasts when a device is discovered
                     IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                     registerReceiver(mReceiver, filter);

                     // Register for broadcasts when discovery has finished
                     filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                     registerReceiver(mReceiver, filter);

                     // Get the local Bluetooth adapter
                     mBtAdapter = BluetoothAdapter.getDefaultAdapter();

                     // Get a set of currently paired devices
                     Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
                     for (BluetoothDevice d : pairedDevices) {
                        if (d.getName().startsWith(BLUEMOTE_NAME_PREFIX)) {
                           // found our device, connect to it
                           Log.d(TAG, "Found device " + d.getName() + " - " + d.getAddress());
                           connectDevice(d, true);
                        }
                     }

                     doDiscovery();
                  }
               }).create().show();
   }

   public void onDisconnect(View v) {
      finish();
   }

   private void doDiscovery() {
      if (pd != null) {
         pd.dismiss();
      }

      pd = new ProgressDialog(this);
      pd.setMessage(getString(R.string.msg_scanning));
      pd.setIndeterminate(true);
      pd.show();

      // If we're already discovering, stop it
      if (mBtAdapter.isDiscovering()) {
         mBtAdapter.cancelDiscovery();
      }

      // Request discover from BluetoothAdapter
      mBtAdapter.startDiscovery();
   }

   // The BroadcastReceiver that listens for discovered devices and
   // changes the title when discovery is finished
   private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
         String action = intent.getAction();

         // When discovery finds a device
         if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            // Get the BluetoothDevice object from the Intent
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            // If it's already paired, skip it, because it's been listed already
            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
               if (device.getName().startsWith(BLUEMOTE_NAME_PREFIX)) {
                  Log.d(TAG, "Found unpaired device " + device.getName() + " - " + device.getAddress());
                  connectDevice(device, false);
               }
            }
            // When discovery is finished, change the Activity title
         } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            if (pd != null) pd.dismiss();
            unregisterReceiver(mReceiver);
            pairing = false;
         }
      }
   };

}