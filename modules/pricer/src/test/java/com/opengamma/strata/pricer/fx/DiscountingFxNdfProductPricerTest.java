/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.FxIndexObservation;
import com.opengamma.strata.basics.index.ImmutableFxIndex;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.fx.ResolvedFxNdf;
import com.opengamma.strata.product.fx.ResolvedFxSingle;

/**
 * Test {@link DiscountingFxNdfProductPricer}.
 */
@Test
public class DiscountingFxNdfProductPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final FxMatrix FX_MATRIX = RatesProviderFxDataSets.fxMatrix();
  private static final RatesProvider PROVIDER = RatesProviderFxDataSets.createProvider();
  private static final Currency KRW = Currency.KRW;
  private static final Currency USD = Currency.USD;
  private static final LocalDate PAYMENT_DATE = RatesProviderFxDataSets.VAL_DATE_2014_01_22.plusWeeks(8);
  private static final LocalDate PAYMENT_DATE_PAST = RatesProviderFxDataSets.VAL_DATE_2014_01_22.minusDays(1);
  private static final double NOMINAL_USD = 100_000_000;
  private static final CurrencyAmount CURRENCY_NOTIONAL = CurrencyAmount.of(USD, NOMINAL_USD);
  private static final double FX_RATE = 1123.45;
  private static final CurrencyAmount CURRENCY_NOTIONAL_INVERSE = CurrencyAmount.of(KRW, NOMINAL_USD * FX_RATE);
  private static final FxIndex INDEX = ImmutableFxIndex.builder()
      .name("USD/KRW")
      .currencyPair(CurrencyPair.of(USD, KRW))
      .fixingCalendar(USNY)
      .maturityDateOffset(DaysAdjustment.ofBusinessDays(2, USNY))
      .build();
  private static final LocalDate FIXING_DATE = INDEX.calculateFixingFromMaturity(PAYMENT_DATE, REF_DATA);
  private static final LocalDate FIXING_DATE_PAST = INDEX.calculateFixingFromMaturity(PAYMENT_DATE_PAST, REF_DATA);

  private static final ResolvedFxNdf NDF = ResolvedFxNdf.builder()
      .settlementCurrencyNotional(CURRENCY_NOTIONAL)
      .agreedFxRate(FxRate.of(USD, KRW, FX_RATE))
      .observation(FxIndexObservation.of(INDEX, FIXING_DATE, REF_DATA))
      .paymentDate(PAYMENT_DATE)
      .build();
  private static final ResolvedFxNdf NDF_INVERSE = ResolvedFxNdf.builder()
      .settlementCurrencyNotional(CURRENCY_NOTIONAL_INVERSE)
      .agreedFxRate(FxRate.of(USD, KRW, FX_RATE))
      .observation(FxIndexObservation.of(INDEX, FIXING_DATE, REF_DATA))
      .paymentDate(PAYMENT_DATE)
      .build();

  private static final DiscountingFxNdfProductPricer PRICER = DiscountingFxNdfProductPricer.DEFAULT;
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

  public void test_presentValue_inverse() {
    CurrencyAmount computed = PRICER.presentValue(NDF_INVERSE, PROVIDER);
    double dscUsd = PROVIDER.discountFactor(USD, NDF_INVERSE.getPaymentDate());
    double dscKrw = PROVIDER.discountFactor(KRW, NDF_INVERSE.getPaymentDate());
    double expected = NOMINAL_USD * FX_RATE * (dscKrw - dscUsd * 1 / FX_RATE / PROVIDER.fxRate(CurrencyPair.of(KRW, USD)));
    assertEquals(computed.getCurrency(), KRW);
    assertEquals(computed.getAmount(), expected, NOMINAL_USD * FX_RATE * TOL);
  }

  public void test_presentValue_ended() {
    ResolvedFxNdf ndf = ResolvedFxNdf.builder()
        .settlementCurrencyNotional(CURRENCY_NOTIONAL)
        .agreedFxRate(FxRate.of(USD, KRW, FX_RATE))
        .observation(FxIndexObservation.of(INDEX, FIXING_DATE_PAST, REF_DATA))
        .paymentDate(PAYMENT_DATE_PAST)
        .build();
    CurrencyAmount computed = PRICER.presentValue(ndf, PROVIDER);
    assertEquals(computed.getAmount(), 0d);
  }

  public void test_forwardValue() {
    FxRate computed = PRICER.forwardFxRate(NDF, PROVIDER);
    ResolvedFxNdf ndfFwd = ResolvedFxNdf.builder()
        .settlementCurrencyNotional(CURRENCY_NOTIONAL)
        .agreedFxRate(computed)
        .observation(FxIndexObservation.of(INDEX, FIXING_DATE, REF_DATA))
        .paymentDate(PAYMENT_DATE)
        .build();
    CurrencyAmount computedFwd = PRICER.presentValue(ndfFwd, PROVIDER);
    assertEquals(computedFwd.getAmount(), 0d, NOMINAL_USD * TOL);
  }

  public void test_presentValueSensitivity() {
    PointSensitivities point = PRICER.presentValueSensitivity(NDF, PROVIDER);
    CurrencyParameterSensitivities computed = PROVIDER.parameterSensitivity(point);
    CurrencyParameterSensitivities expected = CAL_FD.sensitivity(PROVIDER, (p) -> PRICER.presentValue(NDF, (p)));
    assertTrue(computed.equalWithTolerance(expected, NOMINAL_USD * EPS_FD));
  }

  public void test_presentValueSensitivity_ended() {
    ResolvedFxNdf ndf = ResolvedFxNdf.builder()
        .settlementCurrencyNotional(CURRENCY_NOTIONAL)
        .agreedFxRate(FxRate.of(USD, KRW, FX_RATE))
        .observation(FxIndexObservation.of(INDEX, FIXING_DATE_PAST, REF_DATA))
        .paymentDate(PAYMENT_DATE_PAST)
        .build();
    PointSensitivities computed = PRICER.presentValueSensitivity(ndf, PROVIDER);
    assertEquals(computed, PointSensitivities.empty());
  }

  //-------------------------------------------------------------------------

  public void test_currencyExposure() {
    CurrencyAmount pv = PRICER.presentValue(NDF, PROVIDER);
    MultiCurrencyAmount ce = PRICER.currencyExposure(NDF, PROVIDER);
    CurrencyAmount ceConverted = ce.convertedTo(pv.getCurrency(), PROVIDER);
    assertEquals(pv.getAmount(), ceConverted.getAmount(), NOMINAL_USD * TOL);
  }

  public void test_currencyExposure_ended() {
    ResolvedFxNdf ndf = ResolvedFxNdf.builder()
        .settlementCurrencyNotional(CURRENCY_NOTIONAL)
        .agreedFxRate(FxRate.of(USD, KRW, FX_RATE))
        .observation(FxIndexObservation.of(INDEX, LocalDate.of(2011, 5, 2), REF_DATA))
        .paymentDate(LocalDate.of(2011, 5, 4))
        .build();
    MultiCurrencyAmount computed = PRICER.currencyExposure(ndf, PROVIDER);
    assertEquals(computed.size(), 0);
  }

  public void test_currencyExposure_from_pt_sensitivity() {
    MultiCurrencyAmount ceDirect = PRICER.currencyExposure(NDF, PROVIDER);
    PointSensitivities pts = PRICER.presentValueSensitivity(NDF, PROVIDER);
    MultiCurrencyAmount cePts = PROVIDER.currencyExposure(pts);
    CurrencyAmount cePv = PRICER.presentValue(NDF, PROVIDER);
    MultiCurrencyAmount ceExpected = cePts.plus(cePv);
    assertEquals(ceDirect.getAmount(USD).getAmount(), ceExpected.getAmount(USD).getAmount(), NOMINAL_USD * TOL);
    assertEquals(ceDirect.getAmount(KRW).getAmount(), ceExpected.getAmount(KRW).getAmount(),
        NOMINAL_USD * TOL * FX_MATRIX.fxRate(USD, KRW));
  }

  public void test_currencyExposure_from_pt_sensitivity_inverse() {
    MultiCurrencyAmount ceDirect = PRICER.currencyExposure(NDF_INVERSE, PROVIDER);
    PointSensitivities pts = PRICER.presentValueSensitivity(NDF_INVERSE, PROVIDER);
    MultiCurrencyAmount cePts = PROVIDER.currencyExposure(pts);
    CurrencyAmount cePv = PRICER.presentValue(NDF_INVERSE, PROVIDER);
    MultiCurrencyAmount ceExpected = cePts.plus(cePv);
    assertEquals(ceDirect.getAmount(USD).getAmount(), ceExpected.getAmount(USD).getAmount(), NOMINAL_USD * TOL);
    assertEquals(ceDirect.getAmount(KRW).getAmount(), ceExpected.getAmount(KRW).getAmount(),
        NOMINAL_USD * TOL * FX_MATRIX.fxRate(USD, KRW));
  }

  //-------------------------------------------------------------------------
  private static final ResolvedFxSingle FOREX =
      ResolvedFxSingle.of(CurrencyAmount.of(USD, NOMINAL_USD), FxRate.of(USD, KRW, FX_RATE), PAYMENT_DATE);
  private static final DiscountingFxSingleProductPricer PRICER_FX = DiscountingFxSingleProductPricer.DEFAULT;

  // Checks that the NDF present value is coherent with the standard FX forward present value.
  public void test_presentValueVsForex() {
    CurrencyAmount pvNDF = PRICER.presentValue(NDF, PROVIDER);
    MultiCurrencyAmount pvFX = PRICER_FX.presentValue(FOREX, PROVIDER);
    assertEquals(
        pvNDF.getAmount(),
        pvFX.getAmount(USD).getAmount() + pvFX.getAmount(KRW).getAmount() * FX_MATRIX.fxRate(KRW, USD),
        NOMINAL_USD * TOL);
  }

  // Checks that the NDF currency exposure is coherent with the standard FX forward present value.
  public void test_currencyExposureVsForex() {
    MultiCurrencyAmount pvNDF = PRICER.currencyExposure(NDF, PROVIDER);
    MultiCurrencyAmount pvFX = PRICER_FX.currencyExposure(FOREX, PROVIDER);
    assertEquals(pvNDF.getAmount(USD).getAmount(), pvFX.getAmount(USD).getAmount(), NOMINAL_USD * TOL);
    assertEquals(pvNDF.getAmount(KRW).getAmount(), pvFX.getAmount(KRW).getAmount(),
        NOMINAL_USD * TOL * FX_MATRIX.fxRate(USD, KRW));
  }

  // Checks that the NDF forward rate is coherent with the standard FX forward present value.
  public void test_forwardRateVsForex() {
    FxRate fwdNDF = PRICER.forwardFxRate(NDF, PROVIDER);
    FxRate fwdFX = PRICER_FX.forwardFxRate(FOREX, PROVIDER);
    assertEquals(fwdNDF.fxRate(fwdNDF.getPair()), fwdFX.fxRate(fwdFX.getPair()), 1e-10);
  }

  // Checks that the NDF present value sensitivity is coherent with the standard FX forward present value.
  public void test_presentValueCurveSensitivityVsForex() {
    PointSensitivities pvcsNDF = PRICER.presentValueSensitivity(NDF, PROVIDER).normalized();
    CurrencyParameterSensitivities sensiNDF = PROVIDER.parameterSensitivity(pvcsNDF);
    PointSensitivities pvcsFX = PRICER_FX.presentValueSensitivity(FOREX, PROVIDER).normalized();
    CurrencyParameterSensitivities sensiFX = PROVIDER.parameterSensitivity(pvcsFX);
    assertTrue(sensiNDF.equalWithTolerance(sensiFX.convertedTo(USD, PROVIDER), NOMINAL_USD * TOL));
  }

  //-------------------------------------------------------------------------
  public void test_currentCash_zero() {
    CurrencyAmount computed = PRICER.currentCash(NDF, PROVIDER);
    assertEquals(computed, CurrencyAmount.zero(NDF.getSettlementCurrency()));
  }

  public void test_currentCash_onPayment() {
    double rate = 1111.2;
    LocalDate paymentDate = NDF.getPaymentDate();
    RatesProvider provider = RatesProviderFxDataSets.createProvider(paymentDate, NDF.getIndex(), rate);
    CurrencyAmount computed = PRICER.currentCash(NDF, provider);
    assertEquals(computed, CurrencyAmount.of(NDF.getSettlementCurrency(), NOMINAL_USD * (1d - FX_RATE / rate)));
  }
}
