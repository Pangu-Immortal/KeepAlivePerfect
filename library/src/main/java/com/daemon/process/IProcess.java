package com.daemon.process;

import android.content.Context;
import android.os.Build;

public interface IProcess {
	/**
	 * Initialization some files or other when 1st time 
	 */
	boolean onInit(Context context);

	/**
	 * when Persistent processName create
	 * 
	 */
	void onPersistentCreate(Context context, Configs configs);

	/**
	 * when DaemonAssistant processName create
	 */
	void onDaemonAssistantCreate(Context context, Configs configs);

	/**
	 * when watches the processName dead which it watched
	 */
	void onDaemonDead();

	
	class Fetcher {

		private static volatile IProcess mDaemonStrategy;

		/**
		 * fetch the strategy for this device
		 * 
		 * @return the daemon strategy for this device
		 */
		static IProcess fetchStrategy() {
			if (mDaemonStrategy != null) {
				return mDaemonStrategy;
			}
			int sdk = Build.VERSION.SDK_INT;
			mDaemonStrategy = new ProcessImpl();
			return mDaemonStrategy;
		}
	}
}
