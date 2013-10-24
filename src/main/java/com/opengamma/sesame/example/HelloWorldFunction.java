/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.example;

import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.sesame.config.DefaultImplementation;
import com.opengamma.sesame.config.EngineFunction;
import com.opengamma.sesame.config.Target;

/**
 *
 */
@EngineFunction("HelloWorld")
@DefaultImplementation(HelloWorld.class)
public interface HelloWorldFunction {

  // TODO will this need an annotation so the graph builder knows where to look for the target?
  String getGreeting(@Target EquitySecurity security);
}
