/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.marketdata.MarketDataFactory;
import com.opengamma.strata.calc.marketdata.MarketDataFunction;
import com.opengamma.strata.calc.marketdata.ObservableDataProvider;
import com.opengamma.strata.calc.marketdata.TimeSeriesProvider;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.measure.bond.BillTradeCalculationFunction;
import com.opengamma.strata.measure.bond.BondFutureOptionTradeCalculationFunction;
import com.opengamma.strata.measure.bond.BondFutureTradeCalculationFunction;
import com.opengamma.strata.measure.bond.CapitalIndexedBondTradeCalculationFunction;
import com.opengamma.strata.measure.bond.FixedCouponBondTradeCalculationFunction;
import com.opengamma.strata.measure.capfloor.IborCapFloorTradeCalculationFunction;
import com.opengamma.strata.measure.cms.CmsTradeCalculationFunction;
import com.opengamma.strata.measure.credit.CdsIndexTradeCalculationFunction;
import com.opengamma.strata.measure.credit.CdsTradeCalculationFunction;
import com.opengamma.strata.measure.curve.CurveMarketDataFunction;
import com.opengamma.strata.measure.deposit.TermDepositTradeCalculationFunction;
import com.opengamma.strata.measure.dsf.DsfTradeCalculationFunction;
import com.opengamma.strata.measure.fra.FraTradeCalculationFunction;
import com.opengamma.strata.measure.fx.FxNdfTradeCalculationFunction;
import com.opengamma.strata.measure.fx.FxRateMarketDataFunction;
import com.opengamma.strata.measure.fx.FxSingleTradeCalculationFunction;
import com.opengamma.strata.measure.fx.FxSwapTradeCalculationFunction;
import com.opengamma.strata.measure.fxopt.FxCollarTradeCalculationFunction;
import com.opengamma.strata.measure.fxopt.FxOptionVolatilitiesMarketDataFunction;
import com.opengamma.strata.measure.fxopt.FxSingleBarrierOptionTradeCalculationFunction;
import com.opengamma.strata.measure.fxopt.FxVanillaOptionTradeCalculationFunction;
import com.opengamma.strata.measure.index.IborFutureOptionTradeCalculationFunction;
import com.opengamma.strata.measure.index.IborFutureTradeCalculationFunction;
import com.opengamma.strata.measure.index.OvernightFutureTradeCalculationFunction;
import com.opengamma.strata.measure.payment.BulletPaymentTradeCalculationFunction;
import com.opengamma.strata.measure.rate.RatesCurveGroupMarketDataFunction;
import com.opengamma.strata.measure.rate.RatesCurveInputsMarketDataFunction;
import com.opengamma.strata.measure.security.GenericSecurityPositionCalculationFunction;
import com.opengamma.strata.measure.security.GenericSecurityTradeCalculationFunction;
import com.opengamma.strata.measure.security.SecurityPositionCalculationFunction;
import com.opengamma.strata.measure.security.SecurityTradeCalculationFunction;
import com.opengamma.strata.measure.swap.SwapTradeCalculationFunction;
import com.opengamma.strata.measure.swaption.SwaptionTradeCalculationFunction;
import com.opengamma.strata.product.GenericSecurityPosition;
import com.opengamma.strata.product.GenericSecurityTrade;
import com.opengamma.strata.product.SecurityPosition;
import com.opengamma.strata.product.SecurityTrade;
import com.opengamma.strata.product.bond.BondFutureOptionPosition;
import com.opengamma.strata.product.bond.BondFutureOptionTrade;
import com.opengamma.strata.product.bond.BondFuturePosition;
import com.opengamma.strata.product.bond.BondFutureTrade;
import com.opengamma.strata.product.bond.CapitalIndexedBondPosition;
import com.opengamma.strata.product.bond.CapitalIndexedBondTrade;
import com.opengamma.strata.product.bond.FixedCouponBondPosition;
import com.opengamma.strata.product.bond.FixedCouponBondTrade;
import com.opengamma.strata.product.capfloor.IborCapFloorTrade;
import com.opengamma.strata.product.credit.CdsIndexTrade;
import com.opengamma.strata.product.credit.CdsTrade;
import com.opengamma.strata.product.deposit.TermDepositTrade;
import com.opengamma.strata.product.dsf.DsfPosition;
import com.opengamma.strata.product.dsf.DsfTrade;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.product.fx.FxNdfTrade;
import com.opengamma.strata.product.fx.FxSingleTrade;
import com.opengamma.strata.product.fx.FxSwapTrade;
import com.opengamma.strata.product.fxopt.FxSingleBarrierOptionTrade;
import com.opengamma.strata.product.fxopt.FxVanillaOptionTrade;
import com.opengamma.strata.product.index.IborFutureOptionPosition;
import com.opengamma.strata.product.index.IborFutureOptionTrade;
import com.opengamma.strata.product.index.IborFuturePosition;
import com.opengamma.strata.product.index.IborFutureTrade;
import com.opengamma.strata.product.index.OvernightFuturePosition;
import com.opengamma.strata.product.index.OvernightFutureTrade;
import com.opengamma.strata.product.payment.BulletPaymentTrade;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swaption.SwaptionTrade;

/**
 * Factory methods for creating standard Strata components.
 * <p>
 * These components are suitable for performing calculations using the built-in asset classes,
 * market data types and pricers.
 * <p>
 * The market data factory can create market data values derived from other values.
 * For example it can create calibrated curves given market quotes.
 * However it cannot request market data from an external provider, such as Bloomberg,
 * or look up data from a data store, for example a time series database.
 * Instances of {@link CalculationRunner} are created directly using the static methods on the interface.
 */
public final class StandardComponents {

