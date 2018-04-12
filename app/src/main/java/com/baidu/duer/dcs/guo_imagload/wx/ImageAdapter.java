package com.baidu.duer.dcs.guo_imagload.wx;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.baidu.duer.dcs.guo_imagload.R;
import com.baidu.duer.dcs.guo_imagload.wx.ImageLoader;

import java.util.ArrayList;

public class ImageAdapter extends BaseAdapter{
	private ArrayList<String> mUrlList;
	private Context mContext;
	private ImageLoader mImageLoader;
	private boolean mIsGridViewIdle = false;
	private boolean mCanGetBitmapFromNetWork = false;
	private int mImageWidth;
	private int mImageHeight;

	public ImageAdapter(Context context, ArrayList<String> urlList) {
		mUrlList = urlList;
		mContext = context;
		mImageLoader = ImageLoader.getInstance(mContext);
		mImageWidth = (int) mContext.getResources().getDimension(R.dimen.image_width);
		mImageHeight = (int) mContext.getResources().getDimension(R.dimen.image_height);
	}
	@Override
	public int getCount() {
		return mUrlList.size();
	}

	@Override
	public Object getItem(int position) {
		return mUrlList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		if(convertView == null) {
			convertView = LayoutInflater.from(mContext).inflate(R.layout.gridview_item, parent, false);
			holder = new ViewHolder();
			holder.imageView = (ImageView) convertView.findViewById(R.id.image);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		ImageView imageView = holder.imageView;
		final String tag = (String) imageView.getTag();
		final String url = (String) getItem(position);
		if(!url.equals(tag)) {
			imageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_launcher));
		}
		if(mIsGridViewIdle && mCanGetBitmapFromNetWork) {
			imageView.setTag(url);
			mImageLoader.bindBitmap(url, imageView, mImageWidth, mImageHeight);
		}
		return convertView;
	}

	private class ViewHolder {
		private ImageView imageView;
	}
}
