/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.value;

import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.GBP_FIXED_1Y_LIBOR_3M;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.market.sensitivity.SwaptionSensitivity;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivity;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;

/**
 * Test {@link SwaptionVolatilities}.
 */
@Test
public class SwaptionVolatilitiesTest {

  private static final ZonedDateTime DATE_TIME = ZonedDateTime.now();

  //-------------------------------------------------------------------------
  public void test_defaultMethods() {
    SwaptionVolatilities test = new TestSwaptionVolatilities();
    assertEquals(test.getValuationDate(), DATE_TIME.toLocalDate());
    assertEquals(test.volatility(DATE_TIME, 1, 2, 3), 6d);
  }

  static class TestSwaptionVolatilities implements SwaptionVolatilities {

    @Override
    public FixedIborSwapConvention getConvention() {
      return GBP_FIXED_1Y_LIBOR_3M;
    }

    @Override
    public ZonedDateTime getValuationDateTime() {
      return DATE_TIME;
    }

    @Override
    public double volatility(double expiry, double tenor, double strike, double forward) {
      return expiry * 2d;
    }

    @Override
    public SurfaceCurrencyParameterSensitivity surfaceCurrencyParameterSensitivity(SwaptionSensitivity pointSensitivity) {
      throw new UnsupportedOperationException();
    }

    @Override
    public double price(double expiry, double tenor, PutCall putCall, double strike, double forward, double volatility) {
      throw new UnsupportedOperationException();
    }

    @Override
    public double priceDelta(double expiry, double tenor, PutCall putCall, double strike, double forward, double volatility) {
      throw new UnsupportedOperationException();
    }

    @Override
    public double priceGamma(double expiry, double tenor, PutCall putCall, double strike, double forward, double volatility) {
      throw new UnsupportedOperationException();
    }

    @Override
    public double priceTheta(double expiry, double tenor, PutCall putCall, double strike, double forward, double volatility) {
      throw new UnsupportedOperationException();
    }

    @Override
    public double priceVega(double expiry, double tenor, PutCall putCall, double strike, double forward, double volatility) {
      throw new UnsupportedOperationException();
    }

    @Override
    public double relativeTime(ZonedDateTime date) {
      return 3d;
    }

    @Override
    public double tenor(LocalDate startDate, LocalDate endDate) {
      throw new UnsupportedOperationException();
    }

  }

}
