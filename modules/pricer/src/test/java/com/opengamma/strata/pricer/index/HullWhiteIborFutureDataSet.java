/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_6M;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.pricer.model.HullWhiteOneFactorPiecewiseConstantParameters;
import com.opengamma.strata.pricer.model.HullWhiteOneFactorPiecewiseConstantParametersProvider;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.index.IborFuture;
import com.opengamma.strata.product.index.IborFutureTrade;

/**
 * Data set used for testing futures pricers under Hull-White one factor model.
 */
public class HullWhiteIborFutureDataSet {

  // Hull-White model parameters
  private static final double MEAN_REVERSION = 0.01;
  private static final DoubleArray VOLATILITY = DoubleArray.of(0.01, 0.011, 0.012, 0.013, 0.014);
  private static final DoubleArray VOLATILITY_TIME = DoubleArray.of(0.5, 1.0, 2.0, 5.0);
  private static final HullWhiteOneFactorPiecewiseConstantParameters MODEL_PARAMETERS =
      HullWhiteOneFactorPiecewiseConstantParameters.of(MEAN_REVERSION, VOLATILITY, VOLATILITY_TIME);

  // Rates provider
  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final DoubleArray DSC_TIME = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0, 10.0);
  private static final DoubleArray DSC_RATE = DoubleArray.of(0.0150, 0.0125, 0.0150, 0.0175, 0.0150, 0.0150);
  /** discounting curve name */
  public static final CurveName DSC_NAME = CurveName.of("EUR Dsc");
  private static final CurveMetadata META_DSC = Curves.zeroRates(DSC_NAME, ACT_ACT_ISDA);
  private static final InterpolatedNodalCurve DSC_CURVE =
      InterpolatedNodalCurve.of(META_DSC, DSC_TIME, DSC_RATE, INTERPOLATOR);
  private static final DoubleArray FWD3_TIME = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 3.0, 4.0, 5.0, 10.0);
  private static final DoubleArray FWD3_RATE =
      DoubleArray.of(0.0150, 0.0125, 0.0150, 0.0175, 0.0175, 0.0190, 0.0200, 0.0210);
  /** Forward curve name */
  public static final CurveName FWD3_NAME = CurveName.of("EUR EURIBOR 3M");
  private static final CurveMetadata META_FWD3 = Curves.zeroRates(FWD3_NAME, ACT_ACT_ISDA);
  private static final InterpolatedNodalCurve FWD3_CURVE =
      InterpolatedNodalCurve.of(META_FWD3, FWD3_TIME, FWD3_RATE, INTERPOLATOR);
  private static final DoubleArray FWD6_TIME = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0, 10.0);
  private static final DoubleArray FWD6_RATE = DoubleArray.of(0.0150, 0.0125, 0.0150, 0.0175, 0.0150, 0.0150);
  /** Forward curve name */
  public static final CurveName FWD6_NAME = CurveName.of("EUR EURIBOR 6M");
  private static final CurveMetadata META_FWD6 = Curves.zeroRates(FWD6_NAME, ACT_ACT_ISDA);
  private static final InterpolatedNodalCurve FWD6_CURVE =
      InterpolatedNodalCurve.of(META_FWD6, FWD6_TIME, FWD6_RATE, INTERPOLATOR);

  /**
   * Creates Hull-White one factor model parameters with specified valuation date for swaption
   * 
   * @param valuationDate  the valuation date
   * @return  the parameter provider
   */
  public static HullWhiteOneFactorPiecewiseConstantParametersProvider createHullWhiteProvider(LocalDate valuationDate) {
    return HullWhiteOneFactorPiecewiseConstantParametersProvider.of(
        MODEL_PARAMETERS, ACT_ACT_ISDA, valuationDate.atTime(LocalTime.NOON).atZone(ZoneOffset.UTC));
  }

  /**
   * Creates rates provider with specified  valuation date.
   * 
   * @param valuationDate  the valuation date
   * @return  the rates provider
   */
  public static ImmutableRatesProvider createRatesProvider(LocalDate valuationDate) {
    return ImmutableRatesProvider.builder(valuationDate)
        .discountCurves(ImmutableMap.of(EUR, DSC_CURVE))
        .indexCurves(ImmutableMap.of(EUR_EURIBOR_3M, FWD3_CURVE, EUR_EURIBOR_6M, FWD6_CURVE))
        .fxRateProvider(FxMatrix.empty())
        .build();
  }

  // Instruments
  /** Notional of product */
  public static final double NOTIONAL = 1000000.0;
  private static final LocalDate LAST_TRADE_DATE = LocalDate.of(2012, 9, 17);
  private static final double FUTURE_FACTOR = 0.25;
  /**  Ibor future product  */
  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Ticker", "FutSec");
  public static final IborFuture IBOR_FUTURE = IborFuture.builder()
      .securityId(SECURITY_ID)
      .currency(EUR)
      .notional(NOTIONAL)
      .lastTradeDate(LAST_TRADE_DATE)
      .index(EUR_EURIBOR_3M)
      .accrualFactor(FUTURE_FACTOR)
      .rounding(Rounding.none())
      .build();
  /** Quantity of trade */
  public static final long QUANTITY = 400L;
  private static final double REFERENCE_PRICE = 0.99;
  private static final LocalDate TRADE_DATE = LocalDate.of(2011, 5, 11);
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(TRADE_DATE).build();
  /** Ibor future trade */
  public static final IborFutureTrade IBOR_FUTURE_TRADE = IborFutureTrade.builder()
      .info(TRADE_INFO)
      .product(IBOR_FUTURE)
      .quantity(QUANTITY)
      .price(REFERENCE_PRICE)
      .build();
  /** Last margin price */
  public static final double LAST_MARGIN_PRICE = 0.98;
}
