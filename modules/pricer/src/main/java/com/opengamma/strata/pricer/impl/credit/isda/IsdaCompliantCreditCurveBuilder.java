/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import java.time.LocalDate;
import java.time.Period;

import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Construct a credit curve that is consistent with the ISDA c code - i.e. the credit curve is piecewise constant in the (forward)
 * hazard rates,  and agrees with ISDA (up to numerical round-off) for the same inputs 
 */
public abstract class IsdaCompliantCreditCurveBuilder {

  private static final ArbitrageHandling DEFAULT_ARBITRAGE_HANDLING = ArbitrageHandling.Ignore;
  private static final AccrualOnDefaultFormulae DEFAULT_FORMULA = AccrualOnDefaultFormulae.ORIGINAL_ISDA;

  private final ArbitrageHandling _arbHandling;
  private final AccrualOnDefaultFormulae _formula;

  protected IsdaCompliantCreditCurveBuilder() {
    _arbHandling = DEFAULT_ARBITRAGE_HANDLING;
    _formula = DEFAULT_FORMULA;
  }

  protected IsdaCompliantCreditCurveBuilder(AccrualOnDefaultFormulae formula) {
    ArgChecker.notNull(formula, "formula");
    _arbHandling = DEFAULT_ARBITRAGE_HANDLING;
    _formula = formula;
  }

  protected IsdaCompliantCreditCurveBuilder(AccrualOnDefaultFormulae formula, ArbitrageHandling arbHandling) {
    ArgChecker.notNull(formula, "formula");
    ArgChecker.notNull(arbHandling, "arbHandling");
    _arbHandling = arbHandling;
    _formula = formula;
  }

  public ArbitrageHandling getArbHanding() {
    return _arbHandling;
  }

  public AccrualOnDefaultFormulae getAccOnDefaultFormula() {
    return _formula;
  }

  /**
   * Bootstrapper the credit curve from a single market CDS quote. Obviously the resulting credit (hazard)
   * curve will be flat.
   *  
   * @param calibrationCDS The single market CDS - this is the reference instruments used to build the credit curve 
   * @param marketQuote The market quote of the CDS 
   * @param yieldCurve The yield (or discount) curve  
   * @return The credit curve  
   */
  public IsdaCompliantCreditCurve calibrateCreditCurve(
      CdsAnalytic calibrationCDS,
      CdsQuoteConvention marketQuote,
      IsdaCompliantYieldCurve yieldCurve) {

    double puf;
    double coupon;
    if (marketQuote instanceof CdsParSpread) {
      puf = 0.0;
      coupon = marketQuote.getCoupon();
    } else if (marketQuote instanceof CdsQuotedSpread) {
      puf = 0.0;
      coupon = ((CdsQuotedSpread) marketQuote).getQuotedSpread();
    } else if (marketQuote instanceof PointsUpFront) {
      PointsUpFront temp = (PointsUpFront) marketQuote;
      puf = temp.getPointsUpFront();
      coupon = temp.getCoupon();
    } else {
      throw new IllegalArgumentException("Unknown CDSQuoteConvention type " + marketQuote.getClass());
    }

    return calibrateCreditCurve(
        new CdsAnalytic[] {calibrationCDS}, new double[] {coupon}, yieldCurve, new double[] {puf});
  }

  /**
   * Bootstrapper the credit curve from a set of reference/calibration CDSs with market quotes 
   * @param calibrationCDSs The market CDSs - these are the reference instruments used to build the credit curve 
   * @param marketQuotes The market quotes of the CDSs 
   * @param yieldCurve The yield (or discount) curve 
   * @return The credit curve 
   */
  public IsdaCompliantCreditCurve calibrateCreditCurve(
      CdsAnalytic[] calibrationCDSs,
      CdsQuoteConvention[] marketQuotes,
      IsdaCompliantYieldCurve yieldCurve) {

    ArgChecker.noNulls(marketQuotes, "marketQuotes");
    int n = marketQuotes.length;
    double[] coupons = new double[n];
    double[] pufs = new double[n];
    for (int i = 0; i < n; i++) {
      double[] temp = getStandardQuoteForm(calibrationCDSs[i], marketQuotes[i], yieldCurve);
      coupons[i] = temp[0];
      pufs[i] = temp[1];
    }
    return calibrateCreditCurve(calibrationCDSs, coupons, yieldCurve, pufs);
  }

