/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import com.opengamma.sesame.OutputName;
import com.opengamma.sesame.config.FunctionArguments;

/**
 * Wraps the root function in a tree of functions so it can be called by the engine.
 */
public interface InvokableFunction {

  /**
   * Invokes the function.
   * @param input the function's input, e.g. trade or security, possibly null
   * @param args contains any other function arguments
   * @return the function's return value
   */
  Object invoke(Object input, FunctionArguments args);

  /**
   * @return the name of the output produced by the function
   * TODO this isn't used. does it need to exist any more? replace with a method returning a function key?
   */
  OutputName getOutputName();

  /**
   * @return the underlying object that implements the function
   */
  Object getReceiver();

}
