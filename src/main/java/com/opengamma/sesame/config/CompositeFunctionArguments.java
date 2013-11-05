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
/* package */ class CompositeFunctionArguments implements FunctionArguments {

  private final FunctionArguments _args1;
  private final FunctionArguments _args2;

  /* package */ CompositeFunctionArguments(FunctionArguments args1, FunctionArguments args2) {
    _args1 = ArgumentChecker.notNull(args1, "args1");
    _args2 = ArgumentChecker.notNull(args2, "args2");
  }

  @Override
  public Object getArgument(String parameterName) {
    Object arg = _args1.getArgument(parameterName);
    if (arg != null) {
      return arg;
    } else {
      return _args2.getArgument(parameterName);
    }
  }
}
