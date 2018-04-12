package com.baidu.duer.dcs.guo_imagload;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.widget.ImageView;


/**
 * 图片的加载技术，缓存技术，缩略技术
 * @author wx
 *
 */
public class ImageLoader {
	private LruCache<String, Bitmap> mMemoryCache;//缓存技术，最近最少使用
	private Context mContext;
	private Bitmap mEmptyBitmap;
	public ImageLoader(Context context) {
		mContext = context;
		mEmptyBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_launcher);
		int maxMemory =  (int) (Runtime.getRuntime().maxMemory());
		int maxCacheSize = maxMemory/8;
		mMemoryCache = new LruCache<String, Bitmap>(maxCacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap value) {
				return value.getByteCount();
			}
		};
	}

	/**
	 *
	 * @param resId 两个作用，缓存的key值，获取图片资源的id
	 * @param imageView
	 */
	public void LoadBitmap(int resId, ImageView imageView) {
		final String imageKey = String.valueOf(resId);
		final Bitmap bitmap = getBitmapFromCache(imageKey);
		if (bitmap != null) {
			imageView.setImageBitmap(bitmap);
		} else if(cancelPotentialWork(resId, imageView)){
			BitmapWorkTask bitmapWorkTask = new BitmapWorkTask(imageView);
			AsyncDrawable asyncDrawable = new AsyncDrawable(mContext.getResources()
					, mEmptyBitmap, bitmapWorkTask);
			imageView.setImageDrawable(asyncDrawable);
			bitmapWorkTask.execute(resId);
		}
	}

	private boolean cancelPotentialWork(int resId, ImageView imageView) {
		BitmapWorkTask bitmapWorkTask = getBitmapWorkTask(imageView);
		if(bitmapWorkTask != null) {
			String imageKey = bitmapWorkTask.imageKey;
			if(imageKey == null || !imageKey.equals(String.valueOf(resId))) {
				bitmapWorkTask.cancel(true);
			} else {
				return false;
			}
		}
		return true;
	}
	private class AsyncDrawable extends BitmapDrawable {
		private WeakReference<BitmapWorkTask> wReference;
		public AsyncDrawable(Resources resources, Bitmap bitmap, BitmapWorkTask bitmapWorkTask) {
			super(resources, bitmap);
			wReference = new WeakReference<BitmapWorkTask>(bitmapWorkTask);
		}

		public BitmapWorkTask getBitmapWorkTask() {
			return wReference.get();
		}
	}

	private BitmapWorkTask getBitmapWorkTask(ImageView imageView) {
		Drawable drawable = imageView.getDrawable();
		if(drawable instanceof AsyncDrawable) {
			AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
			return asyncDrawable.getBitmapWorkTask();
		}
		return null;
	}

	private class BitmapWorkTask extends AsyncTask<Integer, Void, Bitmap> {
		private String imageKey;
		private WeakReference<ImageView> weakReference;
		public BitmapWorkTask(ImageView imageView) {
			weakReference = new WeakReference<ImageView>(imageView);
		}
		@Override
		protected Bitmap doInBackground(Integer... params) {
			imageKey = String.valueOf(params[0]);
			final Bitmap  bitmap = decodeSampledBitmapFromResourc(mContext.getResources()
					, params[0], 100, 100);
			addBitmapToCache(imageKey, bitmap);
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			super.onPostExecute(result);
			ImageView imageView = getAttachImageView();
			if(imageView != null && result != null) {
				imageView.setImageBitmap(result);
			}
		}
		private ImageView getAttachImageView() {
			ImageView imageView = weakReference.get();
			BitmapWorkTask bitmapWorkTask = getBitmapWorkTask(imageView);
			if(this == bitmapWorkTask) {
				return imageView;
			}
			return null;
		}

	}
	private void addBitmapToCache(String key, Bitmap bitmap) {
		if(mMemoryCache.get(key) == null) {
			mMemoryCache.put(key, bitmap);
		}
	}

	private Bitmap getBitmapFromCache(String key) {
		return mMemoryCache.get(key);
	}

	public static int calculateInSampleSize(BitmapFactory.Options options
			, int reqWidth, int reqHeight) {
		final int width = options.outWidth;
		final int height = options.outHeight;
		int inSampleSize = 1 ;
		if (height > reqHeight || width > reqHeight) {
			final int hegihtRatio = Math.round((float)height / (float)reqHeight);
			final int widthRatio = Math.round((float)width / (float)reqHeight);
			inSampleSize = widthRatio < hegihtRatio ? widthRatio : hegihtRatio;
		}
		return inSampleSize;
	}

	/**
	 * 高效加载图片，返回需要缩略图大小的图片
	 * @param resources
	 * @param resId
	 * @param reqWidth
	 * @param reqHeight
	 * @return
	 */
	public static Bitmap decodeSampledBitmapFromResourc(Resources resources, int resId
			, int reqWidth, int reqHeight) {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(resources, resId);
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeResource(resources, resId, options);
	}


}
