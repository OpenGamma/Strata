/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.rate;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.joda.beans.ImmutableBean;
import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.data.FxMatrixId;
import com.opengamma.strata.data.FxRateId;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.ObservableSource;

/**
 * Test {@link FxRateLookup}.
 */
@Test
public class FxRateLookupTest {

  private static final ObservableSource OBS_SOURCE = ObservableSource.of("Vendor");
  private static final LocalDate VAL_DATE = date(2016, 6, 30);

  //-------------------------------------------------------------------------
  public void test_ofRates() {
    FxRateLookup test = FxRateLookup.ofRates();
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE)
        .addValue(FxRateId.of(GBP, USD), FxRate.of(GBP, USD, 1.5d))
        .build();

    assertEquals(test.fxRateProvider(marketData).fxRate(GBP, USD), 1.5d);
  }

  public void test_ofRates_source() {
    FxRateLookup test = FxRateLookup.ofRates(OBS_SOURCE);
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE)
        .addValue(FxRateId.of(GBP, USD, OBS_SOURCE), FxRate.of(GBP, USD, 1.5d))
        .build();

    assertEquals(test.fxRateProvider(marketData).fxRate(GBP, USD), 1.5d);
  }

  public void test_ofRates_currency() {
    FxRateLookup test = FxRateLookup.ofRates(EUR);
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE)
        .addValue(FxRateId.of(GBP, USD), FxRate.of(GBP, USD, 1.5d))
        .build();

    assertEquals(test.fxRateProvider(marketData).fxRate(GBP, USD), 1.5d);
  }

  public void test_ofRates_currency_source() {
    FxRateLookup test = FxRateLookup.ofRates(EUR, OBS_SOURCE);
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE)
        .addValue(FxRateId.of(GBP, USD, OBS_SOURCE), FxRate.of(GBP, USD, 1.5d))
        .build();

    assertEquals(test.fxRateProvider(marketData).fxRate(GBP, USD), 1.5d);
  }

  public void test_ofMatrix() {
    FxRateLookup test = FxRateLookup.ofMatrix();
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE)
        .addValue(FxMatrixId.standard(), FxMatrix.of(GBP, USD, 1.5d))
        .build();

    assertEquals(test.fxRateProvider(marketData).fxRate(GBP, USD), 1.5d);
  }

  public void test_ofMatrix_source() {
    FxRateLookup test = FxRateLookup.ofMatrix(FxMatrixId.of(OBS_SOURCE));
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE)
        .addValue(FxMatrixId.of(OBS_SOURCE), FxMatrix.of(GBP, USD, 1.5d))
        .build();

    assertEquals(test.fxRateProvider(marketData).fxRate(GBP, USD), 1.5d);
  }

  //-------------------------------------------------------------------------
  public void coverage_rates() {
    FxRateLookup test = FxRateLookup.ofRates();
    coverImmutableBean((ImmutableBean) test);
    FxRateLookup test2 = FxRateLookup.ofRates(EUR);
    coverBeanEquals((ImmutableBean) test, (ImmutableBean) test2);
  }

  public void coverage_matrix() {
    FxRateLookup test = FxRateLookup.ofMatrix();
    coverImmutableBean((ImmutableBean) test);
    FxRateLookup test2 = FxRateLookup.ofMatrix(FxMatrixId.of(OBS_SOURCE));
    coverBeanEquals((ImmutableBean) test, (ImmutableBean) test2);
  }

  public void test_serialization() {
    FxRateLookup test1 = FxRateLookup.ofRates();
    assertSerialization((ImmutableBean) test1);
    FxRateLookup test2 = FxRateLookup.ofMatrix();
    assertSerialization((ImmutableBean) test2);
  }

}
