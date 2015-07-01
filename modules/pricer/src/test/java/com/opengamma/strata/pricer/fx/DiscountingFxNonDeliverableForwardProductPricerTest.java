/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.ImmutableFxIndex;
import com.opengamma.strata.finance.fx.Fx;
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

  private static final FxMatrix FX_MATRIX = RatesProviderFxDataSets.fxMatrix();
  private static final RatesProvider PROVIDER = RatesProviderFxDataSets.createProvider();
  private static final Currency KRW = Currency.KRW;
  private static final Currency USD = Currency.USD;
  private static final LocalDate PAYMENT_DATE = RatesProviderFxDataSets.VAL_DATE_2014_01_22.plusWeeks(8);
  private static final LocalDate PAYMENT_DATE_PAST = RatesProviderFxDataSets.VAL_DATE_2014_01_22.minusDays(1);
  private static final double NOMINAL_USD = 100_000_000;
  private static final CurrencyAmount CURRENCY_NOTIONAL = CurrencyAmount.of(USD, NOMINAL_USD);
  private static final double FX_RATE = 1123.45;
  private static final FxIndex INDEX = ImmutableFxIndex.builder()
      .name("USD/KRW")
      .currencyPair(CurrencyPair.of(USD, KRW))
      .fixingCalendar(HolidayCalendars.USNY)
      .maturityDateOffset(DaysAdjustment.ofBusinessDays(2, HolidayCalendars.USNY))
      .build();
  private static final FxNonDeliverableForward NDF =
      FxNonDeliverableForward.builder()
          .settlementCurrencyNotional(CURRENCY_NOTIONAL)
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

  public void test_presentValue_ended() {
    FxNonDeliverableForward ndf =
        FxNonDeliverableForward.builder()
            .settlementCurrencyNotional(CURRENCY_NOTIONAL)
            .agreedFxRate(FxRate.of(USD, KRW, FX_RATE))
            .paymentDate(PAYMENT_DATE_PAST)
            .index(INDEX)
            .build();
    CurrencyAmount computed = PRICER.presentValue(ndf, PROVIDER);
    assertEquals(computed.getAmount(), 0d);
  }

  public void test_forwardValue() {
    FxRate computed = PRICER.forwardFxRate(NDF, PROVIDER);
    FxNonDeliverableForward ndfFwd =
        FxNonDeliverableForward.builder()
            .settlementCurrencyNotional(CURRENCY_NOTIONAL)
            .agreedFxRate(computed)
            .paymentDate(PAYMENT_DATE)
            .index(INDEX)
            .build();
    CurrencyAmount computedFwd = PRICER.presentValue(ndfFwd, PROVIDER);
    assertEquals(computedFwd.getAmount(), 0d, NOMINAL_USD * TOL);
  }

  public void test_presentValueSensitivity() {
    PointSensitivities point = PRICER.presentValueSensitivity(NDF, PROVIDER);
    CurveCurrencyParameterSensitivities computed = PROVIDER.curveParameterSensitivity(point);
    CurveCurrencyParameterSensitivities expected = CAL_FD.sensitivity(
        (ImmutableRatesProvider) PROVIDER, (p) -> PRICER.presentValue(NDF, (p)));
    assertTrue(computed.equalWithTolerance(expected, NOMINAL_USD * EPS_FD));
  }

  public void test_presentValueSensitivity_ended() {
    FxNonDeliverableForward ndf =
        FxNonDeliverableForward.builder()
            .settlementCurrencyNotional(CURRENCY_NOTIONAL)
            .agreedFxRate(FxRate.of(USD, KRW, FX_RATE))
            .paymentDate(PAYMENT_DATE_PAST)
            .index(INDEX)
            .build();
    PointSensitivities computed = PRICER.presentValueSensitivity(ndf, PROVIDER);
    assertEquals(computed, PointSensitivities.empty());
  }

  //-------------------------------------------------------------------------
  private static final Fx FOREX = Fx
      .of(CurrencyAmount.of(USD, NOMINAL_USD), FxRate.of(USD, KRW, FX_RATE), PAYMENT_DATE);
  private static final DiscountingFxProductPricer PRICER_FX = DiscountingFxProductPricer.DEFAULT;
  
  // Checks that the NDF present value is coherent with the standard FX forward present value.
  public void presentValueVsForex() {
    CurrencyAmount pvNDF = PRICER.presentValue(NDF, PROVIDER);
    MultiCurrencyAmount pvFX = PRICER_FX.presentValue(FOREX, PROVIDER);
    assertEquals(
        pvNDF.getAmount(),
        pvFX.getAmount(USD).getAmount() + pvFX.getAmount(KRW).getAmount() * FX_MATRIX.fxRate(KRW, USD),
        NOMINAL_USD *TOL);
  }

  // Checks that the NDF forward rate is coherent with the standard FX forward present value.
  public void forwardRateVsForex() {
    FxRate fwdNDF = PRICER.forwardFxRate(NDF, PROVIDER);
    FxRate fwdFX = PRICER_FX.forwardFxRate(FOREX, PROVIDER);
    assertEquals(fwdNDF, fwdFX);
  }

  // Checks that the NDF present value sensitivity is coherent with the standard FX forward present value.
  public void presentValueCurveSensitivityVsForex() {
    PointSensitivities pvcsNDF = PRICER.presentValueSensitivity(NDF, PROVIDER).normalized();
    CurveCurrencyParameterSensitivities sensiNDF =   PROVIDER.curveParameterSensitivity(pvcsNDF);
    PointSensitivities pvcsFX = PRICER_FX.presentValueSensitivity(FOREX, PROVIDER).normalized();
    CurveCurrencyParameterSensitivities sensiFX = PROVIDER.curveParameterSensitivity(pvcsFX);
    assertTrue(sensiNDF.equalWithTolerance(sensiFX.convertedTo(USD, PROVIDER), NOMINAL_USD * TOL));
  }
}
