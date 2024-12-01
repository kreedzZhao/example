package io.github.libxposed.msa

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModuleInterface
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.annotations.AfterInvocation
import io.github.libxposed.api.annotations.BeforeInvocation
import io.github.libxposed.api.annotations.XposedHooker
import io.github.libxposed.utils.KreModule


private lateinit var module: HookEntry

class HookEntry(base: XposedInterface, param: ModuleLoadedParam) : KreModule(base, param) {
    init {
        log("HookEntry at " + param.processName)
        module = this
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("DiscouragedPrivateApi")
    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        super.onPackageLoaded(param)
        log("onPackageLoaded: " + param.packageName)
        log("param classloader is " + param.classLoader)
        log("module apk path: " + this.applicationInfo.sourceDir)

        try {
            System.loadLibrary("hooktest")
        }catch (e: UnsatisfiedLinkError) {
            log("Failed to load native library: " + e.message, e)
        }

        try {
            val eClass = Class.forName("A4.e", false, param.classLoader)
            val chainClass = Class.forName("okhttp3.Interceptor\$Chain", false, param.classLoader)

            // 这里居然传入接口
//            val interceptMethod = eClass.getDeclaredMethod("intercept", chainClass)
//            hook(interceptMethod, InterceptHooker::class.java)

            hookMethod(InterceptHooker::class.java, eClass, "intercept", chainClass)
            // okhttp3.Request
//            val requestClass = Class.forName("okhttp3.Request", false, param.classLoader)
//            hookAllConstructors(DefaultHooker::class.java, requestClass)
        } catch (e: NoSuchMethodException) {
            log("NoSuchMethodException: ${e.message}")
        } catch (e: ClassNotFoundException) {
            log("ClassNotFoundException: ${e.message}")
        }
        log("----------")
    }


    @XposedHooker
    class InterceptHooker(private val magic: Int) : Hooker {
        companion object {
            @JvmStatic
            @BeforeInvocation
            fun beforeInvocation(callback: XposedInterface.BeforeHookCallback): InterceptHooker {
                val chain = callback.args[0]
                val request = chain::class.java.getDeclaredMethod("request").invoke(chain)
                module.log("beforeInvocation: $request")
                // okhttp3.Request
                val requestClass =
                    Class.forName("okhttp3.Request", false, chain::class.java.classLoader)
                val response = chain::class.java.getDeclaredMethod("proceed", requestClass)
                    .invoke(chain, request)
                callback.returnAndSkip(response)
                return InterceptHooker(0)
            }

            @JvmStatic
            @AfterInvocation
            fun afterInvocation(
                callback: XposedInterface.AfterHookCallback,
                context: InterceptHooker
            ) {
                module.log("afterInvocation")
            }
        }
    }


    @XposedHooker
    class DefaultHooker(private val magic: Int) : Hooker {
        companion object {
            @JvmStatic
            @BeforeInvocation
            fun beforeInvocation(callback: XposedInterface.BeforeHookCallback): DefaultHooker {
                module.log("Default beforeInvocation")
                return DefaultHooker(0)
            }

            @JvmStatic
            @AfterInvocation
            fun afterInvocation(
                callback: XposedInterface.AfterHookCallback,
                context: DefaultHooker
            ) {
                module.log("Default afterInvocation: ${Gson().toJson(callback.thisObject)}")
            }
        }
    }

}