//package com.mtech.gosafe;
//
//    import android.Manifest;
//    import android.content.pm.PackageManager;
//    import android.graphics.Color;
//    import android.location.Address;
//    import android.location.Geocoder;
//    import android.location.Location;
//    import android.os.Bundle;
//    import android.view.LayoutInflater;
//    import android.view.View;
//    import android.view.ViewGroup;
//    import android.widget.Toast;
//
//    import androidx.annotation.NonNull;
//    import androidx.annotation.Nullable;
//    import androidx.core.app.ActivityCompat;
//    import androidx.fragment.app.Fragment;
//
//    import com.google.android.gms.location.FusedLocationProviderClient;
//    import com.google.android.gms.location.LocationCallback;
//    import com.google.android.gms.location.LocationRequest;
//    import com.google.android.gms.location.LocationResult;
//    import com.google.android.gms.location.LocationServices;
//    import com.google.android.gms.maps.CameraUpdateFactory;
//    import com.google.android.gms.maps.GoogleMap;
//    import com.google.android.gms.maps.OnMapReadyCallback;
//    import com.google.android.gms.maps.SupportMapFragment;
//    import com.google.android.gms.maps.model.LatLng;
//    import com.google.android.gms.maps.model.MarkerOptions;
//    import com.google.android.gms.maps.model.PolylineOptions;
//    import com.mimik.mimoeclient.MimOEClient;
//    import com.mimik.mimoeclient.MimOERequestError;
//    import com.mimik.mimoeclient.MimOERequestResponse;
//    import com.mimik.mimoeclient.MimOEResponseHandler;
//    import com.mimik.mimoeclient.authobject.CombinedAuthResponse;
//    import com.mimik.mimoeclient.authobject.DeveloperTokenLoginConfig;
//    import com.mimik.mimoeclient.mimoeservice.MimOEConfig;
//
//    import java.io.IOException;
//    import java.util.ArrayList;
//    import java.util.List;
//    import java.util.concurrent.Executors;
//
//    import okhttp3.Call;
//    import okhttp3.Callback;
//    import okhttp3.OkHttpClient;
//    import okhttp3.Request;
//    import okhttp3.Response;
//
//    public class MapsFragment extends Fragment {
//
//        private static final int PERMISSION_FINE_LOCATION = 99;
//        private MimOEClient mimOEClient;
//        private String accessToken;
//        private FusedLocationProviderClient fusedLocationProviderClient;
//        private LocationCallback locationCallback;
//        private LocationRequest locationRequest;
//        private GoogleMap mMap;
//
//        private OnMapReadyCallback callback = new OnMapReadyCallback() {
//            @Override
//            public void onMapReady(GoogleMap googleMap) {
//                mMap = googleMap;
//                updateGPS();
//            }
//        };
//
//        @Nullable
//        @Override
//        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//            View view = inflater.inflate(R.layout.fragment_maps, container, false);
//
//            mimOEClient = new MimOEClient(getContext(), new MimOEConfig().license(BuildConfig.MIM_OE_LICENSE));
//            Executors.newSingleThreadExecutor().execute(this::startMimOE);
//
//            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
//
//            locationCallback = new LocationCallback() {
//                @Override
//                public void onLocationResult(@NonNull LocationResult locationResult) {
//                    super.onLocationResult(locationResult);
//                    updateMapLocation(locationResult.getLastLocation());
//                }
//            };
//
//            return view;
//        }
//
//        @Override
//        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//            super.onViewCreated(view, savedInstanceState);
//            SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
//            if (mapFragment != null) {
//                mapFragment.getMapAsync(callback);
//            }
//        }
//
//        private void startMimOE() {
//            if (mimOEClient.startMimOESynchronously()) {
//                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "mim OE started!", Toast.LENGTH_LONG).show());
//                authorizeMimOE();
//            } else {
//                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "mim OE failed to start!", Toast.LENGTH_LONG).show());
//            }
//        }
//
//        private void authorizeMimOE() {
//            String developerIdToken = BuildConfig.DEVELOPER_ID_TOKEN;
//            String clientId = BuildConfig.CLIENT_ID;
//
//            DeveloperTokenLoginConfig config = new DeveloperTokenLoginConfig();
//            config.setAuthorizationRootUri(BuildConfig.AUTHORIZATION_ENDPOINT);
//            config.setDeveloperToken(developerIdToken);
//            config.setClientId(clientId);
//
//            mimOEClient.loginWithDeveloperToken(getContext(), config, new MimOEResponseHandler() {
//                @Override
//                public void onError(MimOERequestError mimOERequestError) {
//                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error getting access token! " + mimOERequestError.getErrorMessage(), Toast.LENGTH_LONG).show());
//                }
//
//                @Override
//                public void onResponse(MimOERequestResponse mimOERequestResponse) {
//                    CombinedAuthResponse tokens = mimOEClient.getCombinedAccessTokens();
//                    accessToken = tokens.getMimikTokens().getAccessToken();
//                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Got access token!", Toast.LENGTH_LONG).show());
//                    fetchWildfireData();
//                }
//            });
//        }
//
//        private void fetchWildfireData() {
//            // Replace with your actual endpoint and parameters
//            String url = "https://api.yourservice.com/wildfire-data";
//            OkHttpClient client = new OkHttpClient();
//            Request request = new Request.Builder()
//                    .url(url)
//                    .addHeader("Authorization", "Bearer " + accessToken)
//                    .build();
//
//            client.newCall(request).enqueue(new Callback() {
//                @Override
//                public void onFailure(@NonNull Call call, @NonNull IOException e) {
//                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Failed to fetch wildfire data", Toast.LENGTH_LONG).show());
//                }
//
//                @Override
//                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                    if (response.isSuccessful()) {
//                        String responseData = response.body().string();
//                        // Parse the response data and update the map
//                        getActivity().runOnUiThread(() -> updateMapWithWildfireData(responseData));
//                    } else {
//                        getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error fetching wildfire data", Toast.LENGTH_LONG).show());
//                    }
//                }
//            });
//        }
//
//        private void updateMapWithWildfireData(String data) {
//            // Parse the data and update the map with wildfire locations and evacuation routes
//            // This is a placeholder implementation
//            LatLng wildfireLocation = new LatLng(-34, 151); // Example coordinates
//            mMap.addMarker(new MarkerOptions().position(wildfireLocation).title("Wildfire Detected"));
//            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(wildfireLocation, 10));
//
//            // Add evacuation routes and other relevant information
//            fetchEvacuationRoutes();
//        }
//
//        private void fetchEvacuationRoutes() {
//            // Replace with your actual endpoint and parameters
//            String url = "https://api.yourservice.com/evacuation-routes";
//            OkHttpClient client = new OkHttpClient();
//            Request request = new Request.Builder()
//                    .url(url)
//                    .addHeader("Authorization", "Bearer " + accessToken)
//                    .build();
//
//            client.newCall(request).enqueue(new Callback() {
//                @Override
//                public void onFailure(@NonNull Call call, @NonNull IOException e) {
//                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Failed to fetch evacuation routes", Toast.LENGTH_LONG).show());
//                }
//
//                @Override
//                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                    if (response.isSuccessful()) {
//                        String responseData = response.body().string();
//                        // Parse the response data and update the map
//                        getActivity().runOnUiThread(() -> updateMapWithEvacuationRoutes(responseData));
//                    } else {
//                        getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error fetching evacuation routes", Toast.LENGTH_LONG).show());
//                    }
//                }
//            });
//        }
//
//        private void updateMapWithEvacuationRoutes(String data) {
//            // Parse the data and update the map with evacuation routes
//            // This is a placeholder implementation
//            List<LatLng> routePoints = parseRouteData(data);
//            PolylineOptions polylineOptions = new PolylineOptions().addAll(routePoints).color(Color.RED).width(5);
//            mMap.addPolyline(polylineOptions);
//        }
//
//        private List<LatLng> parseRouteData(String data) {
//            // Parse the JSON data to extract route points
//            // This is a placeholder implementation
//            List<LatLng> routePoints = new ArrayList<>();
//            // Add parsed points to routePoints
//            return routePoints;
//        }
//
//        private void updateGPS() {
//            locationRequest = LocationRequest.create();
//            locationRequest.setInterval(10000);
//            locationRequest.setFastestInterval(5000);
//            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//
//            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//                fusedLocationProviderClient.getLastLocation().addOnSuccessListener(getActivity(), location -> {
//                    if (location != null) {
//                        updateMapLocation(location);
//                    }
//                });
//
//                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
//            } else {
//                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
//            }
//        }
//
//        private void updateMapLocation(Location location) {
//            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
//            mMap.clear();
//            mMap.addMarker(new MarkerOptions().position(userLocation).title("You are here"));
//            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
//
//            Geocoder geocoder = new Geocoder(getContext());
//            try {
//                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
//                if (!addresses.isEmpty()) {
//                    Address address = addresses.get(0);
//                    String cityName = address.getLocality();
//                    String stateName = address.getAdminArea();
//                    Toast.makeText(getContext(), "Location: " + cityName + ", " + stateName, Toast.LENGTH_LONG).show();
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        @Override
//        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//            if (requestCode == PERMISSION_FINE_LOCATION) {
//                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    updateGPS();
//                } else {
//                    Toast.makeText(getContext(), "This app requires permission to be granted in order to work properly", Toast.LENGTH_SHORT).show();
//                }
//            }
//        }
//
//        @Override
//        public void onPause() {
//            super.onPause();
//            if (fusedLocationProviderClient != null) {
//                fusedLocationProviderClient.removeLocationUpdates(locationCallback);
//            }
//        }
//
//        @Override
//        public void onResume() {
//            super.onResume();
//            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
//            }
//        }
//    }


