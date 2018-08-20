/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.CompoundedRateType;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.product.bond.ResolvedBill;

/**
 * Pricer for bill products.
 * <p>
 * This function provides the ability to price a {@link ResolvedBill}.
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
   * @param bill  the product
   * @param provider  the discounting provider
   * @return the present value of the bill product
   */
  public CurrencyAmount presentValue(ResolvedBill bill, LegalEntityDiscountingProvider provider) {
    if (provider.getValuationDate().isAfter(bill.getNotional().getDate())) {
      return CurrencyAmount.of(bill.getCurrency(), 0.0d);
    }
    IssuerCurveDiscountFactors issuerDf = issuerCurveDf(bill, provider);
    double dfMaturity = issuerDf.discountFactor(bill.getNotional().getDate());
    return bill.getNotional().getValue().multipliedBy(dfMaturity);
  }

  /**
   * Calculates the present value of a bill product with z-spread.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or
   * periodic compounded rates of the issuer discounting curve.
   * 
   * @param bill  the product
   * @param provider  the discounting provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the present value of the bill product
   */
  public CurrencyAmount presentValueWithZSpread(
      ResolvedBill bill,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    if (provider.getValuationDate().isAfter(bill.getNotional().getDate())) {
      return CurrencyAmount.of(bill.getCurrency(), 0.0d);
    }
    IssuerCurveDiscountFactors issuerDf = issuerCurveDf(bill, provider);
    double dfMaturity = issuerDf.getDiscountFactors()
        .discountFactorWithSpread(bill.getNotional().getDate(), zSpread, compoundedRateType, periodsPerYear);
    return bill.getNotional().getValue().multipliedBy(dfMaturity);
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
    if (provider.getValuationDate().isAfter(bill.getNotional().getDate())) {
      return PointSensitivities.empty();
    }
    IssuerCurveDiscountFactors issuerDf = issuerCurveDf(bill, provider);
    double dfEndBar = bill.getNotional().getAmount();
    PointSensitivityBuilder sensMaturity = issuerDf.zeroRatePointSensitivity(bill.getNotional().getDate())
        .multipliedBy(dfEndBar);
    return sensMaturity.build();
  }

  /**
   * Calculates the present value sensitivity of the bill product with z-spread.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or
   * periodic compounded rates of the issuer discounting curve.
   * 
   * @param bill  the product
   * @param provider  the discounting provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the present value curve sensitivity of the product
   */
  public PointSensitivities presentValueSensitivityWithZSpread(
      ResolvedBill bill,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    if (provider.getValuationDate().isAfter(bill.getNotional().getDate())) {
      return PointSensitivities.empty();
    }
    IssuerCurveDiscountFactors issuerDf = issuerCurveDf(bill, provider);
    double dfEndBar = bill.getNotional().getAmount();
    ZeroRateSensitivity zeroSensMaturity = issuerDf.getDiscountFactors()
        .zeroRatePointSensitivityWithSpread(bill.getNotional().getDate(), zSpread, compoundedRateType, periodsPerYear);
    IssuerCurveZeroRateSensitivity dscSensMaturity =
        IssuerCurveZeroRateSensitivity.of(zeroSensMaturity, issuerDf.getLegalEntityGroup())
            .multipliedBy(dfEndBar);

    return dscSensMaturity.build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price for settlement at a given settlement date using curves.
   * 
   * @param bill  the bill
   * @param provider  the discounting provider
   * @param settlementDate  the settlement date
   * @return the price
   */
  public double priceFromCurves(ResolvedBill bill, LegalEntityDiscountingProvider provider, LocalDate settlementDate) {
    ArgChecker.inOrderNotEqual(settlementDate, bill.getNotional().getDate(), "settlementDate", "endDate");
    ArgChecker.inOrderOrEqual(provider.getValuationDate(), settlementDate, "valuationDate", "settlementDate");
    IssuerCurveDiscountFactors issuerDf = issuerCurveDf(bill, provider);
    double dfMaturity = issuerDf.discountFactor(bill.getNotional().getDate());
    RepoCurveDiscountFactors repoDf = repoCurveDf(bill, provider);
    double dfRepoSettle = repoDf.discountFactor(settlementDate);
    return dfMaturity / dfRepoSettle;
  }

  /**
   * Calculates the price for settlement at a given settlement date using curves with z-spread.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or
   * periodic compounded rates of the issuer discounting curve.
   * <p>
   * The z-spread is applied only on the legal entity curve, not on the repo curve.
   * 
   * @param bill  the bill
   * @param provider  the discounting provider
   * @param settlementDate  the settlement date
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the price
   */
  public double priceFromCurvesWithZSpread(
      ResolvedBill bill,
      LegalEntityDiscountingProvider provider,
      LocalDate settlementDate,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    ArgChecker.inOrderNotEqual(settlementDate, bill.getNotional().getDate(), "settlementDate", "endDate");
    ArgChecker.inOrderOrEqual(provider.getValuationDate(), settlementDate, "valuationDate", "settlementDate");
    IssuerCurveDiscountFactors issuerDf = issuerCurveDf(bill, provider);
    double dfMaturity = issuerDf.getDiscountFactors()
        .discountFactorWithSpread(bill.getNotional().getDate(), zSpread, compoundedRateType, periodsPerYear);
    RepoCurveDiscountFactors repoDf = repoCurveDf(bill, provider);
    double dfRepoSettle = repoDf.discountFactor(settlementDate);
    return dfMaturity / dfRepoSettle;
  }

  /**
   * Calculates the yield for settlement at a given settlement date using curves.
   * 
   * @param bill  the bill
   * @param provider  the discounting provider
   * @param settlementDate  the settlement date
   * @return the yield
   */
  public double yieldFromCurves(ResolvedBill bill, LegalEntityDiscountingProvider provider, LocalDate settlementDate) {
    double price = priceFromCurves(bill, provider, settlementDate);
    return bill.yieldFromPrice(price, settlementDate);
  }

  /**
   * Calculates the yield for settlement at a given settlement date using curves with z-spread.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or
   * periodic compounded rates of the issuer discounting curve.
   * <p>
   * The z-spread is applied only on the legal entity curve, not on the repo curve.
   * 
   * @param bill  the bill
   * @param provider  the discounting provider
   * @param settlementDate  the settlement date
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the yield
   */
  public double yieldFromCurvesWithZSpread(
      ResolvedBill bill,
      LegalEntityDiscountingProvider provider,
      LocalDate settlementDate,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    double price =
        priceFromCurvesWithZSpread(bill, provider, settlementDate, zSpread, compoundedRateType, periodsPerYear);
    return bill.yieldFromPrice(price, settlementDate);
  }

  //-------------------------------------------------------------------------
  // extracts the repo curve discount factors for the bond
  static RepoCurveDiscountFactors repoCurveDf(ResolvedBill bill, LegalEntityDiscountingProvider provider) {
    return provider.repoCurveDiscountFactors(bill.getSecurityId(), bill.getLegalEntityId(), bill.getCurrency());
  }

  // extracts the issuer curve discount factors for the bond
  static IssuerCurveDiscountFactors issuerCurveDf(ResolvedBill bill, LegalEntityDiscountingProvider provider) {
    return provider.issuerCurveDiscountFactors(bill.getLegalEntityId(), bill.getCurrency());
  }

}
