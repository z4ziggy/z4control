package com.z4control;

import android.content.Context;
import android.graphics.Typeface;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TextViewPreference extends Preference {

	private TextView z4LogView;

	public TextViewPreference(Context context) {
		super(context);
	}

	public TextViewPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TextViewPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected View onCreateView(ViewGroup parent) {
		LinearLayout layout = new LinearLayout(getContext());
		z4LogView = new TextView(getContext());
		z4LogView.setText(getSummary());
		z4LogView.setTextSize(12);
		z4LogView.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
		layout.addView(z4LogView);
		layout.setId(android.R.id.widget_frame);
		return layout;
	}
}
