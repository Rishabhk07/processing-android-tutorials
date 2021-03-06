package processing.test.androidwatchexport;

import android.app.Activity;
import java.util.ArrayList;
import android.content.DialogInterface;
import android.Manifest;
import android.content.pm.PackageManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v4.os.ResultReceiver; 
        
import processing.android.PWatchFaceCanvas;
import processing.core.PApplet;
        
public class MainService extends PWatchFaceCanvas {
  private static final String[] permissions = {Manifest.permission.WAKE_LOCK};
  private static final int REQUEST_PERMISSIONS = 1;
  private static final String KEY_RESULT_RECEIVER = "resultReceiver";
  private static final String KEY_PERMISSIONS = "permissions";
  private static final String KEY_GRANT_RESULTS = "grantResults";
  private static final String KEY_REQUEST_CODE = "requestCode";      
  
  public MainService() {
    super();
    PApplet sketch = new AndroidWatchExport();
    setSketch(sketch);
  }
  
  // https://developer.android.com/training/articles/wear-permissions.html
  // Inspired by PermissionHelper.java from Michael von Glasow:
  // https://github.com/mvglasow/satstat/blob/master/src/com/vonglasow/michael/satstat/utils/PermissionHelper.java
  // Example of use:
  // https://github.com/mvglasow/satstat/blob/master/src/com/vonglasow/michael/satstat/PasvLocListenerService.java  
  @Override
  public void requestPermissions() { 
    ArrayList<String> needed = new ArrayList<String>();
    int check;
    boolean danger = false;
    for (String p: permissions) {
      check = ContextCompat.checkSelfPermission(this, p);
      if (check != PackageManager.PERMISSION_GRANTED) {
        needed.add(p);
      } else {
        danger = true;
      }
    }
  
    if (!needed.isEmpty()) {
      requestPermissions(needed.toArray(new String[needed.size()]), REQUEST_PERMISSIONS);
    } else if (danger) {
      onPermissionsGranted();
    }    
  }
      
  // The event handler for the permission result
  public void onRequestPermissionsResult(int requestCode,
                                         String permissions[], int[] grantResults) {      
    if (requestCode == REQUEST_PERMISSIONS) {      
      if (grantResults.length > 0) {
        boolean granted = true;
        for (int i = 0; i < grantResults.length; i++) {
          if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {         
            granted = false;
            break;
          }
        }
        if (granted) onPermissionsGranted();
      }
    }    
  }       
      
  // requestPermissions() method for services
  public void requestPermissions(String[] permissions, int requestCode) {
    ResultReceiver resultReceiver = new ResultReceiver(new Handler(Looper.getMainLooper())) {
    @Override
      protected void onReceiveResult (int resultCode, Bundle resultData) {
        String[] outPermissions = resultData.getStringArray(KEY_PERMISSIONS);
        int[] grantResults = resultData.getIntArray(KEY_GRANT_RESULTS);
        onRequestPermissionsResult(resultCode, outPermissions, grantResults);
      }
    };
    final Intent permIntent = new Intent(this, PermissionRequestActivity.class);
    permIntent.putExtra(KEY_RESULT_RECEIVER, resultReceiver);
    permIntent.putExtra(KEY_PERMISSIONS, permissions);
    permIntent.putExtra(KEY_REQUEST_CODE, requestCode);  

    // Create a notification on watch faces, otherwise it does not work.
    TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
    stackBuilder.addNextIntent(permIntent);
    PendingIntent permPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
        .setSmallIcon(R.drawable.icon)
        .setContentTitle("Requesting permissions")
        .setContentText("The watch face need permissions to work properly")
        .setOngoing(true)
        .setAutoCancel(true)
        .setWhen(0)
        .setContentIntent(permPendingIntent)
        .setStyle(null);
    NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    notificationManager.notify(requestCode, builder.build());
  }    
    
  public static class PermissionRequestActivity extends Activity {
    ResultReceiver resultReceiver;
    String[] permissions;
    int requestCode;
    @Override
    protected void onStart() {
      super.onStart();
      resultReceiver = this.getIntent().getParcelableExtra(KEY_RESULT_RECEIVER);
      permissions = this.getIntent().getStringArrayExtra(KEY_PERMISSIONS);
      requestCode = this.getIntent().getIntExtra(KEY_REQUEST_CODE, 0);
      ActivityCompat.requestPermissions(this, permissions, requestCode);
    }    
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
      Bundle resultData = new Bundle();
      resultData.putStringArray(KEY_PERMISSIONS, permissions);
      resultData.putIntArray(KEY_GRANT_RESULTS, grantResults);
      resultReceiver.send(requestCode, resultData);
      finish();
    }
  }
}
