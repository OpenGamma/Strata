/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.opengamma.util.ArgumentChecker;

/**
 * Composite function arguments implementation.
 * <p>
 * This is used by {@link CompositeFunctionModelConfig}.
 */
public class CompositeFunctionArguments implements FunctionArguments {

  private final List<FunctionArguments> _args;

  private CompositeFunctionArguments(List<FunctionArguments> args) {
    _args = args;
  }

  //-------------------------------------------------------------------------
  @Override
  public Object getArgument(String parameterName) {
    for (FunctionArguments arg : _args) {
      Object argVal = arg.getArgument(parameterName);

      if (argVal != null) {
        return argVal;
      }
    }
    return null;
  }

  //-------------------------------------------------------------------------

  @Override
  public String toString() {
    return "CompositeFunctionArguments [_args=" + _args + "]";
  }

  //-------------------------------------------------------------------------

  public static FunctionArguments compose(FunctionArguments args1, FunctionArguments args2, FunctionArguments... args) {
    ArgumentChecker.notNull(args1, "args1");
    ArgumentChecker.notNull(args2, "args2");
    List<FunctionArguments> argList = new ArrayList<>(2 + args.length);
    argList.add(args1);
    argList.add(args2);
    Collections.addAll(argList, args);
    return new CompositeFunctionArguments(argList);
  }
}
