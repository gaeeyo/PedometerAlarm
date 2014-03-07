package jp.syoboi.android.pedometeralarm.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

import jp.syoboi.android.pedometeralarm.R;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

@EViewGroup
public class LogRowView extends RelativeLayout {

	@ViewById(R.id.time) 	TextView	mTime;
	@ViewById(R.id.tag)		TextView	mTag;
	@ViewById(R.id.msg)		TextView	mMsg;
	
	public LogRowView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void bind(int position, String log) {
		CharSequence time = null;
		CharSequence msg = null;
		CharSequence tag = null;
		int pos = log.indexOf('\t');
		if (pos == -1) {
			msg = log;
		} else {
			int pos2 = log.indexOf('\t', pos + 1);
			if (pos2 == -1) {
				msg = log;
			} else {	
				time = log.subSequence(0, pos);
				tag = log.subSequence(pos + 1, pos2);
				msg = log.subSequence(pos2 + 1, log.length());
			}
		}
		mTime.setText(position + ":" + (time != null ? time : ""));
		mTag.setText(tag);
		mMsg.setText(msg);
	}
	
}
