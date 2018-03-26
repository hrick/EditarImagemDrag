package com.henrique.editarimagemdrag;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.NoSuchElementException;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener, RotationGestureDetector.OnRotationGestureListener {
    private static final String TAG = "Touch";

    //These matrices will be used to move and zoom image
    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();
    private RotationGestureDetector mRotationDetector;

    // We can be in one of these 3 states
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    static final int DRAW = 3;
    int mode = NONE;

    // Remember some things for zooming
    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;

    // Limit zoomable/pannable image
    private float[] matrixValues = new float[9];
    private float maxZoom;
    private float minZoom;
    private float height;
    private float width;
    private RectF viewRect;


    // Remember some things for zooming
    private float xCoOrdinate, yCoOrdinate;

    ImageView view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        view = (ImageView) findViewById(R.id.imgOrologio);
//        view.setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher));
        maxZoom = 4;
        minZoom = 0.25f;
        height = view.getDrawable().getIntrinsicHeight()+20;
        width = view.getDrawable().getIntrinsicWidth()+20;
        viewRect = new RectF(0, 0, view.getWidth()+20, view.getHeight()+20);
//        view.setOnTouchListener(new View.OnTouchListener() {
//        @Override
//        public boolean onTouch (View view, MotionEvent event){
//            switch (event.getActionMasked()) {
//                case MotionEvent.ACTION_DOWN:
//                    xCoOrdinate = view.getX() - event.getRawX();
//                    yCoOrdinate = view.getY() - event.getRawY();
//                    break;
//                case MotionEvent.ACTION_MOVE:
//                    view.animate().x(event.getRawX() + xCoOrdinate).y(event.getRawY() + yCoOrdinate).setDuration(0).start();
//                    break;
//
//                default:
//                    return false;
//            }
//            return true;
//        }
//////    });
        view.setOnTouchListener(this);
        mRotationDetector = new RotationGestureDetector(this);

        //TODO: para juntar duas imagens
        Bitmap icon = BitmapFactory.decodeResource(this.getResources(),
                R.drawable.arrow_red);
        Bitmap ferrariMotor = BitmapFactory.decodeResource(this.getResources(),
                R.drawable.ferrari_motor);
        Bitmap combined = combineImages(ferrariMotor,icon);
        view.setImageBitmap(combined);

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            init();
        }
    }

    private void init() {
        maxZoom = 4;
        minZoom = 0.25f;
        height = view.getDrawable().getIntrinsicHeight() + 20;
        width = view.getDrawable().getIntrinsicWidth() + 20;
        viewRect = new RectF(0, 0, view.getWidth() + 20, view.getHeight() + 20);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mRotationDetector.onTouchEvent(event);

        // Dump touch event to log
        dumpEvent(event);
        // Handle touch events here...
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:

                xCoOrdinate = view.getX() - event.getRawX();
                yCoOrdinate = view.getY() - event.getRawY();
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                Log.d(TAG, "mode=DRAG");
                mode = DRAG;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                Log.d(TAG, "oldDist=" + oldDist);
                if (oldDist > 10f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                    Log.d(TAG, "mode=ZOOM");
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                Log.d(TAG, "mode=NONE");
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == DRAW) {
                    onTouchEvent(event);
                }
                if (mode == DRAG) {
                    view.animate().x(event.getRawX() + xCoOrdinate).y(event.getRawY() + yCoOrdinate).setDuration(0).start();
                } else if (mode == ZOOM) {

                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        matrix.set(savedMatrix);
                        float scale = newDist / oldDist;
                        matrix.getValues(matrixValues);
                        float currentScale = matrixValues[Matrix.MSCALE_X];
                        // limit zoom
                        if (scale * currentScale > maxZoom) {
                            scale = maxZoom / currentScale;
                        } else if (scale * currentScale < minZoom) {
                            scale = minZoom / currentScale;
                        }
                        matrix.postScale(scale, scale, mid.x, mid.y);
                    }

                }
                break;
        }
        view.setImageMatrix(matrix);
        view.getLayoutParams().height = (int) getHeightFromMatrix(matrix,view);
        view.getLayoutParams().width = (int) getWidthFromMatrix(matrix,view);
        //scaleImage(view);

        return true; // indicate event was handled
    }

    private float getWidthFromMatrix(Matrix matrix, ImageView imageview) {

        float[] values = new float[9];
        matrix.getValues(values);

        float width = values[0]* imageview.getWidth();

        return width;
    }

    private float getHeightFromMatrix(Matrix matrix, ImageView imageview) {

        float[] values = new float[9];
        matrix.getValues(values);

        float height = values[4] * imageview.getHeight();

        return height;
    }

    public Bitmap combineImages(Bitmap background, Bitmap foreground) {

        int width = 0, height = 0;
        Bitmap cs;

        width = getWindowManager().getDefaultDisplay().getWidth();
        height = getWindowManager().getDefaultDisplay().getHeight();

        cs = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas comboImage = new Canvas(cs);
        background = Bitmap.createScaledBitmap(background, width, height, true);
        comboImage.drawBitmap(background, 0, 0, null);
        comboImage.drawBitmap(foreground, matrix, null);

        return cs;
    }
