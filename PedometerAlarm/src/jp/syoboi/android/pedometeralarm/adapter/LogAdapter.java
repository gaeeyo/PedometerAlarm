package jp.syoboi.android.pedometeralarm.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

import jp.syoboi.android.pedometeralarm.R;
import jp.syoboi.android.pedometeralarm.view.LogRowView;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.SystemService;

@EBean
public class LogAdapter extends BaseAdapter {

	@SystemService	LayoutInflater	mInfalater;
	
	List<String>	mItems = new ArrayList<String>();
	
	public LogAdapter(Context context) {
	}
	
	public void setItems(List<String> items) {
		mItems = items;
		notifyDataSetChanged();
	}
	
	public void addLog(String log) {
		mItems.add(log);
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		return mItems.size();
	}

	@Override
	public String getItem(int position) {
		return mItems.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LogRowView v;
		if (convertView != null) {
			v = (LogRowView) convertView;
		} else {
			v = (LogRowView) mInfalater.inflate(R.layout.log_row, parent, false);
		}
		v.bind(position, getItem(position));
		
		return v;
	}
}