  /**
   * The standard calculation functions.
   */
  private static final CalculationFunctions STANDARD = CalculationFunctions.of(
      new BulletPaymentTradeCalculationFunction(),
      new CdsTradeCalculationFunction(),
      new CdsIndexTradeCalculationFunction(),
      new FraTradeCalculationFunction(),
      new FxNdfTradeCalculationFunction(),
      new FxSingleBarrierOptionTradeCalculationFunction(),
      new FxSingleTradeCalculationFunction(),
      new FxSwapTradeCalculationFunction(),
      new FxVanillaOptionTradeCalculationFunction(),
      new FxCollarTradeCalculationFunction(),
      new IborCapFloorTradeCalculationFunction(),
      new SecurityPositionCalculationFunction(),
      new SecurityTradeCalculationFunction(),
      new SwapTradeCalculationFunction(),
      new SwaptionTradeCalculationFunction(),
      new CmsTradeCalculationFunction(),
      new TermDepositTradeCalculationFunction(),
      new GenericSecurityPositionCalculationFunction(),
      new GenericSecurityTradeCalculationFunction(),
      BondFutureTradeCalculationFunction.TRADE,
      BondFutureTradeCalculationFunction.POSITION,
      BondFutureOptionTradeCalculationFunction.TRADE,
      BondFutureOptionTradeCalculationFunction.POSITION,
      CapitalIndexedBondTradeCalculationFunction.TRADE,
      CapitalIndexedBondTradeCalculationFunction.POSITION,
      DsfTradeCalculationFunction.TRADE,
      DsfTradeCalculationFunction.POSITION,
      FixedCouponBondTradeCalculationFunction.TRADE,
      FixedCouponBondTradeCalculationFunction.POSITION,
      BillTradeCalculationFunction.TRADE,
      BillTradeCalculationFunction.POSITION,
      IborFutureTradeCalculationFunction.TRADE,
      IborFutureTradeCalculationFunction.POSITION,
      OvernightFutureTradeCalculationFunction.TRADE,
      OvernightFutureTradeCalculationFunction.POSITION,
      IborFutureOptionTradeCalculationFunction.TRADE,
      IborFutureOptionTradeCalculationFunction.POSITION);

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
    return marketDataFactory(ObservableDataProvider.none());
  }

  /**
   * Returns a market data factory containing the standard set of market data functions.
   * <p>
   * This factory can create market data values from other market data. For example it
   * can create calibrated curves given a set of market quotes for the points on the curve.
   * <p>
   * The set of functions are the ones provided by {@link #marketDataFunctions()}.
   *
   * @param observableDataProvider  the provider of observable data
   * @return a market data factory containing the standard set of market data functions
   */
  public static MarketDataFactory marketDataFactory(ObservableDataProvider observableDataProvider) {
    return MarketDataFactory.of(observableDataProvider, TimeSeriesProvider.none(), marketDataFunctions());
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
   *  <li>FX rates from quotes
   *  <li>FX option volatilities from quotes
   * </ul>
   *
   * @return the standard market data functions
   */
  public static List<MarketDataFunction<?, ?>> marketDataFunctions() {
    return ImmutableList.of(
        new CurveMarketDataFunction(),
        new RatesCurveGroupMarketDataFunction(),
        new RatesCurveInputsMarketDataFunction(),
        new FxRateMarketDataFunction(),
        new FxOptionVolatilitiesMarketDataFunction());
  }

  /**
   * Returns the standard calculation functions.
   * <p>
   * These define how to calculate the standard measures for the standard asset classes.
   * <p>
   * The standard calculation functions require no further configuration and are designed to allow
   * easy access to all built-in asset class coverage.
   * The supported asset classes are:
   * <ul>
   *  <li>Bond future - {@link BondFutureTrade} and {@link BondFuturePosition}
   *  <li>Bond future option - {@link BondFutureOptionTrade} and {@link BondFutureOptionPosition}
   *  <li>Bullet Payment - {@link BulletPaymentTrade}
   *  <li>Cap/floor (Ibor) - {@link IborCapFloorTrade}
   *  <li>Capital Indexed bond - {@link CapitalIndexedBondTrade} and {@link CapitalIndexedBondPosition}
   *  <li>Credit Default Swap - {@link CdsTrade}
   *  <li>CDS Index - {@link CdsIndexTrade}
   *  <li>Deliverable Swap Future - {@link DsfTrade} and {@link DsfPosition}
   *  <li>Forward Rate Agreement - {@link FraTrade}
   *  <li>Fixed coupon bond - {@link FixedCouponBondTrade} and {@link FixedCouponBondPosition}
   *  <li>FX spot and FX forward - {@link FxSingleTrade}
   *  <li>FX NDF - {@link FxNdfTrade}
   *  <li>FX swap - {@link FxSwapTrade}
   *  <li>FX vanilla option - {@link FxVanillaOptionTrade}
   *  <li>FX single barrier option - {@link FxSingleBarrierOptionTrade}
   *  <li>Generic Security - {@link GenericSecurityTrade} and {@link GenericSecurityPosition}
   *  <li>Rate Swap - {@link SwapTrade}
   *  <li>Swaption - {@link SwaptionTrade}
   *  <li>Security - {@link SecurityTrade} and {@link SecurityPosition}
   *  <li>STIR Future (Ibor) - {@link IborFutureTrade} and {@link IborFuturePosition}
   *  <li>STIR Future (Overnight Indices) - {@link OvernightFutureTrade} and {@link OvernightFuturePosition}
   *  <li>STIR Future Option (Ibor) - {@link IborFutureOptionTrade} and {@link IborFutureOptionPosition}
   *  <li>Term Deposit - {@link TermDepositTrade}
   * </ul>
   *
   * @return calculation functions used to perform calculations
   */
  public static CalculationFunctions calculationFunctions() {
    return STANDARD;
  }

}
