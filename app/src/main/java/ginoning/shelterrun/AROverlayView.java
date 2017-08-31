package ginoning.shelterrun;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.opengl.Matrix;
import android.util.Log;
import android.view.View;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.List;

import ginoning.shelterrun.helper.LocationHelper;

/**
 * Created by bgh29 on 2017-08-31.
 */

public class AROverlayView  extends View {

    Context context;
    private float[] rotatedProjectionMatrix = new float[16];
    private Location currentLocation;
    private List<ARPoint> arPoints;
    private Bitmap rightTriangleBitmap, leftTriangleBitmap;

    public AROverlayView(Context context) {
        super(context);

        this.context = context;

        Picasso.with(context).load(R.drawable.right_triangle).resize(50, 80).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                rightTriangleBitmap = bitmap;
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) { }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) { }
        });
        Picasso.with(context).load(R.drawable.left_triangle).resize(50, 80).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                leftTriangleBitmap = bitmap;
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) { }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) { }
        });

        //Demo points
        arPoints = new ArrayList<ARPoint>() {{
            add(new ARPoint("Sun Wheel", 35.891249, 128.610787, 120));
            add(new ARPoint("Linh Ung Pagoda", 35.892433, 128.610606, 120));
            add(new ARPoint("Linh Ung Pagoda2", 35.892433, 128.610606, 120));
        }};
    }

    public void updateRotatedProjectionMatrix(float[] rotatedProjectionMatrix) {
        this.rotatedProjectionMatrix = rotatedProjectionMatrix;
        this.invalidate();
    }

    public void updateCurrentLocation(Location currentLocation){
        this.currentLocation = currentLocation;

        for (int i = 0; i < arPoints.size(); i ++) {

        }
        this.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (currentLocation == null) {
            return;
        }
        int leftNum=0, rightNum=0;
        final int radius = 30;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(60);

        for (int i = 0; i < arPoints.size(); i ++) {
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
                float y = (0.5f - cameraCoordinateVector[1]/cameraCoordinateVector[3]) * canvas.getHeight();
                if(x>canvas.getWidth()) {
                    y = canvas.getHeight()/2-rightTriangleBitmap.getHeight()/2+(1.5f*rightTriangleBitmap.getHeight()*rightNum++);
                    canvas.drawBitmap(rightTriangleBitmap, canvas.getWidth() - rightTriangleBitmap.getWidth(), y, paint);
                    String distance = String.valueOf(currentLocation.distanceTo(arPoints.get(i).getLocation()));
                    canvas.drawText(arPoints.get(i).getName(),
                            canvas.getWidth() - (30 * arPoints.get(i).getName().length()) - rightTriangleBitmap.getWidth() - 10, y + rightTriangleBitmap.getHeight() / 2 - 10, paint);
                    canvas.drawText(distance,
                            canvas.getWidth() - (30 * distance.length()) - rightTriangleBitmap.getWidth() - 20, y + rightTriangleBitmap.getHeight() / 2 + 50, paint);
                }
                else if(x<0){
                    y = canvas.getHeight()/2-leftTriangleBitmap.getHeight()/2+(1.5f*rightTriangleBitmap.getHeight()*leftNum++);
                    canvas.drawBitmap(leftTriangleBitmap, 0, y, paint);
                    String distance = String.valueOf(currentLocation.distanceTo(arPoints.get(i).getLocation()));
                    canvas.drawText(arPoints.get(i).getName(),
                            leftTriangleBitmap.getWidth()+10, y + leftTriangleBitmap.getHeight() / 2 - 10, paint);
                    canvas.drawText(distance,
                            leftTriangleBitmap.getWidth()+10, y + leftTriangleBitmap.getHeight() / 2 + 50, paint);

                }
                else {
                    canvas.drawCircle(x, y, radius, paint);
                    String distance = String.valueOf(currentLocation.distanceTo(arPoints.get(i).getLocation()));
                    canvas.drawText(arPoints.get(i).getName(),
                            x - (30 * arPoints.get(i).getName().length() / 2), y - 160, paint);
                    canvas.drawText(distance,
                            x - (30 * distance.length() / 2), y - 60, paint);
                }
            }
        }
    }
}
