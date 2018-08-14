/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import java.time.LocalDate;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.CompoundedRateType;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.bond.FixedCouponBondPaymentPeriod;
import com.opengamma.strata.product.bond.ResolvedFixedCouponBond;
import com.opengamma.strata.product.bond.ResolvedFixedCouponBondSettlement;
import com.opengamma.strata.product.bond.ResolvedFixedCouponBondTrade;

/**
 * Pricer for fixed coupon bond trades.
 * <p>
 * This function provides the ability to price a {@link ResolvedFixedCouponBondTrade}.
 * 
 * <h4>Price</h4>
 * Strata uses <i>decimal prices</i> for bonds in the trade model, pricers and market data.
 * For example, a price of 99.32% is represented in Strata by 0.9932.
 */
public class DiscountingFixedCouponBondTradePricer {

  /**
   * Default implementation.
   */
  public static final DiscountingFixedCouponBondTradePricer DEFAULT = new DiscountingFixedCouponBondTradePricer(
      DiscountingFixedCouponBondProductPricer.DEFAULT,
      DiscountingPaymentPricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedFixedCouponBond}.
   */
  private final DiscountingFixedCouponBondProductPricer productPricer;
  /**
   * Pricer for {@link Payment}.
   */
  private final DiscountingPaymentPricer paymentPricer;

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link ResolvedFixedCouponBond}
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
   * @param trade  the trade
   * @param provider  the discounting provider
   * @return the present value of the fixed coupon bond trade
   */
  public CurrencyAmount presentValue(ResolvedFixedCouponBondTrade trade, LegalEntityDiscountingProvider provider) {
    LocalDate settlementDate = settlementDate(trade, provider.getValuationDate());
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
   * @param trade  the trade
   * @param provider  the discounting provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the present value of the fixed coupon bond trade
   */
  public CurrencyAmount presentValueWithZSpread(
      ResolvedFixedCouponBondTrade trade,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    LocalDate settlementDate = settlementDate(trade, provider.getValuationDate());
    CurrencyAmount pvProduct = productPricer.presentValueWithZSpread(
        trade.getProduct(), provider, zSpread, compoundedRateType, periodsPerYear, settlementDate);
    return presentValueFromProductPresentValue(trade, provider, pvProduct);
  }

  private CurrencyAmount presentValueFromProductPresentValue(
      ResolvedFixedCouponBondTrade trade,
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
   * @param trade  the trade
   * @param provider  the discounting provider
   * @param refData  the reference data used to calculate the settlement date
   * @param cleanPrice  the clean price
   * @return the present value of the fixed coupon bond trade
   */
  public CurrencyAmount presentValueFromCleanPrice(
      ResolvedFixedCouponBondTrade trade,
      LegalEntityDiscountingProvider provider,
      ReferenceData refData,
      double cleanPrice) {

    ResolvedFixedCouponBond product = trade.getProduct();
    LocalDate standardSettlementDate = standardSettlementDate(product, provider, refData);
    LocalDate tradeSettlementDate = settlementDate(trade, provider.getValuationDate());
    Currency currency = product.getCurrency();
    RepoCurveDiscountFactors repoDf = DiscountingFixedCouponBondProductPricer.repoCurveDf(product, provider);
    double df = repoDf.discountFactor(standardSettlementDate);
    double pvStandard =
        (cleanPrice * product.getNotional() + productPricer.accruedInterest(product, standardSettlementDate)) * df;
    if (standardSettlementDate.isEqual(tradeSettlementDate)) {
      return presentValueFromProductPresentValue(trade, provider, CurrencyAmount.of(currency, pvStandard));
    }
    // check coupon payment between two settlement dates
    IssuerCurveDiscountFactors issuerDf = DiscountingFixedCouponBondProductPricer.issuerCurveDf(product, provider);
    double pvDiff = 0d;
    if (standardSettlementDate.isAfter(tradeSettlementDate)) {
      pvDiff = productPricer.presentValueCoupon(product, issuerDf, tradeSettlementDate, standardSettlementDate);
    } else {
      pvDiff = -productPricer.presentValueCoupon(product, issuerDf, standardSettlementDate, tradeSettlementDate);
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
   * @param trade  the trade
   * @param provider  the discounting provider
   * @param refData  the reference data used to calculate the settlement date
   * @param cleanPrice  the clean price
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the present value of the fixed coupon bond trade
   */
  public CurrencyAmount presentValueFromCleanPriceWithZSpread(
      ResolvedFixedCouponBondTrade trade,
      LegalEntityDiscountingProvider provider,
      ReferenceData refData,
      double cleanPrice,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    ResolvedFixedCouponBond product = trade.getProduct();
    LocalDate standardSettlementDate = standardSettlementDate(product, provider, refData);
    LocalDate tradeSettlementDate = settlementDate(trade, provider.getValuationDate());
    Currency currency = product.getCurrency();
    RepoCurveDiscountFactors repoDf = DiscountingFixedCouponBondProductPricer.repoCurveDf(product, provider);
    double df = repoDf.discountFactor(standardSettlementDate);
    double pvStandard =
        (cleanPrice * product.getNotional() + productPricer.accruedInterest(product, standardSettlementDate)) * df;
    if (standardSettlementDate.isEqual(tradeSettlementDate)) {
      return presentValueFromProductPresentValue(trade, provider, CurrencyAmount.of(currency, pvStandard));
    }
    // check coupon payment between two settlement dates
    IssuerCurveDiscountFactors issuerDf = DiscountingFixedCouponBondProductPricer.issuerCurveDf(product, provider);
    double pvDiff = 0d;
    if (standardSettlementDate.isAfter(tradeSettlementDate)) {
      pvDiff = productPricer.presentValueCouponWithZSpread(
          product,
          issuerDf,
          tradeSettlementDate,
          standardSettlementDate,
          zSpread,
          compoundedRateType,
          periodsPerYear);
    } else {
      pvDiff = -productPricer.presentValueCouponWithZSpread(
          product,
          issuerDf,
          standardSettlementDate,
          tradeSettlementDate,
          zSpread,
          compoundedRateType,
          periodsPerYear);
    }
    return presentValueFromProductPresentValue(trade, provider, CurrencyAmount.of(currency, pvStandard + pvDiff));
  }

  // calculates the settlement date using the offset from the valuation date
  private LocalDate standardSettlementDate(
      ResolvedFixedCouponBond product,
      LegalEntityDiscountingProvider provider,
      ReferenceData refData) {

    return product.getSettlementDateOffset().adjust(provider.getValuationDate(), refData);
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
   * @param trade  the trade
   * @param provider  the discounting provider
   * @return the present value curve sensitivity of the trade
   */
  public PointSensitivities presentValueSensitivity(
      ResolvedFixedCouponBondTrade trade,
      LegalEntityDiscountingProvider provider) {

    LocalDate settlementDate = settlementDate(trade, provider.getValuationDate());
    PointSensitivityBuilder sensiProduct = productPricer.presentValueSensitivity(
        trade.getProduct(), provider, settlementDate);
    return presentValueSensitivityFromProductPresentValueSensitivity(trade, provider, sensiProduct).build();
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
   * @param trade  the trade
   * @param provider  the discounting provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the present value curve sensitivity of the trade
   */
  public PointSensitivities presentValueSensitivityWithZSpread(
      ResolvedFixedCouponBondTrade trade,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    LocalDate settlementDate = settlementDate(trade, provider.getValuationDate());
    PointSensitivityBuilder sensiProduct = productPricer.presentValueSensitivityWithZSpread(
        trade.getProduct(), provider, zSpread, compoundedRateType, periodsPerYear, settlementDate);
    return presentValueSensitivityFromProductPresentValueSensitivity(trade, provider, sensiProduct).build();
  }

  private PointSensitivityBuilder presentValueSensitivityFromProductPresentValueSensitivity(
      ResolvedFixedCouponBondTrade trade,
      LegalEntityDiscountingProvider provider,
      PointSensitivityBuilder productPresnetValueSensitivity) {

    PointSensitivityBuilder sensiProduct = productPresnetValueSensitivity.multipliedBy(trade.getQuantity());
    PointSensitivityBuilder sensiPayment = presentValueSensitivityPayment(trade, provider);
    return sensiProduct.combinedWith(sensiPayment);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the fixed coupon bond trade.
   * 
   * @param trade  the trade
   * @param provider  the discounting provider
   * @return the currency exposure of the fixed coupon bond trade
   */
  public MultiCurrencyAmount currencyExposure(ResolvedFixedCouponBondTrade trade, LegalEntityDiscountingProvider provider) {

    return MultiCurrencyAmount.of(presentValue(trade, provider));
  }

  /**
   * Calculates the currency exposure of the fixed coupon bond trade with z-spread.
   * 
   * @param trade  the trade
   * @param provider  the discounting provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the currency exposure of the fixed coupon bond trade
   */
  public MultiCurrencyAmount currencyExposureWithZSpread(
      ResolvedFixedCouponBondTrade trade,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    return MultiCurrencyAmount.of(presentValueWithZSpread(trade, provider, zSpread, compoundedRateType, periodsPerYear));
  }

  /**
   * Calculates the current cash of the fixed coupon bond trade.
   * 
   * @param trade  the trade
   * @param valuationDate  the valuation date
   * @return the current cash amount
   */
  public CurrencyAmount currentCash(ResolvedFixedCouponBondTrade trade, LocalDate valuationDate) {
    Payment upfrontPayment = upfrontPayment(trade);
    Currency currency = upfrontPayment.getCurrency(); // assumes single currency is involved in trade
    CurrencyAmount currentCash = CurrencyAmount.zero(currency);
    if (upfrontPayment.getDate().equals(valuationDate)) {
      currentCash = currentCash.plus(upfrontPayment.getValue());
    }
    if (trade.getSettlement().isPresent()) {
      LocalDate settlementDate = trade.getSettlement().get().getSettlementDate();
      ResolvedFixedCouponBond product = trade.getProduct();
      if (!settlementDate.isAfter(valuationDate)) {
        double cashCoupon = product.hasExCouponPeriod() ? 0d : currentCashCouponPayment(product, valuationDate);
        Payment payment = product.getNominalPayment();
        double cashNominal = payment.getDate().isEqual(valuationDate) ? payment.getAmount() : 0d;
        currentCash = currentCash.plus(CurrencyAmount.of(currency, (cashCoupon + cashNominal) * trade.getQuantity()));
      }
    }
    return currentCash;
  }

  private double currentCashCouponPayment(ResolvedFixedCouponBond product, LocalDate referenceDate) {
    double cash = 0d;
    for (FixedCouponBondPaymentPeriod period : product.getPeriodicPayments()) {
      if (period.getPaymentDate().isEqual(referenceDate)) {
        cash += period.getFixedRate() * period.getNotional() * period.getYearFraction();
      }
    }
    return cash;
  }

  //-------------------------------------------------------------------------
  private CurrencyAmount presentValuePayment(ResolvedFixedCouponBondTrade trade, LegalEntityDiscountingProvider provider) {
    RepoCurveDiscountFactors repoDf = DiscountingFixedCouponBondProductPricer.repoCurveDf(trade.getProduct(), provider);
    Payment upfrontPayment = upfrontPayment(trade);
    return paymentPricer.presentValue(upfrontPayment, repoDf.getDiscountFactors());
  }

  private PointSensitivityBuilder presentValueSensitivityPayment(
      ResolvedFixedCouponBondTrade trade,
      LegalEntityDiscountingProvider provider) {

    RepoCurveDiscountFactors repoDf = DiscountingFixedCouponBondProductPricer.repoCurveDf(trade.getProduct(), provider);
    Payment upfrontPayment = upfrontPayment(trade);
    PointSensitivityBuilder pt = paymentPricer.presentValueSensitivity(
        upfrontPayment, repoDf.getDiscountFactors());
    if (pt instanceof ZeroRateSensitivity) {
      return RepoCurveZeroRateSensitivity.of((ZeroRateSensitivity) pt, repoDf.getRepoGroup());
    }
    return pt; // NoPointSensitivity
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the payment that was made for the trade.
   * <p>
   * This is the payment that was made on the settlement date, based on the quantity and clean price.
   * 
   * @param trade  the trade
   * @return the payment that was made
   */
  public Payment upfrontPayment(ResolvedFixedCouponBondTrade trade) {
    ResolvedFixedCouponBond product = trade.getProduct();
    Currency currency = product.getCurrency();
    if (!trade.getSettlement().isPresent()) {
      return Payment.of(CurrencyAmount.zero(currency), product.getStartDate());  // date doesn't matter as it is zero
    }
    // payment is based on the dirty price
    ResolvedFixedCouponBondSettlement settlement = trade.getSettlement().get();
    LocalDate settlementDate = settlement.getSettlementDate();
    double cleanPrice = settlement.getPrice();
    double dirtyPrice = productPricer.dirtyPriceFromCleanPrice(product, settlementDate, cleanPrice);
    // calculate payment
    double quantity = trade.getQuantity();
    double notional = product.getNotional();
    return Payment.of(CurrencyAmount.of(currency, -quantity * notional * dirtyPrice), settlementDate);
  }

  //-------------------------------------------------------------------------
  // calculate the settlement date
  private LocalDate settlementDate(ResolvedFixedCouponBondTrade trade, LocalDate valuationDate) {
    return trade.getSettlement()
        .map(settle -> settle.getSettlementDate())
        .orElse(valuationDate);
  }

}
