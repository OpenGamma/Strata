/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.core.position.PositionOrTrade;

/**
 * A function that takes a {@link PositionOrTrade} and returns null.
 */
public class NoOutputFunction {

  public static final String NO_OUTPUT = "No Output";
  public static final FunctionMetadata METADATA;
  
  static {
    try {
      Method doNothing = NoOutputFunction.class.getMethod("doNothing", PositionOrTrade.class);
      Constructor<NoOutputFunction> constructor = NoOutputFunction.class.getConstructor();
      METADATA = new FunctionMetadata(doNothing, constructor);
    } catch (NoSuchMethodException e) {
      // won't happen but need to throw the exception to convince the compiler
      throw new OpenGammaRuntimeException("Unexpected problem", e);
    }
  }
  
  @Output(NO_OUTPUT)
  public Object doNothing(PositionOrTrade ignored) {
    return null;
  }
}
