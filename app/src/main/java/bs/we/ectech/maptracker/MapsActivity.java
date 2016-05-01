package bs.we.ectech.maptracker;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Member;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MapsActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    public static final String TAG = MapsActivity.class.getSimpleName();
    private LocationRequest mLocationRequest;
    private Gson gson = new Gson();
    private WebSocketClient mWebSocketClient;
    private String mID = UUID.randomUUID().toString();
    private UserLocation mUserLocation;
    private String mName;
    private String mGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        final LocationManager mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }

        buildAlertMessageGetName();

        mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        setUpMap();

        try {
            connectWebSocket();
        } catch (WebsocketNotConnectedException e) {
            Log.e("Websocket", "Cannot connect to server");
            buildAlertMessageNoWebsocketService();
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(AppIndex.API).build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */


    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        mGoogleApiClient.connect();

        focusCurrentLocation();
    }

    private void focusCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.moveCamera(CameraUpdateFactory.zoomTo(15));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
       // if (mGoogleApiClient.isConnected()) {
           // LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
           // mGoogleApiClient.disconnect();
        //}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUserLocation = new UserLocation("disconnect", mID);
        String message = gson.toJson(mUserLocation);
        sendMessage(message);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "UserLocation services connected.");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else {
            try {
                handleNewLocation(location);
                focusCurrentLocation();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    private void setUpMap() {
        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }

    private void handleNewLocation(Location location) throws JSONException {
        Log.d(TAG, location.toString());
        mMap.clear();

        mUserLocation = new UserLocation(mName, mGroup , "update", mID, location);

        String message = gson.toJson(mUserLocation);
        sendMessage(message);
    }

    private void setMarker(JSONObject mLocationObj) throws JSONException {
        JSONObject latlng =  mLocationObj.getJSONObject("latlng");
        double currentLatitude = latlng.getDouble("lat");
        double currentLongitude = latlng.getDouble("lng");
        int currentAcc = mLocationObj.getInt("acr");
        String mName = mLocationObj.getString("name");

        mUserLocation.setCurrentLat(currentLatitude);
        mUserLocation.setCurrentLng(currentLongitude);

        LatLng latLng = new LatLng(mUserLocation.getCurrentLat(), mUserLocation.getCurrentLng());

        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title(mName+" (Within " + currentAcc + " meters radius)");

        mMap.addMarker(options);
    }


    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "UserLocation services suspended. Please reconnect.");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        try {
            handleNewLocation(location);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void buildAlertMessageNoGps (){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, please enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private synchronized void buildAlertMessageNoWebsocketService() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Server is currently unavailable, please try again later.")
                .setCancelable(false)
                .setPositiveButton("OK", null);
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private synchronized void buildAlertMessageGetName() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Please enter your name");

        final EditText input = new EditText(this);

        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mName = input.getText().toString();
                //Log.i("User_name", mName);
                buildAlertMessageGetGroup();
            }
        });
        builder.show();
    }

    private void buildAlertMessageGetGroup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Please enter your group number");

        final EditText input = new EditText(this);

        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mGroup = input.getText().toString();
                //Log.i("User_name", mGroup);
            }
        });
        builder.show();
    }

    private void connectWebSocket() throws WebsocketNotConnectedException{
        URI uri;
        try {
            //uri = new URI("ws://ectech.ddns.net:9000/server.php");
            uri = new URI("ws://cloud.wppd.co:9000/google_map/server.php");
            Log.d("WebSocket", "Connected to websocket server");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i("Websocket", "Opened");
            }

            @Override
            public void onMessage(String s) {
                final String message = s;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        handleMessage(message);
                    }
                });
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("Websocket", "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.i("Websocket", "Error " + e.getMessage());
            }
        };
        mWebSocketClient.connect();
    }

    private void handleMessage(String message) {
        JSONObject mMemberlocations = null ;
        //Log.d("Websocket Received msg", message);
        try {
            mMemberlocations = new JSONObject(message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Iterator<String> iter = mMemberlocations.keys();
        while (iter.hasNext()) {
            String key = iter.next();
            try {
                JSONObject mLocationObj = mMemberlocations.getJSONObject(key);
                setMarker(mLocationObj);
            } catch (JSONException e) {
                // Something went wrong!
            }

        }
    }

    public void sendMessage(String message) {
        try {
            mWebSocketClient.send(message);
        }catch(WebsocketNotConnectedException e){
            Log.e("Websocket", "Cannot send message");
        }
    }
}
