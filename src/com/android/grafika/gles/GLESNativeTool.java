package com.android.grafika.gles;


public class GLESNativeTool {
    static{
        System.loadLibrary("gles_tools");
    }
    public static native void glReadPixelWithJni(int x, int y, int width, int height, int format, int type, int offset);
}
