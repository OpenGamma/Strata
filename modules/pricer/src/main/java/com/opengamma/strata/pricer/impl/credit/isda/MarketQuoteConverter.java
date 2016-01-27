/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import java.util.Arrays;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Standard CDS have a standard premium (100 or 500bps in North America) and are quoted as either
 * as Points Up-Front (PUF) or quoted spread - these method allow conversion via a single constant hazard rate.
 * <p>
 * A credit curve can be built that is consistent with market quotes;
 * this can then be used to imply equivalent par spreads, and vice-versa.  
 */
public class MarketQuoteConverter {

  private final IsdaCompliantCreditCurveBuilder _builder;
  private final AnalyticCdsPricer _pricer;

  public MarketQuoteConverter() {
    _builder = new FastCreditCurveBuilder();
    _pricer = new AnalyticCdsPricer();
  }

  public MarketQuoteConverter(AccrualOnDefaultFormulae formula) {
    _builder = new FastCreditCurveBuilder(formula);
    _pricer = new AnalyticCdsPricer(formula);
  }

  //**************************************************************************************************************
  // Various ways of quoting the price for a given credit curve 
  //**************************************************************************************************************

  /**
   * The clean price as a fraction of notional (it is often expressed as a percentage of notional).
   * 
   * @param fractionalPUF  the points up-front (as a fraction)
   * @return the clean price (as a fraction)
   */
  public double cleanPrice(double fractionalPUF) {
    return 1 - fractionalPUF;
  }

  /**
   * The clean price as a fraction of notional (it is often expressed as a percentage of notional).
   * <p>
   * This requires that a credit curve is bootstrapped first.
   * 
   * @param cds  the CDS to be traded
   * @param yieldCurve  the yield/discount curve
   * @param creditCurve  the credit/hazard curve
   * @param coupon  the fractional quoted spread (coupon) of the CDS
   * @return the clean price  (as a fraction)
   */
  public double cleanPrice(
      CdsAnalytic cds,
      IsdaCompliantYieldCurve yieldCurve,
      IsdaCompliantCreditCurve creditCurve,
      double coupon) {

    double puf = pointsUpFront(cds, coupon, yieldCurve, creditCurve);
    return 1 - puf;
  }

  /**
   * The principal - this is the clean present value.
   * 
   * @param notional  the notional of the trade
   * @param cds  the CDS to be traded
   * @param yieldCurve  the yield/discount curve
   * @param creditCurve  the credit/hazard curve
   * @param coupon  the fractional quoted spread (coupon) of the CDS
   * @return the principle
   */
  public double principal(
      double notional,
      CdsAnalytic cds,
      IsdaCompliantYieldCurve yieldCurve,
      IsdaCompliantCreditCurve creditCurve,
      double coupon) {

    return notional * _pricer.pv(cds, yieldCurve, creditCurve, coupon, CdsPriceType.CLEAN);
  }

  /**
   * Get the points up-front  - this requires that a credit curve is bootstrapped first.
   * 
   * @param cds  the CDS to be traded
   * @param premium  the standard premium of the CDS <b>expressed as a fraction</b>
   * @param yieldCurve  the yield/discount curve
   * @param creditCurve  the credit/hazard curve
   * @return points up-front - these are usually quoted as a percentage of the notional.
   *  Here we return a fraction of notional, so 0.01 is 1(%) points up-front
   */
  public double pointsUpFront(
      CdsAnalytic cds,
      double premium,
      IsdaCompliantYieldCurve yieldCurve,
      IsdaCompliantCreditCurve creditCurve) {

    return _pricer.pv(cds, yieldCurve, creditCurve, premium, CdsPriceType.CLEAN);
  }

