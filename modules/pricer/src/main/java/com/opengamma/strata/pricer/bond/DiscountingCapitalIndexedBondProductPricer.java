/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MONTHS;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.function.Function;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.math.impl.rootfinding.BracketRoot;
import com.opengamma.strata.math.impl.rootfinding.BrentSingleRootFinder;
import com.opengamma.strata.math.impl.rootfinding.RealSingleRootFinder;
import com.opengamma.strata.pricer.CompoundedRateType;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.bond.CapitalIndexedBondPaymentPeriod;
import com.opengamma.strata.product.bond.CapitalIndexedBondYieldConvention;
import com.opengamma.strata.product.bond.ResolvedCapitalIndexedBond;
import com.opengamma.strata.product.rate.InflationEndInterpolatedRateComputation;
import com.opengamma.strata.product.rate.InflationEndMonthRateComputation;
import com.opengamma.strata.product.rate.RateComputation;

/**
 * Pricer for capital indexed bond products.
 * <p>
 * This function provides the ability to price a {@link ResolvedCapitalIndexedBond}.
 * 
 * <h4>Price</h4>
 * Strata uses <i>decimal prices</i> for bonds in the trade model, pricers and market data.
 * For example, a price of 99.32% is represented in Strata by 0.9932.
 */
public class DiscountingCapitalIndexedBondProductPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingCapitalIndexedBondProductPricer DEFAULT =
      new DiscountingCapitalIndexedBondProductPricer(DiscountingCapitalIndexedBondPaymentPeriodPricer.DEFAULT);
  /**
   * The root finder.
   */
  private static final RealSingleRootFinder ROOT_FINDER = new BrentSingleRootFinder();
  /**
   * Brackets a root.
   */
  private static final BracketRoot ROOT_BRACKETER = new BracketRoot();
  /**
   * Small parameter used in finite difference approximation.
   */
  private static final double FD_EPS = 1.0e-5;
  /**
   * Pricer for {@link CapitalIndexedBondPaymentPeriod}.
   */
  private final DiscountingCapitalIndexedBondPaymentPeriodPricer periodPricer;

  /**
   * Creates an instance.
   * 
   * @param periodPricer  the pricer for {@link CapitalIndexedBondPaymentPeriod}.
   */
  public DiscountingCapitalIndexedBondProductPricer(DiscountingCapitalIndexedBondPaymentPeriodPricer periodPricer) {
    this.periodPricer = ArgChecker.notNull(periodPricer, "periodPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the period pricer.
   * 
   * @return the period pricer
   */
  public DiscountingCapitalIndexedBondPaymentPeriodPricer getPeriodPricer() {
    return periodPricer;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the bond.
   * <p>
   * The present value of the product is the value on the valuation date.
   * The result is expressed using the payment currency of the bond.
   * <p>
   * Coupon payments of the product are considered based on the valuation date.
   * 
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param discountingProvider  the discount factors provider
   * @return the present value of the product
   */
  public CurrencyAmount presentValue(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider) {

    validate(ratesProvider, discountingProvider);
    return presentValue(bond, ratesProvider, discountingProvider, ratesProvider.getValuationDate());
  }

  // calculate the present value
  CurrencyAmount presentValue(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      LocalDate referenceDate) {

    IssuerCurveDiscountFactors issuerDf = issuerCurveDf(bond, discountingProvider);
    double pvNominal = periodPricer.presentValue(bond.getNominalPayment(), ratesProvider, issuerDf);
    double pvCoupon = 0d;
    for (CapitalIndexedBondPaymentPeriod period : bond.getPeriodicPayments()) {
      if ((bond.hasExCouponPeriod() && period.getDetachmentDate().isAfter(referenceDate)) ||
          (!bond.hasExCouponPeriod() && period.getPaymentDate().isAfter(referenceDate))) {
        pvCoupon += periodPricer.presentValue(period, ratesProvider, issuerDf);
      }
    }
    return CurrencyAmount.of(bond.getCurrency(), pvCoupon + pvNominal);
  }

  /**
   * Calculates the present value of the bond product with z-spread.
   * <p>
   * The present value of the product is the value on the valuation date.
   * The result is expressed using the payment currency of the bond.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or
   * periodic compounded rates of the issuer discounting curve.
   * 
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param discountingProvider  the discount factors provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the present value of the bond product
   */
  public CurrencyAmount presentValueWithZSpread(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    validate(ratesProvider, discountingProvider);
    return presentValueWithZSpread(bond, ratesProvider, discountingProvider,
        ratesProvider.getValuationDate(), zSpread, compoundedRateType, periodsPerYear);
  }

  // calculate the present value
  CurrencyAmount presentValueWithZSpread(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      LocalDate referenceDate,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    IssuerCurveDiscountFactors issuerDf = issuerCurveDf(bond, discountingProvider);
    double pvNominal = periodPricer.presentValueWithZSpread(
        bond.getNominalPayment(), ratesProvider, issuerDf, zSpread, compoundedRateType, periodsPerYear);
    double pvCoupon = 0d;
    for (CapitalIndexedBondPaymentPeriod period : bond.getPeriodicPayments()) {
      if ((bond.hasExCouponPeriod() && period.getDetachmentDate().isAfter(referenceDate)) ||
          (!bond.hasExCouponPeriod() && period.getPaymentDate().isAfter(referenceDate))) {
        pvCoupon += periodPricer.presentValueWithZSpread(
            period, ratesProvider, issuerDf, zSpread, compoundedRateType, periodsPerYear);
      }
    }
    return CurrencyAmount.of(bond.getCurrency(), pvCoupon + pvNominal);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the bond product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param discountingProvider  the discount factors provider
   * @return the present value curve sensitivity of the product
   */
  public PointSensitivityBuilder presentValueSensitivity(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider) {

    validate(ratesProvider, discountingProvider);
    return presentValueSensitivity(
        bond, ratesProvider, discountingProvider, ratesProvider.getValuationDate());
  }

  /**
   * Calculates the present value sensitivity of the bond product for the specified reference date.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   *
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param discountingProvider  the discount factors provider
   * @param referenceDate  the reference date
   * @return the present value curve sensitivity of the product
   */
  public PointSensitivityBuilder presentValueSensitivity(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      LocalDate referenceDate) {

    IssuerCurveDiscountFactors issuerDf = issuerCurveDf(bond, discountingProvider);
    PointSensitivityBuilder pointNominal =
        periodPricer.presentValueSensitivity(bond.getNominalPayment(), ratesProvider, issuerDf);
    PointSensitivityBuilder pointCoupon = PointSensitivityBuilder.none();
    for (CapitalIndexedBondPaymentPeriod period : bond.getPeriodicPayments()) {
      if ((bond.hasExCouponPeriod() && period.getDetachmentDate().isAfter(referenceDate)) ||
          (!bond.hasExCouponPeriod() && period.getPaymentDate().isAfter(referenceDate))) {
        pointCoupon = pointCoupon.combinedWith(
            periodPricer.presentValueSensitivity(period, ratesProvider, issuerDf));
      }
    }
    return pointNominal.combinedWith(pointCoupon);
  }

  /**
   * Calculates the present value sensitivity of the bond product with z-spread.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or
   * periodic compounded rates of the issuer discounting curve.
   * 
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param discountingProvider  the discount factors provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the present value curve sensitivity of the product
   */
  public PointSensitivityBuilder presentValueSensitivityWithZSpread(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    validate(ratesProvider, discountingProvider);
    return presentValueSensitivityWithZSpread(bond, ratesProvider, discountingProvider,
        ratesProvider.getValuationDate(), zSpread, compoundedRateType, periodsPerYear);
  }

  // calculate the present value sensitivity
  PointSensitivityBuilder presentValueSensitivityWithZSpread(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      LocalDate referenceDate,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    IssuerCurveDiscountFactors issuerDf = issuerCurveDf(bond, discountingProvider);
    PointSensitivityBuilder pointNominal = periodPricer.presentValueSensitivityWithZSpread(
        bond.getNominalPayment(), ratesProvider, issuerDf, zSpread, compoundedRateType, periodsPerYear);
    PointSensitivityBuilder pointCoupon = PointSensitivityBuilder.none();
    for (CapitalIndexedBondPaymentPeriod period : bond.getPeriodicPayments()) {
      if ((bond.hasExCouponPeriod() && period.getDetachmentDate().isAfter(referenceDate)) ||
          (!bond.hasExCouponPeriod() && period.getPaymentDate().isAfter(referenceDate))) {
        pointCoupon = pointCoupon.combinedWith(periodPricer.presentValueSensitivityWithZSpread(
            period, ratesProvider, issuerDf, zSpread, compoundedRateType, periodsPerYear));
      }
    }
    return pointNominal.combinedWith(pointCoupon);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the bond product.
   * 
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param discountingProvider  the discount factors provider
   * @param referenceDate  the reference date
   * @return the currency exposure of the product 
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      LocalDate referenceDate) {

    return MultiCurrencyAmount.of(presentValue(bond, ratesProvider, discountingProvider, referenceDate));
  }

  /**
   * Calculates the currency exposure of the bond product with z-spread.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or
   * periodic compounded rates of the issuer discounting curve.
   * 
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param discountingProvider  the discount factors provider
   * @param referenceDate  the reference date
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the currency exposure of the product 
   */
  public MultiCurrencyAmount currencyExposureWithZSpread(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      LocalDate referenceDate,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    return MultiCurrencyAmount.of(presentValueWithZSpread(bond, ratesProvider, discountingProvider,
        referenceDate, zSpread, compoundedRateType, periodsPerYear));
  }

  /**
   * Calculates the current cash of the bond product.
   * 
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @return the current cash of the product 
   */
  public CurrencyAmount currentCash(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LocalDate settlementDate) {

    LocalDate valuationDate = ratesProvider.getValuationDate();
    Currency currency = bond.getCurrency();
    CurrencyAmount currentCash = CurrencyAmount.zero(currency);
    if (settlementDate.isBefore(valuationDate)) {
      double cashCoupon = bond.hasExCouponPeriod() ?
          0d :
          currentCashPayment(bond, ratesProvider, valuationDate);
      CapitalIndexedBondPaymentPeriod nominal = bond.getNominalPayment();
      double cashNominal = nominal.getPaymentDate().isEqual(valuationDate) ?
          periodPricer.forecastValue(nominal, ratesProvider) :
          0d;
      currentCash = currentCash.plus(CurrencyAmount.of(currency, cashCoupon + cashNominal));
    }
    return currentCash;
  }

  private double currentCashPayment(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LocalDate valuationDate) {

    double cash = 0d;
    for (CapitalIndexedBondPaymentPeriod period : bond.getPeriodicPayments()) {
      if (period.getPaymentDate().isEqual(valuationDate)) {
        cash += periodPricer.forecastValue(period, ratesProvider);
      }
    }
    return cash;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the dirty price of the bond security.
   * <p>
   * The bond is represented as {@link Security} where standard ID of the bond is stored.
   * <p>
   * Strata uses <i>decimal prices</i> for bonds. For example, a price of 99.32% is represented in Strata by 0.9932.
   * 
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param discountingProvider  the discount factors provider
   * @param refData  the reference data used to calculate the settlement date
   * @return the dirty price of the bond security
   */
  public double dirtyNominalPriceFromCurves(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      ReferenceData refData) {

    validate(ratesProvider, discountingProvider);
    LocalDate settlementDate = bond.calculateSettlementDateFromValuation(ratesProvider.getValuationDate(), refData);
    return dirtyNominalPriceFromCurves(bond, ratesProvider, discountingProvider, settlementDate);
  }

  /**
   * Calculates the dirty price of the bond security for the specified settlement date.
   * <p>
   * The bond is represented as {@link Security} where standard ID of the bond is stored.
   * <p>
   * Strata uses <i>decimal prices</i> for bonds. For example, a price of 99.32% is represented in Strata by 0.9932.
   *
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param discountingProvider  the discount factors provider
   * @param settlementDate  the settlement date
   * @return the dirty price of the bond security
   */
  public double dirtyNominalPriceFromCurves(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      LocalDate settlementDate) {

    CurrencyAmount pv = presentValue(bond, ratesProvider, discountingProvider, settlementDate);
    RepoCurveDiscountFactors repoDf = repoCurveDf(bond, discountingProvider);
    double df = repoDf.discountFactor(settlementDate);
    double notional = bond.getNotional();
    return pv.getAmount() / (df * notional);
  }

  /**
   * Calculates the dirty price of the bond security with z-spread.
   * <p>
   * The bond is represented as {@link Security} where standard ID of the bond is stored.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic
   * compounded rates of the discounting curve.
   * 
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param discountingProvider  the discount factors provider
   * @param refData  the reference data used to calculate the settlement date
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the dirty price of the bond security
   */
  public double dirtyNominalPriceFromCurvesWithZSpread(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      ReferenceData refData,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    validate(ratesProvider, discountingProvider);
    LocalDate settlementDate = bond.calculateSettlementDateFromValuation(ratesProvider.getValuationDate(), refData);
    return dirtyNominalPriceFromCurvesWithZSpread(
        bond, ratesProvider, discountingProvider, settlementDate, zSpread, compoundedRateType, periodsPerYear);
  }

  // calculate the dirty price
  double dirtyNominalPriceFromCurvesWithZSpread(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      LocalDate settlementDate,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    CurrencyAmount pv = presentValueWithZSpread(
        bond, ratesProvider, discountingProvider, settlementDate, zSpread, compoundedRateType, periodsPerYear);
    RepoCurveDiscountFactors repoDf = repoCurveDf(bond, discountingProvider);
    double df = repoDf.discountFactor(settlementDate);
    double notional = bond.getNotional();
    return pv.getAmount() / (df * notional);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the dirty price sensitivity of the bond security.
   * <p>
   * The dirty price sensitivity of the security is the sensitivity of the dirty price value to
   * the underlying curves.
   * 
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param discountingProvider  the discount factors provider
   * @param refData  the reference data used to calculate the settlement date
   * @return the dirty price curve sensitivity of the security
   */
  public PointSensitivityBuilder dirtyNominalPriceSensitivity(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      ReferenceData refData) {

    validate(ratesProvider, discountingProvider);
    LocalDate settlementDate = bond.calculateSettlementDateFromValuation(ratesProvider.getValuationDate(), refData);
    return dirtyNominalPriceSensitivity(bond, ratesProvider, discountingProvider, settlementDate);
  }

  /**
   * Calculates the dirty price sensitivity of the bond security for the specified settlement date.
   * <p>
   * The dirty price sensitivity of the security is the sensitivity of the dirty price value to
   * the underlying curves.
   *
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param discountingProvider  the discount factors provider
   * @param settlementDate  the settlement date
   * @return the dirty price curve sensitivity of the security
   */
  public PointSensitivityBuilder dirtyNominalPriceSensitivity(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      LocalDate settlementDate) {

    double notional = bond.getNotional();
    CurrencyAmount pv = presentValue(bond, ratesProvider, discountingProvider, settlementDate);
    RepoCurveDiscountFactors repoDf = repoCurveDf(bond, discountingProvider);
    double df = repoDf.discountFactor(settlementDate);
    PointSensitivityBuilder pvSensi = presentValueSensitivity(
        bond, ratesProvider, discountingProvider, settlementDate).multipliedBy(1d / (df * notional));
    RepoCurveZeroRateSensitivity dfSensi =
        repoDf.zeroRatePointSensitivity(settlementDate).multipliedBy(-pv.getAmount() / (df * df * notional));
    return pvSensi.combinedWith(dfSensi);
  }

  /**
   * Calculates the dirty price sensitivity of the bond security with z-spread.
   * <p>
   * The dirty price sensitivity of the security is the sensitivity of the dirty price value to
   * the underlying curves.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic
   * compounded rates of the discounting curve.
   * 
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param discountingProvider  the discount factors provider
   * @param refData  the reference data used to calculate the settlement date
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the dirty price curve sensitivity of the security
   */
  public PointSensitivityBuilder dirtyNominalPriceSensitivityWithZSpread(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      ReferenceData refData,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    validate(ratesProvider, discountingProvider);
    LocalDate settlementDate = bond.calculateSettlementDateFromValuation(ratesProvider.getValuationDate(), refData);
    return dirtyNominalPriceSensitivityWithZSpread(
        bond,
        ratesProvider,
        discountingProvider,
        settlementDate,
        zSpread,
        compoundedRateType,
        periodsPerYear);
  }

  // calculate the dirty price sensitivity
  PointSensitivityBuilder dirtyNominalPriceSensitivityWithZSpread(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      LocalDate settlementDate,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    double notional = bond.getNotional();
    CurrencyAmount pv = presentValueWithZSpread(
        bond, ratesProvider, discountingProvider, settlementDate, zSpread, compoundedRateType,
        periodsPerYear);
    RepoCurveDiscountFactors repoDf = repoCurveDf(bond, discountingProvider);
    double df = repoDf.discountFactor(settlementDate);
    PointSensitivityBuilder pvSensi = presentValueSensitivityWithZSpread(bond, ratesProvider, discountingProvider,
        settlementDate, zSpread, compoundedRateType, periodsPerYear).multipliedBy(1d / (df * notional));
    RepoCurveZeroRateSensitivity dfSensi =
        repoDf.zeroRatePointSensitivity(settlementDate).multipliedBy(-pv.getAmount() / df / df / notional);
    return pvSensi.combinedWith(dfSensi);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the dirty price from the conventional real yield.
   * <p>
   * The resulting dirty price is real price or nominal price depending on the yield convention.
   * <p>
   * The input yield and output are expressed in fraction.
   * 
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @param yield  the yield
   * @return the dirty price of the product 
   */
  public double dirtyPriceFromRealYield(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LocalDate settlementDate,
      double yield) {

    ArgChecker.isTrue(settlementDate.isBefore(bond.getUnadjustedEndDate()),
        "settlement date must be before end date");
    int periodIndex = bond.findPeriodIndex(settlementDate)
        .orElseThrow(() -> new IllegalArgumentException("Date outside range of bond"));
    CapitalIndexedBondPaymentPeriod period = bond.getPeriodicPayments().get(periodIndex);
    int nbCoupon = bond.getPeriodicPayments().size() - periodIndex;
    double couponPerYear = bond.getFrequency().eventsPerYear();
    CapitalIndexedBondYieldConvention yieldConvention = bond.getYieldConvention();
    if (yieldConvention.equals(CapitalIndexedBondYieldConvention.US_IL_REAL)) {
      double pvAtFirstCoupon;
      double cpnRate = bond.getPeriodicPayments().get(0).getRealCoupon();
      if (Math.abs(yield) > 1.0E-8) {
        double factorOnPeriod = 1d + yield / couponPerYear;
        double vn = Math.pow(factorOnPeriod, 1 - nbCoupon);
        pvAtFirstCoupon = cpnRate * couponPerYear / yield * (factorOnPeriod - vn) + vn;
      } else {
        pvAtFirstCoupon = cpnRate * nbCoupon + 1d;
      }
      return pvAtFirstCoupon / (1d + factorToNextCoupon(bond, settlementDate) * yield / couponPerYear);
    }

    double realRate = period.getRealCoupon();
    double firstYearFraction = bond.yearFraction(period.getUnadjustedStartDate(), period.getUnadjustedEndDate());
    double v = 1d / (1d + yield / couponPerYear);
    double rs = ratioPeriodToNextCoupon(period, settlementDate);
    if (yieldConvention.equals(CapitalIndexedBondYieldConvention.GB_IL_FLOAT)) {
      RateComputation obs = period.getRateComputation();
      LocalDateDoubleTimeSeries ts = ratesProvider.priceIndexValues(bond.getRateCalculation().getIndex()).getFixings();
      YearMonth lastKnownFixingMonth = YearMonth.from(ts.getLatestDate());
      double indexRatio = ts.getLatestValue() / bond.getFirstIndexValue();
      YearMonth endFixingMonth = null;
      if (obs instanceof InflationEndInterpolatedRateComputation) {
        endFixingMonth = ((InflationEndInterpolatedRateComputation) obs).getEndSecondObservation().getFixingMonth();
      } else if (obs instanceof InflationEndMonthRateComputation) {
        endFixingMonth = ((InflationEndMonthRateComputation) obs).getEndObservation().getFixingMonth();
      } else {
        throw new IllegalArgumentException("The rate observation " + obs.toString() + " is not supported.");
      }
      double nbMonth = Math.abs(MONTHS.between(endFixingMonth, lastKnownFixingMonth));
      double u = Math.sqrt(1d / 1.03);
      double a = indexRatio * Math.pow(u, nbMonth / 6d);
      if (nbCoupon == 1) {
        return (realRate + 1d) * a / u * Math.pow(u * v, rs);
      } else {
        double firstCashFlow = firstYearFraction * realRate * indexRatio * couponPerYear;
        CapitalIndexedBondPaymentPeriod secondPeriod = bond.getPeriodicPayments().get(periodIndex + 1);
        double secondYearFraction = bond.yearFraction(secondPeriod.getUnadjustedStartDate(), secondPeriod.getUnadjustedEndDate());
        double secondCashFlow = secondYearFraction * realRate * indexRatio * couponPerYear;
        double vn = Math.pow(v, nbCoupon - 1);
        double pvAtFirstCoupon =
            firstCashFlow + secondCashFlow * u * v + a * realRate * v * v * (1d - vn / v) / (1d - v) + a * vn;
        return pvAtFirstCoupon * Math.pow(u * v, rs);
      }
    }
    if (yieldConvention.equals(CapitalIndexedBondYieldConvention.GB_IL_BOND)) {
      double indexRatio = indexRatio(bond, ratesProvider, settlementDate);
      double firstCashFlow = realRate * indexRatio * firstYearFraction * couponPerYear;
      if (nbCoupon == 1) {
        return Math.pow(v, rs) * (firstCashFlow + 1d);
      } else {
        CapitalIndexedBondPaymentPeriod secondPeriod = bond.getPeriodicPayments().get(periodIndex + 1);
        double secondYearFraction = bond.yearFraction(secondPeriod.getUnadjustedStartDate(), secondPeriod.getUnadjustedEndDate());
        double secondCashFlow = realRate * indexRatio * secondYearFraction * couponPerYear;
        double vn = Math.pow(v, nbCoupon - 1);
        double pvAtFirstCoupon = firstCashFlow + secondCashFlow * v + realRate * v * v * (1d - vn / v) / (1d - v) + vn;
        return pvAtFirstCoupon * Math.pow(v, rs);
      }
    }
    if (yieldConvention.equals(CapitalIndexedBondYieldConvention.JP_IL_SIMPLE)) {
      LocalDate maturityDate = bond.getEndDate();
      double maturity = bond.yearFraction(settlementDate, maturityDate);
      double cleanPrice = (1d + realRate * couponPerYear * maturity) / (1d + yield * maturity);
      return dirtyRealPriceFromCleanRealPrice(bond, settlementDate, cleanPrice);
    }
    if (yieldConvention.equals(CapitalIndexedBondYieldConvention.JP_IL_COMPOUND)) {
      double pvAtFirstCoupon = 0d;
      for (int loopcpn = 0; loopcpn < nbCoupon; loopcpn++) {
        CapitalIndexedBondPaymentPeriod paymentPeriod = bond.getPeriodicPayments().get(loopcpn + periodIndex);
        pvAtFirstCoupon += paymentPeriod.getRealCoupon() * Math.pow(v, loopcpn);
      }
      pvAtFirstCoupon += Math.pow(v, nbCoupon - 1);
      double factorToNext = factorToNextCoupon(bond, settlementDate);
      return pvAtFirstCoupon * Math.pow(v, factorToNext);
    }
    throw new IllegalArgumentException(
        "The convention " + bond.getYieldConvention().toString() + " is not supported.");
  }

  /**
   * Computes the dirty price from the conventional real yield and its derivative wrt the yield.
   * <p>
   * The resulting dirty price is real price or nominal price depending on the yield convention.
   * <p>
   * The input yield and output are expressed in fraction.
   *
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @param yield  the yield
   * @return the dirty price of the product and its derivative
   */
  public ValueDerivatives dirtyPriceFromRealYieldAd(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LocalDate settlementDate,
      double yield) {

    ArgChecker.isTrue(settlementDate.isBefore(bond.getUnadjustedEndDate()),
        "settlement date must be before end date");
    int periodIndex = bond.findPeriodIndex(settlementDate)
        .orElseThrow(() -> new IllegalArgumentException("Date outside range of bond"));
    CapitalIndexedBondPaymentPeriod period = bond.getPeriodicPayments().get(periodIndex);
    int nbCoupon = bond.getPeriodicPayments().size() - periodIndex;
    double couponPerYear = bond.getFrequency().eventsPerYear();
    CapitalIndexedBondYieldConvention yieldConvention = bond.getYieldConvention();
    if (yieldConvention.equals(CapitalIndexedBondYieldConvention.US_IL_REAL)) {
      double pvAtFirstCoupon;
      double pvAtFirstCouponDeriv;
      double cpnRate = bond.getPeriodicPayments().get(0).getRealCoupon();
      if (Math.abs(yield) > 1.0E-8) {
        double factorOnPeriod = 1d + yield / couponPerYear;
        double factorOnPeriodDeriv =  1d / couponPerYear;
        double vn = Math.pow(factorOnPeriod, 1 - nbCoupon);
        double vnDeriv = (1d - nbCoupon) * Math.pow(factorOnPeriod, -nbCoupon) * factorOnPeriodDeriv;
        pvAtFirstCoupon = cpnRate * couponPerYear / yield * (factorOnPeriod - vn) + vn;
        pvAtFirstCouponDeriv = -cpnRate * couponPerYear / yield / yield * (factorOnPeriod - vn) +
            cpnRate * couponPerYear / yield * (factorOnPeriodDeriv - vnDeriv) + vnDeriv;
      } else {
        pvAtFirstCoupon = cpnRate * nbCoupon + 1d;
        pvAtFirstCouponDeriv = (1d - nbCoupon) / couponPerYear + 0.5 * (1d - nbCoupon) * nbCoupon * cpnRate / couponPerYear;
      }
      double den = 1d + factorToNextCoupon(bond, settlementDate) * yield / couponPerYear;
      double denDeriv = factorToNextCoupon(bond, settlementDate) / couponPerYear;
      double price =  pvAtFirstCoupon / den;
      double priceDeriv =  pvAtFirstCouponDeriv / den - pvAtFirstCoupon / den / den * denDeriv;
      return ValueDerivatives.of(price, DoubleArray.of(priceDeriv));
    }

    double realRate = period.getRealCoupon();
    double firstYearFraction = bond.yearFraction(period.getUnadjustedStartDate(), period.getUnadjustedEndDate());
    double v = 1d / (1d + yield / couponPerYear);
    double vDeriv = -v * v / couponPerYear;
    double rs = ratioPeriodToNextCoupon(period, settlementDate);
    if (yieldConvention.equals(CapitalIndexedBondYieldConvention.GB_IL_FLOAT)) {
      RateComputation obs = period.getRateComputation();
      LocalDateDoubleTimeSeries ts = ratesProvider.priceIndexValues(bond.getRateCalculation().getIndex()).getFixings();
      YearMonth lastKnownFixingMonth = YearMonth.from(ts.getLatestDate());
      double indexRatio = ts.getLatestValue() / bond.getFirstIndexValue();
      YearMonth endFixingMonth = null;
      if (obs instanceof InflationEndInterpolatedRateComputation) {
        endFixingMonth = ((InflationEndInterpolatedRateComputation) obs).getEndSecondObservation().getFixingMonth();
      } else if (obs instanceof InflationEndMonthRateComputation) {
        endFixingMonth = ((InflationEndMonthRateComputation) obs).getEndObservation().getFixingMonth();
      } else {
        throw new IllegalArgumentException("The rate observation " + obs.toString() + " is not supported.");
      }
      double nbMonth = Math.abs(MONTHS.between(endFixingMonth, lastKnownFixingMonth));
      double u = Math.sqrt(1d / 1.03);
      double a = indexRatio * Math.pow(u, nbMonth / 6d);
      if (nbCoupon == 1) {
        double price = (realRate + 1d) * a / u * Math.pow(u * v, rs);
        double priceDeriv = (realRate + 1d) * a / u * Math.pow(u * v, rs - 1d) * rs * u * vDeriv;
        return ValueDerivatives.of(price, DoubleArray.of(priceDeriv));
      } else {
        double firstCashFlow = firstYearFraction * realRate * indexRatio * couponPerYear;
        CapitalIndexedBondPaymentPeriod secondPeriod = bond.getPeriodicPayments().get(periodIndex + 1);
        double secondYearFraction = bond.yearFraction(secondPeriod.getUnadjustedStartDate(), secondPeriod.getUnadjustedEndDate());
        double secondCashFlow = secondYearFraction * realRate * indexRatio * couponPerYear;
        double vn = Math.pow(v, nbCoupon - 1);
        double vnDeriv = (nbCoupon - 1d) * Math.pow(v, nbCoupon - 2) * vDeriv;
        double pvAtFirstCoupon =
            firstCashFlow + secondCashFlow * u * v + a * realRate * v * v * (1d - vn / v) / (1d - v) + a * vn;
        double pvAtFirstCouponDeriv =
             secondCashFlow * u * vDeriv + a * vnDeriv +
                 2d * a * realRate * v * vDeriv * (1d - vn / v) / (1d - v) -
                 a * realRate * v * vnDeriv / (1d - v) +
                 a * realRate * vn * vDeriv / (1d - v) +
                 a * realRate * v * v * (1d - vn / v) / Math.pow(1d - v, 2) * vDeriv;
        double price = pvAtFirstCoupon * Math.pow(u * v, rs);
        double priceDeriv = pvAtFirstCouponDeriv * Math.pow(u * v, rs) +
            rs * u * vDeriv * pvAtFirstCoupon * Math.pow(u * v, rs - 1);
        return ValueDerivatives.of(price, DoubleArray.of(priceDeriv));
      }
    }
    if (yieldConvention.equals(CapitalIndexedBondYieldConvention.GB_IL_BOND)) {
      double indexRatio = indexRatio(bond, ratesProvider, settlementDate);
      double firstCashFlow = realRate * indexRatio * firstYearFraction * couponPerYear;
      if (nbCoupon == 1) {
        double price = Math.pow(v, rs) * (firstCashFlow + 1d);
        double priceDeriv = rs * vDeriv * Math.pow(v, rs - 1d) * (firstCashFlow + 1d);
        return ValueDerivatives.of(price, DoubleArray.of(priceDeriv));
      } else {
        CapitalIndexedBondPaymentPeriod secondPeriod = bond.getPeriodicPayments().get(periodIndex + 1);
        double secondYearFraction = bond.yearFraction(secondPeriod.getUnadjustedStartDate(), secondPeriod.getUnadjustedEndDate());
        double secondCashFlow = realRate * indexRatio * secondYearFraction * couponPerYear;
        double vn = Math.pow(v, nbCoupon - 1);
        double vnDeriv = (nbCoupon - 1d) * vDeriv * Math.pow(v, nbCoupon - 2);
        double pvAtFirstCoupon = firstCashFlow + secondCashFlow * v + realRate * v * v * (1d - vn / v) / (1d - v) + vn;
        double pvAtFirstCouponDeriv = secondCashFlow * vDeriv + vnDeriv +
            2d * realRate * v * vDeriv * (1d - vn / v) / (1d - v) -
            realRate * v * vnDeriv / (1d - v) +
            realRate * vDeriv * vn / (1d - v) +
            realRate * v * v * vDeriv * (1d - vn / v) / Math.pow(1d - v, 2);
        double price = pvAtFirstCoupon * Math.pow(v, rs);
        double priceDeriv = pvAtFirstCouponDeriv * Math.pow(v, rs) + pvAtFirstCoupon * Math.pow(v, rs - 1) * vDeriv * rs;
        return ValueDerivatives.of(price, DoubleArray.of(priceDeriv));
      }
    }
    if (yieldConvention.equals(CapitalIndexedBondYieldConvention.JP_IL_SIMPLE)) {
      LocalDate maturityDate = bond.getEndDate();
      double maturity = bond.yearFraction(settlementDate, maturityDate);
      double cleanPrice = (1d + realRate * couponPerYear * maturity) / (1d + yield * maturity);
      double cleanPriceDeriv = -cleanPrice * maturity / (1d + yield * maturity);
      double price = dirtyRealPriceFromCleanRealPrice(bond, settlementDate, cleanPrice);
      return ValueDerivatives.of(price, DoubleArray.of(cleanPriceDeriv));
    }
    if (yieldConvention.equals(CapitalIndexedBondYieldConvention.JP_IL_COMPOUND)) {
      double pvAtFirstCoupon = 0d;
      double pvAtFirstCouponDeriv = 0d;
      for (int loopcpn = 0; loopcpn < nbCoupon; loopcpn++) {
        CapitalIndexedBondPaymentPeriod paymentPeriod = bond.getPeriodicPayments().get(loopcpn + periodIndex);
        pvAtFirstCoupon += paymentPeriod.getRealCoupon() * Math.pow(v, loopcpn);
        pvAtFirstCouponDeriv += paymentPeriod.getRealCoupon() * loopcpn * Math.pow(v, loopcpn - 1) * vDeriv;
      }
      pvAtFirstCoupon += Math.pow(v, nbCoupon - 1);
      pvAtFirstCouponDeriv += (nbCoupon - 1) * Math.pow(v, nbCoupon - 2) * vDeriv;
      double factorToNext = factorToNextCoupon(bond, settlementDate);
      double price = pvAtFirstCoupon * Math.pow(v, factorToNext);
      double priceDeriv = pvAtFirstCouponDeriv * Math.pow(v, factorToNext) +
          pvAtFirstCoupon * factorToNext * Math.pow(v, factorToNext - 1d) * vDeriv;
      return ValueDerivatives.of(price, DoubleArray.of(priceDeriv));
    }
    throw new IllegalArgumentException(
        "The convention " + bond.getYieldConvention().toString() + " is not supported.");
  }

  /**
   * Computes the clean price from the conventional real yield.
   * <p>
   * The resulting clean price is real price or nominal price depending on the yield convention.
   * <p>
   * The input yield and output are expressed in fraction.
   * <p>
   * Strata uses <i>decimal prices</i> for bonds. For example, a price of 99.32% is represented in Strata by 0.9932.
   * 
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @param yield  the yield
   * @return the clean price of the product 
   */
  public double cleanPriceFromRealYield(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LocalDate settlementDate,
      double yield) {

    double dirtyPrice = dirtyPriceFromRealYield(bond, ratesProvider, settlementDate, yield);
    return cleanRealPriceFromDirtyRealPrice(bond, settlementDate, dirtyPrice);
  }

  /**
   * Computes the conventional real yield from the dirty price.
   * <p>
   * The input dirty price should be real price or nominal price depending on the yield convention. This is coherent to  
   * the implementation of {@link #dirtyPriceFromRealYield(ResolvedCapitalIndexedBond, RatesProvider, LocalDate, double)}.
   * <p>
   * The input price and output are expressed in fraction.
   * 
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @param dirtyPrice  the bond dirty price
   * @return the yield of the product 
   */
  public double realYieldFromDirtyPrice(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LocalDate settlementDate,
      double dirtyPrice) {

    final Function<Double, Double> priceResidual = new Function<Double, Double>() {
      @Override
      public Double apply(Double y) {
        return dirtyPriceFromRealYield(bond, ratesProvider, settlementDate, y) - dirtyPrice;
      }
    };
    double[] range = ROOT_BRACKETER.getBracketedPoints(priceResidual, -0.05, 0.10);
    double yield = ROOT_FINDER.getRoot(priceResidual, range[0], range[1]);
    return yield;
  }

  /**
   * Computes the conventional real yield from the dirty price and its derivaitve wrt the dirty price.
   * <p>
   * The input dirty price should be real price or nominal price depending on the yield convention. This is coherent to
   * the implementation of {@link #dirtyPriceFromRealYield(ResolvedCapitalIndexedBond, RatesProvider, LocalDate, double)}.
   * <p>
   * The input price and output are expressed in fraction.
   *
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @param dirtyPrice  the bond dirty price
   * @return the yield of the product and its derivative
   */
  public ValueDerivatives realYieldFromDirtyPriceAd(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LocalDate settlementDate,
      double dirtyPrice) {

    final Function<Double, Double> priceResidual = new Function<Double, Double>() {
      @Override
      public Double apply(Double y) {
        return dirtyPriceFromRealYield(bond, ratesProvider, settlementDate, y) - dirtyPrice;
      }
    };
    double[] range = ROOT_BRACKETER.getBracketedPoints(priceResidual, -0.05, 0.10);
    double yield = ROOT_FINDER.getRoot(priceResidual, range[0], range[1]);
    ValueDerivatives priceDYield = dirtyPriceFromRealYieldAd(bond, ratesProvider, settlementDate, yield);
    return ValueDerivatives.of(yield, DoubleArray.of(1.0 / priceDYield.getDerivative(0)));
  }

  /**
   * Computes the conventional real yield from the curves.
   * <p>
   * The yield is in the bill yield convention.
   * 
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param discountingProvider  the discount factors provider
   * @param refData  the reference data used to calculate the settlement date
   * @return the yield of the product 
   */
  public double realYieldFromCurves(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      ReferenceData refData) {

    validate(ratesProvider, discountingProvider);
    LocalDate settlementDate = bond.calculateSettlementDateFromValuation(ratesProvider.getValuationDate(), refData);
    double dirtyNominalPrice =
        dirtyNominalPriceFromCurves(bond, ratesProvider, discountingProvider, settlementDate);
    double dirtyRealPrice = realPriceFromNominalPrice(bond, ratesProvider, settlementDate, dirtyNominalPrice);
    return realYieldFromDirtyPrice(bond, ratesProvider, settlementDate, dirtyRealPrice);
  }

  /**
   * Computes the dirty price from the standard yield.
   * <p>
   * The input yield and output are expressed in fraction.
   * 
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @param yield  the standard yield
   * @return the dirty price of the product 
   */
  public double dirtyPriceFromStandardYield(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LocalDate settlementDate,
      double yield) {

    int nbCoupon = bond.getPeriodicPayments().size();
    double couponPerYear = bond.getFrequency().eventsPerYear();
    double factorOnPeriod = 1d + yield / couponPerYear;
    double pvAtFirstCoupon = 0d;
    int pow = 0;
    double factorToNext = factorToNextCoupon(bond, settlementDate);
    for (int loopcpn = 0; loopcpn < nbCoupon; loopcpn++) {
      CapitalIndexedBondPaymentPeriod period = bond.getPeriodicPayments().get(loopcpn);
      if ((bond.hasExCouponPeriod() && !settlementDate.isAfter(period.getDetachmentDate())) ||
          (!bond.hasExCouponPeriod() && period.getPaymentDate().isAfter(settlementDate))) {
        pvAtFirstCoupon += period.getRealCoupon() / Math.pow(factorOnPeriod, pow);
        ++pow;
      }
    }
    pvAtFirstCoupon += 1d / Math.pow(factorOnPeriod, pow - 1);
    return pvAtFirstCoupon * Math.pow(factorOnPeriod, -factorToNext);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the modified duration from the conventional real yield using finite difference approximation.
   * <p>
   * The modified duration is defined as the minus of the first derivative of clean price with respect to yield, 
   * divided by the clean price.
   * <p>
   * The clean price here is real price or nominal price depending on the yield convention. This is coherent to 
   * the implementation of {@link #dirtyPriceFromRealYield(ResolvedCapitalIndexedBond, RatesProvider, LocalDate, double)}.
   * <p>
   * The input yield and output are expressed in fraction.
   * 
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @param yield  the yield
   * @return the modified duration of the product 
   */
  public double modifiedDurationFromRealYieldFiniteDifference(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LocalDate settlementDate,
      double yield) {

    double price = cleanPriceFromRealYield(bond, ratesProvider, settlementDate, yield);
    double priceplus = cleanPriceFromRealYield(bond, ratesProvider, settlementDate, yield + FD_EPS);
    double priceminus = cleanPriceFromRealYield(bond, ratesProvider, settlementDate, yield - FD_EPS);
    return -0.5 * (priceplus - priceminus) / (price * FD_EPS);
  }

  /**
   * Calculates the convexity from the conventional real yield using finite difference approximation.
   * <p>
   * The convexity is defined as the second derivative of clean price with respect to yield, divided by the clean price.
   * <p>
   * The clean price here is real price or nominal price depending on the yield convention. This is coherent to 
   * the implementation of {@link #dirtyPriceFromRealYield(ResolvedCapitalIndexedBond, RatesProvider, LocalDate, double)}.
   * <p>
   * The input yield and output are expressed in fraction.
   * 
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @param yield  the yield
   * @return the covexity of the product 
   */
  public double convexityFromRealYieldFiniteDifference(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LocalDate settlementDate,
      double yield) {

    double price = cleanPriceFromRealYield(bond, ratesProvider, settlementDate, yield);
    double priceplus = cleanPriceFromRealYield(bond, ratesProvider, settlementDate, yield + FD_EPS);
    double priceminus = cleanPriceFromRealYield(bond, ratesProvider, settlementDate, yield - FD_EPS);
    return (priceplus - 2 * price + priceminus) / (price * FD_EPS * FD_EPS);
  }

  /**
   * Computes the modified duration from the standard yield.
   * <p>
   * The modified duration is defined as the minus of the first derivative of dirty price with respect to yield, 
   * divided by the dirty price.
   * <p>
   * The input yield and output are expressed in fraction.
   * 
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @param yield  the standard yield
   * @return the modified duration of the product 
   */
  public double modifiedDurationFromStandardYield(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LocalDate settlementDate,
      double yield) {

    int nbCoupon = bond.getPeriodicPayments().size();
    double couponPerYear = bond.getFrequency().eventsPerYear();
    double factorOnPeriod = 1d + yield / couponPerYear;
    double mdAtFirstCoupon = 0d;
    double pvAtFirstCoupon = 0d;
    int pow = 0;
    double factorToNext = factorToNextCoupon(bond, settlementDate);
    for (int loopcpn = 0; loopcpn < nbCoupon; loopcpn++) {
      CapitalIndexedBondPaymentPeriod period = bond.getPeriodicPayments().get(loopcpn);
      if ((bond.hasExCouponPeriod() && !settlementDate.isAfter(period.getDetachmentDate())) ||
          (!bond.hasExCouponPeriod() && period.getPaymentDate().isAfter(settlementDate))) {
        mdAtFirstCoupon += period.getRealCoupon() / Math.pow(factorOnPeriod, pow + 1) *
            (pow + factorToNext) / couponPerYear;
        pvAtFirstCoupon += period.getRealCoupon() / Math.pow(factorOnPeriod, pow);
        ++pow;
      }
    }
    mdAtFirstCoupon += (pow - 1d + factorToNext) / (couponPerYear * Math.pow(factorOnPeriod, pow));
    pvAtFirstCoupon += 1d / Math.pow(factorOnPeriod, pow - 1);
    double dp = pvAtFirstCoupon * Math.pow(factorOnPeriod, -factorToNext);
    double md = mdAtFirstCoupon * Math.pow(factorOnPeriod, -factorToNext) / dp;
    return md;
  }

  /**
   * Computes the covexity from the standard yield.
   * <p>
   * The convexity is defined as the second derivative of dirty price with respect to yield, divided by the dirty price.
   * <p>
   * The input yield and output are expressed in fraction.
   * 
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @param yield  the standard yield
   * @return the convexity of the product 
   */
  public double convexityFromStandardYield(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LocalDate settlementDate,
      double yield) {

    int nbCoupon = bond.getPeriodicPayments().size();
    double couponPerYear = bond.getFrequency().eventsPerYear();
    double factorOnPeriod = 1d + yield / couponPerYear;
    double cvAtFirstCoupon = 0;
    double pvAtFirstCoupon = 0;
    int pow = 0;
    double factorToNext = factorToNextCoupon(bond, settlementDate);
    for (int loopcpn = 0; loopcpn < nbCoupon; loopcpn++) {
      CapitalIndexedBondPaymentPeriod period = bond.getPeriodicPayments().get(loopcpn);
      if ((bond.hasExCouponPeriod() && !settlementDate.isAfter(period.getDetachmentDate())) ||
          (!bond.hasExCouponPeriod() && period.getPaymentDate().isAfter(settlementDate))) {
        cvAtFirstCoupon += period.getRealCoupon() * (pow + factorToNext) * (pow + factorToNext + 1d) /
            (Math.pow(factorOnPeriod, pow + 2) * couponPerYear * couponPerYear);
        pvAtFirstCoupon += period.getRealCoupon() / Math.pow(factorOnPeriod, pow);
        ++pow;
      }
    }
    cvAtFirstCoupon += (pow - 1d + factorToNext) *
        (pow + factorToNext) / (Math.pow(factorOnPeriod, pow + 1) * couponPerYear * couponPerYear);
    pvAtFirstCoupon += 1d / Math.pow(factorOnPeriod, pow - 1);
    double pv = pvAtFirstCoupon * Math.pow(factorOnPeriod, -factorToNext);
    double cv = cvAtFirstCoupon * Math.pow(factorOnPeriod, -factorToNext) / pv;
    return cv;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the dirty real price of the bond from its settlement date and clean real price.
   * 
   * @param bond  the product
   * @param settlementDate  the settlement date
   * @param cleanPrice  the clean real price
   * @return the price of the bond product
   */
  public double dirtyRealPriceFromCleanRealPrice(
      ResolvedCapitalIndexedBond bond,
      LocalDate settlementDate,
      double cleanPrice) {

    double notional = bond.getNotional();
    return cleanPrice + bond.accruedInterest(settlementDate) / notional;
  }

  /**
   * Calculates the clean real price of the bond from its settlement date and dirty real price.
   * 
   * @param bond  the product
   * @param settlementDate  the settlement date
   * @param dirtyPrice  the dirty real price
   * @return the price of the bond product
   */
  public double cleanRealPriceFromDirtyRealPrice(
      ResolvedCapitalIndexedBond bond,
      LocalDate settlementDate,
      double dirtyPrice) {

    double notional = bond.getNotional();
    return dirtyPrice - bond.accruedInterest(settlementDate) / notional;
  }

  /**
   * Calculates the dirty nominal price of the bond from its settlement date and clean nominal price.
   * 
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @param cleanPrice  the clean nominal price
   * @return the price of the bond product
   */
  public double dirtyNominalPriceFromCleanNominalPrice(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LocalDate settlementDate,
      double cleanPrice) {

    double notional = bond.getNotional();
    double indexRatio = indexRatio(bond, ratesProvider, settlementDate);
    return cleanPrice + bond.accruedInterest(settlementDate) / notional * indexRatio;
  }

  /**
   * Calculates the clean nominal price of the bond from its settlement date and dirty nominal price.
   * 
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @param dirtyPrice  the dirty nominal price
   * @return the price of the bond product
   */
  public double cleanNominalPriceFromDirtyNominalPrice(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LocalDate settlementDate,
      double dirtyPrice) {

    double notional = bond.getNotional();
    double indexRatio = indexRatio(bond, ratesProvider, settlementDate);
    return dirtyPrice - bond.accruedInterest(settlementDate) / notional * indexRatio;
  }

  /**
   * Calculates the real price of the bond from its settlement date and nominal price.
   * <p>
   * The input and output prices are both clean or dirty.
   * 
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @param nominalPrice  the nominal price
   * @return the price of the bond product
   */
  public double realPriceFromNominalPrice(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LocalDate settlementDate,
      double nominalPrice) {

    double indexRatio = indexRatio(bond, ratesProvider, settlementDate);
    return nominalPrice / indexRatio;
  }

  /**
   * Calculates the real price of the bond from its settlement date and nominal price
   * and its derivative wrt the nominal price.
   * <p>
   * The input and output prices are both clean or dirty.
   *
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @param nominalPrice  the nominal price
   * @return the price of the bond product and its derivative
   */
  public ValueDerivatives realPriceFromNominalPriceAd(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LocalDate settlementDate,
      double nominalPrice) {

    double indexRatio = indexRatio(bond, ratesProvider, settlementDate);
    return ValueDerivatives.of(nominalPrice / indexRatio, DoubleArray.of(1d / indexRatio));
  }

  /**
   * Calculates the nominal price of the bond from its settlement date and real price.
   * <p>
   * The input and output prices are both clean or dirty.
   * 
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @param realPrice  the real price
   * @return the price of the bond product
   */
  public double nominalPriceFromRealPrice(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LocalDate settlementDate,
      double realPrice) {

    double indexRatio = indexRatio(bond, ratesProvider, settlementDate);
    return realPrice * indexRatio;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the z-spread of the bond from curves and clean price.
   * <p>
   * The input clean price is real price or nominal price depending on the yield convention.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic
   * compounded rates of the discounting curve associated to the bond (Issuer Entity)
   * to match the present value.
   * 
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param discountingProvider  the discount factors provider
   * @param refData  the reference data used to calculate the settlement date
   * @param cleanPrice  the clean price
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the z-spread of the bond security
   */
  public double zSpreadFromCurvesAndCleanPrice(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      ReferenceData refData,
      double cleanPrice,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    validate(ratesProvider, discountingProvider);
    LocalDate settlementDate = bond.calculateSettlementDateFromValuation(ratesProvider.getValuationDate(), refData);
    final Function<Double, Double> residual = new Function<Double, Double>() {
      @Override
      public Double apply(Double z) {
        double dirtyPrice = dirtyNominalPriceFromCurvesWithZSpread(
            bond,
            ratesProvider,
            discountingProvider,
            settlementDate,
            z,
            compoundedRateType,
            periodsPerYear);
        double dirtyRealPrice = realPriceFromNominalPrice(bond, ratesProvider, settlementDate, dirtyPrice);
        return cleanRealPriceFromDirtyRealPrice(bond, settlementDate, dirtyRealPrice) - cleanPrice;
      }
    };
    double[] range = ROOT_BRACKETER.getBracketedPoints(residual, -0.5, 0.5); // Starting range is [-1%, 1%]
    return ROOT_FINDER.getRoot(residual, range[0], range[1]);
  }

  /**
   * Calculates the z-spread of the bond from curves and present value.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic
   * compounded rates of the discounting curve associated to the bond (Issuer Entity)
   * to match the present value.
   * 
   * @param bond  the product
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param discountingProvider  the discount factors provider
   * @param refData  the reference data used to calculate the settlement date
   * @param presentValue  the present value
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the z-spread of the bond product
   */
  public double zSpreadFromCurvesAndPv(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      ReferenceData refData,
      CurrencyAmount presentValue,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    validate(ratesProvider, discountingProvider);
    LocalDate settlementDate = bond.calculateSettlementDateFromValuation(ratesProvider.getValuationDate(), refData);
    final Function<Double, Double> residual = new Function<Double, Double>() {
      @Override
      public Double apply(Double z) {
        return presentValueWithZSpread(bond, ratesProvider, discountingProvider, settlementDate,
            z, compoundedRateType, periodsPerYear).getAmount() - presentValue.getAmount();
      }
    };
    double[] range = ROOT_BRACKETER.getBracketedPoints(residual, -0.5, 0.5); // Starting range is [-1%, 1%]
    return ROOT_FINDER.getRoot(residual, range[0], range[1]);
  }

  //-------------------------------------------------------------------------
  private double ratioPeriodToNextCoupon(CapitalIndexedBondPaymentPeriod bond, LocalDate settlementDate) {
    double nbDayToSpot = DAYS.between(settlementDate, bond.getUnadjustedEndDate());
    double nbDaysPeriod = DAYS.between(bond.getUnadjustedStartDate(), bond.getUnadjustedEndDate());
    return nbDayToSpot / nbDaysPeriod;
  }

  private double factorToNextCoupon(ResolvedCapitalIndexedBond bond, LocalDate settlementDate) {
    if (bond.getUnadjustedStartDate().isAfter(settlementDate)) {
      return 0d;
    }
    int periodIndex = bond.findPeriodIndex(settlementDate)
        .orElseThrow(() -> new IllegalArgumentException("Date outside range of bond"));
    CapitalIndexedBondPaymentPeriod period = bond.getPeriodicPayments().get(periodIndex);
    LocalDate previousAccrualDate = period.getUnadjustedStartDate();
    double factorSpot = bond.yearFraction(previousAccrualDate, settlementDate);
    double factorPeriod = bond.yearFraction(previousAccrualDate, period.getUnadjustedEndDate());
    return (factorPeriod - factorSpot) / factorPeriod;
  }

  double indexRatio(ResolvedCapitalIndexedBond bond, RatesProvider ratesProvider, LocalDate settlementDate) {
    LocalDate endReferenceDate = settlementDate.isBefore(ratesProvider.getValuationDate()) ?
        ratesProvider.getValuationDate() :
        settlementDate;
    RateComputation modifiedComputation = bond.getRateCalculation().createRateComputation(endReferenceDate);
    return 1d + periodPricer.getRateComputationFn().rate(
        modifiedComputation,
        bond.getUnadjustedStartDate(), // dates not used
        bond.getUnadjustedEndDate(),
        ratesProvider);
  }

  PointSensitivityBuilder indexRatioSensitivity(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      LocalDate settlementDate) {

    LocalDate endReferenceDate = settlementDate.isBefore(ratesProvider.getValuationDate()) ?
        ratesProvider.getValuationDate() :
        settlementDate;
    RateComputation modifiedComputation = bond.getRateCalculation().createRateComputation(endReferenceDate);
    return periodPricer.getRateComputationFn().rateSensitivity(
        modifiedComputation,
        bond.getUnadjustedStartDate(), // dates not used
        bond.getUnadjustedEndDate(),
        ratesProvider);
  }

  private void validate(RatesProvider ratesProvider, LegalEntityDiscountingProvider discountingProvider) {
    ArgChecker.isTrue(ratesProvider.getValuationDate().isEqual(discountingProvider.getValuationDate()),
        "the rates providers should be for the same date");
  }

  //-------------------------------------------------------------------------
  // compute pv of coupon payment(s) s.t. referenceDate1 < coupon <= referenceDate2
  double presentValueCoupon(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      IssuerCurveDiscountFactors discountFactors,
      LocalDate referenceDate1,
      LocalDate referenceDate2) {

    double pvDiff = 0d;
    for (CapitalIndexedBondPaymentPeriod period : bond.getPeriodicPayments()) {
      if (period.getDetachmentDate().isAfter(referenceDate1) && !period.getDetachmentDate().isAfter(referenceDate2)) {
        pvDiff += periodPricer.presentValue(period, ratesProvider, discountFactors);
      }
    }
    return pvDiff;
  }

  // compute pv of coupon payment(s) s.t. referenceDate1 < coupon <= referenceDate2
  double presentValueCouponWithZSpread(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      IssuerCurveDiscountFactors discountFactors,
      LocalDate referenceDate1,
      LocalDate referenceDate2,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    double pvDiff = 0d;
    for (CapitalIndexedBondPaymentPeriod period : bond.getPeriodicPayments()) {
      if (period.getDetachmentDate().isAfter(referenceDate1) && !period.getDetachmentDate().isAfter(referenceDate2)) {
        pvDiff += periodPricer.presentValueWithZSpread(
            period, ratesProvider, discountFactors, zSpread, compoundedRateType, periodsPerYear);
      }
    }
    return pvDiff;
  }

  PointSensitivityBuilder presentValueSensitivityCoupon(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      IssuerCurveDiscountFactors discountFactors,
      LocalDate referenceDate1,
      LocalDate referenceDate2) {

    PointSensitivityBuilder pvSensiDiff = PointSensitivityBuilder.none();
    for (CapitalIndexedBondPaymentPeriod period : bond.getPeriodicPayments()) {
      if (period.getDetachmentDate().isAfter(referenceDate1) && !period.getDetachmentDate().isAfter(referenceDate2)) {
        pvSensiDiff = pvSensiDiff.combinedWith(periodPricer.presentValueSensitivity(period, ratesProvider, discountFactors));
      }
    }
    return pvSensiDiff;
  }

  // compute pv sensitivity of coupon payment(s) s.t. referenceDate1 < coupon <= referenceDate2
  PointSensitivityBuilder presentValueSensitivityCouponWithZSpread(
      ResolvedCapitalIndexedBond bond,
      RatesProvider ratesProvider,
      IssuerCurveDiscountFactors discountFactors,
      LocalDate referenceDate1,
      LocalDate referenceDate2,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    PointSensitivityBuilder pvSensiDiff = PointSensitivityBuilder.none();
    for (CapitalIndexedBondPaymentPeriod period : bond.getPeriodicPayments()) {
      if (period.getDetachmentDate().isAfter(referenceDate1) && !period.getDetachmentDate().isAfter(referenceDate2)) {
        pvSensiDiff = pvSensiDiff.combinedWith(periodPricer.presentValueSensitivityWithZSpread(
            period, ratesProvider, discountFactors, zSpread, compoundedRateType, periodsPerYear));
      }
    }
    return pvSensiDiff;
  }

  //-------------------------------------------------------------------------
  // extracts the repo curve discount factors for the bond
  static RepoCurveDiscountFactors repoCurveDf(ResolvedCapitalIndexedBond bond, LegalEntityDiscountingProvider provider) {
    return provider.repoCurveDiscountFactors(bond.getSecurityId(), bond.getLegalEntityId(), bond.getCurrency());
  }

  // extracts the issuer curve discount factors for the bond
  static IssuerCurveDiscountFactors issuerCurveDf(ResolvedCapitalIndexedBond bond, LegalEntityDiscountingProvider provider) {
    return provider.issuerCurveDiscountFactors(bond.getLegalEntityId(), bond.getCurrency());
  }

}
