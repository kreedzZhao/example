#include <cstdio>
#include <cstring>
#include <jni.h>
#include <string>
#include <dlfcn.h>
#include <android/log.h>
#include "lsposed.h"


static HookFunType hook_func = nullptr;

int (*backup)();

int fake() {
    return backup() + 1;
}

FILE *(*backup_fopen)(const char *filename, const char *mode);

FILE *fake_fopen(const char *filename, const char *mode) {
    if (strstr(filename, "banned")) return nullptr;
    __android_log_print(ANDROID_LOG_INFO, "LSPosedContext", "fopen: %s", filename);
    return backup_fopen(filename, mode);
}

jclass (*backup_FindClass)(JNIEnv *env, const char *name);
jclass fake_FindClass(JNIEnv *env, const char *name)
{
    if(!strcmp(name, "dalvik/system/BaseDexClassLoader"))
        return nullptr;
    return backup_FindClass(env, name);
}

void on_library_loaded(const char *name, void *handle) {
    // hooks on `libtarget.so`
    if (std::string(name).ends_with("libtarget.so")) {
        void *target = dlsym(handle, "target_fun");
        hook_func(target, (void *) fake, (void **) &backup);
    }
}

extern "C" [[gnu::visibility("default")]] [[gnu::used]]
jint JNI_OnLoad(JavaVM *jvm, void*) {
    JNIEnv *env = nullptr;
    jvm->GetEnv((void **)&env, JNI_VERSION_1_6);
    hook_func((void *)env->functions->FindClass, (void *)fake_FindClass, (void **)&backup_FindClass);
    __android_log_print(ANDROID_LOG_INFO, "LSPosedContext", "JNI_OnLoad ended");
    return JNI_VERSION_1_6;
}

extern "C" [[gnu::visibility("default")]] [[gnu::used]]
NativeOnModuleLoaded native_init(const NativeAPIEntries *entries) {
    hook_func = entries->hook_func;
    // system hooks
    hook_func((void*) fopen, (void*) fake_fopen, (void**) &backup_fopen);
    return on_library_loaded;
}