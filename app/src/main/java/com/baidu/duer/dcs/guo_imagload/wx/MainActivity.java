package com.baidu.duer.dcs.guo_imagload.wx;

import android.app.Activity;
import android.os.Bundle;
import android.widget.GridView;

import com.baidu.duer.dcs.guo_imagload.R;
import com.baidu.duer.dcs.guo_imagload.wx.ImageAdapter;

import java.util.ArrayList;

public class MainActivity extends Activity {

	private GridView mImageGridView;
	private ImageAdapter mImageAdapter;
	private ArrayList<String> mUrlList;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mImageGridView = (GridView) findViewById(R.id.grid_view);
		mImageAdapter = new ImageAdapter(this, mUrlList);
		mImageGridView.setAdapter(mImageAdapter);
	}

}
