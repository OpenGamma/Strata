/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.future;

import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.platform.finance.future.ExpandedIborFuture;
import com.opengamma.platform.finance.future.IborFutureSecurityTrade;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.future.IborFutureProductPricerFn;

/**
 * Pricer implementation for swaps.
 * <p>
 * The swap is priced by examining the swap legs.
 */
public class DefaultExpandedIborFuturePricerFn
    implements IborFutureProductPricerFn<ExpandedIborFuture> {

  /**
   * Default implementation.
   */
  public static final DefaultExpandedIborFuturePricerFn DEFAULT = new DefaultExpandedIborFuturePricerFn();

  /**
   * Creates an instance.
   */
  public DefaultExpandedIborFuturePricerFn() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double price(PricingEnvironment env, ExpandedIborFuture iborFuture) {
    // FuturesPriceMulticurveCalculator.visitInterestRateFutureSecurity()
    double forward = env.iborIndexRate(iborFuture.getRate().getIndex(), iborFuture.getRate().getFixingDate());
    // TODO this is an analytic price, not a user one
    return (1.0 - forward);
  }

  public CurrencyAmount presentValue(
      PricingEnvironment env,
      ExpandedIborFuture iborFuture,
      IborFutureSecurityTrade trade,
      double lastMarginPrice) {

    // FuturesTransactionMulticurveMethod.presentValue()
    double price = price(env, iborFuture);
    // FuturesTransactionMethod.presentValueFromPrice()
    double priceIndex = marginIndex(iborFuture, price);
    // TODO check "referencePrice" = "paymentAmount"
    double referenceIndex = marginIndex(iborFuture, lastMarginPrice);
    double pv = (priceIndex - referenceIndex) * trade.getMultiplier();
    return CurrencyAmount.of(iborFuture.getCurrency(), pv);
  }

  // referencePrice
  // Provides the reference margin price,
  // for futures, options and other exchange traded securities that are margined. <p>
  // This is typically last night's close price, but may, on the trade date itself, be the trade price.<p>

  private double marginIndex(final ExpandedIborFuture iborFuture, double price) {
    // FuturesMarginIndexFromPriceCalculator.visitInterestRateFutureSecurity()
    return price * iborFuture.getNotional() * iborFuture.getAccrualFactor();
  }

  // securityMarketPrice()
  // MarginPriceVisitor
  // the referencePrice on the trade

  // securityModelPrice() -
  // MarketQuoteDiscountingCalculator -> FuturesSecurityMulticurveMethod -> FuturesPriceMulticurveCalculator
  // (1.0 - iborRateFromCurve)

  // presentValue()
  // PresentValueDiscountingCalculator -> FuturesTransactionMulticurveMethod
  // as above

  // parRate()
  // ParRateDiscountingCalculator -> InterestRateFutureSecurityDiscountingMethod
  // iborRateFromCurve

  // PV01
  // new PV01CurveParametersCalculator<>(PresentValueCurveSensitivityDiscountingCalculator.getInstance())
  // result * 100  (calc based on sensitivity)

  // FuturesTransactionDefinition sets up referencePrice
  // lastMarginPrice passed in from InterestRateFutureTransactionDefinition
  // toDerivative passes in last fixing from market
}
