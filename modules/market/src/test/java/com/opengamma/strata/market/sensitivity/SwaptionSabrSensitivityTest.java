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
  private static final double FORWARD = 0.015;
  private static final double STRIKE = -0.005;
  private static final double ALPHA_SENSI = 2.5d;
  private static final double BETA_SENSI = 0.75d;
  private static final double RHO_SENSI = -0.125d;
  private static final double NU_SENSI = 1.5d;


  public void test_of() {
    SwaptionSabrSensitivity test = SwaptionSabrSensitivity.of(
        SWAP_CONV, DATE_TIME, SWAP_TENOR, STRIKE, FORWARD, GBP, ALPHA_SENSI, BETA_SENSI, RHO_SENSI, NU_SENSI);
    assertEquals(test.getAlphaSensitivity(), ALPHA_SENSI);
    assertEquals(test.getBetaSensitivity(), BETA_SENSI);
    assertEquals(test.getRhoSensitivity(), RHO_SENSI);
    assertEquals(test.getNuSensitivity(), NU_SENSI);
    assertEquals(test.getConvention(), SWAP_CONV);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getExpiry(), DATE_TIME);
    assertEquals(test.getStrike(), STRIKE);
    assertEquals(test.getTenor(), SWAP_TENOR);
  }

  public void test_withCurrency() {
    SwaptionSabrSensitivity base = SwaptionSabrSensitivity.of(
        SWAP_CONV, DATE_TIME, SWAP_TENOR, STRIKE, FORWARD, GBP, ALPHA_SENSI, BETA_SENSI, RHO_SENSI, NU_SENSI);
    assertSame(base.withCurrency(GBP), base);
    SwaptionSabrSensitivity expected = SwaptionSabrSensitivity.of(
        SWAP_CONV, DATE_TIME, SWAP_TENOR, STRIKE, FORWARD, USD, ALPHA_SENSI, BETA_SENSI, RHO_SENSI, NU_SENSI);
    assertEquals(base.withCurrency(USD), expected);
  }

  public void test_multipliedBy() {
    SwaptionSabrSensitivity base = SwaptionSabrSensitivity.of(
        SWAP_CONV, DATE_TIME, SWAP_TENOR, STRIKE, FORWARD, GBP, ALPHA_SENSI, BETA_SENSI, RHO_SENSI, NU_SENSI);
    double factor = -2.1d;
    SwaptionSabrSensitivity expected = SwaptionSabrSensitivity.of(SWAP_CONV, DATE_TIME, SWAP_TENOR, STRIKE, FORWARD,
        GBP, ALPHA_SENSI * factor, BETA_SENSI * factor, RHO_SENSI * factor, NU_SENSI * factor);
    assertEquals(base.multipliedBy(factor), expected);
  }

  public void test_convertedTo() {
    SwaptionSabrSensitivity base = SwaptionSabrSensitivity.of(
        SWAP_CONV, DATE_TIME, SWAP_TENOR, STRIKE, FORWARD, GBP, ALPHA_SENSI, BETA_SENSI, RHO_SENSI, NU_SENSI);
    double rate = 1.5d;
    FxMatrix matrix = FxMatrix.of(CurrencyPair.of(GBP, USD), rate);
    assertSame(base.convertedTo(GBP, matrix), base);
    SwaptionSabrSensitivity expected = SwaptionSabrSensitivity.of(SWAP_CONV, DATE_TIME, SWAP_TENOR, STRIKE, FORWARD,
        USD, ALPHA_SENSI * rate, BETA_SENSI * rate, RHO_SENSI * rate, NU_SENSI * rate);
    assertEquals(base.convertedTo(USD, matrix), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SwaptionSabrSensitivity test1 = SwaptionSabrSensitivity.of(
        SWAP_CONV, DATE_TIME, SWAP_TENOR, STRIKE, FORWARD, GBP, ALPHA_SENSI, BETA_SENSI, RHO_SENSI, NU_SENSI);
    coverImmutableBean(test1);
    SwaptionSabrSensitivity test2 = SwaptionSabrSensitivity.of(FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M,
        dateUtc(2015, 4, 27), 10d, 0.015d, 0.005d, USD, 1.2d, -0.24d, 3.01d, 0.98d);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    SwaptionSabrSensitivity test = SwaptionSabrSensitivity.of(
        SWAP_CONV, DATE_TIME, SWAP_TENOR, STRIKE, FORWARD, GBP, ALPHA_SENSI, BETA_SENSI, RHO_SENSI, NU_SENSI);
    assertSerialization(test);
  }

}
