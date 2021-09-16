/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.common.SettlementType;
import com.opengamma.strata.product.swaption.ResolvedSwaption;
import com.opengamma.strata.product.swaption.ResolvedSwaptionTrade;

/**
 * Pricer for swaption trade in the SABR model on the swap rate.
 * <p>
 * The swap underlying the swaption must have a fixed leg on which the forward rate is computed.
 * The underlying swap must be single currency.
 * <p>
 * The volatility parameters are not adjusted for the underlying swap convention.
 * The volatilities from the provider are taken as such.
 * <p>
 * The present value and sensitivities of the premium, if in the future, are also taken into account.
 */
public class SabrSwaptionTradePricer {

  /**
   * Default implementation.
   */
  public static final SabrSwaptionTradePricer DEFAULT = new SabrSwaptionTradePricer(
      SabrSwaptionCashParYieldProductPricer.DEFAULT,
      SabrSwaptionPhysicalProductPricer.DEFAULT,
      DiscountingPaymentPricer.DEFAULT);

  /**
   * Pricer for cash par yield.
   */
  private final SabrSwaptionCashParYieldProductPricer cashParYieldPricer;
  /**
   * Pricer for physical.
   */
  private final SabrSwaptionPhysicalProductPricer physicalPricer;
  /**
   * Pricer for {@link Payment}.
   */
  private final DiscountingPaymentPricer paymentPricer;

