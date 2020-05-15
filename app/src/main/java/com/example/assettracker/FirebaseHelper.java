package com.example.assettracker;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.assettracker.entities.DeviceDoc;
import com.example.assettracker.entities.LogMessageDoc;
import com.example.assettracker.entities.MeasurementDoc;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.annotations.NotNull;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Source;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.example.assettracker.entities.UserDoc;

import static com.example.assettracker.ValidationException.ensureTrue;

// Singleton class.
// Handles the communication with the google firestore database.
public class FirebaseHelper {
    private static final String TAG = FirebaseHelper.class.getSimpleName();
    private static Task<FirebaseHelper> instanceTask;
    private static FirebaseHelper instance = null;
    private static final ReentrantLock lock = new ReentrantLock();
    private static List<LogMessageDoc> unwrittenLogMessages = new ArrayList<>();
    private FirebaseFirestore db;
    private DocumentReference deviceReference;

    // Private so create() must be used.
    private FirebaseHelper() {
        this.db = FirebaseFirestore.getInstance();
        String authUserUid = getFirebaseUser().getUid();
        this.deviceReference = getFirestoreDocumentForThisDeviceReference(authUserUid);
    }

    // Returns a task that, when resolved, returns an instance of this class.
    // The user must be authenticated before calling this method or an exception is thrown.
    // Ensures that the user document exist. The device document contains no data so no check necessary.
    @NonNull
    public static Task<FirebaseHelper> create() {
        if (instanceTask == null) {
            // Hold lock till writeUnwrittenLogsToDatabase returns.
            lock.lock();
            try {
                if (instanceTask == null) {
                    ensureTrue(getFirebaseUser() != null, "This method may only be called after the user is authenticated.");
                    FirebaseHelper inst = new FirebaseHelper();
                    DocumentReference userRef = inst.getFirestoreDocumentForUserReference();
                    instanceTask = inst.saveDocIfNotExists(userRef, createUserDoc())
                            .continueWithTask(task -> inst.saveDocIfNotExists(inst.deviceReference, createDeviceDoc(inst.deviceReference.getId())))
                            .continueWith(task -> {
                                try {
                                    if (task.isSuccessful()) {
                                        instance = inst;
                                        inst.writeUnwrittenLogsToDatabase();
                                        logInfo(TAG, "Logged in.");
                                        return inst;
                                    } else {
                                        throw getException(task);
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
                // If you get here it probably means a developer error.
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

    @SuppressWarnings("UnusedReturnValue")
    @NonNull
    public Task<Void> saveMeasurementDoc(@NonNull MeasurementDoc doc) {
        String docName = Long.toString(doc.getTime().toDate().getTime());
        DocumentReference docRef = deviceReference.collection("location-measurements").document(docName);
        return saveDoc(docRef, doc, "saveMeasurementDoc");
    }

    private Task<Void> saveLogMessageDoc(@NonNull LogMessageDoc doc) {
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
                        return Tasks.forException(getException(task));
                    } else {
                        DocumentSnapshot res = task.getResult();
                        if (res == null || !res.exists()) {
                            return saveDoc(docRef, data, "saveDocIfNotExists");
                        } else {
                            return Tasks.forResult(null);
                        }
                    }
                });
    }

    @NonNull
    private OnFailureListener createFailureListener(@NonNull final String callerName) {
        return e -> Log.e(TAG, String.format("Firebase error. CallerName: %s", callerName), e);
    }

    @NonNull
    private DocumentReference getFirestoreDocumentForUserReference() {
        FirebaseUser fUser = getFirebaseUser();
        return db.document(String.format("users/%s", fUser.getUid()));
    }

    @NonNull
    private DocumentReference getFirestoreDocumentForThisDeviceReference(@NonNull String userUid) {
        // FirebaseInstanceId can be used as unique id of the app.
        String firebaseInstanceId = FirebaseInstanceId.getInstance().getId();
        return db.document(String.format("users/%s/devices/%s", userUid, firebaseInstanceId));
    }

    @NonNull
    private static UserDoc createUserDoc() {
        FirebaseUser fUser = getFirebaseUser();
        // Note getUid is NonNull.
        return new UserDoc(fUser.getUid(), fUser.getEmail(), fUser.getDisplayName());
    }

    @NonNull
    private static DeviceDoc createDeviceDoc(@NotNull String deviceId) {
        return new DeviceDoc(deviceId, Build.MANUFACTURER, Build.MODEL, Build.VERSION.SDK_INT);
    }

    public static void logInfo(@NonNull String tag, @NonNull String msg) {
        logToDatabase(tag, LogMessageDoc.LEVEL_INFO, msg);
    }

    public static void logError(@NonNull String tag, @NonNull String msg) {
        logToDatabase(tag, LogMessageDoc.LEVEL_ERROR, msg);
    }

    // Accepts messages to be written to the database without requiring an instance.
    // If the instance is not yet created it will collect the logs and write them when the instance is initialized.
    @SuppressWarnings("UnusedReturnValue")
    private static Task<Void> logToDatabase(@NonNull String tag, @NonNull String level, @NonNull String msg) {
        LogMessageDoc msgDoc = new LogMessageDoc(level, msg);
        Log.println(msgDoc.isError() ? Log.ERROR : Log.INFO, tag, msg);
        if (instance != null) {
            return instance.saveLogMessageDoc(msgDoc);
        }
        lock.lock();
        try {
            if (instance != null) {
                // This method is async. No lock required till completed because only unwrittenLogMessages needs protection.
                return instance.saveLogMessageDoc(msgDoc);
            }
            unwrittenLogMessages.add(msgDoc);
            return Tasks.forResult(null);
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

    public static String getMessageForUser(@NonNull Throwable e) {
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException fex = (FirebaseFirestoreException) e;
            String exCodeName = fex.getCode().name(); // eg: PERMISSION_DENIED or UNAVAILABLE
            return String.format("%s error when accessing firebase.", exCodeName);
        }
        return e.toString();
    }

    // Helper method that ensures that result of task.getException() is not null.
    @NonNull
    private static Exception getException(@NonNull Task task)
    {
        ensureTrue (!task.isSuccessful(), "getException may only be called for not successful tasks.");
        Exception ex = task.getException();
        return ex != null ? ex : new ValidationException("Task is not successful and has no exception. isCancelled: " + task.isCanceled());
    }

    public static boolean isConnectedToInternet(@NonNull Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

}
