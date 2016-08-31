#include "com_android_grafika_gles_GLESNativeTool.h"
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

JNIEXPORT void JNICALL Java_com_android_grafika_gles_GLESNativeTool_glReadPixelWithJni
  (JNIEnv * env, jobject obj, jint x, jint y, jint width, jint height, jint format, jint type, jint offset){
	glReadPixels(x, y, width, height, format, type, 0);
}
