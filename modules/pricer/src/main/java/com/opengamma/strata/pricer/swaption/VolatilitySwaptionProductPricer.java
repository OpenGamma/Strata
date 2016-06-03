/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import java.time.ZonedDateTime;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.product.swaption.SwaptionSensitivity;
import com.opengamma.strata.market.product.swaption.SwaptionVolatilities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.swaption.ResolvedSwaption;
import com.opengamma.strata.product.swaption.SettlementType;

/**
 * Pricer for swaptions handling physical and cash par yield settlement based on volatilities.
 * <p>
 * The swap underlying the swaption must have a fixed leg on which the forward rate is computed.
 * The underlying swap must be single currency.
 * <p>
 * The volatility parameters are not adjusted for the underlying swap convention.
 * <p>
 * The value of the swaption after expiry is 0.
 * For a swaption which has already expired, a negative number is returned by
 * {@link SwaptionVolatilities#relativeTime(ZonedDateTime)}.
 */
public class VolatilitySwaptionProductPricer {

  /**
   * Default implementation.
   */
  public static final VolatilitySwaptionProductPricer DEFAULT = new VolatilitySwaptionProductPricer(
      VolatilitySwaptionCashParYieldProductPricer.DEFAULT,
      VolatilitySwaptionPhysicalProductPricer.DEFAULT);

  /**
   * Pricer for cash par yield.
   */
  private final VolatilitySwaptionCashParYieldProductPricer cashParYieldPricer;
  /**
   * Pricer for physical.
   */
  private final VolatilitySwaptionPhysicalProductPricer physicalPricer;

  /**
   * Creates an instance.
   * 
   * @param cashParYieldPricer  the pricer for cash par yield
   * @param physicalPricer  the pricer for physical
   */
  public VolatilitySwaptionProductPricer(
      VolatilitySwaptionCashParYieldProductPricer cashParYieldPricer,
      VolatilitySwaptionPhysicalProductPricer physicalPricer) {

    this.cashParYieldPricer = ArgChecker.notNull(cashParYieldPricer, "cashParYieldPricer");
    this.physicalPricer = ArgChecker.notNull(physicalPricer, "physicalPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the swaption.
   * <p>
   * The result is expressed using the currency of the swaption.
   * 
   * @param swaption  the swaption
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value of the swaption
   */
  public CurrencyAmount presentValue(
      ResolvedSwaption swaption,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    if (swaption.getSwaptionSettlement().getSettlementType().equals(SettlementType.CASH)) {
      return cashParYieldPricer.presentValue(swaption, ratesProvider, swaptionVolatilities);
    } else {
      return physicalPricer.presentValue(swaption, ratesProvider, swaptionVolatilities);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the currency exposure of the swaption.
   * <p>
   * This is equivalent to the present value of the swaption.
   * 
   * @param swaption  the swaption
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value of the swaption
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedSwaption swaption,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    if (swaption.getSwaptionSettlement().getSettlementType().equals(SettlementType.CASH)) {
      return cashParYieldPricer.currencyExposure(swaption, ratesProvider, swaptionVolatilities);
    } else {
      return physicalPricer.currencyExposure(swaption, ratesProvider, swaptionVolatilities);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the implied volatility of the swaption.
   * 
   * @param swaption  the swaption
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the implied volatility associated with the swaption
   */
  public double impliedVolatility(
      ResolvedSwaption swaption,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    if (swaption.getSwaptionSettlement().getSettlementType().equals(SettlementType.CASH)) {
      return cashParYieldPricer.impliedVolatility(swaption, ratesProvider, swaptionVolatilities);
    } else {
      return physicalPricer.impliedVolatility(swaption, ratesProvider, swaptionVolatilities);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value delta of the swaption.
   * <p>
   * The present value delta is given by {@code pvbp * priceDelta} where {@code priceDelta}
   * is the first derivative of the price with respect to forward.
   * <p>
   * The derivative is computed in the formula underlying the volatility (Black or Normal).
   * It does not take into account the potential change of implied volatility induced by
   * the change of forward. The number computed by this method is closely related to the
   * {@link VolatilitySwaptionProductPricer#presentValueSensitivityStickyStrike} method.
   * <p>
   * The result is expressed using the currency of the swaption.
   * 
   * @param swaption  the swaption
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value delta of the swaption
   */
  public CurrencyAmount presentValueDelta(
      ResolvedSwaption swaption,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    if (swaption.getSwaptionSettlement().getSettlementType().equals(SettlementType.CASH)) {
      return cashParYieldPricer.presentValueDelta(swaption, ratesProvider, swaptionVolatilities);
    } else {
      return physicalPricer.presentValueDelta(swaption, ratesProvider, swaptionVolatilities);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value gamma of the swaption.
   * <p>
   * The present value gamma is given by {@code pvbp * priceGamma} where {@code priceGamma}
   * is the second derivative of the price with respect to forward.
   * <p>
   * The result is expressed using the currency of the swaption.
   * 
   * @param swaption  the swaption
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value gamma of the swaption
   */
  public CurrencyAmount presentValueGamma(
      ResolvedSwaption swaption,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    if (swaption.getSwaptionSettlement().getSettlementType().equals(SettlementType.CASH)) {
      return cashParYieldPricer.presentValueGamma(swaption, ratesProvider, swaptionVolatilities);
    } else {
      return physicalPricer.presentValueGamma(swaption, ratesProvider, swaptionVolatilities);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the swaption.
   * <p>
   * The present value theta is given by {@code pvbp * priceTheta} where {@code priceTheta}
   * is the minus of the price sensitivity to {@code timeToExpiry}.
   * <p>
   * The result is expressed using the currency of the swaption.
   * 
   * @param swaption  the swaption
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value theta of the swaption
   */
  public CurrencyAmount presentValueTheta(
      ResolvedSwaption swaption,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    if (swaption.getSwaptionSettlement().getSettlementType().equals(SettlementType.CASH)) {
      return cashParYieldPricer.presentValueTheta(swaption, ratesProvider, swaptionVolatilities);
    } else {
      return physicalPricer.presentValueTheta(swaption, ratesProvider, swaptionVolatilities);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the swaption.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param swaption  the swaption
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value curve sensitivity of the swap product
   */
  public PointSensitivityBuilder presentValueSensitivityStickyStrike(
      ResolvedSwaption swaption,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    if (swaption.getSwaptionSettlement().getSettlementType().equals(SettlementType.CASH)) {
      return cashParYieldPricer.presentValueSensitivityStickyStrike(swaption, ratesProvider, swaptionVolatilities);
    } else {
      return physicalPricer.presentValueSensitivityStickyStrike(swaption, ratesProvider, swaptionVolatilities);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity to the implied volatility of the swaption.
   * <p>
   * The sensitivity to the implied volatility is also called vega.
   * 
   * @param swaption  the swaption
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the point sensitivity to the volatility
   */
  public SwaptionSensitivity presentValueSensitivityVolatility(
      ResolvedSwaption swaption,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    if (swaption.getSwaptionSettlement().getSettlementType().equals(SettlementType.CASH)) {
      return cashParYieldPricer.presentValueSensitivityVolatility(swaption, ratesProvider, swaptionVolatilities);
    } else {
      return physicalPricer.presentValueSensitivityVolatility(swaption, ratesProvider, swaptionVolatilities);
    }
  }

}
