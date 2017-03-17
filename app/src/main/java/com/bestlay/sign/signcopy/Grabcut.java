package com.bestlay.sign.signcopy;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Grabcut extends Activity implements View.OnTouchListener {
    ImageView iv;
    Bitmap bitmap;
    Canvas canvas;

    Point tl, br;
    int counter;
    Bitmap bitmapResult, bitmapBackground;

    final String pathToImage  = Environment.getExternalStorageDirectory()+"/gcut.png";
    public static final String TAG = "Grabcut demo";
    @SuppressWarnings("unused")
    private static final float MIN_ZOOM = 1f,MAX_ZOOM = 1f;

    private static final int RESULT_LOAD_IMAGE = 100;
    private static final int FINE_MEDIA_ACCESS_PERMISSION_REQUEST = 123;
    // These matrices will be used to scale points of the image
    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();

    // The 3 states (events) which the user is trying to perform
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;

    // these PointF objects are used to record the point(s) the user is touching
    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;

    /** Called when the activity is first created. */
    static {
          if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
          }
        }
    @Override
    public void onCreate(Bundle savedInstanceState) {

            super.onCreate(savedInstanceState);
        setContentView(R.layout.grabcut_main);
        iv = (ImageView) this.findViewById(R.id.imageView);
iv.setOnTouchListener(this);




    }
    public void open(View v){
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.indexa);
        //Bitmap b=makeBlackTransparent(bitmap);
        //iv.setImageBitmap(b);
      /* try {
            ImageColour colour=new ImageColour(bitmap);
            Log.d("Colour",colour.returnColour());
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        changeWhite(bitmap);


    }
    public void changeWhite(Bitmap image) {
  /*  // load as color image BGR
    cv::Mat input = cv::imread("C:/StackOverflow/Input/transparentWhite.png");

    cv::Mat input_bgra;
    cv::cvtColor(input, input_bgra, CV_BGR2BGRA);

    // find all white pixel and set alpha value to zero:
    for (int y = 0; y < input_bgra.rows; ++y)
        for (int x = 0; x < input_bgra.cols; ++x)
        {
            cv::Vec4b & pixel = input_bgra.at<cv::Vec4b>(y, x);
            // if pixel is white
            if (pixel[0] == 255 && pixel[1] == 255 && pixel[2] == 255)
            {
                // set alpha to zero:
                pixel[3] = 0;
            }
        }

    // save as .png file (which supports alpha channels/transparency)
    cv::imwrite("C:/StackOverflow/Output/transparentWhite.png", input_bgra);*/


