/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static org.testng.Assert.assertEquals;

import java.time.ZonedDateTime;
import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.product.common.PutCall;

/**
 * Test {@link IborCapletFloorletVolatilities}.
 */
@Test
public class IborCapletFloorletVolatilitiesTest {

  private static final ZonedDateTime DATE_TIME = dateUtc(2015, 8, 27);

  //-------------------------------------------------------------------------
  public void test_defaultMethods() {
    IborCapletFloorletVolatilities test = new TestingIborCapletFloorletVolatilities();
    assertEquals(test.getValuationDate(), DATE_TIME.toLocalDate());
    assertEquals(test.volatility(DATE_TIME, 1, 2), 6d);
  }

  static class TestingIborCapletFloorletVolatilities implements IborCapletFloorletVolatilities {

    @Override
    public IborIndex getIndex() {
      return GBP_LIBOR_3M;
    }

    @Override
    public ZonedDateTime getValuationDateTime() {
      return DATE_TIME;
    }

    @Override
    public <T> Optional<T> findData(MarketDataName<T> name) {
      return Optional.empty();
    }

    @Override
    public double volatility(double expiry, double strike, double forward) {
      return expiry * 2d;
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

    @Override
    public int getParameterCount() {
      return 0;
    }

    @Override
    public double getParameter(int parameterIndex) {
      return 0;
    }

    @Override
    public ParameterMetadata getParameterMetadata(int parameterIndex) {
      return null;
    }

    @Override
    public IborCapletFloorletVolatilitiesName getName() {
      return null;
    }

    @Override
    public ValueType getVolatilityType() {
      return null;
    }

    @Override
    public IborCapletFloorletVolatilities withParameter(int parameterIndex, double newValue) {
      return null;
    }

    @Override
    public IborCapletFloorletVolatilities withPerturbation(ParameterPerturbation perturbation) {
      return null;
    }

    @Override
    public CurrencyParameterSensitivities parameterSensitivity(PointSensitivities pointSensitivities) {
      return null;
    }

  }

}
