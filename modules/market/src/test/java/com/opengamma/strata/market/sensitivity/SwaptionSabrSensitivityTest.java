/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import static org.testng.Assert.assertSame;

import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;

/**
 * Test {@link SwaptionSabrSensitivity}.
 */
@Test
public class SwaptionSabrSensitivityTest {

  private static final FixedIborSwapConvention SWAP_CONV = FixedIborSwapConventions.GBP_FIXED_1Y_LIBOR_3M;
  private static final ZonedDateTime DATE_TIME = dateUtc(2015, 8, 27);
  private static final double SWAP_TENOR = 3d;
  private static final double ALPHA_SENSI = 2.5d;
  private static final double BETA_SENSI = 0.75d;
  private static final double RHO_SENSI = -0.125d;
  private static final double NU_SENSI = 1.5d;


  public void test_of() {
    SwaptionSabrSensitivity test = SwaptionSabrSensitivity.of(
        SWAP_CONV, DATE_TIME, SWAP_TENOR, GBP, ALPHA_SENSI, BETA_SENSI, RHO_SENSI, NU_SENSI);
    assertEquals(test.getAlphaSensitivity(), ALPHA_SENSI);
    assertEquals(test.getBetaSensitivity(), BETA_SENSI);
    assertEquals(test.getRhoSensitivity(), RHO_SENSI);
    assertEquals(test.getNuSensitivity(), NU_SENSI);
    assertEquals(test.getConvention(), SWAP_CONV);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getExpiry(), DATE_TIME);
    assertEquals(test.getTenor(), SWAP_TENOR);
  }

  public void test_withCurrency() {
    SwaptionSabrSensitivity base = SwaptionSabrSensitivity.of(
        SWAP_CONV, DATE_TIME, SWAP_TENOR, GBP, ALPHA_SENSI, BETA_SENSI, RHO_SENSI, NU_SENSI);
    assertSame(base.withCurrency(GBP), base);
    SwaptionSabrSensitivity expected = SwaptionSabrSensitivity.of(
        SWAP_CONV, DATE_TIME, SWAP_TENOR, USD, ALPHA_SENSI, BETA_SENSI, RHO_SENSI, NU_SENSI);
    assertEquals(base.withCurrency(USD), expected);
  }

  public void test_withSensitivities() {
    SwaptionSabrSensitivity base = SwaptionSabrSensitivity.of(
        SWAP_CONV, DATE_TIME, SWAP_TENOR, GBP, ALPHA_SENSI, BETA_SENSI, RHO_SENSI, NU_SENSI);
    double alpha = 1d, beta = 2.2d, rho = 1.3d, nu = 0.5d;
    SwaptionSabrSensitivity expected = SwaptionSabrSensitivity.of(
        SWAP_CONV, DATE_TIME, SWAP_TENOR, GBP, alpha, beta, rho, nu);
    assertEquals(base.withSensitivities(alpha, beta, rho, nu), expected);
  }

  public void test_multipliedBy() {
    SwaptionSabrSensitivity base = SwaptionSabrSensitivity.of(
        SWAP_CONV, DATE_TIME, SWAP_TENOR, GBP, ALPHA_SENSI, BETA_SENSI, RHO_SENSI, NU_SENSI);
    double factor = -2.1d;
    SwaptionSabrSensitivity expected = SwaptionSabrSensitivity.of(SWAP_CONV, DATE_TIME, SWAP_TENOR,
        GBP, ALPHA_SENSI * factor, BETA_SENSI * factor, RHO_SENSI * factor, NU_SENSI * factor);
    assertEquals(base.multipliedBy(factor), expected);
  }

  public void test_convertedTo() {
    SwaptionSabrSensitivity base = SwaptionSabrSensitivity.of(
        SWAP_CONV, DATE_TIME, SWAP_TENOR, GBP, ALPHA_SENSI, BETA_SENSI, RHO_SENSI, NU_SENSI);
    double rate = 1.5d;
    FxMatrix matrix = FxMatrix.of(CurrencyPair.of(GBP, USD), rate);
    assertSame(base.convertedTo(GBP, matrix), base);
    SwaptionSabrSensitivity expected = SwaptionSabrSensitivity.of(SWAP_CONV, DATE_TIME, SWAP_TENOR,
        USD, ALPHA_SENSI * rate, BETA_SENSI * rate, RHO_SENSI * rate, NU_SENSI * rate);
    assertEquals(base.convertedTo(USD, matrix), expected);
  }

  public void test_compareKey() {
    SwaptionSabrSensitivity test0 = SwaptionSabrSensitivity.of(
        SWAP_CONV, DATE_TIME, SWAP_TENOR, GBP, ALPHA_SENSI, BETA_SENSI, RHO_SENSI, NU_SENSI);
    SwaptionSabrSensitivity test1 = SwaptionSabrSensitivity.of(
        SWAP_CONV, DATE_TIME, SWAP_TENOR, GBP, ALPHA_SENSI, BETA_SENSI, RHO_SENSI, NU_SENSI);
    SwaptionSabrSensitivity test2 = SwaptionSabrSensitivity.of(
        SWAP_CONV, DATE_TIME, SWAP_TENOR, GBP, 0.5d, BETA_SENSI, RHO_SENSI, NU_SENSI);
    SwaptionSabrSensitivity test3 = SwaptionSabrSensitivity.of(
        SWAP_CONV, DATE_TIME, SWAP_TENOR, GBP, ALPHA_SENSI, 0.5d, RHO_SENSI, NU_SENSI);
    SwaptionSabrSensitivity test4 = SwaptionSabrSensitivity.of(
        SWAP_CONV, DATE_TIME, SWAP_TENOR, GBP, ALPHA_SENSI, BETA_SENSI, 0.5d, NU_SENSI);
    SwaptionSabrSensitivity test5 = SwaptionSabrSensitivity.of(
        SWAP_CONV, DATE_TIME, SWAP_TENOR, GBP, ALPHA_SENSI, BETA_SENSI, RHO_SENSI, 0.5d);
    SwaptionSabrSensitivity test6 = SwaptionSabrSensitivity.of(
        SWAP_CONV, DATE_TIME, SWAP_TENOR, USD, ALPHA_SENSI, BETA_SENSI, RHO_SENSI, NU_SENSI);
    SwaptionSabrSensitivity test7 = SwaptionSabrSensitivity.of(
        SWAP_CONV, DATE_TIME, 2d, GBP, ALPHA_SENSI, BETA_SENSI, RHO_SENSI, NU_SENSI);
    SwaptionSabrSensitivity test8 = SwaptionSabrSensitivity.of(
        SWAP_CONV, dateUtc(2015, 4, 27), SWAP_TENOR, GBP, ALPHA_SENSI, BETA_SENSI, RHO_SENSI, NU_SENSI);
    SwaptionSabrSensitivity test9 = SwaptionSabrSensitivity.of(FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M,
        DATE_TIME, SWAP_TENOR, GBP, ALPHA_SENSI, BETA_SENSI, RHO_SENSI, NU_SENSI);
    assertEquals(test0.compareKey(test1), 0);
    assertEquals(test0.compareKey(test2), 0);
    assertEquals(test0.compareKey(test3), 0);
    assertEquals(test0.compareKey(test4), 0);
    assertEquals(test0.compareKey(test5), 0);
    assertEquals(test0.compareKey(test6) < 0, true);
    assertEquals(test0.compareKey(test7) > 0, true);
    assertEquals(test0.compareKey(test8) > 0, true);
    assertEquals(test0.compareKey(test9) < 0, true);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SwaptionSabrSensitivity test1 = SwaptionSabrSensitivity.of(
        SWAP_CONV, DATE_TIME, SWAP_TENOR, GBP, ALPHA_SENSI, BETA_SENSI, RHO_SENSI, NU_SENSI);
    coverImmutableBean(test1);
    SwaptionSabrSensitivity test2 = SwaptionSabrSensitivity.of(FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M,
        dateUtc(2015, 4, 27), 10d, USD, 1.2d, -0.24d, 3.01d, 0.98d);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    SwaptionSabrSensitivity test = SwaptionSabrSensitivity.of(
        SWAP_CONV, DATE_TIME, SWAP_TENOR, GBP, ALPHA_SENSI, BETA_SENSI, RHO_SENSI, NU_SENSI);
    assertSerialization(test);
  }

}
