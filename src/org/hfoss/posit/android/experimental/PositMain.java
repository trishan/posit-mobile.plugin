/*
 * File: PositMain.java
 * 
 * Copyright (C) 2009 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Search and Identification Tool.
 *
 * POSIT is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License (LGPL) as published 
 * by the Free Software Foundation; either version 3.0 of the License, or (at
 * your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU LGPL along with this program; 
 * if not visit http://www.gnu.org/licenses/lgpl.html.
 * 
 */
package org.hfoss.posit.android.experimental;

import java.util.ArrayList;

import org.hfoss.posit.android.experimental.api.LocaleManager;
import org.hfoss.posit.android.experimental.api.User;
import org.hfoss.posit.android.experimental.api.activity.ListProjectsActivity;
//import org.hfoss.posit.android.experimental.api.activity.LoginActivity;
import org.hfoss.posit.android.experimental.api.activity.MapFindsActivity;
import org.hfoss.posit.android.experimental.api.activity.SettingsActivity;
import org.hfoss.posit.android.experimental.api.database.DbManager;
import org.hfoss.posit.android.experimental.plugin.FindActivityProvider;
import org.hfoss.posit.android.experimental.plugin.FindPluginManager;
import org.hfoss.posit.android.experimental.plugin.FunctionPlugin;
import org.hfoss.posit.android.experimental.plugin.acdivoca.AcdiVocaFind;
import org.hfoss.posit.android.experimental.plugin.acdivoca.AttributeManager;
import org.hfoss.posit.android.experimental.plugin.Plugin;
import org.hfoss.posit.android.experimental.sync.Communicator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;

/**
 * Implements the main activity and the main screen for the POSIT application.
 */
public class PositMain extends OrmLiteBaseActivity<DbManager> implements android.view.View.OnClickListener {

	// extends Activity implements OnClickListener { //,RWGConstants {

	private static final String TAG = "PositMain";

	private static final int CONFIRM_EXIT = 0;

//	public static final int LOGIN_CANCELED = 3;
//	public static final int LOGIN_SUCCESSFUL = 4;

	private SharedPreferences mSharedPrefs;
	private Editor mSpEditor;
	
	//private boolean mMainMenuExtensionPointEnabled = false;
	private ArrayList<FunctionPlugin> mMainMenuPlugins = null;
	private FunctionPlugin mMainLoginPlugin = null;
	

