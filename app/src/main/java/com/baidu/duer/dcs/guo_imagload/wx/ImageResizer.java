package com.baidu.duer.dcs.guo_imagload.wx;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

import java.io.FileDescriptor;

//图标压缩功能
public class ImageResizer {
	private static final String TAG = "ImageResizer";

	public ImageResizer() {

	}

	/**
	 * 从Resources获取bitmap
	 * @param res
	 * @param resId
	 * @param reqWidth
	 * @param reqHeight
	 * @return
	 */
	public Bitmap decodeBtimapFromResource(Resources res,
										   int resId, int reqWidth, int reqHeight) {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, resId, options);

		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
		//计算inSampleSize
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeResource(res, resId, options);
	}

	/**
	 * 从FileDescriptor获取bitmap
	 * @param fd
	 * @param reqWidth
	 * @param reqHeight
	 * @return
	 */
	public Bitmap decodeBitmapFromFileDescriptor(FileDescriptor fd,
												 int reqWidth, int reqHeight) {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFileDescriptor(fd, null, options);
		//计算inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeFileDescriptor(fd, null, options);
	}
	/**
	 * 计算采样率
	 * @param options
	 * @param reqWidth
	 * @param reqHeight
	 * @return
	 */
	private int calculateInSampleSize(Options options, int reqWidth,
									  int reqHeight) {
		if(reqWidth == 0 || reqHeight ==0) {
			return 1;
		}
		final int widhth = options.outWidth;
		final int hegiht = options.outHeight;
		int inSampleSize = 1;

		if(hegiht > reqHeight || widhth > reqWidth) {
			final int halfHegiht = hegiht/2;
			final int halfWidth = widhth/2;
			while ((halfWidth/inSampleSize) >= reqHeight
					&& (halfHegiht/inSampleSize) >= reqWidth) {
				inSampleSize *= 2;
			}
		}
		return inSampleSize;
	}
}
