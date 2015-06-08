/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static com.opengamma.strata.basics.BuySell.BUY;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.ImmutableFxIndex;
import com.opengamma.strata.finance.fx.FxNonDeliverableForward;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;

/**
 * Test {@link DiscountingFxNonDeliverableForwardProductPricer}.
 */
@Test
public class DiscountingFxNonDeliverableForwardProductPricerTest {

  //  private static final FxMatrix FX_MATRIX = RatesProviderFxDataSets.fxMatrix();
  private static final RatesProvider PROVIDER = RatesProviderFxDataSets.createProvider();

  private static final Currency KRW = Currency.KRW;
  private static final Currency USD = Currency.USD;
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2012, 5, 4);
  private static final double NOMINAL_USD = 100_000_000;
  private static final double FX_RATE = 1123.45;
  private static final FxIndex INDEX = ImmutableFxIndex.builder()
      .name("USD/KRW")
      .currencyPair(CurrencyPair.of(USD, KRW))
      .fixingCalendar(HolidayCalendars.USNY)
      .maturityDateOffset(DaysAdjustment.ofBusinessDays(2, HolidayCalendars.USNY))
      .build();

  private static final FxNonDeliverableForward NDF =
      FxNonDeliverableForward.builder()
          .buySell(BUY)
          .settlementCurrency(USD)
          .notional(NOMINAL_USD)
          .agreedFxRate(FxRate.of(USD, KRW, FX_RATE))
          .paymentDate(PAYMENT_DATE)
          .index(INDEX)
          .build();

  private static final DiscountingFxNonDeliverableForwardProductPricer PRICER =
      DiscountingFxNonDeliverableForwardProductPricer.DEFAULT;
  private static final double TOL = 1.0E-12;
  private static final double EPS_FD = 1E-7;
  private static final RatesFiniteDifferenceSensitivityCalculator CAL_FD =
      new RatesFiniteDifferenceSensitivityCalculator(EPS_FD);

  public void test_presentValue() {
    CurrencyAmount computed = PRICER.presentValue(NDF, PROVIDER);
    double dscUsd = PROVIDER.discountFactor(USD, NDF.getPaymentDate());
    double dscKrw = PROVIDER.discountFactor(KRW, NDF.getPaymentDate());
    double expected = NOMINAL_USD * (dscUsd - dscKrw * FX_RATE / PROVIDER.fxRate(CurrencyPair.of(USD, KRW)));
    assertEquals(computed.getCurrency(), USD);
    assertEquals(computed.getAmount(), expected, NOMINAL_USD * TOL);
  }

  public void test_presentValueSensitivity() {
    PointSensitivities point = PRICER.presentValueSensitivity(NDF, PROVIDER);
    CurveCurrencyParameterSensitivities computed = PROVIDER.parameterSensitivity(point);
    CurveCurrencyParameterSensitivities expected = CAL_FD.sensitivity(
        (ImmutableRatesProvider) PROVIDER, (p) -> PRICER.presentValue(NDF, (p)));
    assertTrue(computed.equalWithTolerance(expected, NOMINAL_USD * EPS_FD));
  }
}