  /**
   * Creates an instance.
   * 
   * @param cashParYieldPricer  the pricer for cash par yield
   * @param physicalPricer  the pricer for physical
   * @param paymentPricer  the pricer for {@link Payment}
   */
  public SabrSwaptionTradePricer(
      SabrSwaptionCashParYieldProductPricer cashParYieldPricer,
      SabrSwaptionPhysicalProductPricer physicalPricer,
      DiscountingPaymentPricer paymentPricer) {

    this.cashParYieldPricer = ArgChecker.notNull(cashParYieldPricer, "cashParYieldPricer");
    this.physicalPricer = ArgChecker.notNull(physicalPricer, "physicalPricer");
    this.paymentPricer = ArgChecker.notNull(paymentPricer, "paymentPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the swaption trade.
   * <p>
   * The result is expressed using the currency of the swaption.
   * 
   * @param trade  the swaption trade
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedSwaptionTrade trade,
      RatesProvider ratesProvider,
      SabrSwaptionVolatilities swaptionVolatilities) {

    // product
    ResolvedSwaption product = trade.getProduct();
    CurrencyAmount pvProduct = isCash(product) ?
        cashParYieldPricer.presentValue(product, ratesProvider, swaptionVolatilities) :
        physicalPricer.presentValue(product, ratesProvider, swaptionVolatilities);
    // premium
    Payment premium = trade.getPremium();
    CurrencyAmount pvPremium = paymentPricer.presentValue(premium, ratesProvider);
    // total
    return pvProduct.plus(pvPremium);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the swaption trade to the rate curves.
   * <p>
   * The present value sensitivity is computed in a "sticky strike" style, i.e. the sensitivity to the 
   * curve nodes with the volatility at the swaption strike unchanged. This sensitivity does not include a potential 
   * change of volatility due to the implicit change of forward rate or moneyness.
   * 
   * @param trade  the swaption trade
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the point sensitivity to the rate curves
   */
  public PointSensitivities presentValueSensitivityRatesStickyStrike(
      ResolvedSwaptionTrade trade,
      RatesProvider ratesProvider,
      SabrSwaptionVolatilities swaptionVolatilities) {

    // product
    ResolvedSwaption product = trade.getProduct();
    PointSensitivityBuilder pointSens = isCash(product) ?
        cashParYieldPricer.presentValueSensitivityRatesStickyStrike(product, ratesProvider, swaptionVolatilities) :
        physicalPricer.presentValueSensitivityRatesStickyStrike(product, ratesProvider, swaptionVolatilities);
    // premium
    Payment premium = trade.getPremium();
    PointSensitivityBuilder pvcsPremium = paymentPricer.presentValueSensitivity(premium, ratesProvider);
    // total
    return pointSens.combinedWith(pvcsPremium).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the swaption trade to the rate curves.
   * <p>
   * The present value sensitivity is computed in a "sticky model parameter" style, i.e. the sensitivity to the 
   * curve nodes with the SABR model parameters unchanged. This sensitivity does not include a potential 
   * re-calibration of the model parameters to the raw market data.
   * 
   * @param trade  the swaption trade
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the point sensitivity to the rate curves
   */
  public PointSensitivities presentValueSensitivityRatesStickyModel(
      ResolvedSwaptionTrade trade,
      RatesProvider ratesProvider,
      SabrSwaptionVolatilities swaptionVolatilities) {

    // product
    ResolvedSwaption product = trade.getProduct();
    PointSensitivityBuilder pointSens = isCash(product) ?
        cashParYieldPricer.presentValueSensitivityRatesStickyModel(product, ratesProvider, swaptionVolatilities) :
        physicalPricer.presentValueSensitivityRatesStickyModel(product, ratesProvider, swaptionVolatilities);
    // premium
    Payment premium = trade.getPremium();
    PointSensitivityBuilder pvcsPremium = paymentPricer.presentValueSensitivity(premium, ratesProvider);
    // total
    return pointSens.combinedWith(pvcsPremium).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity to the implied volatility of the swaption trade.
   * 
   * @param trade  the swaption trade
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the point sensitivity to the implied volatility
   */
  public PointSensitivities presentValueSensitivityModelParamsVolatility(
      ResolvedSwaptionTrade trade,
      RatesProvider ratesProvider,
      SabrSwaptionVolatilities swaptionVolatilities) {

    ResolvedSwaption product = trade.getProduct();
    SwaptionSensitivity pointSens = isCash(product) ?
        cashParYieldPricer.presentValueSensitivityModelParamsVolatility(product, ratesProvider, swaptionVolatilities) :
        physicalPricer.presentValueSensitivityModelParamsVolatility(product, ratesProvider, swaptionVolatilities);
    return PointSensitivities.of(pointSens);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity to the SABR model parameters of the swaption trade.
   * <p>
   * The sensitivity of the present value to the SABR model parameters, alpha, beta, rho and nu.
   * 
   * @param trade  the swaption trade
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the point sensitivity to the SABR model parameters 
   */
  public PointSensitivities presentValueSensitivityModelParamsSabr(
      ResolvedSwaptionTrade trade,
      RatesProvider ratesProvider,
      SabrSwaptionVolatilities swaptionVolatilities) {

    ResolvedSwaption product = trade.getProduct();
    PointSensitivityBuilder pointSens = isCash(product) ?
        cashParYieldPricer.presentValueSensitivityModelParamsSabr(product, ratesProvider, swaptionVolatilities) :
        physicalPricer.presentValueSensitivityModelParamsSabr(product, ratesProvider, swaptionVolatilities);
    return pointSens.build();
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the currency exposure of the swaption trade.
   * 
   * @param trade  the swaption trade
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedSwaptionTrade trade,
      RatesProvider ratesProvider,
      SabrSwaptionVolatilities swaptionVolatilities) {

    return MultiCurrencyAmount.of(presentValue(trade, ratesProvider, swaptionVolatilities));
  }

  /**
   * Computes the implied volatility of the swaption.
   *
   * @param swaptionTrade  the swaption trade
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the implied volatility
   */
  public double impliedVolatility(
      ResolvedSwaptionTrade swaptionTrade,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    ResolvedSwaption product = swaptionTrade.getProduct();
    if (isCash(product)) {
      return cashParYieldPricer.impliedVolatility(product, ratesProvider, swaptionVolatilities);
    } else {
      return physicalPricer.impliedVolatility(product, ratesProvider, swaptionVolatilities);
    }
  }

  /**
   * Provides the forward rate.
   * <p>
   * This is the par rate for the forward starting swap that is the underlying of the swaption.
   *
   * @param swaptionTrade  the swaption trade
   * @param ratesProvider  the rates provider
   * @return the forward rate
   */
  public double forwardRate(ResolvedSwaptionTrade swaptionTrade, RatesProvider ratesProvider) {
    ResolvedSwaption product = swaptionTrade.getProduct();
    if (isCash(product)) {
      return cashParYieldPricer.forwardRate(product, ratesProvider);
    } else {
      return physicalPricer.forwardRate(product, ratesProvider);
    }
  }

  /**
   * Calculates the current cash of the swaption trade.
   * <p>
   * Only the premium is contributing to the current cash for non-cash settle swaptions.
   * 
   * @param trade  the swaption trade
   * @param valuationDate  the valuation date
   * @return the current cash amount
   */
  public CurrencyAmount currentCash(ResolvedSwaptionTrade trade, LocalDate valuationDate) {
    Payment premium = trade.getPremium();
    if (premium.getDate().equals(valuationDate)) {
      return CurrencyAmount.of(premium.getCurrency(), premium.getAmount());
    }
    return CurrencyAmount.of(premium.getCurrency(), 0d);
  }

  //-------------------------------------------------------------------------
  // is this a cash swaption
  private boolean isCash(ResolvedSwaption product) {
    return product.getSwaptionSettlement().getSettlementType().equals(SettlementType.CASH);
  }

}
