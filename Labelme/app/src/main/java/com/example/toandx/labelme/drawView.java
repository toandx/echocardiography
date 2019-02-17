package com.example.toandx.labelme;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.graphics.Path;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Stack;

import org.json.JSONArray;

import java.io.File;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonObject;
import org.json.JSONArray;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;


/**
 * TODO: document your custom view class.
 */
public class drawView extends View {
    private Pen myPen;
    private Path path;
    private int width,height;
    private int ix,iy;
    private Bitmap bitmap;
    public Boolean Zoom;
    private String bg_color="WHITE";
    private Context mContext;
    private ArrayList<Pair<Path, Pen>> paths;//keeps record of every different path and paint properties associated with it
    private Stack<Pair<Path, Pen>> backup;//keeps a backup for redoing the changes
    private ScaleGestureDetector scaleDetector;
    private Boolean dragged;
    private List<int[]> points;

    private static float MIN_ZOOM = 1f;
    private static float MAX_ZOOM = 5f;
    private static int displayWidth=1024;
    private static int displayHeight=960;

    private float scaleFactor = 1.f;
    private ScaleGestureDetector detector;

    //These constants specify the mode that we're in
    private static int NONE = 0;
    private static int DRAG = 1;
    private static int ZOOM = 2;

    private int mode;

    //These two variables keep track of the X and Y coordinate of the finger when it first
    //touches the screen
    private float startX = 0f;
    private float startY = 0f;

    //These two variables keep track of the amount we need to translate the canvas along the X
    //and the Y coordinate
    private float translateX = 0f;
    private float translateY = 0f;

    //These two variables keep track of the amount we translated the X and Y coordinates, the last time we
    //panned.
    private float previousTranslateX = 0f;
    private float previousTranslateY = 0f;