  /**
   * Get the points up-front for a collection of CDSs - this requires that a credit curve
   * is bootstrapped first.
   * <p>
   * This will give a slightly different answer to using a single (flat) credit curve for each
   * CDS (the latter is the market standard).
   * 
   * @param cds  the collection of CDSs
   * @param premium  the single common premium of the CDSs expressed as fractions (these are usually 0.01 or 0.05)
   * @param yieldCurve  the yield/discount curve
   * @param creditCurve  the credit/hazard curve
   * @return points up-front (as fractions)
   */
  public double[] pointsUpFront(
      CdsAnalytic[] cds,
      double premium,
      IsdaCompliantYieldCurve yieldCurve,
      IsdaCompliantCreditCurve creditCurve) {

    ArgChecker.noNulls(cds, "cds");
    int n = cds.length;
    double[] res = new double[n];
    for (int i = 0; i < n; i++) {
      res[i] = pointsUpFront(cds[i], premium, yieldCurve, creditCurve);
    }
    return res;
  }

  /**
   * Get the points up-front for a collection of CDSs.
   * <p>
   * This requires that a credit curve is bootstrapped first. This will give a slightly
   * different answer to using a single (flat) credit curve for each CDS
   * (the latter is the market standard).
   * 
   * @param cds  the collection of CDSs
   * @param premiums  the premiums of the CDSs expressed as fractions (these are usually 0.01 or 0.05)
   * @param yieldCurve  the yield/discount curve
   * @param creditCurve  the credit/hazard curve
   * @return points-upfront (as fractions)
   */
  public double[] pointsUpFront(
      CdsAnalytic[] cds,
      double[] premiums,
      IsdaCompliantYieldCurve yieldCurve,
      IsdaCompliantCreditCurve creditCurve) {

    ArgChecker.noNulls(cds, "cds");
    ArgChecker.notEmpty(premiums, "premiums");
    int n = cds.length;
    double[] res = new double[n];
    for (int i = 0; i < n; i++) {
      res[i] = pointsUpFront(cds[i], premiums[i], yieldCurve, creditCurve);
    }
    return res;
  }

  /**
   * The par spreads for a collection of CDSs where a single, non-flat, credit/hazard curve is known.
   * 
   * @param cds  the collection of CDSs
   * @param yieldCurve  the yield/discount curve
   * @param creditCurve  the credit/hazard curve
   * @return par spreads
   */
  public double[] parSpreads(CdsAnalytic[] cds, IsdaCompliantYieldCurve yieldCurve, IsdaCompliantCreditCurve creditCurve) {
    ArgChecker.noNulls(cds, "cds");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    ArgChecker.notNull(creditCurve, "creditCurve");
    int n = cds.length;
    double[] res = new double[n];
    for (int i = 0; i < n; i++) {
      res[i] = _pricer.parSpread(cds[i], yieldCurve, creditCurve);
    }
    return res;
  }

  //**************************************************************************************************************
  // Single Converters. These convert between a single quote as PUF or quoted spread by building a 
  // constant hazard rate curve.
  //**************************************************************************************************************

  /**
   * Convert from a CDS quoted spread to points up-front (PUF).
   * @param cds The CDS to be traded
   * @param qSpread The quoted spread
   * @param yieldCurve the yield/discount curve
   * @return the PUF
   */
  public PointsUpFront convert(CdsAnalytic cds, CdsQuotedSpread qSpread, IsdaCompliantYieldCurve yieldCurve) {
    ArgChecker.notNull(qSpread, "qSpread");
    double puf = quotedSpreadToPUF(cds, qSpread.getCoupon(), yieldCurve, qSpread.getQuotedSpread());
    return new PointsUpFront(qSpread.getCoupon(), puf);
  }

