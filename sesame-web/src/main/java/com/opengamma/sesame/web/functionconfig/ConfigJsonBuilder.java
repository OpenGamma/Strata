/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.web.functionconfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.opengamma.core.config.impl.ConfigItem;
import com.opengamma.core.link.ConfigLink;
import com.opengamma.master.config.ConfigMaster;
import com.opengamma.master.config.ConfigSearchRequest;
import com.opengamma.master.config.ConfigSearchResult;
import com.opengamma.sesame.OutputName;
import com.opengamma.sesame.config.EngineUtils;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.config.SimpleFunctionArguments;
import com.opengamma.sesame.config.SimpleFunctionModelConfig;
import com.opengamma.sesame.function.AvailableImplementations;
import com.opengamma.sesame.function.AvailableOutputs;
import com.opengamma.sesame.function.Parameter;
import com.opengamma.sesame.function.ParameterType;
import com.opengamma.sesame.graph.ArgumentConversionErrorNode;
import com.opengamma.sesame.graph.ArgumentNode;
import com.opengamma.sesame.graph.CannotBuildNode;
import com.opengamma.sesame.graph.ClassNode;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.sesame.graph.FunctionModelNode;
import com.opengamma.sesame.graph.InterfaceNode;
import com.opengamma.sesame.graph.MissingArgumentNode;
import com.opengamma.sesame.graph.MissingConfigNode;
import com.opengamma.sesame.graph.NoImplementationNode;
import com.opengamma.sesame.graph.convert.ArgumentConverter;
import com.opengamma.util.ArgumentChecker;

/**
 * Builds maps representing the JSON used in the function configuration web app.
 */
public class ConfigJsonBuilder {

  private static final String FUNC = "func";
  private static final String IMPL = "impl";
  private static final String IMPLS = "impls";
  private static final String ARGS = "args";
  private static final String NAME = "name";
  private static final String VALUE = "value";
  private static final String ERROR = "error";
  private static final String TYPE = "type";
  private static final String COL_NAME = "colName";
  private static final String INPUT_TYPES = "inputTypes";
  private static final String INPUT_TYPE = "inputType";
  private static final String OUTPUT_NAMES = "outputNames";
  private static final String OUTPUT_NAME = "outputName";
  private static final String FUNCTIONS = "functions";
  private static final String CONFIGS = "configs";

  private final AvailableOutputs _availableOutputs;
  private final AvailableImplementations _availableImplementations;
  private final ConfigMaster _configMaster;
  private final ArgumentConverter _argumentConverter;

  /**
   * @param availableOutputs the functions known to the engine that can calculate output values
   * @param availableImplementations the function implementations known to the engine
   * @param configMaster for looking up configuration
   * @param argumentConverter converts arguments to and from strings
   */
  ConfigJsonBuilder(AvailableOutputs availableOutputs,
                    AvailableImplementations availableImplementations,
                    ConfigMaster configMaster,
                    ArgumentConverter argumentConverter) {
    _argumentConverter = ArgumentChecker.notNull(argumentConverter, "argumentConverter");
    _configMaster = ArgumentChecker.notNull(configMaster, "configMaster");
    _availableOutputs = ArgumentChecker.notNull(availableOutputs, "availableOutputs");
    _availableImplementations = ArgumentChecker.notNull(availableImplementations, "availableImplementations");
  }

