#include <jni.h>
#include <string>
#include <android/log.h>
#include <vector>

// OpenCV includes
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>

#define LOG_TAG "NativeLib"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__);

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_edgeviewer_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    LOGI("JNI function called successfully!");
    std::string hello = "Hello from C++ (JNI is working!)";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_edgeviewer_MainActivity_processFrame(
        JNIEnv *env,
        jobject /* this */,
        jbyteArray input,
        jint width,
        jint height) {

    jbyte *input_bytes = env->GetByteArrayElements(input, NULL);
    if (input_bytes == NULL) {
        LOGI("Error getting byte array elements");
        return NULL;
    }

    cv::Mat inputMat(height, width, CV_8UC4, (unsigned char *)input_bytes);

    // 1. Convert the input image to grayscale
    cv::Mat grayMat;
    cv::cvtColor(inputMat, grayMat, cv::COLOR_RGBA2GRAY);

    // 2. Apply Canny edge detection
    cv::Mat edgesMat;
    cv::Canny(grayMat, edgesMat, 50, 150);

    // 3. Convert the single-channel edges back to a 4-channel image
    cv::Mat rgbaMat;
    cv::cvtColor(edgesMat, rgbaMat, cv::COLOR_GRAY2RGBA);

    // 4. *** CRITICAL STEP FOR ANDROID BITMAP ***
    //    Convert the RGBA image to ARGB, which is the format Android's Bitmap expects.
    cv::Mat argbMat;
    cv::cvtColor(rgbaMat, argbMat, cv::COLOR_RGBA2BGRA); // BGRA is ARGB in memory layout

    env->ReleaseByteArrayElements(input, input_bytes, 0);

    // 5. Copy the final ARGB pixel data to a new jbyteArray to return to Kotlin
    jsize output_size = argbMat.total() * argbMat.elemSize();
    jbyteArray output = env->NewByteArray(output_size);
    if (output == NULL) {
        LOGI("Error creating new byte array");
        return NULL;
    }

    env->SetByteArrayRegion(output, 0, output_size, (jbyte *)argbMat.data);

    return output;
}
