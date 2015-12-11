package me.liujia95.phoneguard.utils;

import me.liujia95.phoneguard.BaseApplication;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;

public class UIUtils {

	public static Context getContext() {
		return BaseApplication.getContext();
	}

	public static Resources getResources() {
		return getContext().getResources();
	}

	public static String getString(int resId) {
		return getResources().getString(resId);
	}

	public static Thread getMainThread() {
		return BaseApplication.getMainThread();
	}

	public static Handler getMainHandler() {
		return BaseApplication.getMainHandler();
	}

	public static void post(Runnable task) {
		getMainHandler().post(task);
	}

	public static void postDelayed(Runnable task, long delayMillis) {
		getMainHandler().postDelayed(task, delayMillis);
	}

	public static String getPackageName() {
		return getContext().getPackageName();
	}
}