  /**
   * Bootstrapper the credit curve from a single market CDS quote given as a par spread. Obviously the resulting credit (hazard)
   *  curve will be flat.
   * @param cds  The single market CDS - this is the reference instruments used to build the credit curve 
   * @param parSpread The <b>fractional</b> par spread of the market CDS   
   * @param yieldCurve The yield (or discount) curve  
   * @return The credit curve  
   */
  public IsdaCompliantCreditCurve calibrateCreditCurve(CdsAnalytic cds, double parSpread, IsdaCompliantYieldCurve yieldCurve) {
    return calibrateCreditCurve(new CdsAnalytic[] {cds}, new double[] {parSpread}, yieldCurve, new double[1]);
  }

  /**
   * Bootstrapper the credit curve from a single market CDS quote given as points up-front (PUF) and a standard premium.
   * 
   * @param cds The single market CDS - this is the reference instruments used to build the credit curve 
   * @param premium The standard premium (coupon) as a fraction (these are 0.01 or 0.05 in North America)
   * @param yieldCurve The yield (or discount) curve
   * @param pointsUpfront points up-front as a fraction of notional 
   * @return The credit curve 
   */
  public IsdaCompliantCreditCurve calibrateCreditCurve(
      CdsAnalytic cds,
      double premium,
      IsdaCompliantYieldCurve yieldCurve,
      double pointsUpfront) {

    return calibrateCreditCurve(new CdsAnalytic[] {cds}, new double[] {premium}, yieldCurve, new double[] {pointsUpfront});
  }

  /**
   * Bootstrapper the credit curve from a set of reference/calibration CDSs quoted with par spreads. 
   * 
   * @param calibrationCDSs  The market CDSs - these are the reference instruments used to build the credit curve 
   * @param parSpreads The <b>fractional</b> par spreads of the market CDSs    
   * @param yieldCurve The yield (or discount) curve  
   * @return The credit curve 
   */
  public IsdaCompliantCreditCurve calibrateCreditCurve(
      CdsAnalytic[] calibrationCDSs,
      double[] parSpreads,
      IsdaCompliantYieldCurve yieldCurve) {

    ArgChecker.notNull(calibrationCDSs, "null CDS");
    int n = calibrationCDSs.length;
    double[] pointsUpfront = new double[n];
    return calibrateCreditCurve(calibrationCDSs, parSpreads, yieldCurve, pointsUpfront);
  }

  /**
   * Bootstrapper the credit curve from a set of reference/calibration CDSs quoted with points up-front and standard premiums .
   * 
   * @param calibrationCDSs The market CDSs - these are the reference instruments used to build the credit curve 
   * @param premiums The standard premiums (coupons) as fractions (these are 0.01 or 0.05 in North America) 
   * @param yieldCurve  The yield (or discount) curve  
   * @param pointsUpfront points up-front as fractions of notional 
   * @return The credit curve
   */
  public abstract IsdaCompliantCreditCurve calibrateCreditCurve(
      CdsAnalytic[] calibrationCDSs,
      double[] premiums,
      IsdaCompliantYieldCurve yieldCurve,
      double[] pointsUpfront);

  /**
   * Bootstrapper the credit curve from a single CDS, by making it have zero clean price.
   * Obviously the resulting credit (hazard) curve will be flat.
   * 
   * @param tradeDate The 'current' date
   * @param stepinDate Date when party assumes ownership. This is normally today + 1 (T+1). Aka assignment date or effective date.
   * @param valueDate The valuation date. The date that values are PVed to. Is is normally today + 3 business days.  Aka cash-settle date.
   * @param startDate The protection start date. If protectStart = true, then protections starts at the beginning of the day, otherwise it
   * is at the end.
   * @param endDate The maturity (or end of protection) of  the CDS 
   * @param fractionalParSpread - the (fractional) coupon that makes the CDS worth par (i.e. zero clean price)
   * @param payAccOnDefault Is the accrued premium paid in the event of a default
   * @param tenor The nominal step between premium payments (e.g. 3 months, 6 months).
   * @param stubType the stub convention
   * @param protectStart Does protection start at the beginning of the day
   * @param yieldCurve Curve from which payments are discounted
   * @param recoveryRate the recovery rate 
   * @return The credit curve
   */
  public IsdaCompliantCreditCurve calibrateCreditCurve(
      LocalDate tradeDate,
      LocalDate stepinDate,
      LocalDate valueDate,
      LocalDate startDate,
      LocalDate endDate,
      double fractionalParSpread,
      boolean payAccOnDefault,
      Period tenor,
      StubConvention stubType,
      boolean protectStart,
      IsdaCompliantYieldCurve yieldCurve,
      double recoveryRate) {

    return calibrateCreditCurve(
        tradeDate, stepinDate, valueDate, startDate, new LocalDate[] {endDate},
        new double[] {fractionalParSpread}, payAccOnDefault, tenor, stubType, protectStart,
        yieldCurve, recoveryRate);
  }

