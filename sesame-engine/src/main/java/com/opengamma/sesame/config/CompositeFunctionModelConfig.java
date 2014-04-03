/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import com.opengamma.sesame.function.Parameter;
import com.opengamma.util.ArgumentChecker;

/**
 * Composite function configuration implementation.
 */
public class CompositeFunctionModelConfig implements FunctionModelConfig {

  /**
   * The first configuration.
   */
  private final FunctionModelConfig _config1;
  /**
   * The second configuration.
   */
  private final FunctionModelConfig _config2;

  //-------------------------------------------------------------------------
  /**
   * Composes a list of configuration.
   * 
   * @param configs  the array of configuration, not null
   * @return the composite configuration, not null
   */
  public static FunctionModelConfig compose(FunctionModelConfig... configs) {
    if (configs.length == 0) {
      return EMPTY;
    }
    if (configs.length == 1) {
      return configs[0];
    }
    FunctionModelConfig config = configs[0];
    for (int i = 1; i < configs.length; i++) {
      config = new CompositeFunctionModelConfig(config, configs[i]);
    }
    return config;
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * 
   * @param config1  the first configuration, not null
   * @param config2  the second configuration, not null
   */
  public CompositeFunctionModelConfig(FunctionModelConfig config1, FunctionModelConfig config2) {
    _config1 = ArgumentChecker.notNull(config1, "config1");
    _config2 = ArgumentChecker.notNull(config2, "config2");
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<?> getFunctionImplementation(Class<?> functionType) {
    Class<?> impl = _config1.getFunctionImplementation(functionType);

    if (impl != null) {
      return impl;
    } else {
      return _config2.getFunctionImplementation(functionType);
    }
  }

  @Override
  public Class<?> getFunctionImplementation(Parameter parameter) {
    Class<?> impl = _config1.getFunctionImplementation(parameter);

    if (impl == DecoratorConfig.UnknownImplementation.class) {
      return _config2.getFunctionImplementation(parameter.getType());
    }
    if (impl != null) {
      return impl;
    }
    return _config2.getFunctionImplementation(parameter);
  }

  @Override
  public FunctionArguments getFunctionArguments(Class<?> functionType) {
    return CompositeFunctionArguments.compose(_config1.getFunctionArguments(functionType),
                                              _config2.getFunctionArguments(functionType));
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return "CompositeFunctionModelConfig [" + _config1 + ", " + _config2 + "]";
  }

}
