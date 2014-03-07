package jp.syoboi.android.pedometeralarm.utils;

import android.text.format.Time;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class MyLog {
	
	static File	LOGFILE;
	static Time	TIME = new Time();
	static ArrayList<OnMyLogListener> LISTENERS = new ArrayList<OnMyLogListener>();
	
	public static void setFile(File file) {
		LOGFILE = file;
	}
	
	public static File getFile() {
		return LOGFILE;
	}
	
	public static void registerListener(OnMyLogListener listener) {
		synchronized (LISTENERS) {
			if (!LISTENERS.contains(listener)) {
				LISTENERS.add(listener);
			}
		}
	}
	
	public static void unregisterListener(OnMyLogListener listener) {
		synchronized (LISTENERS) {
			LISTENERS.remove(listener);
		}
	}
	
	public static void v(String tag, String msg) {
		Log.v(tag, msg);
		
		TIME.setToNow();
		String time = TIME.format("%Y-%m-%d %H:%M:%S");
		
		String log = time + "\t" + tag + "\t" + msg;
		appendLog(LOGFILE, log);
		
		synchronized (LISTENERS) {
			if (LISTENERS.size() > 0) {
				for (OnMyLogListener listener: LISTENERS) {
					listener.onLog(log);
				}
			}

		}
	}
	
	public static void appendLog(File file, String log) {
		if (LOGFILE == null) {
			return;
		}
		synchronized(LOGFILE) {
			FileWriter out = null;
			try {
				out = new FileWriter(file, true);
				out.write(log);
				out.write("\n");
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (out != null) {
						out.close();
					}
				} catch (IOException e) {
					;
				}
			}
		}
	}
	
	public static interface OnMyLogListener {
		public void onLog(String log);
	}
}
