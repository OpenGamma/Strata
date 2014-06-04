/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.web.functionconfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.opengamma.DataDuplicationException;
import com.opengamma.core.config.impl.ConfigItem;
import com.opengamma.id.UniqueId;
import com.opengamma.master.config.ConfigDocument;
import com.opengamma.master.config.ConfigMaster;
import com.opengamma.master.config.ConfigSearchRequest;
import com.opengamma.master.config.ConfigSearchResult;
import com.opengamma.sesame.OutputName;
import com.opengamma.sesame.config.SimpleFunctionModelConfig;
import com.opengamma.sesame.config.ViewColumn;
import com.opengamma.sesame.config.ViewOutput;
import com.opengamma.sesame.function.AvailableImplementations;
import com.opengamma.sesame.function.AvailableOutputs;
import com.opengamma.sesame.function.DefaultImplementationProvider;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.sesame.graph.NodeDecorator;
import com.opengamma.sesame.graph.convert.ArgumentConverter;
import com.opengamma.sesame.graph.convert.DefaultArgumentConverter;
import com.opengamma.util.ArgumentChecker;

/**
 * REST resource providing endpoints for the column configuration webapp.
 */
@SuppressWarnings("unchecked")
public class ColumnConfigResource {

  private static final Logger s_logger = LoggerFactory.getLogger(ColumnConfigResource.class);

  private final AvailableOutputs _availableOutputs;
  private final Set<Class<?>> _availableComponents;
  private final ConfigJsonBuilder _jsonBuilder;
  private final SimpleFunctionModelConfig _defaultConfig;
  private final Gson _gson = new Gson();
  private final DefaultImplementationProvider _defaultImpls;
  private final ConfigMaster _configMaster;
  private final ArgumentConverter _argumentConverter;

  /**
   * @param availableOutputs the functions known to the engine that can calculate output values
   * @param availableImplementations the function implementations known to the engine
   * @param availableComponents the types of the components available in the engine
   * @param defaultConfig the default configuration used as the starting point when new configuration is created
   * @param configMaster for looking up configuration
   * @param argumentConverter converts arguments to and from strings
   */
  public ColumnConfigResource(AvailableOutputs availableOutputs,
                              AvailableImplementations availableImplementations,
                              Set<Class<?>> availableComponents,
                              SimpleFunctionModelConfig defaultConfig,
                              ArgumentConverter argumentConverter,
                              ConfigMaster configMaster) {
    _defaultConfig = ArgumentChecker.notNull(defaultConfig, "defaultConfig");
    _argumentConverter = ArgumentChecker.notNull(argumentConverter, "argumentConverter");
    _configMaster = ArgumentChecker.notNull(configMaster, "configMaster");
    _availableOutputs = ArgumentChecker.notNull(availableOutputs, "availableOutputs");
    _availableComponents = ArgumentChecker.notNull(availableComponents, "availableComponents");
    _jsonBuilder = new ConfigJsonBuilder(availableOutputs, availableImplementations, configMaster, new DefaultArgumentConverter());
    _defaultImpls = new DefaultImplementationProvider(availableImplementations);
  }

  /**
   * Returns a map of containing the details of all {@link ViewColumn} instances in the config database.
   * The structure is:
   * <pre>
   *   {columns: [{name: 'column name', id: 'columnUniqueId'}, ...]}
   * </pre>
   *
   * @return a map of containing the details of all {@link ViewColumn} instances in the config database
   */
  public Map<String, Object> getColumnsPageModel() {
    ConfigSearchRequest<ViewColumn> searchRequest = new ConfigSearchRequest<>();
    searchRequest.setType(ViewColumn.class);
    ConfigSearchResult<ViewColumn> searchResult = _configMaster.search(searchRequest);
    List<Map<String, Object>> columns = new ArrayList<>();

    for (ConfigItem<ViewColumn> configItem : searchResult.getValues()) {
      String columnName = configItem.getName();
      UniqueId columnId = configItem.getUniqueId();
      columns.add(ImmutableMap.<String, Object>of("name", columnName, "id", columnId.getObjectId().toString()));
    }
    // don't really need this but strictly speaking a naked array isn't valid JSON
    return ImmutableMap.<String, Object>of("columns", columns);
  }

