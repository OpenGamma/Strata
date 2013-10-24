/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.example;

import com.opengamma.sesame.config.DefaultImplementation;
import com.opengamma.sesame.config.EngineFunction;

/**
 *
 */
@EngineFunction("GreetingName")
@DefaultImplementation(Name.class)
public interface NameFunction {

  String getName();
}
