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
public class CompositeFunctionConfig implements FunctionConfig {

  private final FunctionConfig _config1;
  private final FunctionConfig _config2;

  public CompositeFunctionConfig(FunctionConfig config1, FunctionConfig config2) {
    _config1 = ArgumentChecker.notNull(config1, "config1");
    _config2 = ArgumentChecker.notNull(config2, "config2");
  }

  @Override
  public Class<?> getFunctionImplementation(Class<?> functionInterface) {
    Class<?> impl = _config1.getFunctionImplementation(functionInterface);
    if (impl != null) {
      return impl;
    } else {
      return _config2.getFunctionImplementation(functionInterface);
    }
  }

  @Override
  public FunctionArguments getFunctionArguments(Class<?> functionType) {
    return new CompositeFunctionArguments(_config1.getFunctionArguments(functionType),
                                          _config2.getFunctionArguments(functionType));
  }

  public static FunctionConfig compose(FunctionConfig... configs) {
    if (configs.length == 0) {
      return EMPTY;
    }
    if (configs.length == 1) {
      return configs[0];
    }
    FunctionConfig config = configs[0];
    for (int i = 1; i < configs.length; i++) {
      config = new CompositeFunctionConfig(config, configs[i]);
    }
    return config;
  }
}
