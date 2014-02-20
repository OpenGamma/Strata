/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import com.opengamma.util.ArgumentChecker;

/**
 * Composite function arguments implementation.
 * <p>
 * This is used by {@link CompositeFunctionModelConfig}.
 */
/* package */ class CompositeFunctionArguments implements FunctionArguments {

  /**
   * The first arguments instance.
   */
  private final FunctionArguments _args1;
  /**
   * The second arguments instance.
   */
  private final FunctionArguments _args2;

  /**
   * Creates an instance.
   * 
   * @param args1  the first arguments, not null
   * @param args2  the second arguments, not null
   */
  /* package */ CompositeFunctionArguments(FunctionArguments args1, FunctionArguments args2) {
    _args1 = ArgumentChecker.notNull(args1, "args1");
    _args2 = ArgumentChecker.notNull(args2, "args2");
  }

  //-------------------------------------------------------------------------
  @Override
  public Object getArgument(String parameterName) {
    Object arg = _args1.getArgument(parameterName);
    if (arg != null) {
      return arg;
    }
    return _args2.getArgument(parameterName);
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return "CompositeFunctionArguments [" + _args1 + ", " + _args2 + "]";
  }

}
