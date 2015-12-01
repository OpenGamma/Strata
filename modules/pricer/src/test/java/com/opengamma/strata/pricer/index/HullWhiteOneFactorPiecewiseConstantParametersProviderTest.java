/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.pricer.impl.rate.model.HullWhiteOneFactorPiecewiseConstantInterestRateModel;
import com.opengamma.strata.pricer.impl.rate.model.HullWhiteOneFactorPiecewiseConstantParameters;
import com.opengamma.strata.pricer.index.HullWhiteOneFactorPiecewiseConstantParametersProvider;

/**
 * Test {@link HullWhiteOneFactorPiecewiseConstantParametersProvider}.
 */
@Test
public class HullWhiteOneFactorPiecewiseConstantParametersProviderTest {

  private static final double MEAN_REVERSION = 0.01;
  private static final DoubleArray VOLATILITY = DoubleArray.of(0.01, 0.011, 0.012, 0.013, 0.014);
  private static final DoubleArray VOLATILITY_TIME = DoubleArray.of(0.5, 1.0, 2.0, 5.0);
  private static final HullWhiteOneFactorPiecewiseConstantParameters PARAMETERS =
      HullWhiteOneFactorPiecewiseConstantParameters.of(MEAN_REVERSION, VOLATILITY, VOLATILITY_TIME);
  private static final LocalDate VALUATION = LocalDate.of(2015, 2, 14);
  private static final LocalTime TIME = LocalTime.of(14, 00);
  private static final ZoneId ZONE = ZoneId.of("GMT+05");

  public void test_of_LocalDate() {
    HullWhiteOneFactorPiecewiseConstantParametersProvider test =
        HullWhiteOneFactorPiecewiseConstantParametersProvider.of(PARAMETERS, ACT_360, VALUATION);
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.getParameters(), PARAMETERS);
    assertEquals(test.getValuationDateTime(), VALUATION.atTime(LocalTime.NOON).atZone(ZoneOffset.UTC));
  }

  public void test_of_ZonedDateTime() {
    ZonedDateTime dataTime = VALUATION.atTime(TIME).atZone(ZONE);
    HullWhiteOneFactorPiecewiseConstantParametersProvider test =
        HullWhiteOneFactorPiecewiseConstantParametersProvider.of(PARAMETERS, ACT_360, dataTime);
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.getParameters(), PARAMETERS);
    assertEquals(test.getValuationDateTime(), dataTime);
  }

  public void test_of_LocalDateAndTime() {
    HullWhiteOneFactorPiecewiseConstantParametersProvider test =
        HullWhiteOneFactorPiecewiseConstantParametersProvider.of(PARAMETERS, ACT_360, VALUATION, TIME, ZONE);
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.getParameters(), PARAMETERS);
    assertEquals(test.getValuationDateTime(), VALUATION.atTime(TIME).atZone(ZONE));
  }

  public void test_futuresConvexityFactor() {
    HullWhiteOneFactorPiecewiseConstantParametersProvider provider =
        HullWhiteOneFactorPiecewiseConstantParametersProvider.of(PARAMETERS, ACT_360, VALUATION);
    LocalDate data1 = LocalDate.of(2014, 5, 14);
    LocalDate data2 = LocalDate.of(2014, 5, 20);
    LocalDate data3 = LocalDate.of(2014, 8, 20);
    double computed = provider.futuresConvexityFactor(data1, data2, data3);
    double expected = HullWhiteOneFactorPiecewiseConstantInterestRateModel.DEFAULT.futuresConvexityFactor(PARAMETERS,
        ACT_360.relativeYearFraction(VALUATION, data1), ACT_360.relativeYearFraction(VALUATION, data2),
        ACT_360.relativeYearFraction(VALUATION, data3));
    assertEquals(computed, expected);
  }

  public void test_futuresConvexityFactorAdjoint() {
    HullWhiteOneFactorPiecewiseConstantParametersProvider provider =
        HullWhiteOneFactorPiecewiseConstantParametersProvider.of(PARAMETERS, ACT_360, VALUATION);
    LocalDate data1 = LocalDate.of(2014, 5, 14);
    LocalDate data2 = LocalDate.of(2014, 5, 20);
    LocalDate data3 = LocalDate.of(2014, 8, 20);
    ValueDerivatives computed = provider.futuresConvexityFactorAdjoint(data1, data2, data3);
    ValueDerivatives expected = HullWhiteOneFactorPiecewiseConstantInterestRateModel.DEFAULT
        .futuresConvexityFactorAdjoint(PARAMETERS, ACT_360.relativeYearFraction(VALUATION, data1),
            ACT_360.relativeYearFraction(VALUATION, data2), ACT_360.relativeYearFraction(VALUATION, data3));
    assertEquals(computed, expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    HullWhiteOneFactorPiecewiseConstantParametersProvider test1 =
        HullWhiteOneFactorPiecewiseConstantParametersProvider.of(PARAMETERS, ACT_360, VALUATION);
    coverImmutableBean(test1);
    HullWhiteOneFactorPiecewiseConstantParameters params = HullWhiteOneFactorPiecewiseConstantParameters.of(
        0.02, DoubleArray.of(0.01, 0.011, 0.014), DoubleArray.of(0.5, 5.0));
    HullWhiteOneFactorPiecewiseConstantParametersProvider test2 =
        HullWhiteOneFactorPiecewiseConstantParametersProvider.of(params, ACT_ACT_ISDA, LocalDate.of(2015, 3, 14));
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    HullWhiteOneFactorPiecewiseConstantParametersProvider test =
        HullWhiteOneFactorPiecewiseConstantParametersProvider.of(PARAMETERS, ACT_360, VALUATION);
    assertSerialization(test);
  }
}
