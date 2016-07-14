/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.pricer.fx.RatesProviderFxDataSets;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.fx.ResolvedFxSingle;
import com.opengamma.strata.product.fxopt.ResolvedFxSingleBarrierOption;
import com.opengamma.strata.product.fxopt.ResolvedFxVanillaOption;
import com.opengamma.strata.product.option.BarrierType;
import com.opengamma.strata.product.option.KnockType;
import com.opengamma.strata.product.option.SimpleConstantContinuousBarrier;

/**
 * Test {@link ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer}.
 */
@Test
public class ImpliedTrinomialTreeFxSingleBarrierOptionProductPricerTest {

  private static final ZoneId ZONE = ZoneId.of("Z");
  private static final LocalDate VAL_DATE = LocalDate.of(2011, 6, 13);
  private static final ZonedDateTime VAL_DATETIME = VAL_DATE.atStartOfDay(ZONE);
  private static final LocalDate PAY_DATE = LocalDate.of(2014, 9, 15);
  private static final LocalDate EXPIRY_DATE = LocalDate.of(2014, 9, 15);
  private static final ZonedDateTime EXPIRY_DATETIME = EXPIRY_DATE.atStartOfDay(ZONE);
  // providers - flat
  private static final ImmutableRatesProvider RATE_PROVIDER_FLAT =
      RatesProviderFxDataSets.createProviderEurUsdFlat(VAL_DATE);
  private static final BlackFxOptionSmileVolatilities VOLS_FLAT =
      FxVolatilitySmileDataSet.createVolatilitySmileProvider5FlatFlat(VAL_DATETIME);
  // providers
  private static final ImmutableRatesProvider RATE_PROVIDER =
      RatesProviderFxDataSets.createProviderEURUSD(VAL_DATE);
  private static final BlackFxOptionSmileVolatilities VOLS =
      FxVolatilitySmileDataSet.createVolatilitySmileProvider5(VAL_DATETIME);
  // providers - after maturity
  private static final ImmutableRatesProvider RATE_PROVIDER_AFTER =
      RatesProviderFxDataSets.createProviderEURUSD(EXPIRY_DATE.plusDays(1));
  private static final BlackFxOptionSmileVolatilities VOLS_AFTER =
      FxVolatilitySmileDataSet.createVolatilitySmileProvider5(EXPIRY_DATETIME.plusDays(1));

