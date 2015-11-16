/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import java.time.LocalDate;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.market.sensitivity.IssuerCurveZeroRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.RepoCurveZeroRateSensitivity;
import com.opengamma.strata.market.sensitivity.ZeroRateSensitivity;
import com.opengamma.strata.market.value.CompoundedRateType;
import com.opengamma.strata.market.value.IssuerCurveDiscountFactors;
import com.opengamma.strata.market.value.RepoCurveDiscountFactors;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.rootfinding.BracketRoot;
import com.opengamma.strata.math.impl.rootfinding.BrentSingleRootFinder;
import com.opengamma.strata.math.impl.rootfinding.RealSingleRootFinder;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.impl.bond.DiscountingFixedCouponBondPaymentPeriodPricer;
import com.opengamma.strata.pricer.rate.LegalEntityDiscountingProvider;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.bond.ExpandedFixedCouponBond;
import com.opengamma.strata.product.bond.FixedCouponBond;
import com.opengamma.strata.product.bond.FixedCouponBondPaymentPeriod;
import com.opengamma.strata.product.bond.YieldConvention;

/**
 * Pricer for for rate fixed coupon bond products.
 * <p>
 * This function provides the ability to price a {@link FixedCouponBond}.
 */
public class DiscountingFixedCouponBondProductPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingFixedCouponBondProductPricer DEFAULT = new DiscountingFixedCouponBondProductPricer(
      DiscountingFixedCouponBondPaymentPeriodPricer.DEFAULT,
      DiscountingPaymentPricer.DEFAULT);

  /**
   * The root finder.
   */
  private static final RealSingleRootFinder ROOT_FINDER = new BrentSingleRootFinder();
  /**
   * Brackets a root.
   */
  private static final BracketRoot ROOT_BRACKETER = new BracketRoot();

  /**
   * Pricer for {@link Payment}.
   */
  private final DiscountingPaymentPricer nominalPricer;
  /**
   * Pricer for {@link FixedCouponBondPaymentPeriod}.
   */
  private final DiscountingFixedCouponBondPaymentPeriodPricer periodPricer;

  /**
   * Creates an instance.
   * 
   * @param periodPricer  the pricer for {@link FixedCouponBondPaymentPeriod}
   * @param nominalPricer  the pricer for {@link Payment}
   */
  public DiscountingFixedCouponBondProductPricer(
      DiscountingFixedCouponBondPaymentPeriodPricer periodPricer,
      DiscountingPaymentPricer nominalPricer) {

    this.nominalPricer = ArgChecker.notNull(nominalPricer, "nominalPricer");
    this.periodPricer = ArgChecker.notNull(periodPricer, "periodPricer");
  }

  //-------------------------------------------------------------------------

  /**
   * Calculates the present value of the fixed coupon bond product.
   * <p>
   * The present value of the product is the value on the valuation date.
   * The result is expressed using the payment currency of the bond.
   * <p>
   * Coupon payments of the product are considered based on the valuation date. 
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the present value of the fixed coupon bond product
   */
  public CurrencyAmount presentValue(FixedCouponBond product, LegalEntityDiscountingProvider provider) {
    return presentValue(product, provider, provider.getValuationDate());
  }

  // calculate the present value
  CurrencyAmount presentValue(
      FixedCouponBond product,
      LegalEntityDiscountingProvider provider,
      LocalDate referenceDate) {

    ExpandedFixedCouponBond expanded = product.expand();
    IssuerCurveDiscountFactors discountFactors = provider.issuerCurveDiscountFactors(
        product.getLegalEntityId(), product.getCurrency());
    CurrencyAmount pvNominal =
        nominalPricer.presentValue(expanded.getNominalPayment(), discountFactors.getDiscountFactors());
    CurrencyAmount pvCoupon =
        presentValueCoupon(expanded, discountFactors, referenceDate, product.getExCouponPeriod().getDays() != 0);
    return pvNominal.plus(pvCoupon);
  }

  /**
   * Calculates the present value of the fixed coupon bond product with z-spread. 
   * <p>
   * The present value of the product is the value on the valuation date.
   * The result is expressed using the payment currency of the bond.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or
   * periodic compounded rates of the issuer discounting curve. 
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the present value of the fixed coupon bond product
   */
  public CurrencyAmount presentValueWithZSpread(
      FixedCouponBond product,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    return presentValueWithZSpread(product, provider, zSpread, compoundedRateType, periodsPerYear, provider.getValuationDate());
  }

  // calculate the present value
  CurrencyAmount presentValueWithZSpread(
      FixedCouponBond product,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear,
      LocalDate referenceDate) {

    ExpandedFixedCouponBond expanded = product.expand();
    IssuerCurveDiscountFactors discountFactors = provider.issuerCurveDiscountFactors(
        product.getLegalEntityId(), product.getCurrency());
    CurrencyAmount pvNominal = nominalPricer.presentValue(
        expanded.getNominalPayment(), discountFactors.getDiscountFactors(), zSpread, compoundedRateType, periodsPerYear);
    boolean isExCoupon = product.getExCouponPeriod().getDays() != 0;
    CurrencyAmount pvCoupon = presentValueCouponFromZSpread(
        expanded, discountFactors, zSpread, compoundedRateType, periodsPerYear, referenceDate, isExCoupon);
    return pvNominal.plus(pvCoupon);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the dirty price of the fixed coupon bond.
   * <p>
   * The fixed coupon bond is represented as {@link Security} where standard ID of the bond is stored.
   * 
   * @param security  the security to price
   * @param provider  the rates provider
   * @return the dirty price of the fixed coupon bond security
   */
  public double dirtyPriceFromCurves(Security<FixedCouponBond> security, LegalEntityDiscountingProvider provider) {
    FixedCouponBond product = security.getProduct();
    LocalDate settlementDate = product.getSettlementDateOffset().adjust(provider.getValuationDate());
    return dirtyPriceFromCurves(security, provider, settlementDate);
  }

  /**
   * Calculates the dirty price of the fixed coupon bond under the specified settlement date.
   * <p>
   * The fixed coupon bond is represented as {@link Security} where standard ID of the bond is stored.
   * 
   * @param security  the security to price
   * @param provider  the rates provider
   * @param settlementDate  the settlement date
   * @return the dirty price of the fixed coupon bond security
   */
  public double dirtyPriceFromCurves(
      Security<FixedCouponBond> security,
      LegalEntityDiscountingProvider provider,
      LocalDate settlementDate) {

    FixedCouponBond product = security.getProduct();
    CurrencyAmount pv = presentValue(product, provider, settlementDate);
    StandardId securityId = security.getStandardId();
    StandardId legalEntityId = product.getLegalEntityId();
    double df = provider.repoCurveDiscountFactors(
        securityId, legalEntityId, product.getCurrency()).discountFactor(settlementDate);
    double notional = product.getNotional();
    return pv.getAmount() / df / notional;
  }

  /**
   * Calculates the dirty price of the fixed coupon bond with z-spread.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic
   * compounded rates of the discounting curve.
   * <p>
   * The fixed coupon bond is represented as {@link Security} where standard ID of the bond is stored.
   * 
   * @param security  the security to price
   * @param provider  the rates provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the dirty price of the fixed coupon bond security
   */
  public double dirtyPriceFromCurvesWithZSpread(
      Security<FixedCouponBond> security,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    FixedCouponBond product = security.getProduct();
    LocalDate settlementDate = product.getSettlementDateOffset().adjust(provider.getValuationDate());
    return dirtyPriceFromCurvesWithZSpread(security, provider, zSpread, compoundedRateType, periodsPerYear, settlementDate);
  }

  /**
   * Calculates the dirty price of the fixed coupon bond under the specified settlement date with z-spread.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic
   * compounded rates of the discounting curve.
   * <p>
   * The fixed coupon bond is represented as {@link Security} where standard ID of the bond is stored.
   * 
   * @param security  the security to price
   * @param provider  the rates provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @param settlementDate  the settlement date
   * @return the dirty price of the fixed coupon bond security
   */
  public double dirtyPriceFromCurvesWithZSpread(
      Security<FixedCouponBond> security,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear,
      LocalDate settlementDate) {

    FixedCouponBond product = security.getProduct();
    CurrencyAmount pv = presentValueWithZSpread(product, provider, zSpread, compoundedRateType, periodsPerYear, settlementDate);
    StandardId securityId = security.getStandardId();
    StandardId legalEntityId = product.getLegalEntityId();
    double df = provider.repoCurveDiscountFactors(
        securityId, legalEntityId, product.getCurrency()).discountFactor(settlementDate);
    double notional = product.getNotional();
    return pv.getAmount() / df / notional;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the dirty price of the fixed coupon bond from its settlement date and clean price.
   * 
   * @param product  the product to price
   * @param settlementDate  the settlement date
   * @param cleanPrice  the clean price
   * @return the present value of the fixed coupon bond product
   */
  public double dirtyPriceFromCleanPrice(FixedCouponBond product, LocalDate settlementDate, double cleanPrice) {
    double notional = product.getNotional();
    double accruedInterest = accruedInterest(product, settlementDate);
    return cleanPrice + accruedInterest / notional;
  }

  /**
   * Calculates the clean price of the fixed coupon bond from its settlement date and dirty price.
   * 
   * @param product  the product to price
   * @param settlementDate  the settlement date
   * @param dirtyPrice  the dirty price
   * @return the present value of the fixed coupon bond product
   */
  public double cleanPriceFromDirtyPrice(FixedCouponBond product, LocalDate settlementDate, double dirtyPrice) {
    double notional = product.getNotional();
    double accruedInterest = accruedInterest(product, settlementDate);
    return dirtyPrice - accruedInterest / notional;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the z-spread of the fixed coupon bond from curves and dirty price.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic
   * compounded rates of the discounting curve associated to the bond (Issuer Entity)
   * to match the dirty price.
   * 
   * @param security  the security to price
   * @param provider  the rates provider
   * @param dirtyPrice  the dirtyPrice
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the z-spread of the fixed coupon bond security
   */
  public double zSpreadFromCurvesAndDirtyPrice(
      Security<FixedCouponBond> security,
      LegalEntityDiscountingProvider provider,
      double dirtyPrice,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    final Function1D<Double, Double> residual = new Function1D<Double, Double>() {
      @Override
      public Double evaluate(final Double z) {
        return dirtyPriceFromCurvesWithZSpread(security, provider, z, compoundedRateType, periodsPerYear) - dirtyPrice;
      }
    };
    double[] range = ROOT_BRACKETER.getBracketedPoints(residual, -0.01, 0.01); // Starting range is [-1%, 1%]
    return ROOT_FINDER.getRoot(residual, range[0], range[1]);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the fixed coupon bond product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the present value curve sensitivity of the product
   */
  public PointSensitivityBuilder presentValueSensitivity(
      FixedCouponBond product,
      LegalEntityDiscountingProvider provider) {

    return presentValueSensitivity(product, provider, provider.getValuationDate());
  }

  // calculate the present value sensitivity
  PointSensitivityBuilder presentValueSensitivity(
      FixedCouponBond product,
      LegalEntityDiscountingProvider provider,
      LocalDate referenceDate) {

    ExpandedFixedCouponBond expanded = product.expand();
    IssuerCurveDiscountFactors discountFactors = provider.issuerCurveDiscountFactors(
        product.getLegalEntityId(), product.getCurrency());
    PointSensitivityBuilder pvNominal = presentValueSensitivityNominal(expanded, discountFactors);
    PointSensitivityBuilder pvCoupon = presentValueSensitivityCoupon(
        expanded, discountFactors, referenceDate, product.getExCouponPeriod().getDays() != 0);
    return pvNominal.combinedWith(pvCoupon);
  }

  /**
   * Calculates the present value sensitivity of the fixed coupon bond with z-spread.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or
   * periodic compounded rates of the issuer discounting curve. 
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the present value curve sensitivity of the product
   */
  public PointSensitivityBuilder presentValueSensitivityWithZSpread(
      FixedCouponBond product,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    return presentValueSensitivityWithZSpread(
        product, provider, zSpread, compoundedRateType, periodsPerYear, provider.getValuationDate());
  }

  // calculate the present value sensitivity
  PointSensitivityBuilder presentValueSensitivityWithZSpread(
      FixedCouponBond product,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear,
      LocalDate referenceDate) {

    ExpandedFixedCouponBond expanded = product.expand();
    IssuerCurveDiscountFactors discountFactors = provider.issuerCurveDiscountFactors(
        product.getLegalEntityId(), product.getCurrency());
    PointSensitivityBuilder pvNominal = presentValueSensitivityNominalFromZSpread(
        expanded, discountFactors, zSpread, compoundedRateType, periodsPerYear);
    boolean isExCoupon = product.getExCouponPeriod().getDays() != 0;
    PointSensitivityBuilder pvCoupon = presentValueSensitivityCouponFromZSpread(
        expanded, discountFactors, zSpread, compoundedRateType, periodsPerYear, referenceDate, isExCoupon);
    return pvNominal.combinedWith(pvCoupon);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the dirty price sensitivity of the fixed coupon bond product.
   * <p>
   * The dirty price sensitivity of the security is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param security  the security to price
   * @param provider  the rates provider
   * @return the dirty price value curve sensitivity of the security
   */
  public PointSensitivityBuilder dirtyPriceSensitivity(
      Security<FixedCouponBond> security,
      LegalEntityDiscountingProvider provider) {
    FixedCouponBond product = security.getProduct();
    LocalDate settlementDate = product.getSettlementDateOffset().adjust(provider.getValuationDate());
    return dirtyPriceSensitivity(security, provider, settlementDate);
  }

  // calculate the dirty price sensitivity
  PointSensitivityBuilder dirtyPriceSensitivity(
      Security<FixedCouponBond> security,
      LegalEntityDiscountingProvider provider,
      LocalDate referenceDate) {

    FixedCouponBond product = security.getProduct();
    StandardId securityId = security.getStandardId();
    StandardId legalEntityId = product.getLegalEntityId();
    RepoCurveDiscountFactors discountFactors =
        provider.repoCurveDiscountFactors(securityId, legalEntityId, product.getCurrency());
    double df = discountFactors.discountFactor(referenceDate);
    CurrencyAmount pv = presentValue(product, provider);
    double notional = product.getNotional();
    PointSensitivityBuilder pvSensi = presentValueSensitivity(product, provider).multipliedBy(1d / df / notional);
    RepoCurveZeroRateSensitivity dfSensi = discountFactors.zeroRatePointSensitivity(referenceDate)
        .multipliedBy(-pv.getAmount() / df / df / notional);
    return pvSensi.combinedWith(dfSensi);
  }

  /**
   * Calculates the dirty price sensitivity of the fixed coupon bond with z-spread.
   * <p>
   * The dirty price sensitivity of the security is the sensitivity of the present value to
   * the underlying curves.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic
   * compounded rates of the discounting curve.
   * 
   * @param security  the security to price
   * @param provider  the rates provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the dirty price curve sensitivity of the security
   */
  public PointSensitivityBuilder dirtyPriceSensitivityWithZspread(
      Security<FixedCouponBond> security,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    FixedCouponBond product = security.getProduct();
    LocalDate settlementDate = product.getSettlementDateOffset().adjust(provider.getValuationDate());
    return dirtyPriceSensitivityWithZspread(security, provider, zSpread, compoundedRateType, periodsPerYear, settlementDate);
  }

  // calculate the dirty price sensitivity
  PointSensitivityBuilder dirtyPriceSensitivityWithZspread(
      Security<FixedCouponBond> security,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear,
      LocalDate referenceDate) {

    FixedCouponBond product = security.getProduct();
    StandardId securityId = security.getStandardId();
    StandardId legalEntityId = product.getLegalEntityId();
    RepoCurveDiscountFactors discountFactors =
        provider.repoCurveDiscountFactors(securityId, legalEntityId, product.getCurrency());
    double df = discountFactors.discountFactor(referenceDate);
    CurrencyAmount pv = presentValueWithZSpread(product, provider, zSpread, compoundedRateType, periodsPerYear);
    double notional = product.getNotional();
    PointSensitivityBuilder pvSensi = presentValueSensitivityWithZSpread(
        product, provider, zSpread, compoundedRateType, periodsPerYear).multipliedBy(1d / df / notional);
    RepoCurveZeroRateSensitivity dfSensi = discountFactors.zeroRatePointSensitivity(referenceDate)
        .multipliedBy(-pv.getAmount() / df / df / notional);
    return pvSensi.combinedWith(dfSensi);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the accrued interest of the fixed coupon bond with the specified settlement date.
   * 
   * @param product  the product to price
   * @param settlementDate  the settlement date
   * @return the accrued interest of the product 
   */
  public double accruedInterest(FixedCouponBond product, LocalDate settlementDate) {
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
    double fixedRate = product.getFixedRate();
    double accruedInterest = product.getDayCount()
        .yearFraction(previousAccrualDate, settlementDate, scheduleUnadjusted) * fixedRate * notional;
    DaysAdjustment exCouponDays = product.getExCouponPeriod();
    double result = 0d;
    if (exCouponDays.getDays() != 0 && settlementDate.isAfter(exCouponDays.adjust(paymentDate))) {
      result = accruedInterest - notional * fixedRate *
          schedulePeriod.yearFraction(product.getDayCount(), scheduleUnadjusted);
    } else {
      result = accruedInterest;
    }
    return result;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the dirty price of the fixed coupon bond from yield.
   * <p>
   * The yield must be fractional.
   * The dirty price is computed for {@link YieldConvention}, and the result is expressed in fraction. 
   * 
   * @param product  the product to price
   * @param settlementDate  the settlement date
   * @param yield  the yield
   * @return the dirty price of the product 
   */
  public double dirtyPriceFromYield(FixedCouponBond product, LocalDate settlementDate, double yield) {
    ExpandedFixedCouponBond expanded = product.expand();
    ImmutableList<FixedCouponBondPaymentPeriod> payments = expanded.getPeriodicPayments();
    int nCoupon = payments.size() - couponIndex(payments, settlementDate);
    YieldConvention yieldConvention = product.getYieldConvention();
    if (nCoupon == 1) {
      if (yieldConvention.equals(YieldConvention.US_STREET) || yieldConvention.equals(YieldConvention.GERMAN_BONDS)) {
        FixedCouponBondPaymentPeriod payment = payments.get(payments.size() - 1);
        return (1d + payment.getFixedRate() * payment.getYearFraction()) /
            (1d + factorToNextCoupon(product, expanded, settlementDate) * yield /
                ((double) product.getPeriodicSchedule().getFrequency().eventsPerYear()));
      }
    }
    if ((yieldConvention.equals(YieldConvention.US_STREET)) ||
        (yieldConvention.equals(YieldConvention.UK_BUMP_DMO)) ||
        (yieldConvention.equals(YieldConvention.GERMAN_BONDS))) {
      return dirtyPriceFromYieldStandard(product, expanded, settlementDate, yield);
    }
    if (yieldConvention.equals(YieldConvention.JAPAN_SIMPLE)) {
      LocalDate maturityDate = product.getPeriodicSchedule().getEndDate();
      if (settlementDate.isAfter(maturityDate)) {
        return 0d;
      }
      double maturity = product.getDayCount().relativeYearFraction(settlementDate, maturityDate);
      double cleanPrice = (1d + product.getFixedRate() * maturity) / (1d + yield * maturity);
      return dirtyPriceFromCleanPrice(product, settlementDate, cleanPrice);
    }
    throw new UnsupportedOperationException("The convention " + yieldConvention.name() + " is not supported.");
  }

  private double dirtyPriceFromYieldStandard(
      FixedCouponBond product,
      ExpandedFixedCouponBond expanded,
      LocalDate settlementDate,
      double yield) {

    int nbCoupon = expanded.getPeriodicPayments().size();
    double factorOnPeriod = 1 + yield / ((double) product.getPeriodicSchedule().getFrequency().eventsPerYear());
    double fixedRate = product.getFixedRate();
    double pvAtFirstCoupon = 0;
    int pow = 0;
    for (int loopcpn = 0; loopcpn < nbCoupon; loopcpn++) {
      FixedCouponBondPaymentPeriod payment = expanded.getPeriodicPayments().get(loopcpn);
      if ((product.getExCouponPeriod().getDays() != 0 && !settlementDate.isAfter(payment.getDetachmentDate())) ||
          (product.getExCouponPeriod().getDays() == 0 && payment.getPaymentDate().isAfter(settlementDate))) {
        pvAtFirstCoupon += fixedRate * payment.getYearFraction() / Math.pow(factorOnPeriod, pow);
        ++pow;
      }
    }
    pvAtFirstCoupon += 1d / Math.pow(factorOnPeriod, pow - 1);
    return pvAtFirstCoupon * Math.pow(factorOnPeriod, -factorToNextCoupon(product, expanded, settlementDate));
  }

  /**
   * Calculates the yield of the fixed coupon bond product from dirty price.
   * <p>
   * The dirty price must be fractional. 
   * If the analytic formula is not available, the yield is computed by solving
   * a root-finding problem with {@link #dirtyPriceFromYield(FixedCouponBond, LocalDate, double)}.  
   * The result is also expressed in fraction. 
   * 
   * @param product  the product to price
   * @param settlementDate  the settlement date
   * @param dirtyPrice  the dirty price
   * @return the yield of the product 
   */
  public double yieldFromDirtyPrice(FixedCouponBond product, LocalDate settlementDate, double dirtyPrice) {
    if (product.getYieldConvention().equals(YieldConvention.JAPAN_SIMPLE)) {
      double cleanPrice = cleanPriceFromDirtyPrice(product, settlementDate, dirtyPrice);
      LocalDate maturityDate = product.getPeriodicSchedule().getEndDate();
      double maturity = product.getDayCount().relativeYearFraction(settlementDate, maturityDate);
      return (product.getFixedRate() + (1d - cleanPrice) / maturity) / cleanPrice;
    }

    final Function1D<Double, Double> priceResidual = new Function1D<Double, Double>() {
      @Override
      public Double evaluate(final Double y) {
        return dirtyPriceFromYield(product, settlementDate, y) - dirtyPrice;
      }
    };
    double[] range = ROOT_BRACKETER.getBracketedPoints(priceResidual, 0.00, 0.20);
    double yield = ROOT_FINDER.getRoot(priceResidual, range[0], range[1]);
    return yield;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the modified duration of the fixed coupon bond product from yield.
   * <p>
   * The modified duration is defined as the minus of the first derivative of dirty price
   * with respect to yield, divided by the dirty price. 
   * <p>
   * The input yield must be fractional. The dirty price and its derivative are
   * computed for {@link YieldConvention}, and the result is expressed in fraction. 
   * 
   * @param product  the product to price
   * @param settlementDate  the settlement date
   * @param yield  the yield
   * @return the modified duration of the product 
   */
  public double modifiedDurationFromYield(FixedCouponBond product, LocalDate settlementDate, double yield) {
    ExpandedFixedCouponBond expanded = product.expand();
    ImmutableList<FixedCouponBondPaymentPeriod> payments = expanded.getPeriodicPayments();
    int nCoupon = payments.size() - couponIndex(payments, settlementDate);
    YieldConvention yieldConvention = product.getYieldConvention();
    if (nCoupon == 1) {
      if (yieldConvention.equals(YieldConvention.US_STREET) || yieldConvention.equals(YieldConvention.GERMAN_BONDS)) {
        double couponPerYear = product.getPeriodicSchedule().getFrequency().eventsPerYear();
        double factor = factorToNextCoupon(product, expanded, settlementDate);
        return factor / couponPerYear / (1d + factor * yield / couponPerYear);
      }
    }
    if (yieldConvention.equals(YieldConvention.US_STREET) ||
        yieldConvention.equals(YieldConvention.UK_BUMP_DMO) ||
        yieldConvention.equals(YieldConvention.GERMAN_BONDS)) {
      return modifiedDurationFromYieldStandard(product, expanded, settlementDate, yield);
    }
    if (yieldConvention.equals(YieldConvention.JAPAN_SIMPLE)) {
      LocalDate maturityDate = product.getPeriodicSchedule().getEndDate();
      if (settlementDate.isAfter(maturityDate)) {
        return 0d;
      }
      double maturity = product.getDayCount().relativeYearFraction(settlementDate, maturityDate);
      double num = 1d + product.getFixedRate() * maturity;
      double den = 1d + yield * maturity;
      double dirtyPrice = dirtyPriceFromCleanPrice(product, settlementDate, num / den);
      return num * maturity / den / den / dirtyPrice;
    }
    throw new UnsupportedOperationException("The convention " + yieldConvention.name() + " is not supported.");
  }

  private double modifiedDurationFromYieldStandard(FixedCouponBond product, ExpandedFixedCouponBond expanded,
      LocalDate settlementDate, double yield) {
    int nbCoupon = expanded.getPeriodicPayments().size();
    double couponPerYear = product.getPeriodicSchedule().getFrequency().eventsPerYear();
    double factorToNextCoupon = factorToNextCoupon(product, expanded, settlementDate);
    double factorOnPeriod = 1 + yield / couponPerYear;
    double nominal = product.getNotional();
    double fixedRate = product.getFixedRate();
    double mdAtFirstCoupon = 0d;
    double pvAtFirstCoupon = 0d;
    int pow = 0;
    for (int loopcpn = 0; loopcpn < nbCoupon; loopcpn++) {
      FixedCouponBondPaymentPeriod payment = expanded.getPeriodicPayments().get(loopcpn);
      if ((product.getExCouponPeriod().getDays() != 0 && !settlementDate.isAfter(payment.getDetachmentDate())) ||
          (product.getExCouponPeriod().getDays() == 0 && payment.getPaymentDate().isAfter(settlementDate))) {
        mdAtFirstCoupon += payment.getYearFraction() / Math.pow(factorOnPeriod, pow + 1) *
            (pow + factorToNextCoupon) / couponPerYear;
        pvAtFirstCoupon += payment.getYearFraction() / Math.pow(factorOnPeriod, pow);
        ++pow;
      }
    }
    mdAtFirstCoupon *= fixedRate * nominal;
    pvAtFirstCoupon *= fixedRate * nominal;
    mdAtFirstCoupon += nominal / Math.pow(factorOnPeriod, pow) * (pow - 1 + factorToNextCoupon) /
        couponPerYear;
    pvAtFirstCoupon += nominal / Math.pow(factorOnPeriod, pow - 1);
    double md = mdAtFirstCoupon / pvAtFirstCoupon;
    return md;
  }

  /**
   * Calculates the Macaulay duration of the fixed coupon bond product from yield.
   * <p>
   * Macaulay defined an alternative way of weighting the future cash flows. 
   * <p>
   * The input yield must be fractional. The dirty price and its derivative are
   * computed for {@link YieldConvention}, and the result is expressed in fraction. 
   * 
   * @param product  the product to price
   * @param settlementDate  the settlement date
   * @param yield  the yield
   * @return the modified duration of the product 
   */
  public double macaulayDurationFromYield(FixedCouponBond product, LocalDate settlementDate, double yield) {
    ExpandedFixedCouponBond expanded = product.expand();
    ImmutableList<FixedCouponBondPaymentPeriod> payments = expanded.getPeriodicPayments();
    int nCoupon = payments.size() - couponIndex(payments, settlementDate);
    YieldConvention yieldConvention = product.getYieldConvention();
    if ((yieldConvention.equals(YieldConvention.US_STREET)) && (nCoupon == 1)) {
      return factorToNextCoupon(product, expanded, settlementDate) /
          product.getPeriodicSchedule().getFrequency().eventsPerYear();
    }
    if ((yieldConvention.equals(YieldConvention.US_STREET)) ||
        (yieldConvention.equals(YieldConvention.UK_BUMP_DMO)) ||
        (yieldConvention.equals(YieldConvention.GERMAN_BONDS))) {
      return modifiedDurationFromYield(product, settlementDate, yield) *
          (1d + yield / product.getPeriodicSchedule().getFrequency().eventsPerYear());
    }
    throw new UnsupportedOperationException("The convention " + yieldConvention.name() + " is not supported.");
  }

  /**
   * Calculates the convexity of the fixed coupon bond product from yield.
   * <p>
   * The convexity is defined as the second derivative of dirty price with respect
   * to yield, divided by the dirty price. 
   * <p>
   * The input yield must be fractional. The dirty price and its derivative are
   * computed for {@link YieldConvention}, and the result is expressed in fraction. 
   * 
   * @param product  the product to price
   * @param settlementDate  the settlement date
   * @param yield  the yield
   * @return the convexity of the product 
   */
  public double convexityFromYield(FixedCouponBond product, LocalDate settlementDate, double yield) {
    ExpandedFixedCouponBond expanded = product.expand();
    ImmutableList<FixedCouponBondPaymentPeriod> payments = expanded.getPeriodicPayments();
    int nCoupon = payments.size() - couponIndex(payments, settlementDate);
    YieldConvention yieldConvention = product.getYieldConvention();
    if (nCoupon == 1) {
      if (yieldConvention.equals(YieldConvention.US_STREET) || yieldConvention.equals(YieldConvention.GERMAN_BONDS)) {
        double couponPerYear = product.getPeriodicSchedule().getFrequency().eventsPerYear();
        double factorToNextCoupon = factorToNextCoupon(product, expanded, settlementDate);
        double timeToPay = factorToNextCoupon / couponPerYear;
        double disc = (1d + factorToNextCoupon * yield / couponPerYear);
        return 2d * timeToPay * timeToPay / (disc * disc);
      }
    }
    if (yieldConvention.equals(YieldConvention.US_STREET) || yieldConvention.equals(YieldConvention.UK_BUMP_DMO) ||
        yieldConvention.equals(YieldConvention.GERMAN_BONDS)) {
      return convexityFromYieldStandard(product, expanded, settlementDate, yield);
    }
    if (yieldConvention.equals(YieldConvention.JAPAN_SIMPLE)) {
      LocalDate maturityDate = product.getPeriodicSchedule().getEndDate();
      if (settlementDate.isAfter(maturityDate)) {
        return 0d;
      }
      double maturity = product.getDayCount().relativeYearFraction(settlementDate, maturityDate);
      double num = 1d + product.getFixedRate() * maturity;
      double den = 1d + yield * maturity;
      double dirtyPrice = dirtyPriceFromCleanPrice(product, settlementDate, num / den);
      return 2d * num * Math.pow(maturity, 2) * Math.pow(den, -3) / dirtyPrice;
    }
    throw new UnsupportedOperationException("The convention " + yieldConvention.name() + " is not supported.");
  }

  // assumes notional and coupon rate are constant across the payments. 
  private double convexityFromYieldStandard(
      FixedCouponBond product,
      ExpandedFixedCouponBond expanded,
      LocalDate settlementDate,
      double yield) {

    int nbCoupon = expanded.getPeriodicPayments().size();
    double couponPerYear = product.getPeriodicSchedule().getFrequency().eventsPerYear();
    double factorToNextCoupon = factorToNextCoupon(product, expanded, settlementDate);
    double factorOnPeriod = 1 + yield / couponPerYear;
    double nominal = product.getNotional();
    double fixedRate = product.getFixedRate();
    double cvAtFirstCoupon = 0;
    double pvAtFirstCoupon = 0;
    int pow = 0;
    for (int loopcpn = 0; loopcpn < nbCoupon; loopcpn++) {
      FixedCouponBondPaymentPeriod payment = expanded.getPeriodicPayments().get(loopcpn);
      if ((product.getExCouponPeriod().getDays() != 0 && !settlementDate.isAfter(payment.getDetachmentDate())) ||
          (product.getExCouponPeriod().getDays() == 0 && payment.getPaymentDate().isAfter(settlementDate))) {
        cvAtFirstCoupon += payment.getYearFraction() / Math.pow(factorOnPeriod, pow + 2) *
            (pow + factorToNextCoupon) * (pow + factorToNextCoupon + 1);
        pvAtFirstCoupon += payment.getYearFraction() / Math.pow(factorOnPeriod, pow);
        ++pow;
      }
    }
    cvAtFirstCoupon *= fixedRate * nominal / (couponPerYear * couponPerYear);
    pvAtFirstCoupon *= fixedRate * nominal;
    cvAtFirstCoupon += nominal / Math.pow(factorOnPeriod, pow + 1) * (pow - 1 + factorToNextCoupon) *
        (pow + factorToNextCoupon) / (couponPerYear * couponPerYear);
    pvAtFirstCoupon += nominal / Math.pow(factorOnPeriod, pow - 1);
    final double pv = pvAtFirstCoupon * Math.pow(factorOnPeriod, -factorToNextCoupon);
    final double cv = cvAtFirstCoupon * Math.pow(factorOnPeriod, -factorToNextCoupon) / pv;
    return cv;
  }

  //-------------------------------------------------------------------------
  private double factorToNextCoupon(FixedCouponBond product, ExpandedFixedCouponBond expanded, LocalDate settlementDate) {
    if (expanded.getPeriodicPayments().get(0).getStartDate().isAfter(settlementDate)) {
      return 0d;
    }
    int couponIndex = couponIndex(expanded.getPeriodicPayments(), settlementDate);
    double factorSpot = accruedInterest(product, settlementDate) / product.getFixedRate() / product.getNotional();
    double factorPeriod = expanded.getPeriodicPayments().get(couponIndex).getYearFraction();
    return (factorPeriod - factorSpot) / factorPeriod;
  }

  private int couponIndex(Schedule schedule, LocalDate date) {
    int nbCoupon = schedule.getPeriods().size();
    int couponIndex = 0;
    for (int loopcpn = 0; loopcpn < nbCoupon; ++loopcpn) {
      if (schedule.getPeriods().get(loopcpn).getEndDate().isAfter(date)) {
        couponIndex = loopcpn;
        break;
      }
    }
    return couponIndex;
  }

  private int couponIndex(ImmutableList<FixedCouponBondPaymentPeriod> list, LocalDate date) {
    int nbCoupon = list.size();
    int couponIndex = 0;
    for (int loopcpn = 0; loopcpn < nbCoupon; ++loopcpn) {
      if (list.get(loopcpn).getEndDate().isAfter(date)) {
        couponIndex = loopcpn;
        break;
      }
    }
    return couponIndex;
  }

  //-------------------------------------------------------------------------
  private CurrencyAmount presentValueCoupon(
      ExpandedFixedCouponBond product,
      IssuerCurveDiscountFactors discountFactors,
      LocalDate referenceDate,
      boolean exCoupon) {

    double total = 0d;
    for (FixedCouponBondPaymentPeriod period : product.getPeriodicPayments()) {
      if ((exCoupon && period.getDetachmentDate().isAfter(referenceDate)) ||
          (!exCoupon && period.getPaymentDate().isAfter(referenceDate))) {
        total += periodPricer.presentValue(period, discountFactors);
      }
    }
    return CurrencyAmount.of(product.getCurrency(), total);
  }

  private CurrencyAmount presentValueCouponFromZSpread(
      ExpandedFixedCouponBond product,
      IssuerCurveDiscountFactors discountFactors,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear,
      LocalDate referenceDate,
      boolean exCoupon) {

    double total = 0d;
    for (FixedCouponBondPaymentPeriod period : product.getPeriodicPayments()) {
      if ((exCoupon && period.getDetachmentDate().isAfter(referenceDate)) ||
          (!exCoupon && period.getPaymentDate().isAfter(referenceDate))) {
        total += periodPricer.presentValueWithSpread(period, discountFactors, zSpread, compoundedRateType, periodsPerYear);
      }
    }
    return CurrencyAmount.of(product.getCurrency(), total);
  }

  //-------------------------------------------------------------------------
  private PointSensitivityBuilder presentValueSensitivityCoupon(
      ExpandedFixedCouponBond product,
      IssuerCurveDiscountFactors discountFactors,
      LocalDate referenceDate,
      boolean exCoupon) {

    PointSensitivityBuilder builder = PointSensitivityBuilder.none();
    for (FixedCouponBondPaymentPeriod period : product.getPeriodicPayments()) {
      if ((exCoupon && period.getDetachmentDate().isAfter(referenceDate)) ||
          (!exCoupon && period.getPaymentDate().isAfter(referenceDate))) {
        builder = builder.combinedWith(periodPricer.presentValueSensitivity(period, discountFactors));
      }
    }
    return builder;
  }

  private PointSensitivityBuilder presentValueSensitivityCouponFromZSpread(
      ExpandedFixedCouponBond product,
      IssuerCurveDiscountFactors discountFactors,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear,
      LocalDate referenceDate,
      boolean exCoupon) {

    PointSensitivityBuilder builder = PointSensitivityBuilder.none();
    for (FixedCouponBondPaymentPeriod period : product.getPeriodicPayments()) {
      if ((exCoupon && period.getDetachmentDate().isAfter(referenceDate)) ||
          (!exCoupon && period.getPaymentDate().isAfter(referenceDate))) {
        builder = builder.combinedWith(periodPricer.presentValueSensitivityWithSpread(
            period, discountFactors, zSpread, compoundedRateType, periodsPerYear));
      }
    }
    return builder;
  }

  private PointSensitivityBuilder presentValueSensitivityNominal(
      ExpandedFixedCouponBond product,
      IssuerCurveDiscountFactors discountFactors) {

    Payment nominal = product.getNominalPayment();
    PointSensitivityBuilder pt = nominalPricer.presentValueSensitivity(nominal, discountFactors.getDiscountFactors());
    if (pt instanceof ZeroRateSensitivity) {
      return IssuerCurveZeroRateSensitivity.of((ZeroRateSensitivity) pt, discountFactors.getLegalEntityGroup());
    }
    return pt; // NoPointSensitivity
  }

  private PointSensitivityBuilder presentValueSensitivityNominalFromZSpread(
      ExpandedFixedCouponBond product,
      IssuerCurveDiscountFactors discountFactors,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    Payment nominal = product.getNominalPayment();
    PointSensitivityBuilder pt = nominalPricer.presentValueSensitivity(
        nominal, discountFactors.getDiscountFactors(), zSpread, compoundedRateType, periodsPerYear);
    if (pt instanceof ZeroRateSensitivity) {
      return IssuerCurveZeroRateSensitivity.of((ZeroRateSensitivity) pt, discountFactors.getLegalEntityGroup());
    }
    return pt; // NoPointSensitivity
  }

  //-------------------------------------------------------------------------
  // compute pv of coupon payment(s) s.t. referenceDate1 < coupon <= referenceDate2
  double presentValueCoupon(
      ExpandedFixedCouponBond product,
      IssuerCurveDiscountFactors discountFactors,
      LocalDate referenceDate1,
      LocalDate referenceDate2,
      boolean exCoupon) {

    double pvDiff = 0d;
    for (FixedCouponBondPaymentPeriod period : product.getPeriodicPayments()) {
      if ((exCoupon && period.getDetachmentDate().isAfter(referenceDate1) && !period.getDetachmentDate().isAfter(referenceDate2)) ||
          (!exCoupon && period.getPaymentDate().isAfter(referenceDate1) && !period.getPaymentDate().isAfter(referenceDate2))) {
        pvDiff += periodPricer.presentValue(period, discountFactors);
      }
    }
    return pvDiff;
  }

  // compute pv of coupon payment(s) s.t. referenceDate1 < coupon <= referenceDate2
  double presentValueCouponWithZSpread(
      ExpandedFixedCouponBond expanded,
      IssuerCurveDiscountFactors discountFactors,
      LocalDate referenceDate1,
      LocalDate referenceDate2,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear,
      boolean exCoupon) {

    double pvDiff = 0d;
    for (FixedCouponBondPaymentPeriod period : expanded.getPeriodicPayments()) {
      if ((exCoupon && period.getDetachmentDate().isAfter(referenceDate1) && !period.getDetachmentDate().isAfter(referenceDate2)) ||
          (!exCoupon && period.getPaymentDate().isAfter(referenceDate1) && !period.getPaymentDate().isAfter(referenceDate2))) {
        pvDiff += periodPricer.presentValueWithSpread(period, discountFactors, zSpread, compoundedRateType, periodsPerYear);
      }
    }
    return pvDiff;
  }

}
