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
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
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
    if (bill.getProduct().getNotional().getDate().isAfter(provider.getValuationDate())) {
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
   * Calculates the present value sensitivity of a bill trade.
   * <p>
   * If the settlement details are provided, the sensitivity is the sum of the underlying product's sensitivity
   * multiplied by the quantity and the sensitivity of the settlement payment if still due at the valuation date. 
   * If not it is the underlying product's sensitivity multiplied by the quantity.
   * 
   * @param bill  the bill
   * @param provider  the discounting provider
   * @return  the present value
   */
  public PointSensitivities presentValueSensitivity(ResolvedBillTrade bill, LegalEntityDiscountingProvider provider) {
    if (bill.getProduct().getNotional().getDate().isAfter(provider.getValuationDate())) {
      return PointSensitivities.empty();
    }
    PointSensitivities sensiProduct = productPricer.presentValueSensitivity(bill.getProduct(), provider)
        .multipliedBy(bill.getQuantity());
    if (bill.getSettlement().isPresent()) {
      DiscountFactors discountFactorsRepo = provider.repoCurveDiscountFactors(
          bill.getProduct().getSecurityId(), bill.getProduct().getLegalEntityId(), bill.getProduct().getCurrency())
          .getDiscountFactors();
      PointSensitivities sensiSettle = 
          paymentPricer.presentValueSensitivity(bill.getSettlement().get(), discountFactorsRepo).build();
      return sensiProduct.combinedWith(sensiSettle);
    }
    return sensiProduct;
  }

  // TODO with z: pv, sensi

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
    if (bill.getSettlement().isPresent() || bill.getSettlement().get().getDate().equals(valuationDate)) {
      return bill.getSettlement().get().getValue();
    }
    return CurrencyAmount.zero(bill.getProduct().getCurrency());
  }

}