  /**
   * Builds a configuration object from JSON produced by the client.
   * The expected format of the JSON is:
   *
   * <pre>
   *   {
   *     impls: {interface1: impl1, interface2: impl2, ... },
   *     args: {
   *         impl1: {
   *         propertyName1: arg1,
   *         propertyName2: arg2,
   *         ...
   *       },
   *       impl2: {
   *         propertyName3: arg3,
   *         ...
   *       },
   *       ...
   *     }
   *   }
   * </pre>
   *
   * @param json JSON representing function configuration
   * @return the configuration as an object
   * @throws IllegalArgumentException if the JSON doesn't define valid configuration
   */
  @SuppressWarnings("unchecked")
  public SimpleFunctionModelConfig getConfigFromJson(Map<String, Object> json) {
    Map<String, String> implsJson = (Map<String, String>) json.get(IMPLS);
    Map<Class<?>, Class<?>> impls = new HashMap<>();

    for (Map.Entry<String, String> entry : implsJson.entrySet()) {
      String fnType = entry.getKey();
      String implType = entry.getValue();
      if (!StringUtils.isEmpty(fnType) && !StringUtils.isEmpty(implType)) {
        try {
          impls.put(Class.forName(fnType), Class.forName(implType));
        } catch (ClassNotFoundException e) {
          throw new IllegalArgumentException(e);
        }
      }
    }
    Map<String, Map<String, String>> argsJson = (Map<String, Map<String, String>>) json.get(ARGS);
    Map<Class<?>, SimpleFunctionArguments> args = new HashMap<>();

    for (Map.Entry<String, Map<String, String>> entry : argsJson.entrySet()) {
      String fnTypeName = entry.getKey();
      Class<?> fnType;
      try {
        fnType = Class.forName(fnTypeName);
      } catch (ClassNotFoundException e) {
        throw new IllegalArgumentException(e);
      }
      Map<String, String> fnArgStrs = entry.getValue();
      Map<String, Object> fnArgs = new HashMap<>();

      for (Map.Entry<String, String> fnArgEntry : fnArgStrs.entrySet()) {
        String paramName = fnArgEntry.getKey();
        String paramValueStr = fnArgEntry.getValue();

        if (StringUtils.isEmpty(paramValueStr)) {
          continue;
        }
        Parameter parameter = Parameter.named(paramName, fnType);

        if (EngineUtils.isConfig(parameter.getType())) {
          fnArgs.put(paramName, ConfigLink.resolvable(paramValueStr, parameter.getType()));
        } else if (_argumentConverter.isConvertible(parameter.getParameterType())) {
          fnArgs.put(paramName, _argumentConverter.convertFromString(parameter.getParameterType(), paramValueStr));
        } else {
          throw new IllegalArgumentException("Cannot convert from string to parameter type " + parameter.getParameterType());
        }
      }
      SimpleFunctionArguments simpleFunctionArguments = new SimpleFunctionArguments(fnArgs);
      args.put(fnType, simpleFunctionArguments);
    }
    return new SimpleFunctionModelConfig(impls, args);
  }

  /**
   * Converts a configuration instance to a map representing some JSON.
   * The format of the JSON is:
   *
   * <pre>
   *   {
   *     impls: {interface1: impl1, interface2: impl2, ... },
   *     args: {
   *         impl1: {
   *         propertyName1: arg1,
   *         propertyName2: arg2,
   *         ...
   *       },
   *       impl2: {
   *         propertyName3: arg3,
   *         ...
   *       },
   *       ...
   *     }
   *   }
   * </pre>
   *
   * @param config some configuration
   * @return the configuration as JSON
   */
  public Map<String, Object> getJsonFromConfig(SimpleFunctionModelConfig config) {
    Map<String, Object> jsonMap = new HashMap<>();
    Map<String, Object> implsMap = new HashMap<>();

    for (Map.Entry<Class<?>, Class<?>> entry : config.getImplementations().entrySet()) {
      implsMap.put(entry.getKey().getName(), entry.getValue().getName());
    }
    jsonMap.put(IMPLS, implsMap);

    Map<String, Object> argsMap = new HashMap<>();

    for (Map.Entry<Class<?>, SimpleFunctionArguments> entry : config.getArguments().entrySet()) {
      Map<String, String> fnArgsMap = new HashMap<>();
      Class<?> functionType = entry.getKey();
      SimpleFunctionArguments fnArgs = entry.getValue();

      for (Map.Entry<String, Object> argEntry : fnArgs.getArguments().entrySet()) {
        String parameterName = argEntry.getKey();
        Object argument = argEntry.getValue();
        String argumentStr;
        Parameter parameter = Parameter.named(parameterName, functionType);
        ParameterType parameterType = parameter.getParameterType();

        if (_argumentConverter.isConvertible(parameterType)) {
          argumentStr = _argumentConverter.convertToString(parameterType, argument);
        } else {
          argumentStr = argument.toString();
        }
        fnArgsMap.put(parameterName, argumentStr);
      }
      String typeName = functionType.getName();
      argsMap.put(typeName, fnArgsMap);
    }
    jsonMap.put(ARGS, argsMap);
    return jsonMap;
  }

