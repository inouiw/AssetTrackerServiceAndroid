package com.example.assettracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseUser;

import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST = 1;
    private static final int RC_SIGN_IN = 123;
    private static final String TAG = MainActivity.class.getSimpleName();

    private FirebaseHelper firebaseHelper = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler((t, ex) -> logError("Uncaught exception: " + ex.toString()));
        logInfo("App started.");
        checkUserIsAuthenticated();
    }

    private void checkUserIsAuthenticated() {
        FirebaseUser currentUser = FirebaseHelper.getFirebaseUser();

        if (currentUser == null) {
            logInfo("User not authenticated.");
            setContentView(R.layout.activity_firebase_u_i);

            // https://firebase.google.com/docs/auth/android/firebaseui#sign_in
            // Choose authentication providers
            List<AuthUI.IdpConfig> providers = Collections.singletonList(
                    new AuthUI.IdpConfig.GoogleBuilder().build());

            // Create and launch sign-in intent
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .build(),
                    RC_SIGN_IN);
        } else {
            onAuthenticated();
        }
    }

    private void checkLocationAccessPermissions() {
        // Check GPS is enabled
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (lm == null) {
            String msg = "Error getting LOCATION_SERVICE.";
            logInfo(msg);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            return;
        }
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            String msg = "Please enable GPS location services";
            logInfo(String.format("Show toast '%s' toast.", msg));
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            return;
        }
        // Check location permission is granted - if it is, start
        // the service, otherwise request the permission
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            onLocationAccessPermissionGraned();
        } else {
            logInfo("Requesting location access permission.");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST);
        }
    }

    private void startTrackerService() {
        ContextCompat.startForegroundService(this, new Intent(this, TrackerService.class));
        finish();
    }

    private void onAuthenticated() {
        FirebaseHelper.create(this.getApplicationContext()).addOnSuccessListener(firebaseHelperInstance -> {
            firebaseHelper = firebaseHelperInstance;
            checkLocationAccessPermissions();
        });
    }

    private void onLocationAccessPermissionGraned() {
        startTrackerService();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST && grantResults.length == 1
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onLocationAccessPermissionGraned();
        } else {
            logError("Location access permission not granted. requestCode: " + requestCode
                    + ", grantResults.length: " + grantResults.length + ", grantResults[0]: "
                    + (grantResults.length == 0 ? "" : grantResults[0]));
            Toast.makeText(this, "Requesting location permission failed", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                onAuthenticated();
            } else {
                // Sign in failed. If response is null the user canceled the sign-in flow using the back button.
                // Otherwise check response.getError().getErrorCode() and handle the error.
                // If you get here and the user did not cancel check logcat output. I got the message:
                //   "GoogleSignInHandler: Developer error: this application is misconfigured. Check your SHA1 and package name in the Firebase console."
                //   See README.md for how to add the SHA certificate fingerprint.
                String msg = "Sign in failed. resultCode: " + resultCode ;
                msg += ", " + response == null ? "response: null" : response.getError() == null
                        ? "response.getError(): null" : "response errorCode: " + response.getError().getErrorCode()
                        + ", response Status: " + response.getError().getMessage(); // Example: "Code: 10, message: 10: " See CommonStatusCodes enum for meaning.

                logError(msg);
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
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
