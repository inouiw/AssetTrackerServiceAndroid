package com.example.transporttracker;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Helper {
    public static DocumentReference getFirestoreDocumentForThisDevice() {
        // FirebaseInstanceId can be used as unique id of the app.
        String firebaseInstanceId = FirebaseInstanceId.getInstance().getId();
        return FirebaseFirestore.getInstance().document(String.format("locations/%s", firebaseInstanceId));
    }

    // If user is null then there will be a PERMISSION_DENIED exception when writing to the database.
    public static FirebaseUser getFirebaseUser() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        return mAuth.getCurrentUser();
    }

    public static void logToDatabase(@NonNull String tag, @NonNull DocumentReference thisDeviceDocRef, Boolean isError, @NonNull String msg) {
        if (isError) {
            Log.e(tag, msg);
        }
        else {
            Log.i(tag, msg);
        }
        Map<String, Object> logMsg = new HashMap<>();
        logMsg.put("level", isError ? "error" : "info");
        logMsg.put("message", msg);

        if (thisDeviceDocRef != null) {
            DocumentReference logMsgDocRef = thisDeviceDocRef.collection("log-messages").document(Long.toString(new Date().getTime()));

            logMsgDocRef.set(logMsg).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    //Boolean isPermissionDenied = e instanceof FirebaseFirestoreException
                    //        && ((FirebaseFirestoreException) e).getCode().name() == "PERMISSION_DENIED";

                    Log.e("Helper", "Error saving log message " + e.toString());
                }
            });
        }
    }
}
