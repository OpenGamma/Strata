/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.capfloor.ResolvedOvernightInArrearsCapFloor;
import com.opengamma.strata.product.capfloor.ResolvedOvernightInArrearsCapFloorTrade;

/**
 * Pricer for overnight rate in arrears cap/floor trades in SABR model.
 */
public class SabrOvernightInArrearsCapFloorTradePricer {

  /**
   * Default implementation.
   */
  public static final SabrOvernightInArrearsCapFloorTradePricer DEFAULT =
      new SabrOvernightInArrearsCapFloorTradePricer(
          SabrOvernightInArrearsCapFloorProductPricer.DEFAULT,
          DiscountingPaymentPricer.DEFAULT);
  /**
   * The pricer for {@link ResolvedOvernightInArrearsCapFloor}.
   */
  private final SabrOvernightInArrearsCapFloorProductPricer productPricer;
  /**
   * Pricer for {@link Payment}.
   */
  private final DiscountingPaymentPricer paymentPricer;

  /**
   * Creates an instance.
   *
   * @param productPricer the pricer for {@link ResolvedOvernightInArrearsCapFloor}
   * @param paymentPricer the pricer for {@link Payment}
   */
  public SabrOvernightInArrearsCapFloorTradePricer(
      SabrOvernightInArrearsCapFloorProductPricer productPricer,
      DiscountingPaymentPricer paymentPricer) {

    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
    this.paymentPricer = ArgChecker.notNull(paymentPricer, "paymentPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the product pricer.
   *
   * @return the product pricer
   */
  public SabrOvernightInArrearsCapFloorProductPricer getProductPricer() {
    return productPricer;
  }

  /**
   * Gets the payment pricer.
   *
   * @return the payment pricer
   */
  public DiscountingPaymentPricer getPaymentPricer() {
    return paymentPricer;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the overnight rate in arrears cap/floor trade.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * <p>
   * The cap/floor leg and pay leg are typically in the same currency,
   * thus the present value is expressed as a single currency amount in most cases.
   *
   * @param trade the cap/floor trade
   * @param ratesProvider the rates provider
   * @param volatilities the volatilities
   * @return the present value
   */
  public MultiCurrencyAmount presentValue(
      ResolvedOvernightInArrearsCapFloorTrade trade,
      RatesProvider ratesProvider,
      SabrParametersIborCapletFloorletVolatilities volatilities) {

    MultiCurrencyAmount pvProduct = productPricer.presentValue(trade.getProduct(), ratesProvider, volatilities);
    if (!trade.getPremium().isPresent()) {
      return pvProduct;
    }
    CurrencyAmount pvPremium = paymentPricer.presentValue(trade.getPremium().get(), ratesProvider);
    return pvProduct.plus(pvPremium);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value for each caplet/floorlet of the overnight rate in arrears cap/floor trade.
   * <p>
   * The present value of each caplet/floorlet is the value on the valuation date.
   * The result is returned using the payment currency of the leg.
   * <p>
   * The present value will not be calculated for the trade premium or for the pay leg
   * if the cap/floor product has one.
   *
   * @param trade the cap/floor leg
   * @param ratesProvider the rates provider
   * @param volatilities the volatilities
   * @return the present values
   */
  public OvernightInArrearsCapletFloorletPeriodCurrencyAmounts presentValueCapletFloorletPeriods(
      ResolvedOvernightInArrearsCapFloorTrade trade,
      RatesProvider ratesProvider,
      SabrParametersIborCapletFloorletVolatilities volatilities) {

    return productPricer.presentValueCapletFloorletPeriods(trade.getProduct(), ratesProvider, volatilities);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value rates sensitivity of the overnight rate in arrears cap/floor trade.
   * <p>
   * The present value sensitivity is computed in a "sticky model parameter" style, i.e. the sensitivity to the
   * curve nodes with the SABR model parameters unchanged. This sensitivity does not include a potential
   * re-calibration of the model parameters to the raw market data.
   *
   * @param trade the cap/floor trade
   * @param ratesProvider the rates provider
   * @param volatilities the volatilities
   * @return the present value sensitivity
   */
  public PointSensitivities presentValueSensitivityRatesStickyModel(
      ResolvedOvernightInArrearsCapFloorTrade trade,
      RatesProvider ratesProvider,
      SabrParametersIborCapletFloorletVolatilities volatilities) {

    PointSensitivityBuilder pvSensiProduct =
        productPricer.presentValueSensitivityRatesStickyModel(trade.getProduct(), ratesProvider, volatilities);
    if (!trade.getPremium().isPresent()) {
      return pvSensiProduct.build();
    }
    PointSensitivityBuilder pvSensiPremium =
        getPaymentPricer().presentValueSensitivity(trade.getPremium().get(), ratesProvider);
    return pvSensiProduct.combinedWith(pvSensiPremium).build();
  }

  /**
   * Calculates the present value volatility sensitivity of the overnight rate in arrears cap/floor trade.
   * <p>
   * The sensitivity of the present value to the SABR model parameters, alpha, beta, rho and nu.
   *
   * @param trade the cap/floor trade
   * @param ratesProvider the rates provider
   * @param volatilities the volatilities
   * @return the present value sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivityModelParamsSabr(
      ResolvedOvernightInArrearsCapFloorTrade trade,
      RatesProvider ratesProvider,
      SabrParametersIborCapletFloorletVolatilities volatilities) {

    return productPricer.presentValueSensitivityModelParamsSabr(trade.getProduct(), ratesProvider, volatilities);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the overnight rate in arrears cap/floor trade.
   *
   * @param trade the cap/floor trade
   * @param ratesProvider the rates provider
   * @param volatilities the volatilities
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedOvernightInArrearsCapFloorTrade trade,
      RatesProvider ratesProvider,
      SabrParametersIborCapletFloorletVolatilities volatilities) {

    MultiCurrencyAmount ceProduct = productPricer.currencyExposure(trade.getProduct(), ratesProvider, volatilities);
    if (!trade.getPremium().isPresent()) {
      return ceProduct;
    }
    CurrencyAmount pvPremium = paymentPricer.presentValue(trade.getPremium().get(), ratesProvider);
    return ceProduct.plus(pvPremium);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the current cash of the overnight rate in arrears cap/floor trade.
   *
   * @param trade the cap/floor trade
   * @param ratesProvider the rates provider
   * @param volatilities the volatilities
   * @return the current cash
   */
  public MultiCurrencyAmount currentCash(
      ResolvedOvernightInArrearsCapFloorTrade trade,
      RatesProvider ratesProvider,
      SabrParametersIborCapletFloorletVolatilities volatilities) {

    MultiCurrencyAmount ccProduct = productPricer.currentCash(trade.getProduct(), ratesProvider, volatilities);
    if (!trade.getPremium().isPresent()) {
      return ccProduct;
    }
    Payment premium = trade.getPremium().get();
    if (premium.getDate().equals(ratesProvider.getValuationDate())) {
      ccProduct = ccProduct.plus(premium.getCurrency(), premium.getAmount());
    }
    return ccProduct;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the forward rates for each caplet/floorlet of the overnight rate in arrears cap/floor trade.
   *
   * @param trade the cap/floor trade
   * @param ratesProvider the rates provider
   * @return the forward rates
   */
  public OvernightInArrearsCapletFloorletPeriodAmounts forwardRates(
      ResolvedOvernightInArrearsCapFloorTrade trade,
      RatesProvider ratesProvider) {

    return productPricer.forwardRates(trade.getProduct(), ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the implied volatilities for each caplet/floorlet of the overnight rate in arrears cap/floor trade.
   *
   * @param trade the cap/floor trade
   * @param ratesProvider the rates provider
   * @param volatilities the volatilities
   * @return the implied volatilities
   */
  public OvernightInArrearsCapletFloorletPeriodAmounts impliedVolatilities(
      ResolvedOvernightInArrearsCapFloorTrade trade,
      RatesProvider ratesProvider,
      SabrParametersIborCapletFloorletVolatilities volatilities) {

    return productPricer.impliedVolatilities(trade.getProduct(), ratesProvider, volatilities);
  }

}
