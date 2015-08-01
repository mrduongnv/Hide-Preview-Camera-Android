package com.duongnv.cameraexample;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;


public class AndroidCameraExample extends Activity {
	private Button capture, record;
	private CameraModule cameraModule;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		capture = (Button) findViewById(R.id.button_capture);
		capture.setOnClickListener(captureListener);

		record = (Button) findViewById(R.id.button_record);
		record.setOnClickListener(recordListener);
	}

	public void onResume() {
		super.onResume();
		if (cameraModule == null) {
			cameraModule = CameraModule.getInstance(this);
			cameraModule.initialize();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	Timer timer = null;

	OnClickListener recordListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (timer != null) {
				timer.cancel();
				timer = null;
				return;
			}
			timer = new Timer();
			timer.schedule(new TimerTask() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					if (cameraModule != null)
						cameraModule.record();
				}
			}, 1000, 5000);

		}
	};

	OnClickListener captureListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (timer != null) {
				timer.cancel();
				timer = null;
				return;
			}
			timer = new Timer();
			timer.schedule(new TimerTask() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					if (cameraModule != null)
						cameraModule.takePicture();
				}
			}, 1000, 5000);
		}
	};

}