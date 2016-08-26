/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.basics.index.OvernightIndexObservation;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.DiscountFactorsDecoratedForward;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;


/**
 * Tests {@link RatesProviderDecoratedForward}.
 */
@Test
public class RatesProviderDecoratedForwardTest {

  private static final LocalDate DATE_VAL = date(2015, 6, 4);
  private static final LocalDate DATE_FWD = LocalDate.of(2015, 6, 5);
  private static final RatesProvider PROVIDER_START = RatesProviderDataSets.multiGbpUsd(DATE_VAL);
  private static final RatesProvider PROVIDER_FWD = RatesProviderDecoratedForward.of(PROVIDER_START, DATE_FWD);
  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final CurrencyPair GBP_USD = CurrencyPair.of(GBP, USD);

  private static final double TOLERANCE = 1.0E-8;
  
  public void fwd_values_df() {
    int nbDate = 10;
    Period step = Period.ofMonths(5);
    for (int i = 0; i < nbDate; i++) {
      LocalDate testDate = DATE_FWD.plus(step.multipliedBy(i));
      double dfComputed = PROVIDER_FWD.discountFactor(USD, testDate);
      DiscountFactors dscUsd = DiscountFactorsDecoratedForward.of(PROVIDER_START.discountFactors(USD), DATE_FWD);
      double dfExpected = dscUsd.discountFactor(testDate);
      assertEquals(dfComputed, dfExpected, TOLERANCE, "date " + i);
    }
  }
  
  public void fwd_values_ibor() {
    int nbDate = 10;
    Period step = Period.ofMonths(5);
    for (int i = 0; i < nbDate; i++) {
      LocalDate testDate = DATE_FWD.plus(step.multipliedBy(i));
      IborIndexObservation obs = IborIndexObservation.of(USD_LIBOR_3M, testDate, REF_DATA);
      double iborComputed = PROVIDER_FWD.iborIndexRates(USD_LIBOR_3M).rate(obs);
      double iborExpected = PROVIDER_START.iborIndexRates(USD_LIBOR_3M).rate(obs);
      assertEquals(iborComputed, iborExpected, TOLERANCE, "date " + i);
    }
  }
  
  public void fwd_values_on() {
    int nbDate = 10;
    Period step = Period.ofMonths(5);
    for (int i = 0; i < nbDate; i++) {
      LocalDate testDate = DATE_FWD.plus(step.multipliedBy(i));
      OvernightIndexObservation obs = OvernightIndexObservation.of(USD_FED_FUND, testDate, REF_DATA);
      double onComputed = PROVIDER_FWD.overnightIndexRates(USD_FED_FUND).rate(obs);
      double onExpected = PROVIDER_START.overnightIndexRates(USD_FED_FUND).rate(obs);
      assertEquals(onComputed, onExpected, TOLERANCE, "date " + i);
    }
  }
  
  public void fwd_values_fx() {
    int nbDate = 10;
    Period step = Period.ofMonths(5);
    for (int i = 0; i < nbDate; i++) {
      LocalDate testDate = DATE_FWD.plus(step.multipliedBy(i));
      double fxComputed = PROVIDER_FWD.fxForwardRates(GBP_USD).rate(GBP, testDate);
      double fxExpected = PROVIDER_START.fxForwardRates(GBP_USD).rate(GBP, testDate);
      assertEquals(fxComputed, fxExpected, TOLERANCE, "date " + i);
    }
  }

  public void fx_today() {
    double fxComputed = PROVIDER_FWD.fxRate(GBP, USD);
    double fxExpected = PROVIDER_START.fxForwardRates(CurrencyPair.of(GBP, USD)).rate(GBP, DATE_FWD);
    assertEquals(fxComputed, fxExpected, TOLERANCE);
  }
  
}