//
    private void scaleImage(ImageView view) throws NoSuchElementException  {
        // Get bitmap from the the ImageView.
        Bitmap bitmap = null;

        try {
            Drawable drawing = view.getDrawable();
            bitmap = ((BitmapDrawable) drawing).getBitmap();
        } catch (NullPointerException e) {
            throw new NoSuchElementException("No drawable on given view");
        }

        // Get current dimensions AND the desired bounding box
        int width = 0;

        try {
            width = bitmap.getWidth();
        } catch (NullPointerException e) {
            throw new NoSuchElementException("Can't find bitmap on given view/drawable");
        }

        int height = bitmap.getHeight();
        int bounding = dpToPx(250);
        Log.i("Test", "original width = " + Integer.toString(width));
        Log.i("Test", "original height = " + Integer.toString(height));
        Log.i("Test", "bounding = " + Integer.toString(bounding));

        // Determine how much to scale: the dimension requiring less scaling is
        // closer to the its side. This way the image always stays inside your
        // bounding box AND either x/y axis touches it.
        float xScale = ((float) bounding) / width;
        float yScale = ((float) bounding) / height;
        float scale = (xScale <= yScale) ? xScale : yScale;
        Log.i("Test", "xScale = " + Float.toString(xScale));
        Log.i("Test", "yScale = " + Float.toString(yScale));
        Log.i("Test", "scale = " + Float.toString(scale));

        // Create a matrix for the scaling and add the scaling data
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        // Create a new bitmap and convert it to a format understood by the ImageView
        Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        width = scaledBitmap.getWidth(); // re-use
        height = scaledBitmap.getHeight(); // re-use
        BitmapDrawable result = new BitmapDrawable(scaledBitmap);
        Log.i("Test", "scaled width = " + Integer.toString(width));
        Log.i("Test", "scaled height = " + Integer.toString(height));

        // Apply the scaled bitmap
        view.setImageDrawable(result);

        // Now change ImageView's dimensions to match the scaled image
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        params.width = width;
        params.height = height;
        view.setLayoutParams(params);

        Log.i("Test", "done");
    }

    private int dpToPx(int dp) {
        float density = getApplicationContext().getResources().getDisplayMetrics().density;
        return Math.round((float)dp * density);
    }


    /**
     * Show an event in the LogCat view, for debugging
     */
    private void dumpEvent(MotionEvent event) {
        String names[] = {"DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE",
                "POINTER_DOWN", "POINTER_UP", "7?", "8?", "9?"};
        StringBuilder sb = new StringBuilder();
        int action = event.getAction();
        int actionCode = action & MotionEvent.ACTION_MASK;
        sb.append("event ACTION_").append(names[actionCode]);
        if (actionCode == MotionEvent.ACTION_POINTER_DOWN
                || actionCode == MotionEvent.ACTION_POINTER_UP) {
            sb.append("(pid ").append(
                    action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
            sb.append(")");
        }
        sb.append("[");
        for (int i = 0; i < event.getPointerCount(); i++) {
            sb.append("#").append(i);
            sb.append("(pid ").append(event.getPointerId(i));
            sb.append(")=").append((int) event.getX(i));
            sb.append(",").append((int) event.getY(i));
            if (i + 1 < event.getPointerCount())
                sb.append(";");
        }
        sb.append("]");
    }

    /**
     * Determine the space between the first two fingers
     */
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(((x * x) + (y * y)));
    }

    /**
     * Calculate the mid point of the first two fingers
     */
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    @Override
    public void OnRotation(RotationGestureDetector rotationDetector) {
        float angle = rotationDetector.getAngle();
        view.setRotation(view.getRotation() + (-angle));
        Log.d("RotationGestureDetector", "Rotation: " + Float.toString(angle));
    }
}