package ginoning.shelterrun;

import android.location.Location;
import android.support.annotation.NonNull;

import java.util.Comparator;

/**
 * Created by bgh29 on 2017-08-31.
 */

public class ARPoint implements Comparable<ARPoint>{
    Location location;
    float initDistance;
    String name;

    public ARPoint(String name, double lat, double lon, double altitude, float initDistance) {
        this.name = name;
        location = new Location("ARPoint");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setAltitude(altitude);
        this.initDistance = initDistance;
    }

    public Location getLocation() {
        return location;
    }

    public float getInitDistance(){
        return initDistance;
    }

    public String getName() {
        return name;
    }

    @Override
    public int compareTo(@NonNull ARPoint o) {
        if (initDistance > o.getInitDistance()) {
            return 1;
        }
        else if (initDistance <  o.getInitDistance()) {
            return -1;
        }
        else {
            return 0;
        }
    }


    @Override
    public int hashCode() {
            return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ARPoint)) {
            return false;
        }
        ARPoint s = (ARPoint)obj;
        return name.equals(s.name);
    }

}
