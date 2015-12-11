package me.liujia95.phoneguard.activity;

import me.liujia95.phoneguard.R;
import android.app.Activity;
import android.os.Bundle;

public class HomeActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initView();
	}

	private void initView() {
		setContentView(R.layout.homt_activity);
	}
	
}