  /**
   * Returns a map of containing the details of a single {@link ViewColumn} instance from the config database.
   * The structure is:
   * <pre>
   *   {
   *     name: 'column name',
   *     id: 'columnUniqueId',
   *     inputTypes: [{name: 'input type name', type: 'input type class'}, ...]
   *   }
   * </pre>
   *
   * @return a map of containing the details of all {@link ViewColumn} instances in the config database
   */
  public Map<String, Object> getColumnPageModel(UniqueId columnId) {
    ViewColumn column = loadColumn(columnId);
    List<Map<String, Object>> inputTypes = new ArrayList<>(column.getOutputs().size());

    for (Class<?> inputType : column.getOutputs().keySet()) {
      inputTypes.add(ImmutableMap.<String, Object>of("name", ConfigJsonBuilder.getName(inputType), "type", inputType.getName()));
    }
    return ImmutableMap.of("inputTypes", inputTypes, "name", column.getName(), "id", columnId.toString());
  }

  /**
   * Returns a map containing the model for the column configuration page.
   * The structure is:
   * <pre>
   *
   * </pre>
   *
   * @param columnId the ID of the column
   * @param configJsonStr the column config from the client
   * @return the model JSON for the column configuration page
   */
  public Map<String, Object> getConfigPageModel(UniqueId columnId, String configJsonStr) {
    ArgumentChecker.notEmpty(configJsonStr, "configJsonStr");
    ArgumentChecker.notNull(columnId, "columnId");

    ViewColumn column = loadColumn(columnId);
    Class<?> inputType;
    SimpleFunctionModelConfig config;
    OutputName outputName;
    FunctionModel model;

    Map<String, Object> configJson;
    try {
      configJson = _gson.fromJson(configJsonStr, Map.class);
    } catch (JsonSyntaxException e) {
      throw new IllegalArgumentException(e);
    }
    String inputTypeName = (String) configJson.get("inputType");

    if (!StringUtils.isEmpty(inputTypeName)) {
      inputType = loadInputType(inputTypeName);
      String outputNameStr = (String) configJson.get("outputName");
      FunctionMetadata outputFunction;

      if (StringUtils.isEmpty(outputNameStr)) {
        // use the existing output name, possibly null
        outputName = column.getOutputName(inputType);
      } else {
        // use the output name from the client
        outputName = OutputName.of(outputNameStr);
      }
      if (outputName != null) {
        outputFunction = _availableOutputs.getOutputFunction(outputName, inputType);
        config = _jsonBuilder.getConfigFromJson(configJson);
        model = FunctionModel.forFunction(outputFunction, config, _availableComponents, NodeDecorator.IDENTITY, _argumentConverter);
      } else {
        config = SimpleFunctionModelConfig.EMPTY;
        model = null;
      }
    } else {
      inputType = null;
      outputName = null;
      config = null;
      model = null;
    }
    return _jsonBuilder.getConfigPageModel(column.getName(), config, inputType, outputName, model);
  }

  /**
   * Saves configuration for a single output function to the config master.
   * The expected format of the JSON is
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
   * @param columnId unique ID of the column which owns the output
   * @param inputTypeName the fully qualified name of the input type to the function
   * @param body the body of the request containing the configuration as JSON
   * @return unique ID of the updated column
   */
  public UniqueId saveConfig(UniqueId columnId, String inputTypeName, String body) {
    Map<String, Object> inputJson;
    try {
      inputJson = _gson.fromJson(body, Map.class);
    } catch (JsonSyntaxException e) {
      throw new IllegalArgumentException(e);
    }
    SimpleFunctionModelConfig config = _jsonBuilder.getConfigFromJson(inputJson);
    String outputName = (String) inputJson.get("outputName");

    if (outputName == null) {
      throw new IllegalArgumentException("No output name specified");
    }
    ConfigItem<ViewColumn> configItem = loadColumnConfigItem(columnId);
    ViewColumn column = configItem.getValue();
    
    Class<?> inputType = loadInputType(inputTypeName);
    Map<Class<?>, ViewOutput> outputs = column.getOutputs();
    ViewOutput output = new ViewOutput(OutputName.of(outputName), config);
    Map<Class<?>, ViewOutput> newOutputs = new HashMap<>();
    newOutputs.putAll(outputs);
    newOutputs.put(inputType, output);
    ViewColumn newColumn = column.toBuilder().outputs(newOutputs).build();
    ConfigDocument document = new ConfigDocument(ConfigItem.of(newColumn, newColumn.getName()));
    document.setUniqueId(configItem.getUniqueId());
    ConfigDocument newDocument = _configMaster.update(document);
    UniqueId newId = newDocument.getUniqueId();
    s_logger.debug("Saved column with ID {}, {}", newId, newColumn);
    return newId;
  }

