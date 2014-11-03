/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.curves;

import static com.opengamma.basics.date.Tenor.TENOR_1M;
import static com.opengamma.basics.date.Tenor.TENOR_2M;
import static com.opengamma.basics.date.Tenor.TENOR_3M;
import static com.opengamma.basics.date.Tenor.TENOR_4M;
import static com.opengamma.basics.date.Tenor.TENOR_6M;
import static com.opengamma.basics.date.Tenor.TENOR_9M;
import static com.opengamma.platform.curves.InterpolationMethod.LINEAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.testng.Assert.assertNotNull;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.basics.date.Tenor;
import com.opengamma.platform.curves.SimpleCurveCalibrator;
import com.opengamma.platform.curves.YieldCurve;

public class SimpleCurveCalibratorTest {

  @Test
  public void simpleCurveCreation() {

    SimpleCurveCalibrator curveCalibrator = new SimpleCurveCalibrator();

    ImmutableMap<Tenor, Double> rates = ImmutableMap.of(
        TENOR_1M, 0.0015,
        TENOR_2M, 0.0019,
        TENOR_3M, 0.0023,
        TENOR_6M, 0.0032);

    YieldCurve yieldCurve = curveCalibrator.buildYieldCurve(rates, LocalDate.of(2014, 1, 1));
    assertNotNull(yieldCurve);
  }

  @Test
  public void interpolateDiscountFactor() {

    SimpleCurveCalibrator curveCalibrator = new SimpleCurveCalibrator().withInterpolation(LINEAR);

    LocalDate start = LocalDate.of(2014, 7, 23);

    ImmutableMap<Tenor, Double> rates = ImmutableMap.of(
        TENOR_1M, 0.0015,
        TENOR_2M, 0.0019,
        TENOR_3M, 0.0023,
        TENOR_6M, 0.0032,
        TENOR_9M, 0.0042);

    YieldCurve yieldCurve = curveCalibrator.buildYieldCurve(rates, start);
    assertThat(yieldCurve.getDiscountFactor(TENOR_4M)).isCloseTo(0.999123, within(10e-6));
  }

  @Test
  public void interpolateForwardRate() {

    SimpleCurveCalibrator curveCalibrator = new SimpleCurveCalibrator().withInterpolation(LINEAR);

    LocalDate start = LocalDate.of(2014, 7, 23);

    ImmutableMap<Tenor, Double> rates = ImmutableMap.of(
        TENOR_1M, 0.0015,
        TENOR_2M, 0.0019,
        TENOR_3M, 0.0023,
        TENOR_6M, 0.0032,
        TENOR_9M, 0.0042);

    YieldCurve yieldCurve = curveCalibrator.buildYieldCurve(rates, start);
    assertThat(yieldCurve.getForwardRate(TENOR_6M, TENOR_9M)).isCloseTo(0.006249, within(10e-6));
  }

}

