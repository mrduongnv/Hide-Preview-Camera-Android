package com.duongnv.cameraexample;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;

public class CameraModule {

	private static final String TAG = "CameraModule-DUONGNV";

	private String flash = null;
	private File outputPictureFile = null;
	private File outputVideoFile = null;

	private Context _context;
	private Camera mCamera;
	private PictureCallback mPicture;
	private SurfaceTexture surfaceTexture;
	private SurfaceView mPreview;
	private MediaRecorder mMediaRecorder;
	
	private static CameraModule cameraModule;

	private boolean isRecording = false;

	public static CameraModule getInstance(Context context) {
		//if (cameraModule == null)
		cameraModule = new CameraModule(context);
		return cameraModule;
	}

	private CameraModule(Context context) {
		this._context = context;
		mPreview = new SurfaceView(this._context);
	}

	public void record() {
		if (isRecording) {
			mMediaRecorder.stop();
			releaseMediaRecorder();
			mCamera.lock();

			isRecording = false;
			releaseCamera();

			// Initialize camera again for taking photo
			initialize();
		} else {
			new RecordPrepareTask().execute(null, null, null);
		}
	}

	public void takePicture() {
		if (isRecording) {
			mMediaRecorder.stop();
			releaseMediaRecorder();
			mCamera.lock();
			isRecording = false;
			releaseCamera();

		} else {
			new CapturePrepareTask().execute(null, null, null);
		}
	}

	public void setFlash(String flash) {
		this.flash = flash;
	}

