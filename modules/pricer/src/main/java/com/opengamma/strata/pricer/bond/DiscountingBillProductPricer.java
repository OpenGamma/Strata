/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import static com.opengamma.strata.product.bond.BillYieldConvention.DISCOUNT;
import static com.opengamma.strata.product.bond.BillYieldConvention.FRANCE_CD;
import static com.opengamma.strata.product.bond.BillYieldConvention.INTEREST_AT_MATURITY;
import static com.opengamma.strata.product.bond.BillYieldConvention.JAPAN_BILL;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.product.bond.BillYieldConvention;
import com.opengamma.strata.product.bond.ResolvedBill;

/**
 * Pricer for bill products.
 * <p>
 * This function provides the ability to price a {@link ResolvedBill}.
 * 
 * <h4>Price</h4>
 * Strata uses <i>decimal prices</i> for bills in the trade model, pricers and market data.
 * For example, a price of 99.32% is represented in Strata by 0.9932.
 */
public class DiscountingBillProductPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingBillProductPricer DEFAULT = new DiscountingBillProductPricer();

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the bill product.
   * <p>
   * The present value of the product is the value on the valuation date.
   * The result is expressed using the payment currency of the bill.
   * <p>
   * Coupon payments of the product are considered based on the valuation date.
   * 
   * @param bond  the product
   * @param provider  the discounting provider
   * @return the present value of the fixed coupon bond product
   */
  public CurrencyAmount presentValue(ResolvedBill bill, LegalEntityDiscountingProvider provider) {
    IssuerCurveDiscountFactors discountFactors = provider.issuerCurveDiscountFactors(
        bill.getLegalEntityId(), bill.getCurrency());
    double dfMaturity = discountFactors.discountFactor(bill.getMaturityDate());
    return CurrencyAmount.of(bill.getCurrency(), dfMaturity * bill.getNotional());
  }
  
  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the bill product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param bill  the product
   * @param provider  the discounting provider
   * @return the present value curve sensitivity of the product
   */
  public PointSensitivities presentValueSensitivity(ResolvedBill bill, LegalEntityDiscountingProvider provider) {
    IssuerCurveDiscountFactors discountFactors = provider.issuerCurveDiscountFactors(
        bill.getLegalEntityId(), bill.getCurrency());
    double dfEndBar = bill.getNotional();
    PointSensitivityBuilder sensMaturity = discountFactors.zeroRatePointSensitivity(bill.getMaturityDate())
        .multipliedBy(dfEndBar);
    return sensMaturity.build();
  }
  
  /**
   * Calculates the settlement amount from the yield at a given settlement date.
   * 
   * @param bill  the bill
   * @param yield  the yield
   * @param settlementDate  the settlement date
   * @return the amount
   */
  public CurrencyAmount settlementAmountFromYield(ResolvedBill bill, double yield, LocalDate settlementDate) {
    BillYieldConvention yieldConv = bill.getYieldConvention();
    double accrualFactor = bill.getDayCount().relativeYearFraction(settlementDate, bill.getMaturityDate());
    if (yieldConv.equals(DISCOUNT)) {
      double amount = bill.getNotional() * (1.0d - accrualFactor * yield);
      return CurrencyAmount.of(bill.getCurrency(), amount);
    }
    if (yieldConv.equals(INTEREST_AT_MATURITY) || yieldConv.equals(FRANCE_CD) || yieldConv.equals(JAPAN_BILL)) {
      double amount = bill.getNotional() / (1.0d + accrualFactor * yield);
      return CurrencyAmount.of(bill.getCurrency(), amount);
    }
    throw new UnsupportedOperationException("The convention " + yieldConv.name() + " is not supported.");
  }

  /**
   * Calculates the yield from the amount paid at a given date.
   * 
   * @param bill  the bill product
   * @param amount  the amount
   * @param amountDate  the date at which the amount is paid
   * @return the yield
   */
  public double yieldFromSettlementAmount(ResolvedBill bill, Payment amount) {
    ArgChecker.isTrue(bill.getCurrency().equals(amount.getCurrency()), 
        "payment should be in the currency of the bill");
    BillYieldConvention yieldConv = bill.getYieldConvention();
    double accrualFactor = bill.getDayCount().relativeYearFraction(amount.getDate(), bill.getMaturityDate());
    if (yieldConv.equals(DISCOUNT)) {
      return (bill.getNotional() - amount.getAmount()) / accrualFactor;
    }
    if (yieldConv.equals(INTEREST_AT_MATURITY) || yieldConv.equals(FRANCE_CD) || yieldConv.equals(JAPAN_BILL)) {
      return (bill.getNotional() / amount.getAmount() - 1) / accrualFactor;
    }
    throw new UnsupportedOperationException("The convention " + yieldConv.name() + " is not supported.");
  }
  
  /**
   * Calculates the yield for settlement at a given settlement date from valuation date using curves.
   * 
   * @param bill  the bill
   * @param provider  the discounting provider
   * @param settlementDate  the settlement date
   * @return the yield
   */
  public double yieldFromCurves(ResolvedBill bill, LegalEntityDiscountingProvider provider, LocalDate settlementDate) {
    IssuerCurveDiscountFactors discountFactors = provider.issuerCurveDiscountFactors(
        bill.getLegalEntityId(), bill.getCurrency());
    double dfMaturity = discountFactors.discountFactor(bill.getMaturityDate());
    RepoCurveDiscountFactors discountFactorsRepo =
        provider.repoCurveDiscountFactors(bill.getSecurityId(), bill.getLegalEntityId(), bill.getCurrency());
    double dfRepoSettle = discountFactorsRepo.discountFactor(settlementDate);
    double settleAmount = bill.getNotional() * dfMaturity / dfRepoSettle;
    return yieldFromSettlementAmount(bill, Payment.of(bill.getCurrency(), settleAmount, settlementDate));
  }
  
//  public settlementAmountFromCurves

  // TODO: price?
  
  // TODO: present value from yield

}
