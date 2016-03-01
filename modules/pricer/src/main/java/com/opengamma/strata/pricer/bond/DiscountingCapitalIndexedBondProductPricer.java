/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;


import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MONTHS;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.function.Function;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.RepoCurveZeroRateSensitivity;
import com.opengamma.strata.market.value.CompoundedRateType;
import com.opengamma.strata.market.view.IssuerCurveDiscountFactors;
import com.opengamma.strata.market.view.RepoCurveDiscountFactors;
import com.opengamma.strata.math.impl.rootfinding.BracketRoot;
import com.opengamma.strata.math.impl.rootfinding.BrentSingleRootFinder;
import com.opengamma.strata.math.impl.rootfinding.RealSingleRootFinder;
import com.opengamma.strata.pricer.impl.bond.DiscountingCapitalIndexedBondPaymentPeriodPricer;
import com.opengamma.strata.pricer.rate.LegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.bond.CapitalIndexedBond;
import com.opengamma.strata.product.bond.CapitalIndexedBondPaymentPeriod;
import com.opengamma.strata.product.bond.ExpandedCapitalIndexedBond;
import com.opengamma.strata.product.bond.YieldConvention;
import com.opengamma.strata.product.rate.InflationEndInterpolatedRateObservation;
import com.opengamma.strata.product.rate.InflationEndMonthRateObservation;
import com.opengamma.strata.product.rate.RateObservation;

