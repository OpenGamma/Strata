/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure;

import com.opengamma.strata.calc.Measure;

/**
 * The advanced set of measures which can be calculated by Strata.
 * <p>
 * These measures are rarely needed and should be used with care.
 * <p>
 * Note that not all measures will be available for all targets.
 */
public final class AdvancedMeasures {

  /**
   * Measure representing the semi-parallel bucketed gamma PV01 of the calculation target.
   */
  public static final Measure PV01_SEMI_PARALLEL_GAMMA_BUCKETED =
      Measure.of(StandardMeasures.PV01_SEMI_PARALLEL_GAMMA_BUCKETED.getName());
  /**
   * Measure representing the single-node bucketed gamma PV01 of the calculation target.
   */
  public static final Measure PV01_SINGLE_NODE_GAMMA_BUCKETED =
      Measure.of(StandardMeasures.PV01_SINGLE_NODE_GAMMA_BUCKETED.getName());

  //-------------------------------------------------------------------------
  private AdvancedMeasures() {
  }

}