  /**
   * Convert from a CDS quoted spread to points up-front (PUF). <b>Note:</b> Quoted spread is not the same as par spread
   * (although they are numerically similar) -  it is simply an alternative quoting convention from PUF where the CDS is priced
   *  off a flat credit/hazard curve.
   * @param cds The CDS to be traded
   * @param premium The standard premium of the CDS <b>expressed as a fraction</b>
   * @param yieldCurve the yield/discount curve
   * @param quotedSpread The quoted spread (<b>as a fraction</b>).
   * @return points up-front - these are usually quoted as a percentage of the notional - here we return a fraction of notional,
   *  so 0.01 is 1(%) points up-front
   */
  public double quotedSpreadToPUF(CdsAnalytic cds, double premium, IsdaCompliantYieldCurve yieldCurve, double quotedSpread) {
    ArgChecker.notNull(cds, "cds");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    IsdaCompliantCreditCurve creditCurve = _builder.calibrateCreditCurve(cds, quotedSpread, yieldCurve);
    return pointsUpFront(cds, premium, yieldCurve, creditCurve);
  }

  /**
   * Convert from a CDS points up-front (PUF) to a quoted spread.
   * @param cds The CDS to be traded
   * @param puf point-up-front (this contains the premium) 
   * @param yieldCurve the yield/discount curve
   * @return the par spread
   */
  public CdsQuotedSpread convert(CdsAnalytic cds, PointsUpFront puf, IsdaCompliantYieldCurve yieldCurve) {
    ArgChecker.notNull(puf, "puf");
    double qs = pufToQuotedSpread(cds, puf.getCoupon(), yieldCurve, puf.getPointsUpFront());
    return new CdsQuotedSpread(puf.getCoupon(), qs);
  }

  /**
   * Convert from a CDS quote as points up-front (PUF) and a standard premium, to a <i>quoted</i> spread.
   * This is simply an alternative quoting convention from PUF where the CDS is priced off a flat credit/hazard curve.
   * @param cds The CDS to be traded
   * @param premium The standard premium of the CDS <b>expressed as a fraction</b>
   * @param yieldCurve the yield/discount curve
   * @param pointsUpfront points up-front
   * @return the par spread <b>expressed as a fraction</b>
   */
  public double pufToQuotedSpread(CdsAnalytic cds, double premium, IsdaCompliantYieldCurve yieldCurve, double pointsUpfront) {
    ArgChecker.notNull(cds, "cds");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    IsdaCompliantCreditCurve creditCurve = _builder.calibrateCreditCurve(cds, premium, yieldCurve, pointsUpfront);
    return _pricer.parSpread(cds, yieldCurve, creditCurve);
  }

  //**************************************************************************************************************
  // multiple Converters. These convert between N quotes as PUF or quoted spread by building N independent  
  // constant hazard rate curves. 
  //**************************************************************************************************************

  public PointsUpFront[] convert(CdsAnalytic[] cds, CdsQuotedSpread[] qSpreads, IsdaCompliantYieldCurve yieldCurve) {
    ArgChecker.notNull(cds, "cds");
    ArgChecker.notNull(qSpreads, "qSpreads");
    int n = cds.length;
    ArgChecker.isTrue(n == qSpreads.length, "numbe of CDSs does not match qSpreads");
    PointsUpFront[] res = new PointsUpFront[n];
    for (int i = 0; i < n; i++) {
      res[i] = convert(cds[i], qSpreads[i], yieldCurve);
    }
    return res;
  }

  /**
   *  Convert from a set of CDSs quoted spreads  to points up-front (PUF). <b>Note:</b> Quoted spread is not the same as par spread
   * (although they are numerically similar) - it is simply an alternative quoting convention from PUF where each CDS is priced
   *  off a <b>separate</b> flat credit/hazard curve - i.e. the CDSs are completely decoupled from each other.
   * @param cds   collection of CDSs
   * @param premium The single common premium of the CDSs expressed as fractions (these are usually 0.01 or 0.05)
   * @param yieldCurve yieldCurve the yield/discount curve
   * @param quotedSpreads The quoted spreads (<b>as a fractions</b>).
   * @return points up-front (expressed as fractions)
   */
  public double[] quotedSpreadsToPUF(
      CdsAnalytic[] cds,
      double premium,
      IsdaCompliantYieldCurve yieldCurve,
      double[] quotedSpreads) {

    ArgChecker.notNull(cds, "cds");
    ArgChecker.notEmpty(quotedSpreads, "parSpreads");
    int n = cds.length;
    ArgChecker.isTrue(n == quotedSpreads.length, "parSpreads wrong length");
    double[] res = new double[n];
    for (int i = 0; i < n; i++) {
      res[i] = quotedSpreadToPUF(cds[i], premium, yieldCurve, quotedSpreads[i]);
    }
    return res;
  }

