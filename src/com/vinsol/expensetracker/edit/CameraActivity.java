package com.vinsol.expensetracker.edit;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.vinsol.expensetracker.DatabaseAdapter;
import com.vinsol.expensetracker.R;
import com.vinsol.expensetracker.listing.ExpenseListing;
import com.vinsol.expensetracker.show.ShowCameraActivity;
import com.vinsol.expensetracker.show.ShowTextActivity;
import com.vinsol.expensetracker.utils.CameraFileSave;
import com.vinsol.expensetracker.utils.DateHelper;
import com.vinsol.expensetracker.utils.FileDelete;
import com.vinsol.expensetracker.utils.ImagePreview;
import com.vinsol.expensetracker.utils.LocationHelper;

public class CameraActivity extends EditAbstract implements OnClickListener {

	private static final int PICTURE_RESULT = 35;
	private LinearLayout editCameraDetails;
	private ImageView editImageDisplay;
	private RelativeLayout editLoadProgress;
	private Button editDelete;
	private Button editSaveEntry;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		// //////********* Get id from intent extras ******** ////////////
		intentExtras = getIntent().getBundleExtra("cameraBundle");
		
		// ////// ******** Initializing and assigning memory to UI Items
		// ********** /////////

		editCameraDetails = (LinearLayout) findViewById(R.id.edit_camera_details);
		editImageDisplay = (ImageView) findViewById(R.id.edit_image_display);
		editLoadProgress = (RelativeLayout) findViewById(R.id.edit_load_progress);
		editSaveEntry = (Button) findViewById(R.id.edit_save_entry);
		editDelete = (Button) findViewById(R.id.edit_delete);
		typeOfEntry = R.string.voice;
		typeOfEntryFinished = R.string.finished_voiceentry;
		typeOfEntryUnfinished = R.string.unfinished_voiceentry;
		editHelper();
		if (intentExtras.containsKey("mDisplayList")) {
			if(setUnknown){
				startCamera();
			}
			File mFile = new File("/sdcard/ExpenseTracker/" + userId + "_small.jpg");
			if (mFile.canRead()) {
				Drawable mDrawable = Drawable.createFromPath(mFile.getPath());
				setImageResource(mDrawable);
			} else {
				editImageDisplay.setImageResource(R.drawable.no_image_small);
			}
		}
		
		setGraphicsCamera();
		setClickListeners();

		// ////// *********** Initializing Database Adaptor **********
		// //////////
		mDatabaseAdapter = new DatabaseAdapter(this);
		
		dateViewString = dateBarDateview.getText().toString();
		
