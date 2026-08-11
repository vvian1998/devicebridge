package com.devicebridge.handlers;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import com.google.gson.JsonObject;
import com.devicebridge.utils.JsonHelper;

public class LocationHandler {

    private final Context context;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private boolean tracking = false;

    public LocationHandler(Context context) {
        this.context = context;
    }

    public String handle(String action, JsonObject payload) {
        switch (action) {
            case "get": return getCurrentLocation();
            case "startTracking": return startTracking();
            case "stopTracking": return stopTracking();
            default: return JsonHelper.error("Unknown location action: " + action);
        }
    }

    private String getCurrentLocation() {
        try {
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager == null) return JsonHelper.error("LocationManager not available");

            Location location = null;

            try {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            } catch (SecurityException ignored) {}

            if (location == null) {
                try {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                } catch (SecurityException ignored) {}
            }

            if (location != null) {
                JsonObject resp = new JsonObject();
                resp.addProperty("latitude", location.getLatitude());
                resp.addProperty("longitude", location.getLongitude());
                resp.addProperty("accuracy", location.getAccuracy());
                resp.addProperty("provider", location.getProvider());
                resp.addProperty("altitude", location.getAltitude());
                resp.addProperty("speed", location.getSpeed());
                resp.addProperty("time", location.getTime());
                return JsonHelper.success(resp);
            }

            return JsonHelper.error("Location not available");

        } catch (Exception e) {
            return JsonHelper.error(e.getMessage());
        }
    }

    private String startTracking() {
        if (tracking) return JsonHelper.success("already tracking");

        try {
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager == null) return JsonHelper.error("LocationManager not available");

            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    sendLocationUpdate(location);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(String provider) {}

                @Override
                public void onProviderDisabled(String provider) {}
            };

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    5000, 5, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    10000, 10, locationListener);

            tracking = true;
            return JsonHelper.success("tracking started");
        } catch (Exception e) {
            return JsonHelper.error(e.getMessage());
        }
    }

    private String stopTracking() {
        if (locationManager != null && locationListener != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (Exception ignored) {}
        }
        tracking = false;
        locationListener = null;
        return JsonHelper.success("tracking stopped");
    }

    private void sendLocationUpdate(Location location) {
        try {
            JsonObject eventData = new JsonObject();
            eventData.addProperty("type", "location");
            eventData.addProperty("latitude", location.getLatitude());
            eventData.addProperty("longitude", location.getLongitude());
            eventData.addProperty("accuracy", location.getAccuracy());
            eventData.addProperty("provider", location.getProvider());
            eventData.addProperty("speed", location.getSpeed());

            Class<?> bsClass = Class.forName("com.devicebridge.BridgeService");
        } catch (Exception ignored) {}
    }
}
