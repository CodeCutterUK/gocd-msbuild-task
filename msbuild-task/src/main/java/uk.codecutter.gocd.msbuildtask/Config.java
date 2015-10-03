package uk.codecutter.gocd.msbuildtask;

import java.util.Map;

public class Config {
    private final String arguments;
    private final String command;

    public Config(Map config) {
        arguments = getValue(config, MSBuildTask.ARGUMENTS_PROPERTY);
        command = getValue(config, MSBuildTask.COMMAND_PROPERTY);
    }

    private String getValue(Map config, String property) {
        return (String) ((Map) config.get(property)).get("value");
    }

    public String getArguments() {
        return arguments;
    }

    public String getCommand() {
        return command;
    }
}