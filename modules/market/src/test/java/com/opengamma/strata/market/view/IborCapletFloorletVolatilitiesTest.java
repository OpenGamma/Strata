/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.view;

import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static org.testng.Assert.assertEquals;

import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.market.sensitivity.IborCapletFloorletSensitivity;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivity;

/**
 * Test {@link IborCapletFloorletVolatilities}.
 */
@Test
public class IborCapletFloorletVolatilitiesTest {

  private static final ZonedDateTime DATE_TIME = dateUtc(2015, 8, 27);

  //-------------------------------------------------------------------------
  public void test_defaultMethods() {
    IborCapletFloorletVolatilities test = new TestIborCapletFloorletVolatilities();
    assertEquals(test.getValuationDate(), DATE_TIME.toLocalDate());
    assertEquals(test.volatility(DATE_TIME, 1, 2), 6d);
  }

  static class TestIborCapletFloorletVolatilities implements IborCapletFloorletVolatilities {

    @Override
    public IborIndex getIndex() {
      return GBP_LIBOR_3M;
    }

    @Override
    public ZonedDateTime getValuationDateTime() {
      return DATE_TIME;
    }

    @Override
    public double volatility(double expiry, double strike, double forward) {
      return expiry * 2d;
    }

    @Override
    public SurfaceCurrencyParameterSensitivity surfaceCurrencyParameterSensitivity(
        IborCapletFloorletSensitivity pointSensitivity) {
      throw new UnsupportedOperationException();
    }

    @Override
    public double price(double expiry, PutCall putCall, double strike, double forward, double volatility) {
      throw new UnsupportedOperationException();
    }

    @Override
    public double priceDelta(double expiry, PutCall putCall, double strike, double forward,
        double volatility) {
      throw new UnsupportedOperationException();
    }

    @Override
    public double priceGamma(double expiry, PutCall putCall, double strike, double forward,
        double volatility) {
      throw new UnsupportedOperationException();
    }

    @Override
    public double priceTheta(double expiry, PutCall putCall, double strike, double forward,
        double volatility) {
      throw new UnsupportedOperationException();
    }

    @Override
    public double priceVega(double expiry, PutCall putCall, double strike, double forward,
        double volatility) {
      throw new UnsupportedOperationException();
    }

    @Override
    public double relativeTime(ZonedDateTime date) {
      return 3d;
    }

  }

}