/**
 * Pricer for capital indexed bond products.
 * <p>
 * This function provides the ability to price a {@link CapitalIndexedBond}.
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
   * Calculates the present value of the product.
   * <p>
   * The present value of the product is the value on the valuation date.
   * The result is expressed using the payment currency of the bond.
   * <p>
   * Coupon payments of the product are considered based on the valuation date. 
   * 
   * @param product  the product to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactorsProvider  the discount factors provider
   * @return the present value of the product
   */
  public CurrencyAmount presentValue(
      CapitalIndexedBond product,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider) {

    validate(ratesProvider, issuerDiscountFactorsProvider);
    return presentValue(product, ratesProvider, issuerDiscountFactorsProvider, ratesProvider.getValuationDate());
  }

  // calculate the present value
  CurrencyAmount presentValue(
      CapitalIndexedBond product,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      LocalDate referenceDate) {

    ExpandedCapitalIndexedBond expanded = product.expand();
    IssuerCurveDiscountFactors issuerDiscountFactors = issuerDiscountFactorsProvider.issuerCurveDiscountFactors(
        product.getLegalEntityId(), product.getCurrency());
    double pvNominal =
        periodPricer.presentValue(expanded.getNominalPayment(), ratesProvider, issuerDiscountFactors);
    double pvCoupon = 0d;
    for (CapitalIndexedBondPaymentPeriod period : expanded.getPeriodicPayments()) {
      if ((product.getExCouponPeriod().getDays() != 0 && period.getDetachmentDate().isAfter(referenceDate)) ||
          (product.getExCouponPeriod().getDays() == 0 && period.getPaymentDate().isAfter(referenceDate))) {
        pvCoupon += periodPricer.presentValue(period, ratesProvider, issuerDiscountFactors);
      }
    }
    return CurrencyAmount.of(product.getCurrency(), pvCoupon + pvNominal);
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
   * @param product  the product to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactorsProvider  the discount factors provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the present value of the bond product
   */
  public CurrencyAmount presentValueWithZSpread(
      CapitalIndexedBond product,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    validate(ratesProvider, issuerDiscountFactorsProvider);
    return presentValueWithZSpread(product, ratesProvider, issuerDiscountFactorsProvider,
        ratesProvider.getValuationDate(), zSpread, compoundedRateType, periodsPerYear);
  }

  // calculate the present value
  CurrencyAmount presentValueWithZSpread(
      CapitalIndexedBond product,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      LocalDate referenceDate,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    ExpandedCapitalIndexedBond expanded = product.expand();
    IssuerCurveDiscountFactors issuerDiscountFactors = issuerDiscountFactorsProvider.issuerCurveDiscountFactors(
        product.getLegalEntityId(), product.getCurrency());
    double pvNominal = periodPricer.presentValueWithZSpread(
            expanded.getNominalPayment(), ratesProvider, issuerDiscountFactors, zSpread, compoundedRateType, periodsPerYear);
    double pvCoupon = 0d;
    for (CapitalIndexedBondPaymentPeriod period : expanded.getPeriodicPayments()) {
      if ((product.getExCouponPeriod().getDays() != 0 && period.getDetachmentDate().isAfter(referenceDate)) ||
          (product.getExCouponPeriod().getDays() == 0 && period.getPaymentDate().isAfter(referenceDate))) {
        pvCoupon += periodPricer.presentValueWithZSpread(
            period, ratesProvider, issuerDiscountFactors, zSpread, compoundedRateType, periodsPerYear);
      }
    }
    return CurrencyAmount.of(product.getCurrency(), pvCoupon + pvNominal);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the bond product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param product  the product to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactorsProvider  the discount factors provider
   * @return the present value curve sensitivity of the product
   */
  public PointSensitivityBuilder presentValueSensitivity(
      CapitalIndexedBond product,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider
      issuerDiscountFactorsProvider) {

    validate(ratesProvider, issuerDiscountFactorsProvider);
    return presentValueSensitivity(
        product, ratesProvider, issuerDiscountFactorsProvider, ratesProvider.getValuationDate());
  }

  // calculate the present value sensitivity
  PointSensitivityBuilder presentValueSensitivity(
      CapitalIndexedBond product,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      LocalDate referenceDate) {

    ExpandedCapitalIndexedBond expanded = product.expand();
    IssuerCurveDiscountFactors issuerDiscountFactors = issuerDiscountFactorsProvider.issuerCurveDiscountFactors(
        product.getLegalEntityId(), product.getCurrency());
    PointSensitivityBuilder pointNominal =
        periodPricer.presentValueSensitivity(expanded.getNominalPayment(), ratesProvider, issuerDiscountFactors);
    PointSensitivityBuilder pointCoupon = PointSensitivityBuilder.none();
    for (CapitalIndexedBondPaymentPeriod period : expanded.getPeriodicPayments()) {
      if ((product.getExCouponPeriod().getDays() != 0 && period.getDetachmentDate().isAfter(referenceDate)) ||
          (product.getExCouponPeriod().getDays() == 0 && period.getPaymentDate().isAfter(referenceDate))) {
        pointCoupon = pointCoupon.combinedWith(
            periodPricer.presentValueSensitivity(period, ratesProvider, issuerDiscountFactors));
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
   * @param product  the product to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactorsProvider  the discount factors provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the present value curve sensitivity of the product
   */
  public PointSensitivityBuilder presentValueSensitivityWithZSpread(
      CapitalIndexedBond product,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    validate(ratesProvider, issuerDiscountFactorsProvider);
    return presentValueSensitivityWithZSpread(product, ratesProvider, issuerDiscountFactorsProvider,
        ratesProvider.getValuationDate(), zSpread, compoundedRateType, periodsPerYear);
  }

  // calculate the present value sensitivity
  PointSensitivityBuilder presentValueSensitivityWithZSpread(
      CapitalIndexedBond product,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      LocalDate referenceDate,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    ExpandedCapitalIndexedBond expanded = product.expand();
    IssuerCurveDiscountFactors issuerDiscountFactors = issuerDiscountFactorsProvider.issuerCurveDiscountFactors(
        product.getLegalEntityId(), product.getCurrency());
    PointSensitivityBuilder pointNominal = periodPricer.presentValueSensitivityWithZSpread(
        expanded.getNominalPayment(), ratesProvider, issuerDiscountFactors, zSpread, compoundedRateType, periodsPerYear);
    PointSensitivityBuilder pointCoupon = PointSensitivityBuilder.none();
    for (CapitalIndexedBondPaymentPeriod period : expanded.getPeriodicPayments()) {
      if ((product.getExCouponPeriod().getDays() != 0 && period.getDetachmentDate().isAfter(referenceDate)) ||
          (product.getExCouponPeriod().getDays() == 0 && period.getPaymentDate().isAfter(referenceDate))) {
        pointCoupon = pointCoupon.combinedWith(periodPricer.presentValueSensitivityWithZSpread(
            period, ratesProvider, issuerDiscountFactors, zSpread, compoundedRateType, periodsPerYear));
      }
    }
    return pointNominal.combinedWith(pointCoupon);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the bond product.
   * 
   * @param product  the product to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactorsProvider  the discount factors provider
   * @param referenceDate  the reference date
   * @return the currency exposure of the product 
   */
  public MultiCurrencyAmount currencyExposure(
      CapitalIndexedBond product,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      LocalDate referenceDate) {

    return MultiCurrencyAmount.of(presentValue(product, ratesProvider, issuerDiscountFactorsProvider, referenceDate));
  }

  /**
   * Calculates the currency exposure of the bond product with z-spread.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or
   * periodic compounded rates of the issuer discounting curve. 
   * 
   * @param product  the product to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactorsProvider  the discount factors provider
   * @param referenceDate  the reference date
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the currency exposure of the product 
   */
  public MultiCurrencyAmount currencyExposureWithZSpread(
      CapitalIndexedBond product,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      LocalDate referenceDate,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    return MultiCurrencyAmount.of(presentValueWithZSpread(product, ratesProvider, issuerDiscountFactorsProvider,
        referenceDate, zSpread, compoundedRateType, periodsPerYear));
  }

  /**
   * Calculates the current cash of the bond product.
   * 
   * @param product  the product to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @return the current cash of the product 
   */
  public CurrencyAmount currentCash(
      CapitalIndexedBond product,
      RatesProvider ratesProvider,
      LocalDate settlementDate) {

    LocalDate valuationDate = ratesProvider.getValuationDate();
    Currency currency = product.getCurrency();
    CurrencyAmount currentCash = CurrencyAmount.zero(currency);
    if (settlementDate.isBefore(valuationDate)) {
      ExpandedCapitalIndexedBond expanded = product.expand();
      double cashCoupon = product.getExCouponPeriod().getDays() != 0 ? 0d :
          currentCashPayment(expanded, ratesProvider, valuationDate);
      CapitalIndexedBondPaymentPeriod nominal = expanded.getNominalPayment();
      double cashNominal = nominal.getPaymentDate().isEqual(valuationDate) ?
          periodPricer.forecastValue(nominal, ratesProvider) : 0d;
      currentCash = currentCash.plus(CurrencyAmount.of(currency, cashCoupon + cashNominal));
    }
    return currentCash;
  }

  private double currentCashPayment(
      ExpandedCapitalIndexedBond product,
      RatesProvider ratesProvider,
      LocalDate valuationDate) {

    double cash = 0d;
    for (CapitalIndexedBondPaymentPeriod period : product.getPeriodicPayments()) {
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
   * 
   * @param security  the security to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactorsProvider  the discount factors provider
   * @return the dirty price of the bond security
   */
  public double dirtyNominalPriceFromCurves(
      Security<CapitalIndexedBond> security,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider) {

    validate(ratesProvider, issuerDiscountFactorsProvider);
    CapitalIndexedBond product = security.getProduct();
    LocalDate settlementDate = product.getSettlementDateOffset().adjust(ratesProvider.getValuationDate());
    return dirtyNominalPriceFromCurves(security, ratesProvider, issuerDiscountFactorsProvider, settlementDate);
  }

  // calculate the dirty price
  double dirtyNominalPriceFromCurves(
      Security<CapitalIndexedBond> security,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      LocalDate settlementDate) {

    CapitalIndexedBond product = security.getProduct();
    CurrencyAmount pv = presentValue(product, ratesProvider, issuerDiscountFactorsProvider, settlementDate);
    StandardId securityId = security.getStandardId();
    StandardId legalEntityId = product.getLegalEntityId();
    double df = issuerDiscountFactorsProvider.repoCurveDiscountFactors(
        securityId, legalEntityId, product.getCurrency()).discountFactor(settlementDate);
    double notional = product.getNotional();
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
   * @param security  the security to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactorsProvider  the discount factors provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the dirty price of the bond security
   */
  public double dirtyNominalPriceFromCurvesWithZSpread(
      Security<CapitalIndexedBond> security,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    validate(ratesProvider, issuerDiscountFactorsProvider);
    CapitalIndexedBond product = security.getProduct();
    LocalDate settlementDate = product.getSettlementDateOffset().adjust(ratesProvider.getValuationDate());
    return dirtyNominalPriceFromCurvesWithZSpread(security, ratesProvider, issuerDiscountFactorsProvider,
        settlementDate, zSpread, compoundedRateType, periodsPerYear);
  }

  // calculate the dirty price
  double dirtyNominalPriceFromCurvesWithZSpread(
      Security<CapitalIndexedBond> security,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      LocalDate settlementDate,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    CapitalIndexedBond product = security.getProduct();
    CurrencyAmount pv = presentValueWithZSpread(
        product, ratesProvider, issuerDiscountFactorsProvider, settlementDate, zSpread, compoundedRateType,
        periodsPerYear);
    StandardId securityId = security.getStandardId();
    StandardId legalEntityId = product.getLegalEntityId();
    double df = issuerDiscountFactorsProvider.repoCurveDiscountFactors(
        securityId, legalEntityId, product.getCurrency()).discountFactor(settlementDate);
    double notional = product.getNotional();
    return pv.getAmount() / (df * notional);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the dirty price sensitivity of the bond security.
   * <p>
   * The dirty price sensitivity of the security is the sensitivity of the dirty price value to
   * the underlying curves.
   * 
   * @param security  the security to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactorsProvider  the discount factors provider
   * @return the dirty price curve sensitivity of the security
   */
  public PointSensitivityBuilder dirtyNominalPriceSensitivity(
      Security<CapitalIndexedBond> security,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider) {

    validate(ratesProvider, issuerDiscountFactorsProvider);
    CapitalIndexedBond product = security.getProduct();
    LocalDate settlementDate = product.getSettlementDateOffset().adjust(ratesProvider.getValuationDate());
    return dirtyNominalPriceSensitivity(security, ratesProvider, issuerDiscountFactorsProvider, settlementDate);
  }

  // calculate the dirty price sensitivity
  PointSensitivityBuilder dirtyNominalPriceSensitivity(
      Security<CapitalIndexedBond> security,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      LocalDate settlementDate) {

    CapitalIndexedBond product = security.getProduct();
    double notional = product.getNotional();
    CurrencyAmount pv = presentValue(product, ratesProvider, issuerDiscountFactorsProvider, settlementDate);
    StandardId securityId = security.getStandardId();
    StandardId legalEntityId = product.getLegalEntityId();
    RepoCurveDiscountFactors discountFactors =
        issuerDiscountFactorsProvider.repoCurveDiscountFactors(securityId, legalEntityId, product.getCurrency());
    double df = discountFactors.discountFactor(settlementDate);
    PointSensitivityBuilder pvSensi = presentValueSensitivity(
        product, ratesProvider, issuerDiscountFactorsProvider, settlementDate).multipliedBy(1d / (df * notional));
    RepoCurveZeroRateSensitivity dfSensi = discountFactors
        .zeroRatePointSensitivity(settlementDate).multipliedBy(-pv.getAmount() / (df * df * notional));
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
   * @param security  the security to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactorsProvider  the discount factors provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the dirty price curve sensitivity of the security
   */
  public PointSensitivityBuilder dirtyNominalPriceSensitivityWithZSpread(
      Security<CapitalIndexedBond> security,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    validate(ratesProvider, issuerDiscountFactorsProvider);
    CapitalIndexedBond product = security.getProduct();
    LocalDate settlementDate = product.getSettlementDateOffset().adjust(ratesProvider.getValuationDate());
    return dirtyNominalPriceSensitivityWithZSpread(
        security, ratesProvider, issuerDiscountFactorsProvider, settlementDate, zSpread, compoundedRateType, periodsPerYear);
  }

  // calculate the dirty price sensitivity
  PointSensitivityBuilder dirtyNominalPriceSensitivityWithZSpread(
      Security<CapitalIndexedBond> security,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      LocalDate settlementDate,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    CapitalIndexedBond product = security.getProduct();
    double notional = product.getNotional();
    CurrencyAmount pv = presentValueWithZSpread(
        product, ratesProvider, issuerDiscountFactorsProvider, settlementDate, zSpread, compoundedRateType,
        periodsPerYear);
    StandardId securityId = security.getStandardId();
    StandardId legalEntityId = product.getLegalEntityId();
    RepoCurveDiscountFactors discountFactors =
        issuerDiscountFactorsProvider.repoCurveDiscountFactors(securityId, legalEntityId, product.getCurrency());
    double df = discountFactors.discountFactor(settlementDate);
    PointSensitivityBuilder pvSensi = presentValueSensitivityWithZSpread(product, ratesProvider, issuerDiscountFactorsProvider,
        settlementDate, zSpread, compoundedRateType, periodsPerYear).multipliedBy(1d / (df * notional));
    RepoCurveZeroRateSensitivity dfSensi = discountFactors
        .zeroRatePointSensitivity(settlementDate).multipliedBy(-pv.getAmount() / df / df / notional);
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
   * @param product  the product to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @param yield  the yield
   * @return the dirty price of the product 
   */
  public double dirtyPriceFromRealYield(
      CapitalIndexedBond product,
      RatesProvider ratesProvider,
      LocalDate settlementDate,
      double yield) {

    ArgChecker.isTrue(settlementDate.isBefore(product.getPeriodicSchedule().getEndDate()),
        "settlement date must be before end date");
    Schedule scheduleAdjusted = product.getPeriodicSchedule().createSchedule();
    Schedule scheduleUnadjusted = scheduleAdjusted.toUnadjusted();
    List<Double> coupon = product.getRateCalculation().getGearing().orElse(ValueSchedule.ALWAYS_1)
        .resolveValues(scheduleAdjusted.getPeriods());
    int nbCoupon = scheduleAdjusted.getPeriods().size() - couponIndex(scheduleUnadjusted, settlementDate);
    YieldConvention yieldConvention = product.getYieldConvention();
    if (yieldConvention.equals(YieldConvention.US_IL_REAL)) {
      double pvAtFirstCoupon;
      double cpnRate = coupon.get(0);
      double couponPerYear = product.getPeriodicSchedule().getFrequency().eventsPerYear();
      if (Math.abs(yield) > 1.0E-8) {
        double factorOnPeriod = 1d + yield / couponPerYear;
        double vn = Math.pow(factorOnPeriod, 1 - nbCoupon);
        pvAtFirstCoupon = cpnRate * couponPerYear / yield * (factorOnPeriod - vn) + vn;
      } else {
        pvAtFirstCoupon = cpnRate * nbCoupon + 1d;
      }
      return pvAtFirstCoupon /
          (1d + factorToNextCoupon(scheduleUnadjusted, product.getDayCount(), settlementDate) * yield / couponPerYear);
    }

    int couponIndex = couponIndex(scheduleUnadjusted, settlementDate);
    double realRate = coupon.get(couponIndex);
    double firstYearFraction =
        scheduleUnadjusted.getPeriod(couponIndex).yearFraction(product.getDayCount(), scheduleUnadjusted);
    double v = 1d / (1d + yield / product.getPeriodicSchedule().getFrequency().eventsPerYear());
    if (yieldConvention.equals(YieldConvention.INDEX_LINKED_FLOAT)) {
      ExpandedCapitalIndexedBond expanded = product.expand();
      RateObservation obs = expanded.getPeriodicPayments().get(couponIndex).getRateObservation();
      LocalDateDoubleTimeSeries ts = ratesProvider.priceIndexValues(product.getRateCalculation().getIndex()).getFixings();
      YearMonth lastKnownFixingMonth = YearMonth.from(ts.getLatestDate());
      double indexRatio = ts.getLatestValue() / product.getStartIndexValue();
      YearMonth endFixingMonth = null;
      if (obs instanceof InflationEndInterpolatedRateObservation) {
        endFixingMonth = ((InflationEndInterpolatedRateObservation) obs).getReferenceEndInterpolationMonth();
      } else if (obs instanceof InflationEndMonthRateObservation) {
        endFixingMonth = ((InflationEndMonthRateObservation) obs).getReferenceEndMonth();
      } else {
        throw new IllegalArgumentException("The rate observation " + obs.toString() + " is not supported.");
      }
      double nbMonth = Math.abs(MONTHS.between(endFixingMonth, lastKnownFixingMonth));
      double u = Math.sqrt(1d / 1.03);
      double a = indexRatio * Math.pow(u, nbMonth / 6d);
      double firstCashFlow = firstYearFraction * realRate * indexRatio;
      if (nbCoupon == 1) {
        return (realRate + 1d) * a / u *
            Math.pow(u * v, ratioPeriodToNextCoupon(expanded.getPeriodicPayments().get(couponIndex), settlementDate));
      } else {
        double secondYearFraction =
            scheduleUnadjusted.getPeriod(couponIndex  + 1).yearFraction(product.getDayCount(), scheduleUnadjusted);
        double secondCashFlow = secondYearFraction * realRate * indexRatio;
        double vn = Math.pow(v, nbCoupon - 1);
        double pvAtFirstCoupon =
            firstCashFlow + secondCashFlow * u * v + a * realRate * v * v * (1d - vn / v) / (1d - v) + a * vn;
        return pvAtFirstCoupon *
            Math.pow(u * v, ratioPeriodToNextCoupon(expanded.getPeriodicPayments().get(couponIndex), settlementDate));
      }
    }
    if (yieldConvention.equals(YieldConvention.UK_IL_BOND)) {
      double firstCashFlow = firstYearFraction * realRate;
      if (nbCoupon == 1) {
        return Math.pow(v, factorToNextCoupon(scheduleUnadjusted, product.getDayCount(),
            settlementDate)) * (firstCashFlow + 1);
      } else {
        double secondYearFraction =
            scheduleUnadjusted.getPeriod(couponIndex + 1).yearFraction(product.getDayCount(), scheduleUnadjusted);
        double secondCashFlow = secondYearFraction * realRate;
        double vn = Math.pow(v, nbCoupon - 1);
        double pvAtFirstCoupon =
            firstCashFlow + secondCashFlow * v + realRate * v * v * (1d - vn / v) / (1d - v) + vn;
        return pvAtFirstCoupon *
            Math.pow(v, factorToNextCoupon(scheduleUnadjusted, product.getDayCount(), settlementDate));
      }
    }
    throw new IllegalArgumentException(
        "The convention " + product.getYieldConvention().toString() + " is not supported.");
  }

  /**
   * Computes the clean price from the conventional real yield.
   * <p>
   * The resulting clean price is real price or nominal price depending on the yield convention. 
   * <p>
   * The input yield and output are expressed in fraction. 
   * 
   * @param product  the product to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @param yield  the yield
   * @return the clean price of the product 
   */
  public double cleanPriceFromRealYield(
      CapitalIndexedBond product,
      RatesProvider ratesProvider,
      LocalDate settlementDate,
      double yield) {

    double dirtyPrice = dirtyPriceFromRealYield(product, ratesProvider, settlementDate, yield);
    if (product.getYieldConvention().equals(YieldConvention.INDEX_LINKED_FLOAT)) {
      return cleanNominalPriceFromDirtyNominalPrice(product, ratesProvider, settlementDate, dirtyPrice);
    }
    return cleanRealPriceFromDirtyRealPrice(product, settlementDate, dirtyPrice);
  }

  /**
   * Computes the conventional real yield from the dirty price.
   * <p>
   * The input dirty price should be real price or nominal price depending on the yield convention. This is coherent to  
   * the implementation of {@link #dirtyPriceFromRealYield(CapitalIndexedBond, RatesProvider, LocalDate, double)}.
   * <p>
   * The input price and output are expressed in fraction. 
   * 
   * @param product  the product to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @param dirtyPrice  the bond dirty price
   * @return the yield of the product 
   */
  public double realYieldFromDirtyPrice(
      CapitalIndexedBond product,
      RatesProvider ratesProvider,
      LocalDate settlementDate,
      double dirtyPrice) {

    final Function<Double, Double> priceResidual = new Function<Double, Double>() {
      @Override
      public Double apply(Double y) {
        return dirtyPriceFromRealYield(product, ratesProvider, settlementDate, y) - dirtyPrice;
      }
    };
    double[] range = ROOT_BRACKETER.getBracketedPoints(priceResidual, -0.05, 0.10);
    double yield = ROOT_FINDER.getRoot(priceResidual, range[0], range[1]);
    return yield;
  }

  /**
   * Computes the conventional real yield from the curves. 
   * <p>
   * The yield is in the bill yield convention.
   * 
   * @param security  the security to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactorsProvider  the discount factors provider
   * @return the yield of the product 
   */
  public double realYieldFromCurves(
      Security<CapitalIndexedBond> security,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider) {

    validate(ratesProvider, issuerDiscountFactorsProvider);
    CapitalIndexedBond product = security.getProduct();
    LocalDate settlementDate = product.getSettlementDateOffset().adjust(ratesProvider.getValuationDate());
    double dirtyPrice;
    if (product.getYieldConvention().equals(YieldConvention.INDEX_LINKED_FLOAT)) {
      dirtyPrice = dirtyNominalPriceFromCurves(security, ratesProvider, issuerDiscountFactorsProvider, settlementDate);
    } else {
      double dirtyNominalPrice =
          dirtyNominalPriceFromCurves(security, ratesProvider, issuerDiscountFactorsProvider, settlementDate);
      dirtyPrice = realPriceFromNominalPrice(product, ratesProvider, settlementDate, dirtyNominalPrice);
    }
    return realYieldFromDirtyPrice(product, ratesProvider, settlementDate, dirtyPrice);
  }

  /**
   * Computes the dirty price from the standard yield.
   * <p>
   * The input yield and output are expressed in fraction. 
   * 
   * @param product  the product to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @param yield  the standard yield
   * @return the dirty price of the product 
   */
  public double dirtyPriceFromStandardYield(
      CapitalIndexedBond product,
      RatesProvider ratesProvider,
      LocalDate settlementDate,
      double yield) {

    ExpandedCapitalIndexedBond expanded = product.expand();
    Schedule scheduleUnadjusted = product.getPeriodicSchedule().createSchedule().toUnadjusted();
    int nbCoupon = expanded.getPeriodicPayments().size();
    double couponPerYear = product.getPeriodicSchedule().getFrequency().eventsPerYear();
    double factorOnPeriod = 1d + yield / couponPerYear;
    double pvAtFirstCoupon = 0d;
    int pow = 0;
    double factorToNext = factorToNextCoupon(scheduleUnadjusted, product.getDayCount(), settlementDate);
    for (int loopcpn = 0; loopcpn < nbCoupon; loopcpn++) {
      CapitalIndexedBondPaymentPeriod period = expanded.getPeriodicPayments().get(loopcpn);
      if ((product.getExCouponPeriod().getDays() != 0 && !settlementDate.isAfter(period.getDetachmentDate())) ||
          (product.getExCouponPeriod().getDays() == 0 && period.getPaymentDate().isAfter(settlementDate))) {
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
   * the implementation of {@link #dirtyPriceFromRealYield(CapitalIndexedBond, RatesProvider, LocalDate, double)}.
   * <p>
   * The input yield and output are expressed in fraction. 
   * 
   * @param product  the product to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @param yield  the yield
   * @return the modified duration of the product 
   */
  public double modifiedDurationFromRealYieldFiniteDifference(
      CapitalIndexedBond product,
      RatesProvider ratesProvider,
      LocalDate settlementDate,
      double yield) {

    double price = cleanPriceFromRealYield(product, ratesProvider, settlementDate, yield);
    double priceplus = cleanPriceFromRealYield(product, ratesProvider, settlementDate, yield + FD_EPS);
    double priceminus = cleanPriceFromRealYield(product, ratesProvider, settlementDate, yield - FD_EPS);
    return -0.5 * (priceplus - priceminus) / (price * FD_EPS);
  }

  /**
   * Calculates the convexity from the conventional real yield using finite difference approximation.
   * <p>
   * The convexity is defined as the second derivative of clean price with respect to yield, divided by the clean price. 
   * <p>
   * The clean price here is real price or nominal price depending on the yield convention. This is coherent to 
   * the implementation of {@link #dirtyPriceFromRealYield(CapitalIndexedBond, RatesProvider, LocalDate, double)}.
   * <p>
   * The input yield and output are expressed in fraction. 
   * 
   * @param product  the product to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @param yield  the yield
   * @return the covexity of the product 
   */
  public double convexityFromRealYieldFiniteDifference(
      CapitalIndexedBond product,
      RatesProvider ratesProvider,
      LocalDate settlementDate,
      double yield) {

    double price = cleanPriceFromRealYield(product, ratesProvider, settlementDate, yield);
    double priceplus = cleanPriceFromRealYield(product, ratesProvider, settlementDate, yield + FD_EPS);
    double priceminus = cleanPriceFromRealYield(product, ratesProvider, settlementDate, yield - FD_EPS);
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
   * @param product  the product to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @param yield  the standard yield
   * @return the modified duration of the product 
   */
  public double modifiedDurationFromStandardYield(
      CapitalIndexedBond product,
      RatesProvider ratesProvider,
      LocalDate settlementDate,
      double yield) {

    ExpandedCapitalIndexedBond expanded = product.expand();
    Schedule scheduleUnadjusted = product.getPeriodicSchedule().createSchedule().toUnadjusted();
    int nbCoupon = expanded.getPeriodicPayments().size();
    double couponPerYear = product.getPeriodicSchedule().getFrequency().eventsPerYear();
    double factorOnPeriod = 1d + yield / couponPerYear;
    double mdAtFirstCoupon = 0d;
    double pvAtFirstCoupon = 0d;
    int pow = 0;
    double factorToNext = factorToNextCoupon(scheduleUnadjusted, product.getDayCount(), settlementDate);
    for (int loopcpn = 0; loopcpn < nbCoupon; loopcpn++) {
      CapitalIndexedBondPaymentPeriod period = expanded.getPeriodicPayments().get(loopcpn);
      if ((product.getExCouponPeriod().getDays() != 0 && !settlementDate.isAfter(period.getDetachmentDate())) ||
          (product.getExCouponPeriod().getDays() == 0 && period.getPaymentDate().isAfter(settlementDate))) {
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
   * @param product  the product to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @param yield  the standard yield
   * @return the convexity of the product 
   */
  public double convexityFromStandardYield(
      CapitalIndexedBond product,
      RatesProvider ratesProvider,
      LocalDate settlementDate,
      double yield) {

    ExpandedCapitalIndexedBond expanded = product.expand();
    Schedule scheduleUnadjusted = product.getPeriodicSchedule().createSchedule().toUnadjusted();
    int nbCoupon = expanded.getPeriodicPayments().size();
    double couponPerYear = product.getPeriodicSchedule().getFrequency().eventsPerYear();
    double factorOnPeriod = 1d + yield / couponPerYear;
    double cvAtFirstCoupon = 0;
    double pvAtFirstCoupon = 0;
    int pow = 0;
    double factorToNext = factorToNextCoupon(scheduleUnadjusted, product.getDayCount(), settlementDate);
    for (int loopcpn = 0; loopcpn < nbCoupon; loopcpn++) {
      CapitalIndexedBondPaymentPeriod period = expanded.getPeriodicPayments().get(loopcpn);
      if ((product.getExCouponPeriod().getDays() != 0 && !settlementDate.isAfter(period.getDetachmentDate())) ||
          (product.getExCouponPeriod().getDays() == 0 && period.getPaymentDate().isAfter(settlementDate))) {
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
   * @param product  the product to price
   * @param settlementDate  the settlement date
   * @param cleanPrice  the clean real price
   * @return the price of the bond product
   */
  public double dirtyRealPriceFromCleanRealPrice(
      CapitalIndexedBond product,
      LocalDate settlementDate,
      double cleanPrice) {

    double notional = product.getNotional();
    return cleanPrice + accruedInterest(product, settlementDate) / notional;
  }

  /**
   * Calculates the clean real price of the bond from its settlement date and dirty real price.
   * 
   * @param product  the product to price
   * @param settlementDate  the settlement date
   * @param dirtyPrice  the dirty real price
   * @return the price of the bond product
   */
  public double cleanRealPriceFromDirtyRealPrice(
      CapitalIndexedBond product,
      LocalDate settlementDate,
      double dirtyPrice) {

    double notional = product.getNotional();
    return dirtyPrice - accruedInterest(product, settlementDate) / notional;
  }

  /**
   * Calculates the dirty nominal price of the bond from its settlement date and clean nominal price.
   * 
   * @param product  the product to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @param cleanPrice  the clean nominal price
   * @return the price of the bond product
   */
  public double dirtyNominalPriceFromCleanNominalPrice(
      CapitalIndexedBond product,
      RatesProvider ratesProvider,
      LocalDate settlementDate,
      double cleanPrice) {

    double notional = product.getNotional();
    double indexRatio = indexRatio(product, ratesProvider, settlementDate);
    return cleanPrice + accruedInterest(product, settlementDate) / notional * indexRatio;
  }

  /**
   * Calculates the clean nominal price of the bond from its settlement date and dirty nominal price.
   * 
   * @param product  the product to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @param dirtyPrice  the dirty nominal price
   * @return the price of the bond product
   */
  public double cleanNominalPriceFromDirtyNominalPrice(
      CapitalIndexedBond product,
      RatesProvider ratesProvider,
      LocalDate settlementDate,
      double dirtyPrice) {

    double notional = product.getNotional();
    double indexRatio = indexRatio(product, ratesProvider, settlementDate);
    return dirtyPrice - accruedInterest(product, settlementDate) / notional * indexRatio;
  }

  /**
   * Calculates the real price of the bond from its settlement date and nominal price.
   * <p>
   * The input and output prices are both clean or dirty. 
   * 
   * @param product  the product to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @param nominalPrice  the nominal price
   * @return the price of the bond product
   */
  public double realPriceFromNominalPrice(
      CapitalIndexedBond product,
      RatesProvider ratesProvider,
      LocalDate settlementDate,
      double nominalPrice) {

    double indexRatio = indexRatio(product, ratesProvider, settlementDate);
    return nominalPrice / indexRatio;
  }

  /**
   * Calculates the nominal price of the bond from its settlement date and real price.
   * <p>
   * The input and output prices are both clean or dirty. 
   * 
   * @param product  the product to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param settlementDate  the settlement date
   * @param realPrice  the real price
   * @return the price of the bond product
   */
  public double nominalPriceFromRealPrice(
      CapitalIndexedBond product,
      RatesProvider ratesProvider,
      LocalDate settlementDate,
      double realPrice) {

    double indexRatio = indexRatio(product, ratesProvider, settlementDate);
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
   * @param security  the security to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactorsProvider  the discount factors provider
   * @param cleanPrice  the clean price
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the z-spread of the bond security
   */
  public double zSpreadFromCurvesAndCleanPrice(
      Security<CapitalIndexedBond> security,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      double cleanPrice,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    validate(ratesProvider, issuerDiscountFactorsProvider);
    CapitalIndexedBond product = security.getProduct();
    LocalDate settlementDate = product.getSettlementDateOffset().adjust(ratesProvider.getValuationDate());
    final Function<Double, Double> residual = new Function<Double, Double>() {
      @Override
      public Double apply(Double z) {
        double dirtyPrice = dirtyNominalPriceFromCurvesWithZSpread(
            security, ratesProvider, issuerDiscountFactorsProvider, settlementDate, z, compoundedRateType, periodsPerYear);
        if (product.getYieldConvention().equals(YieldConvention.INDEX_LINKED_FLOAT)) {
          return cleanNominalPriceFromDirtyNominalPrice(product, ratesProvider, settlementDate, dirtyPrice) - cleanPrice;
        }
        double dirtyRealPrice = realPriceFromNominalPrice(product, ratesProvider, settlementDate, dirtyPrice);
        return cleanRealPriceFromDirtyRealPrice(product, settlementDate, dirtyRealPrice) - cleanPrice;
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
   * @param product  the product to price
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactorsProvider  the discount factors provider
   * @param presentValue  the present value
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the z-spread of the bond product
   */
  public double zSpreadFromCurvesAndPV(
      CapitalIndexedBond product,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      CurrencyAmount presentValue,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    validate(ratesProvider, issuerDiscountFactorsProvider);
    LocalDate settlementDate = product.getSettlementDateOffset().adjust(ratesProvider.getValuationDate());
    final Function<Double, Double> residual = new Function<Double, Double>() {
      @Override
      public Double apply(Double z) {
        return presentValueWithZSpread(product, ratesProvider, issuerDiscountFactorsProvider, settlementDate,
            z, compoundedRateType, periodsPerYear).getAmount() - presentValue.getAmount();
      }
    };
    double[] range = ROOT_BRACKETER.getBracketedPoints(residual, -0.5, 0.5); // Starting range is [-1%, 1%]
    return ROOT_FINDER.getRoot(residual, range[0], range[1]);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the accrued interest of the bond with the specified settlement date.
   * 
   * @param product  the product to price
   * @param settlementDate  the settlement date
   * @return the accrued interest of the product 
   */
  public double accruedInterest(CapitalIndexedBond product, LocalDate settlementDate) {
    Schedule scheduleAdjusted = product.getPeriodicSchedule().createSchedule();
    Schedule scheduleUnadjusted = scheduleAdjusted.toUnadjusted();
    if (scheduleUnadjusted.getPeriods().get(0).getStartDate().isAfter(settlementDate)) {
      return 0d;
    }
    double notional = product.getNotional();
    int couponIndex = couponIndex(scheduleUnadjusted, settlementDate);
    SchedulePeriod schedulePeriod = scheduleUnadjusted.getPeriod(couponIndex);
    LocalDate previousAccrualDate = schedulePeriod.getStartDate();
    LocalDate paymentDate = scheduleAdjusted.getPeriod(couponIndex).getEndDate();

    double realCoupon = product.getRateCalculation().getGearing().orElse(ValueSchedule.ALWAYS_1)
        .resolveValues(scheduleAdjusted.getPeriods()).get(couponIndex);
    double couponPerYear = product.getPeriodicSchedule().getFrequency().eventsPerYear();
    double accruedInterest = product.getDayCount()
        .yearFraction(previousAccrualDate, settlementDate, scheduleUnadjusted) * realCoupon * couponPerYear * notional;
    DaysAdjustment exCouponDays = product.getExCouponPeriod();
    double result = 0d;
    if (exCouponDays.getDays() != 0 && !settlementDate.isBefore(exCouponDays.adjust(paymentDate))) {
      result = accruedInterest - notional * realCoupon * couponPerYear *
          schedulePeriod.yearFraction(product.getDayCount(), scheduleUnadjusted);
    } else {
      result = accruedInterest;
    }
    return result;
  }

  //-------------------------------------------------------------------------
  private double ratioPeriodToNextCoupon(CapitalIndexedBondPaymentPeriod period, LocalDate settlementDate) {
    double nbDayToSpot = DAYS.between(settlementDate, period.getUnadjustedEndDate());
    double nbDaysPeriod = DAYS.between(period.getUnadjustedEndDate(), period.getUnadjustedStartDate());
    return nbDayToSpot / nbDaysPeriod;
  }

  private double factorToNextCoupon(Schedule scheduleUnadjusted, DayCount daycount, LocalDate settlementDate) {
    if (scheduleUnadjusted.getPeriod(0).getUnadjustedStartDate().isAfter(settlementDate)) {
      return 0d;
    }
    int couponIndex = couponIndex(scheduleUnadjusted, settlementDate);
    SchedulePeriod schedulePeriod = scheduleUnadjusted.getPeriod(couponIndex);
    LocalDate previousAccrualDate = schedulePeriod.getStartDate();
    double factorSpot = daycount.yearFraction(previousAccrualDate, settlementDate, scheduleUnadjusted);
    double factorPeriod = scheduleUnadjusted.getPeriod(couponIndex).yearFraction(daycount, scheduleUnadjusted);
    return (factorPeriod - factorSpot) / factorPeriod;
  }

  private int couponIndex(Schedule schedule, LocalDate date) {
    int nbCoupon = schedule.getPeriods().size();
    int couponIndex = 0;
    for (int loopcpn = 0; loopcpn < nbCoupon; ++loopcpn) {
      if (schedule.getPeriods().get(loopcpn).getUnadjustedEndDate().isAfter(date)) {
        couponIndex = loopcpn;
        break;
      }
    }
    return couponIndex;
  }

  double indexRatio(CapitalIndexedBond product, RatesProvider ratesProvider, LocalDate settlementDate) {
    LocalDate endReferenceDate = settlementDate.isBefore(ratesProvider.getValuationDate()) ?
        ratesProvider.getValuationDate() : settlementDate;
    RateObservation modifiedObservation =
        product.getRateCalculation().createRateObservation(endReferenceDate, product.getStartIndexValue());
    return 1d + periodPricer.getRateObservationFn().rate(
        modifiedObservation,
        product.getPeriodicSchedule().getStartDate(), // dates not used
        product.getPeriodicSchedule().getEndDate(),
        ratesProvider);
  }

  PointSensitivityBuilder indexRatioSensitivity(
      CapitalIndexedBond product,
      RatesProvider ratesProvider,
      LocalDate settlementDate) {
    LocalDate endReferenceDate = settlementDate.isBefore(ratesProvider.getValuationDate()) ?
        ratesProvider.getValuationDate() : settlementDate;
    RateObservation modifiedObservation =
        product.getRateCalculation().createRateObservation(endReferenceDate, product.getStartIndexValue());
    return periodPricer.getRateObservationFn().rateSensitivity(
        modifiedObservation,
        product.getPeriodicSchedule().getStartDate(), // dates not used
        product.getPeriodicSchedule().getEndDate(),
        ratesProvider);
  }

  private void validate(RatesProvider ratesProvider, LegalEntityDiscountingProvider issuerDiscountFactorsProvider) {
    ArgChecker.isTrue(ratesProvider.getValuationDate().isEqual(issuerDiscountFactorsProvider.getValuationDate()),
        "the rates providers should be for the same date");
  }

  //-------------------------------------------------------------------------
  // compute pv of coupon payment(s) s.t. referenceDate1 < coupon <= referenceDate2
  double presentValueCoupon(
      ExpandedCapitalIndexedBond product,
      RatesProvider ratesProvider,
      IssuerCurveDiscountFactors discountFactors,
      LocalDate referenceDate1,
      LocalDate referenceDate2,
      boolean exCoupon) {

    double pvDiff = 0d;
    for (CapitalIndexedBondPaymentPeriod period : product.getPeriodicPayments()) {
      if ((exCoupon && period.getDetachmentDate().isAfter(referenceDate1) && !period.getDetachmentDate().isAfter(referenceDate2)) ||
          (!exCoupon && period.getPaymentDate().isAfter(referenceDate1) && !period.getPaymentDate().isAfter(referenceDate2))) {
        pvDiff += periodPricer.presentValue(period, ratesProvider, discountFactors);
      }
    }
    return pvDiff;
  }

  // compute pv of coupon payment(s) s.t. referenceDate1 < coupon <= referenceDate2
  double presentValueCouponWithZSpread(
      ExpandedCapitalIndexedBond expanded,
      RatesProvider ratesProvider,
      IssuerCurveDiscountFactors discountFactors,
      LocalDate referenceDate1,
      LocalDate referenceDate2,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear,
      boolean exCoupon) {

    double pvDiff = 0d;
    for (CapitalIndexedBondPaymentPeriod period : expanded.getPeriodicPayments()) {
      if ((exCoupon && period.getDetachmentDate().isAfter(referenceDate1) && !period.getDetachmentDate().isAfter(referenceDate2)) ||
          (!exCoupon && period.getPaymentDate().isAfter(referenceDate1) && !period.getPaymentDate().isAfter(referenceDate2))) {
        pvDiff += periodPricer.presentValueWithZSpread(
            period, ratesProvider, discountFactors, zSpread, compoundedRateType, periodsPerYear);
      }
    }
    return pvDiff;
  }

  PointSensitivityBuilder presentValueSensitivityCoupon(
      ExpandedCapitalIndexedBond product,
      RatesProvider ratesProvider,
      IssuerCurveDiscountFactors discountFactors,
      LocalDate referenceDate1,
      LocalDate referenceDate2,
      boolean exCoupon) {

    PointSensitivityBuilder pvSensiDiff = PointSensitivityBuilder.none();
    for (CapitalIndexedBondPaymentPeriod period : product.getPeriodicPayments()) {
      if ((exCoupon && period.getDetachmentDate().isAfter(referenceDate1) && !period.getDetachmentDate().isAfter(referenceDate2)) ||
          (!exCoupon && period.getPaymentDate().isAfter(referenceDate1) && !period.getPaymentDate().isAfter(referenceDate2))) {
        pvSensiDiff = pvSensiDiff.combinedWith(periodPricer.presentValueSensitivity(period, ratesProvider, discountFactors));
      }
    }
    return pvSensiDiff;
  }

  // compute pv sensitivity of coupon payment(s) s.t. referenceDate1 < coupon <= referenceDate2
  PointSensitivityBuilder presentValueSensitivityCouponWithZSpread(
      ExpandedCapitalIndexedBond expanded,
      RatesProvider ratesProvider,
      IssuerCurveDiscountFactors discountFactors,
      LocalDate referenceDate1,
      LocalDate referenceDate2,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear,
      boolean exCoupon) {

    PointSensitivityBuilder pvSensiDiff = PointSensitivityBuilder.none();
    for (CapitalIndexedBondPaymentPeriod period : expanded.getPeriodicPayments()) {
      if ((exCoupon && period.getDetachmentDate().isAfter(referenceDate1) && !period.getDetachmentDate().isAfter(referenceDate2)) ||
          (!exCoupon && period.getPaymentDate().isAfter(referenceDate1) && !period.getPaymentDate().isAfter(referenceDate2))) {
        pvSensiDiff = pvSensiDiff.combinedWith(periodPricer.presentValueSensitivityWithZSpread(
            period, ratesProvider, discountFactors, zSpread, compoundedRateType, periodsPerYear));
      }
    }
    return pvSensiDiff;
  }

}