  /**
   * Gets the default configuration as a map, corresponds to {@link SimpleFunctionModelConfig}.
   * This is built from system default configuration combined with the default implementations that can be inferred
   * where there is only one known implementation of an interface. The format is
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
   * @return the default configuration as a map
   */
  public Map<String, Object> getDefaultConfig() {
    Map<Class<?>, Class<?>> impls = Maps.newHashMap(_defaultImpls.getDefaultImplementations());
    impls.putAll(_defaultConfig.getImplementations());
    return _jsonBuilder.getJsonFromConfig(new SimpleFunctionModelConfig(impls, _defaultConfig.getArguments()));
  }

  /**
   * Gets the configuration for a column and input type as a map, corresponds to {@link SimpleFunctionModelConfig}.
   * If there is no configuration for the column and type the {@link #getDefaultConfig() default configuration}
   * is returned. The existing configuration is merged with the current defaults with the existing configuration
   * taking priority. The format is
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
   * @param columnId ID of the column
   * @param inputTypeName the fully qualified name of the input type
   * @return the configuration as a map
   */
  public Map<String, Object> getConfig(UniqueId columnId, String inputTypeName) {
    ViewColumn column = loadColumn(columnId);
    Class<?> inputType = loadInputType(inputTypeName);
    SimpleFunctionModelConfig config = column.getFunctionConfig(inputType);

    if (config != null) {
      // compose the implementations from the defaults into the existing config
      Map<Class<?>, Class<?>> impls = Maps.newHashMap(_defaultImpls.getDefaultImplementations());
      impls.putAll(_defaultConfig.getImplementations());
      impls.putAll(config.getImplementations());
      SimpleFunctionModelConfig defaultConfig = new SimpleFunctionModelConfig(impls, _defaultConfig.getArguments());
      return _jsonBuilder.getJsonFromConfig(config.mergeWith(defaultConfig));
    } else {
      return getDefaultConfig();
    }
  }

  /**
   * Adds a new column.
   *
   * @param name the column name, not empty
   * @return the
   */
  public UniqueId addColumn(String name) {
    ArgumentChecker.notEmpty(name, "name");

    ConfigSearchRequest<ViewColumn> searchRequest = new ConfigSearchRequest<>();
    searchRequest.setType(ViewColumn.class);
    searchRequest.setName(name);
    ConfigSearchResult<ViewColumn> searchResult = _configMaster.search(searchRequest);

    if (!searchResult.getValues().isEmpty()) {
      throw new DataDuplicationException("A column already exists with the name '" + name + "'");
    }
    ViewColumn column = new ViewColumn(name, null, Collections.<Class<?>, ViewOutput>emptyMap());
    ConfigDocument document = _configMaster.add(new ConfigDocument(ConfigItem.of(column, name)));
    return document.getUniqueId();
  }

  /**
   * Deletes a column.
   *
   * @param columnId the ID of the column
   */
  public void deleteColumn(UniqueId columnId) {
    _configMaster.remove(columnId);
  }

  /**
   * Deletes the configuration for an output.
   *
   * @param columnId the column containing the configuration
   * @param inputTypeName the input type whose configuration should be deleted
   */
  public void deleteConfig(UniqueId columnId, String inputTypeName) {
    Class<?> inputType = loadInputType(inputTypeName);
    ConfigItem<ViewColumn> configItem = loadColumnConfigItem(columnId);
    ViewColumn column = configItem.getValue();
    Map<Class<?>, ViewOutput> outputs = new HashMap<>(column.getOutputs());
    ViewOutput removed = outputs.remove(inputType);

    if (removed != null) {
      ViewColumn updatedColumn = column.toBuilder().outputs(outputs).build();
      ConfigItem<ViewColumn> updatedItem = ConfigItem.of(updatedColumn, updatedColumn.getName());
      updatedItem.setUniqueId(configItem.getUniqueId());
      _configMaster.update(new ConfigDocument(updatedItem));
    }
  }

  private Class<?> loadInputType(String inputTypeName) {
    try {
      return Class.forName(inputTypeName);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private ViewColumn loadColumn(UniqueId columnId) {
    Object value = _configMaster.get(columnId).getValue().getValue();

    if (!(value instanceof ViewColumn)) {
      throw new IllegalArgumentException("ID " + columnId + " refers to an instance of " + value.getClass().getName());
    }
    return (ViewColumn) value;
  }

  private ConfigItem<ViewColumn> loadColumnConfigItem(UniqueId columnId) {
    ConfigItem<?> configItem = _configMaster.get(columnId).getValue();
    Object value = configItem.getValue();

    if (!(value instanceof ViewColumn)) {
      throw new IllegalArgumentException("ID " + columnId + " refers to an instance of " + value.getClass().getName());
    }
    return (ConfigItem<ViewColumn>) configItem;
  }
}
