package com.dvlab.photogallery.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.dvlab.photogallery.services.PollService;

// This fragment subscribes for "com.dvlab.photogallery.ACTION_NEW_PHOTOS_NOTIFICATION" messages when visible
// so it intercepts "new photos" message and cancels it. Otherwise message broadcasts further to `NotificationReceiver`
public class VisibleFragment extends Fragment {

    public static final String TAG = "VisibleFragment";
    private BroadcastReceiver onShowNotificationReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // If we receive this, we're visible, so cancel the notification
            Log.i(TAG, "canceling notification");
            setResultCode(Activity.RESULT_CANCELED);
        }

    };

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(PollService.ACTION_NEW_PHOTOS_NOTIFICATION);
        getActivity().registerReceiver(onShowNotificationReceiver, filter, PollService.PERM_PRIVATE, null);
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(onShowNotificationReceiver);
    }

}