  /**
  *  Convert from a set of CDSs quoted spreads  to points up-front (PUF). <b>Note:</b> Quoted spread is not the same as par spread
   * (although they are numerically similar) - it is simply an alternative quoting convention from PUF where each CDS is priced
   *  off a <b>separate</b> flat credit/hazard curve - i.e. the CDSs are completely decoupled from each other.
   * @param cds   collection of CDSs
   * @param premiums The premiums of the CDSs expressed as fractions (these are usually 0.01 or 0.05)
   * @param yieldCurve yieldCurve the yield/discount curve
   * @param  quotedSpreads The quoted spreads (<b>as a fractions</b>).
   * @return points up-front (expressed as fractions)
   */
  public double[] quotedSpreadsToPUF(
      CdsAnalytic[] cds,
      double[] premiums,
      IsdaCompliantYieldCurve yieldCurve,
      double[] quotedSpreads) {

    ArgChecker.notNull(cds, "cds");
    ArgChecker.notEmpty(premiums, "premiums");
    ArgChecker.notEmpty(quotedSpreads, "parSpreads");
    int n = cds.length;
    ArgChecker.isTrue(n == premiums.length, "premiums wrong length");
    ArgChecker.isTrue(n == quotedSpreads.length, "parSpreads wrong length");
    double[] res = new double[n];
    for (int i = 0; i < n; i++) {
      res[i] = quotedSpreadToPUF(cds[i], premiums[i], yieldCurve, quotedSpreads[i]);
    }
    return res;
  }

  public CdsQuotedSpread[] convert(CdsAnalytic[] cds, PointsUpFront[] puf, IsdaCompliantYieldCurve yieldCurve) {
    ArgChecker.notNull(cds, "cds");
    ArgChecker.notNull(puf, "puf");
    int n = cds.length;
    ArgChecker.isTrue(n == puf.length, "numbe of CDSs does not match puf");
    CdsQuotedSpread[] res = new CdsQuotedSpread[n];
    for (int i = 0; i < n; i++) {
      res[i] = convert(cds[i], puf[i], yieldCurve);
    }
    return res;
  }

  /**
   * Get the equivalent <i>quoted</i> spreads for a collection of CDSs. This is simply a quoting convention -each CDS is priced
   * off a <b>separate</b> flat credit/hazard curve - i.e. the CDSs are completely decoupled from each other.
   * @param cds collection of CDSs
   * @param premium The single common premium of the CDSs expressed as fractions (these are usually 0.01 or 0.05)
   * @param yieldCurve the yield/discount curve
   * @param pointsUpfront The points up-front (expressed as fractions)
   * @see MarketQuoteConverter#pufToQuotedSpread
   * @return collection of CDSs
   */
  public double[] pufToQuotedSpreads(
      CdsAnalytic[] cds,
      double premium,
      IsdaCompliantYieldCurve yieldCurve,
      double[] pointsUpfront) {

    ArgChecker.noNulls(cds, "cds");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    ArgChecker.notEmpty(pointsUpfront, "pointsUpfront");
    int n = cds.length;
    double[] res = new double[n];
    for (int i = 0; i < n; i++) {
      res[i] = pufToQuotedSpread(cds[i], premium, yieldCurve, pointsUpfront[i]);
    }
    return res;
  }

