/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.analysis;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.math.impl.cern.RandomEngine;

public class DailyUpdateBackTestingAnalysis {

  private static final String SEC_ID_ORIGINAL = "F-XEEE-F7BM-202406";

  @Test
  void daily_forecast() {

  }

  // load file: secId, price, scanning range, delta
  // Compute: return, forecast [rate, vol norm, vol T5, volT6]
  // Remove jumps + stalled + ???
  // Compare: IM v forecast
  // statistics: average, std dev

}
