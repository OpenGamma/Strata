/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.bond;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.finance.Security;
import com.opengamma.strata.finance.rate.bond.ExpandedFixedCouponBond;
import com.opengamma.strata.finance.rate.bond.FixedCouponBond;
import com.opengamma.strata.finance.rate.bond.FixedCouponBondTrade;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.RepoCurveZeroRateSensitivity;
import com.opengamma.strata.market.sensitivity.ZeroRateSensitivity;
import com.opengamma.strata.market.value.CompoundedRateType;
import com.opengamma.strata.market.value.IssuerCurveDiscountFactors;
import com.opengamma.strata.market.value.RepoCurveDiscountFactors;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.LegalEntityDiscountingProvider;

/**
 * Pricer for for rate fixed coupon bond trades.
 * <p>
 * This function provides the ability to price a {@link FixedCouponBondTrade}.
 */
public class DiscountingFixedCouponBondTradePricer {

  /**
   * Default implementation.
   */
  public static final DiscountingFixedCouponBondTradePricer DEFAULT = new DiscountingFixedCouponBondTradePricer(
      DiscountingFixedCouponBondProductPricer.DEFAULT,
      DiscountingPaymentPricer.DEFAULT);

  /**
   * Pricer for {@link FixedCouponBond}.
   */
  private final DiscountingFixedCouponBondProductPricer productPricer;
  /**
   * Pricer for {@link Payment}.
   */
  private final DiscountingPaymentPricer paymentPricer;

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link FixedCouponBond}
   * @param paymentPricer  the pricer for {@link Payment}
  */
  public DiscountingFixedCouponBondTradePricer(
      DiscountingFixedCouponBondProductPricer productPricer,
      DiscountingPaymentPricer paymentPricer) {

    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
    this.paymentPricer = ArgChecker.notNull(paymentPricer, "paymentPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the fixed coupon bond trade.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * The result is expressed using the payment currency of the bond.
   * <p>
   * Coupon payments of the underlying product are considered based on the settlement date of the trade. 
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @return the present value of the fixed coupon bond trade
   */
  public CurrencyAmount presentValue(FixedCouponBondTrade trade, LegalEntityDiscountingProvider provider) {
    LocalDate settlementDate = trade.getTradeInfo().getSettlementDate().get();
    CurrencyAmount pvProduct = productPricer.presentValue(trade.getProduct(), provider, settlementDate);
    return presentValueFromProductPresentValue(trade, provider, pvProduct);
  }

  /**
   * Calculates the present value of the fixed coupon bond trade with z-spread.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * The result is expressed using the payment currency of the bond.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic
   * compounded rates of the discounting curve.
   * <p>
   * Coupon payments of the underlying product are considered based on the settlement date of the trade. 
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the present value of the fixed coupon bond trade
   */
  public CurrencyAmount presentValueWithZSpread(
      FixedCouponBondTrade trade,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    LocalDate settlementDate = trade.getTradeInfo().getSettlementDate().get();
    CurrencyAmount pvProduct = productPricer.presentValueWithZSpread(
        trade.getProduct(), provider, zSpread, compoundedRateType, periodsPerYear, settlementDate);
    return presentValueFromProductPresentValue(trade, provider, pvProduct);
  }

  private CurrencyAmount presentValueFromProductPresentValue(
      FixedCouponBondTrade trade,
      LegalEntityDiscountingProvider provider,
      CurrencyAmount productPresentValue) {
    CurrencyAmount pvProduct = productPresentValue.multipliedBy(trade.getQuantity());
    CurrencyAmount pvPayment = presentValuePayment(trade, provider);
    return pvProduct.plus(pvPayment);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the fixed coupon bond trade from the clean price of the underlying product.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * The result is expressed using the payment currency of the bond.
   * <p>
   * Coupon payments of the underlying product are considered based on the settlement date of the trade. 
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @param cleanPrice  the clean price
   * @return the present value of the fixed coupon bond trade
   */
  public CurrencyAmount presentValueFromCleanPrice(
      FixedCouponBondTrade trade,
      LegalEntityDiscountingProvider provider,
      double cleanPrice) {

    Security<FixedCouponBond> security = trade.getSecurity();
    FixedCouponBond product = security.getProduct();
    LocalDate standardSettlementDate = product.getSettlementDateOffset().adjust(provider.getValuationDate());
    LocalDate tradeSettlementDate = trade.getTradeInfo().getSettlementDate().get();
    StandardId securityId = security.getStandardId();
    StandardId legalEntityId = product.getLegalEntityId();
    Currency currency = product.getCurrency();
    double df = provider.repoCurveDiscountFactors(securityId, legalEntityId, currency).discountFactor(standardSettlementDate);
    double pvStandard =
        (cleanPrice * product.getNotional() + productPricer.accruedInterest(product, standardSettlementDate)) * df;
    if (standardSettlementDate.isEqual(tradeSettlementDate)) {
      return presentValueFromProductPresentValue(trade, provider, CurrencyAmount.of(currency, pvStandard));
    }
    // check coupon payment between two settlement dates
    IssuerCurveDiscountFactors discountFactors = provider.issuerCurveDiscountFactors(legalEntityId, currency);
    ExpandedFixedCouponBond expanded = product.expand();
    boolean exCoupon = product.getExCouponPeriod().getDays() != 0;
    double pvDiff = 0d;
    if (standardSettlementDate.isAfter(tradeSettlementDate)) {
      pvDiff = productPricer.presentValueCoupon(
          expanded, discountFactors, tradeSettlementDate, standardSettlementDate, exCoupon);
    } else {
      pvDiff = -productPricer.presentValueCoupon(
          expanded, discountFactors, standardSettlementDate, tradeSettlementDate, exCoupon);
    }
    return presentValueFromProductPresentValue(trade, provider, CurrencyAmount.of(currency, pvStandard + pvDiff));
  }

  /**
   * Calculates the present value of the fixed coupon bond trade with z-spread from the
   * clean price of the underlying product.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * The result is expressed using the payment currency of the bond.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic
   * compounded rates of the discounting curve.
   * <p>
   * Coupon payments of the underlying product are considered based on the settlement date of the trade. 
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @param cleanPrice  the clean price
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the present value of the fixed coupon bond trade
   */
  public CurrencyAmount presentValueFromCleanPriceWithZSpread(
      FixedCouponBondTrade trade,
      LegalEntityDiscountingProvider provider,
      double cleanPrice,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    Security<FixedCouponBond> security = trade.getSecurity();
    FixedCouponBond product = security.getProduct();
    LocalDate standardSettlementDate = product.getSettlementDateOffset().adjust(provider.getValuationDate());
    LocalDate tradeSettlementDate = trade.getTradeInfo().getSettlementDate().get();
    StandardId securityId = security.getStandardId();
    StandardId legalEntityId = product.getLegalEntityId();
    Currency currency = product.getCurrency();
    double df = provider.repoCurveDiscountFactors(securityId, legalEntityId, currency).discountFactor(
        standardSettlementDate);
    double pvStandard =
        (cleanPrice * product.getNotional() + productPricer.accruedInterest(product, standardSettlementDate)) * df;
    if (standardSettlementDate.isEqual(tradeSettlementDate)) {
      return presentValueFromProductPresentValue(trade, provider, CurrencyAmount.of(currency, pvStandard));
    }
    // check coupon payment between two settlement dates
    IssuerCurveDiscountFactors discountFactors = provider.issuerCurveDiscountFactors(legalEntityId, currency);
    ExpandedFixedCouponBond expanded = product.expand();
    boolean exCoupon = product.getExCouponPeriod().getDays() != 0;
    double pvDiff = 0d;
    if (standardSettlementDate.isAfter(tradeSettlementDate)) {
      pvDiff = productPricer.presentValueCouponWithZSpread(
          expanded, discountFactors, tradeSettlementDate, standardSettlementDate, zSpread, compoundedRateType, periodsPerYear, exCoupon);
    } else {
      pvDiff = -productPricer.presentValueCouponWithZSpread(
          expanded, discountFactors, standardSettlementDate, tradeSettlementDate, zSpread, compoundedRateType, periodsPerYear, exCoupon);
    }
    return presentValueFromProductPresentValue(trade, provider, CurrencyAmount.of(currency, pvStandard + pvDiff));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the fixed coupon bond trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * <p>
   * Coupon payments of the underlying product are considered based on the settlement date of the trade. 
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @return the present value curve sensitivity of the trade
   */
  public PointSensitivityBuilder presentValueSensitivity(
      FixedCouponBondTrade trade,
      LegalEntityDiscountingProvider provider) {

    LocalDate settlementDate = trade.getTradeInfo().getSettlementDate().get();
    PointSensitivityBuilder sensiProduct = productPricer.presentValueSensitivity(
        trade.getProduct(), provider, settlementDate);
    return presnetValueSensitivityFromProductPresentValueSensitivity(trade, provider, sensiProduct);
  }

  /**
   * Calculates the present value sensitivity of the fixed coupon bond trade with z-spread.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic
   * compounded rates of the discounting curve.
   * <p>
   * Coupon payments of the underlying product are considered based on the settlement date of the trade. 
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the present value curve sensitivity of the trade
   */
  public PointSensitivityBuilder presentValueSensitivityWithZSpread(
      FixedCouponBondTrade trade,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    LocalDate settlementDate = trade.getTradeInfo().getSettlementDate().get();
    PointSensitivityBuilder sensiProduct = productPricer.presentValueSensitivityWithZSpread(
        trade.getProduct(), provider, zSpread, compoundedRateType, periodsPerYear, settlementDate);
    return presnetValueSensitivityFromProductPresentValueSensitivity(trade, provider, sensiProduct);
  }

  private PointSensitivityBuilder presnetValueSensitivityFromProductPresentValueSensitivity(
      FixedCouponBondTrade trade,
      LegalEntityDiscountingProvider provider,
      PointSensitivityBuilder productPresnetValueSensitivity) {

    PointSensitivityBuilder sensiProduct = productPresnetValueSensitivity.multipliedBy(trade.getQuantity());
    PointSensitivityBuilder sensiPayment = presentValueSensitivityPayment(trade, provider);
    return sensiProduct.combinedWith(sensiPayment);
  }

  //-------------------------------------------------------------------------
  private CurrencyAmount presentValuePayment(FixedCouponBondTrade trade, LegalEntityDiscountingProvider provider) {
    FixedCouponBond product = trade.getProduct();
    RepoCurveDiscountFactors discountFactors = provider.repoCurveDiscountFactors(
        product.getLegalEntityId(), trade.getSecurity().getStandardId(), product.getCurrency());
    return paymentPricer.presentValue(trade.getPayment(), discountFactors.getDiscountFactors());
  }

  private PointSensitivityBuilder presentValueSensitivityPayment(
      FixedCouponBondTrade trade,
      LegalEntityDiscountingProvider provider) {

    FixedCouponBond product = trade.getProduct();
    RepoCurveDiscountFactors discountFactors = provider.repoCurveDiscountFactors(
        product.getLegalEntityId(), trade.getSecurity().getStandardId(), product.getCurrency());
    PointSensitivityBuilder pt = paymentPricer.presentValueSensitivity(
        trade.getPayment(), discountFactors.getDiscountFactors());
    if (pt instanceof ZeroRateSensitivity) {
      return RepoCurveZeroRateSensitivity.of((ZeroRateSensitivity) pt, discountFactors.getBondGroup());
    }
    return pt; // NoPointSensitivity
  }

}