  private static final double NOTIONAL = 100_000_000d;
  private static final double LEVEL_LOW = 1.25;
  private static final double LEVEL_HIGH = 1.6;
  private static final SimpleConstantContinuousBarrier BARRIER_DKO =
      SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_OUT, LEVEL_LOW);
  private static final SimpleConstantContinuousBarrier BARRIER_UKI =
      SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_IN, LEVEL_HIGH);
  private static final double REBATE_AMOUNT = 5_000_000d; // large rebate for testing
  private static final CurrencyAmount REBATE = CurrencyAmount.of(USD, REBATE_AMOUNT);
  private static final CurrencyAmount REBATE_BASE = CurrencyAmount.of(EUR, REBATE_AMOUNT);
  private static final double STRIKE_RATE_HIGH = 1.45;
  private static final double STRIKE_RATE_LOW = 1.35;
  // call
  private static final CurrencyAmount EUR_AMOUNT_REC = CurrencyAmount.of(EUR, NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT_PAY = CurrencyAmount.of(USD, -NOTIONAL * STRIKE_RATE_LOW);
  private static final ResolvedFxSingle FX_PRODUCT = ResolvedFxSingle.of(EUR_AMOUNT_REC, USD_AMOUNT_PAY, PAY_DATE);
  private static final ResolvedFxVanillaOption CALL = ResolvedFxVanillaOption.builder()
      .longShort(LongShort.LONG)
      .expiry(EXPIRY_DATETIME)
      .underlying(FX_PRODUCT)
      .build();
  private static final CurrencyAmount EUR_AMOUNT_PAY = CurrencyAmount.of(EUR, -NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT_REC = CurrencyAmount.of(USD, NOTIONAL * STRIKE_RATE_HIGH);
  private static final ResolvedFxSingle FX_PRODUCT_INV = ResolvedFxSingle.of(EUR_AMOUNT_PAY, USD_AMOUNT_REC, PAY_DATE);
  private static final ResolvedFxVanillaOption PUT = ResolvedFxVanillaOption.builder()
      .longShort(LongShort.SHORT)
      .expiry(EXPIRY_DATETIME)
      .underlying(FX_PRODUCT_INV)
      .build();
  private static final ResolvedFxSingleBarrierOption CALL_DKO =
      ResolvedFxSingleBarrierOption.of(CALL, BARRIER_DKO);
  private static final ResolvedFxSingleBarrierOption CALL_UKI_C =
      ResolvedFxSingleBarrierOption.of(CALL, BARRIER_UKI, REBATE);
  // pricers and pre-calibration
  private static final ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer PRICER_39 =
      new ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer(39);
  private static final RecombiningTrinomialTreeData DATA_39 =
      PRICER_39.getCalibrator().calibrateTrinomialTree(CALL, RATE_PROVIDER, VOLS);
  private static final ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer PRICER_70 =
      new ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer(70);
  private static final RecombiningTrinomialTreeData DATA_70_FLAT =
      PRICER_70.getCalibrator().calibrateTrinomialTree(CALL, RATE_PROVIDER_FLAT, VOLS_FLAT);
  private static final BlackFxSingleBarrierOptionProductPricer BLACK_PRICER = BlackFxSingleBarrierOptionProductPricer.DEFAULT;
  private static final BlackFxVanillaOptionProductPricer VANILLA_PRICER = BlackFxVanillaOptionProductPricer.DEFAULT;

  @Test
  public void test_black() {
    double tol = 1.0e-2;
    for (int i = 0; i < 11; ++i) {
      // up barrier
      double lowerBarrier = 1.1 + 0.025 * i;
      SimpleConstantContinuousBarrier dko =
          SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_OUT, lowerBarrier);
      ResolvedFxSingleBarrierOption optionDko = ResolvedFxSingleBarrierOption.of(CALL, dko);
      double priceDkoBlack = BLACK_PRICER.price(optionDko, RATE_PROVIDER_FLAT, VOLS_FLAT);
      double priceDko = PRICER_70.price(optionDko, RATE_PROVIDER_FLAT, VOLS_FLAT, DATA_70_FLAT);
      assertEqualsRelative(priceDko, priceDkoBlack, tol);
      SimpleConstantContinuousBarrier dki =
          SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, lowerBarrier);
      ResolvedFxSingleBarrierOption optionDki = ResolvedFxSingleBarrierOption.of(CALL, dki);
      double priceDkiBlack = BLACK_PRICER.price(optionDki, RATE_PROVIDER_FLAT, VOLS_FLAT);
      double priceDki = PRICER_70.price(optionDki, RATE_PROVIDER_FLAT, VOLS_FLAT, DATA_70_FLAT);
      assertEqualsRelative(priceDki, priceDkiBlack, tol);
      // down barrier
      double higherBarrier = 1.45 + 0.025 * i;
      SimpleConstantContinuousBarrier uko =
          SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_OUT, higherBarrier);
      ResolvedFxSingleBarrierOption optionUko = ResolvedFxSingleBarrierOption.of(CALL, uko);
      double priceUkoBlack = BLACK_PRICER.price(optionUko, RATE_PROVIDER_FLAT, VOLS_FLAT);
      double priceUko = PRICER_70.price(optionUko, RATE_PROVIDER_FLAT, VOLS_FLAT, DATA_70_FLAT);
      assertEqualsRelative(priceUko, priceUkoBlack, tol);
      SimpleConstantContinuousBarrier uki =
          SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_IN, higherBarrier);
      ResolvedFxSingleBarrierOption optionUki = ResolvedFxSingleBarrierOption.of(CALL, uki);
      double priceUkiBlack = BLACK_PRICER.price(optionUki, RATE_PROVIDER_FLAT, VOLS_FLAT);
      double priceUki = PRICER_70.price(optionUki, RATE_PROVIDER_FLAT, VOLS_FLAT, DATA_70_FLAT);
      assertEqualsRelative(priceUki, priceUkiBlack, tol);
    }
  }

  @Test
  public void test_black_currencyExposure() {
    double tol = 7.0e-2; // large tol due to approximated delta
    for (int i = 0; i < 8; ++i) {
      // up barrier
      double lowerBarrier = 1.1 + 0.025 * i;
      SimpleConstantContinuousBarrier dko =
          SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_OUT, lowerBarrier);
      ResolvedFxSingleBarrierOption optionDko = ResolvedFxSingleBarrierOption.of(CALL, dko, REBATE_BASE);
      MultiCurrencyAmount ceDkoBlack = BLACK_PRICER.currencyExposure(optionDko, RATE_PROVIDER_FLAT, VOLS_FLAT);
      MultiCurrencyAmount ceDko =
          PRICER_70.currencyExposure(optionDko, RATE_PROVIDER_FLAT, VOLS_FLAT, DATA_70_FLAT);
      assertEquals(ceDko.getAmount(EUR).getAmount(), ceDkoBlack.getAmount(EUR).getAmount(), NOTIONAL * tol);
      assertEquals(ceDko.getAmount(USD).getAmount(), ceDkoBlack.getAmount(USD).getAmount(), NOTIONAL * tol);
      SimpleConstantContinuousBarrier dki =
          SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, lowerBarrier);
      ResolvedFxSingleBarrierOption optionDki = ResolvedFxSingleBarrierOption.of(CALL, dki, REBATE);
      MultiCurrencyAmount ceDkiBlack = BLACK_PRICER.currencyExposure(optionDki, RATE_PROVIDER_FLAT, VOLS_FLAT);
      MultiCurrencyAmount ceDki =
          PRICER_70.currencyExposure(optionDki, RATE_PROVIDER_FLAT, VOLS_FLAT, DATA_70_FLAT);
      assertEquals(ceDki.getAmount(EUR).getAmount(), ceDkiBlack.getAmount(EUR).getAmount(), NOTIONAL * tol);
      assertEquals(ceDki.getAmount(USD).getAmount(), ceDkiBlack.getAmount(USD).getAmount(), NOTIONAL * tol);
      // down barrier
      double higherBarrier = 1.45 + 0.025 * i;
      SimpleConstantContinuousBarrier uko =
          SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_OUT, higherBarrier);
      ResolvedFxSingleBarrierOption optionUko = ResolvedFxSingleBarrierOption.of(CALL, uko, REBATE);
      MultiCurrencyAmount ceUkoBlack = BLACK_PRICER.currencyExposure(optionUko, RATE_PROVIDER_FLAT, VOLS_FLAT);
      MultiCurrencyAmount ceUko =
          PRICER_70.currencyExposure(optionUko, RATE_PROVIDER_FLAT, VOLS_FLAT, DATA_70_FLAT);
      assertEquals(ceUko.getAmount(EUR).getAmount(), ceUkoBlack.getAmount(EUR).getAmount(), NOTIONAL * tol);
      assertEquals(ceUko.getAmount(USD).getAmount(), ceUkoBlack.getAmount(USD).getAmount(), NOTIONAL * tol);
      SimpleConstantContinuousBarrier uki =
          SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_IN, higherBarrier);
      ResolvedFxSingleBarrierOption optionUki = ResolvedFxSingleBarrierOption.of(CALL, uki, REBATE_BASE);
      MultiCurrencyAmount ceUkiBlack = BLACK_PRICER.currencyExposure(optionUki, RATE_PROVIDER_FLAT, VOLS_FLAT);
      MultiCurrencyAmount ceUki =
          PRICER_70.currencyExposure(optionUki, RATE_PROVIDER_FLAT, VOLS_FLAT, DATA_70_FLAT);
      assertEquals(ceUki.getAmount(EUR).getAmount(), ceUkiBlack.getAmount(EUR).getAmount(), NOTIONAL * tol);
      assertEquals(ceUki.getAmount(USD).getAmount(), ceUkiBlack.getAmount(USD).getAmount(), NOTIONAL * tol);
    }
  }

  @Test
  public void test_black_rebate() {
    double tol = 1.5e-2;
    for (int i = 0; i < 11; ++i) {
      // up barrier
      double lowerBarrier = 1.1 + 0.025 * i;
      SimpleConstantContinuousBarrier dko =
          SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_OUT, lowerBarrier);
      ResolvedFxSingleBarrierOption optionDko = ResolvedFxSingleBarrierOption.of(PUT, dko, REBATE_BASE);
      double priceDkoBlack = BLACK_PRICER.price(optionDko, RATE_PROVIDER_FLAT, VOLS_FLAT);
      double priceDko = PRICER_70.price(optionDko, RATE_PROVIDER_FLAT, VOLS_FLAT, DATA_70_FLAT);
      assertEqualsRelative(priceDko, priceDkoBlack, tol);
      SimpleConstantContinuousBarrier dki =
          SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, lowerBarrier);
      ResolvedFxSingleBarrierOption optionDki = ResolvedFxSingleBarrierOption.of(PUT, dki, REBATE_BASE);
      double priceDkiBlack = BLACK_PRICER.price(optionDki, RATE_PROVIDER_FLAT, VOLS_FLAT);
      double priceDki = PRICER_70.price(optionDki, RATE_PROVIDER_FLAT, VOLS_FLAT, DATA_70_FLAT);
      assertEqualsRelative(priceDki, priceDkiBlack, tol);
      // down barrier
      double higherBarrier = 1.45 + 0.025 * i;
      SimpleConstantContinuousBarrier uko =
          SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_OUT, higherBarrier);
      ResolvedFxSingleBarrierOption optionUko = ResolvedFxSingleBarrierOption.of(PUT, uko, REBATE);
      double priceUkoBlack = BLACK_PRICER.price(optionUko, RATE_PROVIDER_FLAT, VOLS_FLAT);
      double priceUko = PRICER_70.price(optionUko, RATE_PROVIDER_FLAT, VOLS_FLAT, DATA_70_FLAT);
      assertEqualsRelative(priceUko, priceUkoBlack, tol);
      SimpleConstantContinuousBarrier uki =
          SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_IN, higherBarrier);
      ResolvedFxSingleBarrierOption optionUki = ResolvedFxSingleBarrierOption.of(PUT, uki, REBATE);
      double priceUkiBlack = BLACK_PRICER.price(optionUki, RATE_PROVIDER_FLAT, VOLS_FLAT);
      double priceUki = PRICER_70.price(optionUki, RATE_PROVIDER_FLAT, VOLS_FLAT, DATA_70_FLAT);
      assertEqualsRelative(priceUki, priceUkiBlack, tol);
    }
  }

  @Test
  public void test_monotonicity() {
    double priceDkoPrev = 100d;
    double priceDkiPrev = 0d;
    double priceUkoPrev = 0d;
    double priceUkiPrev = 100d;
    for (int i = 0; i < 50; ++i) {
      // up barrier
      double lowerBarrier = 1.1 + 0.006 * i;
      SimpleConstantContinuousBarrier dko =
          SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_OUT, lowerBarrier);
      ResolvedFxSingleBarrierOption optionDko = ResolvedFxSingleBarrierOption.of(CALL, dko);
      double priceDko = PRICER_39.price(optionDko, RATE_PROVIDER, VOLS, DATA_39);
      SimpleConstantContinuousBarrier dki =
          SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, lowerBarrier);
      ResolvedFxSingleBarrierOption optionDki = ResolvedFxSingleBarrierOption.of(CALL, dki);
      double priceDki = PRICER_39.price(optionDki, RATE_PROVIDER, VOLS, DATA_39);
      // down barrier
      double higherBarrier = 1.4 + 0.006 * (i + 1);
      SimpleConstantContinuousBarrier uko =
          SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_OUT, higherBarrier);
      ResolvedFxSingleBarrierOption optionUko = ResolvedFxSingleBarrierOption.of(CALL, uko);
      double priceUko = PRICER_39.price(optionUko, RATE_PROVIDER, VOLS, DATA_39);
      SimpleConstantContinuousBarrier uki =
          SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_IN, higherBarrier);
      ResolvedFxSingleBarrierOption optionUki = ResolvedFxSingleBarrierOption.of(CALL, uki);
      double priceUki = PRICER_39.price(optionUki, RATE_PROVIDER, VOLS, DATA_39);
      assertTrue(priceDkoPrev > priceDko);
      assertTrue(priceDkiPrev < priceDki);
      assertTrue(priceUkoPrev < priceUko);
      assertTrue(priceUkiPrev > priceUki);
      priceDkoPrev = priceDko;
      priceDkiPrev = priceDki;
      priceUkoPrev = priceUko;
      priceUkiPrev = priceUki;
    }
  }

  @Test
  public void test_inOutParity() {
    double tol = 1.0e-2;
    double callPrice = VANILLA_PRICER.price(CALL, RATE_PROVIDER, VOLS);
    double putPrice = VANILLA_PRICER.price(PUT, RATE_PROVIDER, VOLS);
    for (int i = 0; i < 11; ++i) {
      // up barrier
      double lowerBarrier = 1.1 + 0.025 * i;
      SimpleConstantContinuousBarrier dko =
          SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_OUT, lowerBarrier);
      ResolvedFxSingleBarrierOption callDko = ResolvedFxSingleBarrierOption.of(CALL, dko);
      double priceCallDko = PRICER_39.price(callDko, RATE_PROVIDER, VOLS, DATA_39);
      ResolvedFxSingleBarrierOption putDko = ResolvedFxSingleBarrierOption.of(PUT, dko);
      double pricePutDko = PRICER_39.price(putDko, RATE_PROVIDER, VOLS, DATA_39);
      SimpleConstantContinuousBarrier dki =
          SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, lowerBarrier);
      ResolvedFxSingleBarrierOption callDki = ResolvedFxSingleBarrierOption.of(CALL, dki);
      double priceCallDki = PRICER_39.price(callDki, RATE_PROVIDER, VOLS, DATA_39);
      ResolvedFxSingleBarrierOption putDki = ResolvedFxSingleBarrierOption.of(PUT, dki);
      double pricePutDki = PRICER_39.price(putDki, RATE_PROVIDER, VOLS, DATA_39);
      assertEqualsRelative(priceCallDko + priceCallDki, callPrice, tol);
      assertEqualsRelative(pricePutDko + pricePutDki, putPrice, tol);
      // down barrier
      double higherBarrier = 1.45 + 0.025 * i;
      SimpleConstantContinuousBarrier uko =
          SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_OUT, higherBarrier);
      ResolvedFxSingleBarrierOption callUko = ResolvedFxSingleBarrierOption.of(CALL, uko);
      double priceCallUko = PRICER_39.price(callUko, RATE_PROVIDER, VOLS, DATA_39);
      ResolvedFxSingleBarrierOption putUko = ResolvedFxSingleBarrierOption.of(PUT, uko);
      double pricePutUko = PRICER_39.price(putUko, RATE_PROVIDER, VOLS, DATA_39);
      SimpleConstantContinuousBarrier uki =
          SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_IN, higherBarrier);
      ResolvedFxSingleBarrierOption callUki = ResolvedFxSingleBarrierOption.of(CALL, uki);
      double priceCallUki = PRICER_39.price(callUki, RATE_PROVIDER, VOLS, DATA_39);
      ResolvedFxSingleBarrierOption putUki = ResolvedFxSingleBarrierOption.of(PUT, uki);
      double pricePutUki = PRICER_39.price(putUki, RATE_PROVIDER, VOLS, DATA_39);
      assertEqualsRelative(priceCallUko + priceCallUki, callPrice, tol);
      assertEqualsRelative(pricePutUko + pricePutUki, putPrice, tol);
    }
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity() {
    ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer pricer =
        new ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer(21);
    CurrencyParameterSensitivities computed =
        pricer.presentValueRatesSensitivity(CALL_UKI_C, RATE_PROVIDER, VOLS);
    RatesFiniteDifferenceSensitivityCalculator calc = new RatesFiniteDifferenceSensitivityCalculator(1.0e-5);
    CurrencyParameterSensitivities expected =
        calc.sensitivity(RATE_PROVIDER, p -> pricer.presentValue(CALL_UKI_C, p, VOLS));
    assertTrue(computed.equalWithTolerance(expected, 1.0e-13));
  }

  //-------------------------------------------------------------------------
  public void test_withData() {
    ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer pricer =
        new ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer(5);
    RecombiningTrinomialTreeData data =
        pricer.getCalibrator().calibrateTrinomialTree(CALL_DKO.getUnderlyingOption(), RATE_PROVIDER, VOLS);
    double price = pricer.price(CALL_UKI_C, RATE_PROVIDER, VOLS);
    double priceWithData = pricer.price(CALL_UKI_C, RATE_PROVIDER, VOLS, data);
    assertEquals(price, priceWithData);
    CurrencyAmount pv = pricer.presentValue(CALL_DKO, RATE_PROVIDER, VOLS);
    CurrencyAmount pvWithData = pricer.presentValue(CALL_DKO, RATE_PROVIDER, VOLS, data);
    assertEquals(pv, pvWithData);
    MultiCurrencyAmount ce = pricer.currencyExposure(CALL_UKI_C, RATE_PROVIDER, VOLS);
    MultiCurrencyAmount ceWithData = pricer.currencyExposure(CALL_UKI_C, RATE_PROVIDER, VOLS, data);
    assertEquals(ce, ceWithData);
  }

  public void test_expired_calibration() {
    assertThrowsIllegalArg(() -> PRICER_39.getCalibrator().calibrateTrinomialTree(CALL_DKO.getUnderlyingOption(),
        RATE_PROVIDER_AFTER, VOLS_AFTER));
    // pricing also fails because trinomial data can not be obtained
    assertThrowsIllegalArg(() -> PRICER_39.price(CALL_DKO, RATE_PROVIDER_AFTER, VOLS_AFTER));
    assertThrowsIllegalArg(() -> PRICER_39.presentValue(CALL_DKO, RATE_PROVIDER_AFTER, VOLS_AFTER));
    assertThrowsIllegalArg(() -> PRICER_39.currencyExposure(CALL_DKO, RATE_PROVIDER_AFTER, VOLS_AFTER));
  }

  public void test_dataMismatch() {
    assertThrowsIllegalArg(() -> PRICER_70.presentValueRatesSensitivity(CALL_DKO, RATE_PROVIDER,
        VOLS, DATA_39));
  }

  //-------------------------------------------------------------------------
  private void assertEqualsRelative(double computed, double expected, double relTol) {
    assertEquals(computed, expected, Math.max(1d, Math.abs(expected)) * relTol);
  }

}
