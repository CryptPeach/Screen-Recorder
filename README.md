Add Permissions to Manifest : 

    <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION"/>
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
    //// Under <application>:
            <service
            android:name=".Activity.ScreenRecordingService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="mediaProjection" /> 
            
            <activity
            android:name=".Activity.Recorder"
            android:exported="true" />
   
            





