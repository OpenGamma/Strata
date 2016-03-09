/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.config.pricing.PricingRules;
import com.opengamma.strata.calc.marketdata.DefaultMarketDataFactory;
import com.opengamma.strata.calc.marketdata.MarketDataFactory;
import com.opengamma.strata.calc.marketdata.function.MarketDataFunction;
import com.opengamma.strata.calc.marketdata.function.ObservableMarketDataFunction;
import com.opengamma.strata.calc.marketdata.function.TimeSeriesProvider;
import com.opengamma.strata.calc.marketdata.mapping.FeedIdMapping;
import com.opengamma.strata.function.marketdata.curve.CurveGroupMarketDataFunction;
import com.opengamma.strata.function.marketdata.curve.CurveInputsMarketDataFunction;
import com.opengamma.strata.function.marketdata.curve.DiscountCurveMarketDataFunction;
import com.opengamma.strata.function.marketdata.curve.IborIndexCurveMarketDataFunction;
import com.opengamma.strata.function.marketdata.curve.OvernightIndexCurveMarketDataFunction;
import com.opengamma.strata.function.marketdata.fx.FxRateMarketDataFunction;
import com.opengamma.strata.product.GenericSecurityTrade;
import com.opengamma.strata.product.SecurityPosition;
import com.opengamma.strata.product.SecurityTrade;
import com.opengamma.strata.product.credit.CdsTrade;
import com.opengamma.strata.product.deposit.TermDepositTrade;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.product.fx.FxNdfTrade;
import com.opengamma.strata.product.fx.FxSingleTrade;
import com.opengamma.strata.product.fx.FxSwapTrade;
import com.opengamma.strata.product.index.IborFutureTrade;
import com.opengamma.strata.product.payment.BulletPaymentTrade;
import com.opengamma.strata.product.swap.DeliverableSwapFutureTrade;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Factory methods for creating standard Strata components.
 * <p>
 * These components are suitable for performing calculations using the built-in asset classes,
 * market data types and pricers. The market data must provided by the user.
 * <p>
 * The market data factory can create market data values derived from other values.
 * For example it can create calibrated curves given market quotes.
 * However it cannot request market data from an external provider, such as Bloomberg,
 * or look up data from a data store, for example a time series database.
 * Instances of {@link CalculationRunner} are created directly using the static methods on the interface.
 */
public class StandardComponents {

  /**
   * Restricted constructor.
   */
  private StandardComponents() {
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a market data factory containing the standard set of market data functions.
   * <p>
   * This factory can create market data values from other market data. For example it
   * can create calibrated curves given a set of market quotes for the points on the curve.
   * <p>
   * The set of functions are the ones provided by {@link #marketDataFunctions()}.
   *
   * @return a market data factory containing the standard set of market data functions
   */
  public static MarketDataFactory marketDataFactory() {
    return marketDataFactory(ObservableMarketDataFunction.none());
  }

  /**
   * Returns a market data factory containing the standard set of market data functions.
   * <p>
   * This factory can create market data values from other market data. For example it
   * can create calibrated curves given a set of market quotes for the points on the curve.
   * <p>
   * The set of functions are the ones provided by {@link #marketDataFunctions()}.
   *
   * @param observableMarketData  the function providing observable data
   * @return a market data factory containing the standard set of market data functions
   */
  public static MarketDataFactory marketDataFactory(ObservableMarketDataFunction observableMarketData) {
    return new DefaultMarketDataFactory(
        TimeSeriesProvider.none(),
        observableMarketData,
        FeedIdMapping.identity(),
        marketDataFunctions());
  }

  /**
   * Returns the standard market data functions used to build market data values from other market data.
   * <p>
   * These include functions to build:
   * <ul>
   *  <li>Par rates from quotes
   *  <li>Curve groups from par rates
   *  <li>Curves from curve groups
   *  <li>Discount factors and index rates from curves
   * </ul>
   *
   * @return the standard market data functions
   */
  public static List<MarketDataFunction<?, ?>> marketDataFunctions() {
    return ImmutableList.of(
        new DiscountCurveMarketDataFunction(),
        new IborIndexCurveMarketDataFunction(),
        new OvernightIndexCurveMarketDataFunction(),
        new CurveGroupMarketDataFunction(),
        new CurveInputsMarketDataFunction(),
        new FxRateMarketDataFunction());
  }

  /**
   * Returns the standard pricing rules.
   * <p>
   * These rules define how to calculate the standard measures for the standard asset classes.
   * <p>
   * The standard pricing rules require no further configuration and are designed to allow
   * easy access to all built-in asset class coverage.
   * The supported asset classes are:
   * <ul>
   *  <li>Bullet Payment - {@link BulletPaymentTrade}
   *  <li>Credit Default Swap - {@link CdsTrade}
   *  <li>Deliverable Swap Future - {@link DeliverableSwapFutureTrade}
   *  <li>Forward Rate Agreement - {@link FraTrade}
   *  <li>FX single (spot/forward) - {@link FxSingleTrade}
   *  <li>FX NDF - {@link FxNdfTrade}
   *  <li>FX swap - {@link FxSwapTrade}
   *  <li>Generic Security - {@link GenericSecurityTrade}
   *  <li>Ibor Future (STIR) - {@link IborFutureTrade}
   *  <li>Rate Swap - {@link SwapTrade}
   *  <li>Security - {@link SecurityTrade} and {@link SecurityPosition}
   *  <li>Term Deposit - {@link TermDepositTrade}
   * </ul>
   *
   * @return pricing rules defining how to calculate the standard measures for the standard asset classes
   */
  public static PricingRules pricingRules() {
    return StandardPricingRules.standard();
  }

}