// convert image to matrix
        Mat src = new Mat(image.getWidth(), image.getHeight(), CvType.CV_8UC4);
        Utils.bitmapToMat(image, src);
        Mat input_bgra = new Mat(image.getWidth(), image.getHeight(), CvType.CV_8UC4);

        // convert image to grayscale
        Imgproc.cvtColor(src, input_bgra, Imgproc.COLOR_BGR2BGRA);
        Bitmap output = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
       Utils.matToBitmap(input_bgra, output);
       iv.setImageBitmap(output);
        // find all white pixel and set alpha value to zero:
        for (int y = 0; y < input_bgra.rows(); ++y) {
            for (int x = 0; x < input_bgra.cols(); ++x) {
                double[] pixel = input_bgra.get(y, x); //Stores element in an array
                /*for (int k = 0; k < ch; k++) //Runs for the available number of channels
                {
                    data[k] = data[k] * 2; //Pixel modification done here
                }
                img.put(i, j, data); //Puts element back into matrix*/

                // cv::Vec4b & pixel = input_bgra.at<cv::Vec4b>(y, x);
                // if pixel is white
                if (pixel[0] == 255 && pixel[1] == 255 && pixel[2] == 255) {
                    // set alpha to zero:
                    pixel[3] = 0;
                    pixel[0]=0;
                    pixel[1]=0;
                    pixel[2]=0;
                    input_bgra.put(y,x,pixel);
                   // Log.d("white",pixel.toString());

                }else{
                    pixel[0]=255;
                    pixel[1]=255;
                    pixel[2]=0;
                    input_bgra.put(y,x,pixel);
                }
            }
        }
        Bitmap output2 = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(input_bgra, output2);
        iv.setImageBitmap(output2);

    }
    public void cutBg(){
        Scalar color = new Scalar(255, 0, 0, 255);
        Mat dst = new Mat();
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.index);
        Log.d(TAG, "bitmap: " + bitmap.getWidth() + "x" + bitmap.getHeight());


        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Log.d(TAG, "bitmap 8888: " + bitmap.getWidth() + "x" + bitmap.getHeight());


        //GrabCut part
        Mat img = new Mat();
        Utils.bitmapToMat(bitmap, img);
        Log.d(TAG, "img: " + img);

        int r = img.rows();
        int c = img.cols();

        Point p1 = new Point(c/5, r/5);
        Point p2 = new Point(c-c/5, r-r/8);

        Rect rect = new Rect(p1,p2);
        //Rect rect = new Rect(50,30, 100,200);
        Log.d(TAG, "rect: " + rect);

        Mat mask = new Mat();
        debugger(""+mask.type());
        mask.setTo(new Scalar(125));
        Mat fgdModel = new Mat();
        fgdModel.setTo(new Scalar(255, 255, 255));
        Mat bgdModel = new Mat();
        bgdModel.setTo(new Scalar(255, 255, 255));

        Mat imgC3 = new Mat();
        Imgproc.cvtColor(img, imgC3, Imgproc.COLOR_RGBA2RGB);
        Log.d(TAG, "imgC3: " + imgC3);

        Log.d(TAG, "Grabcut begins");
        Imgproc.grabCut(imgC3, mask, rect, bgdModel, fgdModel, 5, Imgproc.GC_INIT_WITH_RECT);

        Mat source = new Mat(1, 1, CvType.CV_8U, new Scalar(3.0));


        Core.compare(mask, source, mask, Core.CMP_EQ);
        Mat foreground = new Mat(img.size(), CvType.CV_8UC3, new Scalar(255, 255, 255));
        img.copyTo(foreground, mask);
        Imgproc.rectangle(img, p1, p2, color);

        Mat background = new Mat();
        try {
            background = Utils.loadResource(getApplicationContext(),
                    R.drawable.wall2 );
        } catch (IOException e) {

            e.printStackTrace();
        }
        Mat tmp = new Mat();
        Imgproc.resize(background, tmp, img.size());

        background = tmp;

        Mat tempMask = new Mat(foreground.size(), CvType.CV_8UC1, new Scalar(255, 255, 255));
        Imgproc.cvtColor(foreground, tempMask, 6/* COLOR_BGR2GRAY */);
        //Imgproc.threshold(tempMask, tempMask, 254, 255, 1 /* THRESH_BINARY_INV */);

        Mat vals = new Mat(1, 1, CvType.CV_8UC3, new Scalar(0.0));
        dst = new Mat();
        background.setTo(vals, tempMask);
        Imgproc.resize(foreground, tmp, mask.size());
        foreground = tmp;
        Core.add(background, foreground, dst, tempMask);

        //convert to Bitmap
        Log.d(TAG, "Convert to Bitmap");
        Utils.matToBitmap(dst, bitmap);

        iv.setBackgroundResource(R.drawable.wall2);
        iv.setImageBitmap(makeBlackTransparent(bitmap));
        //release MAT part
        img.release();
        imgC3.release();
        mask.release();
        fgdModel.release();
        bgdModel.release();
    }
    /**

     * Make the black background of a PNG-Bitmap-Image transparent.
     * code based on example at http://j.mp/1uCxOV5
     * @Param image png bitmap image
     * @return output image
     */

    private static Bitmap makeBlackTransparent(Bitmap image) {
        // convert image to matrix
        Mat src = new Mat(image.getWidth(), image.getHeight(), CvType.CV_8UC4);
        Utils.bitmapToMat(image, src);

        // init new matrices
        Mat dst = new Mat(image.getWidth(), image.getHeight(), CvType.CV_8UC4);
        Mat tmp = new Mat(image.getWidth(), image.getHeight(), CvType.CV_8UC4);
        Mat alpha = new Mat(image.getWidth(), image.getHeight(), CvType.CV_8UC4);

        // convert image to grayscale
        Imgproc.cvtColor(src, tmp, Imgproc.COLOR_BGR2GRAY);


        // threshold the image to create alpha channel with complete transparency in black background region and zero transparency in foreground object region.
        Imgproc.threshold(tmp, alpha, 100, 255, Imgproc.THRESH_BINARY);

        // split the original image into three single channel.
        List<Mat> rgb = new ArrayList<>(3);
        Core.split(src, rgb);

        // Create the final result by merging three single channel and alpha(BGRA order)
        List<Mat> rgba = new ArrayList<Mat>(4);
        rgba.add(rgb.get(0));
        rgba.add(rgb.get(1));
        rgba.add(rgb.get(2));
        rgba.add(alpha);
        Core.merge(rgba, dst);

        // convert matrix to output bitmap
        Bitmap output = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst, output);
        return output;
    }
    public void debugger(String s){
        Log.v("","########### "+s);
    }
    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        ImageView view = (ImageView) v;
        view.setScaleType(ImageView.ScaleType.MATRIX);
        float scale;

        dumpEvent(event);
        // Handle touch events here...

        switch (event.getAction() & MotionEvent.ACTION_MASK)
        {
            case MotionEvent.ACTION_DOWN:   // first finger down only
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                Log.d(TAG, "mode=DRAG"); // write to LogCat
                mode = DRAG;
                break;

            case MotionEvent.ACTION_UP: // first finger lifted

            case MotionEvent.ACTION_POINTER_UP: // second finger lifted

                mode = NONE;
                Log.d(TAG, "mode=NONE");
                break;

            case MotionEvent.ACTION_POINTER_DOWN: // first and second finger down

                oldDist = spacing(event);
                Log.d(TAG, "oldDist=" + oldDist);
                if (oldDist > 5f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                    Log.d(TAG, "mode=ZOOM");
                }
                break;

            case MotionEvent.ACTION_MOVE:

                if (mode == DRAG)
                {
                    matrix.set(savedMatrix);
                    matrix.postTranslate(event.getX() - start.x, event.getY() - start.y); // create the transformation in the matrix  of points
                }
                else if (mode == ZOOM)
                {
                    // pinch zooming
                    float newDist = spacing(event);
                    Log.d(TAG, "newDist=" + newDist);
                    if (newDist > 5f)
                    {
                        matrix.set(savedMatrix);
                        scale = newDist / oldDist; // setting the scaling of the
                        // matrix...if scale > 1 means
                        // zoom in...if scale < 1 means
                        // zoom out
                        matrix.postScale(scale, scale, mid.x, mid.y);
                    }
                }
                break;
        }

        view.setImageMatrix(matrix); // display the transformation on screen

        return true; // indicate event was handled
    }

    /*
     * --------------------------------------------------------------------------
     * Method: spacing Parameters: MotionEvent Returns: float Description:
     * checks the spacing between the two fingers on touch
     * ----------------------------------------------------
     */

    private float spacing(MotionEvent event)
    {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /*
     * --------------------------------------------------------------------------
     * Method: midPoint Parameters: PointF object, MotionEvent Returns: void
     * Description: calculates the midpoint between the two fingers
     * ------------------------------------------------------------
     */

    private void midPoint(PointF point, MotionEvent event)
    {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    /** Show an event in the LogCat view, for debugging */
    private void dumpEvent(MotionEvent event)
    {
        String names[] = { "DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE","POINTER_DOWN", "POINTER_UP", "7?", "8?", "9?" };
        StringBuilder sb = new StringBuilder();
        int action = event.getAction();
        int actionCode = action & MotionEvent.ACTION_MASK;
        sb.append("event ACTION_").append(names[actionCode]);

        if (actionCode == MotionEvent.ACTION_POINTER_DOWN || actionCode == MotionEvent.ACTION_POINTER_UP)
        {
            sb.append("(pid ").append(action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
            sb.append(")");
        }

        sb.append("[");
        for (int i = 0; i < event.getPointerCount(); i++)
        {
            sb.append("#").append(i);
            sb.append("(pid ").append(event.getPointerId(i));
            sb.append(")=").append((int) event.getX(i));
            sb.append(",").append((int) event.getY(i));
            if (i + 1 < event.getPointerCount())
                sb.append(";");
        }

        sb.append("]");
        Log.d("Touch Events ---------", sb.toString());
    }
    private void findPhoto() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    FINE_MEDIA_ACCESS_PERMISSION_REQUEST);
        } else {
            //  mMap.setMyLocationEnabled(true);
            Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, RESULT_LOAD_IMAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case FINE_MEDIA_ACCESS_PERMISSION_REQUEST: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    findPhoto();
                }
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            iv.setImageBitmap(photo);
           /* Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);

            cursor.close();*/

        }
    }
}
