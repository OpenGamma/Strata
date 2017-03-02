/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.GBP_FIXED_1Y_LIBOR_3M;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;

/**
 * Test {@link SwaptionVolatilities}.
 */
@Test
public class SwaptionVolatilitiesTest {

  private static final ZonedDateTime DATE_TIME = ZonedDateTime.now();

  //-------------------------------------------------------------------------
  public void test_defaultMethods() {
    SwaptionVolatilities test = new TestingSwaptionVolatilities();
    assertEquals(test.getValuationDate(), DATE_TIME.toLocalDate());
    assertEquals(test.volatility(DATE_TIME, 1, 2, 3), 6d);
    assertEquals(test.parameterSensitivity(), CurrencyParameterSensitivities.empty());
  }

  static class TestingSwaptionVolatilities implements SwaptionVolatilities {

    @Override
    public SwaptionVolatilitiesName getName() {
      return SwaptionVolatilitiesName.of("Default");
    }

    @Override
    public FixedIborSwapConvention getConvention() {
      return GBP_FIXED_1Y_LIBOR_3M;
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
    public int getParameterCount() {
      throw new UnsupportedOperationException();
    }

    @Override
    public double getParameter(int parameterIndex) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ParameterMetadata getParameterMetadata(int parameterIndex) {
      throw new UnsupportedOperationException();
    }

    @Override
    public SwaptionVolatilities withParameter(int parameterIndex, double newValue) {
      throw new UnsupportedOperationException();
    }

    @Override
    public SwaptionVolatilities withPerturbation(ParameterPerturbation perturbation) {
      throw new UnsupportedOperationException();
    }

    @Override
    public double volatility(double expiry, double tenor, double strike, double forward) {
      return expiry * 2d;
    }

    @Override
    public CurrencyParameterSensitivities parameterSensitivity(PointSensitivities pointSensitivities) {
      return CurrencyParameterSensitivities.empty();
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

    @Override
    public LocalDate getValuationDate() {
      return getValuationDateTime().toLocalDate();
    }

    @Override
    public ValueType getVolatilityType() {
      return ValueType.BLACK_VOLATILITY;
    }

  }

}
