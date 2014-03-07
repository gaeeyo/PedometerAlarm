package jp.syoboi.android.pedometeralarm.fragment;

import android.app.Fragment;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.syoboi.android.pedometeralarm.R;
import jp.syoboi.android.pedometeralarm.adapter.LogAdapter;
import jp.syoboi.android.pedometeralarm.utils.MyLog;
import jp.syoboi.android.pedometeralarm.utils.MyLog.OnMyLogListener;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

@EFragment(R.layout.log_fragment)
public class LogFragment extends Fragment {
	
	@ViewById(android.R.id.list)	ListView	mListView;
	
	@Bean
	LogAdapter	mAdapter;
	
	
	@AfterViews
	void afterViews() {
		mListView.setAdapter(mAdapter);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		MyLog.registerListener(mLogListener);
		loadLog();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		MyLog.unregisterListener(mLogListener);
	}
	
	
	OnMyLogListener mLogListener = new OnMyLogListener() {
		@Override
		public void onLog(String log) {
			addLog(log);
		}
	};
	
	@UiThread
	void addLog(String log) {
		mAdapter.addLog(log);
		scrollToBottom();
	}
	
	@Background
	void loadLog() {
		ArrayList<String> lines = new ArrayList<String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(MyLog.getFile()));
			String line;
			while ((line = br.readLine()) != null) {
				lines.add(line);
			}
			onLoadLog(lines);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	@UiThread
	void onLoadLog(List<String> lines) {
		mAdapter.setItems(lines);
		scrollToBottom();
	}
	
	void scrollToBottom() {
		if (mListView.getCount() > 0) {
			mListView.setSelection(mListView.getCount() - 1);
		}

	}
	
}
