package com.emdoor.gmsinstaller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RecoverySystem;
import android.util.Log;
import android.view.Window;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;

public class MainActivity extends Activity implements OnClickListener ,OnCancelListener {

	private static final String UPDATE_FILE_NAME = "/mnt/sdcard/update_google_apps_emdoor_temp.zip";
	private static final int MSG_EXTRACT_FILE_START = 0;
	private static final int MSG_EXTRACT_FILE_FAIL = 1;
	private static final int MSG_EXTRACT_FILE_SUCCESS = 2;
	private static final String TAG = "MainActivity";
	private ProgressDialog extractDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		showInstallDialog();

	}

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
			case MSG_EXTRACT_FILE_START:
				showExtractFileDialog();
				break;
			case MSG_EXTRACT_FILE_SUCCESS:
				if (extractDialog != null) {
					extractDialog.dismiss();
				}
				if(MainActivity.this.isFinishing())
				{
					return;
				}
				if(installUpdateFile()){
					Log.d(TAG, "start to install update package");
					MainActivity.this.finish();
				}else {
					showFailDialog(getString(R.string.install_update_file_error));
					Log.d(TAG, "fail to install update package");
				}
				break;
			case MSG_EXTRACT_FILE_FAIL:
				if (extractDialog != null) {
					extractDialog.dismiss();
				}
				if(MainActivity.this.isFinishing())
				{
					return;
				}
				showFailDialog(getString(R.string.extract_update_file_error));
				break;
			default:
				break;
			}

		}

	};

	private boolean installUpdateFile()
	{
		File packageFile=new File(UPDATE_FILE_NAME);
		if(!packageFile.exists())
		{
			return false;
		}
		try {
			RecoverySystem.installPackage(this, packageFile);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
	}
	

	private void showInstallDialog() {

		AlertDialog.Builder builder = new Builder(this);
		builder.setTitle(R.string.instal_google_apps_title);

		builder.setMessage(R.string.instal_google_apps_content);

		builder.setNegativeButton(R.string.cancel, this);
		builder.setPositiveButton(R.string.ok, this);
		
		AlertDialog dialog= builder.create();
		dialog.setOnCancelListener(this);
		dialog.show();
	}

	

	private void showFailDialog(String msg) {

		if(this.isFinishing())
		{
			return;
		}
		
		AlertDialog.Builder builder = new Builder(this);
		builder.setTitle(R.string.error_occurred);

		builder.setMessage(msg);
		
		builder.setNegativeButton(R.string.ok, this);

		AlertDialog dialog= builder.create();
		dialog.setOnCancelListener(this);
		dialog.show();
	}

	private void showExtractFileDialog() {
		if(this.isFinishing())
		{
			return;
		}
		
		extractDialog = new ProgressDialog(this);
		extractDialog.setTitle(R.string.extract_update_file_title);
		extractDialog.setMessage(getString( R.string.extract_update_file_content));
		extractDialog.setCancelable(false);
		extractDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString( R.string.cancel), this);
		extractDialog.show();
	}

	public void extractUpdateFile() {

		Message msg=new Message();
		msg.what=MSG_EXTRACT_FILE_START;
		mHandler.sendMessage(msg);
		InputStream is = getResources()
				.openRawResource(R.raw.google_apps_patch);
		try {
			int length=is.available();
			if (length <= 0) {
				return ;
			}
			Log.d(TAG, "InputStream lenth="+length);
			byte[] buffer = new byte[10240];

			File outFile = new File(UPDATE_FILE_NAME);
			if (!outFile.getParentFile().exists()) {
				outFile.getParentFile().mkdir();
			}
			OutputStream out = new FileOutputStream(outFile);
			while (true) {
				int count = is.read(buffer);
				if (count == -1) {
					break;
				}
				if(this.isFinishing())
				{				
					break;
				}
				
				out.write(buffer, 0, count);
			}
			is.close();
			out.close();
			if(isFinishing())
			{
				outFile.delete();
				return;
			}
			Log.d(TAG, "update file lenth="+outFile.length());
			if(outFile.exists()&&outFile.length()==length)
			{
				msg=new Message();
				msg.what=MSG_EXTRACT_FILE_SUCCESS;
				mHandler.sendMessage(msg);
			}else {
				msg=new Message();
				msg.what=MSG_EXTRACT_FILE_FAIL;
				mHandler.sendMessage(msg);
			}
			

		} catch (Exception e) {
			e.printStackTrace();
			msg=new Message();
			msg.what=MSG_EXTRACT_FILE_FAIL;
			mHandler.sendMessage(msg);
			return ;
		}

	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
		case DialogInterface.BUTTON_POSITIVE:
			new Thread() {
				@Override
				public void run() {
					extractUpdateFile();
				}

			}.start();
			
			break;
		default:
			this.finish();
			break;

		}
	}


	@Override
	public void onCancel(DialogInterface dialog) {
		this.finish();
		
	}


}
