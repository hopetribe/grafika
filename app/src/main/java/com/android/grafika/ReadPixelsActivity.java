package com.android.grafika;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.opengl.GLES10;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.grafika.gles.EglCore;
import com.android.grafika.gles.OffscreenSurface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

/**
 * Basic glReadPixels() speed test.
 */
public class ReadPixelsActivity extends Activity {
    private static final String TAG = ReadPixelsActivity.TAG + ":ericczhuang";

    private static final int WIDTH = 640;

    private static final int HEIGHT = 368;

    private static final int ITERATIONS = 100;

    private volatile boolean mIsCanceled;

    // only for debug, Added by Eric Huang
    boolean usePbo = true;

    private final static int DEFUALT_NUMBER_PBOS = 2;

    int[] pboIds;

    int index = 0;

    int mPBOQuantity = DEFUALT_NUMBER_PBOS;

    CheckBox pboCheckBox;

    ImageView imvResult;

    EditText mLooperCountEditText, mPBOQuantityEditText;

    TextView mResultTextView;

    private final static boolean DEBUG = true;

    // end here

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_pixels);
        pboCheckBox = (CheckBox)findViewById(R.id.pbo_check);
        imvResult = (ImageView)findViewById(R.id.imv_result);
        mLooperCountEditText = (EditText)findViewById(R.id.et_cycles);
        mPBOQuantityEditText = (EditText)findViewById(R.id.et_pbo_quantity);
        mResultTextView = (TextView)findViewById(R.id.gfxResult_text);

    }

    /**
     * Sets the text in the message field.
     */
    void setMessage(int id, String msg) {
        TextView result = (TextView)findViewById(id);
        result.setText(msg);
    }

    private AlertDialog showProgressDialog() {
        // Put up the progress dialog.
        AlertDialog.Builder builder = WorkDialog.create(this, R.string.running_test);
        builder.setCancelable(false); // only by button
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mIsCanceled = true;
                // let the async task handle dismiss the dialog
            }
        });
        return builder.show();

    }

    /**
     * onClick handler for gfx test button.
     */
    public void clickRunGfxTest(@SuppressWarnings("unused")
                                        View unused) {
        usePbo = pboCheckBox.isChecked();

        Resources res = getResources();
        String running = res.getString(R.string.state_running);
        setMessage(R.id.gfxResult_text, running);

        int loop = ITERATIONS;
        try {
            loop = Integer.valueOf(mLooperCountEditText.getText().toString());
            mPBOQuantity = Integer.valueOf(mPBOQuantityEditText.getText().toString());
        } catch (NumberFormatException e) {
        }
        // AlertDialog dialog = showProgressDialog();

        ReadPixelsTask task = new ReadPixelsTask(null, R.id.gfxResult_text, WIDTH, HEIGHT, loop);
        mIsCanceled = false;
        task.execute();
    }

    /**
     * AsyncTask class that executes the test.
     */
    private class ReadPixelsTask extends AsyncTask<Void, Integer, Long> {
        private int mWidth;

        private int mHeight;

        private int mImageSize;

        private int mIterations;

        private int mResultTextId;

        private AlertDialog mDialog;

        private ProgressBar mProgressBar;

        ByteBuffer mPixelBuffer;

        private int downloadNumber;
        
        GlPboReader mGlPboReader;
        
        final Bitmap bmp;

        /**
         * Prepare for the glReadPixels test.
         */
        public ReadPixelsTask(AlertDialog dialog, int resultTextId, int width, int height, int iterations) {
            mDialog = dialog;
            mResultTextId = resultTextId;
            mWidth = width;
            mHeight = height;

            mIterations = iterations;

            mImageSize = width * height * 4;

            byte[] bufferBytes = new byte[mImageSize];
            mPixelBuffer = ByteBuffer.wrap(bufferBytes);
            mPixelBuffer.order(ByteOrder.nativeOrder());

            pboIds = new int[mPBOQuantity];

            // mProgressBar =
            // (ProgressBar)dialog.findViewById(R.id.work_progress);
            // mProgressBar.setMax(mIterations);
            downloadNumber = 0;
            bmp = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
            // initPBO(mWidth, mHeight, NUMBER_PBOS);
        }

        @Override
        protected Long doInBackground(Void... params) {
            long result = -1;
            EglCore eglCore = null;
            OffscreenSurface surface = null;

            // TODO: this should not use AsyncTask. The AsyncTask worker thread
            // is run at
            // a lower priority, making it unsuitable for benchmarks. We can
            // counteract
            // it in the current implementation, but this is not guaranteed to
            // work in
            // future releases.
            Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);

            try {
                eglCore = new EglCore(null, 0);
                surface = new OffscreenSurface(eglCore, mWidth, mHeight);
                Log.d(TAG, "Buffer size " + mWidth + "x" + mHeight);
                result = runGfxTest(surface);
            } finally {
                if (surface != null) {
                    surface.release();
                }
                if (eglCore != null) {
                    eglCore.release();
                }
            }
            return result < 0 ? result : result / mIterations;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            // mProgressBar.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(Long result) {
            Log.d(TAG, "onPostExecute result=" + result);
            // mDialog.dismiss();
            // mDialog = null;

            Resources res = getResources();
            if (result < 0) {
                setMessage(mResultTextId, res.getString(R.string.did_not_complete));
            } else {
                // setMessage(mResultTextId, (result / 1000) +
                // res.getString(R.string.usec_per_iteration));
                setMessage(mResultTextId, "Average time = " + (totalTime / mIterations) + " ms");
            }
        }

        /**
         * Does a simple bit of rendering and then reads the pixels back.
         * 
         * @return total time spent on glReadPixels()
         * @throws IOException
         */
        long totalTime = 0;

        private long runGfxTest(OffscreenSurface eglSurface) {

            eglSurface.makeCurrent();

            Log.d(TAG, "Running...");
            if (usePbo && GlPboReader.isPboSupport(getApplicationContext())) {
				mGlPboReader = new GlPboReader(mWidth, mHeight);
			}
            //initPBO(mWidth, mHeight, mPBOQuantity);
            float colorMult = 1.0f / mIterations;
            Random random = new Random();
            for (int i = 0; i < mIterations; i++) {
                if (mIsCanceled) {
                    Log.d(TAG, "Canceled!");
                    totalTime = -2;
                    break;
                }
                // if ((i % (mIterations / 8)) == 0) {
                // publishProgress(i);
                // }
                float r = random.nextInt() % 256;//i * colorMult;
                float g = random.nextInt() % 256;//1.0f - r;
                float b = random.nextInt() % 256;//(r + g) / 2.0f;
                GLES20.glClearColor(r, g, b, 1.0f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
                GLES20.glScissor(mWidth / 4, mHeight / 4, mWidth / 2, mHeight / 2);
                GLES20.glClearColor(b, g, r, 1.0f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                GLES20.glDisable(GLES20.GL_SCISSOR_TEST);

                // Try to ensure that rendering has finished.
                GLES30.glFinish();

                long startWhen = System.currentTimeMillis();
                GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 4);
                if (usePbo && mGlPboReader != null) {
                	Log.i(TAG, "downloadGpuBufferWithPbo");
                	mPixelBuffer = mGlPboReader.downloadGpuBufferWithPbo();
                	//mGlPboReader.downloadGpuBufferWithPbo(mPixelBuffer);
                } else {
                    mPixelBuffer.rewind();
                    GLES10.glReadPixels(0, 0, mWidth, mHeight, GLES10.GL_RGBA, GLES10.GL_UNSIGNED_BYTE, mPixelBuffer);
                }
                long finishTime =  System.currentTimeMillis();
                long costTime = finishTime - startWhen;
                if (DEBUG) {
                	Log.i(TAG, "cost time = " + costTime);
                }
                totalTime += costTime;
                showInImageView();
                //mPixelBuffer.rewind();

            }
            return totalTime;
        }

        private void showInImageView() {
        	if (mPixelBuffer == null) {
				return;
			}
            bmp.copyPixelsFromBuffer(mPixelBuffer);
            imvResult.post(new Runnable() {

                @Override
                public void run() {
                	Log.i(TAG, "showInImageView end position: ");
                    imvResult.setImageBitmap(bmp);

                }
            });
        }

    }

}