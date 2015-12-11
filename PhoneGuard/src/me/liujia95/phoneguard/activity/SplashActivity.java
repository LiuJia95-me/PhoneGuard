package me.liujia95.phoneguard.activity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import me.liujia95.phoneguard.R;
import me.liujia95.phoneguard.utils.IOUtils;
import me.liujia95.phoneguard.utils.PackageInfoUtils;
import me.liujia95.phoneguard.utils.UIUtils;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;
import android.widget.Toast;

public class SplashActivity extends Activity {

	private TextView			mTv_version;
	private String				mDesc			= null;

	private static final int	STATE_SUCCESS	= 0;
	private static final int	STATE_ERROR		= 1;
	private static final int	STATE_NONE		= 2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initView();
		initData();
	}

	Handler	mHandler	= new Handler() {
							public void handleMessage(android.os.Message msg) {
								switch (msg.what) {
								case STATE_SUCCESS:
									// 成功状态
									showSuccessDialog((String) msg.obj);
									break;
								case STATE_ERROR:
									// 异常状态 1.加载主界面 2.弹出异常Toast
									Toast.makeText(UIUtils.getContext(), (String) msg.obj, Toast.LENGTH_SHORT).show();
									loadMain();
									break;
								case STATE_NONE:
									loadMain();
									break;
								default:
									break;
								}
							};
						};

	/**
	 * @des 加载控件
	 */
	private void initView() {
		setContentView(R.layout.splash_activity);
		mTv_version = (TextView) findViewById(R.id.splash_tv_version);
	}

	/**
	 * @des 弹出成功的dialog，问是否要更新
	 * @param obj
	 */
	protected void showSuccessDialog(final String json) {
		AlertDialog.Builder builder = new AlertDialog.Builder(UIUtils.getContext());
		builder.setTitle("版本更新提醒");
		builder.setMessage("版本信息：" + mDesc + "\n是否要更新？");
		builder.setNegativeButton("取消", null); // 取消啥也不做
		builder.setPositiveButton("确定", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// 执行更新操作
				showProgressDialog(json);
			}
		});
		builder.create();
		builder.show();
	}

	/**
	 * @des 弹出显示进度的Dialog
	 * @param json
	 */
	protected void showProgressDialog(String downloadUrl) {
		ProgressDialog dialog = new ProgressDialog(UIUtils.getContext());
		dialog.setCancelable(false);
		dialog.setTitle("更新进度");
		dialog.show();
		UIUtils.post(new DownloadTask(dialog, downloadUrl));
	}

	/**
	 * @des 执行下载任务
	 * @author liujia
	 * 
	 */
	class DownloadTask implements Runnable {
		private ProgressDialog	mDialog;
		private String			mDownloadUrl;

		public DownloadTask(ProgressDialog dialog, String downloadUrl) {
			this.mDialog = dialog;
			this.mDownloadUrl = downloadUrl;
		}

		@Override
		public void run() {
			InputStream is = null;
			FileOutputStream fos = null;
			Message msg = new Message();
			try {
				URL url = new URL(mDownloadUrl);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setReadTimeout(5000);
				conn.setConnectTimeout(5000);
				int code = conn.getResponseCode();
				if (code == 200) {
					// 开始下载
					is = conn.getInputStream();
					File file = new File(Environment.getExternalStorageDirectory(), System.currentTimeMillis() + ".apk");
					fos = new FileOutputStream(file);
					byte[] buf = new byte[1024];
					int len = 0;
					mDialog.setMax(conn.getContentLength());
					int count = 0;
					while ((len = is.read(buf)) != -1) {
						fos.write(buf, 0, len);
						count += len;
						mDialog.setProgress(count);
					}
					mDialog.dismiss();
				} else {
					// 下载失败
					msg.what = STATE_ERROR;
					msg.obj = "error:1004";
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
				msg.what = STATE_ERROR;
				msg.obj = "error:1005";
			} catch (IOException e) {
				e.printStackTrace();
				msg.what = STATE_ERROR;
				msg.obj = "error:1006";
			} finally {
				IOUtils.close(is);
				IOUtils.close(fos);
				mHandler.sendMessage(msg);
			}
		}
	}

	/**
	 * @des 加载主页面
	 */
	public void loadMain() {
		Intent intent = new Intent(this, HomeActivity.class);
		startActivity(intent);
	}

	/**
	 * @des 加载数据
	 * @des 1、访问网络看有没有新的版本
	 * @des 2、一些应用中要用到的数据的初始化
	 */
	private void initData() {
		mTv_version.setText("版本号：" + PackageInfoUtils.getVersionName());
		UIUtils.post(new CheckVersionTask());
	}

	/**
	 * @des 执行检查版本的任务
	 * @author liujia
	 * 
	 */
	class CheckVersionTask implements Runnable {
		@Override
		public void run() {
			// 创建消息
			Message msg = new Message();
			BufferedReader br = null;
			try {
				// 获取网络中版本信息的json
				URL url = new URL(UIUtils.getString(R.string.version_url));
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setReadTimeout(5000);
				conn.setConnectTimeout(5000);
				int code = conn.getResponseCode();

				if (code == 200) {
					// 请求成功
					br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
					byte[] buf = new byte[1024];
					String json = null;

					if ((json = br.readLine()) != null) {
						JSONObject jsonObj = new JSONObject(json);
						mDesc = jsonObj.getString("desc");
						// 获取网络检测到的版本号
						int netVersionCode = Integer.valueOf(jsonObj.getString("versionCode"));
						int localVersionCode = PackageInfoUtils.getVersionCode();
						if (netVersionCode > localVersionCode) {
							// 有新版本
							msg.what = STATE_SUCCESS;
							msg.obj = jsonObj.getString("downloadUrl");
						} else {
							// 没有新版本
							msg.what = STATE_NONE;
						}
					}
				} else {
					// 请求失败
					msg.what = STATE_ERROR;
					msg.obj = "error:1000";
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
				msg.what = STATE_ERROR;
				msg.obj = "error:1001";
			} catch (IOException e) {
				e.printStackTrace();
				msg.what = STATE_ERROR;
				msg.obj = "error:1002";
			} catch (JSONException e) {
				e.printStackTrace();
				msg.what = STATE_ERROR;
				msg.obj = "error:1003";
			} finally {
				IOUtils.close(br);
				mHandler.sendMessage(msg);
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

}
