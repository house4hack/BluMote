package za.co.house4hack.bluemote;

import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class Main extends Activity {
   private static final String BLUEMOTE_NAME_PREFIX = "BlueMote";

   // Debugging
   private static final String TAG = "BlueMote";
   private static final boolean D = true;

   // Message types sent from the BluetoothService Handler
   public static final int MESSAGE_STATE_CHANGE = 1;
   public static final int MESSAGE_READ = 2;
   public static final int MESSAGE_WRITE = 3;
   public static final int MESSAGE_DEVICE_NAME = 4;
   public static final int MESSAGE_TOAST = 5;

   // Key names received from the BluetoothService Handler
   public static final String DEVICE_NAME = "device_name";
   public static final String TOAST = "toast";

   // Intent request codes
   private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
   private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
   private static final int REQUEST_ENABLE_BT = 3;

   // Name of the connected device
   private String mConnectedDeviceName = null;
   // Array adapter for the conversation thread
   private BluetoothAdapter mBluetoothAdapter = null;
   // Member object for the chat services
   private BluetoothService mService = null;
   private BluetoothAdapter mBtAdapter;

   private ProgressDialog pd = null;
   private boolean pairing = false;

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
         if (mService == null) setupChat();
      }
   }

   @Override
   protected void onResume() {
      // TODO Auto-generated method stub
      super.onResume();

      // Performing this check in onResume() covers the case in which BT was
      // not enabled during onStart(), so we were paused to enable it...
      // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
      if (mService != null) {
         // Only if the state is STATE_NONE, do we know that we haven't started
         // already
         if (mService.getState() == BluetoothService.STATE_NONE) {
            // Start the Bluetooth chat services
            mService.start();
         }
      }
   }

   @Override
   protected void onDestroy() {
      // TODO Auto-generated method stub
      super.onDestroy();
      // Stop the Bluetooth chat services
      if (mService != null) mService.stop();
      try { unregisterReceiver(mReceiver); } catch (Exception e) {}
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
               setupChat();
            } else {
               // User did not enable Bluetooth or an error occurred
               Log.d(TAG, "BT not enabled");
               Toast.makeText(this, R.string.msg_bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
               finish();
            }
      }
   }

   public void setupChat() {
      // Initialize the BluetoothService to perform bluetooth connections
      mService = new BluetoothService(this, mHandler);
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
      new AlertDialog.Builder(this).setTitle("Connect BlueMotes")
      .setMessage("Please press button 1 and 2 on your BlueMotes to pair them")
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
                  Log.d(TAG, "Found device " + d.getName() + " - " + d.getUuids());
                  connectDevice(d, true);
               }
            }

            doDiscovery();
         }
      })
      .create().show();
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

   // The Handler that gets information back from the BluetoothService
   private final Handler mHandler = new Handler() {
      @Override
      public void handleMessage(Message msg) {
         switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
               if (D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
               switch (msg.arg1) {
                  case BluetoothService.STATE_CONNECTED:
                     Toast.makeText(getApplicationContext(),
                              "Connected " + msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
                              .show();
                     if (pairing) sendBluetooth("*");
                     break;
                  case BluetoothService.STATE_CONNECTING:
//                     Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
//                              Toast.LENGTH_SHORT).show();
                     break;
                  case BluetoothService.STATE_LISTEN:
                  case BluetoothService.STATE_NONE:
//                     Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
//                              Toast.LENGTH_SHORT).show();
                     break;
               }
               break;
            case MESSAGE_WRITE:
               byte[] writeBuf = (byte[]) msg.obj;
               // construct a string from the buffer
               String writeMessage = new String(writeBuf);
               Toast.makeText(getApplicationContext(), writeMessage, Toast.LENGTH_SHORT).show();
               break;
            case MESSAGE_READ:
               byte[] readBuf = (byte[]) msg.obj;
               // construct a string from the valid bytes in the buffer
               String readMessage = new String(readBuf, 0, msg.arg1);
               Toast.makeText(getApplicationContext(), mConnectedDeviceName + ":  " + readMessage,
                        Toast.LENGTH_SHORT).show();
               break;
            case MESSAGE_DEVICE_NAME:
               // save the connected device's name
               mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
               Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName,
                        Toast.LENGTH_SHORT).show();
               break;
            case MESSAGE_TOAST:
               Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                        Toast.LENGTH_SHORT).show();
               break;
         }
      }
   };

   /**
    * Sends a message.
    * 
    * @param message
    *           A string of text to send.
    */
   private void sendBluetooth(String message) {
      // Check that we're actually connected before trying anything
      if (mService.getState() != BluetoothService.STATE_CONNECTED) { return; }

      // Check that there's actually something to send
      if (message.length() > 0) {
         // Get the message bytes and tell the BluetoothChatService to write
         byte[] send = message.getBytes();
         mService.write(send);
      }
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
                  Log.d(TAG,
                           "Found unpaired device " + device.getName() + " - " + device.getUuids());
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