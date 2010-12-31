/*
 * z4control (c) Elia Yehuda, aka z4ziggy, 2010-2011
 * controls various system settings using init.d/scripts and more
 * Released under the GPLv2
 * 
 * TODO:
 * - check for busybox, if not found, inform the user and allow download?
 * 
 */
package com.z4control;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

public class z4control extends PreferenceActivity {
	private static final String PATH_SDCARD        = Environment.getExternalStorageDirectory().getPath();
	private static final String PATH_Z4CONTROL     = PATH_SDCARD + "/z4control/";
	private static final String PATH_THEMES        = PATH_Z4CONTROL + "themes/";
	private static final String PATH_BOOTANIMS     = PATH_Z4CONTROL + "bootanim/";
	private static final String PATH_ANDANIMS      = PATH_Z4CONTROL + "andanim/";
	private static final String FILE_ABOUT         = PATH_Z4CONTROL + "about";
	private static final String PATH_INIT_D        = "/system/etc/init.d/";
	private static final String FILE_INIT_TWEAKS   = PATH_INIT_D + "S01-tweaks.sh";
	private static final String FILE_INIT_ADBROOT  = PATH_INIT_D + "P01-adbroot.sh";
	private static final String FILE_INIT_Z4MOD    = PATH_INIT_D + "P00-z4mod.sh";
	private static final String FILE_INIT_BOOTANIM = PATH_INIT_D + "P02-bootanim.sh";
	private static final String FILE_Z4MOD_GZ      = "/system/z4mod.tar.gz";
	private static final String FILE_Z4MOD_CONFIG  = "/system/z4mod.config";
	private static final String FILE_Z4MOD_LOG     = PATH_SDCARD + "/z4mod.log";
	private static final int FILEBROWSER_ID_SCRIPT = 1;
	private static final int FILEBROWSER_ID_KERNEL = 2;