		if(userId == null ) {
			if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
				
				HashMap<String, String> toInsert = new HashMap<String, String>();
				if (!dateBarDateview.getText().toString().equals(dateViewString)) {
					try {
						if (!intentExtras.containsKey("mDisplayList")) {
							DateHelper mDateHelper = new DateHelper(dateBarDateview.getText().toString());
							toInsert.put(DatabaseAdapter.KEY_DATE_TIME,mDateHelper.getTimeMillis() + "");
						} else {
							if(!intentExtras.containsKey("timeInMillis")){
								DateHelper mDateHelper = new DateHelper(dateBarDateview.getText().toString());
								toInsert.put(DatabaseAdapter.KEY_DATE_TIME, mDateHelper.getTimeMillis()+"");
							} else {
								Calendar mCalendar = Calendar.getInstance();
								mCalendar.setTimeInMillis(intentExtras.getLong("timeInMillis"));
								mCalendar.setFirstDayOfWeek(Calendar.MONDAY);
								DateHelper mDateHelper = new DateHelper(dateBarDateview.getText().toString(),mCalendar);
								toInsert.put(DatabaseAdapter.KEY_DATE_TIME, mDateHelper.getTimeMillis()+"");
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					Calendar mCalendar = Calendar.getInstance();
					mCalendar.setFirstDayOfWeek(Calendar.MONDAY);
					toInsert.put(DatabaseAdapter.KEY_DATE_TIME, mCalendar.getTimeInMillis()+"");
				}
				
				if (LocationHelper.currentAddress != null && LocationHelper.currentAddress.trim() != "") {
					toInsert.put(DatabaseAdapter.KEY_LOCATION, LocationHelper.currentAddress);
				}
				
				toInsert.put(DatabaseAdapter.KEY_TYPE, getString(R.string.camera));
				mDatabaseAdapter.open();
				userId = mDatabaseAdapter.insertToDatabase(toInsert);
				mDatabaseAdapter.close();
			}
		}
		
		if (!intentExtras.containsKey("mDisplayList"))
			startCamera();
		
	}

	private void setImageResource(Drawable mDrawable) {
		if(mDrawable.getIntrinsicHeight() > mDrawable.getIntrinsicWidth()) {
			final float scale = this.getResources().getDisplayMetrics().density;
			int width = (int) (84 * scale + 0.5f);
			int height = (int) (111 * scale + 0.5f);			
			editImageDisplay.setLayoutParams(new LayoutParams(width, height));
		}
		editImageDisplay.setImageDrawable(mDrawable);
	}
	
	private void startCamera() {
		// ///// ******* Starting Camera to capture Image ******** //////////
		if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
			Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			File path = new File("/mnt/sdcard/ExpenseTracker");
			path.mkdirs();
			String name = userId + ".jpg";
			File file = new File(path, name);
			camera.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
			startActivityForResult(camera, PICTURE_RESULT);
		} else {
			Toast.makeText(this, "sdcard not available", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (PICTURE_RESULT == requestCode) {
			if(Activity.RESULT_OK == resultCode) {
				isChanged = true;
				new SaveAndDisplayImage().execute();
			} else {
				isChanged = false;
				if(!setUnknown) {
					File mFile = new File("/sdcard/ExpenseTracker/" + userId+ "_small.jpg");
					if (mFile.canRead()) {
						Drawable mDrawable = Drawable.createFromPath(mFile.getPath());
						setImageResource(mDrawable);
					} else {
						DatabaseAdapter adapter = new DatabaseAdapter(this);
						adapter.open();
						adapter.deleteDatabaseEntryID(userId + "");
						adapter.close();
					}
				}
				if(!intentExtras.containsKey("isFromShowPage")) {
					finish();
				}
			}
		}
	}

	private class SaveAndDisplayImage extends AsyncTask<Void, Void, Void> {
		@Override
		protected void onPreExecute() {
			editLoadProgress.setVisibility(View.VISIBLE);
			editImageDisplay.setVisibility(View.GONE);
			editDelete.setEnabled(false);
			editSaveEntry.setEnabled(false);
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params) {
			new CameraFileSave(CameraActivity.this).resizeImageAndSaveThumbnails(userId + "");
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			editLoadProgress.setVisibility(View.GONE);
			editImageDisplay.setVisibility(View.VISIBLE);
			File mFile = new File("/sdcard/ExpenseTracker/" + userId+ "_small.jpg");
			Drawable mDrawable = Drawable.createFromPath(mFile.getPath());
			setImageResource(mDrawable);
			editDelete.setEnabled(true);
			editSaveEntry.setEnabled(true);
			super.onPostExecute(result);
		}
	}

	private void setGraphicsCamera() {
		// ///// ***** Sets Title Camera Entry *********///////
		editHeaderTitle.setText("Camera Entry");

		// //// ****** Shows Camera Details ********////////
		editCameraDetails.setVisibility(View.VISIBLE);
	}

	private void setClickListeners() {
		// ////// ******* Adding Click Listeners to UI Items ******** //////////
		editSaveEntry.setOnClickListener(this);
		editDelete.setOnClickListener(this);
		ImageView editImageDisplay = (ImageView) findViewById(R.id.edit_image_display);
		editImageDisplay.setOnClickListener(this);
		Button editRetakeButton = (Button) findViewById(R.id.edit_retake_button);
		editRetakeButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		// ////// ******** Adding Action to save entry ********* ///////////
		if (v.getId() == R.id.edit_save_entry) {
			saveEntry();
		}

		// /////// ********* Adding action if delete button ********** /////////
		if (v.getId() == R.id.edit_delete) {
			new FileDelete(userId);

			// //// ******* Delete entry from database ******** /////////
			mDatabaseAdapter.open();
			mDatabaseAdapter.deleteDatabaseEntryID(Long.toString(userId));
			mDatabaseAdapter.close();
			if(intentExtras.containsKey("isFromShowPage")){
				Intent mIntent = new Intent(this, ShowTextActivity.class);
				ArrayList<String> listOnResult = new ArrayList<String>();
				listOnResult.add("");
				Bundle tempBundle = new Bundle();
				tempBundle.putStringArrayList("mDisplayList", listOnResult);
				mEditList = new ArrayList<String>();
				mEditList.addAll(listOnResult);
				mIntent.putExtra("textShowBundle", tempBundle);
				setResult(Activity.RESULT_CANCELED, mIntent);
			}
			finish();
		}

		// //////// ********** Adding action if image is pressed ********		 ///////////
		if (v.getId() == R.id.edit_image_display) {
			File mFile = new File("/sdcard/ExpenseTracker/" + userId + ".jpg");
			if(mFile.canRead()) {
				Intent intent = new Intent(this, ImagePreview.class);
				intent.putExtra("id", userId);
				startActivity(intent);
			} else {
				Toast.makeText(this, "no image to preview", Toast.LENGTH_SHORT).show();
			}
		}

		// /////// ********** Adding action if retake button is pressed ******//////////
		if (v.getId() == R.id.edit_retake_button) {
			startCamera();
		}
	}

	private void saveEntry() {
		
		HashMap<String, String> toSave = getSaveEntryData(dateBarDateview,dateViewString);
		
		// //// ******* Update database if user added additional info *******		 ///////
		mDatabaseAdapter.open();
		mDatabaseAdapter.editDatabase(toSave);
		mDatabaseAdapter.close();
		if(!intentExtras.containsKey("isFromShowPage")){
			Intent intentExpenseListing = new Intent(this, ExpenseListing.class);
			Bundle mToHighLight = new Bundle();
			mToHighLight.putString("toHighLight", toSave.get(DatabaseAdapter.KEY_ID));
			intentExpenseListing.putExtras(mToHighLight);
			intentExpenseListing.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intentExpenseListing);
		} else {
			Intent mIntent = new Intent(this, ShowCameraActivity.class);
			Bundle tempBundle = new Bundle();
			tempBundle.putStringArrayList("mDisplayList", getListOnResult(toSave));
			mIntent.putExtra("cameraShowBundle", tempBundle);
			setResult(Activity.RESULT_OK, mIntent);
		}
		finish();
	}

	// /// ****************** Handling back press of key ********** ///////////
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			onBackPressed();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	public void onBackPressed() {
		// This will be called either automatically for you on 2.0
		// or later, or by the code above on earlier versions of the platform.
		saveEntry();
		return;
	}
}