  /**
   * Get the equivalent <i>quoted</i> spreads for a collection of CDSs. This is simply a quoting convention -each CDS is priced
   * off a <b>separate</b> flat credit/hazard curve - i.e. the CDSs are completely decoupled from each other.
   * @param cds collection of CDSs
   * @param premiums The premiums of the CDSs expressed as fractions (these are usually 0.01 or 0.05)
   * @param yieldCurve the yield/discount curve
   * @param pointsUpfront The points up-front (expressed as fractions)
   * @see MarketQuoteConverter#pufToQuotedSpread
   * @return equivalent par spreads
   */
  public double[] pufToQuotedSpreads(
      CdsAnalytic[] cds,
      double[] premiums,
      IsdaCompliantYieldCurve yieldCurve,
      double[] pointsUpfront) {

    ArgChecker.noNulls(cds, "cds");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    ArgChecker.notEmpty(premiums, "premiums");
    ArgChecker.notEmpty(pointsUpfront, "pointsUpfront");
    int n = cds.length;
    double[] res = new double[n];
    for (int i = 0; i < n; i++) {
      res[i] = pufToQuotedSpread(cds[i], premiums[i], yieldCurve, pointsUpfront[i]);
    }
    return res;
  }

  //**************************************************************************************************************
  // Full Curve Converters. These convert between par spread quotes and PUF by constructing a single 
  // credit curve consistent with the quotes.
  //**************************************************************************************************************

  /**
   *  Convert from a set of CDSs quoted as a par spreads (the old way of quoting) to points up-front (PUF).
   *  Each CDS is priced off a <b>single non-flat</b> credit/hazard curve. <br>
   *  If the CDS are quoted as <b>quoted</b> spreads one must use quotedSpreadsToPUF instead
   *  {@link #pointsUpFront}
   * @param cds  collection of CDSs
   * @param premium The single common premium of the CDSs expressed as fractions (these are usually 0.01 or 0.05)
   * @param yieldCurve yieldCurve the yield/discount curve
   * @param parSpreads The par-spreads (<b>as a fractions</b>).
   * @return points up-front (expressed as fractions)
   */
  public double[] parSpreadsToPUF(
      CdsAnalytic[] cds,
      double premium,
      IsdaCompliantYieldCurve yieldCurve,
      double[] parSpreads) {

    IsdaCompliantCreditCurve creditCurve = _builder.calibrateCreditCurve(cds, parSpreads, yieldCurve);
    return pointsUpFront(cds, premium, yieldCurve, creditCurve);
  }

  /**
   *  Convert from a set of CDSs quoted as a par spreads (the old way of quoting) to points up-front (PUF).
   *  Each CDS is priced off a <b>single non-flat</b> credit/hazard curve. <br>
   *  If the CDS are quoted as <b>quoted</b> spreads one must use quotedSpreadsToPUF instead
   * @param cds  collection of CDSs
   * @param premiums The premiums of the CDSs expressed as fractions (these are usually 0.01 or 0.05)
   * @param yieldCurve yieldCurve the yield/discount curve
   * @param parSpreads The par-spreads (<b>as a fractions</b>).
   * @see MarketQuoteConverter#pointsUpFront
   * @return points up-front (expressed as fractions)
   */
  public double[] parSpreadsToPUF(
      CdsAnalytic[] cds,
      double[] premiums,
      IsdaCompliantYieldCurve yieldCurve,
      double[] parSpreads) {

    IsdaCompliantCreditCurve creditCurve = _builder.calibrateCreditCurve(cds, parSpreads, yieldCurve);
    return pointsUpFront(cds, premiums, yieldCurve, creditCurve);
  }

  /**
   * The equivalent par spreads for a collection of CDSs where a single, non-flat, credit/hazard curve is bootstrapped to
   * reprice all the given CDSs.
   * @param cds collection of CDSs
   * @param premium The single common premium of the CDSs expressed as fractions (these are usually 0.01 or 0.05)
   * @param yieldCurve the yield/discount curve
   * @param pointsUpfront The points up-front (expressed as fractions)
   * @see MarketQuoteConverter#pufToParSpreads
   * @return equivalent par spreads
   */
  public double[] pufToParSpreads(
      CdsAnalytic[] cds,
      double premium,
      IsdaCompliantYieldCurve yieldCurve,
      double[] pointsUpfront) {

    ArgChecker.noNulls(cds, "cds");
    int n = cds.length;
    double[] premiums = new double[n];
    Arrays.fill(premiums, premium);
    return pufToParSpreads(cds, premiums, yieldCurve, pointsUpfront);
  }

