package io.github.libxposed.msa

import android.R.attr.classLoader
import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModuleInterface
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.annotations.AfterInvocation
import io.github.libxposed.api.annotations.BeforeInvocation
import io.github.libxposed.api.annotations.XposedHooker
import io.github.libxposed.utils.KreModule
import java.net.URL


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

//            val aClass = Class.forName("H8.a", false, param.classLoader)
//            hookAllConstructors(DefaultHooker::class.java, aClass)
//            // f8.m
//            val mClass = Class.forName("f8.m", false, param.classLoader)
//            hookAllConstructors(DefaultHooker::class.java, mClass)
//            // H8.PlayIntegrityAppCheckProvider 直接查找接口的交叉引用
//            // B7.C0646m.c
//            val cClass = Class.forName("B7.m", false, param.classLoader)
//            hookMethod(DefaultHooker::class.java, cClass, "c", Exception::class.java)

            // D8.b
            val bClass = Class.forName("D8.b", false, param.classLoader)
            hookAllConstructors(DefaultHooker::class.java, bClass)

            // D8.i
            val iClass = Class.forName("D8.i", false, param.classLoader)
            val jClass = Class.forName("D8.j", false, param.classLoader)
            hookMethod(QueryHooker::class.java, iClass, "c", URL::class.java, ByteArray::class.java, jClass, Boolean::class.java)
            // n8.i onTransact 通过 binder 发送数据
            // 所以需要到

        } catch (e: NoSuchMethodException) {
            log("NoSuchMethodException: ${e.message}")
        } catch (e: ClassNotFoundException) {
            log("ClassNotFoundException: ${e.message}")
        }
        log("----------")
    }


    @XposedHooker
    class QueryHooker(private val magic: Int) : Hooker {
        companion object {
            @JvmStatic
            @BeforeInvocation
            fun beforeInvocation(callback: XposedInterface.BeforeHookCallback): QueryHooker {
                module.log("Default beforeInvocation url: ${callback.args[0]}")
                val bytes = callback.args[1] as ByteArray
                module.log("Default beforeInvocation byteArr: ${String(bytes)}")
                module.log("Default beforeInvocation arg3: ${callback.args[2]}", Throwable())
                return QueryHooker(0)
            }

            @JvmStatic
            @AfterInvocation
            fun afterInvocation(
                callback: XposedInterface.AfterHookCallback,
                context: QueryHooker
            ) {
                module.log("Default afterInvocation")
            }
        }
    }


    @XposedHooker
    class InterceptHooker(private val magic: Int) : Hooker {
        companion object {
            @JvmStatic
            @BeforeInvocation
            fun beforeInvocation(callback: XposedInterface.BeforeHookCallback): InterceptHooker {
                val chain = callback.args[0]
                val request = chain::class.java.getDeclaredMethod("request").invoke(chain)
//                module.log("beforeInvocation: $request")
                module.log("beforeInvocation")
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
//                module.log("Default beforeInvocation arg0: ${callback.args[0].javaClass.name} -> ${callback.args[0]}", Throwable())
                module.log("Default beforeInvocation arg0: ${callback.args[0]}")
                module.log("Default beforeInvocation arg1: ${callback.args[1]}")
                module.log("Default beforeInvocation arg2: ${callback.args[2]}", Throwable())
                return DefaultHooker(0)
            }

            @JvmStatic
            @AfterInvocation
            fun afterInvocation(
                callback: XposedInterface.AfterHookCallback,
                context: DefaultHooker
            ) {
                module.log("Default afterInvocation")
            }
        }
    }

}