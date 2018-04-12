package com.baidu.duer.dcs.guo_imagload;

import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;

/**
 * 利用findVIewWithTag()关联Image和Url的关系;
 * @author wx
 *
 */
public class ImageAdapter1 extends BaseAdapter {
	private LruCache<String, BitmapDrawable> mMemoryCache;
	private String[] mImageUrls;
	private Context mContext;
	private ListView mListView;
	public ImageAdapter1(Context context, String[] ImageUrls) {
		mContext = context;
		mImageUrls = ImageUrls;
		int maxMemorySize = (int) Runtime.getRuntime().maxMemory();
		int cacheSize = maxMemorySize/8;
		mMemoryCache = new LruCache<String, BitmapDrawable>(cacheSize) {
			@Override
			protected int sizeOf(String key, BitmapDrawable value) {
				return value.getBitmap().getByteCount();
			}
		};
	}

	@Override
	public int getCount() {
		return mImageUrls.length;
	}

	@Override
	public String getItem(int position) {
		return mImageUrls[position];
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if(mListView == null) {
			mListView = (ListView) parent;
		}
		ViewHolder holder;
		if(convertView == null) {
			convertView = LayoutInflater.from(mContext).inflate(R.layout.list_view_item, null);
			holder = new ViewHolder(convertView);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		holder.imageView.setTag(mImageUrls[position]);
		BitmapDrawable drawable = getBitmapFromCache(mImageUrls[position]);
		if (drawable != null) {
			holder.imageView.setImageDrawable(drawable);
		} else {
			ImageWorkerTask task = new ImageWorkerTask();
			task.execute(mImageUrls);
		}
		return convertView;
	}

	private BitmapDrawable getBitmapFromCache(String key) {
		return mMemoryCache.get(key);
	}

	private void addBitmapToCache(String key, BitmapDrawable drawable) {
		if(mMemoryCache.get(key) == null ) {
			mMemoryCache.put(key, drawable);
		}
	}

	private class ViewHolder {
		public ImageView imageView;

		public ViewHolder(View convertView) {
			imageView = (ImageView) convertView.findViewById(R.id.image);
		}
	}

	private class ImageWorkerTask extends AsyncTask<String, Void, BitmapDrawable> {
		String mImageUrl;
		@Override
		protected BitmapDrawable doInBackground(String... params) {
			mImageUrl = params[0];
			Bitmap bitmap = downloadBitamp(mImageUrl);
			BitmapDrawable drawable = new BitmapDrawable(mContext.getResources(), bitmap);
			addBitmapToCache(mImageUrl, drawable);
			return drawable;
		}

		private Bitmap downloadBitamp(String imageUrl) {
			Bitmap bitmap = null;
			HttpsURLConnection connection = null;
			try {
				URL url = new URL(imageUrl);
				connection = (HttpsURLConnection) url.openConnection();
				connection.setConnectTimeout(5 * 1000);
				connection.setReadTimeout(10 * 1000);
				bitmap = BitmapFactory.decodeStream(connection.getInputStream());
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if(connection != null) {
					connection.disconnect();
				}
			}
			return bitmap;
		}

		@Override
		protected void onPostExecute(BitmapDrawable drawable) {
			super.onPostExecute(drawable);
			ImageView imageView = (ImageView) mListView.findViewWithTag(mImageUrl);
			imageView.setImageDrawable(drawable);
		}
	}
}
