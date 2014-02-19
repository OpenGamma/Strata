/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import com.opengamma.sesame.OutputName;
import com.opengamma.sesame.config.FunctionArguments;

/**
 * TODO return type
 */
public interface InvokableFunction {

  Object invoke(Object input, FunctionArguments args);

  OutputName getOutputName();

  Object getReceiver();

}
