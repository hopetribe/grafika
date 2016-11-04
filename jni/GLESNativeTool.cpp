#include "com_android_grafika_gles_GLESNativeTool.h"
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

JNIEXPORT void JNICALL Java_com_android_grafika_gles_GLESNativeTool_glReadPixelWithJni
  (JNIEnv * env, jobject obj, jint x, jint y, jint width, jint height, jint format, jint type, jint offset){
	glReadPixels(x, y, width, height, format, type, 0);
}

JNIEXPORT void JNICALL Java_com_android_grafika_gles_GLESNativeTool_convertByteBufferToByteArray
  (JNIEnv * env, jobject obj, jobject input, jbyteArray output){

	jbyte *c_array = env->GetByteArrayElements(output, 0);

	jclass cls = env->GetObjectClass(input);
	jfieldID fid = env->GetFieldID(cls, "data","Ljava/nio/ByteBuffer;");
	jobject bar = env->GetObjectField(obj, fid);
	c_array = (jbyte *)env->GetDirectBufferAddress(bar);
}

