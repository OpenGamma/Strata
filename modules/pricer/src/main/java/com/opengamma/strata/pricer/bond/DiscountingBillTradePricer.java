/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.CompoundedRateType;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.product.bond.ResolvedBill;
import com.opengamma.strata.product.bond.ResolvedBillTrade;

/**
 * Pricer for bill trades.
 * <p>
 * This function provides the ability to price a {@link ResolvedBillTrade}.
 */
public class DiscountingBillTradePricer {

  /**
   * Default implementation.
   */
  public static final DiscountingBillTradePricer DEFAULT = new DiscountingBillTradePricer(
      DiscountingBillProductPricer.DEFAULT,
      DiscountingPaymentPricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedBill}.
   */
  private final DiscountingBillProductPricer productPricer;
  /**
   * Pricer for {@link Payment}.
   */
  private final DiscountingPaymentPricer paymentPricer;

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link ResolvedBill}
   * @param paymentPricer  the pricer for {@link Payment}
  */
  public DiscountingBillTradePricer(
      DiscountingBillProductPricer productPricer,
      DiscountingPaymentPricer paymentPricer) {

    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
    this.paymentPricer = ArgChecker.notNull(paymentPricer, "paymentPricer");
  }

  /**
   * Calculates the present value of a bill trade.
   * <p>
   * If the settlement details are provided, the present value is the sum of the underlying product's present value
   * multiplied by the quantity and the present value of the settlement payment if still due at the valuation date. 
   * If not it is the underlying product's present value multiplied by the quantity.
   * 
   * @param trade  the trade
   * @param provider  the discounting provider
   * @return the present value
   */
  public CurrencyAmount presentValue(ResolvedBillTrade trade, LegalEntityDiscountingProvider provider) {
    if (provider.getValuationDate().isAfter(trade.getProduct().getNotional().getDate())) {
      return CurrencyAmount.of(trade.getProduct().getCurrency(), 0.0d);
    }
    CurrencyAmount pvProduct = productPricer.presentValue(trade.getProduct(), provider)
        .multipliedBy(trade.getQuantity());
    if (trade.getSettlement().isPresent()) {
      RepoCurveDiscountFactors repoDf = DiscountingBillProductPricer.repoCurveDf(trade.getProduct(), provider);
      CurrencyAmount pvSettle = paymentPricer.presentValue(trade.getSettlement().get(), repoDf.getDiscountFactors());
      return pvProduct.plus(pvSettle);
    }
    return pvProduct;
  }

  /**
   * Calculates the present value of a bill trade with z-spread.
   * <p>
   * If the settlement details are provided, the present value is the sum of the underlying product's present value
   * multiplied by the quantity and the present value of the settlement payment if still due at the valuation date. 
   * If not it is the underlying product's present value multiplied by the quantity.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic compounded rates of 
   * the issuer discounting curve. The z-spread is applied only on the legal entity curve, not on the repo curve used
   * for the settlement amount.
   * 
   * @param trade  the trade
   * @param provider  the discounting provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the present value
   */
  public CurrencyAmount presentValueWithZSpread(
      ResolvedBillTrade trade,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    if (provider.getValuationDate().isAfter(trade.getProduct().getNotional().getDate())) {
      return CurrencyAmount.of(trade.getProduct().getCurrency(), 0.0d);
    }
    CurrencyAmount pvProduct = productPricer
        .presentValueWithZSpread(trade.getProduct(), provider, zSpread, compoundedRateType, periodsPerYear)
        .multipliedBy(trade.getQuantity());
    if (trade.getSettlement().isPresent()) {
      RepoCurveDiscountFactors repoDf = DiscountingBillProductPricer.repoCurveDf(trade.getProduct(), provider);
      CurrencyAmount pvSettle = paymentPricer.presentValue(trade.getSettlement().get(), repoDf.getDiscountFactors());
      return pvProduct.plus(pvSettle);
    }
    return pvProduct;
  }

  /**
   * Calculates the present value sensitivity of a bill trade.
   * <p>
   * If the settlement details are provided, the sensitivity is the sum of the underlying product's sensitivity
   * multiplied by the quantity and the sensitivity of the settlement payment if still due at the valuation date. 
   * If not it is the underlying product's sensitivity multiplied by the quantity.
   * 
   * @param trade  the trade
   * @param provider  the discounting provider
   * @return the present value sensitivity
   */
  public PointSensitivities presentValueSensitivity(ResolvedBillTrade trade, LegalEntityDiscountingProvider provider) {
    if (provider.getValuationDate().isAfter(trade.getProduct().getNotional().getDate())) {
      return PointSensitivities.empty();
    }
    PointSensitivities sensiProduct = productPricer.presentValueSensitivity(trade.getProduct(), provider)
        .multipliedBy(trade.getQuantity());
    if (!trade.getSettlement().isPresent()) {
      return sensiProduct;
    }
    Payment settlement = trade.getSettlement().get();
    RepoCurveDiscountFactors repoDf = DiscountingBillProductPricer.repoCurveDf(trade.getProduct(), provider);
    PointSensitivities sensiSettle = presentValueSensitivitySettlement(settlement, repoDf);
    return sensiProduct.combinedWith(sensiSettle);
  }

  /**
   * Calculates the present value sensitivity of a bill trade with z-spread.
   * <p>
   * If the settlement details are provided, the sensitivity is the sum of the underlying product's sensitivity
   * multiplied by the quantity and the sensitivity of the settlement payment if still due at the valuation date. 
   * If not it is the underlying product's sensitivity multiplied by the quantity.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic compounded rates of 
   * the issuer discounting curve. The z-spread is applied only on the legal entity curve, not on the repo curve used
   * for the settlement amount.
   * 
   * @param trade  the trade
   * @param provider  the discounting provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the present value sensitivity
   */
  public PointSensitivities presentValueSensitivityWithZSpread(
      ResolvedBillTrade trade,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    if (provider.getValuationDate().isAfter(trade.getProduct().getNotional().getDate())) {
      return PointSensitivities.empty();
    }
    PointSensitivities sensiProduct = productPricer
        .presentValueSensitivityWithZSpread(trade.getProduct(), provider, zSpread, compoundedRateType, periodsPerYear)
        .multipliedBy(trade.getQuantity());
    if (!trade.getSettlement().isPresent()) {
      return sensiProduct;
    }
    Payment settlement = trade.getSettlement().get();
    RepoCurveDiscountFactors repoDf = DiscountingBillProductPricer.repoCurveDf(trade.getProduct(), provider);
    PointSensitivities sensiSettle = presentValueSensitivitySettlement(settlement, repoDf);
    return sensiProduct.combinedWith(sensiSettle);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of a bill trade.
   * 
   * @param trade  the trade
   * @param provider  the discounting provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(ResolvedBillTrade trade, LegalEntityDiscountingProvider provider) {
    return MultiCurrencyAmount.of(presentValue(trade, provider));
  }

  /**
   * Calculates the currency exposure of a bill trade with z-spread.
   * 
   * @param trade  the trade
   * @param provider  the discounting provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposureWithZSpread(
      ResolvedBillTrade trade,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    return MultiCurrencyAmount.of(
        presentValueWithZSpread(trade, provider, zSpread, compoundedRateType, periodsPerYear));
  }

  /**
   * Calculates the current cash of a bill trade.
   * 
   * @param trade  the trade
   * @param valuationDate  the valuation date
   * @return the current cash amount
   */
  public CurrencyAmount currentCash(ResolvedBillTrade trade, LocalDate valuationDate) {
    if (trade.getProduct().getNotional().getDate().equals(valuationDate)) {
      return trade.getProduct().getNotional().getValue().multipliedBy(trade.getQuantity());
    }
    if (trade.getSettlement().isPresent() && trade.getSettlement().get().getDate().equals(valuationDate)) {
      return trade.getSettlement().get().getValue();
    }
    return CurrencyAmount.zero(trade.getProduct().getCurrency());
  }

  //-------------------------------------------------------------------------
  private PointSensitivities presentValueSensitivitySettlement(
      Payment settlement,
      RepoCurveDiscountFactors repoDf) {

    PointSensitivityBuilder pointSettle = paymentPricer.presentValueSensitivity(
        settlement, repoDf.getDiscountFactors());
    if (pointSettle instanceof ZeroRateSensitivity) {
      return RepoCurveZeroRateSensitivity.of((ZeroRateSensitivity) pointSettle, repoDf.getRepoGroup()).build();
    }
    return pointSettle.build(); // NoPointSensitivity
  }

}
