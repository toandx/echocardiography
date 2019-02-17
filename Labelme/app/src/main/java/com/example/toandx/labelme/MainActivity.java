package com.example.toandx.labelme;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private Bitmap bitmap;
    private ImageView imageView;
    private int width,height;
    private drawView drawView1;
    private Button zoomBtn;
    private final int PICK_IMAGE_REQUEST = 71;
    private final int TAKE_CAMERA_REQUEST=78;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        drawView1=(drawView) findViewById(R.id.drawView);
        zoomBtn=(Button) findViewById(R.id.button5);
    }
    public void chooseImage(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
        //Toast.makeText(this,"abc",Toast.LENGTH_LONG).show();
    }
    public void takeCam(View view)
    {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, TAKE_CAMERA_REQUEST);
        }
    }
    public Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {

        int width = bm.getWidth();

        int height = bm.getHeight();

        float scaleWidth = ((float) newWidth) / width;

        float scaleHeight = ((float) newHeight) / height;

// create a matrix for the manipulation

        Matrix matrix = new Matrix();

// resize the bit map

        matrix.postScale(scaleWidth, scaleHeight);

// recreate the new Bitmap

        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);

        return resizedBitmap;

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null )
        {
            Uri filePath = data.getData();
            //filePath=Uri.parse("content://media/external/images/media/4127");
            Toast.makeText(this,filePath.toString(),Toast.LENGTH_LONG).show();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                width=bitmap.getWidth();
                height=bitmap.getHeight();
                bitmap=Bitmap.createScaledBitmap(bitmap,1024,960,true);
                drawView1.setCanvasBitmap(bitmap,width,height);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                //Log.d("ABC","Exception");
            }
        } else
        if (requestCode == TAKE_CAMERA_REQUEST  && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            bitmap = (Bitmap) extras.get("data");
            width=bitmap.getWidth();
            height=bitmap.getHeight();
            bitmap=Bitmap.createScaledBitmap(bitmap,1024,960,true);
            drawView1.setCanvasBitmap(bitmap,width,height);
        }
    }
    public void clear(View view)
    {
        drawView1.clear();
    }
    public void Undo(View view)
    {
        drawView1.undo();
    }
    public void Redo(View view) {drawView1.redo();}
    public void Zoom(View view) {drawView1.EnableZoom();
        if (drawView1.Zoom)
            zoomBtn.setTextColor(Color.GREEN); else
                zoomBtn.setTextColor(Color.BLACK);

    }
    public void Save(View view)
    {
        drawView1.save();
    }
}
