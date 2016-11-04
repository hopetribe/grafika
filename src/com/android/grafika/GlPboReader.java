package com.android.grafika;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLES30;
import android.os.Build;
import android.util.Log;

import com.android.grafika.gles.GLESNativeTool;

/*
 * Download pixel buffer from GPU to CPU with PBO.
 * Only works with GL ES 3.0 
 * Created By Huang Chengzong
 * 2016/11/4
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class GlPboReader {

	private final static String TAG = "GlPboReader";

	private final static int DEFUALT_NUMBER_PBOS = 2;
	int[] mPboIds;
	int mPboIndex = 0;
	int mPboNumember = DEFUALT_NUMBER_PBOS;
	private int mPboBufferSize;
	private int mPboDownloadCount;
	private AtomicBoolean mIsInit = new AtomicBoolean(false);

	private int mWidth, mHeight;

	public GlPboReader(int width, int height) {
		mWidth = width;
		mHeight = height;
		mPboBufferSize = mWidth * height * 4;
		initPBO();
	}

	public static boolean isPboSupport(final Context context) {
		final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
		return configurationInfo.reqGlEsVersion >= 0x30000;
	}

	private void initPBO() {
		Log.i(TAG, "initPBO");
		mPboIds = new int[DEFUALT_NUMBER_PBOS];
		GLES30.glGenBuffers(DEFUALT_NUMBER_PBOS, mPboIds, 0);

		for (int i = 0; i < DEFUALT_NUMBER_PBOS; i++) {
			GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mPboIds[i]);
			GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, mPboBufferSize, null, GLES30.GL_STREAM_DRAW);
		}

		GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);
		mIsInit.set(true);
	}

	public ByteBuffer downloadGpuBufferWithPbo() {

		ByteBuffer pb = ByteBuffer.allocate(mPboBufferSize);
		mPboIndex = (mPboIndex + 1) % mPboNumember;

		int nextPboIndex = (mPboIndex + 1) % mPboNumember;

		if (mPboDownloadCount < mPboNumember) {
			GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mPboIds[mPboIndex]);
			GLESNativeTool.glReadPixelWithJni(0, 0, mWidth, mHeight, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, 0);
		} else {
			GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mPboIds[mPboIndex]);

			GLESNativeTool.glReadPixelWithJni(0, 0, mWidth, mHeight, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, 0);
			GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mPboIds[nextPboIndex]);

			pb = (ByteBuffer) GLES30.glMapBufferRange(GLES30.GL_PIXEL_PACK_BUFFER, 0, mPboBufferSize, GLES30.GL_MAP_READ_BIT);

			GLES30.glUnmapBuffer(GLES30.GL_PIXEL_PACK_BUFFER);
			GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);

		}
		mPboDownloadCount++;
		if (mPboDownloadCount == Integer.MAX_VALUE) {
			mPboDownloadCount = mPboNumember;
		}
		GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);
		return pb;
	}

}