  /**
   * Bootstrapper the credit curve, by making each market CDS in turn have zero clean price.
   * 
   * @param tradeDate The 'current' date
   * @param stepinDate Date when party assumes ownership. This is normally today + 1 (T+1). Aka assignment date or effective date.
   * @param valueDate The valuation date. The date that values are PVed to. Is is normally today + 3 business days.  Aka cash-settle date.
   * @param startDate The protection start date. If protectStart = true, then protections starts at the beginning of the day, otherwise it
   * is at the end.
   * @param endDates The maturities (or end of protection) of each of the CDSs - must be ascending 
   * @param fractionalParSpreads - the (fractional) coupon that makes each CDS worth par (i.e. zero clean price)
   * @param payAccOnDefault Is the accrued premium paid in the event of a default
   * @param tenor The nominal step between premium payments (e.g. 3 months, 6 months).
   * @param stubType the stub convention
   * @param protectStart Does protection start at the beginning of the day
   * @param yieldCurve Curve from which payments are discounted
   * @param recoveryRate the recovery rate 
   * @return The credit curve
   */
  public IsdaCompliantCreditCurve calibrateCreditCurve(
      LocalDate tradeDate,
      LocalDate stepinDate,
      LocalDate valueDate,
      LocalDate startDate,
      LocalDate[] endDates,
      double[] fractionalParSpreads,
      boolean payAccOnDefault,
      Period tenor,
      StubConvention stubType,
      boolean protectStart,
      IsdaCompliantYieldCurve yieldCurve,
      double recoveryRate) {

    ArgChecker.notNull(endDates, "null endDates");
    ArgChecker.notEmpty(fractionalParSpreads, "no or null couponRates");
    int n = endDates.length;
    ArgChecker.isTrue(n == fractionalParSpreads.length, "length of couponRates does not match endDates");

    CdsAnalytic[] cds = new CdsAnalytic[n];
    for (int i = 0; i < n; i++) {
      cds[i] = new CdsAnalytic(
          tradeDate, stepinDate, valueDate, startDate, endDates[i], payAccOnDefault,
          tenor, stubType, protectStart, recoveryRate);
    }
    return calibrateCreditCurve(cds, fractionalParSpreads, yieldCurve);
  }

  /**
   * Put any CDS market quote into the form needed for the curve builder,
   * namely coupon and points up-front (which can be zero).
   * 
   * @param calibrationCDS
   * @param marketQuote
   * @param yieldCurve
   * @return The market quotes in the form required by the curve builder
   */
  private double[] getStandardQuoteForm(
      CdsAnalytic calibrationCDS,
      CdsQuoteConvention marketQuote,
      IsdaCompliantYieldCurve yieldCurve) {

    AnalyticCdsPricer pricer = new AnalyticCdsPricer(_formula);

    double[] res = new double[2];
    if (marketQuote instanceof CdsParSpread) {
      res[0] = marketQuote.getCoupon();
    } else if (marketQuote instanceof CdsQuotedSpread) {
      CdsQuotedSpread temp = (CdsQuotedSpread) marketQuote;
      double coupon = temp.getCoupon();
      double qSpread = temp.getQuotedSpread();
      IsdaCompliantCreditCurve cc = calibrateCreditCurve(
          new CdsAnalytic[] {calibrationCDS}, new double[] {qSpread}, yieldCurve, new double[1]);
      res[0] = coupon;
      res[1] = pricer.pv(calibrationCDS, yieldCurve, cc, coupon, CdsPriceType.CLEAN);
    } else if (marketQuote instanceof PointsUpFront) {
      PointsUpFront temp = (PointsUpFront) marketQuote;
      res[0] = temp.getCoupon();
      res[1] = temp.getPointsUpFront();
    } else {
      throw new IllegalArgumentException("Unknown CDSQuoteConvention type " + marketQuote.getClass());
    }
    return res;
  }

  /**
   * How should any arbitrage in the input data be handled 
   */
  public enum ArbitrageHandling {
    /**
     * If the market data has arbitrage, the curve will still build, but the survival probability will not be monotonically
     * decreasing (equivalently, some forward hazard rates will be negative)
     */
    Ignore,
    /**
     * An exception is throw if an arbitrage is found
     */
    Fail,
    /**
     * If a particular spread implies a negative forward hazard rate, the hazard rate is set to zero, and the calibration 
     * continues. The resultant curve will of course not exactly reprice the input CDSs, but will find new spreads that
     * just avoid arbitrage.   
     */
    ZeroHazardRate
  }

}
