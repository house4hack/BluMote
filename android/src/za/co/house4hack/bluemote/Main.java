package za.co.house4hack.bluemote;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class Main extends Activity {
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
   }
   
   public void setupChat() {
      // Initialize the BluetoothService to perform bluetooth connections
      mService = new BluetoothService(this, mHandler);      
   }
   
// The Handler that gets information back from the BluetoothService
   private final Handler mHandler = new Handler() {
       @Override
       public void handleMessage(Message msg) {
           switch (msg.what) {
           case MESSAGE_STATE_CHANGE:
               if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
               switch (msg.arg1) {
               case BluetoothService.STATE_CONNECTED:
                  Toast.makeText(getApplicationContext(), "Connected " + msg.getData().getString(TOAST),
                           Toast.LENGTH_SHORT).show();
                   break;
               case BluetoothService.STATE_CONNECTING:
                  Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                           Toast.LENGTH_SHORT).show();
                   break;
               case BluetoothService.STATE_LISTEN:
               case BluetoothService.STATE_NONE:
                  Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                           Toast.LENGTH_SHORT).show();
                   break;
               }
               break;
           case MESSAGE_WRITE:
               byte[] writeBuf = (byte[]) msg.obj;
               // construct a string from the buffer
               String writeMessage = new String(writeBuf);
               Toast.makeText(getApplicationContext(), writeMessage,
                        Toast.LENGTH_SHORT).show();
               break;
           case MESSAGE_READ:
               byte[] readBuf = (byte[]) msg.obj;
               // construct a string from the valid bytes in the buffer
               String readMessage = new String(readBuf, 0, msg.arg1);
               Toast.makeText(getApplicationContext(), mConnectedDeviceName+":  " + readMessage,
                        Toast.LENGTH_SHORT).show();
               break;
           case MESSAGE_DEVICE_NAME:
               // save the connected device's name
               mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
               Toast.makeText(getApplicationContext(), "Connected to "
                              + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
               break;
           case MESSAGE_TOAST:
               Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                              Toast.LENGTH_SHORT).show();
               break;
           }
       }
   };
   
}