	private Map<String, ?> options_org;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.z4preference);
		//
		// check if this is 1st run!
		//
		options_org = getPreferenceManager().getSharedPreferences().getAll();
		if (options_org.size() == 0) {
			if (!(new File(PATH_INIT_D).isDirectory()) ) {
				Helpers.RunSystemCmd("mkdir " + PATH_INIT_D);
			}
		}

		if (new File(PATH_Z4CONTROL).isDirectory() ) {
			//
			// set the title if file /z4control/about is found
			//
			try {
				BufferedReader bProcFS = new BufferedReader(new FileReader(FILE_ABOUT));
				String z4About = bProcFS.readLine();
				bProcFS.close();
				setTitle(getTitle() + " / " + z4About);
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}
			//
			// populate themes lists from their corresponding path
			//
			Helpers.PopulateList(findPreference("ListThemes"),new File(PATH_THEMES).list()); 
			Helpers.PopulateList(findPreference("ListBootAnims"),new File(PATH_BOOTANIMS).list()); 
			Helpers.PopulateList(findPreference("ListAndAnims"),new File(PATH_ANDANIMS).list());
		} else {
		    PreferenceCategory category = (PreferenceCategory) findPreference("LookAndFeel");
		    category.removePreference(findPreference("ListThemes"));
		    category.removePreference(findPreference("ListBootAnims"));
		    category.removePreference(findPreference("ListAndAnims"));
		}

		//
		// fill options
		//
		((CheckBoxPreference)findPreference("EnableTweaks")).setChecked(new File(FILE_INIT_TWEAKS).isFile());
		((CheckBoxPreference)findPreference("EnableADBroot")).setChecked(new File(FILE_INIT_ADBROOT).isFile());
		((CheckBoxPreference)findPreference("EnableBootAnim")).setChecked(new File(FILE_INIT_BOOTANIM).isFile());
		//
		// set current density
		//
		((ListPreference)findPreference("ListDensity")).setValue(Helpers.GetIniValue("/system/build.prop", "ro.sf.lcd_density"));
		//
		// set lagfix settings
		//
		if (new File("/z4mod").isDirectory()) {
			//
			// populate lagfix lists with supported filesystems and select current filesystem type
			//
			String[] lsFileSystems = Helpers.GetSupportedFileSystems();
			Helpers.PopulateFSList(findPreference("LagFixData"),lsFileSystems, "data");
			Helpers.PopulateFSList(findPreference("LagFixDBData"),lsFileSystems,"dbdata");
			Helpers.PopulateFSList(findPreference("LagFixCache"),lsFileSystems,"cache");
			Helpers.PopulateFSList(findPreference("LagFixSystem"),lsFileSystems,"system");
			Helpers.PopulateFSList(findPreference("LagFixAll"),lsFileSystems,null);
		} else {
			//
			// disable specific z4mod options on non-z4mod systems
			//
			findPreference("LagFix").setEnabled(false);
		}
		//
		// read preferences on startup so we know what have changed on exit
		//
		options_org = getPreferenceManager().getSharedPreferences().getAll();
	}

	//
	// monitor clicks and execute appropriate command
	//
	@Override
	public boolean onPreferenceTreeClick (PreferenceScreen preferenceScreen, Preference preference) {
		//
		// check which button was clicked
		//
		if ("Installkernel".equals(preference.getKey())) {
            startActivityForResult(new Intent(this, FolderList.class),FILEBROWSER_ID_KERNEL);
		} else if ("RunScript".equals(preference.getKey())) {
            startActivityForResult(new Intent(this, FolderList.class),FILEBROWSER_ID_SCRIPT);
		} else if ("DisplayLog".equals(preference.getKey())) {
			findPreference("LogTextView").setSummary(Helpers.ReadFileToString(FILE_Z4MOD_LOG));
		} else if ("DeleteLog".equals(preference.getKey())) {
			Helpers.RunRootCmd("busybox rm " + FILE_Z4MOD_LOG);
			Toast.makeText(getBaseContext(), "Log removed", Toast.LENGTH_SHORT).show();
		} else if ("About".equals(preference.getKey())) {
			new Builder(this)
			.setTitle("About z4control")
			.setMessage("Set various z4mod system settings.\n\n(C) Elia Yehuda aka z4ziggy\n2010-2011\n\nFor donations, buy a homeless a meal.")
			.show();		
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case FILEBROWSER_ID_SCRIPT :
				Helpers.RunRootCmd("/system/bin/sh " + data.getStringExtra("SelectedFile"));
				Toast.makeText(this, "Script finished", Toast.LENGTH_SHORT).show();
				break;
			case FILEBROWSER_ID_KERNEL :
				final String FILE_REDBEND = "/data/local/tmp/redbendua";
				Helpers.CopyResFile(getResources().openRawResource(R.raw.redbendua), FILE_REDBEND);
				Helpers.RunRootCmd(FILE_REDBEND + " restore " + data.getStringExtra("SelectedFile") + " /dev/block/bml7");
				Helpers.RunRootCmd("sync; sync; busybox sleep 1; /system/bin/toolbox reboot");
				break;
			}
		}
	}

	//
	// monitor key press for Back-Key, 
	// apply changes (if any) and reboot if needed
	//
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK) {
			if (IsOptionsChanged()) {
				new Builder(this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle("Apply changes and reboot.")
				.setMessage("Are you sure?")
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						ApplyChanges();
						z4control.this.finish();    
					}
				})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						z4control.this.finish();    
					}
				})
				.show();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	//
	// returns true if something was changed, false otherwise
	//
	public boolean IsOptionsChanged() {
		Map<String, ?> options_new = getPreferenceManager().getSharedPreferences().getAll(); 
		return !options_new.values().toString().equals(options_org.values().toString());
	}

	//
	// apply changes (if any)
	//
	public boolean ApplyChanges() {
		ProgressDialog.show(this, "", "Applying changes...", true);
		//
		// read the preferences to compare original ones
		//
		Map<String, ?> options_new = getPreferenceManager().getSharedPreferences().getAll(); 
		StringBuffer z4modlines = new StringBuffer();
		Boolean NeedReboot = false;

		//
		// apply changes
		//
		for(String key: options_new.keySet()) {
			if (!options_new.get(key).equals(options_org.get(key))) {
				if ("EnableTweaks".equals(key)) {
					if ((Boolean)options_new.get(key) == true ) {
						Helpers.CopyResFile(getResources().openRawResource(R.raw.init_d_tweaks),FILE_INIT_TWEAKS);
					} else {
						Helpers.RunSystemCmd("busybox rm -rf " + FILE_INIT_TWEAKS);
					}
				}
				else if ("EnableADBroot".equals(key)) {
					if ((Boolean)options_new.get(key) == true ) {
						Helpers.CopyResFile(getResources().openRawResource(R.raw.init_d_adbroot),FILE_INIT_ADBROOT);
					} else {
						Helpers.RunSystemCmd("busybox rm -rf " + FILE_INIT_ADBROOT);
					}
				}
				else if ("EnableBootAnim".equals(key)) {
					if ((Boolean)options_new.get(key) == true ) {
						Helpers.CopyResFile(getResources().openRawResource(R.raw.init_d_bootanim),FILE_INIT_BOOTANIM);
					} else {
						Helpers.RunSystemCmd("busybox rm -rf " + FILE_INIT_BOOTANIM);
					}
				}
				else if ("ListDensity".equals(key)) {
					Helpers.RunSystemCmd(
							"busybox sed -i 's/ro.sf.lcd_density=.*/ro.sf.lcd_density=" + 
							options_new.get(key) + "/g' /system/build.prop");
					NeedReboot = true;
				}
				else if ("ListThemes".equals(key)) {
					Helpers.RunSystemCmd( "busybox cp -r " + PATH_THEMES + options_new.get(key) + "/* /system/");
					Helpers.RunRootCmd("busybox rm -rf /data/dalvik-cache/*; sync; sync");
					NeedReboot = true;
				}
				else if ("ListBootAnims".equals(key)) {
					Helpers.RunSystemCmd( "busybox cp -r " + PATH_BOOTANIMS + options_new.get(key) + "/* /system/media/");
					Helpers.RunRootCmd("busybox rm -rf /data/dalvik-cache/*; sync; sync");
					NeedReboot = true;
				}
				else if ("ListAndAnims".equals(key)) {
					Helpers.UpdateAnimationFromPath(PATH_ANDANIMS + options_new.get(key));
					Helpers.RunRootCmd("busybox rm -rf /data/dalvik-cache/*; sync; sync");
					NeedReboot = true;
				}
				else if ("LagFixAll".equals(key)) {
					String fs = (String) options_new.get(key);
					if (fs != null) {
						if (!options_new.get(key).equals(Helpers.GetCurrentFileSystem("data"))) {
							z4modlines.append(Helpers.Getz4modParams("data", options_new.get(key)));
						}
						if (!options_new.get(key).equals(Helpers.GetCurrentFileSystem("dbdata"))) {
							z4modlines.append(Helpers.Getz4modParams("dbdata", options_new.get(key)));
						}
						if (!options_new.get(key).equals(Helpers.GetCurrentFileSystem("cache"))) {
							z4modlines.append(Helpers.Getz4modParams("cache", options_new.get(key)));
						}
						if (!options_new.get(key).equals(Helpers.GetCurrentFileSystem("system"))) {
							z4modlines.append(Helpers.Getz4modParams("system", options_new.get(key)));
						}
					}
				}
				else if ("LagFixData".equals(key)) {
					if (!options_new.get(key).equals(Helpers.GetCurrentFileSystem("data"))) {
						z4modlines.append(Helpers.Getz4modParams("data", options_new.get(key)));
					}
				}
				else if ("LagFixDBData".equals(key)) {
					if (!options_new.get(key).equals(Helpers.GetCurrentFileSystem("dbdata"))) {
						z4modlines.append(Helpers.Getz4modParams("dbdata", options_new.get(key)));
					}
				}
				else if ("LagFixCache".equals(key)) {
					if (!options_new.get(key).equals(Helpers.GetCurrentFileSystem("cache"))) {
						z4modlines.append(Helpers.Getz4modParams("cache", options_new.get(key)));
					}
				}
				else if ("LagFixSystem".equals(key)) {
					if (!options_new.get(key).equals(Helpers.GetCurrentFileSystem("system"))) {
						z4modlines.append(Helpers.Getz4modParams("system", options_new.get(key)));
					}
				}
			}
		}

		if (z4modlines.length() > 0) {
			File fCfgFile = Helpers.WriteStringToFile(z4modlines.toString());
			Helpers.RunSystemCmd("busybox cp " + fCfgFile.getPath() + " " + FILE_Z4MOD_CONFIG);
			fCfgFile.delete();
			Helpers.CopyResFile(getResources().openRawResource(R.raw.init_d_z4mod), FILE_INIT_Z4MOD);
			Helpers.CopyResFile(getResources().openRawResource(R.raw.z4mod_gz), FILE_Z4MOD_GZ);
			NeedReboot = true;
		}

		Helpers.RunRootCmd("sync; sync; busybox sleep 1; /system/bin/toolbox reboot");
		return NeedReboot;
	}
}
