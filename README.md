# Asset Tracker Service for Android
This android app runs a background service that saves the device location, speed, direction and other measurements every 10 seconds to a google firestore database. 
The full location history is available for live or deferred analysis.


## Deploying the app to an android phone
1. Install Android Studio.
1. Clone or download this repository.
1. Create a Firebase account because data is saved there. The free account is sufficient. See https://console.firebase.google.com
    1. In Firebase crate a project, for example "asset-tracker".
    1. Navigate to Develop / Database  and open the Rules tab. There replace the line with `allow read, write ...` with `allow read, write: if request.auth.uid != null;` Click publish.
    1. Navigate to Authentication / Sign-in method and enable the Google sign-in provider.
    1. Navigate to <img src="https://storage.googleapis.com/support-kms-prod/vMSwtm9y2uvHQAg2OfjmWpsBMtG4xwSIPWxh" width="22" heigth="22"> Project settings. There click on the android icon on the bottom. For Android package name enter "com.example.transporttracker" then click Register app and then click Download google-services.json.
 1. Copy the downloaded google-services.json to the /app folder.
 1. Open the project in Android Studio.
 1. Connect your android device with a usb cable.
 1. Click Run or click Debug. The app will be compiled and be installed on your phone. A bus icon appears in the status bar to indicate that the app is running. Click it to stop the app.
 1. The app will ask you to log-in with your google account. This is only required the first time the app starts. Even after a reboot you do not need to log-in again.
 
 ## Accessing the location data
 To see the generated data, login to the firebase account (see above) then navigate to Database and open the Data tab. You can access the Google Firestore database from android, iPhone
 or a web app. Firestore can push data modifications to clients.
 
 ## Structure of the saved data
The organization of the data is:

`/users/{userId}/devices/{deviceId}/measurements/{timestamp}`

For example the measurement `/users/qDrkd0i898cjJhEo3MDJZaxe2gO/devices/f3SWYP9eSEWSFVfhIrRnAT/measurements/1588103575602` may contain:

``` javascript
{ 
    lattitude: 51.1214398,
    longitude: 8.9528029,
    horizontalAccuracyMeters: 22.291000366210938,
    bearing: 271.8037109375,
    speed: 0.10250464826822281,
    time: 1588103575602,
    day: 20200428
```

The user is at the root because the collected data belongs to the signed-in user and s/he should determine who can access it.
A user can have several devices so devices is a subcollection of the user.
The key for a measurement document is the timestamp of the measurement. It contains the fields lattitude, longitude, ...
The field day is included so an index can be created on the field to efficiently extract measurements for a given day.
The user document also includes the eMail, displayName and accountCreatedAt fields.

Log messages are also pushed to the server. These are saved at `/users/{userId}/devices/{deviceId}/logs/{timestamp}`
 
 ## Other things to know
 
 - The app will not start automatically after a reboot. You need to click the app icon "Asset Tracker". 
 - If the internet connection is lost, the location is recorded on the device and synced once the connection is reestablished.
 
  ### License
  MIT