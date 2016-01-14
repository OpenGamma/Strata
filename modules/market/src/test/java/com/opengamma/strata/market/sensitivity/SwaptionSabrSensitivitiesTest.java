/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;

/**
 * Test {@link SwaptionSabrSensitivities}.
 */
@Test
public class SwaptionSabrSensitivitiesTest {

  private static final FixedIborSwapConvention SWAP_CONV = FixedIborSwapConventions.GBP_FIXED_1Y_LIBOR_3M;
  private static final ZonedDateTime DATE_TIME_1 = dateUtc(2015, 8, 27);
  private static final ZonedDateTime DATE_TIME_2 = dateUtc(2016, 8, 28);
  private static final double SWAP_TENOR = 3d;
  private static final double ALPHA_SENSI_1 = 2.5d;
  private static final double BETA_SENSI_1 = 0.75d;
  private static final double RHO_SENSI_1 = -0.125d;
  private static final double NU_SENSI_1 = 1.5d;
  private static final double ALPHA_SENSI_2 = -0.12d;
  private static final double BETA_SENSI_2 = 1.15d;
  private static final double RHO_SENSI_2 = 0.15d;
  private static final double NU_SENSI_2 = 2.5d;
  private static final SwaptionSabrSensitivity SENSI_1 = SwaptionSabrSensitivity.of(
      SWAP_CONV, DATE_TIME_1, SWAP_TENOR, GBP, ALPHA_SENSI_1, BETA_SENSI_1, RHO_SENSI_1, NU_SENSI_1);
  private static final SwaptionSabrSensitivity SENSI_2 = SwaptionSabrSensitivity.of(
      SWAP_CONV, DATE_TIME_1, SWAP_TENOR, GBP, ALPHA_SENSI_2, BETA_SENSI_2, RHO_SENSI_2, NU_SENSI_2);
  private static final SwaptionSabrSensitivity SENSI_3 = SwaptionSabrSensitivity.of(
      SWAP_CONV, DATE_TIME_2, SWAP_TENOR, GBP, ALPHA_SENSI_1, BETA_SENSI_1, RHO_SENSI_1, NU_SENSI_1);
  private static final SwaptionSabrSensitivity SENSI_12 = SwaptionSabrSensitivity.of(SWAP_CONV, DATE_TIME_1, SWAP_TENOR,
      GBP, ALPHA_SENSI_1 + ALPHA_SENSI_2, BETA_SENSI_1 + BETA_SENSI_2, RHO_SENSI_1 + RHO_SENSI_2, NU_SENSI_1 + NU_SENSI_2);

  public void test_empty() {
    SwaptionSabrSensitivities test = SwaptionSabrSensitivities.empty();
    assertTrue(test.getSensitivities().isEmpty());
  }

  public void test_of_sensitivity() {
    SwaptionSabrSensitivities test = SwaptionSabrSensitivities.of(SENSI_1);
    assertEquals(test.getSensitivities().size(), 1);
    assertEquals(test.getSensitivities().get(0), SENSI_1);
  }

  public void test_of_sensitivities() {
    List<SwaptionSabrSensitivity> list = Arrays.asList(SENSI_1, SENSI_3);
    SwaptionSabrSensitivities test = SwaptionSabrSensitivities.of(list);
    assertEquals(test.getSensitivities().size(), 2);
    assertEquals(test.getSensitivities().get(0), SENSI_1);
    assertEquals(test.getSensitivities().get(1), SENSI_3);
  }

  public void test_of_sensitivities_normalize() {
    List<SwaptionSabrSensitivity> list = Arrays.asList(SENSI_1, SENSI_2);
    SwaptionSabrSensitivities test = SwaptionSabrSensitivities.of(list).normalize();
    assertEquals(test.getSensitivities().size(), 1);
    assertEquals(test.getSensitivities().get(0), SENSI_12);
  }

  public void test_add() {
    SwaptionSabrSensitivities base = SwaptionSabrSensitivities.of(SENSI_1);
    SwaptionSabrSensitivities expected = SwaptionSabrSensitivities.of(Arrays.asList(SENSI_1, SENSI_3));
    assertEquals(base.add(SENSI_3), expected);
  }

  public void test_add_normalize() {
    SwaptionSabrSensitivities base = SwaptionSabrSensitivities.of(SENSI_1);
    SwaptionSabrSensitivities expected = SwaptionSabrSensitivities.of(Arrays.asList(SENSI_12));
    assertEquals(base.add(SENSI_2).normalize(), expected);
  }

  public void test_combine() {
    SwaptionSabrSensitivities base = SwaptionSabrSensitivities.of(SENSI_1);
    SwaptionSabrSensitivities other = SwaptionSabrSensitivities.of(SENSI_3);
    SwaptionSabrSensitivities expected = SwaptionSabrSensitivities.of(Arrays.asList(SENSI_1, SENSI_3));
    assertEquals(base.combine(other), expected);
  }

  public void test_combine_normalize() {
    SwaptionSabrSensitivities base = SwaptionSabrSensitivities.of(SENSI_1);
    SwaptionSabrSensitivities other = SwaptionSabrSensitivities.of(Arrays.asList(SENSI_2, SENSI_3));
    SwaptionSabrSensitivities expected = SwaptionSabrSensitivities.of(Arrays.asList(SENSI_12, SENSI_3));
    assertEquals(base.combine(other).normalize(), expected);
  }

  public void test_convertedTo() {
    SwaptionSabrSensitivities base = SwaptionSabrSensitivities.of(Arrays.asList(SENSI_1, SENSI_3));
    double rate = 1.5d;
    FxMatrix matrix = FxMatrix.of(CurrencyPair.of(GBP, USD), rate);
    assertEquals(base.convertedTo(GBP, matrix), base);
    SwaptionSabrSensitivities expected = SwaptionSabrSensitivities.of(
        Arrays.asList(SENSI_1.convertedTo(USD, matrix), SENSI_3.convertedTo(USD, matrix)));
    assertEquals(base.convertedTo(USD, matrix), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SwaptionSabrSensitivities test1 = SwaptionSabrSensitivities.of(SENSI_1);
    coverImmutableBean(test1);
    SwaptionSabrSensitivities test2 = SwaptionSabrSensitivities.of(Arrays.asList(SENSI_2, SENSI_3));
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    SwaptionSabrSensitivities test = SwaptionSabrSensitivities.of(SENSI_1);
    assertSerialization(test);
  }

}
