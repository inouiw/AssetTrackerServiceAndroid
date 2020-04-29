package com.example.transporttracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.util.Log;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentReference;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST = 1;
    private static final int RC_SIGN_IN = 123;
    private static final String TAG = MainActivity.class.getSimpleName();

    private DocumentReference thisDeviceDocRef;
    private Boolean isAuthenticated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkUserIsAuthenticated();
    }

    private void checkUserIsAuthenticated() {
        FirebaseUser currentUser = Helper.getFirebaseUser();

        if (currentUser == null) {
            log(false, "User not authenticated.");
            setContentView(R.layout.activity_firebase_u_i);

            // https://firebase.google.com/docs/auth/android/firebaseui#sign_in
            // Choose authentication providers
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.EmailBuilder().build(),
                    new AuthUI.IdpConfig.PhoneBuilder().build(),
                    new AuthUI.IdpConfig.GoogleBuilder().build());

            // Create and launch sign-in intent
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .build(),
                    RC_SIGN_IN);
        }
        else {
            onAuthenticated();
        }
    }

    private void checkLocationAccessPermissions() {
        // Check GPS is enabled
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            log(false, "Show 'Please enable GPS location services' toast.");
            Toast.makeText(this, "Please enable GPS location services", Toast.LENGTH_SHORT).show();
            finish();
        }
        // Check location permission is granted - if it is, start
        // the service, otherwise request the permission
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            onLocationAccessPermissionGraned();
        } else {
            log(false, "Requesting location access permission.");
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    PERMISSIONS_REQUEST);
        }
    }

    private void startTrackerService() {
        startService(new Intent(this, TrackerService.class));
        finish();
    }

    private void onAuthenticated() {
        isAuthenticated = true;
        thisDeviceDocRef = Helper.getFirestoreDocumentForThisDevice();
        logAuthenticatedUserEmail();
        checkLocationAccessPermissions();
    }

    private void onLocationAccessPermissionGraned() {
        startTrackerService();
    }

    private void logAuthenticatedUserEmail() {
        FirebaseUser firebaseUser = Helper.getFirebaseUser();

        if (firebaseUser != null) {
            for (UserInfo profile : firebaseUser.getProviderData()) {
                String email = profile.getEmail();
                log(false, "User E-Mail: " + email);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST && grantResults.length == 1
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onLocationAccessPermissionGraned();
        } else {
            log(false, "Location access permission not granted. requestCode: " + requestCode
                    + ", grantResults.length: " + grantResults.length + ", grantResults[0]: "
                    + (grantResults.length == 0 ? "" : grantResults[0]));
            finish();
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
                String resAsStr = response == null ? "response: null" : response.getError() == null
                        ? "response.getError(): null"  : "response errorCode: " + response.getError().getErrorCode();
                log(true, "Sign in failed. resultCode: " + resultCode + ", " + resAsStr);
            }
        }
    }

    private void log(Boolean isError, @NonNull String msg) {
        if (!isAuthenticated)
        {
            if (isError) {
                Log.e(TAG, msg);
            }
            else {
                Log.i(TAG, msg);
            }
        }
        else {
            Helper.logToDatabase(TAG, thisDeviceDocRef, isError, msg);
        }
    }

}
