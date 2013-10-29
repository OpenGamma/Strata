/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.util.Map;

/**
 *
 */
public interface Invoker {

  Object invoke(Map<String, Object> args);
}
