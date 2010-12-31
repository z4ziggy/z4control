/*
 * z4control (c) Elia Yehuda, aka z4ziggy, 2010-2011
 * controls various system settings using init.d/scripts and more
 * Released under the GPLv2
 *
 * Helper functions
 * 
 */
package com.z4control;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import android.preference.ListPreference;
import android.preference.Preference;

public class Helpers {
	//
	// fill lists with files from array of strings[]
	//
	public static void PopulateList(Preference preference, String[] List) {
		ListPreference PrefList = (ListPreference)preference;
		if (List == null) {
			PrefList.setEnabled(false);
		} else {
			PrefList.setEntryValues(List); 
			PrefList.setEntries(List);
			PrefList.setEnabled(true);
		}
	}

	//
	// fill filesystem lists with files from array of strings[]
	//
	public static void PopulateFSList(Preference preference, String[] List, String filesystem) {
		PopulateList(preference, List);
		ListPreference PrefList = (ListPreference)preference;
		if (filesystem != null) {
			String currentfs = GetCurrentFileSystem(filesystem);
			PrefList.setSummary("current filesystem: " +  currentfs);
			PrefList.setValue(currentfs);
		} else {
			PrefList.setValue("");
		}
	}

	//
	// run command as 'root'
	//
	public static void RunRootCmd(String cmd) {
		try
		{
			//
			// run our command using 'su' to gain root
			//
			Process process = Runtime.getRuntime().exec("su");
			DataOutputStream outputStream = new DataOutputStream(process.getOutputStream()); 

			outputStream.writeBytes(cmd + "\n");
			outputStream.flush();

			outputStream.writeBytes("exit\n"); 
			outputStream.flush(); 
			process.waitFor();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	//
	// wrap cmd with remount /system and execute as root
	//
	public static void RunSystemCmd(String cmd) {
		String command = new String(
				"busybox mount -o remount,rw,llw,check=no /system || busybox mount -o remount,rw /system; " +
				cmd + ";" +
				"busybox mount -o remount,ro,llw,check=no /system"
		);
		RunRootCmd(command);
	}

	//
	// write stream to a tempfile
	//
	public static File WriteStreamToFile(InputStream resStream) {
		try {
			byte[] bytes = new byte[resStream.available()];
			File tmpFile = File.createTempFile("z4-", ".tmp");
			tmpFile.deleteOnExit();
			DataInputStream dis = new DataInputStream(resStream);
			dis.readFully(bytes);
			FileOutputStream foutStream = new FileOutputStream(tmpFile.getPath());
			foutStream.write(bytes);
			foutStream.close();
			dis.close();
			return tmpFile;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	//
	// extract resource file and copy to destination
	//
	public static void CopyResFile(InputStream resStream, String sTarget) {
		File tmpFile = WriteStreamToFile(resStream);
		if (tmpFile != null) {
			RunSystemCmd(String.format("busybox cp %s %s; chmod 777 %s", tmpFile.getPath(), sTarget, sTarget));
			tmpFile.delete();
		}
	}

	//
	// update framework-res.apk file from external path
	//
	public static void UpdateAnimationFromPath(String sFromPath) {
		String sTmpZipFile = "/data/local/tmp/z4res.zip";
		String sOrgZipFile = "/system/framework/framework-res.apk";
		try {
			RunRootCmd("busybox cp " + sOrgZipFile + " " + sTmpZipFile );
			File fNewZipFile = UpdateZipFromPath(sTmpZipFile, sFromPath);
			RunSystemCmd("busybox cp " + fNewZipFile.getPath() + " " + sOrgZipFile + "; busybox rm " + sTmpZipFile);
			fNewZipFile.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//
	// return a list of supported filesystems
	//
	public static String[] GetSupportedFileSystems() {
		try {
			FileInputStream fProcFS = new FileInputStream(new File("/proc/filesystems"));
			ArrayList<String> filesystems = new ArrayList<String>();
			byte[] data1 = new byte[1024];
			int len = fProcFS.read(data1);
			fProcFS.close();
			String fs = new String(data1, 0, len);
			if (fs.contains("rfs")) filesystems.add("rfs");
			if (fs.contains("jfs")) filesystems.add("jfs");
			if (fs.contains("ext2")) filesystems.add("ext2");
			if (fs.contains("ext3")) filesystems.add("ext3");
			if (fs.contains("ext4")) filesystems.add("ext4");
			String[] List = (String[])filesystems.toArray(new String[filesystems.size()]);
			return List;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	//
	// return the current filesystem for a given partition
	//
	public static String GetCurrentFileSystem(String PartitionName) {
		String[] args = GetMountLine(PartitionName);
		if (args != null) {
    		return args[2];
		}
		return null;
	}

	//
	// return a formatted line for converting a given partition
	//
	public static String Getz4modParams(String PartitionName, Object filesystem) {
		String[] args = GetMountLine(PartitionName);
		if (args != null) {
    		return String.format("%s %s %s\n", PartitionName, args[0].split("/")[3], filesystem);
		}
		return null;
	}

	//
	// returns a variable value from an ini-formatted file
	//
	public static String GetIniValue(String filename, String variable) {
		try {
			BufferedReader bProcFS = new BufferedReader(new FileReader(filename));
	        String readLine = null;
            while ((readLine = bProcFS.readLine()) != null) {
            	if (readLine.startsWith(variable)) {
            		bProcFS.close();
	            	return readLine.split("=")[1];
            	}
            }
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	//
	// returns given partition mount-line from /proc/mounts
	//
	public static String[] GetMountLine(String PartitionName) {
		try {
			BufferedReader bProcFS = new BufferedReader(new FileReader("/proc/mounts"));
	        String readLine = null;
            while ((readLine = bProcFS.readLine()) != null) {
            	String[] args = readLine.split(" ");
            	if ((args.length > 3) && (args[1].equalsIgnoreCase("/" + PartitionName) )) {
            		return args;
            	}
            }
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	//
	// copy input stream to output using buffer
	//
	public static void copy(InputStream input, OutputStream output) throws IOException {
		// 32kb buffer
		final byte[] BUFFER = new byte[32 * 1024];
		int bytesRead;
		while ((bytesRead = input.read(BUFFER))!= -1) {
			output.write(BUFFER, 0, bytesRead);
		}
	}

	//
	// update a zip file from a given path, using a tempfile, and returns tempfile
	//
	public static File UpdateZipFromPath(String sZipFile, String sPath) throws Exception {
		File tmpFile = File.createTempFile("z4zip-tmp-", ".zip");
		tmpFile.deleteOnExit();
		ZipFile inZip = new ZipFile(sZipFile);
		ZipOutputStream outZip = new ZipOutputStream(new FileOutputStream(tmpFile.getPath()));

		Enumeration<? extends ZipEntry> entries = inZip.entries();
		while (entries.hasMoreElements()) {
			ZipEntry e = entries.nextElement();
			outZip.putNextEntry(e);
			if (!e.isDirectory()) {
				File f = new File(sPath + "/" + e.getName());
				if (f.exists()) {
					copy(new FileInputStream(f.getPath()), outZip);
				} else {
					copy(inZip.getInputStream(e), outZip);
				}
			}
			outZip.closeEntry();
		}
		inZip.close();
		outZip.close();
		return tmpFile;
	}

	//
	// simple wrapper to dump string to file, returns tmpfile
	//
	public static File WriteStringToFile(String str) {
		File tmpFile = null;
		try {
			tmpFile = File.createTempFile("z4-", ".tmp");
			BufferedWriter out = new BufferedWriter( new FileWriter(tmpFile));
			out.write(str);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return tmpFile;
	}
	
	//
	// simple wrapper to read a file
	//
	public static StringBuilder ReadFileToString(String filename) {
        StringBuilder text = new StringBuilder();
		try {
			BufferedReader bProcFS = new BufferedReader(new FileReader(filename));
	        String readLine = null;
            while ((readLine = bProcFS.readLine()) != null) {
            	text.append(readLine);
            	text.append("\n");
            }
	        bProcFS.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        return text;
	}
}
