package za.co.house4hack.bluemote;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

// The Handler that gets information back from the BluetoothService
public class BluMoteHandler extends Handler {
   private static final String SHAC_OPEN = "za.co.house4hack.shac.OPEN";
   private static final String BATTERYFU = "sync://on";
   
   // Debugging
   private static final String TAG = "BlueMoteHandler";
   private static final boolean D = true;
   
   // Key names received from the BluetoothService Handler
   public static final String DEVICE_NAME = "device_name";
   public static final String TOAST = "toast";
   
   // Message types sent from the BluetoothService Handler
   public static final int MESSAGE_STATE_CHANGE = 1;
   public static final int MESSAGE_READ = 2;
   public static final int MESSAGE_WRITE = 3;
   public static final int MESSAGE_DEVICE_NAME = 4;
   public static final int MESSAGE_TOAST = 5;   

   protected BluetoothService context;

   // Name of the connected device
   private String mConnectedDeviceName = null;
   
   public BluMoteHandler(BluetoothService context) {
      this.context = context;
   }
   
   @Override
   public void handleMessage(Message msg) {
      switch (msg.what) {
         case MESSAGE_STATE_CHANGE:
            if (D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
            switch (msg.arg1) {
               case BluetoothService.STATE_CONNECTED:
                  Toast.makeText(context, "Connected " + msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                  sendBluetooth("*");
                  break;
               case BluetoothService.STATE_CONNECTING:
                  // Toast.makeText(getApplicationContext(),
                  // msg.getData().getString(TOAST),
                  // Toast.LENGTH_SHORT).show();
                  break;
               case BluetoothService.STATE_LISTEN:
               case BluetoothService.STATE_NONE:
                  // Toast.makeText(getApplicationContext(),
                  // msg.getData().getString(TOAST),
                  // Toast.LENGTH_SHORT).show();
                  break;
            }
            break;
         case MESSAGE_WRITE:
            byte[] writeBuf = (byte[]) msg.obj;
            // construct a string from the buffer
            String writeMessage = new String(writeBuf);
            if (D) Toast.makeText(context, writeMessage, Toast.LENGTH_SHORT).show();
            break;
         case MESSAGE_READ:
            byte[] readBuf = (byte[]) msg.obj;
            // construct a string from the valid bytes in the buffer
            String readMessage = new String(readBuf, 0, msg.arg1);
            if (D) Toast.makeText(context, mConnectedDeviceName + ":  " + readMessage, Toast.LENGTH_SHORT).show();
            processCommand(new String(readBuf));
            break;
         case MESSAGE_DEVICE_NAME:
            // save the connected device's name
            mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
            if (D && mConnectedDeviceName != null) {
               Toast.makeText(context, "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
            }
            break;
         case MESSAGE_TOAST:
            Toast.makeText(context, msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
            break;
      }
   }
   

   protected void processCommand(String cmd) {
      // vibrate to let user know we received command
      Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
      v.vibrate(250);
      
      // for now we support 4 buttons
      if (cmd.startsWith("1")) {
         if (isIntentAvailable(context, SHAC_OPEN)) {
            if (!isNetworkConnected(context) && isIntentAvailable(context, BATTERYFU)) {
               // enable data in case it's not. We'll do a sync, which will give us enough time
               context.sendBroadcast(new Intent(Intent.ACTION_VIEW, Uri.parse(BATTERYFU)));
               // we need to wait at least 10 seconds for data to connect
               new Thread() {
                  public void run() {
                     try { sleep(10*1000); } catch (Exception e) {}
                     post(new Runnable() {
                        @Override
                        public void run() {
                           Intent intent = new Intent(SHAC_OPEN);
                           intent.putExtra("access", "door");
                           intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                           context.startActivity(intent);
                        }
                     });
                  };
               }.start();
            } else {
               Intent intent = new Intent(SHAC_OPEN);
               intent.putExtra("access", "door");
               intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
               context.startActivity(intent);
            }
         } else {
            Toast.makeText(context, "SHAC is not installed", Toast.LENGTH_SHORT).show();
         }
      } else if (cmd.startsWith("2")) {
         soundAlarm(context);
      } else if (cmd.startsWith("3")) {
         recordAudio(context);
      } else if (cmd.startsWith("4")) {
         Intent intent = new Intent(Intent.ACTION_CALL);
         intent.setData(Uri.parse("tel:100"));
         intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         context.startActivity(intent);
      }
   }

   public boolean isIntentAvailable(Context context, String action) {
      final PackageManager packageManager = context.getPackageManager();
      final Intent intent = new Intent(action);
      List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
      if (resolveInfo.size() > 0) { return true; }
      return false;
   }

   /**
    * Returns true if there is a network connected
    * 
    * @param context
    * @return
    */
   public boolean isNetworkConnected(Context context) {
      boolean retVal = false;

      ConnectivityManager cm = (ConnectivityManager) context
               .getSystemService(context.CONNECTIVITY_SERVICE);
      if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting()) {
         retVal = true;
      } else {
         retVal = false;
      }

      return retVal;
   }   
   
   private void recordAudio(Context context) {
      final MediaRecorder recorder = new MediaRecorder();
      try {
         recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
         recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
         recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
         recorder.setOutputFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/BluMoteRecord" + new Date().getTime()
                  + ".3gp");
         recorder.prepare();
      } catch (IOException e1) {
         Toast.makeText(context, "Error recording" + e1.getMessage(), Toast.LENGTH_LONG).show();
      }

      recorder.start(); // Recording is now started

      Thread t = new Thread() {
         public void run() {
            try {
               sleep(10000);
            } catch (InterruptedException e) {
            }
            recorder.stop();
            recorder.reset(); // You can reuse the object by going back to
                              // setAudioSource() step
            recorder.release(); // Now the object cannot be reused
         };
      };
      t.start();
   }

   /**
    * Sound the anti-theft alarm
    * 
    * @param context
    */
   private void soundAlarm(Context context) {
      // turn up volume to full
      AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

      audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM),
               AudioManager.FLAG_PLAY_SOUND);
      audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
               AudioManager.FLAG_PLAY_SOUND);

      // play alarm sound file
      try {
         AssetFileDescriptor afd = context.getAssets().openFd("theft_alarm.mp3");
         final MediaPlayer player = new MediaPlayer();
         player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
         player.prepare();
         player.setLooping(true);
         player.start();

         Thread t = new Thread() {
            public void run() {
               try {
                  sleep(5000);
               } catch (InterruptedException e) {
               }
               player.stop();
            };
         };
         t.start();
      } catch (Exception e) {
         // aaah crap, I guess this device gets stolen :-(
         Log.e(TAG, "Error playing alarm", e);
      }
   }   

   /**
    * Sends a message.
    * 
    * @param message
    *           A string of text to send.
    */
   private void sendBluetooth(String message) {
      // Check that we're actually connected before trying anything
      if (context.getState() != BluetoothService.STATE_CONNECTED) { return; }

      // Check that there's actually something to send
      if (message.length() > 0) {
         // Get the message bytes and tell the BluetoothChatService to write
         byte[] send = message.getBytes();
         context.write(send);
      }
   }
}
