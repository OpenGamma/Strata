/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.model;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.pricer.impl.rate.model.HullWhiteOneFactorPiecewiseConstantInterestRateModel;

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
  private static final LocalDate VAL_DATE = LocalDate.of(2015, 2, 14);
  private static final LocalTime TIME = LocalTime.of(14, 00);
  private static final ZoneId ZONE = ZoneId.of("GMT+05");
  private static final ZonedDateTime DATE_TIME = VAL_DATE.atTime(TIME).atZone(ZONE);

  public void test_of_ZonedDateTime() {
    HullWhiteOneFactorPiecewiseConstantParametersProvider test =
        HullWhiteOneFactorPiecewiseConstantParametersProvider.of(PARAMETERS, ACT_360, DATE_TIME);
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.getParameters(), PARAMETERS);
    assertEquals(test.getValuationDateTime(), DATE_TIME);
  }

  public void test_of_LocalDateAndTime() {
    HullWhiteOneFactorPiecewiseConstantParametersProvider test =
        HullWhiteOneFactorPiecewiseConstantParametersProvider.of(PARAMETERS, ACT_360, VAL_DATE, TIME, ZONE);
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.getParameters(), PARAMETERS);
    assertEquals(test.getValuationDateTime(), VAL_DATE.atTime(TIME).atZone(ZONE));
  }

  public void test_futuresConvexityFactor() {
    HullWhiteOneFactorPiecewiseConstantParametersProvider provider =
        HullWhiteOneFactorPiecewiseConstantParametersProvider.of(PARAMETERS, ACT_360, DATE_TIME);
    LocalDate data1 = LocalDate.of(2015, 5, 14);
    LocalDate data2 = LocalDate.of(2015, 5, 20);
    LocalDate data3 = LocalDate.of(2015, 8, 20);
    double computed = provider.futuresConvexityFactor(data1, data2, data3);
    double expected = HullWhiteOneFactorPiecewiseConstantInterestRateModel.DEFAULT.futuresConvexityFactor(PARAMETERS,
        ACT_360.relativeYearFraction(VAL_DATE, data1), ACT_360.relativeYearFraction(VAL_DATE, data2),
        ACT_360.relativeYearFraction(VAL_DATE, data3));
    assertEquals(computed, expected);
  }

  public void test_futuresConvexityFactorAdjoint() {
    HullWhiteOneFactorPiecewiseConstantParametersProvider provider =
        HullWhiteOneFactorPiecewiseConstantParametersProvider.of(PARAMETERS, ACT_360, DATE_TIME);
    LocalDate data1 = LocalDate.of(2015, 5, 14);
    LocalDate data2 = LocalDate.of(2015, 5, 20);
    LocalDate data3 = LocalDate.of(2015, 8, 20);
    ValueDerivatives computed = provider.futuresConvexityFactorAdjoint(data1, data2, data3);
    ValueDerivatives expected = HullWhiteOneFactorPiecewiseConstantInterestRateModel.DEFAULT
        .futuresConvexityFactorAdjoint(PARAMETERS, ACT_360.relativeYearFraction(VAL_DATE, data1),
            ACT_360.relativeYearFraction(VAL_DATE, data2), ACT_360.relativeYearFraction(VAL_DATE, data3));
    assertEquals(computed, expected);
  }

  public void test_alpha() {
    HullWhiteOneFactorPiecewiseConstantParametersProvider provider =
        HullWhiteOneFactorPiecewiseConstantParametersProvider.of(PARAMETERS, ACT_360, DATE_TIME);
    LocalDate data1 = LocalDate.of(2015, 5, 20);
    LocalDate data2 = LocalDate.of(2015, 8, 20);
    LocalDate data3 = LocalDate.of(2015, 8, 20);
    LocalDate data4 = LocalDate.of(2015, 8, 27);
    double computed = provider.alpha(data1, data2, data3, data4);
    double expected = HullWhiteOneFactorPiecewiseConstantInterestRateModel.DEFAULT.alpha(PARAMETERS,
        ACT_360.relativeYearFraction(VAL_DATE, data1), ACT_360.relativeYearFraction(VAL_DATE, data2),
        ACT_360.relativeYearFraction(VAL_DATE, data3), ACT_360.relativeYearFraction(VAL_DATE, data4));
    assertEquals(computed, expected);
  }

  public void test_alphaAdjoint() {
    HullWhiteOneFactorPiecewiseConstantParametersProvider provider =
        HullWhiteOneFactorPiecewiseConstantParametersProvider.of(PARAMETERS, ACT_360, DATE_TIME);
    LocalDate data1 = LocalDate.of(2015, 5, 20);
    LocalDate data2 = LocalDate.of(2015, 8, 20);
    LocalDate data3 = LocalDate.of(2015, 8, 20);
    LocalDate data4 = LocalDate.of(2015, 8, 27);
    ValueDerivatives computed = provider.alphaAdjoint(data1, data2, data3, data4);
    ValueDerivatives expected = HullWhiteOneFactorPiecewiseConstantInterestRateModel.DEFAULT.alphaAdjoint(
        PARAMETERS, ACT_360.relativeYearFraction(VAL_DATE, data1), ACT_360.relativeYearFraction(VAL_DATE, data2),
        ACT_360.relativeYearFraction(VAL_DATE, data3), ACT_360.relativeYearFraction(VAL_DATE, data4));
    assertEquals(computed, expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    HullWhiteOneFactorPiecewiseConstantParametersProvider test1 =
        HullWhiteOneFactorPiecewiseConstantParametersProvider.of(PARAMETERS, ACT_360, DATE_TIME);
    coverImmutableBean(test1);
    HullWhiteOneFactorPiecewiseConstantParameters params = HullWhiteOneFactorPiecewiseConstantParameters.of(
        0.02, DoubleArray.of(0.01, 0.011, 0.014), DoubleArray.of(0.5, 5.0));
    HullWhiteOneFactorPiecewiseConstantParametersProvider test2 =
        HullWhiteOneFactorPiecewiseConstantParametersProvider.of(params, ACT_ACT_ISDA, DATE_TIME.plusDays(1));
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    HullWhiteOneFactorPiecewiseConstantParametersProvider test =
        HullWhiteOneFactorPiecewiseConstantParametersProvider.of(PARAMETERS, ACT_360, DATE_TIME);
    assertSerialization(test);
  }
}