  /**
   * The equivalent par spreads for a collection of CDSs where a single, non-flat, credit/hazard curve is bootstrapped to
   * reprice all the given CDSs.
   * @param cds collection of CDSs
   * @param premiums The premiums of the CDSs expressed as fractions (these are usually 0.01 or 0.05)
   * @param yieldCurve the yield/discount curve
   * @param pointsUpfront The points up-front (expressed as fractions)
   * @see MarketQuoteConverter#parSpreads
   * @return equivalent par spreads
   */
  public double[] pufToParSpreads(
      CdsAnalytic[] cds,
      double[] premiums,
      IsdaCompliantYieldCurve yieldCurve,
      double[] pointsUpfront) {

    IsdaCompliantCreditCurve creditCurve = _builder.calibrateCreditCurve(cds, premiums, yieldCurve, pointsUpfront);
    return parSpreads(cds, yieldCurve, creditCurve);
  }

  /**
  * Convert from par spreads to quoted spreads
   * @param cds collection of CDSs
   * @param premium The single common premium of the CDSs expressed as fractions (these are usually 0.01 or 0.05)
   * @param yieldCurve the yield/discount curve
   * @param parSpreads par spreads
   * @return quoted spreads
   */
  public double[] parSpreadsToQuotedSpreads(
      CdsAnalytic[] cds,
      double premium,
      IsdaCompliantYieldCurve yieldCurve,
      double[] parSpreads) {

    double[] puf = parSpreadsToPUF(cds, premium, yieldCurve, parSpreads);
    return pufToQuotedSpreads(cds, premium, yieldCurve, puf);
  }

  /**
   * Convert from par spreads to quoted spreads
   * @param cds collection of CDSs
   * @param premiums The premiums of the CDSs expressed as fractions (these are usually 0.01 or 0.05)
   * @param yieldCurve the yield/discount curve
   * @param parSpreads par spreads
   * @return quoted spreads
   */
  public double[] parSpreadsToQuotedSpreads(
      CdsAnalytic[] cds,
      double[] premiums,
      IsdaCompliantYieldCurve yieldCurve,
      double[] parSpreads) {

    double[] puf = parSpreadsToPUF(cds, premiums, yieldCurve, parSpreads);
    return pufToQuotedSpreads(cds, premiums, yieldCurve, puf);
  }

  /**
   * Convert from quoted spreads to par spreads
   * @param cds collection of CDSs
   * @param premium The single common premium of the CDSs expressed as fractions (these are usually 0.01 or 0.05)
   * @param yieldCurve the yield/discount curve
   * @param quotedSpreads The quoted spreads
   * @return par spreads
   */
  public double[] quotedSpreadToParSpreads(
      CdsAnalytic[] cds,
      double premium,
      IsdaCompliantYieldCurve yieldCurve,
      double[] quotedSpreads) {

    double[] puf = quotedSpreadsToPUF(cds, premium, yieldCurve, quotedSpreads);
    return pufToParSpreads(cds, premium, yieldCurve, puf);
  }

  /**
   * Convert from quoted spreads to par spreads
   * @param cds collection of CDSs
   * @param premiums The premiums of the CDSs expressed as fractions (these are usually 0.01 or 0.05)
   * @param yieldCurve the yield/discount curve
   * @param quotedSpreads The quoted spreads
   * @return par spreads
   */
  public double[] quotedSpreadToParSpreads(
      CdsAnalytic[] cds,
      double[] premiums,
      IsdaCompliantYieldCurve yieldCurve,
      double[] quotedSpreads) {

    double[] puf = quotedSpreadsToPUF(cds, premiums, yieldCurve, quotedSpreads);
    return pufToParSpreads(cds, premiums, yieldCurve, puf);
  }

}
