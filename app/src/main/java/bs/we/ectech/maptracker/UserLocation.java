package bs.we.ectech.maptracker;

import android.location.Location;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Chun on 30/4/2016.
 */
public class UserLocation {
    private String type;
    private String id;
    private String name;
    private String group;
    private Map<String, Double> latlng = new HashMap<String, Double>();
    private int acr;
    private double currentLat;
    private double CurrentLng;

    public double getCurrentLat() {
        return currentLat;
    }

    public void setCurrentLat(double currentLat) {
        this.currentLat = currentLat;
    }

    public double getCurrentLng() {
        return CurrentLng;
    }

    public void setCurrentLng(double currentLng) {
        CurrentLng = currentLng;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }


    public int getAcr() {
        return acr;
    }

    public void setAcr(int acr) {
        this.acr = acr;
    }

    public Map<String, Double> getLatlng() {
        return latlng;
    }

    public void setLatlng(Map<String, Double> latlng) {
        this.latlng = latlng;
    }

    public UserLocation(String mName, String mGroup, String msg_type, String user_id, Location location){
        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        int currentAcc = (int) location.getAccuracy();

        type = msg_type;
        id = user_id;
        name = mName;
        group = mGroup;

        latlng.put("lat", currentLatitude);
        latlng.put("lng", currentLongitude);


        setAcr(currentAcc);
    }

    public UserLocation(String msg_type, String user_id){
        type = msg_type;
        id = user_id;
    }
}
