/*
 * z4control (c) Elia Yehuda, aka z4ziggy, 2010-2011
 * controls various system settings using init.d/scripts and more
 * Released under the GPLv2
 *
 * File browser Activity
 * 
 */
package com.z4control;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class FolderList extends ListActivity {
	private ArrayList<String> mFileList;
	private File mPath;
	
	public class FolderListAdapter extends ArrayAdapter<String> {
		public FolderListAdapter(Context context, int textViewResourceId, List<String> objects) {
			super(context, textViewResourceId, objects);
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = LayoutInflater.from(getContext()).inflate(R.layout.row, parent, false);
			TextView label = (TextView)row.findViewById(R.id.text);
			label.setText(mFileList.get(position));
			ImageView icon = (ImageView)row.findViewById(R.id.icon);
			if (new File( mPath.getPath() + "/" + mFileList.get(position)).isDirectory()) {
				icon.setImageResource(R.drawable.folder);
			}
			else{
				icon.setImageResource(R.drawable.bash);
			}
			return row;
		}
	}
	public void UpdateList(String path) {
		mPath = new File(path);
		mFileList.clear();
		if (mPath.list() != null)
			mFileList.addAll(Arrays.asList(mPath.list()));
		//if (!"/".equals(path))
			mFileList.add(0,".."); 
		setListAdapter(new FolderListAdapter(this, R.layout.row, mFileList));
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
//        List<Map<String, Object>> resourceNames = new ArrayList<Map<String, Object>>();
//        Map<String, Object> data;
//        data = new HashMap<String, Object>();
//        data.put("line", stg );
//        data.put("img", R.drawable.folder );
//        resourceNames.add(data);
//        SimpleAdapter notes = new SimpleAdapter(this,resourceNames,R.layout.row,
//            new String[] { "line", "img" }, new int[] { R.id.text, R.id.icon } );
//        setListAdapter(notes);
		mFileList = new ArrayList<String>();
        UpdateList("/sdcard");
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		try {
			File newPath;
			if ("..".equals(mFileList.get(position)) ) {
				newPath = new File( mPath.getPath().substring(0, mPath.getPath().lastIndexOf("/") + 1 ));
				UpdateList(newPath.getPath());
			} else {
				newPath = new File( mPath.getPath() + "/" + mFileList.get(position) );
				if (newPath.isDirectory()) { 
					UpdateList(newPath.getPath());
				} else {
					// file chosen
					Intent intent=new Intent();  
					intent.putExtra("SelectedFile", newPath.getCanonicalPath() );
					this.setResult(RESULT_OK, intent);
					this.finish();
				}
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
}
