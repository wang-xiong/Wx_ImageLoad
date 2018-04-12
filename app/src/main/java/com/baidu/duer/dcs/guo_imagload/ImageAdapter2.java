package com.baidu.duer.dcs.guo_imagload;

import java.lang.ref.WeakReference;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

/**
 * ImageView AyncDrawable BitmapWorkTask 两层弱引用关联起来
 * @author wx
 *
 */
public class ImageAdapter2 extends BaseAdapter{
	private Context mContext;
	private String[] mImageUrls;
	private LruCache<String, BitmapDrawable> mMemoryCache;//删除最近最少使用的缓存
	private Bitmap mLoadingBitmap;
	public ImageAdapter2(Context context, String[] obejcts) {
		mContext = context;
		mImageUrls = obejcts;
		mLoadingBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_launcher);
		int maxMemoryCache = (int) Runtime.getRuntime().maxMemory();
		int cacheSize = maxMemoryCache/8;
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
	public Object getItem(int position) {
		return mImageUrls[position];
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		String imageUrl = mImageUrls[position];
		ViewHolder holder = null;
		if (convertView == null) {
			convertView = LayoutInflater.from(mContext).inflate(R.layout.list_view_item, null);
			holder = new ViewHolder(convertView);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		BitmapDrawable drawable = getBitmapFromCache(imageUrl);
		if(drawable != null) {
			holder.imageView.setImageDrawable(drawable);
		} else if (cancelPotentialWoek(imageUrl, holder.imageView)){
			BitmapWorkerTask task = new BitmapWorkerTask(holder.imageView);
			AsyncDrawable asyncDrawable = new AsyncDrawable(mContext.getResources(), mLoadingBitmap, task);
			holder.imageView.setImageDrawable(asyncDrawable);
			task.execute(mImageUrls);
		}
		return convertView;
	}

	private class ViewHolder {
		public ImageView imageView;
		public ViewHolder(View convertView){
			imageView = (ImageView) convertView.findViewById(R.id.image);
		}
	}

	private BitmapDrawable getBitmapFromCache(String key) {
		return mMemoryCache.get(key);
	}

	private void addBitmapToCache(String key, BitmapDrawable drawable) {
		if(mMemoryCache.get(key) == null ) {
			mMemoryCache.put(key, drawable);
		}
	}

	/**
	 * 取消后台的潜在任务，如果发现当前ImageView存在另一个请求时，把它取消返回true
	 * @param url
	 * @param imageView
	 * @return
	 */
	public boolean cancelPotentialWoek(String url, ImageView imageView) {
		BitmapWorkerTask task = getBitmpWorkerTask(imageView);
		if(task != null){
			String imageUrl = task.imageUrl;
			if(imageUrl == null || !imageUrl.equals(url)) {
				task.cancel(true);
			} else {
				return false;
			}
		}
		return true;
	}

	private class AsyncDrawable extends BitmapDrawable {
		private WeakReference<BitmapWorkerTask> reference;

		public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask task) {
			super(res, bitmap);
			reference = new WeakReference<BitmapWorkerTask>(task);
		}

		public BitmapWorkerTask getBitmapWorkerTask() {
			return reference.get();
		}
	}
	private class BitmapWorkerTask extends AsyncTask<String, Void, BitmapDrawable> {
		private WeakReference<ImageView> reference;
		private String imageUrl;
		public BitmapWorkerTask(ImageView imageView) {
			reference = new WeakReference<ImageView>(imageView);
		}

		@Override
		protected BitmapDrawable doInBackground(String... params) {
			imageUrl = params[0];
			Bitmap bitmap = dowonloadBitmap(imageUrl);
			BitmapDrawable drawable = new BitmapDrawable(mContext.getResources(), bitmap);
			addBitmapToCache(imageUrl, drawable);
			return drawable;
		}

		@Override
		protected void onPostExecute(BitmapDrawable drawable) {
			super.onPostExecute(drawable);
			ImageView imageView = getAttachImageView();
			if(imageView != null && drawable != null) {
				imageView.setImageDrawable(drawable);
			}
		}
		private ImageView getAttachImageView() {
			ImageView imageView = reference.get();
			BitmapWorkerTask task = getBitmpWorkerTask(imageView);
			if(this == task) {
				return imageView;
			}
			return null;
		}

		private Bitmap dowonloadBitmap(String imageUrl) {
			Bitmap bitmap = null;
			HttpsURLConnection connection = null;
			try {
				URL url = new URL(imageUrl);
				connection = (HttpsURLConnection) url.openConnection();
				connection.setConnectTimeout(5 * 1000);
				connection.setReadTimeout(10 * 1000);
				bitmap = BitmapFactory.decodeStream(connection.getInputStream());
			} catch(Exception e) {
				e.printStackTrace();
			} finally {
				if (connection !=null) {
					connection.disconnect();
				}
			}
			return bitmap;
		}

	}

	public BitmapWorkerTask getBitmpWorkerTask(ImageView imageView) {
		if(imageView != null) {
			Drawable drawable = imageView.getDrawable();
			if(drawable instanceof AsyncDrawable) {
				AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
				return asyncDrawable.getBitmapWorkerTask();
			}
		}
		return null;
	}
}
