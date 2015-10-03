package uk.codecutter.gocd.msbuildtask;

import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.api.task.*;
import org.apache.commons.io.IOUtils;
import com.google.gson.GsonBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Extension
public class MSBuildTask implements GoPlugin {

    public static final String ARGUMENTS_PROPERTY = "Arguments";
    public static final String COMMAND_PROPERTY = "Command";

    private Logger logger = Logger.getLoggerFor(MSBuildTask.class);

    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {
    }

    @Override
    public GoPluginApiResponse handle(GoPluginApiRequest request) throws UnhandledRequestTypeException {
        if ("configuration".equals(request.requestName())) {
            return handleGetConfigRequest();
        } else if ("validate".equals(request.requestName())) {
            return handleValidation(request);
        } else if ("execute".equals(request.requestName())) {
            return handleTaskExecution(request);
        } else if ("view".equals(request.requestName())) {
            return handleTaskView();
        }
        throw new UnhandledRequestTypeException(request.requestName());
    }

    private GoPluginApiResponse handleTaskView() {
        int responseCode = DefaultGoApiResponse.SUCCESS_RESPONSE_CODE;
        HashMap view = new HashMap();
        view.put("displayValue", "MS Build");
        try {
            view.put("template", IOUtils.toString(getClass().getResourceAsStream("/views/task.template.html"), "UTF-8"));
        } catch (Exception e) {
            responseCode = DefaultGoApiResponse.INTERNAL_ERROR;
            String errorMessage = "Failed to find template: " + e.getMessage();
            view.put("exception", errorMessage);
            logger.error(errorMessage, e);
        }
        return createResponse(responseCode, view);
    }

    private GoPluginApiResponse handleTaskExecution(GoPluginApiRequest request) {
        MSBuildTaskExecutor executor = new MSBuildTaskExecutor();
        Map executionRequest = (Map) new GsonBuilder().create().fromJson(request.requestBody(), Object.class);
        Map config = (Map) executionRequest.get("config");
        Map context = (Map) executionRequest.get("context");

        Result result = executor.execute(new Config(config), new Context(context), JobConsoleLogger.getConsoleLogger());
        return createResponse(result.responseCode(), result.toMap());
    }

    private GoPluginApiResponse handleValidation(GoPluginApiRequest request) {
        HashMap validationResult = new HashMap();
        int responseCode = DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE;
        Map configMap = (Map) new GsonBuilder().create().fromJson(request.requestBody(), Object.class);
        HashMap errorMap = new HashMap();
        if (!configMap.containsKey(COMMAND_PROPERTY) || ((Map) configMap.get(COMMAND_PROPERTY)).get("value") == null || ((String) ((Map) configMap.get(COMMAND_PROPERTY)).get("value")).trim().isEmpty()) {
            errorMap.put(COMMAND_PROPERTY, "URL cannot be empty");
        }
        validationResult.put("errors", errorMap);
        return createResponse(responseCode, validationResult);
    }

    private GoPluginApiResponse handleGetConfigRequest() {
        HashMap config = new HashMap();

        HashMap command = new HashMap();
        command.put("display-order", "0");
        command.put("display-name", "Command");
        command.put("required", true);
        config.put(COMMAND_PROPERTY, command);

        HashMap arguments = new HashMap();
        arguments.put("display-order", "1");
        arguments.put("display-name", "Arguments");
        arguments.put("required", false);
        config.put(ARGUMENTS_PROPERTY, arguments);

        return createResponse(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, config);
    }

    private GoPluginApiResponse createResponse(int responseCode, Map body) {
        final DefaultGoPluginApiResponse response = new DefaultGoPluginApiResponse(responseCode);
        response.setResponseBody(new GsonBuilder().serializeNulls().create().toJson(body));
        return response;
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return new GoPluginIdentifier("task", Arrays.asList("1.0"));
    }
}
