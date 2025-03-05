package com.mtech.gosafe;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;


import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.mimik.mimoeclient.MimOEClient;
import com.mimik.mimoeclient.MimOERequestError;
import com.mimik.mimoeclient.MimOERequestResponse;
import com.mimik.mimoeclient.MimOEResponseHandler;
import com.mimik.mimoeclient.authobject.CombinedAuthResponse;
import com.mimik.mimoeclient.authobject.DeveloperTokenLoginConfig;
import com.mimik.mimoeclient.microserviceobjects.MicroserviceDeploymentConfig;
import com.mimik.mimoeclient.microserviceobjects.MicroserviceDeploymentStatus;
import com.mimik.mimoeclient.mimoeservice.MimOEConfig;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    MimOEClient mimOEClient;
    private static final int PERMISSION_FINE_LOCATION =99 ;

    String accessToken;
    String randomNumberRoot;
    Button getButton;
    // location request
    LocationRequest locationRequest;

    // current location
    Location currentLocation;

    // google api location services
    FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    Toolbar toolbar;
    TextView tv_lat, tv_long, tv_alt , tv_accu, tv_address;
    // list of saved locations
    List<Location> savedLocations;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
        // Instantiate new instance of mim OE runtime
        mimOEClient = new MimOEClient(this, new MimOEConfig().license(BuildConfig.MIM_OE_LICENSE));

        // Start mim OE using a new thread so as not to slow down activity creation
        Executors.newSingleThreadExecutor().execute(this::startMimOE);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            WindowInsetsCompat windowInsets = WindowInsetsCompat.toWindowInsetsCompat(Objects.requireNonNull(insets.toWindowInsets()), v);
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnApplyWindowInsetsListener((v, insets) -> {
            WindowInsetsCompat windowInsets = WindowInsetsCompat.toWindowInsetsCompat(insets, v);
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();
                if (itemId == R.id.nav_chat) {
                    selectedFragment = new ChatFragment();
                } else if (itemId == R.id.nav_map) {
                    selectedFragment = new MapsFragment();
                } else if (itemId == R.id.nav_notification) {
                    selectedFragment = new Fragment();
                }
                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.frame_layout, selectedFragment)
                            .commit();
                }
                return true;
            }
        });

        // Set default selection
        bottomNavigationView.setSelectedItemId(R.id.nav_map);

        getButton = findViewById(R.id.btn_get);
        getButton.setOnClickListener(this::onGetClicked);
        toolbar = findViewById(R.id.toolbar);
        View customToolbar = LayoutInflater.from(this).inflate(R.layout.search_toolbar, toolbar, false);
        toolbar.addView(customToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);
        tv_lat = toolbar.findViewById(R.id.tv_latitude);
        tv_long = toolbar.findViewById(R.id.tv_longitude);
        tv_alt = toolbar.findViewById(R.id.tv_altitude);
        tv_accu = toolbar.findViewById(R.id.tv_accuracy);
        tv_address = toolbar.findViewById(R.id.tv_address);

        tv_address.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddressDialog();
            }
        });

        // event that triggered whenever the update interval is met
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                // save the location
                updateUIValues(locationResult.getLastLocation());
            }
        };
        updateGPS();
    }
    private void startMimOE() {
        if (mimOEClient.startMimOESynchronously()) { // Start mim OE runtime
            runOnUiThread(() -> {
                Toast.makeText(
                        MainActivity.this,
                        "mim OE started!",
                        Toast.LENGTH_LONG).show();
            });
            authorizeMimOE();
        } else {
            runOnUiThread(() -> {
                Toast.makeText(
                        MainActivity.this,
                        "mim OE failed to start!",
                        Toast.LENGTH_LONG).show();
            });
        }
    }
    private void authorizeMimOE() {
        // Get the DEVELOPER_ID_TOKEN from the BuildConfig settings
        String developerIdToken = BuildConfig.DEVELOPER_ID_TOKEN;

        String clientId = BuildConfig.CLIENT_ID; // The Client ID

        // Create mimik configuration object for Developer ID Token login
        DeveloperTokenLoginConfig config = new DeveloperTokenLoginConfig();
        // Set the root URL
        config.setAuthorizationRootUri(BuildConfig.AUTHORIZATION_ENDPOINT);
        // Set the value for the DEVELOPER_ID_TOKEN
        config.setDeveloperToken(developerIdToken);
        // Set the value for the CLIENT_ID
        config.setClientId(clientId);
        // Login to the mimik Cloud
        mimOEClient.loginWithDeveloperToken(
                this,
                config,
                new MimOEResponseHandler() {
                    @Override
                    public void onError(MimOERequestError mimOERequestError) {
                        // Display error message
                        runOnUiThread(() -> {
                            Toast.makeText(
                                    MainActivity.this,
                                    "Error getting access token! " + mimOERequestError.getErrorMessage(),
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                    // A valid return makes the Access Token available by way of
                    // the method, mimOEClient.getCombinedAccessTokens()
                    @Override
                    public void onResponse(MimOERequestResponse mimOERequestResponse) {
                        // Get all the token that are stored within the
                        // mimOEClient
                        CombinedAuthResponse tokens = mimOEClient.getCombinedAccessTokens();
                        // Extract the Access Token from the tokens object and assign
                        // it to the class variable, accessToken
                        accessToken = tokens.getMimikTokens().getAccessToken();
                        runOnUiThread(() -> {
                            Toast.makeText(
                                    MainActivity.this,
                                    "Got access token!",
                                    Toast.LENGTH_LONG).show();
                        });
                        // Deploy mim OE microservice now that an access token
                        // has been generated
                        deployRandomNumberMicroservice();
                    }
                }
        );
    }
    private void deployRandomNumberMicroservice() {

        // Create microservice deployment configuration, dependent
        // on microservice implementation
        MicroserviceDeploymentConfig config = new MicroserviceDeploymentConfig();

        // set the name that will represent the microservice
        config.setName("randomnumber-v1");

        // Get the tar file that represents the mim OE microservice
        // but stored in the project's file system as a raw resource
        config.setResourceStream(getResources().openRawResource(R.raw.randomnumber_v1));

        // Set the filename that by which the mim OE client will identify
        // the microservice internally. This filename is associated internally
        // with the resource stream initialized above
        config.setFilename("randomnumber_v1.tar");

        // Declare the URI by  which the application code will access
        // the microservice
        config.setApiRootUri(Uri.parse("/randomnumber/v1"));

        // Deploy mim OE microservice using the client library instance variable
        MicroserviceDeploymentStatus status =
                mimOEClient.deployMimOEMicroservice(accessToken, config);
        if (status.error != null) {
            // Display microservice deployment error
            runOnUiThread(() -> {
                Toast.makeText(
                        MainActivity.this,
                        "Failed to deploy microservice! " + status.error.getMessage(),
                        Toast.LENGTH_LONG).show();
            });
        } else {
            // Store the microservice API root URI in the class variable,
            // randomNumberRoot
            randomNumberRoot = status.response.getContainer().getApiRootUri().toString();
            // Display a message indicating a successful microservice deployment
            runOnUiThread(() -> {
                Toast.makeText(
                        MainActivity.this,
                        "Successfully deployed microservice!",
                        Toast.LENGTH_LONG).show();
                getButton.setEnabled(true);
            });
        }
    }
    private void onGetClicked(View view) {
        if (randomNumberRoot == null || mimOEClient.getMimOEPort() == -1) {
            Toast.makeText(
                    MainActivity.this,
                    "mim OE is not ready yet!",
                    Toast.LENGTH_LONG).show();
            return;
        }
        // Construct an API request for the mim OE microservice
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(String.format(
                        "http://127.0.0.1:%d%s/randomNumber",
                        // use the client to get the default localhost port
                        mimOEClient.getMimOEPort(),
                        randomNumberRoot)) // root URI determined by microservice deployment
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(
                    @NotNull Call call,
                    @NotNull IOException e) {
                // Display microservice request error
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(
                            MainActivity.this,
                            "Failed to communicate with microservice! " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(
                    @NotNull Call call,
                    @NotNull final Response response) throws IOException {
                if (!response.isSuccessful()) {
                    // Display microservice unknown error
                    runOnUiThread(() -> {
                        Toast.makeText(
                                MainActivity.this,
                                "Microservice returned unexpected code! " + response,
                                Toast.LENGTH_LONG).show();
                    });
                } else {
                    // Display microservice response
                    runOnUiThread(() -> {
                        try {
                            Toast.makeText(
                                    MainActivity.this,
                                    "Got " + response.body().string(),
                                    Toast.LENGTH_LONG).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        });
    }
    // find location functionality
    private void updateGPS() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000); // Set the desired interval for active location updates, in milliseconds.
        locationRequest.setFastestInterval(5000); // Set the fastest rate for active location updates, in milliseconds.
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // Set the priority of the request.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    updateUIValues(location);
                    currentLocation = location;

//                    MyApplication myApplication = (MyApplication) getApplicationContext();
//                    savedLocations = myApplication.getLocations();
//
//                    if (savedLocations == null) {
//                        savedLocations = new ArrayList<>();
//                        myApplication.setLocations(savedLocations);
//                    }
//                    savedLocations.add(currentLocation);
                }
            });
        }
        else {
            // permission not granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch ((requestCode)){
            case PERMISSION_FINE_LOCATION:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    updateGPS();
                }
                else {
                    Toast.makeText(this, "This app requires permission to be granted in order to work properly", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    private void updateUIValues(Location location) {
        Geocoder geocoder = new Geocoder(MainActivity.this);
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                String cityName = address.getLocality();
                String stateName = address.getAdminArea();
                tv_address.setText(String.format("%s, %s", cityName, stateName));

                // save the city name to shared preferences for later use in home fragment
                SharedPreferences sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("cityName", cityName);
                editor.putString("stateName", stateName);

                editor.apply();
            }
        }
        catch (Exception e) {
            promptTurnOnLocation();

        }
    }
    private static final int REQUEST_CHECK_SETTINGS = 0x1;

    private void promptTurnOnLocation() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationProviderClient.getLastLocation().addOnSuccessListener(MainActivity.this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Update the UI with the location details
                            updateUIValues(location);
                        }
                    });
                }
                updateGPS();
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }

    private void showAddressDialog(){
        Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.location_details);
        TextView tvLongitude = dialog.findViewById(R.id.tv_longitude);
        TextView tvLatitude = dialog.findViewById(R.id.tv_latitude);
        TextView tvAltitude = dialog.findViewById(R.id.tv_altitude);
        TextView tvAccuracy = dialog.findViewById(R.id.tv_accuracy);
        TextView tvCityName = dialog.findViewById(R.id.tv_city_name);
        TextView tvFullAddress = dialog.findViewById(R.id.tv_full_address);
        TextView tvCountryCode = dialog.findViewById(R.id.tv_country_code);
        TextView tvCountryName = dialog.findViewById(R.id.tv_country_name);
        // Request the current location
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(MainActivity.this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    // Update the UI with the location details
                    Geocoder geocoder = new Geocoder(MainActivity.this);
                    try {
                        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        if (!addresses.isEmpty()) {
                            Address address = addresses.get(0);
                            tvLongitude.setText("Longitude: " + location.getLongitude());
                            tvLatitude.setText("Latitude: " + location.getLatitude());
                            tvAltitude.setText("Altitude: " + location.getAltitude());
                            tvAccuracy.setText("Accuracy: " + location.getAccuracy());
                            tvCityName.setText("City Name: " + address.getLocality());
                            tvFullAddress.setText("Full Address: " + address.getAddressLine(0));
                            tvCountryCode.setText("Country Code: " + address.getCountryCode());
                            tvCountryName.setText("Country Name: " + address.getCountryName());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        dialog.show();
    }
    @Override
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    updateUIValues(location);
                                }
                            });
                        }
                        if (states != null && states.isGpsUsable()) {
                            // GPS is turned on
                            Toast.makeText(this, "GPS is turned on", Toast.LENGTH_SHORT).show();
                        } else {
                            // GPS is turned off
                            Toast.makeText(this, "GPS is turned off", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        Toast.makeText(this, "Location services are required for this app to work properly", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
                break;
        }
    }

}