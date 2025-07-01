/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fxopt.ResolvedFxSingleBarrierOption;
import com.opengamma.strata.product.fxopt.ResolvedFxSingleBarrierOptionTrade;

/**
 * Pricer for FX barrier option trades under implied trinomial tree.
 * <p>
 * This function provides the ability to price an {@link ResolvedFxSingleBarrierOptionTrade}.
 */
public class ImpliedTrinomialTreeFxSingleBarrierOptionTradePricer {

  /**
   * Default implementation.
   */
  public static final ImpliedTrinomialTreeFxSingleBarrierOptionTradePricer DEFAULT =
      new ImpliedTrinomialTreeFxSingleBarrierOptionTradePricer(
          ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer.DEFAULT,
          DiscountingPaymentPricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedFxSingleBarrierOption}.
   */
  private final ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer productPricer;
  /**
   * Pricer for {@link Payment}.
   */
  private final DiscountingPaymentPricer paymentPricer;

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link ResolvedFxSingleBarrierOption}
   * @param paymentPricer  the pricer for {@link Payment}
   */
  public ImpliedTrinomialTreeFxSingleBarrierOptionTradePricer(
      ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer productPricer,
      DiscountingPaymentPricer paymentPricer) {
    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
    this.paymentPricer = ArgChecker.notNull(paymentPricer, "paymentPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the FX barrier option trade.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * <p>
   * The trinomial tree is first calibrated to Black volatilities, 
   * then the price is computed based on the calibrated tree.
   * 
   * @param trade  the option trade
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value of the trade
   */
  public MultiCurrencyAmount presentValue(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    ResolvedFxSingleBarrierOption product = trade.getProduct();
    CurrencyAmount pvProduct = productPricer.presentValue(product, ratesProvider, volatilities);
    Payment premium = trade.getPremium();
    CurrencyAmount pvPremium = paymentPricer.presentValue(premium, ratesProvider);
    return MultiCurrencyAmount.of(pvProduct, pvPremium);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the FX barrier option trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * <p>
   * The sensitivity is computed by bump and re-price, returning {@link CurrencyParameterSensitivities},
   * not {@link PointSensitivities}.
   * <p>
   * The trinomial tree is first calibrated to Black volatilities, 
   * then the price is computed based on the calibrated tree.
   * 
   * @param trade  the option trade
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value curve sensitivity of the trade
   */
  public CurrencyParameterSensitivities presentValueSensitivityRates(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    ResolvedFxSingleBarrierOption product = trade.getProduct();
    CurrencyParameterSensitivities sensProduct =
        productPricer.presentValueSensitivityRates(product, ratesProvider, volatilities);
    Payment premium = trade.getPremium();
    PointSensitivityBuilder pvcsPremium = paymentPricer.presentValueSensitivity(premium, ratesProvider);
    CurrencyParameterSensitivities sensPremium = ratesProvider.parameterSensitivity(pvcsPremium.build());
    return sensProduct.combinedWith(sensPremium);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the FX barrier option trade.
   * <p>
   * The trinomial tree is first calibrated to Black volatilities, 
   * then the price is computed based on the calibrated tree.
   * 
   * @param trade  the option trade
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedFxSingleBarrierOptionTrade trade,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    Payment premium = trade.getPremium();
    CurrencyAmount pvPremium = paymentPricer.presentValue(premium, ratesProvider);
    ResolvedFxSingleBarrierOption product = trade.getProduct();
    return productPricer.currencyExposure(product, ratesProvider, volatilities).plus(pvPremium);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the current of the FX barrier option trade.
   * 
   * @param trade  the option trade
   * @param valuationDate  the valuation date
   * @return the current cash amount
   */
  public CurrencyAmount currentCash(ResolvedFxSingleBarrierOptionTrade trade, LocalDate valuationDate) {
    Payment premium = trade.getPremium();
    if (premium.getDate().equals(valuationDate)) {
      return CurrencyAmount.of(premium.getCurrency(), premium.getAmount());
    }
    return CurrencyAmount.of(premium.getCurrency(), 0d);
  }

}
