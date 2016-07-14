/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.pricer.fx.RatesProviderFxDataSets;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.impl.tree.EuropeanVanillaOptionFunction;
import com.opengamma.strata.pricer.impl.tree.OptionFunction;
import com.opengamma.strata.pricer.impl.tree.TrinomialTree;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.fx.ResolvedFxSingle;
import com.opengamma.strata.product.fxopt.ResolvedFxVanillaOption;

/**
 * Test {@link ImpliedTrinomialTreeFxOptionCalibrator}.
 * <p>
 * Further tests with barrier options are in {@link ImpliedTrinomialTreeFxSingleBarrierOptionProductPricerTest}.
 */
@Test
public class ImpliedTrinomialTreeFxOptionCalibratorTest {

  private static final ZoneId ZONE = ZoneId.of("Z");
  private static final LocalDate VAL_DATE = LocalDate.of(2011, 6, 13);
  private static final ZonedDateTime VAL_DATETIME = VAL_DATE.atStartOfDay(ZONE);
  private static final LocalDate PAY_DATE = LocalDate.of(2012, 9, 15);
  private static final LocalDate EXPIRY_DATE = LocalDate.of(2012, 9, 15);
  private static final ZonedDateTime EXPIRY_DATETIME = EXPIRY_DATE.atStartOfDay(ZONE);
  // providers
  private static final BlackFxOptionSmileVolatilities VOLS =
      FxVolatilitySmileDataSet.createVolatilitySmileProvider5(VAL_DATETIME);
  private static final BlackFxOptionSmileVolatilities VOLS_MRKT =
      FxVolatilitySmileDataSet.createVolatilitySmileProvider5Market(VAL_DATETIME);
  private static final ImmutableRatesProvider RATE_PROVIDER =
      RatesProviderFxDataSets.createProviderEurUsdFlat(VAL_DATE);
  // call - for calibration
  private static final double NOTIONAL = 100_000_000d;
  private static final double STRIKE_RATE = 1.35;
  private static final CurrencyAmount EUR_AMOUNT_REC = CurrencyAmount.of(EUR, NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT_PAY = CurrencyAmount.of(USD, -NOTIONAL * STRIKE_RATE);
  private static final ResolvedFxSingle FX_PRODUCT = ResolvedFxSingle.of(EUR_AMOUNT_REC, USD_AMOUNT_PAY, PAY_DATE);
  private static final ResolvedFxVanillaOption CALL = ResolvedFxVanillaOption.builder()
      .longShort(LongShort.LONG)
      .expiry(EXPIRY_DATETIME)
      .underlying(FX_PRODUCT)
      .build();
  private static final ImpliedTrinomialTreeFxOptionCalibrator CALIB = new ImpliedTrinomialTreeFxOptionCalibrator(39);
  private static final RecombiningTrinomialTreeData TREE_DATA =
      CALIB.calibrateTrinomialTree(CALL, RATE_PROVIDER, VOLS);
  private static final RecombiningTrinomialTreeData TREE_DATA_MRKT =
      CALIB.calibrateTrinomialTree(CALL, RATE_PROVIDER, VOLS_MRKT);
  private static final TrinomialTree TREE = new TrinomialTree();

  public void test_recoverVolatility() {
    int nSteps = TREE_DATA.getNumberOfSteps();
    double spot = TREE_DATA.getSpot();
    double timeToExpiry = TREE_DATA.getTime(nSteps);
    double dfDom = RATE_PROVIDER.discountFactors(USD).discountFactor(timeToExpiry);
    double dfFor = RATE_PROVIDER.discountFactors(EUR).discountFactor(timeToExpiry);
    double forward = spot * dfFor / dfDom;
    for (int i = 0; i < 100; ++i) {
      double strike = spot * (0.8 + 0.004 * i);
      OptionFunction func = EuropeanVanillaOptionFunction.of(strike, timeToExpiry, PutCall.CALL, nSteps);
      double price = TREE.optionPrice(func, TREE_DATA);
      double impliedVol = BlackFormulaRepository.impliedVolatility(price / dfDom, forward, strike, timeToExpiry, true);
      double orgVol = VOLS.volatility(FX_PRODUCT.getCurrencyPair(), timeToExpiry, strike, forward);
      assertEquals(impliedVol, orgVol, orgVol * 0.1); // large tol
      double priceMrkt = TREE.optionPrice(func, TREE_DATA_MRKT);
      double impliedVolMrkt =
          BlackFormulaRepository.impliedVolatility(priceMrkt / dfDom, forward, strike, timeToExpiry, true);
      double orgVolMrkt = VOLS_MRKT.volatility(FX_PRODUCT.getCurrencyPair(), timeToExpiry, strike, forward);
      assertEquals(impliedVolMrkt, orgVolMrkt, orgVolMrkt * 0.1); // large tol
    }
  }

}
