package com.example.transporttracker;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Source;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.example.transporttracker.Entities.UserDoc;

import org.jetbrains.annotations.NotNull;

// Handles the communication with the google firestore database.
public class FirebaseHelper {
    private static final String TAG = FirebaseHelper.class.getSimpleName();
    private Context context;
    private Boolean isInitialized = false;
    // Firestore base document reference for the authenticated user and device.
    private DocumentReference baseDocRef = null;
    private FirebaseFirestore db;

    // Private so create() must be used.
    private FirebaseHelper(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
    }

    // Returns an instance of this class.
    // Ensures that the user document exist. The device document contains no data so no check necessary.
    // After the returned task completes successfully the instance of this class may be used.
    @NotNull
    public static Task<FirebaseHelper> create(@NonNull final Context context) {
        if (getFirebaseUser() == null) {
            throw new RuntimeException("Programming Error: This method may only be called after the user is authenticated.");
        }
        FirebaseHelper inst = new FirebaseHelper(context);
        DocumentReference userDocRef = inst.getFirestoreDocumentForUserReference();

        return inst.saveDocIfNotExists(userDocRef, createUserDoc())
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        inst.baseDocRef = inst.getFirestoreDocumentForThisDeviceReference();
                        inst.isInitialized = true;
                        inst.logToDatabase(TAG, false, "logged in");
                        return inst;
                    } else {
                        throw task.getException();
                    }
                });
    }

    // Gets the currently authenticated firebase user or null if no user was authenticated.
    // If user is null then there will be a PERMISSION_DENIED exception when writing to the database.
    public static FirebaseUser getFirebaseUser() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        // Returns null if no user was authenticated.
        return firebaseUser;
    }

    public Task<DocumentSnapshot> getDoc(@NonNull String subPath, Source source) {
        DocumentReference docRef = db.document(baseDocRef.getPath() + subPath);
        return getDoc(docRef, source);
    }

    @NotNull
    private Task<DocumentSnapshot> getDoc(@NonNull DocumentReference docRef, Source source) {
        return docRef.get(source).addOnFailureListener(createFailureListener("getDoc"));
    }

    public Task<Void> saveDoc(@NonNull String subPath, @NonNull Object data) {
        DocumentReference docRef = db.document(baseDocRef.getPath() + subPath);
        return saveDoc(docRef, data);
    }

    @NotNull
    private Task<Void> saveDoc(@NonNull DocumentReference docRef, @NonNull Object data) {
        return docRef.set(data).addOnFailureListener(createFailureListener("saveDoc"));
//                .addOnSuccessListener(aVoid -> {
//                    Log.i(TAG, "save success");
//                });
    }

    @NotNull
    private Task<Void> saveDocIfNotExists(@NonNull final DocumentReference docRef, @NonNull final Object data) {
        return getDoc(docRef, Source.SERVER)  // Force load from server so there will be an error if no permission.
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        return Tasks.forException(task.getException());
                    } else {
                        if (!task.getResult().exists()) {
                            return saveDoc(docRef, data);
                        } else {
                            return Tasks.forResult(null);
                        }
                    }
                });
    }

    @NotNull
    private OnFailureListener createFailureListener(@NonNull final String callerName) {
        return e -> {
            Log.e(TAG, String.format("Firebase error. CallerName: %s", callerName), e);

            if (e instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException fex = (FirebaseFirestoreException) e;
                String exCodeName = fex.getCode().name(); // eg: PERMISSION_DENIED or UNAVAILABLE
                String appName = context.getString(R.string.app_name);
                makeToast(String.format("%s: %s error when accessing firebase from %s.", appName, exCodeName, callerName));
            }
        };
    }

    @NotNull
    private DocumentReference getFirestoreDocumentForUserReference() {
        FirebaseUser fUser = getFirebaseUser();
        return db.document(String.format("users/%s", fUser.getUid()));
    }

    @NotNull
    private DocumentReference getFirestoreDocumentForThisDeviceReference() {
        // FirebaseInstanceId can be used as unique id of the app.
        String firebaseInstanceId = FirebaseInstanceId.getInstance().getId();
        return getFirestoreDocumentForUserReference().collection("devices").document(firebaseInstanceId);
    }

    @NotNull
    private static UserDoc createUserDoc() {
        FirebaseUser fUser = getFirebaseUser();
        // Note getUid is NonNull.
        return new UserDoc(fUser.getUid(), fUser.getEmail(), fUser.getDisplayName(), new Date().getTime());
    }

    public void logToDatabase(@NonNull String tag, @NotNull Boolean isError, @NonNull String msg) {
        if (isError) {
            Log.e(tag, msg);
        } else {
            Log.i(tag, msg);
        }
        if (!isInitialized) {
            throw new RuntimeException("Programming Error: Field isInitialized must be true before calling methods on FirebaseHelper.");
        }
        Map<String, Object> logMsg = new HashMap<>();
        logMsg.put("level", isError ? "error" : "info");
        logMsg.put("message", msg);

        saveDoc("/logs/" + new Date().getTime(), logMsg);
    }

    // Toast needs to work from Activity and from Service and from background tasks of each.
    public void makeToast(final String message) {
        new Thread(() -> {
            Handler handler = new Handler(context.getMainLooper());
            handler.post(() -> {
                Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    public Boolean isInitialized() {
        return isInitialized;
    }

}