	/**
	 * Called when the activity is first created. Sets the UI layout, adds the
	 * buttons, checks whether the phone is registered with a POSIT server.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "Creating");

		// Initialize plugins and managers
		FindPluginManager.initInstance(this);
		mMainMenuPlugins = FindPluginManager.getFunctionPlugins(FindPluginManager.MAIN_MENU_EXTENSION);
		Log.i(TAG, "# main menu plugins = " + mMainMenuPlugins.size());
		mMainLoginPlugin = FindPluginManager.getFunctionPlugin(FindPluginManager.MAIN_LOGIN_EXTENSION);	

		// NOTE: This is AcdiVoca stuff and should be put in a plugin
		// AcdiVocaSmsManager.initInstance(this);
		AttributeManager.init();

		// NOTE: Not sure if this is the best way to do this -- perhaps these kinds of prefs
		//  should go in the plugins_preferences.xml
		// A newly installed POSIT should have no shared prefs. Set the default phone pref if
		// it is not already set.
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		try {
			String phone = mSharedPrefs.getString(getString(R.string.smsPhoneKey), "");
			if (phone.equals("")) {
				mSpEditor = mSharedPrefs.edit();
				mSpEditor.putString(getString(R.string.smsPhoneKey), getString(R.string.default_phone));
				mSpEditor.commit();
			}
			String server = mSharedPrefs.getString(getString(R.string.serverPref), "");
			if (server.equals("")) {
				mSpEditor = mSharedPrefs.edit();
				mSpEditor.putString(getString(R.string.serverPref), getString(R.string.defaultServer));
				mSpEditor.commit();
			}
			Log.i(TAG, "Preferences= " + mSharedPrefs.getAll().toString());
		} catch (ClassCastException e) {
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
		}

		
		// Login Extension Point
		// Run login plugin, if necessary
		
		if (mMainLoginPlugin != null) {
			Intent intent = new Intent();
			Class<Activity> loginActivity = mMainLoginPlugin.getActivity();
			intent.setClass(this, loginActivity);
			intent.putExtra(User.USER_TYPE_STRING, User.UserType.USER.ordinal());
			Log.i(TAG, "Starting login activity for result");
			if (mMainLoginPlugin.getActivityReturnsResult()) 
				this.startActivityForResult(intent, mMainLoginPlugin.getActivityResultAction());
			else
				this.startActivity(intent);
		}

//		Intent intent = new Intent();
//		Class<Activity> loginActivity = FindActivityProvider.getLoginActivityClass();
//		if (loginActivity != null) {
//			intent.setClass(this, loginActivity);
//			intent.putExtra(User.USER_TYPE_STRING, User.UserType.USER.ordinal());
//			Log.i(TAG, "started activity fo rresult");
//			this.startActivityForResult(intent, LoginActivity.ACTION_LOGIN);
//		}
	}

	/**
	 * When POSIT starts it should either display a Registration View, if the
	 * phone is not registered with a POSIT server, or it should display the
	 * main View (ListFinds, AddFinds). This helper method is called in various
	 * places in the Android, including in onCreate() and onRestart().
	 */
	private void startPOSIT() {
		setContentView(R.layout.main);

		// Change visibility of buttons based on UserType

		// Log.i(TAG, "POSIT Start, distrStage = " +
		// AppControlManager.displayDistributionStage(this));

		if (FindPluginManager.mFindPlugin.mMainIcon != null) {
			final ImageView mainLogo = (ImageView) findViewById(R.id.Logo);
			int resID = getResources().getIdentifier(FindPluginManager.mFindPlugin.mMainIcon, "drawable", this.getPackageName());
			mainLogo.setImageResource(resID);
		}

		// New Beneficiary button
		if (FindPluginManager.mFindPlugin.mAddButtonLabel != null) {
			final ImageButton addFindButton = (ImageButton) findViewById(R.id.addFindButton);
			int resid = this.getResources()
					.getIdentifier(FindPluginManager.mFindPlugin.mAddButtonLabel, "string", getPackageName());

			if (addFindButton != null) {
				addFindButton.setTag(resid);
				addFindButton.setOnClickListener(this);
			}

			// // Button is gone for AGRI and AGRON users and for USER users
			// during distribution events
			// if ( (AppControlManager.isAgriUser() ||
			// AppControlManager.isAgronUser())
			// || (AppControlManager.isRegularUser() &&
			// AppControlManager.isDuringDistributionEvent())) {
			// addFindButton.setVisibility(View.GONE);
			// } else {
			// addFindButton.setVisibility(View.VISIBLE);
			// }
		}

		// Send messages button
		if (FindPluginManager.mFindPlugin.mListButtonLabel != null) {
			final ImageButton listFindButton = (ImageButton) findViewById(R.id.listFindButton);
			int resid = this.getResources().getIdentifier(FindPluginManager.mFindPlugin.mListButtonLabel, "string",
					getPackageName());
			if (listFindButton != null) {
				listFindButton.setTag(resid);
				listFindButton.setOnClickListener(this);
			}

			// // Button is gone for USER and AGRI users during distribution
			// events
			// if (AppControlManager.isDuringDistributionEvent()
			// && (AppControlManager.isRegularUser()
			// || AppControlManager.isAgriUser()
			// || AppControlManager.isAgronUser() )) {
			// listFindButton.setVisibility(View.GONE);
			// } else {
			// listFindButton.setVisibility(View.VISIBLE);
			// }
		}

		// Update button -- used during Distribution events
		if (FindPluginManager.mFindPlugin.mExtraButtonLabel != null && !FindPluginManager.mFindPlugin.mExtraButtonLabel.equals("")) {
			final ImageButton extraButton = (ImageButton) findViewById(R.id.extraButton);
			int resid = this.getResources().getIdentifier(FindPluginManager.mFindPlugin.mExtraButtonLabel, "string",
					getPackageName());
			if (extraButton != null) {
				extraButton.setOnClickListener(this);
				extraButton.setTag(resid);
				extraButton.setVisibility(View.VISIBLE);
			}

			// // Button is gone for USER and ADMIN users except during
			// distribution events
			// if (AppControlManager.isRegularUser() ||
			// AppControlManager.isAdminUser()) {
			// if (AppControlManager.isDuringDistributionEvent())
			// extraButton.setVisibility(View.VISIBLE);
			// else
			// extraButton.setVisibility(View.GONE);
			//
			// // Enable the Button only if the event is started
			// if (AppControlManager.isDistributionStarted())
			// extraButton.setEnabled(true);
			// else
			// extraButton.setEnabled(false);
			// } else if (AppControlManager.isAgriUser() ||
			// AppControlManager.isAgronUser())
			// extraButton.setVisibility(View.GONE);
		}

		// New agriculture beneficiary
		if (FindPluginManager.mFindPlugin.mExtraButtonLabel2 != null && !FindPluginManager.mFindPlugin.mExtraButtonLabel2.equals("")) {
			final ImageButton extraButton = (ImageButton) findViewById(R.id.extraButton2);
			int resid = this.getResources().getIdentifier(FindPluginManager.mFindPlugin.mExtraButtonLabel2, "string",
					getPackageName());
			if (extraButton != null) {
				extraButton.setTag(resid);
				extraButton.setVisibility(View.VISIBLE);
				extraButton.setOnClickListener(this);
			}

			// // Button is gone for USER users and (AGRI users during
			// distribution events)
			// Log.i(TAG, "Distr Stage = " +
			// AppControlManager.displayDistributionStage(this));
			// if (AppControlManager.isRegularUser()
			// || AppControlManager.isAdminUser()
			// || (AppControlManager.isAgriUser() &&
			// AppControlManager.isDuringDistributionEvent())
			// || (AppControlManager.isAgronUser() &&
			// AppControlManager.isDuringDistributionEvent())) {
			// extraButton.setVisibility(View.GONE);
			// } else {
			// extraButton.setVisibility(View.VISIBLE);
			// }

			Log.i(TAG, "Extra button visibility = " + extraButton.getVisibility());
		}

	}