// with demo

package com.mtech.gosafe;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.mimik.mimoeclient.MimOEClient;
import com.mimik.mimoeclient.MimOERequestError;
import com.mimik.mimoeclient.MimOERequestResponse;
import com.mimik.mimoeclient.MimOEResponseHandler;
import com.mimik.mimoeclient.authobject.CombinedAuthResponse;
import com.mimik.mimoeclient.authobject.DeveloperTokenLoginConfig;
import com.mimik.mimoeclient.mimoeservice.MimOEConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MapsFragment extends Fragment {

    private static final int PERMISSION_FINE_LOCATION = 99;
    private MimOEClient mimOEClient;
    private String accessToken;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private GoogleMap mMap;

    private OnMapReadyCallback callback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(GoogleMap googleMap) {
            mMap = googleMap;
            updateGPS();
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maps, container, false);

        mimOEClient = new MimOEClient(getContext(), new MimOEConfig().license(BuildConfig.MIM_OE_LICENSE));
//        Executors.newSingleThreadExecutor().execute(this::startMimOE);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                updateMapLocation(locationResult.getLastLocation());
            }
        };

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
        if (getArguments() != null) {
            String evacuationPlan = getArguments().getString("evacuationPlan");
            if (evacuationPlan != null) {
                findDestination(evacuationPlan);
            }
        }
    }
    private void findDestination(String evacuationPlan) {
        if (evacuationPlan.contains("via")) {
            String[] parts = evacuationPlan.split("to");
            if (parts.length > 1) {
                String destination = parts[1].trim();
                searchLocation(destination);
            }
        }
    }
    private void searchLocation(String address) {
        Geocoder geocoder = new Geocoder(getContext());
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (!addresses.isEmpty()) {
                Address location = addresses.get(0);
                LatLng destinationLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Safe Zone: " + address));
                drawRoute(destinationLatLng);
            } else {
                Toast.makeText(getContext(), "Location not found!", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @SuppressLint("MissingPermission")
    private void drawRoute(LatLng destination) {
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(getActivity(), location -> {
            if (location != null) {
                LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                PolylineOptions polylineOptions = new PolylineOptions()
                        .add(userLatLng)
                        .add(destination)
                        .width(8)
                        .color(Color.BLUE);
                mMap.addPolyline(polylineOptions);

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 12));
            } else {
                Toast.makeText(getContext(), "User location not available", Toast.LENGTH_SHORT).show();
            }
        });
    }


//    private void startMimOE() {
//        if (mimOEClient.startMimOESynchronously()) {
//            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "mim OE started!", Toast.LENGTH_LONG).show());
//            authorizeMimOE();
//        } else {
//            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "mim OE failed to start!", Toast.LENGTH_LONG).show());
//        }
//    }

//    private void authorizeMimOE() {
//        String developerIdToken = BuildConfig.DEVELOPER_ID_TOKEN;
//        String clientId = BuildConfig.CLIENT_ID;
//
//        DeveloperTokenLoginConfig config = new DeveloperTokenLoginConfig();
//        config.setAuthorizationRootUri(BuildConfig.AUTHORIZATION_ENDPOINT);
//        config.setDeveloperToken(developerIdToken);
//        config.setClientId(clientId);
//
//        mimOEClient.loginWithDeveloperToken(getContext(), config, new MimOEResponseHandler() {
//            @Override
//            public void onError(MimOERequestError mimOERequestError) {
//                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error getting access token! " + mimOERequestError.getErrorMessage(), Toast.LENGTH_LONG).show());
//            }
//
//            @Override
//            public void onResponse(MimOERequestResponse mimOERequestResponse) {
//                CombinedAuthResponse tokens = mimOEClient.getCombinedAccessTokens();
//                accessToken = tokens.getMimikTokens().getAccessToken();
//                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Got access token!", Toast.LENGTH_LONG).show());
//                fetchWildfireData();
//            }
//        });
//    }
//
//    private void fetchWildfireData() {
//        // Replace with your actual endpoint and parameters
//        String url = "https://api.yourservice.com/wildfire-data";
//        OkHttpClient client = new OkHttpClient();
//        Request request = new Request.Builder()
//                .url(url)
//                .addHeader("Authorization", "Bearer " + accessToken)
//                .build();
//
//        client.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(@NonNull Call call, @NonNull IOException e) {
////                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Failed to fetch wildfire data", Toast.LENGTH_LONG).show());
//            }
//
//            @Override
//            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                if (response.isSuccessful()) {
//                    String responseData = response.body().string();
//                    // Parse the response data and update the map
//                    getActivity().runOnUiThread(() -> updateMapWithWildfireData(responseData));
//                } else {
////                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error fetching wildfire data", Toast.LENGTH_LONG).show());
//                }
//            }
//        });
//    }
//    private void updateMapWithWildfireData(String data) {
//        // Parse the data and update the map with wildfire locations and evacuation routes
//        // This is a placeholder implementation
//        LatLng wildfireLocation = new LatLng(29, 77); // Example coordinates
//        mMap.addMarker(new MarkerOptions().position(wildfireLocation).title("Wildfire Detected"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(wildfireLocation, 10));
//
//        // Add evacuation routes and other relevant information
//        fetchEvacuationRoutes();
//    }
//    private void fetchEvacuationRoutes() {
//        // Get the current location
//        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(getActivity(), location -> {
//                if (location != null) {
//                    double currentLat = location.getLatitude();
//                    double currentLng = location.getLongitude();
//                    // Destination coordinates for Delhi
//                    double delhiLat = 29.5041;
//                    double delhiLng = 77.4025;
//                    // Create JSON data for the route
//                    String demoData = String.format("[{\"lat\":%f,\"lng\":%f},{\"lat\":%f,\"lng\":%f}]", currentLat, currentLng, delhiLat, delhiLng);
//                    updateMapWithEvacuationRoutes(demoData);
//                } else {
//                    Toast.makeText(getContext(), "Unable to get current location", Toast.LENGTH_SHORT).show();
//                }
//            });
//        } else {
//            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
//        }
//    }
//    private void updateMapWithEvacuationRoutes(String data) {
//        // Parse the data and update the map with evacuation routes
//        List<LatLng> routePoints = parseRouteData(data);
//        if (routePoints != null && !routePoints.isEmpty()) {
//            PolylineOptions polylineOptions = new PolylineOptions().addAll(routePoints).color(Color.RED).width(5);
//            mMap.addPolyline(polylineOptions);
//            Log.d("MapsFragment", "Polyline added with points: " + routePoints.toString());
//        } else {
//            Toast.makeText(getContext(), "No route points available", Toast.LENGTH_SHORT).show();
//            Log.d("MapsFragment", "No route points available");
//        }
//    }
//
//    private List<LatLng> parseRouteData(String data) {
//        List<LatLng> routePoints = new ArrayList<>();
//        try {
//            JSONArray jsonArray = new JSONArray(data);
//            for (int i = 0; i < jsonArray.length(); i++) {
//                JSONObject jsonObject = jsonArray.getJSONObject(i);
//                double lat = jsonObject.getDouble("lat");
//                double lng = jsonObject.getDouble("lng");
//                routePoints.add(new LatLng(lat, lng));
//            }
//            Log.d("MapsFragment", "Parsed route points: " + routePoints.toString());
//        } catch (JSONException e) {
//            e.printStackTrace();
//            Log.e("MapsFragment", "Error parsing route data", e);
//        }
//        return routePoints;
//    }

    private void updateGPS() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(getActivity(), location -> {
                if (location != null) {
                    updateMapLocation(location);
                }
            });

            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
        }
    }

    private void updateMapLocation(Location location) {
        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(userLocation).title("You are here"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));

        Geocoder geocoder = new Geocoder(getContext());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                String cityName = address.getLocality();
                String stateName = address.getAdminArea();
