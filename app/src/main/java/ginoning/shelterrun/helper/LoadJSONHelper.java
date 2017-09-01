package ginoning.shelterrun.helper;

import android.content.Context;
import android.content.res.AssetManager;
import android.location.Location;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import ginoning.shelterrun.ARPoint;

/**
 * Created by Newyear on 2017-09-01.
 */

public class LoadJSONHelper {
    private static final float MAX_DISTANCE = 4000.0f;
    public static List<ARPoint> loadJSONFromAssert(Context con, Location userLoc) {
        AssetManager assetManager = con.getResources().getAssets();
        List<ARPoint> arPoints = new ArrayList<>();
        ArrayList<ARPoint> newArrList=null;
        try {
                AssetManager.AssetInputStream ais = (AssetManager.AssetInputStream) assetManager.open("data.json");
            BufferedReader br = new BufferedReader(new InputStreamReader(ais));
            StringBuilder sb = new StringBuilder();
            int bufferSize = 1024 * 1024;
            char readBuf[] = new char[bufferSize];
            int resultSize = 0;

            while ((resultSize = br.read(readBuf)) != -1) {
                if (resultSize == bufferSize) {
                    sb.append(readBuf);
                } else {
                    for (int i = 0; i < resultSize; i++) {
                        sb.append(readBuf[i]);
                    }
                }
            }
            String jString = sb.toString();
            JSONObject jsonObject = new JSONObject(jString);
            JSONArray jArr = new JSONArray(jsonObject.getString("shelter"));
            for (int i = 0; i < jArr.length(); i++) {
                double tempLongitude =  jArr.getJSONObject(i).getDouble("longitude");
                double tempLatitude = jArr.getJSONObject(i).getDouble("latitude");
                float[] distance = new float[3];
                Location.distanceBetween(userLoc.getLatitude(), userLoc.getLongitude(), tempLatitude, tempLongitude, distance);
                ARPoint newARPoint = new ARPoint(jArr.getJSONObject(i).getString("name").toString(), tempLatitude, tempLongitude, 100, distance[0]);
                if(distance[0]<MAX_DISTANCE && !arPoints.contains(newARPoint))
                    arPoints.add(newARPoint);
            }
            HashSet<ARPoint> hs = new HashSet(arPoints);
            newArrList = new ArrayList<>(hs);
            Collections.sort(newArrList);
        } catch (JSONException je) {
            Log.e("jsonErr", "json error", je);
        } catch (Exception e) {
            Log.e("execption", "not found file", e);
        }
        return newArrList;
    }
}