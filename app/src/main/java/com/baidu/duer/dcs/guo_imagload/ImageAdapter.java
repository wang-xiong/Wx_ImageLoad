package com.baidu.duer.dcs.guo_imagload;

import java.io.IOException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

/**
 * 缓存，异步的思想，但是会出现线程同步问题，图片加载显示乱序问题
 * @author wx
 *
 */
public class ImageAdapter extends ArrayAdapter<String> {
	private LruCache<String, BitmapDrawable> mMemoryCache;

	public ImageAdapter(Context context, int resource, String[] objects) {
		super(context, resource, objects);
		int maxMemory = (int) Runtime.getRuntime().maxMemory();
		int cacheSize = maxMemory/8;
		mMemoryCache = new LruCache<String, BitmapDrawable>(cacheSize) {
			@Override
			protected int sizeOf(String key, BitmapDrawable value) {
				return value.getBitmap().getByteCount();
			}
		};
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		String url = getItem(position);
		ViewHolder viewHolder;
		if(convertView == null) {
			convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_view_item, null);
			viewHolder = new ViewHolder(convertView);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		BitmapDrawable bitmapDrawable = getBitmapFromMemoryCache(url);
		if(bitmapDrawable != null) {
			viewHolder.imageView.setImageDrawable(bitmapDrawable);
		} else {
			BitampWorkerTask task = new BitampWorkerTask(viewHolder.imageView);
			task.equals(url);
		}


		return convertView;
	}

	private void addBitmapToMemoryCache(String key, BitmapDrawable value) {
		if(mMemoryCache.get(key) == null) {
			mMemoryCache.put(key, value);
		}
	}

	private BitmapDrawable getBitmapFromMemoryCache(String key) {
		return mMemoryCache.get(key);

	}

	private class ViewHolder {
		public ImageView imageView;
		public ViewHolder(View convertView) {
			imageView = (ImageView) convertView.findViewById(R.id.image);
		}
	}

	private class BitampWorkerTask extends AsyncTask<String, Void, BitmapDrawable> {
		private ImageView mImageView;

		public BitampWorkerTask(ImageView imageView) {
			mImageView = imageView;
		}

		@Override
		protected BitmapDrawable doInBackground(String... params) {
			String imageUrl = params[0];
			Bitmap bitmap = downLoadBitmap(imageUrl);
			BitmapDrawable bitmapDrawable = new BitmapDrawable(getContext().getResources(), bitmap);
			addBitmapToMemoryCache(imageUrl, bitmapDrawable);
			return bitmapDrawable;
		}

		@Override
		protected void onPostExecute(BitmapDrawable bitmapDrawable) {
			super.onPostExecute(bitmapDrawable);
			if(mImageView != null && bitmapDrawable != null) {
				mImageView.setImageDrawable(bitmapDrawable);
			}
		}

		private Bitmap downLoadBitmap(String imageUrl) {
			Bitmap bitmap = null;
			HttpsURLConnection connection = null;
			try {
				URL url = new URL(imageUrl);
				connection = (HttpsURLConnection) url.openConnection();
				connection.setConnectTimeout(5 * 1000);
				connection.setReadTimeout(10 * 1000);
				bitmap = BitmapFactory.decodeStream(connection.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (connection != null ) {
					connection.disconnect();
				}
			}
			return bitmap;
		}
	}

}
