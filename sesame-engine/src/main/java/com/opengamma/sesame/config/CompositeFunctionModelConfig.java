/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import com.opengamma.util.ArgumentChecker;

/**
 *
 */
public class CompositeFunctionModelConfig implements FunctionModelConfig {

  private final FunctionModelConfig _config1;
  private final FunctionModelConfig _config2;

  public CompositeFunctionModelConfig(FunctionModelConfig config1, FunctionModelConfig config2) {
    _config1 = ArgumentChecker.notNull(config1, "config1");
    _config2 = ArgumentChecker.notNull(config2, "config2");
  }

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
  public FunctionArguments getFunctionArguments(Class<?> functionType) {
    return new CompositeFunctionArguments(_config1.getFunctionArguments(functionType),
                                          _config2.getFunctionArguments(functionType));
  }

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
}
