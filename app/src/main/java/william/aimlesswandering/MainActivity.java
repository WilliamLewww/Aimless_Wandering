package william.aimlesswandering;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
    LocationClass locationClass;
    LocationService locationService;

    Intent intent;

    public GoogleMap mMap;

    public PolylineOptions track;

    private Button button0;
    private Button button1;
    private Button button2;

    boolean isTracking = false;
    boolean serviceRunning = false;

    boolean isPaused = false;
    boolean isBound = false;

    SharedPreferences mPrefs;

    private NotificationManager mNotificationManager = null;
    private final NotificationCompat.Builder mNotificationBuilder = new NotificationCompat.Builder(this);

    private void setupNotifications() {
        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        PendingIntent pendingCloseIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP).setAction("close"), 0);

        mNotificationBuilder
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(getText(R.string.app_name))
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Test_0", pendingCloseIntent)
                .setOngoing(true);
    }

    private void showNotification() {
        mNotificationBuilder
                .setTicker(getText(R.string.common_google_play_services_enable_button))
                .setContentText("Location Service Running");
        if (mNotificationManager != null) {
            mNotificationManager.notify(1, mNotificationBuilder.build());
        }
    }

    private void startService() {
        if (serviceRunning == false) {
            ServiceConnection connection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    LocationService.LocationBinder binder = (LocationService.LocationBinder) service;
                    locationService = binder.getServiceSystem();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) { }
            };

            intent = new Intent(this, LocationService.class);
            if (isBound == false) {
                bindService(intent, connection, Context.BIND_AUTO_CREATE);
                isBound = true;
            }
            startService(intent);
        }

        serviceRunning = true;
    }

    private void stopService() {
        if (intent != null) {
            Log.e("Service", "Stopped");
            stopService(intent);
        }
    }

    public void start() {
        //loadTrack();

        if (mNotificationManager != null) {
            mNotificationManager.cancel(1);
        }

        stopService();
        if (locationClass.isConnected == true) {
            if (isTracking == false) { locationClass.stopLocationUpdates(); }
            else { locationClass.startLocationUpdates(); }
        }

        if (locationService != null) {
            locationService.locationClass.stopLocationUpdates();
            mMap.clear();
            if (locationService.locationClass.tempTrack.getPoints().size() > 0 && !track.getPoints().get(track.getPoints().size() - 1).equals(locationService.locationClass.tempTrack.getPoints().get(0))) {
                track.addAll(locationService.locationClass.tempTrack.getPoints().subList(1, locationService.locationClass.tempTrack.getPoints().size()));
            }
            else {
                track.addAll(locationService.locationClass.tempTrack.getPoints());
            }
            locationService.locationClass.tempTrack = new PolylineOptions();
            mMap.addPolyline(track);
            Log.e("Service", "Added");
        }

        isPaused = false;
    }

    public void pause() {
        //saveTrack();

        serviceRunning = false;
        if (isPaused == false) {
            if (isTracking == true) {
                startService();
                setupNotifications();
                showNotification();
            }
            locationClass.stopLocationUpdates();

            isPaused = true;
        }
    }

    private void setTrack() {
        track.width(5);
        track.color(Color.MAGENTA);
    }

    private void saveTrack() {
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(track);
        prefsEditor.putString("track", json);
        prefsEditor.commit();

        Log.e("File", "Saved");
    }

    private void loadTrack() {
        Gson gson = new Gson();
        String json = mPrefs.getString("track", "");
        track = gson.fromJson(json, PolylineOptions.class);

        Log.e("File", "Loaded");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        track = new PolylineOptions();

        mPrefs = getPreferences(MODE_PRIVATE);

        button0 = (Button)findViewById(R.id.button_0);
        button0.setBackgroundColor(Color.rgb(233,100,100));

        button0.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isTracking == false) {
                    locationClass.startLocationUpdates();
                    button0.setBackgroundColor(Color.CYAN);
                }
                else {
                    locationClass.stopLocationUpdates();
                    button0.setBackgroundColor(Color.rgb(233,100,100));
                }

                isTracking = !isTracking;
            }
        });

        button1 = (Button)findViewById(R.id.button_1);
        button1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                track = new PolylineOptions();
                setTrack();
                mMap.clear();
            }
        });

        button2 = (Button)findViewById(R.id.button_2);
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.e("Track", "" + track.getPoints().size());
            }
        });

        locationClass = new LocationClass(this);

        Log.e("MainActivity", "onCreate");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        setTrack();
        mMap.addPolyline(track);
    }

    @Override
    protected void onDestroy() {
        if (mNotificationManager != null) {
            mNotificationManager.cancel(1);
        }

        stopService();
        super.onDestroy();
    }
    @Override
    protected void onPause() { pause(); super.onPause(); }
    @Override
    protected void onStop() { pause(); super.onStop(); }
    @Override
    protected void onResume() { start(); super.onResume(); }
    @Override
    protected void onStart() {
        //start();
        super.onStart();
    }
}