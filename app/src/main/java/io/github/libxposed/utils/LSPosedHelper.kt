package io.github.libxposed.utils

import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.libxposed.api.errors.HookFailedError
import java.lang.reflect.Constructor
import java.lang.reflect.Method

abstract class KreModule(base: XposedInterface, param: XposedModuleInterface.ModuleLoadedParam) :
    XposedModule(base, param) {
    fun hookMethod(
        hooker: Class<out XposedInterface.Hooker>,
        clazz: Class<*>,
        methodName: String,
        vararg args: Class<*>
    ): XposedInterface.MethodUnhooker<Method> {
        try {
            val method: Method = clazz.getDeclaredMethod(methodName, *args)
            return hook(method, hooker)
        } catch (e: NoSuchMethodException) {
            throw HookFailedError(e)
        }
    }

    fun hookAllMethods(
        hooker: Class<out XposedInterface.Hooker>,
        clazz: Class<*>,
        methodName: String
    ): Set<XposedInterface.MethodUnhooker<Method>> {
        val hashSet = HashSet<XposedInterface.MethodUnhooker<Method>>()
        try {
            clazz.declaredMethods.forEach {
                if (it.name == methodName) {
                    hashSet.add(hook(it, hooker))
                }
            }
        } catch (e: NoSuchMethodException) {
            throw HookFailedError(e)
        }
        return hashSet
    }

    fun hookConstructor(
        hooker: Class<out XposedInterface.Hooker>,
        clazz: Class<*>,
        vararg args: Class<*>
    ): XposedInterface.MethodUnhooker<out Constructor<*>> {
        try {
            val method: Constructor<out Any> = clazz.getDeclaredConstructor(*args)
            return hook(method, hooker)
        } catch (e: NoSuchMethodException) {
            throw HookFailedError(e)
        }
    }

    fun hookAllConstructors(
        hooker: Class<out XposedInterface.Hooker>,
        clazz: Class<*>
    ): Set<XposedInterface.MethodUnhooker<out Constructor<*>>> {
        val hashSet = HashSet<XposedInterface.MethodUnhooker<out Constructor<*>>>()
        try {
            clazz.declaredConstructors.forEach {
                hashSet.add(hook(it, hooker))
            }
        } catch (e: NoSuchMethodException) {
            throw HookFailedError(e)
        }
        return hashSet
    }
}