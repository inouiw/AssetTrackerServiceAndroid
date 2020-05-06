package com.example.assettracker;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.assettracker.Entities.LogMessageDoc;
import com.example.assettracker.Entities.MeasurementDoc;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Source;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.example.assettracker.Entities.UserDoc;

// Singleton class.
// Handles the communication with the google firestore database.
public class FirebaseHelper {
    private static final String TAG = FirebaseHelper.class.getSimpleName();
    private static Task<FirebaseHelper> instanceTask;
    private static FirebaseHelper instance = null;
    private static final ReentrantLock lock = new ReentrantLock();
    private static List<LogMessageDoc> unwrittenLogMessages = new ArrayList<>();
    private Context applicationContext;
    private FirebaseFirestore db;
    private DocumentReference userReference;
    private DocumentReference deviceReference;

    // Private so create() must be used.
    private FirebaseHelper(Context applicationContext) {
        this.applicationContext = applicationContext;
        this.db = FirebaseFirestore.getInstance();
        this.userReference = getFirestoreDocumentForUserReference();
        this.deviceReference = getFirestoreDocumentForThisDeviceReference();
    }

    // Returns a task that, when resolved, returns an instance of this class.
    // The user must be authenticated before calling this method or an exception is thrown.
    // Ensures that the user document exist. The device document contains no data so no check necessary.
    @NonNull
    public static Task<FirebaseHelper> create(@NonNull final Context applicationContext) {
        if (instanceTask == null) {
            lock.lock();
            try {
                if (instanceTask == null) {
                    if (getFirebaseUser() == null) {
                        throw new RuntimeException("Programming Error: This method may only be called after the user is authenticated.");
                    }
                    FirebaseHelper inst = new FirebaseHelper(applicationContext);
                    instanceTask = inst.saveDocIfNotExists(inst.userReference, createUserDoc())
                            .continueWith(task -> {
                                try {
                                    if (task.isSuccessful()) {
                                        instance = inst;
                                        inst.writeUnwrittenLogsToDatabase();
                                        logInfo(TAG, "logged in");
                                        return inst;
                                    } else {
                                        throw task.getException();
                                    }
                                } finally {
                                    // Usually the lock will be released in this continuation task
                                    // which runs after the create method ended.
                                    lock.unlock();
                                }
                            });
                } else {
                    lock.unlock();
                }
            } catch (Throwable t) {
                // Release lock here if there was an error and the task was never started.
                // If you get here it probably means a programming error.
                lock.unlock();
                throw t;
            }
        }
        return instanceTask;
    }

    // Gets the currently authenticated firebase user or null if no user was authenticated.
    // If user is null then there will be a PERMISSION_DENIED exception when writing to the database.
    public static FirebaseUser getFirebaseUser() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        // Returns null if no user was authenticated.
        return mAuth.getCurrentUser();
    }

    @NonNull
    public Task<DocumentSnapshot> getDoc(@NonNull DocumentReference docRef, Source source, @NonNull String callerName) {
        return docRef.get(source).addOnFailureListener(createFailureListener(callerName + " -> getDoc"));
    }

    @NonNull
    public Task<Void> saveMeasurementDoc(@NonNull MeasurementDoc doc) {
        doc.setCreatedByy(userReference);
        String docName = Long.toString(doc.getTime().toDate().getTime());
        DocumentReference docRef = deviceReference.collection("location-measurements").document(docName);
        return saveDoc(docRef, doc, "saveMeasurementDoc");
    }

    private Task<Void> saveLogMessageDoc(@NonNull LogMessageDoc doc) {
        String test = userReference.toString();
        doc.setGeneratedBy(userReference);
        String docName = Long.toString(doc.getTime().toDate().getTime());
        DocumentReference docRef = deviceReference.collection("logs").document(docName);
        return saveDoc(docRef, doc, "saveLogMessageDoc");
    }

    @NonNull
    public Task<Void> saveDoc(@NonNull DocumentReference docRef, @NonNull Object data, @NonNull String callerName) {
        return docRef.set(data).addOnFailureListener(createFailureListener(callerName + " -> saveDoc"));
    }

    @NonNull
    private Task<Void> saveDocIfNotExists(@NonNull final DocumentReference docRef, @NonNull final Object data) {
        return getDoc(docRef, Source.SERVER, "saveDocIfNotExists")  // Force load from server so there will be an error if no permission.
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        return Tasks.forException(task.getException());
                    } else {
                        if (!task.getResult().exists()) {
                            return saveDoc(docRef, data, "saveDocIfNotExists");
                        } else {
                            return Tasks.forResult(null);
                        }
                    }
                });
    }

    @NonNull
    private OnFailureListener createFailureListener(@NonNull final String callerName) {
        return e -> {
            Log.e(TAG, String.format("Firebase error. CallerName: %s", callerName), e);

            if (e instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException fex = (FirebaseFirestoreException) e;
                String exCodeName = fex.getCode().name(); // eg: PERMISSION_DENIED or UNAVAILABLE
                String appName = applicationContext.getString(R.string.app_name);
                showToast(String.format("%s: %s error when accessing firebase from %s.", appName, exCodeName, callerName));
            }
        };
    }

    @NonNull
    private DocumentReference getFirestoreDocumentForUserReference() {
        FirebaseUser fUser = getFirebaseUser();
        return db.document(String.format("users/%s", fUser.getUid()));
    }

    @NonNull
    private DocumentReference getFirestoreDocumentForThisDeviceReference() {
        // FirebaseInstanceId can be used as unique id of the app.
        String firebaseInstanceId = FirebaseInstanceId.getInstance().getId();
        return db.document(String.format("devices/%s", firebaseInstanceId));
    }

    @NonNull
    private static UserDoc createUserDoc() {
        FirebaseUser fUser = getFirebaseUser();
        // Note getUid is NonNull.
        return new UserDoc(fUser.getUid(), fUser.getEmail(), fUser.getDisplayName());
    }

    public static void logInfo(@NonNull String tag, @NonNull String msg) {
        logToDatabase(tag, LogMessageDoc.LEVEL_INFO, msg);
    }

    public static void logError(@NonNull String tag, @NonNull String msg) {
        logToDatabase(tag, LogMessageDoc.LEVEL_ERROR, msg);
    }

    // Accepts messages to be written to the database without requiring an instance.
    // If the instance is not yet created it will collect the logs and write them when the instance is initialized.
    private static void logToDatabase(@NonNull String tag, @NonNull String level, @NonNull String msg) {
        LogMessageDoc msgDoc = new LogMessageDoc(level, msg);
        Log.println(msgDoc.isError() ? Log.ERROR : Log.INFO, tag, msg);

        if (instance != null) {
            instance.saveLogMessageDoc(msgDoc);
            return;
        }
        lock.lock();
        try {
            if (instance != null) {
                instance.saveLogMessageDoc(msgDoc);
            }
            else {
                unwrittenLogMessages.add(msgDoc);
            }
        } finally {
            lock.unlock();
        }
    }

    private void writeUnwrittenLogsToDatabase() {
        for (LogMessageDoc msgDoc : unwrittenLogMessages) {
            saveLogMessageDoc(msgDoc);
        }
        unwrittenLogMessages.clear();
    }

    public void showToast(final String message) {
        showToast(applicationContext, message);
    }

    // Toast needs to work from Activity and from Service and from background tasks of each.
    public static void showToast(final Context applicationContext, final String message) {
        new Thread(() -> {
            Handler handler = new Handler(applicationContext.getMainLooper());
            handler.post(() -> Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show());
        }).start();
    }

}