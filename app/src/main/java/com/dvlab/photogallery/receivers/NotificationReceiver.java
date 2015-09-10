package com.dvlab.photogallery.receivers;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

// This receiver is registered with -999 priority in manifest, so supposed to receive notification the latest.
// In particular after `VisibleFragment.onShowNotificationReceiver`
public class NotificationReceiver extends BroadcastReceiver {

    private static final String TAG = NotificationReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context c, Intent i) {
        Log.i(TAG, "received result: " + getResultCode());
        if (getResultCode() != Activity.RESULT_OK)
            // A foreground activity cancelled the broadcast
            return;

        int notificationId = i.getIntExtra("NOTIFICATION_ID", 0);
        Notification notification = (Notification) i.getParcelableExtra("NOTIFICATION");
        NotificationManager notificationManager = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, notification);
    }

}
