package uk.codecutter.gocd.msbuildtask;

import com.thoughtworks.go.plugin.api.task.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MSBuildTaskExecutor {

    public Result execute(Config config, Context context, JobConsoleLogger console) {
        try {
            return runCommand(context, config, console);
        } catch (Exception e) {
            return new Result(false, "Failed to execute command: " + config.getCommand(), e);
        }
    }

    private Result runCommand(Context taskContext, Config taskConfig, JobConsoleLogger console) throws IOException, InterruptedException {
        ProcessBuilder commandProcess = createMsbuildCommandWithOptions(taskContext, taskConfig);
        console.printLine("Launching command: " + commandProcess.command());
        commandProcess.environment().putAll(taskContext.getEnvironmentVariables());
        //console.printEnvironment(commandProcess.environment());

        Process curlProcess = commandProcess.start();
        console.readErrorOf(curlProcess.getErrorStream());
        console.readOutputOf(curlProcess.getInputStream());

        int exitCode = curlProcess.waitFor();
        curlProcess.destroy();

        if (exitCode != 0) {
            return new Result(false, "Failed downloading file. Please check the output");
        }

        return new Result(true, "Command executed");
    }

    ProcessBuilder createMsbuildCommandWithOptions(Context taskContext, Config taskConfig) {
        List<String> command = new ArrayList<String>();
        command.add(taskConfig.getCommand());

        String[] arguments = taskConfig.getArguments().split("\n");
        for (int i = 0; i < arguments.length; i++) {
            command.add(arguments[i]);
        }

        return new ProcessBuilder(command);
    }
}