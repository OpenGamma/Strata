/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.pricer.impl.rate.model.HullWhiteOneFactorPiecewiseConstantInterestRateModel;
import com.opengamma.strata.pricer.impl.rate.model.HullWhiteOneFactorPiecewiseConstantParameters;

/**
 * Test {@link HullWhiteOneFactorPiecewiseConstantConvexityFactorProvider}.
 */
@Test
public class HullWhiteOneFactorPiecewiseConstantConvexityFactorProviderTest {

  private static final double MEAN_REVERSION = 0.01;
  private static final DoubleArray VOLATILITY = DoubleArray.of(0.01, 0.011, 0.012, 0.013, 0.014);
  private static final DoubleArray VOLATILITY_TIME = DoubleArray.of(0.5, 1.0, 2.0, 5.0);
  private static final HullWhiteOneFactorPiecewiseConstantParameters PARAMETERS =
      HullWhiteOneFactorPiecewiseConstantParameters.of(MEAN_REVERSION, VOLATILITY, VOLATILITY_TIME);
  private static final LocalDate VALUATION = LocalDate.of(2015, 2, 14);

  public void test_of() {
    HullWhiteOneFactorPiecewiseConstantConvexityFactorProvider test =
        HullWhiteOneFactorPiecewiseConstantConvexityFactorProvider.of(PARAMETERS, ACT_360, VALUATION);
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.getParameters(), PARAMETERS);
    assertEquals(test.getValuationDate(), VALUATION);
  }

  public void test_futuresConvexityFactor() {
    HullWhiteOneFactorPiecewiseConstantConvexityFactorProvider provider =
        HullWhiteOneFactorPiecewiseConstantConvexityFactorProvider.of(PARAMETERS, ACT_360, VALUATION);
    LocalDate data1 = LocalDate.of(2014, 5, 14);
    LocalDate data2 = LocalDate.of(2014, 5, 20);
    LocalDate data3 = LocalDate.of(2014, 8, 20);
    double computed = provider.futuresConvexityFactor(data1, data2, data3);
    double expected = HullWhiteOneFactorPiecewiseConstantInterestRateModel.DEFAULT.futuresConvexityFactor(PARAMETERS,
        ACT_360.relativeYearFraction(VALUATION, data1), ACT_360.relativeYearFraction(VALUATION, data2),
        ACT_360.relativeYearFraction(VALUATION, data3));
    assertEquals(computed, expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    HullWhiteOneFactorPiecewiseConstantConvexityFactorProvider test1 =
        HullWhiteOneFactorPiecewiseConstantConvexityFactorProvider.of(PARAMETERS, ACT_360, VALUATION);
    coverImmutableBean(test1);
    HullWhiteOneFactorPiecewiseConstantParameters params = HullWhiteOneFactorPiecewiseConstantParameters.of(
        0.02, DoubleArray.of(0.01, 0.011, 0.014), DoubleArray.of(0.5, 5.0));
    HullWhiteOneFactorPiecewiseConstantConvexityFactorProvider test2 =
        HullWhiteOneFactorPiecewiseConstantConvexityFactorProvider.of(params, ACT_ACT_ISDA, LocalDate.of(2015, 3, 14));
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    HullWhiteOneFactorPiecewiseConstantConvexityFactorProvider test =
        HullWhiteOneFactorPiecewiseConstantConvexityFactorProvider.of(PARAMETERS, ACT_360, VALUATION);
    assertSerialization(test);
  }
}
