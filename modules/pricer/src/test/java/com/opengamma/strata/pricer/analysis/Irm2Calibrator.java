/*
 * Copyright (C) 2023 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.analysis;

import java.util.Map;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Calibrator associated to the historical VaR-like IM computation.
 */
public class Irm2Calibrator {
  
  // Historical scenarios in relative terms for each underlying
  // TODO implied volatility and scenarios?
  
  public static Map<StandardId, DoubleArray> calibrate(){
    return null;
  }

}
