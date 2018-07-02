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
import com.opengamma.strata.pricer.DiscountFactors;
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
   * @param bill  the bill
   * @param provider  the discounting provider
   * @return  the present value
   */
  public CurrencyAmount presentValue(ResolvedBillTrade bill, LegalEntityDiscountingProvider provider) {
    if (provider.getValuationDate().isAfter(bill.getProduct().getNotional().getDate())) {
      return CurrencyAmount.of(bill.getProduct().getCurrency(), 0.0d);
    }
    CurrencyAmount pvProduct = productPricer.presentValue(bill.getProduct(), provider)
        .multipliedBy(bill.getQuantity());
    if (bill.getSettlement().isPresent()) {
      DiscountFactors discountFactorsRepo = provider.repoCurveDiscountFactors(
          bill.getProduct().getSecurityId(), bill.getProduct().getLegalEntityId(), bill.getProduct().getCurrency())
          .getDiscountFactors();
      CurrencyAmount pvSettle = paymentPricer.presentValue(bill.getSettlement().get(), discountFactorsRepo);
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
   * @param bill  the bill
   * @param provider  the discounting provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return  the present value
   */
  public CurrencyAmount presentValueWithZSpread(
      ResolvedBillTrade bill, 
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    if (provider.getValuationDate().isAfter(bill.getProduct().getNotional().getDate())) {
      return CurrencyAmount.of(bill.getProduct().getCurrency(), 0.0d);
    }
    CurrencyAmount pvProduct = productPricer
        .presentValueWithZSpread(bill.getProduct(), provider, zSpread, compoundedRateType, periodsPerYear)
        .multipliedBy(bill.getQuantity());
    if (bill.getSettlement().isPresent()) {
      DiscountFactors discountFactorsRepo = provider.repoCurveDiscountFactors(
          bill.getProduct().getSecurityId(), bill.getProduct().getLegalEntityId(), bill.getProduct().getCurrency())
          .getDiscountFactors();
      CurrencyAmount pvSettle = paymentPricer.presentValue(bill.getSettlement().get(), discountFactorsRepo);
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
   * @param bill  the bill
   * @param provider  the discounting provider
   * @return  the present value sensitivity
   */
  public PointSensitivities presentValueSensitivity(ResolvedBillTrade bill, LegalEntityDiscountingProvider provider) {
    if (provider.getValuationDate().isAfter(bill.getProduct().getNotional().getDate())) {
      return PointSensitivities.empty();
    }
    PointSensitivities sensiProduct = productPricer.presentValueSensitivity(bill.getProduct(), provider)
        .multipliedBy(bill.getQuantity());
    if (bill.getSettlement().isPresent()) {
      Payment settlement = bill.getSettlement().get();
      RepoCurveDiscountFactors discountFactorsRepo = provider.repoCurveDiscountFactors(
          bill.getProduct().getSecurityId(), bill.getProduct().getLegalEntityId(), bill.getProduct().getCurrency());
      PointSensitivities sensiSettle = presentValueSensitivitySettlement(settlement, discountFactorsRepo);
      return sensiProduct.combinedWith(sensiSettle);
    }
    return sensiProduct;
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
   * @param bill  the bill
   * @param provider  the discounting provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return  the present value sensitivity
   */
  public PointSensitivities presentValueSensitivityWithZSpread(
      ResolvedBillTrade bill, 
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    if (provider.getValuationDate().isAfter(bill.getProduct().getNotional().getDate())) {
      return PointSensitivities.empty();
    }
    PointSensitivities sensiProduct = productPricer
        .presentValueSensitivityWithZSpread(bill.getProduct(), provider, zSpread, compoundedRateType, periodsPerYear)
        .multipliedBy(bill.getQuantity());
    if (bill.getSettlement().isPresent()) {
      Payment settlement = bill.getSettlement().get();
      RepoCurveDiscountFactors discountFactorsRepo = provider.repoCurveDiscountFactors(
          bill.getProduct().getSecurityId(), bill.getProduct().getLegalEntityId(), bill.getProduct().getCurrency());
      PointSensitivities sensiSettle = presentValueSensitivitySettlement(settlement, discountFactorsRepo);
      return sensiProduct.combinedWith(sensiSettle);
    }
    return sensiProduct;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of a bill trade.
   * 
   * @param bill  the bill's trade
   * @param provider  the discounting provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(ResolvedBillTrade bill, LegalEntityDiscountingProvider provider) {
    return MultiCurrencyAmount.of(presentValue(bill, provider));
  }
  
  /**
   * Calculates the currency exposure of a bill trade with z-spread.
   * 
   * @param bill  the bill's trade
   * @param provider  the discounting provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposureWithZSpread(
      ResolvedBillTrade bill, 
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {
    return MultiCurrencyAmount.of(presentValueWithZSpread(bill, provider, zSpread, compoundedRateType, periodsPerYear));
  }

  /**
   * Calculates the current cash of a bill trade.
   * 
   * @param bill  the bill's trade
   * @param valuationDate  the valuation date
   * @return the current cash amount
   */
  public CurrencyAmount currentCash(ResolvedBillTrade bill, LocalDate valuationDate) {
    if (bill.getProduct().getNotional().getDate().equals(valuationDate)) {
      return bill.getProduct().getNotional().getValue().multipliedBy(bill.getQuantity());
    }
    if (bill.getSettlement().isPresent() && bill.getSettlement().get().getDate().equals(valuationDate)) {
      return bill.getSettlement().get().getValue();
    }
    return CurrencyAmount.zero(bill.getProduct().getCurrency());
  }

  //-------------------------------------------------------------------------
  private PointSensitivities presentValueSensitivitySettlement(
      Payment settlement,
      RepoCurveDiscountFactors discountFactorsRepo) {

    PointSensitivityBuilder pointSettle = paymentPricer.presentValueSensitivity(
        settlement, discountFactorsRepo.getDiscountFactors());
    if (pointSettle instanceof ZeroRateSensitivity) {
      return RepoCurveZeroRateSensitivity.of((ZeroRateSensitivity) pointSettle, discountFactorsRepo.getRepoGroup()).build();
    }
    return pointSettle.build(); // NoPointSensitivity
  }

}
