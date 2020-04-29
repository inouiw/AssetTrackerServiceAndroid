package com.example.transporttracker;

import android.app.Service;
import android.content.Intent;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.firestore.DocumentReference;

import com.example.transporttracker.Entities.LocationMeasurement;


public class TrackerService extends Service {

    private static final String TAG = TrackerService.class.getSimpleName();

    private DocumentReference thisDeviceDocRef;

    private String appName() { return getString(R.string.app_name); }

    @Override
    public IBinder onBind(Intent intent) {return null;}

    @Override
    public void onCreate() {
        super.onCreate();

        thisDeviceDocRef = Helper.getFirestoreDocumentForThisDevice();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable ex) {
                logToDatabase(true,"Uncaught exception: " + ex.toString());
            }
        });
        buildNotification();
        requestLocationUpdates();
    }

    private void onLocationUpdate(@NonNull Location location) {
        saveLocation(location);
    }

    private void saveLocation(@NonNull Location location) {
        DocumentReference measurementDocRef = thisDeviceDocRef.collection("loc-data").document(Long.toString(location.getTime()));

        LocationMeasurement lm = new LocationMeasurement(location.getTime(), location.getLatitude(), location.getLongitude(), location.getSpeed(),
                location.getBearing(), location.getAccuracy());

        measurementDocRef.set(lm).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                logToDatabase(true, "Error saving document " + e.toString());
            }
        });
    }

    private void buildNotification() {
        String stop = "stop";
        registerReceiver(stopReceiver, new IntentFilter(stop));
        PendingIntent broadcastIntent = PendingIntent.getBroadcast(
                this, 0, new Intent(stop), PendingIntent.FLAG_UPDATE_CURRENT);
        // Create the persistent notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, appName().replace(" ", ""))
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_text))
                .setOngoing(true)
                .setContentIntent(broadcastIntent)
                .setSmallIcon(R.drawable.ic_tracker);
        startForeground(1, notificationBuilder.build());
    }

    protected BroadcastReceiver stopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            logToDatabase(false, "received stop broadcast");
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

    private void logToDatabase(Boolean isError, @NonNull String msg) {
        Helper.logToDatabase(TAG, thisDeviceDocRef, isError, msg);
    }

}