	// Lifecycle methods just generate Log entries to help debug and understand
	// flow

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG, "Pausing");
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "Resuming");

		LocaleManager.setDefaultLocale(this); // Locale Manager should
														// be in API
		startPOSIT();
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.i(TAG, "Starting");
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.i(TAG, "Restarting");
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.i(TAG, "Stopping");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "Destroying");
	}

	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i(TAG, "onActivityResult resultcode = " + resultCode);
		
		// Login Extension Point result
		if (mMainLoginPlugin != null && requestCode == mMainLoginPlugin.getActivityResultAction()) {
			if (resultCode == Activity.RESULT_OK) {
				Toast.makeText(this, getString(R.string.toast_thankyou), Toast.LENGTH_SHORT).show();
			} else {
				finish();				
			}
		} else 
			super.onActivityResult(requestCode, resultCode, data);
			
		
//		switch (requestCode) {
//		case LoginActivity.ACTION_LOGIN:
//			if (resultCode == RESULT_OK) {
//				Toast.makeText(this, getString(R.string.toast_thankyou), Toast.LENGTH_SHORT).show();
//				break;
//			} else {
//				finish();
//			}
//		default:
//			super.onActivityResult(requestCode, resultCode, data);
//		}
	}

	/**
	 * Handles clicks on PositMain's buttons.
	 */
	public void onClick(View view) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		
		String authKey = Communicator.getAuthKey(this);
		if (authKey == null) {
			Toast.makeText(this, "You must go to Android > Settings > Accounts & Sync to " +
					" set up an account before you use POSIT.", Toast.LENGTH_LONG).show();
			return;
		}
		
		if (sp.getString(getString(R.string.projectNamePref), "").equals("")) {
			Toast.makeText(this, "To get started, you must choose a project.", Toast.LENGTH_LONG).show();
			Intent i = new Intent(this, ListProjectsActivity.class);
			startActivity(i);
		} else {

			Intent intent = new Intent();

			switch (view.getId()) {
			case R.id.addFindButton:
				intent.setClass(this, FindActivityProvider.getFindActivityClass());
				intent.setAction(Intent.ACTION_INSERT);
				// intent.putExtra(AcdiVocaFind.TYPE, AcdiVocaFind.TYPE_MCHN);
				startActivity(intent);
				break;
			case R.id.listFindButton:
				intent = new Intent();
				intent.setAction(Intent.ACTION_SEND);
				// intent.putExtra(AcdiVocaDbHelper.FINDS_STATUS,
				// SearchFilterActivity.RESULT_SELECT_NEW);
				intent.setClass(this, FindActivityProvider.getListFindsActivityClass());
				// intent.setClass(this, AcdiVocaListFindsActivity.class);
				startActivity(intent);
				break;

			case R.id.extraButton:
				intent.setAction(Intent.ACTION_EDIT);
				intent.setClass(this, FindActivityProvider.getExtraActivityClass());
				startActivity(intent);
				break;

			case R.id.extraButton2:
				intent.setAction(Intent.ACTION_INSERT);
				intent.setClass(this, FindActivityProvider.getExtraActivityClass2());
				intent.putExtra(AcdiVocaFind.TYPE, AcdiVocaFind.TYPE_AGRI);
				startActivity(intent);
				break;
			}
		}
	}

	/**
	 * Creates the menu options for the PositMain screen. Menu items are
	 * inflated from a resource file.
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.positmain_menu, menu);	
		return true;
	}

	/**
	 * Shows/Hides menus based on user type, SUPER, ADMIN, USER
	 * 
	 * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		// Re-inflate to force localization.
		menu.clear();
		MenuInflater inflater = getMenuInflater();
//		if (mMainMenuExtensionPointEnabled){
		if (mMainMenuPlugins.size() > 0) {
			for (FunctionPlugin plugin: mMainMenuPlugins) {
				MenuItem item = menu.add(plugin.getmMenuTitle());
				int id = getResources().getIdentifier(
						plugin.getmMenuIcon(), "drawable", "org.hfoss.posit.android.experimental");
				item.setIcon(id);
			}
		}
		inflater.inflate(R.menu.positmain_menu, menu);

		MenuItem adminMenu = menu.findItem(R.id.admin_menu_item);

		// Log.i(TAG, "UserType = " + AppControlManager.getUserType());
		// Log.i(TAG, "distribution stage = " +
		// AppControlManager.getDistributionStage());
		// // Hide the ADMIN menu from regular users
		// if (AppControlManager.isRegularUser() ||
		// AppControlManager.isAgriUser())
		// adminMenu.setVisible(false);
		// else
		// adminMenu.setVisible(true);

		return super.onPrepareOptionsMenu(menu);

	}

	/**
	 * Manages the selection of menu items.
	 * 
	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Log.i(TAG, "onMenuItemSelected " + item.toString());
		switch (item.getItemId()) {
		case R.id.settings_menu_item:
			startActivity(new Intent(this, SettingsActivity.class));
			break;
		case R.id.map_finds_menu_item:
			startActivity(new Intent(this, MapFindsActivity.class));
			break;
		// case R.id.admin_menu_item:
		// startActivity(new Intent(this, AcdiVocaAdminActivity.class));
		// break;
		case R.id.about_menu_item:
			startActivity(new Intent(this, AboutActivity.class));
			break;
			
		default:
			if (mMainMenuPlugins.size() > 0){
				for (FunctionPlugin plugin: mMainMenuPlugins) {
					if (item.getTitle().equals(plugin.getmMenuTitle()))
						startActivity(new Intent(this, plugin.getmMenuActivity()));
				}
			
			}

			break;

		}

		return true;
	}

	/**
	 * Intercepts the back key (KEYCODE_BACK) and displays a confirmation dialog
	 * when the user tries to exit POSIT.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			showDialog(CONFIRM_EXIT);
			return true;
		}
		Log.i("code", keyCode + "");
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Creates a dialog to confirm that the user wants to exit POSIT.
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case CONFIRM_EXIT:
			return new AlertDialog.Builder(this).setIcon(R.drawable.alert_dialog_icon).setTitle(R.string.exit)
					.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							// User clicked OK so do some stuff
							finish();
						}
					}).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							/* User clicked Cancel so do nothing */
						}
					}).create();

		default:
			return null;
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		AlertDialog d = (AlertDialog) dialog;
		Button needsabutton;
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		switch (id) {
		case CONFIRM_EXIT:
			d.setTitle(R.string.exit);
			// d.setButton(DialogInterface.BUTTON_POSITIVE,
			// getString(R.string.alert_dialog_ok), new
			// DialogInterface.OnClickListener() {
			// public void onClick(DialogInterface dialog,
			// int whichButton) {
			// // User clicked OK so do some stuff
			// finish();
			// }
			// } );
			// d.setButton(DialogInterface.BUTTON_NEGATIVE,
			// getString(R.string.alert_dialog_cancel), new
			// DialogInterface.OnClickListener() {
			// public void onClick(DialogInterface dialog,
			// int whichButton) {
			// /* User clicked Cancel so do nothing */
			// }
			// } );
			needsabutton = d.getButton(DialogInterface.BUTTON_POSITIVE);
			needsabutton.setText(R.string.alert_dialog_ok);
			needsabutton.invalidate();

			needsabutton = d.getButton(DialogInterface.BUTTON_NEGATIVE);
			needsabutton.setText(R.string.alert_dialog_cancel);
			needsabutton.invalidate();

			break;
		}
	}

	/**
	 * Makes sure RWG is stopped before exiting the Activity
	 * 
	 * @see android.app.Activity#finish()
	 */
	@Override
	public void finish() {
		Log.i(TAG, "finish()");
		super.finish();
	}
}