/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.view.IborCapletFloorletVolatilities;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.capfloor.IborCapFloorProduct;
import com.opengamma.strata.product.capfloor.IborCapFloorTrade;

/**
 * Pricer for cap/floor trades based on volatilities.
 * <p>
 * This function provides the ability to price {@link IborCapFloorTrade}. 
 * The pricing methodologies are defined in individual implementations of the volatilities, {@link IborCapletFloorletVolatilities}.
 * <p>
 * Greeks of the underlying product are computed in the product pricer, {@link VolatilityIborCapFloorProductPricer}.
 */
public class VolatilityIborCapFloorTradePricer {

  /**
   * Default implementation.
   */
  public static final VolatilityIborCapFloorTradePricer DEFAULT =
      new VolatilityIborCapFloorTradePricer(VolatilityIborCapFloorProductPricer.DEFAULT, DiscountingPaymentPricer.DEFAULT);
  /**
   * Pricer for {@link IborCapFloorProduct}.
   */
  private final VolatilityIborCapFloorProductPricer productPricer;
  /**
   * Pricer for {@link Payment}.
   */
  private final DiscountingPaymentPricer paymentPricer;

  /**
   * Creates an instance. 
   * 
   * @param productPricer  the pricer for {@link IborCapFloorProduct}
   * @param paymentPricer  the pricer for {@link Payment}
   */
  public VolatilityIborCapFloorTradePricer(
      VolatilityIborCapFloorProductPricer productPricer,
      DiscountingPaymentPricer paymentPricer) {

    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
    this.paymentPricer = ArgChecker.notNull(paymentPricer, "paymentPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the Ibor cap/floor trade.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * 
   * @param trade  the Ibor cap/floor trade
   * @param ratesProvider  the rates provider
   * @param volatilities  the caplet/floorlet volatilities
   * @return the present value
   */
  public MultiCurrencyAmount presentValue(
      IborCapFloorTrade trade,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    MultiCurrencyAmount pvProduct = productPricer.presentValue(trade.getProduct(), ratesProvider, volatilities);
    if (!trade.getPremium().isPresent()) {
      return pvProduct;
    }
    CurrencyAmount pvPremium = paymentPricer.presentValue(trade.getPremium().get(), ratesProvider);
    return pvProduct.plus(pvPremium);
  }

  /**
   * Calculates the present value curve sensitivity of the Ibor cap/floor trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to the underlying curves.
   * 
   * @param trade  the Ibor cap/floor trade
   * @param ratesProvider  the rates provider
   * @param volatilities  the caplet/floorlet volatilities
   * @return the present value sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivity(
      IborCapFloorTrade trade,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    PointSensitivityBuilder pvSensiProduct =
        productPricer.presentValueSensitivity(trade.getProduct(), ratesProvider, volatilities);
    if (!trade.getPremium().isPresent()) {
      return pvSensiProduct;
    }
    PointSensitivityBuilder pvSensiPremium =
        paymentPricer.presentValueSensitivity(trade.getPremium().get(), ratesProvider);
    return pvSensiProduct.combinedWith(pvSensiPremium);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the Ibor cap/floor trade.
   * 
   * @param trade  the Ibor cap/floor trade
   * @param ratesProvider  the rates provider
   * @param volatilities  the caplet/floorlet volatilities
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      IborCapFloorTrade trade,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    MultiCurrencyAmount ceProduct = productPricer.currencyExposure(trade.getProduct(), ratesProvider, volatilities);
    if (!trade.getPremium().isPresent()) {
      return ceProduct;
    }
    CurrencyAmount pvPremium = paymentPricer.presentValue(trade.getPremium().get(), ratesProvider);
    return ceProduct.plus(pvPremium);
  }

  /**
   * Calculates the current cash of the Ibor cap/floor trade.
   * 
   * @param trade  the Ibor cap/floor trade
   * @param ratesProvider  the rates provider
   * @param volatilities  the caplet/floorlet volatilities
   * @return the current cash
   */
  public MultiCurrencyAmount currentCash(
      IborCapFloorTrade trade,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

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

}