  /**
   * Returns JSON containing the model for the function configuration page.
   * This contains the configuration for a single output associated with a column.
   *
   * @param columnName the name of the column containing the output
   * @param config the configuration
   * @param inputType the input type for the top level function
   * @param outputName the name of the output calculated by the function
   * @param model the function model of a function that can calculate the named output for the specified input
   *   type, built using the configuration
   * @return the page model for displaying and editing the configuration
   */
  public Map<String, Object> getConfigPageModel(String columnName,
                                                SimpleFunctionModelConfig config,
                                                @Nullable Class<?> inputType,
                                                @Nullable OutputName outputName,
                                                @Nullable FunctionModel model) {
    ArgumentChecker.notEmpty(columnName, "columnName");
    List<Map<String, Object>> inputTypeList = new ArrayList<>();

    // TODO if we're editing an existing config this should either be empty or only contain the selected type
    // the user shouldn't be able to change the input type for an existing config, that's part of the key
    // TODO if we're adding a new config the types shouldn't include the types for which the column already has config
    for (Class<?> type : _availableOutputs.getInputTypes()) {
      inputTypeList.add(typeMap(type));
    }
    Collections.sort(inputTypeList, TypeMapComparator.INSTANCE);

    Map<String, Object> jsonMap = new HashMap<>();
    jsonMap.put(COL_NAME, columnName);
    jsonMap.put(INPUT_TYPES, inputTypeList);

    if (inputType != null) {
      Set<OutputName> availableOutputs = _availableOutputs.getAvailableOutputs(inputType);
      List<String> outputNames = new ArrayList<>(availableOutputs.size());

      for (OutputName availableOutput : availableOutputs) {
        outputNames.add(availableOutput.getName());
      }
      Collections.sort(outputNames);
      // TODO output names needs to be filtered so it only includes names for which no config exists
      // need the existing ViewColumn
      jsonMap.put(INPUT_TYPE, typeMap(inputType));
      jsonMap.put(OUTPUT_NAMES, outputNames);

      if (outputName != null && availableOutputs.contains(outputName)) {
        jsonMap.put(OUTPUT_NAME, outputName.getName());
      }
    }
    List<Map<String, Object>> functions = getFunctions(config, model);

    if (!functions.isEmpty()) {
      jsonMap.put(FUNCTIONS, functions);
    }
    return jsonMap;
  }

  private List<Map<String, Object>> getFunctions(SimpleFunctionModelConfig config, FunctionModel model) {
    List<Map<String, Object>> functions = new ArrayList<>();
    LinkedHashSet<FunctionModelNode> nodes = flattenModel(model);

    for (FunctionModelNode node : nodes) {
      if (node instanceof InterfaceNode) {
        Class<?> functionType = node.getType();
        Class<?> selectedImpl = ((InterfaceNode) node).getImplementationType();

        Map<String, Object> map = new HashMap<>();
        map.put(FUNC, typeMap(functionType));
        map.put(IMPL, typeMap(selectedImpl));
        map.put(IMPLS, getImplementations(functionType));
        map.put(ARGS, getArguments(config, node.getDependencies()));

        functions.add(map);
      } else if (node instanceof ClassNode && hasArguments(node)) {

        Map<String, Object> map = new HashMap<>();
        map.put(IMPL, typeMap(node.getType()));
        map.put(ARGS, getArguments(config, node.getDependencies()));

        functions.add(map);
      } else if (node instanceof NoImplementationNode) {
        NoImplementationNode noImplementationNode = (NoImplementationNode) node;
        Class<?> functionType = noImplementationNode.getException().getInterfaceType();

        Map<String, Object> map = new HashMap<>();
        map.put(FUNC, typeMap(functionType));
        map.put(IMPLS, getImplementations(functionType));

        functions.add(map);
      }
    }
    return functions;
  }

