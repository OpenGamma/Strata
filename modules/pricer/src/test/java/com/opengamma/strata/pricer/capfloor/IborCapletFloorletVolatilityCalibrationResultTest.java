/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.Surfaces;

/**
 * Test {@link IborCapletFloorletVolatilityCalibrationResult}.
 */
public class IborCapletFloorletVolatilityCalibrationResultTest {

  protected static final ZonedDateTime VALUATION = LocalDate.of(2016, 3, 3).atTime(10, 0).atZone(ZoneId.of("America/New_York"));
  private static final ConstantSurface SURFACE = ConstantSurface.of(
      Surfaces.blackVolatilityByExpiryStrike("volSurface", ACT_ACT_ISDA), 0.15);
  private static final BlackIborCapletFloorletExpiryStrikeVolatilities VOLS =
      BlackIborCapletFloorletExpiryStrikeVolatilities.of(USD_LIBOR_3M, VALUATION, SURFACE);

  @Test
  public void test_ofLestSquare() {
    double chiSq = 5.5e-6;
    IborCapletFloorletVolatilityCalibrationResult test = IborCapletFloorletVolatilityCalibrationResult.ofLeastSquare(VOLS, chiSq);
    assertThat(test.getVolatilities()).isEqualTo(VOLS);
    assertThat(test.getChiSquare()).isEqualTo(chiSq);
  }

  @Test
  public void test_ofRootFind() {
    IborCapletFloorletVolatilityCalibrationResult test = IborCapletFloorletVolatilityCalibrationResult.ofRootFind(VOLS);
    assertThat(test.getVolatilities()).isEqualTo(VOLS);
    assertThat(test.getChiSquare()).isEqualTo(0d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    double chiSq = 5.5e-12;
    IborCapletFloorletVolatilityCalibrationResult test1 =
        IborCapletFloorletVolatilityCalibrationResult.ofLeastSquare(VOLS, chiSq);
    coverImmutableBean(test1);
    IborCapletFloorletVolatilityCalibrationResult test2 = IborCapletFloorletVolatilityCalibrationResult.ofRootFind(
        BlackIborCapletFloorletExpiryStrikeVolatilities.of(GBP_LIBOR_3M, VALUATION, SURFACE));
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    IborCapletFloorletVolatilityCalibrationResult test = IborCapletFloorletVolatilityCalibrationResult.ofRootFind(VOLS);
    assertSerialization(test);
  }

}
