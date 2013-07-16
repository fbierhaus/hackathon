package com.verizon.mms;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import com.strumsoft.android.commons.logger.Logger;

public class MmsDebug {

	public static void enableStrictMode() {
		try {
			final String penalty = Logger.STRICT_MODE_FATAL ? "penaltyDeath" : "penaltyLog";

			Class<?> builderClass = Class.forName("android.os.StrictMode$ThreadPolicy$Builder");
			Constructor<?> constructor = builderClass.getConstructor();
			Object builder = constructor.newInstance();

			invoke(builderClass, builder, "detectNetwork");
			invoke(builderClass, builder, "detectCustomSlowCalls");
			invoke(builderClass, builder, penalty);
			Object policy = invoke(builderClass, builder, "build");

			final Class<?> strictMode = Class.forName("android.os.StrictMode");
			Class<?> policyClass = Class.forName("android.os.StrictMode$ThreadPolicy");
			Method meth = strictMode.getMethod("setThreadPolicy", policyClass);
			meth.invoke(null, policy);


			builderClass = Class.forName("android.os.StrictMode$VmPolicy$Builder");
			constructor = builderClass.getConstructor();
			builder = constructor.newInstance();

			invoke(builderClass, builder, "detectLeakedClosableObjects");
			invoke(builderClass, builder, "detectLeakedSqlLiteObjects");
			invoke(builderClass, builder, penalty);
			policy = invoke(builderClass, builder, "build");

			policyClass = Class.forName("android.os.StrictMode$VmPolicy");
			meth = strictMode.getMethod("setVmPolicy", policyClass);
			meth.invoke(null, policy);
		}
		catch (Exception e) {
			Logger.error(e);
		}
	}

	private static Object invoke(Class<?> cls, Object obj, String method) {
		try {
			Method meth = cls.getMethod(method);
			return meth.invoke(obj);
		}
		catch (Exception e) {
			Logger.error(e);
			final Method[] meths = cls.getDeclaredMethods();
			StringBuilder sb = new StringBuilder("Declared methods in " + cls.getSimpleName() + ":");
			for (final Method emeth : meths) {
				sb.append("\n");
				sb.append(emeth.toString());
			}
			if(Logger.IS_DEBUG_ENABLED){
			Logger.debug(sb.toString());
			}
		}
		return null;
	}
}
