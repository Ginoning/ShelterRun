package ginoning.shelterrun;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.Location;
import android.opengl.Matrix;
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
    Context context;
    private float[] rotatedProjectionMatrix = new float[16];
    private Location currentLocation;
    private List<ARPoint> arPoints;
    private Bitmap rightTriangleBitmap, leftTriangleBitmap, shelterBitmap;

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

    private void sidePointDraw(Canvas canvas, Paint paint, ArrayList<ARPoint> list, boolean isLeft){
        if(list==null) return;
        int imageHeight = leftTriangleBitmap.getHeight();
        int imageWeight = leftTriangleBitmap.getWidth();
        int count = Math.min(list.size(), MAX_SHELTER_NUM);
        float base = canvas.getHeight()/2-((imageHeight+50)*count/2);
        if(base<15)
            base = 15;
        for(int j=0 ; j<count ; j++) {
            float[] distance = new float[3];
            Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), list.get(j).getLocation().getLatitude(), list.get(j).getLocation().getLongitude(),distance);
            String distanceString = String.format("%.1f",distance[0]);

            float y = base + j * (imageHeight+50);
            float x;
            if(isLeft){
                x= imageWeight + 10;
                canvas.drawBitmap(leftTriangleBitmap, 0, y, paint);
            } else {
                x = canvas.getWidth() - (55 * list.get(j).getName().length()) - rightTriangleBitmap.getWidth() - 10;
                canvas.drawBitmap(rightTriangleBitmap, canvas.getWidth()-imageWeight, y, paint);
            }

            canvas.drawText(list.get(j).getName(), x, y + imageHeight / 2 - 10, paint);
            canvas.drawText(distanceString, x, y + imageHeight / 2 + 50, paint);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (currentLocation == null) {
            return;
        }
        if(rightTriangleBitmap==null || leftTriangleBitmap==null)
            getBitmapImage();

        ArrayList<ARPoint> leftPoint = new ArrayList<>();
        ArrayList<ARPoint> rightPoint = new ArrayList<>();

        final int radius = 30;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(60);
        int count =Math.min(arPoints.size(),MAX_SHELTER_NUM);
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
                float x  = (0.5f + cameraCoordinateVector[0]/cameraCoordinateVector[3]) * canvas.getWidth();
                float y = (0.5f - cameraCoordinateVector[1]/cameraCoordinateVector[3]) * canvas.getHeight() - (300*(count%2-i));
                if(x>canvas.getWidth()) {
                    rightPoint.add(arPoints.get(i));
                }
                else if(x<0){
                    leftPoint.add(arPoints.get(i));
                }
                else {
                    canvas.drawBitmap(shelterBitmap, x+shelterBitmap.getWidth()/2, y, paint);
                    String distance = String.valueOf(currentLocation.distanceTo(arPoints.get(i).getLocation()));
                    canvas.drawText(arPoints.get(i).getName(), x - (30 * arPoints.get(i).getName().length() / 2), y - 100, paint);
                    canvas.drawText(distance, x - (15 * distance.length() / 2), y - 30, paint);
                }
            }
        }
        sidePointDraw(canvas, paint, leftPoint, true);
        sidePointDraw(canvas, paint, rightPoint, false);
    }
}
