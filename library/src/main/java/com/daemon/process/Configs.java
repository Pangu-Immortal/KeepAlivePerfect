package com.daemon.process;

public class Configs {

    public final Config PERSISTENT_CONFIG;
    public final Config DAEMON_ASSISTANT_CONFIG;

    public Configs(Config persistentConfig, Config daemonAssistantConfig) {
        this.PERSISTENT_CONFIG = persistentConfig;
        this.DAEMON_ASSISTANT_CONFIG = daemonAssistantConfig;
    }

    public static class Config {

        final String processName;
        final String serviceName;
        final String receiverName;
        final String activityName;

        public Config(String processName, String serviceName, String receiverName, String activityName) {
            this.processName = processName;
            this.serviceName = serviceName;
            this.receiverName = receiverName;
            this.activityName = activityName;
        }
    }
}
