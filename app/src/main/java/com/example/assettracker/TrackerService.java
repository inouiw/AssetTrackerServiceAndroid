package com.example.assettracker;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import com.example.assettracker.Entities.MeasurementDoc;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.GeoPoint;

import java.util.Date;


public class TrackerService extends Service {
    private static final String TAG = TrackerService.class.getSimpleName();

    private FirebaseHelper firebaseHelper = null;

    private String appName() {
        return getString(R.string.app_name);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Thread.setDefaultUncaughtExceptionHandler((t, ex) -> logError("Uncaught exception: " + ex.toString()));
        logInfo("Service started.");

        FirebaseHelper.create(this.getApplicationContext()).addOnSuccessListener(firebaseHelperInstance -> {
            firebaseHelper = firebaseHelperInstance;
            buildNotification();
            requestLocationUpdates();
        });
    }

    private void onLocationUpdate(@NonNull Location location) {
        saveLocation(location);
    }

    private void saveLocation(@NonNull Location location) {
        Date locTime = new Date(location.getTime());
        MeasurementDoc lm = new MeasurementDoc(new Timestamp(locTime), new GeoPoint(location.getLatitude(), location.getLongitude()), location.getSpeed(),
                location.getBearing(), location.getAccuracy());
        firebaseHelper.saveMeasurementDoc(lm);
    }

    private void buildNotification() {
        String stop = "stop";
        registerReceiver(stopReceiver, new IntentFilter(stop));
        PendingIntent broadcastIntent = PendingIntent.getBroadcast(
                this, 0, new Intent(stop), PendingIntent.FLAG_UPDATE_CURRENT);
        String channelId = "";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel();
        }
        // Create the persistent notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this.getApplicationContext(), channelId)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_text))
                .setOngoing(true)
                .setContentIntent(broadcastIntent)
                .setSmallIcon(R.drawable.ic_tracker);
        // "After the system has created the service, the app has five seconds to call the
        // service's startForeground() method to show the new service's user-visible notification."
        // See https://developer.android.com/about/versions/oreo/background.html
        startForeground(1, notificationBuilder.build());
    }

    @NonNull
    @TargetApi(26)
    private synchronized String createNotificationChannel() {
        final String channelId = "com.example.assettracker";
        final String channelName = "Assettracker Background Service";
        NotificationChannel mChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW);
        mChannel.enableLights(true);
        mChannel.setLightColor(Color.BLUE);

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;
        notificationManager.createNotificationChannel(mChannel);
        return channelId;
    }

    protected BroadcastReceiver stopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            logInfo("received stop broadcast");
            // Stop the service when the notification is tapped
            unregisterReceiver(stopReceiver);
            stopSelf();
        }
    };

    private void requestLocationUpdates() {
        LocationRequest request = new LocationRequest();
        request.setInterval(10000);
        request.setFastestInterval(5000);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (permission == PackageManager.PERMISSION_GRANTED) {
            client.requestLocationUpdates(request, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    Location location = locationResult.getLastLocation();

                    if (location != null) {
                        onLocationUpdate(location);
                    }
                }
            }, null);
        }
    }

    // Writes the message to the log and to the firestore database.
    private void logInfo(@NonNull String msg) {
        firebaseHelper.logInfo(TAG, msg);
    }

    // Writes the message to the log and to the firestore database.
    private void logError(@NonNull String msg) {
        firebaseHelper.logError(TAG, msg);
    }

}