//                Toast.makeText(getContext(), "Location: " + cityName + ", " + stateName, Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Add evacuation routes
//        fetchEvacuationRoutes();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_FINE_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateGPS();
            } else {
                Toast.makeText(getContext(), "This app requires permission to be granted in order to work properly", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }
}


















//package com.mtech.gosafe;
//
//import android.Manifest;
//import android.content.pm.PackageManager;
//import android.graphics.Color;
//import android.location.Address;
//import android.location.Geocoder;
//import android.location.Location;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.core.app.ActivityCompat;
//import androidx.fragment.app.Fragment;
//
//import com.google.android.gms.location.FusedLocationProviderClient;
//import com.google.android.gms.location.LocationCallback;
//import com.google.android.gms.location.LocationRequest;
//import com.google.android.gms.location.LocationResult;
//import com.google.android.gms.location.LocationServices;
//import com.google.android.gms.maps.CameraUpdateFactory;
//import com.google.android.gms.maps.GoogleMap;
//import com.google.android.gms.maps.OnMapReadyCallback;
//import com.google.android.gms.maps.SupportMapFragment;
//import com.google.android.gms.maps.model.LatLng;
//import com.google.android.gms.maps.model.MarkerOptions;
//import com.google.android.gms.maps.model.PolylineOptions;
//import com.mimik.mimoeclient.MimOEClient;
//import com.mimik.mimoeclient.MimOERequestError;
//import com.mimik.mimoeclient.MimOERequestResponse;
//import com.mimik.mimoeclient.MimOEResponseHandler;
//import com.mimik.mimoeclient.authobject.CombinedAuthResponse;
//import com.mimik.mimoeclient.authobject.DeveloperTokenLoginConfig;
//import com.mimik.mimoeclient.mimoeservice.MimOEConfig;
//
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.Executors;
//
//import okhttp3.Call;
//import okhttp3.Callback;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.Response;
//
//public class MapsFragment extends Fragment {
//
//    private static final int PERMISSION_FINE_LOCATION = 99;
//    private MimOEClient mimOEClient;
//    private String accessToken;
//    private FusedLocationProviderClient fusedLocationProviderClient;
//    private LocationCallback locationCallback;
//    private LocationRequest locationRequest;
//    private GoogleMap mMap;
//
//    private OnMapReadyCallback callback = new OnMapReadyCallback() {
//        @Override
//        public void onMapReady(GoogleMap googleMap) {
//            mMap = googleMap;
//            updateGPS();
//        }
//    };
//
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_maps, container, false);
//
//        mimOEClient = new MimOEClient(getContext(), new MimOEConfig().license(BuildConfig.MIM_OE_LICENSE));
//        Executors.newSingleThreadExecutor().execute(this::startMimOE);
//
//        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
//
//        locationCallback = new LocationCallback() {
//            @Override
//            public void onLocationResult(@NonNull LocationResult locationResult) {
//                super.onLocationResult(locationResult);
//                updateMapLocation(locationResult.getLastLocation());
//            }
//        };
//
//        return view;
//    }
//
//    @Override
//    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
//        if (mapFragment != null) {
//            mapFragment.getMapAsync(callback);
//        }
//    }
//
//    private void startMimOE() {
//        if (mimOEClient.startMimOESynchronously()) {
//            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "mim OE started!", Toast.LENGTH_LONG).show());
//            authorizeMimOE();
//        } else {
//            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "mim OE failed to start!", Toast.LENGTH_LONG).show());
//        }
//    }
//
//    private void authorizeMimOE() {
//        String developerIdToken = BuildConfig.DEVELOPER_ID_TOKEN;
//        String clientId = BuildConfig.CLIENT_ID;
//
//        DeveloperTokenLoginConfig config = new DeveloperTokenLoginConfig();
//        config.setAuthorizationRootUri(BuildConfig.AUTHORIZATION_ENDPOINT);
//        config.setDeveloperToken(developerIdToken);
//        config.setClientId(clientId);
//
//        mimOEClient.loginWithDeveloperToken(getContext(), config, new MimOEResponseHandler() {
//            @Override
//            public void onError(MimOERequestError mimOERequestError) {
//                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error getting access token! " + mimOERequestError.getErrorMessage(), Toast.LENGTH_LONG).show());
//            }
//
//            @Override
//            public void onResponse(MimOERequestResponse mimOERequestResponse) {
//                CombinedAuthResponse tokens = mimOEClient.getCombinedAccessTokens();
//                accessToken = tokens.getMimikTokens().getAccessToken();
//                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Got access token!", Toast.LENGTH_LONG).show());
//                fetchWildfireData();
//            }
//        });
//    }
//
//    private void fetchWildfireData() {
//        // Replace with your actual endpoint and parameters
//        String url = "https://api.yourservice.com/wildfire-data";
//        OkHttpClient client = new OkHttpClient();
//        Request request = new Request.Builder()
//                .url(url)
//                .addHeader("Authorization", "Bearer " + accessToken)
//                .build();
//
//        client.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(@NonNull Call call, @NonNull IOException e) {
////                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Failed to fetch wildfire data", Toast.LENGTH_LONG).show());
//            }
//
//            @Override
//            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                if (response.isSuccessful()) {
//                    String responseData = response.body().string();
//                    // Parse the response data and update the map
//                    getActivity().runOnUiThread(() -> updateMapWithWildfireData(responseData));
//                } else {
////                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error fetching wildfire data", Toast.LENGTH_LONG).show());
//                }
//            }
//        });
//    }
//    private void updateMapWithWildfireData(String data) {
//        // Parse the data and update the map with wildfire locations and evacuation routes
//        // This is a placeholder implementation
//        LatLng wildfireLocation = new LatLng(29, 77); // Example coordinates
//        mMap.addMarker(new MarkerOptions().position(wildfireLocation).title("Wildfire Detected"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(wildfireLocation, 10));
//
//        // Add evacuation routes and other relevant information
//        fetchEvacuationRoutes();
//    }
//    private void fetchEvacuationRoutes() {
//        // Get the current location
//        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(getActivity(), location -> {
//                if (location != null) {
//                    double currentLat = location.getLatitude();
//                    double currentLng = location.getLongitude();
//                    // Destination coordinates for Delhi
//                    double delhiLat = 29.5041;
//                    double delhiLng = 77.4025;
//                    // Create JSON data for the route
//                    String demoData = String.format("[{\"lat\":%f,\"lng\":%f},{\"lat\":%f,\"lng\":%f}]", currentLat, currentLng, delhiLat, delhiLng);
//                    updateMapWithEvacuationRoutes(demoData);
//                } else {
//                    Toast.makeText(getContext(), "Unable to get current location", Toast.LENGTH_SHORT).show();
//                }
//            });
//        } else {
//            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
//        }
//    }
//    private void updateMapWithEvacuationRoutes(String data) {
//        // Parse the data and update the map with evacuation routes
//        List<LatLng> routePoints = parseRouteData(data);
//        if (routePoints != null && !routePoints.isEmpty()) {
//            PolylineOptions polylineOptions = new PolylineOptions().addAll(routePoints).color(Color.RED).width(5);
//            mMap.addPolyline(polylineOptions);
//            Log.d("MapsFragment", "Polyline added with points: " + routePoints.toString());
//        } else {
//            Toast.makeText(getContext(), "No route points available", Toast.LENGTH_SHORT).show();
//            Log.d("MapsFragment", "No route points available");
//        }
//    }
//
//    private List<LatLng> parseRouteData(String data) {
//        List<LatLng> routePoints = new ArrayList<>();
//        try {
//            JSONArray jsonArray = new JSONArray(data);
//            for (int i = 0; i < jsonArray.length(); i++) {
//                JSONObject jsonObject = jsonArray.getJSONObject(i);
//                double lat = jsonObject.getDouble("lat");
//                double lng = jsonObject.getDouble("lng");
//                routePoints.add(new LatLng(lat, lng));
//            }
//            Log.d("MapsFragment", "Parsed route points: " + routePoints.toString());
//        } catch (JSONException e) {
//            e.printStackTrace();
//            Log.e("MapsFragment", "Error parsing route data", e);
//        }
//        return routePoints;
//    }
//
//    private void updateGPS() {
//        locationRequest = LocationRequest.create();
//        locationRequest.setInterval(10000);
//        locationRequest.setFastestInterval(5000);
//        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//
//        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(getActivity(), location -> {
//                if (location != null) {
//                    updateMapLocation(location);
//                }
//            });
//
//            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
//        } else {
//            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
//        }
//    }
//
//    private void updateMapLocation(Location location) {
//        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
//        mMap.clear();
//        mMap.addMarker(new MarkerOptions().position(userLocation).title("You are here"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
//
//        Geocoder geocoder = new Geocoder(getContext());
//        try {
//            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
//            if (!addresses.isEmpty()) {
//                Address address = addresses.get(0);
//                String cityName = address.getLocality();
//                String stateName = address.getAdminArea();
//                Toast.makeText(getContext(), "Location: " + cityName + ", " + stateName, Toast.LENGTH_LONG).show();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        // Add evacuation routes
//        fetchEvacuationRoutes();
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == PERMISSION_FINE_LOCATION) {
//            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                updateGPS();
//            } else {
//                Toast.makeText(getContext(), "This app requires permission to be granted in order to work properly", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//        if (fusedLocationProviderClient != null) {
//            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
//        }
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
//        }
//    }
//}

