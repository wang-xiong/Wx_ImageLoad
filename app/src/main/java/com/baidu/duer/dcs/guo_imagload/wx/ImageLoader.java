package com.baidu.duer.dcs.guo_imagload.wx;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;


public class ImageLoader {
	private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
	private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
	private static final int MAXIMUM_POOL_SIZE = CPU_COUNT *2 +1;
	private static final long KEEP_ALIVE = 10L;
	private static final ThreadFactory sThreadFactory = new ThreadFactory() {
		private final AtomicInteger mCount = new AtomicInteger(1);
		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r, "ImageLoader#" + mCount.getAndIncrement());
		}
	};
	public static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
			CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS
			, new LinkedBlockingDeque<Runnable>(), sThreadFactory);

	private Handler mMainHandler = new Handler(Looper.getMainLooper()) {
		public void handleMessage(Message msg) {
			LoaderResult result = (LoaderResult) msg.obj;
			ImageView imageView = result.imageView;
			String uri = (String) imageView.getTag(TAG_KEY_URI);
			if(uri.equals(result.uri)) {
				imageView.setImageBitmap(result.bitmap);
			} else {
				//uri 不一致 忽略。
			}
		};
	};

	private LruCache<String, Bitmap> mMemoryCache;
	private DiskLruCache mDiskLruCache;
	private boolean mIsDiskLruCacheCreated = false;
	private Context mContext;
	private final long DISK_CACHE_SIZE = 50 *1024 * 1024; //50MB
	private ImageResizer mImageResizer;
	private int IO_BUFFER_SIZE = 8 * 1024;
	private int MESSAG_POST_RESULT = 1;
	private int DISK_CACHE_INDEX = 0;
	private int TAG_KEY_URI = 0;
	private ImageLoader(Context context) {
		mContext = context.getApplicationContext();
		mImageResizer = new ImageResizer();
		int maxMemory = (int) (Runtime.getRuntime().maxMemory()/1024);//当前进程可用内存
		int cacheSize = maxMemory/8;
		mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap value) {
				return value.getRowBytes() * value.getHeight()/1024;
			}
		};

		File diskCacheDir = getDiskCacheDir(mContext, "bimap");
		if(!diskCacheDir.exists()) {
			diskCacheDir.mkdirs();
		}
		if(getUsableSpace(diskCacheDir) > DISK_CACHE_SIZE) {
			try {
				mDiskLruCache = DiskLruCache.open(diskCacheDir, 1, 1, DISK_CACHE_SIZE);
				mIsDiskLruCacheCreated = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	public static ImageLoader getInstance(Context context) {
		return new ImageLoader(context);
	}

	private long getUsableSpace(File path) {
		if(Build.VERSION.SDK_INT >= VERSION_CODES.GINGERBREAD) {
			return path.getUsableSpace();
		}
		final StatFs statFs = new StatFs(path.getPath());
		return (long) statFs.getBlockSize() * (long) statFs.getAvailableBlocks();
	}

	private File getDiskCacheDir(Context context, String string) {
		boolean externalStorageAvailable = Environment
				.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
		final String cachePath;
		if(externalStorageAvailable) {
			cachePath = context.getExternalCacheDir().getPath();
		} else {
			cachePath = context.getCacheDir().getPath();
		}
		return new File(cachePath + File.separator + string);
	}

	private void addBitampToMemoryCache(String key, Bitmap bitmap) {
		if(mMemoryCache.get(key) == null) {
			mMemoryCache.put(key, bitmap);
		}
	}

	private Bitmap getBitmapFromMemoryCache(String key) {
		return mMemoryCache.get(key);
	}

	/**
	 * 主线程调用获取图片
	 * @param uri
	 * @param imageView
	 */
	public void bindBitmap(final String uri, final ImageView imageView) {
		bindBitmap(uri, imageView, 0, 0);
	}
	/**
	 * 磁盘缓存的添加和读取
	 * @param url
	 * @param reqWidth
	 * @param reqHeight
	 * @return
	 */
	private Bitmap loadBitmapFromHttp(String url, int reqWidth, int reqHeight) {
		if(Looper.myLooper() == Looper.getMainLooper()) {
			throw new RuntimeException("can not visit network from UI Thread.");
		}
		if(mDiskLruCache == null) {
			return null;
		}
		String key = hashKeyFromUrl(url);
		DiskLruCache.Editor editor = mDiskLruCache.edit(key);
		if(editor != null) {
			OutputStream outputStream = editor.newOutputStream(DISK_CACHE_INDEX);
			if(downLoadUrlToStream(url, outputStream)) {
				editor.commit();
			} else {
				editor.abort();
			}
			mDiskLruCache.flush();
		}
		return loadBitmapFromDiskCache(url, reqWidth, reqHeight);
	}

	/**
	 * 下载bitmap并写入文件系统中
	 * @param urlString
	 * @param outputStream
	 * @return
	 */
	private boolean downLoadUrlToStream(String urlString, OutputStream outputStream) {
		HttpsURLConnection urlConnection = null;
		BufferedOutputStream out = null;
		BufferedInputStream in = null;
		try {
			final URL url = new URL(urlString);
			urlConnection = (HttpsURLConnection) url.openConnection();
			in = new BufferedInputStream(urlConnection.getInputStream(), IO_BUFFER_SIZE);
			out = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE);
			int b;
			while ((b =in.read()) != -1) {
				out.write(b);
			}
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(urlConnection != null) {
				urlConnection.disconnect();
			}
			if(in != null) {
				try {
					in .close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(out != null) {
				try {
					in .close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}

	/**
	 * 利用FileDescriptor获取压缩后bitmap并存入内存缓存中
	 * @param url
	 * @param reqWidth
	 * @param reqHeight
	 * @return
	 */
	private Bitmap loadBitmapFromDiskCache(String url, int reqWidth,
										   int reqHeight) {
		if(Looper.myLooper() == Looper.getMainLooper()) {
			//"can not visit network from UI Thread."
		}
		if(mDiskLruCache == null) {
			return null;
		}
		Bitmap bitmap = null;
		String key = hashKeyFromUrl(url);
		DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
		if(snapshot != null) {
			try {
				FileInputStream fileInputStream = (FileInputStream)snapshot
						.getInputStream(DISK_CACHE_INDEX);
				FileDescriptor fileDescriptor;
				fileDescriptor = fileInputStream.getFD();
				bitmap = mImageResizer.decodeBitmapFromFileDescriptor(fileDescriptor
						, reqWidth, reqHeight);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(bitmap != null) {
				addBitampToMemoryCache(key, bitmap);
			}
		}
		return bitmap;
	}

	private String hashKeyFromUrl(String url) {
		String cacheKey;
		try {
			final MessageDigest mDigest = MessageDigest.getInstance("MD5");
			mDigest.update(url.getBytes());
			cacheKey = bytesToHexString(mDigest.digest());
		} catch (NoSuchAlgorithmException e) {
			cacheKey = String.valueOf(url.hashCode());
		}
		return cacheKey;
	}

	private String bytesToHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (int i=0 ; i<bytes.length; i++) {
			String hex = Integer.toHexString(0XFF & bytes[i]);
			if(hex.length() == 1) {
				sb.append("0");
			}
			sb.append(hex);
		}
		return sb.toString();
	}

	/**
	 * 同步加载接口，需要再子线程被调用
	 * @param url
	 * @param reqWidth
	 * @param reqHeight
	 * @return
	 */
	public Bitmap loadBitmap(String url, int reqWidth, int reqHeight) {
		Bitmap bitmap = loadBitmapFromMemoryCache(url);
		if(bitmap != null) {
			//从内存获取bitmap；
			return bitmap;
		}
		bitmap = loadBitmapFromDiskCache(url, reqWidth, reqHeight);
		if(bitmap != null) {
			//从磁盘获取bitmap
			return bitmap;
		}
		bitmap = loadBitmapFromHttp(url, reqWidth, reqHeight);
		if(bitmap == null && !mIsDiskLruCacheCreated) {
			//从网络获取
			bitmap = downloadBitmapFromUrl(url);
		}
		return bitmap;
	}

	private Bitmap loadBitmapFromMemoryCache(String url) {
		final String key = hashKeyFromUrl(url);
		return getBitmapFromMemoryCache(key);
	}
	private Bitmap downloadBitmapFromUrl(String urlString) {
		Bitmap bitmap = null;
		HttpsURLConnection urlConnection = null;
		BufferedInputStream in = null;
		try {
			final URL url = new URL(urlString);
			urlConnection = (HttpsURLConnection) url.openConnection();
			in = new BufferedInputStream(urlConnection.getInputStream(), IO_BUFFER_SIZE);
			bitmap = BitmapFactory.decodeStream(in);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(urlConnection != null) {
				urlConnection.disconnect();
			}
			if(in != null) {
				try {
					in .close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return bitmap;
	}

	public void bindBitmap(final String uri, final ImageView imageView
			, final int reqWidth, final int reqHeight) {
		imageView.setTag(TAG_KEY_URI, uri);
		Bitmap bitmap = loadBitmapFromMemoryCache(uri);
		if(bitmap != null) {
			imageView.setImageBitmap(bitmap);
			return;
		}
		Runnable loadBitmapTask = new Runnable() {

			@Override
			public void run() {
				Bitmap bitmap = loadBitmap(uri, reqWidth, reqHeight);
				if(bitmap != null) {
					LoaderResult result = new LoaderResult(imageView, uri, bitmap);
					Message msg = mMainHandler.obtainMessage(MESSAG_POST_RESULT, result);
					msg.sendToTarget();
				}
			}
		};
		THREAD_POOL_EXECUTOR.execute(loadBitmapTask);
	}


	private static class LoaderResult {
		public ImageView imageView;
		public String uri;
		public Bitmap bitmap;
		public LoaderResult(ImageView imageView, String uri, Bitmap bitmap) {
			this.imageView = imageView;
			this.uri = uri;
			this.bitmap = bitmap;
		}

	}

}
