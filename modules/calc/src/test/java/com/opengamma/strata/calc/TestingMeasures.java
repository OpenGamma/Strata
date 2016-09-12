/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

/**
 * Measures for testing.
 */
public final class TestingMeasures {

  /**
   * Measure representing the cash flows of the calculation target.
   */
  public static final Measure CASH_FLOWS = ImmutableMeasure.of("CashFlows");
  /**
   * Measure representing the par rate of the calculation target.
   */
  public static final Measure PAR_RATE = ImmutableMeasure.of("ParRate", false);
  /**
   * Measure representing the present value of the calculation target.
   */
  public static final Measure PRESENT_VALUE = ImmutableMeasure.of("PresentValue");
  /**
   * Measure representing the present value of the calculation target.
   * <p>
   * Calculated values are not converted to the reporting currency and may contain values in multiple currencies
   * if the target contains multiple currencies.
   */
  public static final Measure PRESENT_VALUE_MULTI_CCY = ImmutableMeasure.of("PresentValueMultiCurrency", false);
  /**
   * Measure representing the Bucketed PV01 of the calculation target.
   */
  public static final Measure BUCKETED_PV01 = ImmutableMeasure.of("BucketedPV01");

  //-------------------------------------------------------------------------
  private TestingMeasures() {
  }

}