  private List<Map<String, Object>> getImplementations(Class<?> functionType) {
    List<Class<?>> impls = new ArrayList<>(_availableImplementations.getImplementationTypes(functionType));
    List<Map<String, Object>> implsList = new ArrayList<>(impls.size());
    for (Class<?> impl : impls) {
      implsList.add(typeMap(impl));
    }
    Collections.sort(implsList, TypeMapComparator.INSTANCE);
    return implsList;
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> getArguments(FunctionModelConfig config, List<FunctionModelNode> dependencies) {
    List<Map<String, Object>> args = new ArrayList<>();

    for (FunctionModelNode node : dependencies) {
      Parameter parameter = node.getParameter();
      String paramName = parameter.getName();
      String value;
      String errorMessage;
      List<String> configNames = new ArrayList<>();

      Map<String, Object> map = new HashMap<>();
      map.put(NAME, paramName);
      map.put(TYPE, parameter.getParameterType().getName());

      if (EngineUtils.isConfig(parameter.getType())) {
        Class<?> parameterType = parameter.getType();
        ConfigSearchRequest<?> searchRequest = new ConfigSearchRequest<>();
        searchRequest.setType(parameterType);
        ConfigSearchResult<?> searchResult = _configMaster.search(searchRequest);
        List<? extends ConfigItem<?>> configItems = searchResult.getValues();

        for (ConfigItem<?> configItem : configItems) {
          configNames.add(configItem.getName());
        }
        map.put(CONFIGS, configNames);
      }

      if (node instanceof ArgumentNode) {
        Object argument = config.getFunctionArguments(parameter.getDeclaringClass()).getArgument(paramName);

        if (argument == null) {
          value = null;
        } else if (argument instanceof ConfigLink<?>) {
          value = "TODO - need to expose config link name PLAT-6469";
        } else if (_argumentConverter.isConvertible(parameter.getParameterType())) {
          value = _argumentConverter.convertToString(parameter.getParameterType(), argument);
        } else {
          value = argument.toString();
        }
        errorMessage = null;
      } else if (node instanceof MissingConfigNode) {
        value = null;
        if (!configNames.isEmpty()) {
          errorMessage = "Configuration required";
        } else {
          errorMessage = "No configuration available";
        }
      } else if (node instanceof MissingArgumentNode) {
        value = null;
        errorMessage = "Value required";
      } else if (node instanceof CannotBuildNode) {
        value = null;
        errorMessage = "Unable to create value";
      } else if (node instanceof ArgumentConversionErrorNode) {
        ArgumentConversionErrorNode conversionErrorNode = (ArgumentConversionErrorNode) node;
        value = conversionErrorNode.getValue();
        errorMessage = conversionErrorNode.getErrorMessage();
      } else {
        continue;
      }
      if (value != null) {
        map.put(VALUE, value);
      }
      if (errorMessage != null) {
        map.put(ERROR, errorMessage);
      }
      args.add(map);
    }
    return args;
  }

  /**
   * Returns <code>{name: "type name", type: "fully qualified class name"}</code>
   *
   * @param type the type
   * @return a map containing the type's name and fully qualified class name
   */
  private static Map<String, Object> typeMap(Class<?> type) {
    return ImmutableMap.<String, Object>of(NAME, getName(type), TYPE, type.getName());
  }

  /**
   * Returns the name for an input type.
   * Currently uses the class simple name, but in future could use a value from an annotations. See SSM-224.
   *
   * @param inputType a type that is the input to a calculation in the engine
   * @return the name used for the type in the user interface
   */
  static String getName(Class<?> inputType) {
    // TODO use annotation if available
    return inputType.getSimpleName();
  }

  private static boolean hasArguments(FunctionModelNode node) {
    for (FunctionModelNode childNode : node.getDependencies()) {
      if (childNode instanceof ArgumentNode ||
          childNode instanceof MissingArgumentNode ||
          childNode instanceof MissingConfigNode) {
        return true;
      }
    }
    return false;
  }

  private static LinkedHashSet<FunctionModelNode> flattenModel(@Nullable FunctionModel model) {
    if (model == null) {
      return new LinkedHashSet<>();
    }
    LinkedHashSet<FunctionModelNode> nodes = new LinkedHashSet<>();
    flattenNode(model.getRoot(), nodes);
    return nodes;
  }

  private static void flattenNode(FunctionModelNode node, Set<FunctionModelNode> accumulator) {
    accumulator.add(node);

    for (FunctionModelNode childNode : node.getDependencies()) {
      flattenNode(childNode, accumulator);
    }
  }

  private static class TypeMapComparator implements Comparator<Map<String, Object>> {

    private static final Comparator<Map<String, Object>> INSTANCE = new TypeMapComparator();

    @Override
    public int compare(Map<String, Object> o1, Map<String, Object> o2) {
      String name1 = (String) o1.get(NAME);
      String name2 = (String) o2.get(NAME);
      return name1.compareTo(name2);
    }
  }
}
