/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.util.Map;

/**
 * TODO implementation that adapts function taking a security target into one which takes a position or trade
 */
public interface InvokableFunction {

  Object invoke(Object input, Map<String, Object> args);

  String getOutputName();

  Object getReceiver();
}