	private int findFrontFacingCamera() {
		int cameraId = -1;
		// Search for the front facing camera
		int numberOfCameras = Camera.getNumberOfCameras();
		for (int i = 0; i < numberOfCameras; i++) {
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(i, info);
			if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
				cameraId = i;
				break;
			}
		}
		return cameraId;
	}

	private int findBackFacingCamera() {
		int cameraId = -1;
		int numberOfCameras = Camera.getNumberOfCameras();
		for (int i = 0; i < numberOfCameras; i++) {
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(i, info);
			if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
				cameraId = i;
				break;
			}
		}
		return cameraId;
	}

	public boolean initialize() {
		if (!hasCamera(_context)) {
			return false;
		}
		if (mCamera == null) {
			if (findFrontFacingCamera() < 0) {
				return false;
			}
			mCamera = Camera.open(findBackFacingCamera());
			mPicture = getPictureCallback();
		}

		surfaceTexture = new SurfaceTexture(10);
		try {
			mCamera.setPreviewTexture(surfaceTexture);
		} catch (IOException e) {
			Log.d(TAG, e.getMessage());
			return false;
		}
		mCamera.startPreview();
		return true;
	}

	private boolean hasCamera(Context context) {
		if (context.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_CAMERA)) {
			return true;
		} else {
			return false;
		}
	}

	private PictureCallback getPictureCallback() {
		PictureCallback picture = new PictureCallback() {

			@Override
			public void onPictureTaken(byte[] data, Camera camera) {
				// make a new picture file
				File pictureFile = getOutputPictureFile();

				if (pictureFile == null) {
					return;
				}
				try {
					FileOutputStream fos = new FileOutputStream(pictureFile);
					fos.write(data);
					fos.close();

				} catch (FileNotFoundException e) {
					Log.d(TAG, e.getMessage());
				} catch (IOException e) {
					Log.d(TAG, e.getMessage());
				}
				// refresh camera to continue preview
				refreshCamera();
			}
		};
		return picture;
	}

	private void refreshCamera() {
		try {
			mCamera.stopPreview();
		} catch (Exception e) {
		}
		try {
			SurfaceTexture surfaceTexture = new SurfaceTexture(10);
			mCamera.setPreviewTexture(surfaceTexture);
			mCamera.startPreview();
		} catch (Exception e) {
			Log.d(TAG, e.getMessage());
		}
	}

	public void setOutputPictureFile(File outputPictureFile) {
		this.outputPictureFile = outputPictureFile;
	}

	public void setOutputVideoFile(File outputVideoFile) {
		this.outputVideoFile = outputVideoFile;
	}

	public File getOutputPictureFile() {
		if (outputPictureFile != null)
			return outputPictureFile;
		File mediaStorageDir = new File(Environment
				.getExternalStorageDirectory().getPath(), "CameraModule");

		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				return null;
			}
		}

		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(new Date());
		File mediaFile;
		mediaFile = new File(mediaStorageDir.getPath() + File.separator
				+ "CAMERA_IMG_" + timeStamp + ".jpg");
		return mediaFile;
	}

	public File getOutputVideoFile() {
		if (outputVideoFile != null)
			return outputVideoFile;

		File mediaStorageDir = new File(Environment
				.getExternalStorageDirectory().getPath(), "CameraModule");

		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				return null;
			}
		}

		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(new Date());
		File mediaFile;
		mediaFile = new File(mediaStorageDir.getPath() + File.separator
				+ "CAMERA_VID_" + timeStamp + ".3gp");
		return mediaFile;
	}

	private void releaseCamera() {
		if (mCamera != null) {
			mCamera.release();
			mCamera = null;
		}
	}

	private void releaseMediaRecorder() {
		if (mMediaRecorder != null) {
			mMediaRecorder.reset();
			mMediaRecorder.release();
			mMediaRecorder = null;
			mCamera.lock();
		}
	}

	private static Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes,
			int w, int h) {
		final double ASPECT_TOLERANCE = 0.1;
		double targetRatio = (double) w / h;
		if (sizes == null)
			return null;

		Camera.Size optimalSize = null;

		double minDiff = Double.MAX_VALUE;

		int targetHeight = h;

		for (Camera.Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}

		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Camera.Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}
		return optimalSize;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private boolean prepareVideoRecorder() {

		releaseCamera();

		mCamera = Camera.open();

		Camera.Parameters parameters = mCamera.getParameters();
		List<Camera.Size> mSupportedPreviewSizes = parameters
				.getSupportedPreviewSizes();
		Camera.Size optimalSize = getOptimalPreviewSize(mSupportedPreviewSizes,
				mPreview.getWidth(), mPreview.getHeight());

		// Use the same size for recording profile.
		CamcorderProfile profile = CamcorderProfile
				.get(CamcorderProfile.QUALITY_HIGH);
		profile.videoFrameWidth = optimalSize.width;
		profile.videoFrameHeight = optimalSize.height;

		// likewise for the camera object itself.
		parameters.setPreviewSize(profile.videoFrameWidth,
				profile.videoFrameHeight);

		// TODO: Parameters here
		if (flash != null)
			parameters.setFlashMode(flash);

		mCamera.setParameters(parameters);
		try {
			// Requires API level 11+, For backward compatibility use {@link
			// setPreviewDisplay}
			// with {@link SurfaceView}
			// mCamera.setPreviewDisplay(mPreview.getHolder());
			SurfaceTexture surfaceTexture = new SurfaceTexture(10);
			mCamera.setPreviewTexture(surfaceTexture);
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
			return false;
		}
		mMediaRecorder = new MediaRecorder();

		// Step 1: Unlock and set camera to MediaRecorder
		mCamera.unlock();
		mMediaRecorder.setCamera(mCamera);

		// Step 2: Set sources
		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		// Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
		mMediaRecorder.setProfile(profile);

		// Step 4: Set output file
		mMediaRecorder.setOutputFile(getOutputVideoFile().getAbsolutePath());
		// END_INCLUDE (configure_media_recorder)

		// Step 5: Prepare configured MediaRecorder
		try {
			mMediaRecorder.prepare();
		} catch (IllegalStateException e) {
			Log.d(TAG, e.getMessage());
			releaseMediaRecorder();
			return false;
		} catch (IOException e) {
			Log.d(TAG, e.getMessage());
			releaseMediaRecorder();
			return false;
		}
		return true;
	}

	class CapturePrepareTask extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... voids) {
			// initialize camera
			if (isRecording) {
				return false;

			}
			if (mCamera == null) {
				if (initialize())
					return false;
				// FIXME: Should sleep a few seconds
			}
			mCamera.takePicture(null, null, mPicture);
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			// Nothing to do
		}
	}

	class RecordPrepareTask extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... voids) {
			// initialize video camera
			if (prepareVideoRecorder()) {
				mMediaRecorder.start();
				isRecording = true;
			} else {
				releaseMediaRecorder();
				return false;
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			// Nothing to do
		}
	}

	public void release() {
		releaseMediaRecorder();
		releaseCamera();
	}
}
