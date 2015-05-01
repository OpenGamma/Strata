/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static org.testng.Assert.assertEquals;

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
import com.opengamma.strata.finance.fx.FxForward;
import com.opengamma.strata.finance.fx.FxNonDeliverableForward;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Test {@link DiscountingFxNonDeliverableForwardProductPricerBeta}.
 */
@Test
public class DiscountingFxNonDeliverableForwardProductPricerBetaTest {
  // copied/modified from ForexNonDeliverableForwardDiscountingMethodTest
  // TODO: use FxIndex!

  private static final FxMatrix FX_MATRIX = RatesProviderFxDataSets.fxMatrix();
  private static final RatesProvider PROVIDER = RatesProviderFxDataSets.createProvider();

  private static final Currency KRW = Currency.KRW;
  private static final Currency USD = Currency.USD;
  private static final LocalDate FIXING_DATE = LocalDate.of(2012, 5, 2);
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2012, 5, 4);
  private static final double NOMINAL_USD = 100_000_000;
  private static final double FX_RATE = 1123.45;
  private static final FxIndex INDEX = ImmutableFxIndex.builder()
      .name("Testing USD/KRW")
      .currencyPair(CurrencyPair.of(USD, KRW))
      .fixingCalendar(HolidayCalendars.USNY)
      .maturityDateOffset(DaysAdjustment.ofBusinessDays(2, HolidayCalendars.USNY))
      .build();

  private static final FxNonDeliverableForward NDF =
      FxNonDeliverableForward.builder()
          .settlementCurrency(USD)
          .notional(CurrencyAmount.of(USD, NOMINAL_USD))
          .agreedFxRate(FxRate.of(USD, KRW, FX_RATE))
          .valueDate(PAYMENT_DATE)
          .index(INDEX)
          .fixingDate(FIXING_DATE)
          .build();

  private static final FxForward FWD = FxForward.of(CurrencyAmount.of(USD, NOMINAL_USD), KRW, FX_RATE, PAYMENT_DATE);

  private static final DiscountingFxNonDeliverableForwardProductPricerBeta PRICER_NDF =
      DiscountingFxNonDeliverableForwardProductPricerBeta.DEFAULT;
  private static final DiscountingFxProductPricerBeta PRICER_FWD = DiscountingFxProductPricerBeta.DEFAULT;

  private static final double TOLERANCE_PV = 1.0E-2;

  //-------------------------------------------------------------------------
  public void currencyExposure() {
    MultiCurrencyAmount ce = PRICER_NDF.currencyExposure(NDF, PROVIDER);
    double df1 = PROVIDER.discountFactor(KRW, NDF.getValueDate());
    double df2 = PROVIDER.discountFactor(USD, NDF.getValueDate());
    double ce1 = -NOMINAL_USD * df1 * FX_RATE;
    double ce2 = NOMINAL_USD * df2;
    assertEquals(ce.getAmount(KRW).getAmount(), ce1, TOLERANCE_PV);
    assertEquals(ce.getAmount(USD).getAmount(), ce2, TOLERANCE_PV);
  }

  // Checks that the NDF currency exposure is the same as the standard FX forward currency exposure
  public void currencyExposureVsForex() {
    MultiCurrencyAmount ceNDF = PRICER_NDF.currencyExposure(NDF, PROVIDER);
    MultiCurrencyAmount ceFX = PRICER_FWD.currencyExposure(FWD, PROVIDER);
    assertEquals(ceNDF, ceFX);
  }

  //-------------------------------------------------------------------------
  public void presentValue() {
    MultiCurrencyAmount ce = PRICER_NDF.currencyExposure(NDF, PROVIDER);
    CurrencyAmount pv = PRICER_NDF.presentValue(NDF, PROVIDER);
    double pvExpected = ce.getAmount(KRW).getAmount() * FX_MATRIX.fxRate(KRW, USD) + ce.getAmount(USD).getAmount();
    assertEquals(pv.getAmount(), pvExpected, TOLERANCE_PV);
  }

  // Checks that the NDF present value is coherent with the standard FX forward present value.
  public void presentValueVsForex() {
    CurrencyAmount pvNDF = PRICER_NDF.presentValue(NDF, PROVIDER);
    MultiCurrencyAmount pvFX = PRICER_FWD.presentValue(FWD, PROVIDER);
    assertEquals(
        pvNDF.getAmount(),
        pvFX.getAmount(USD).getAmount() + pvFX.getAmount(KRW).getAmount() * FX_MATRIX.fxRate(KRW, USD),
        TOLERANCE_PV);
  }

  // Checks that the NDF forward rate is coherent with the standard FX forward present value.
  public void forwardRateVsForex() {
    FxRate fwdNDF = PRICER_NDF.forwardFxRate(NDF, PROVIDER);
    FxRate fwdFX = PRICER_FWD.forwardFxRate(FWD, PROVIDER);
    assertEquals(fwdNDF.getRate(), fwdFX.getRate(), TOLERANCE_PV);
  }


//  // Tests the present value curve sensitivity using the Forex instrument curve sensitivity as reference.
//  public void presentValueCurveSensitivity() {
//    PointSensitivities pvcsNDF = METHOD_NDF.presentValueSensitivity(NDF, PROVIDER).normalized();
//    PointSensitivities pvcsFX = METHOD_FX.presentValueSensitivity(FOREX, PROVIDER).normalized();
//    PointSensitivities pvcsFXConverted = pvcsFX.converted(USD, FX_MATRIX).normalized();
//    AssertSensitivityObjects.assertEquals("ForexNonDeliverableForwardDiscountingMethod: presentValueCurveSensitivity",
//        pvcsFXConverted, pvcsNDF, TOLERANCE_PV);
//  }

}
