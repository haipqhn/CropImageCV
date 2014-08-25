package vn.haipq.cropimagecv;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.Toast;

public class CropImageCV extends ActionBarActivity implements OnTouchListener {

	static {
		System.loadLibrary("opencv_java");
	}
	private Uri mImageCaptureUri;
	private ImageView mImageView;
	private Bitmap bmp = null;
	Bitmap alteredBitmap = null;
	BitmapFactory.Options options;
	BitmapDrawable[] layers;
	LayerDrawable layerDrawable;
	Canvas canvas;
	Paint paint;
	Matrix matrix;
	float firstx = 0;
	float firsty = 0;
	float downx = 0;
	float downy = 0;
	float upx = 0;
	float upy = 0;

	private static final int PICK_FROM_CAMERA = 1;
	private static final int PICK_FROM_FILE = 2;

	public native void convertToGrayscale(Bitmap bitmapIn, Bitmap bitmapOut);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_crop_image_cv);
		setupActionBar();
		mImageView = (ImageView) findViewById(R.id.ivMain);
	}

	private void setupActionBar() {
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setHomeButtonEnabled(true);
	}

	private void refreshImage() {
		if (bmp != (null)) {
			setImageCanvas();
		}
	}

	private void acceptCut() {
		try {
			if (alteredBitmap != null) {
				Mat origin = new Mat();
				Utils.bitmapToMat(bmp, origin);
				Mat image = new Mat(), image1 = new Mat();
				Utils.bitmapToMat(alteredBitmap, image);
				Mat mask = new Mat(image.rows() + 2, image.cols() + 2,
						CvType.CV_8U);
				Imgproc.cvtColor(image, image1, Imgproc.COLOR_RGB2GRAY, 1);
				Imgproc.threshold(image1, image1, 0, 255,
						Imgproc.THRESH_BINARY_INV);
				Imgproc.floodFill(image1, mask, new Point(0, 0), new Scalar(0));
				int maxX = 0, minX = image.cols()-1, maxY=0, minY = image.rows()-1;double[] temp;
				for (int i=0;i<image.rows();i++){
					for (int j=0;j<image.cols();j++){
						temp=image.get(i, j);
						if (temp[3]==255){
							maxX = max(maxX, j);
					        minX = min(minX, i);

					        maxY = max(maxY, j);
					        minY = min(minY, i);
						}
					}
				}
				Rect roi=new Rect(new Point(minX, minY), new Point(maxX, maxY));
				// Imgproc.cvtColor(image1, image1, Imgproc.COLOR_GRAY2RGBA);
				List<Mat> channels = new ArrayList<Mat>();
				List<Mat> newChannels = new ArrayList<Mat>();
				Core.split(origin, channels);
				for (int i=0;i<origin.channels();i++){
					Core.bitwise_and(channels.get(i), image1, channels.get(i));
					newChannels.add(new Mat(channels.get(i), roi));				}
				
				Mat finalImage = new Mat();
				Core.merge(newChannels, finalImage);
				// Core.bitwise_and(origin, image1, finalImage);
				alteredBitmap=Bitmap.createBitmap(finalImage.rows(), finalImage.cols(), alteredBitmap.getConfig());
				Utils.matToBitmap(finalImage, alteredBitmap);
				mImageView.setImageBitmap(alteredBitmap);
			} else {
				Toast.makeText(getApplicationContext(), "Chưa nạp ảnh!",
						Toast.LENGTH_SHORT).show();
			}
		} catch (NullPointerException e) {
			Toast.makeText(getApplicationContext(), e.toString(),
					Toast.LENGTH_SHORT).show();
		}
	}

	private void openImage() {
		Intent choosePictureIntent = new Intent(Intent.ACTION_PICK,
				android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(choosePictureIntent, 0);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.layout.main_activity_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		switch (id) {
		case android.R.id.home:
			finish();
			return true;
		case R.id.action_refresh:
			refreshImage();
			return true;
		case R.id.action_openimage:
			openImage();
			return true;
		case R.id.action_accept:
			acceptCut();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		options = new BitmapFactory.Options();
		options.inPreferredConfig = Config.ARGB_8888;

		if (resultCode == RESULT_OK) {
			Uri imageFileUri = data.getData();
			try {
				BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
				bmpFactoryOptions.inJustDecodeBounds = true;
				bmp = BitmapFactory
						.decodeStream(
								getContentResolver().openInputStream(
										imageFileUri), null, bmpFactoryOptions);

				bmpFactoryOptions.inJustDecodeBounds = false;
				bmp = BitmapFactory
						.decodeStream(
								getContentResolver().openInputStream(
										imageFileUri), null, bmpFactoryOptions);
				setImageCanvas();
			} catch (Exception e) {
				Log.v("ERROR", e.toString());
			}
		}
	}
	
	private int max(int a,int b){
		if (a>b){
			return a;
		} else {
			return b;
		}
	}
	private int min(int a,int b){
		if (a<b){
			return a;
		} else {
			return b;
		}
	}

	private void setImageCanvas() {
		alteredBitmap = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(),
				bmp.getConfig());
		canvas = new Canvas(alteredBitmap);
		paint = new Paint();
		int opacity = 255;
		int intColor = Color.argb(opacity, 0, 255, 0);
		int transparentColor = Color.argb(0, 255, 255, 255);

		paint.setColor(intColor);
		paint.setStrokeWidth(5);
		matrix = new Matrix();
		canvas.drawColor(transparentColor);

		layers = new BitmapDrawable[2];
		layers[0] = new BitmapDrawable(bmp);
		layers[1] = new BitmapDrawable(alteredBitmap);
		layerDrawable = new LayerDrawable(layers);
		mImageView.setImageDrawable(layerDrawable);
		mImageView.setOnTouchListener(this);
		mImageView.invalidate();
	}

	public String getRealPathFromURI(Uri contentUri) {
		String[] proj = { MediaStore.Images.Media.DATA };
		Cursor cursor = managedQuery(contentUri, proj, null, null, null);

		if (cursor == null)
			return null;

		int column_index = cursor
				.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

		cursor.moveToFirst();

		return cursor.getString(column_index);
	}

	public byte[] convertBitmapToByteArray(Bitmap bitmap) {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream(
				bitmap.getWidth() * bitmap.getHeight());
		bitmap.compress(CompressFormat.PNG, 100, buffer);
		return buffer.toByteArray();
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int action = event.getAction();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			downx = event.getX();
			downy = event.getY();
			firstx = downx;
			firsty = downy;
			break;
		case MotionEvent.ACTION_MOVE:
			upx = event.getX();
			upy = event.getY();
			canvas.drawLine(downx, downy, upx, upy, paint);
			mImageView.invalidate();
			downx = upx;
			downy = upy;
			break;
		case MotionEvent.ACTION_UP:
			upx = event.getX();
			upy = event.getY();
			canvas.drawLine(downx, downy, upx, upy, paint);
			if (firstx != upx || firsty != upy) {
				canvas.drawLine(upx, upy, firstx, firsty, paint);
			}
			mImageView.invalidate();
			break;
		case MotionEvent.ACTION_CANCEL:
			break;
		default:
			break;
		}
		return true;
	}
}
