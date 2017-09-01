package ginoning.shelterrun;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.location.Location;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import ginoning.shelterrun.helper.LoadJSONHelper;
import ginoning.shelterrun.helper.LocationHelper;

/**
 * Created by bgh29 on 2017-08-31.
 */

public class AROverlayView  extends View {
    public static final int MAX_SHELTER_NUM = 3;
    public static final double TOUCH_DIAMETER = 50;
    Context context;
    private float[] rotatedProjectionMatrix = new float[16];
    private Location currentLocation;
    private List<ARPoint> arPoints;
    private Bitmap rightTriangleBitmap, leftTriangleBitmap, shelterBitmap;
    private List<PointF> displayPointF = new ArrayList<>();;
    private List<ARPoint> dispalyARPoint = new ArrayList<>();

    public AROverlayView(Context context) {
        super(context);
        this.context = context;
    }

    public void updateRotatedProjectionMatrix(float[] rotatedProjectionMatrix) {
        this.rotatedProjectionMatrix = rotatedProjectionMatrix;
        this.invalidate();
    }

    public void updateCurrentLocation(Location currentLocation){
        this.currentLocation = currentLocation;

        if(arPoints==null)
            arPoints = LoadJSONHelper.loadJSONFromAssert(context, currentLocation);
        this.invalidate();
    }

    private Bitmap resizeBitmapImage(Bitmap source, int maxResolution)
    {
        int width = source.getWidth();
        int height = source.getHeight();
        int newWidth = width;
        int newHeight = height;
        float rate = 0.0f;

        if(width > height)
        {
            if(maxResolution < width)
            {
                rate = maxResolution / (float) width;
                newHeight = (int) (height * rate);
                newWidth = maxResolution;
            }
        }
        else
        {
            if(maxResolution < height)
            {
                rate = maxResolution / (float) height;
                newWidth = (int) (width * rate);
                newHeight = maxResolution;
            }
        }

        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true);
    }

    private void getBitmapImage(){
        rightTriangleBitmap = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.right_triangle);
        rightTriangleBitmap = resizeBitmapImage(rightTriangleBitmap, 80);

        leftTriangleBitmap = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.left_triangle);
        leftTriangleBitmap = resizeBitmapImage(leftTriangleBitmap, 80);

        shelterBitmap = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.shelter);
        shelterBitmap = resizeBitmapImage(shelterBitmap, 100);
    }

    private double PointFDistance(PointF start, PointF end){
        return Math.sqrt(Math.pow(start.x-end.x, 2)+Math.pow(start.y-start.y, 2));
    }


    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                if(displayPointF==null) break;
                for(int i=0 ; i<displayPointF.size(); i++){
                    if(PointFDistance(new PointF(x,y), displayPointF.get(i))<=TOUCH_DIAMETER){
                        Log.e("touchShelter", dispalyARPoint.get(i).getName());
                    }
                }
                break;
            }
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (currentLocation == null) {
            return;
        }
        if(rightTriangleBitmap==null || leftTriangleBitmap==null)
            getBitmapImage();

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(60);
        int count =Math.min(arPoints.size(),MAX_SHELTER_NUM);
        displayPointF.clear();
        dispalyARPoint.clear();
        for (int i = 0; arPoints!=null && i < count ; i ++) {
            arPoints.get(i).getLocation().setAltitude(currentLocation.getAltitude());
            float[] currentLocationInECEF = LocationHelper.WSG84toECEF(currentLocation);
            float[] pointInECEF = LocationHelper.WSG84toECEF(arPoints.get(i).getLocation());
            float[] pointInENU = LocationHelper.ECEFtoENU(currentLocation, currentLocationInECEF, pointInECEF);

            float[] cameraCoordinateVector = new float[4];
            Matrix.multiplyMV(cameraCoordinateVector, 0, rotatedProjectionMatrix, 0, pointInENU, 0);

            // cameraCoordinateVector[2] is z, that always less than 0 to display on right position
            // if z > 0, the point will display on the opposite
            if (cameraCoordinateVector[2] < 0) {
                float[] distance = new float[3];
                Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), arPoints.get(i).getLocation().getLatitude(), arPoints.get(i).getLocation().getLongitude(), distance);
                String distanceString = String.format("%.1f", distance[0]);

                float x = (0.5f + cameraCoordinateVector[0] / cameraCoordinateVector[3]) * canvas.getWidth();
                float y = (0.5f - cameraCoordinateVector[1] / cameraCoordinateVector[3]) * canvas.getHeight() - (300 * (i-count % 2));

                if (x > canvas.getWidth()) {
                    x = canvas.getWidth() - (55 * arPoints.get(i).getName().length()) - rightTriangleBitmap.getWidth() - 10;
                    y = canvas.getHeight()/2 - (300 * (i-count % 2));
                    canvas.drawBitmap(rightTriangleBitmap, canvas.getWidth() - rightTriangleBitmap.getWidth(), y, paint);

                    canvas.drawText(arPoints.get(i).getName(), x, y + leftTriangleBitmap.getHeight() / 2 - 10, paint);
                    canvas.drawText(distanceString, x, y + rightTriangleBitmap.getHeight() / 2 + 50, paint);
                }
                else if(x<0) {
                    x = leftTriangleBitmap.getWidth() + 10;
                    y = canvas.getHeight()/2 - (300 * (i-count % 2));
                    canvas.drawBitmap(leftTriangleBitmap, 0, y, paint);

                    canvas.drawText(arPoints.get(i).getName(), x, y + leftTriangleBitmap.getHeight() / 2 - 10, paint);
                    canvas.drawText(distanceString, x, y + rightTriangleBitmap.getHeight() / 2 + 50, paint);
                }
                else {
                    displayPointF.add(new PointF(x+shelterBitmap.getWidth(), y+shelterBitmap.getHeight()/2));
                    dispalyARPoint.add(arPoints.get(i));
                    canvas.drawBitmap(shelterBitmap, x+shelterBitmap.getWidth()/2, y, paint);
                    canvas.drawText(arPoints.get(i).getName(), x - (30 * arPoints.get(i).getName().length() / 2), y - 100, paint);
                    canvas.drawText(distanceString, x - (15 * distanceString.length() / 2), y - 30, paint);
                }
            }
        }
    }
}