    public drawView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        path = new Path();
        paths = new ArrayList<>();
        backup = new Stack<>();
        points = new ArrayList<>();
        bitmap=Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        width=1024;
        height=960;
        Zoom=false;
        initPaint(attrs);
        detector = new ScaleGestureDetector(getContext(), new ScaleListener());
    }
    public void EnableZoom()
    {
        Zoom=!Zoom;
    }
    //called when view is refreshed
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();

        //We're going to scale the X and Y coordinates by the same amount
        //canvas.scale(scaleFactor, scaleFactor);
        canvas.scale(scaleFactor,scaleFactor);

        //If translateX times -1 is lesser than zero, let's set it to zero. This takes care of the left bound
        if((translateX * -1) < 0) {
            translateX = 0;
        }

        //This is where we take care of the right bound. We compare translateX times -1 to (scaleFactor - 1) * displayWidth.
        //If translateX is greater than that value, then we know that we've gone over the bound. So we set the value of
        //translateX to (1 - scaleFactor) times the display width. Notice that the terms are interchanged; it's the same
        //as doing -1 * (scaleFactor - 1) * displayWidth
        else if((translateX * -1) > (scaleFactor - 1) * displayWidth) {
            translateX = (1 - scaleFactor) * displayWidth;
        }

        if(translateY * -1 < 0) {
            translateY = 0;
        }

        //We do the exact same thing for the bottom bound, except in this case we use the height of the display
        else if((translateY * -1) > (scaleFactor - 1) * displayHeight) {
            translateY = (1 - scaleFactor) * displayHeight;
        }

        //We need to divide by the scale factor here, otherwise we end up with excessive panning based on our zoom level
        //because the translation amount also gets scaled according to how much we've zoomed into the canvas.
        canvas.translate(translateX / scaleFactor, translateY / scaleFactor);

        /* The rest of your canvas-drawing code */


        Paint paint = myPen.getPen();
        canvas.drawBitmap(bitmap,0,0,paint);
        for(Pair<Path, Pen> p : paths){
            canvas.drawPath(p.first, p.second.getPen());
        }
        canvas.drawPath(path, paint);

        canvas.restore();


    }
    //called when screen is touched or untouched
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float X = (int) (event.getX()-translateX)/scaleFactor;
        float Y = (int) (event.getY()-translateY)/scaleFactor;
        ix=(int) ((X*width)/displayWidth);
        iy=(int) ((Y*height)/displayHeight);
        //Log.d("Point",Float.toString(X)+" "+Float.toString(Y));
        //Log.d("Point",Integer.toString(ix)+" "+Integer.toString(iy)+" "+Float.toString(scaleFactor)+" "+Float.toString(translateX)+" "+Float.toString(translateY));


        int eventaction = event.getAction();
        if (!Zoom) {
            //Log.d("Point",Float.toString(ix)+" "+Float.toString(iy));
            points.add(new int[]{ix,iy});
        switch (eventaction) {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(X, Y);
                break;

            case MotionEvent.ACTION_MOVE:
                path.lineTo(X, Y);
                break;

            case MotionEvent.ACTION_UP:
                Pair<Path, Pen> pair = new Pair<>(path, myPen);
                paths.add(pair);
                path = new Path();
                String c = myPen.getPaint_color();
                float w = myPen.getStroke_width();
                myPen = new Pen();
                myPen.setStrokeWidth(w);
                myPen.setPaint_color(Color.parseColor(c));
                break;
        }
        invalidate();} else
        {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {

                case MotionEvent.ACTION_DOWN:
                    mode = DRAG;

                    //We assign the current X and Y coordinate of the finger to startX and startY minus the previously translated
                    //amount for each coordinates This works even when we are translating the first time because the initial
                    //values for these two variables is zero.
                    startX = event.getX() - previousTranslateX;
                    startY = event.getY() - previousTranslateY;
                    break;

                case MotionEvent.ACTION_MOVE:
                    translateX = event.getX() - startX;
                    translateY = event.getY() - startY;

                    //We cannot use startX and startY directly because we have adjusted their values using the previous translation values.
                    //This is why we need to add those values to startX and startY so that we can get the actual coordinates of the finger.
                    double distance = Math.sqrt(Math.pow(event.getX() - (startX + previousTranslateX), 2) +
                            Math.pow(event.getY() - (startY + previousTranslateY), 2)
                    );

                    if(distance > 0) {
                        dragged = true;
                    }

                    break;

                case MotionEvent.ACTION_POINTER_DOWN:
                    mode = ZOOM;
                    break;

                case MotionEvent.ACTION_UP:
                    mode = NONE;
                    dragged = false;

                    //All fingers went up, so let's save the value of translateX and translateY into previousTranslateX and
                    //previousTranslate
                    previousTranslateX = translateX;
                    previousTranslateY = translateY;
                    break;

                case MotionEvent.ACTION_POINTER_UP:
                    mode = DRAG;

                    //This is not strictly necessary; we save the value of translateX and translateY into previousTranslateX
                    //and previousTranslateY when the second finger goes up
                    previousTranslateX = translateX;
                    previousTranslateY = translateY;
                    break;
            }
            detector.onTouchEvent(event);

            //We redraw the canvas only in the following cases:
            //
            // o The mode is ZOOM
            //        OR
            // o The mode is DRAG and the scale factor is not equal to 1 (meaning we have zoomed) and dragged is
            //   set to true (meaning the finger has actually moved)
            if ((mode == DRAG && scaleFactor != 1f && dragged) || mode == ZOOM) {
                invalidate();
            }

        }
        return true;
    }
    public void setCanvasBitmap(Bitmap b,int w,int h)
    {
        bitmap=b;
        width=w;
        height=h;
        Log.d("BitmapSize",Integer.toString(width)+" "+Integer.toString(height));
        invalidate();
    }

    //Initializes and set all the values of paint and background color by taking values from xml
    private void initPaint(AttributeSet atr){
        myPen = new Pen();
        TypedArray typedArray = mContext.getTheme().obtainStyledAttributes(atr, R.styleable.Canvas, 0, 0);
        String t_color = typedArray.getString(R.styleable.Canvas_paint_color);
        String t_size = typedArray.getString(R.styleable.Canvas_paint_width);
        if(t_color != null){
            myPen.setPaint_color(Color.parseColor(t_color));
        }
        if(t_size != null){
            myPen.setStrokeWidth(Float.valueOf(t_size));
        }
        String background_color = typedArray.getString(R.styleable.Canvas_bg_color);
        if(background_color != null){
            try {
                this.setBackgroundColor(Color.parseColor(background_color));
                bg_color = String.format("#%06X", (0xFFFFFF & Color.parseColor(background_color)));
            }catch (Exception e){
                Log.d("TouchDrawView", e.toString());
            }

        }
        this.setDrawingCacheEnabled(true);
        this.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);


    }

    //saves the screen data in storage
    public void saveFile(String folderName, String fileName){
        Bitmap bitmap = this.getDrawingCache();
        String path = Environment.getExternalStorageDirectory().getAbsolutePath();

        File f = new File(Environment.getExternalStorageDirectory(), folderName);
        if (!f.exists()) {
            f.mkdirs();
        }
        File file = new File(path+"/" + folderName + "/" + fileName + ".jpeg");
        FileOutputStream ostream;
        try {
            if(file.exists()){
                file.delete();
            }
            file.createNewFile();
            ostream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream);
            ostream.flush();
            ostream.close();
            Toast.makeText(mContext, "image saved", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(mContext, "error", Toast.LENGTH_LONG).show();
        }

    }

    //returns the  bitmap file of the screen
    public Bitmap getFile(){
        Bitmap file = this.getDrawingCache();
        return file;
    }

    //UnDo the last change done
    public void undo(){
        if(paths.size() >=1){
            backup.push(paths.get(paths.size()-1));
            paths.remove(paths.size()-1);
            invalidate();
        }
    }

    //ReDo the last change done
    public void redo(){
        if(!backup.empty()){
            paths.add(backup.peek());
            backup.pop();
            invalidate();
        }
    }

    //Clears the screen
    public void clear(){
        backup.clear();
        for(Pair<Path, Pen> p : paths){
            backup.push(p);
        }
        paths.clear();
        invalidate();
    }

    //getters start
    public String getPaintColor(){
        return myPen.getPaint_color();
    }

    public float getStrokeWidth(){
        return myPen.getStroke_width();
    }

    public String getBGColor(){
        return bg_color;
    }

    //getters end


    //setters start
    public void setPaintColor(int paintColor){
        myPen.setPaint_color(paintColor);
    }

    public void setStrokeWidth(float paintWidth){
        myPen.setStrokeWidth(paintWidth);
    }

    public void setBGColor(int bgColor){
        try {
            this.setBackgroundColor(bgColor);
            bg_color = String.format("#%06X", (0xFFFFFF & bgColor));
        }catch (Exception e){
            //Log.d("Paint", e.toString());
        }
    }
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(MIN_ZOOM, Math.min(scaleFactor, MAX_ZOOM));
            return true;
        }
    }
    public void save() {
        JsonObject obj = new JsonObject();
        JSONArray jsonArray = new JSONArray(points);
        obj.addProperty("points", jsonArray.toString());

        Writer output = null;
        File movieFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File file = new File(movieFile, "labelme.json");
        //Log.d("AAA", movieFile.getAbsolutePath());
        //Log.d("AAA", file.getAbsolutePath());
        //Log.d("AAA", obj.toString());
        try {
            output = new BufferedWriter(new FileWriter(file));
            output.write(obj.toString());
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
            //Log.d("AAA","Con dien");
        }

    }
    //setters end
}
