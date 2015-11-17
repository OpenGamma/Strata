/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import java.util.Arrays;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.ShiftType;
import com.opengamma.strata.math.impl.differentiation.FiniteDifferenceType;

/**
 * This calculates the sensitivity of the present value of a CDS to various (finite) shifts of the market spreads -
 * this is performed by a "bump and reprice" so is accurate for arbitrarily large shifts/bumps.<br> For small bumps (typically
 * less than 1bps) it approximates the derivative $$\frac{\partial V}{\partial S}$$  where $V$ is the present value and $S$ is
 * either a single market spread or the entire market spread curve. However, it is better (in accuracy and speed) to use
 * AnalyticSpreadSensitivityCalculator if this derivative is required.
 */
public class FiniteDifferenceSpreadSensitivityCalculator {

  private final MarketQuoteConverter _pufConverter;
  private final IsdaCompliantCreditCurveBuilder _curveBuilder;
  private final AnalyticCdsPricer _pricer;

  public FiniteDifferenceSpreadSensitivityCalculator() {
    _pufConverter = new MarketQuoteConverter();
    _curveBuilder = new FastCreditCurveBuilder();
    _pricer = new AnalyticCdsPricer();
  }

  public FiniteDifferenceSpreadSensitivityCalculator(AccrualOnDefaultFormulae formula) {
    _pufConverter = new MarketQuoteConverter(formula);
    _curveBuilder = new FastCreditCurveBuilder(formula);
    _pricer = new AnalyticCdsPricer(formula);
  }

  //***************************************************************************************************************
  // parallel CS01 of a CDS from single market quote of that CDS
  //***************************************************************************************************************

  /**
   * The CS01 (or credit DV01)  of a CDS - the sensitivity of the PV to a finite increase of market spread (on NOT the CDS's
   * coupon). If the CDS is quoted as points up-front, this is first converted to a quoted spread, and <b>this</b> is bumped
   * @param cds analytic description of a CDS traded at a certain time - it is this CDS that we are calculation CDV01 for
   * @param quote The market quote for the CDS - these can be ParSpread, PointsUpFront or QuotedSpread
   * @param yieldCurve The yield (or discount) curve
   * @param fracBumpAmount The fraction bump amount of the spread so a 1pb bump is 1e-4
   * @return the parallel CS01
   */
  public double parallelCS01(
      CdsAnalytic cds,
      CdsQuoteConvention quote,
      IsdaCompliantYieldCurve yieldCurve,
      double fracBumpAmount) {

    if (quote instanceof CdsQuotedSpread) {
      CdsQuotedSpread qSpread = (CdsQuotedSpread) quote;
      return parallelCS01FromParSpreads(cds, qSpread.getCoupon(), yieldCurve, new CdsAnalytic[] {cds},
          new double[] {qSpread.getQuotedSpread()}, fracBumpAmount, ShiftType.ABSOLUTE);
    } else if (quote instanceof PointsUpFront) {
      PointsUpFront puf = (PointsUpFront) quote;
      return parallelCS01FromPUF(cds, puf.getCoupon(), yieldCurve, puf.getPointsUpFront(), fracBumpAmount);
    } else if (quote instanceof CdsParSpread) {
      return parallelCS01FromParSpreads(cds, quote.getCoupon(), yieldCurve, new CdsAnalytic[] {cds},
          new double[] {quote.getCoupon()}, fracBumpAmount, ShiftType.ABSOLUTE);
    }
    throw new IllegalArgumentException("unknow type " + quote.getClass());
  }

  /**
   *The CS01 (or credit DV01) by a shift of the quoted (or flat) spread of the CDS <b>when the CDS is quoted as points up-front (PUF)</b>.<br>
   *This simply converts the PUF quote to a quoted (or flat) spread then calls parallelCS01FromQuotedSpread
   * @param cds  analytic description of a CDS traded at a certain time - it is this CDS that we are calculation CDV01 for
   * @param coupon  the of the traded CDS  (expressed as <b>fractions not basis points</b>)
   * @param yieldCurve The yield (or discount) curve
   * @param puf points up-front (as a fraction)
   * @param fracBumpAmount The fraction bump amount <b>of the quoted (or flat) spread</b>, so a 1pb bump is 1e-4
   * @return  The credit DV01
   */
  public double parallelCS01FromPUF(
      CdsAnalytic cds,
      double coupon,
      IsdaCompliantYieldCurve yieldCurve,
      double puf,
      double fracBumpAmount) {

    double bumpedQSpread = _pufConverter.pufToQuotedSpread(cds, coupon, yieldCurve, puf) + fracBumpAmount;
    IsdaCompliantCreditCurve bumpedCurve = _curveBuilder.calibrateCreditCurve(cds, bumpedQSpread, yieldCurve);
    double bumpedPrice = _pricer.pv(cds, yieldCurve, bumpedCurve, coupon);
    return (bumpedPrice - puf) / fracBumpAmount;
  }

  /**
   * The CS01 (or credit DV01) by a shift of the market spread of the CDS (the coupon is unchanged). This finds two flat
   *  credit/hazard curves from  the CDS with its original spread and with the spread bumped. The traded CDS is then priced
   * off both curves, using its coupon, and the difference (divided by the bump size) is the credit DV01
   * @param cds analytic description of a CDS traded at a certain time - it is this CDS that we are calculation CDV01 for
   * @param coupon the of the traded CDS  (expressed as <b>fractions not basis points</b>)
   * @param yieldCurve  The yield (or discount) curve
   * @param marketSpread the market spread of the reference CDS (in this case it is irrelevant whether this is par or quoted spread)
   * @param fracBumpAmount The fraction bump amount, so a 1pb bump is 1e-4
   * @param shiftType ABSOLUTE or RELATIVE
   * @return The credit DV01
   */
  public double parallelCS01FromSpread(
      CdsAnalytic cds,
      double coupon,
      IsdaCompliantYieldCurve yieldCurve,
      double marketSpread,
      double fracBumpAmount,
      ShiftType shiftType) {

    return parallelCS01FromParSpreads(cds, coupon, yieldCurve, new CdsAnalytic[] {cds}, new double[] {marketSpread},
        fracBumpAmount, shiftType);
  }

  //***************************************************************************************************************
  // parallel CS01 of CDS from single market quote of (potentially) different CDS
  //***************************************************************************************************************

  /**
   * The CS01 (or credit DV01) by a shift of the quoted (or flat) spread of the reference CDS. This finds two flat credit/hazard curves from
   * a reference CDS with its original quoted (or flat) spread and with the spread bumped. The traded CDS is then priced off both curves, using its coupon,
   * and the difference (divided by the bump size) is the credit DV01
   * @param cds analytic description of a CDS traded at a certain time - it is this CDS that we are calculation CDV01 for
   * @param coupon the coupon of the traded CDS  (expressed as <b>fractions not basis points</b>)
   * @param yieldCurve  The yield (or discount) curve
   * @param referenceCDS the reference CDS use to find the flat credit/hazard curve (this is often the same as the traded CDS)
   * @param quotedSpread the quoted (or flat) spread of the reference CDS
   * @param fracBumpAmount The fraction bump amount, so a 1pb bump is 1e-4
   * @param shiftType ABSOLUTE or RELATIVE
   * @return The credit DV01
   */
  public double parallelCS01FromQuotedSpread(
      CdsAnalytic cds,
      double coupon,
      IsdaCompliantYieldCurve yieldCurve,
      CdsAnalytic referenceCDS,
      double quotedSpread,
      double fracBumpAmount,
      ShiftType shiftType) {

    ArgChecker.notNull(cds, "cds");
    ArgChecker.notNull(referenceCDS, "referanceCDS");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    ArgChecker.notNull(shiftType, "shiftType");
    ArgChecker.isTrue(Math.abs(fracBumpAmount) > 1e-10, "bump amount too small");
    return parallelCS01FromParSpreads(cds, coupon, yieldCurve, new CdsAnalytic[] {referenceCDS}, new double[] {quotedSpread},
        fracBumpAmount, shiftType);
  }

  //***************************************************************************************************************
  // parallel CS01 of a CDS from a set of market quotes at pillar dates (e.g. 6M, 1Y, 3Y, 5Y, 10Y)
  //***************************************************************************************************************

  /**
   * The CS01 (or credit DV01) by a parallel shift of the market spreads (CDS spread curve). This takes an extraneous yield curve,
   *  a set of reference CDSs (marketCDSs) and their market quotes and bootstraps a credit (hazard) curve -
   * the target CDS is then priced with this credit curve. This is then repeated with the market spreads bumped in parallel by
   * some amount. The result is the difference (bumped minus base price) is divided by the bump amount.<br>
   * This can take quotes as ParSpread, PointsUpFront or QuotedSpread (or some mix).  For par-spreads, these are bumped and a
   * new credit curve built; for quoted-spreads, there are bumped and a new curve build be first converting to PUF; and finally
   * for PUF, these are converted to quoted spreads, bumped and converted back to build the credit curve.
   * @param cds analytic description of a CDS traded at a certain time - it is this CDS that we are calculation CDV01 for
   * @param cdsCoupon the coupon of the traded CDS  (expressed as <b>fractions not basis points</b>)
   * @param yieldCurve The yield (or discount) curve
   * @param marketCDSs The market CDSs - these are the reference instruments used to build the credit curve
   * @param quotes The quotes for the market CDSs - these can be ParSpread, PointsUpFront or QuotedSpread (or any mixture of these)
   * @param fracBumpAmount The fraction bump amount, so a 1pb bump is 1e-4
   * @return  The credit DV01
   */
  public double parallelCS01FromPillarQuotes(
      CdsAnalytic cds,
      double cdsCoupon,
      IsdaCompliantYieldCurve yieldCurve,
      CdsAnalytic[] marketCDSs,
      CdsQuoteConvention[] quotes,
      double fracBumpAmount) {

    ArgChecker.notNull(cds, "cds");
    ArgChecker.noNulls(marketCDSs, "curvePoints");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    ArgChecker.noNulls(quotes, "quotes");
    ArgChecker.isTrue(Math.abs(fracBumpAmount) > 1e-10, "bump amount too small");
    int n = marketCDSs.length;
    ArgChecker.isTrue(n == quotes.length, "speads length does not match curvePoints");

    IsdaCompliantCreditCurve baseCurve = _curveBuilder.calibrateCreditCurve(marketCDSs, quotes, yieldCurve);
    double basePrice = _pricer.pv(cds, yieldCurve, baseCurve, cdsCoupon);

    CdsQuoteConvention[] bumpedQuotes = bumpQuotes(marketCDSs, quotes, yieldCurve, fracBumpAmount);
    IsdaCompliantCreditCurve bumpedCurve = _curveBuilder.calibrateCreditCurve(marketCDSs, bumpedQuotes, yieldCurve);
    double bumpedPrice = _pricer.pv(cds, yieldCurve, bumpedCurve, cdsCoupon);

    return (bumpedPrice - basePrice) / fracBumpAmount;
  }

  /**
   * The CS01 (or credit DV01) by a parallel shift of the market par spreads (CDS par spread curve). This takes an extraneous yield curve, a set of reference CDSs
   * (marketCDSs) and their par-spreads (expressed as <b>fractions not basis points</b>) and bootstraps a credit (hazard) curve -
   * the target CDS is then priced with this credit curve. This is then repeated with the market spreads bumped in parallel by
   * some amount. The result is the difference (bumped minus base price) is divided by the bump amount.<br>
   * For small bumps (<1e-4) this approximates $$\frac{\partial V}{\partial S}$$<br>
   * Credit DV01 is (often) defined as -( V(S + 1bp) - V(s)) - to achieve this use fracBumpAmount = 1e-4 and shiftType ABSOLUTE
   * @param cds analytic description of a CDS traded at a certain time
   * @param cdsFracSpread The <b>fraction</b> spread of the CDS
   * @param yieldCurve The yield (or discount) curve
   * @param marketCDSs The market CDSs - these are the reference instruments used to build the credit curve
   * @param parSpreads The <b>fractional</b> spreads of the market CDSs
   * @param fracBumpAmount The fraction bump amount, so a 1pb bump is 1e-4
   * @param shiftType ABSOLUTE or RELATIVE
   * @return The credit DV01
   */
  public double parallelCS01FromParSpreads(
      CdsAnalytic cds,
      double cdsFracSpread,
      IsdaCompliantYieldCurve yieldCurve,
      CdsAnalytic[] marketCDSs,
      double[] parSpreads,
      double fracBumpAmount,
      ShiftType shiftType) {

    ArgChecker.notNull(cds, "cds");
    ArgChecker.noNulls(marketCDSs, "curvePoints");
    ArgChecker.notEmpty(parSpreads, "spreads");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    ArgChecker.notNull(shiftType, "shiftType");
    ArgChecker.isTrue(Math.abs(fracBumpAmount) > 1e-10, "bump amount too small");
    int n = marketCDSs.length;
    ArgChecker.isTrue(n == parSpreads.length, "speads length does not match curvePoints");
    double[] bumpedSpreads = makeBumpedSpreads(parSpreads, fracBumpAmount, shiftType);
    double diff = fdCreditDV01(cds, cdsFracSpread, marketCDSs, bumpedSpreads, parSpreads, yieldCurve, CdsPriceType.DIRTY);
    return diff / fracBumpAmount;
  }

  public double parallelCS01FromCreditCurve(
      CdsAnalytic cds,
      double cdsCoupon,
      CdsAnalytic[] pillarCDSs,
      IsdaCompliantYieldCurve yieldCurve,
      IsdaCompliantCreditCurve creditCurve,
      double fracBumpAmount) {

    ArgChecker.notNull(cds, "cds");
    ArgChecker.noNulls(pillarCDSs, "pillarCDSs");
    ArgChecker.notNull(creditCurve, "creditCurve");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    ArgChecker.isTrue(Math.abs(fracBumpAmount) > 1e-10, "bump amount too small");
    int n = pillarCDSs.length;

    double[] impSpreads = new double[n];
    double[] t = new double[n];
    for (int i = 0; i < n; i++) {
      impSpreads[i] = _pricer.parSpread(pillarCDSs[i], yieldCurve, creditCurve);
      t[i] = pillarCDSs[i].getProtectionEnd();
      if (i > 0) {
        ArgChecker.isTrue(t[i] > t[i - 1], "pillars must be assending");
      }
    }

    IsdaCompliantCreditCurve baseCurve = _curveBuilder.calibrateCreditCurve(pillarCDSs, impSpreads, yieldCurve);
    double basePrice = _pricer.pv(cds, yieldCurve, baseCurve, cdsCoupon);
    double[] bumpedSpreads = makeBumpedSpreads(impSpreads, fracBumpAmount, ShiftType.ABSOLUTE);
    IsdaCompliantCreditCurve bumpedCurve = _curveBuilder.calibrateCreditCurve(pillarCDSs, bumpedSpreads, yieldCurve);
    double price = _pricer.pv(cds, yieldCurve, bumpedCurve, cdsCoupon);
    double res = (price - basePrice) / fracBumpAmount;

    return res;
  }

  //***************************************************************************************************************
  // bucked CS01 - the sensitivity of the CDS's PV to the market spreads used to build the credit curve - these are
  // the pillar dates (e.g. 6M, 1Y, 3Y, 5Y, 10Y)
  //***************************************************************************************************************

  /**
   * The bucked CS01 (or credit DV01) by a shift of each the market spread in turn. This takes an extraneous yield curve,
   *  a set of reference CDSs (marketCDSs) and their market quotes and bootstraps a credit (hazard) curve -
   * the target CDS is then priced with this credit curve. This is then repeated with each market spreads bumped in turn by
   * some amount. The result is the array of differences (bumped minus base price) is divided by the bump amount.<br>
   * This can take quotes as ParSpread, PointsUpFront or QuotedSpread (or some mix).  For par-spreads, these are bumped and a
   * new credit curve built; for quoted-spreads, there are bumped and a new curve build be first converting to PUF; and finally
   * for PUF, these are converted to quoted spreads, bumped and converted back to build the credit curve.
   * @param cds analytic description of a CDS traded at a certain time - it is this CDS that we are calculation CDV01 for
   * @param cdsCoupon the coupon of the traded CDS  (expressed as <b>fractions not basis points</b>)
   * @param yieldCurve The yield (or discount) curve
   * @param marketCDSs The market CDSs - these are the reference instruments used to build the credit curve
   * @param quotes The quotes for the market CDSs - these can be ParSpread, PointsUpFront or QuotedSpread (or any mixture of these)
   * @param fracBumpAmount The fraction bump amount, so a 1pb bump is 1e-4
   * @return  The bucketed credit DV01
   */
  public double[] bucketedCS01FromPillarQuotes(
      CdsAnalytic cds,
      double cdsCoupon,
      IsdaCompliantYieldCurve yieldCurve,
      CdsAnalytic[] marketCDSs,
      CdsQuoteConvention[] quotes,
      double fracBumpAmount) {

    ArgChecker.notNull(cds, "cds");
    ArgChecker.noNulls(marketCDSs, "curvePoints");
    ArgChecker.noNulls(quotes, "quotes");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    ArgChecker.isTrue(Math.abs(fracBumpAmount) > 1e-10, "bump amount too small");
    int n = marketCDSs.length;
    ArgChecker.isTrue(n == quotes.length, "speads length does not match curvePoints");

    IsdaCompliantCreditCurve baseCurve = _curveBuilder.calibrateCreditCurve(marketCDSs, quotes, yieldCurve);
    double basePrice = _pricer.pv(cds, yieldCurve, baseCurve, cdsCoupon);
    double[] res = new double[n];
    for (int i = 0; i < n; i++) {
      CdsQuoteConvention[] bumpedQuotes = bumpQuoteAtIndex(marketCDSs, quotes, yieldCurve, fracBumpAmount, i);
      IsdaCompliantCreditCurve bumpedCurve = _curveBuilder.calibrateCreditCurve(marketCDSs, bumpedQuotes, yieldCurve);
      double price = _pricer.pv(cds, yieldCurve, bumpedCurve, cdsCoupon);
      res[i] = (price - basePrice) / fracBumpAmount;
    }
    return res;
  }

  /**
   * The bucked CS01 (or credit DV01) by shifting each  market par-spread in turn. This takes an extraneous yield curve, a set of reference CDSs
   * (marketCDSs) and their par-spreads (expressed as <b>fractions not basis points</b>) and bootstraps a credit (hazard) curve -
   * the target CDS is then priced with this credit curve. This is then repeated with each market spreads bumped in turn.
   * The result is the vector of differences (bumped minus base price) divided by the bump amount.<br>
   * For small bumps (<1e-4) this approximates $$\frac{\partial V}{\partial S_i}$$ where $$S_i$$ is the spread of the $$1^{th}$$
   * market CDS<br>
   * @param cds analytic description of a CDS traded at a certain time
   * @param cdsCoupon The <b>fraction</b> spread of the CDS
   * @param yieldCurve The yield (or discount) curve
   * @param marketCDSs The market CDSs - these are the reference instruments used to build the credit curve
   * @param marketParSpreads The <b>fractional</b> par-spreads of the market CDSs
   * @param fracBumpAmount The fraction bump amount, so a 1pb bump is 1e-4
   * @param shiftType ABSOLUTE or RELATIVE
   * @return The credit CS01
   */
  public double[] bucketedCS01FromParSpreads(
      CdsAnalytic cds,
      double cdsCoupon,
      IsdaCompliantYieldCurve yieldCurve,
      CdsAnalytic[] marketCDSs,
      double[] marketParSpreads,
      double fracBumpAmount,
      ShiftType shiftType) {

    ArgChecker.notNull(cds, "cds");
    ArgChecker.noNulls(marketCDSs, "curvePoints");
    ArgChecker.notEmpty(marketParSpreads, "spreads");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    ArgChecker.notNull(shiftType, "shiftType");
    ArgChecker.isTrue(Math.abs(fracBumpAmount) > 1e-10, "bump amount too small");
    int n = marketCDSs.length;
    ArgChecker.isTrue(n == marketParSpreads.length, "speads length does not match curvePoints");
    CdsPriceType priceType = CdsPriceType.DIRTY;

    IsdaCompliantCreditCurve baseCurve = _curveBuilder.calibrateCreditCurve(marketCDSs, marketParSpreads, yieldCurve);
    double basePrice = _pricer.pv(cds, yieldCurve, baseCurve, cdsCoupon, priceType);

    double[] res = new double[n];
    for (int i = 0; i < n; i++) {
      double[] temp = makeBumpedSpreads(marketParSpreads, fracBumpAmount, shiftType, i);
      IsdaCompliantCreditCurve bumpedCurve = _curveBuilder.calibrateCreditCurve(marketCDSs, temp, yieldCurve);
      double price = _pricer.pv(cds, yieldCurve, bumpedCurve, cdsCoupon, priceType);
      res[i] = (price - basePrice) / fracBumpAmount;
    }

    return res;
  }

  /**
   * The bucked CS01 (or credit DV01) by bumping each quoted (or flat) spread in turn. This takes an extraneous yield curve,
   *  a set of reference CDSs (marketCDSs) and their quoted (or flat) spreads (expressed as <b>fractions not basis points</b>) and
   *  bootstraps a credit (hazard) curve -
   * the target CDS is then priced with this credit curve. This is then repeated with each market spreads bumped in turn.
   * The result is the vector of differences (bumped minus base price) divided by the bump amount.<br>
   * For small bumps (<1e-4) this approximates $$\frac{\partial V}{\partial S_i}$$ for a flat curve where $$S_i$$
   * is the spread of the $$1^{th}$$ market CDS
   * @param cds analytic description of a CDS traded at a certain time
   * @param dealSpread The <b>fraction</b> spread of the CDS
   * @param yieldCurve The yield (or discount) curve
   * @param marketCDSs The market CDSs - these are the reference instruments used to build the credit curve
   * @param quotedSpreads The <b>fractional</b> spreads of the market CDSs
   * @param fracBumpAmount The fraction bump amount, so a 1pb bump is 1e-4
   * @param shiftType ABSOLUTE or RELATIVE
   * @return The bucked CS01 for a single CDS
   */
  public double[] bucketedCS01FromQuotedSpreads(
      CdsAnalytic cds,
      double dealSpread,
      IsdaCompliantYieldCurve yieldCurve,
      CdsAnalytic[] marketCDSs,
      double[] quotedSpreads,
      double fracBumpAmount,
      ShiftType shiftType) {

    ArgChecker.notNull(cds, "cds");
    ArgChecker.noNulls(marketCDSs, "curvePoints");
    ArgChecker.notEmpty(quotedSpreads, "spreads");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    ArgChecker.notNull(shiftType, "shiftType");
    ArgChecker.isTrue(Math.abs(fracBumpAmount) > 1e-10, "bump amount too small");
    int n = marketCDSs.length;
    ArgChecker.isTrue(n == quotedSpreads.length, "speads length does not match curvePoints");
    CdsPriceType priceType = CdsPriceType.DIRTY;
    double[] premiums = new double[n];
    Arrays.fill(premiums, dealSpread); // assume the premiums of all CDS are equal

    double[] puf = _pufConverter.quotedSpreadsToPUF(marketCDSs, premiums, yieldCurve, quotedSpreads);
    IsdaCompliantCreditCurve baseCurve = _curveBuilder.calibrateCreditCurve(marketCDSs, premiums, yieldCurve, puf);
    double basePrice = _pricer.pv(cds, yieldCurve, baseCurve, dealSpread, priceType);

    double[] bumpedPUF = new double[n];
    double[] res = new double[n];
    for (int i = 0; i < n; i++) {
      System.arraycopy(puf, 0, bumpedPUF, 0, n);
      double bumpedSpread = bumpedSpread(quotedSpreads[i], fracBumpAmount, shiftType);
      bumpedPUF[i] = _pufConverter.quotedSpreadToPUF(marketCDSs[i], premiums[i], yieldCurve, bumpedSpread);
      // TODO a lot of unnecessary recalibration here
      IsdaCompliantCreditCurve bumpedCurve = _curveBuilder
          .calibrateCreditCurve(marketCDSs, premiums, yieldCurve, bumpedPUF);
      double price = _pricer.pv(cds, yieldCurve, bumpedCurve, dealSpread, priceType);
      res[i] = (price - basePrice) / fracBumpAmount;
    }
    return res;
  }

  /**
   * The bucked CS01 (or credit DV01) on a set of CDSS by bumping each quoted (or flat) spread in turn. This takes an extraneous yield curve,
   *  a set of reference CDSs (marketCDSs) and their quoted (or flat) spreads (expressed as <b>fractions not basis points</b>) and
   *  bootstraps a credit (hazard) curve -
   * the target CDS is then priced with this credit curve. This is then repeated with each market spreads bumped in turn.
   * The result is the vector of differences (bumped minus base price) divided by the bump amount.<br>
   * For small bumps (<1e-4) this approximates $$\frac{\partial V}{\partial S_i}$$ for a flat curve where $$S_i$$
   * @param cds a set of analytic description of  CDSs traded at a certain times
   * @param dealSpread The <b>fraction</b> spread of the CDS
   * @param yieldCurve The yield (or discount) curve
   * @param marketCDSs The market CDSs - these are the reference instruments used to build the credit curve
   * @param quotedSpreads The <b>fractional</b> spreads of the market CDSs
   * @param fracBumpAmount The fraction bump amount, so a 1pb bump is 1e-4
   * @param shiftType ABSOLUTE or RELATIVE
   * @return The bucked CS01 for a set of  CDSs
   */
  public double[][] bucketedCS01FromQuotedSpreads(
      CdsAnalytic[] cds,
      double dealSpread,
      IsdaCompliantYieldCurve yieldCurve,
      CdsAnalytic[] marketCDSs,
      double[] quotedSpreads,
      double fracBumpAmount,
      ShiftType shiftType) {

    ArgChecker.noNulls(cds, "cds");
    ArgChecker.noNulls(marketCDSs, "curvePoints");
    ArgChecker.notEmpty(quotedSpreads, "spreads");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    ArgChecker.notNull(shiftType, "shiftType");
    ArgChecker.isTrue(Math.abs(fracBumpAmount) > 1e-10, "bump amount too small");
    int nMarketCDSs = marketCDSs.length;
    ArgChecker.isTrue(nMarketCDSs == quotedSpreads.length, "speads length does not match curvePoints");
    CdsPriceType priceType = CdsPriceType.DIRTY;
    double[] premiums = new double[nMarketCDSs];
    Arrays.fill(premiums, dealSpread); // assume the premiums of all CDS are equal

    int nTradeCDSs = cds.length;

    double[] puf = _pufConverter.quotedSpreadsToPUF(marketCDSs, premiums, yieldCurve, quotedSpreads);
    //TODO not needed
    IsdaCompliantCreditCurve baseCurve = _curveBuilder.calibrateCreditCurve(marketCDSs, premiums, yieldCurve, puf);
    double[] basePrices = new double[nTradeCDSs];
    for (int j = 0; j < nTradeCDSs; j++) {
      basePrices[j] = _pricer.pv(cds[j], yieldCurve, baseCurve, dealSpread, priceType);
    }

    double[] bumpedPUF = new double[nMarketCDSs];
    double[][] res = new double[nTradeCDSs][nMarketCDSs];

    for (int i = 0; i < nMarketCDSs; i++) { //Outer loop is over bumps
      System.arraycopy(puf, 0, bumpedPUF, 0, nMarketCDSs);
      double bumpedSpread = bumpedSpread(quotedSpreads[i], fracBumpAmount, shiftType);
      bumpedPUF[i] = _pufConverter.quotedSpreadToPUF(marketCDSs[i], premiums[i], yieldCurve, bumpedSpread);
      // TODO a lot of unnecessary recalibration here
      IsdaCompliantCreditCurve bumpedCurve = _curveBuilder
          .calibrateCreditCurve(marketCDSs, premiums, yieldCurve, bumpedPUF);
      for (int j = 0; j < nTradeCDSs; j++) {
        double price = _pricer.pv(cds[j], yieldCurve, bumpedCurve, dealSpread, priceType);
        res[j][i] = (price - basePrices[j]) / fracBumpAmount;
      }
    }
    return res;
  }

  /**
   * The bucked CS01 (or credit DV01) by shifting each implied par-spread in turn. This takes an extraneous yield curve,
   *  a set of pillar CDSs and their corresponding par-spread, and  a set of bucket CDSs (CDSs with maturities equal to the bucket
   *  points). A credit curve is bootstrapped from the pillar CDSs - this is then used to imply spreads at the bucket maturities.
   *  These spreads form pseudo market spreads to bootstraps a new credit (hazard) curve -
   * the target CDS is then priced with this credit curve. This is then repeated with each  spreads bumped in turn.
   * The result is the vector of differences (bumped minus base price) divided by the bump amount.
   * @param cds analytic description of a CDS traded at a certain time
   * @param cdsCoupon The <b>fraction</b> spread of the CDS
   * @param bucketCDSs these are the reference instruments that correspond to maturity buckets
   * @param yieldCurve The yield (or discount) curve
   * @param pillarCDSs  These are the market CDSs used to build the credit curve
   * @param pillarSpreads These are the par-spreads of the market (pillar) CDSs
   * @param fracBumpAmount The fraction bump amount, so a 1pb bump is 1e-4
   * @return The credit DV01
   */
  public double[] bucketedCS01FromParSpreads(
      CdsAnalytic cds,
      double cdsCoupon,
      CdsAnalytic[] bucketCDSs,
      IsdaCompliantYieldCurve yieldCurve,
      CdsAnalytic[] pillarCDSs,
      double[] pillarSpreads,
      double fracBumpAmount) {

    ArgChecker.noNulls(pillarCDSs, "pillarCDSs");
    ArgChecker.notEmpty(pillarSpreads, "pillarSpreads");
    IsdaCompliantCreditCurve creditCurve = _curveBuilder.calibrateCreditCurve(pillarCDSs, pillarSpreads, yieldCurve);
    return bucketedCS01FromCreditCurve(cds, cdsCoupon, bucketCDSs, yieldCurve, creditCurve, fracBumpAmount);
  }

  public double[] bucketedCS01FromPUF(
      CdsAnalytic cds,
      PointsUpFront puf,
      IsdaCompliantYieldCurve yieldCurve,
      CdsAnalytic[] bucketCDSs,
      double fracBumpAmount) {

    IsdaCompliantCreditCurve cc = _curveBuilder.calibrateCreditCurve(cds, puf, yieldCurve);
    return bucketedCS01FromCreditCurve(cds, puf.getCoupon(), bucketCDSs, yieldCurve, cc, fracBumpAmount);
  }

  /**
   * The bucked CS01 (or credit DV01) by shifting each  implied par-spread in turn. This takes an extraneous yield curve and a credit
   *  curve and a set of bucket CDSs (CDSs with maturities equal to the bucket points). Par-spreads at the bucket maturities are
   *  implied from the credit curve. These spreads form pseudo market spreads to bootstraps a new credit (hazard) curve -
   * the target CDS is then priced with this credit curve. This is then repeated with each  spreads bumped in turn.
   * The result is the vector of differences (bumped minus base price) divided by the bump amount.
   * @param cds analytic description of a CDS traded at a certain time
   * @param cdsCoupon The <b>fraction</b> spread of the CDS
   * @param bucketCDSs  these are the reference instruments that correspond to maturity buckets
   * @param yieldCurve The yield (or discount) curve
   * @param creditCurve the credit curve
   * @param fracBumpAmount The fraction bump amount, so a 1pb bump is 1e-4
   * @return The credit DV01
   */
  public double[] bucketedCS01FromCreditCurve(
      CdsAnalytic cds,
      double cdsCoupon,
      CdsAnalytic[] bucketCDSs,
      IsdaCompliantYieldCurve yieldCurve,
      IsdaCompliantCreditCurve creditCurve,
      double fracBumpAmount) {

    ArgChecker.notNull(cds, "cds");
    ArgChecker.noNulls(bucketCDSs, "bucketCDSs");
    ArgChecker.notNull(creditCurve, "creditCurve");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    ArgChecker.isTrue(Math.abs(fracBumpAmount) > 1e-10, "bump amount too small");
    int n = bucketCDSs.length;

    double[] impSpreads = new double[n];
    double[] t = new double[n];
    for (int i = 0; i < n; i++) {
      impSpreads[i] = _pricer.parSpread(bucketCDSs[i], yieldCurve, creditCurve);
      t[i] = bucketCDSs[i].getProtectionEnd();
      if (i > 0) {
        ArgChecker.isTrue(t[i] > t[i - 1], "buckets must be assending");
      }
    }
    int index = Arrays.binarySearch(t, cds.getProtectionEnd());
    if (index < 0) {
      index = -1 - index;
    }
    index = Math.min(index, n - 1);

    IsdaCompliantCreditCurve baseCurve = _curveBuilder.calibrateCreditCurve(bucketCDSs, impSpreads, yieldCurve);
    double basePrice = _pricer.pv(cds, yieldCurve, baseCurve, cdsCoupon);
    double[] res = new double[n];
    for (int i = 0; i <= index; i++) { //don't bother calculating where there is no sensitivity 
      double[] bumpedSpreads = makeBumpedSpreads(impSpreads, fracBumpAmount, ShiftType.ABSOLUTE, i);
      IsdaCompliantCreditCurve bumpedCurve = _curveBuilder.calibrateCreditCurve(bucketCDSs, bumpedSpreads, yieldCurve);
      double price = _pricer.pv(cds, yieldCurve, bumpedCurve, cdsCoupon);
      res[i] = (price - basePrice) / fracBumpAmount;
    }
    return res;
  }

  /**
   * The difference in PV between two market spreads.
   * @param cds analytic description of a CDS traded at a certain time
   * @param cdsFracSpread The <b>fraction</b> spread of the CDS
   * @param priceType Clean or dirty price
   * @param yieldCurve The yield (or discount) curve
   * @param marketCDSs The market CDSs - these are the reference instruments used to build the credit curve
   * @param marketFracSpreads The <b>fractional</b> spreads of the market CDSs
   * @param fracDeltaSpreads Non-negative shifts
   * @param fdType The finite difference type (forward, central or backward)
   * @return The difference in PV between two market spreads
   */
  public double finiteDifferenceSpreadSensitivity(
      CdsAnalytic cds,
      double cdsFracSpread,
      CdsPriceType priceType,
      IsdaCompliantYieldCurve yieldCurve,
      CdsAnalytic[] marketCDSs,
      double[] marketFracSpreads,
      double[] fracDeltaSpreads,
      FiniteDifferenceType fdType) {

    ArgChecker.notNull(cds, "cds");
    ArgChecker.noNulls(marketCDSs, "curvePoints");
    ArgChecker.notEmpty(marketFracSpreads, "spreads");
    ArgChecker.notEmpty(fracDeltaSpreads, "deltaSpreads");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    ArgChecker.notNull(priceType, "priceType");

    int n = marketCDSs.length;
    ArgChecker.isTrue(n == marketFracSpreads.length, "speads length does not match curvePoints");
    ArgChecker.isTrue(n == fracDeltaSpreads.length, "deltaSpreads length does not match curvePoints");
    for (int i = 0; i < n; i++) {
      ArgChecker.isTrue(marketFracSpreads[i] > 0, "spreads must be positive");
      ArgChecker.isTrue(fracDeltaSpreads[i] >= 0, "deltaSpreads must none negative");
      ArgChecker.isTrue(fdType == FiniteDifferenceType.FORWARD || fracDeltaSpreads[i] < marketFracSpreads[i],
          "deltaSpread must be less spread, unless forward difference is used");
    }

    switch (fdType) {
      case CENTRAL:
        return fdCentral(cds, cdsFracSpread, marketCDSs, marketFracSpreads, fracDeltaSpreads, yieldCurve, priceType);
      case FORWARD:
        return fdForward(cds, cdsFracSpread, marketCDSs, marketFracSpreads, fracDeltaSpreads, yieldCurve, priceType);
      case BACKWARD:
        return fdBackwards(cds, cdsFracSpread, marketCDSs, marketFracSpreads, fracDeltaSpreads, yieldCurve, priceType);
      default:
        throw new IllegalArgumentException("unknown type " + fdType);
    }
  }

  private double fdCreditDV01(
      CdsAnalytic pricingCDS, double cdsSpread,
      CdsAnalytic[] curvePoints,
      double[] spreadsUp,
      double[] spreadsDown,
      IsdaCompliantYieldCurve yieldCurve,
      CdsPriceType priceType) {

    IsdaCompliantCreditCurve curveUp = _curveBuilder.calibrateCreditCurve(curvePoints, spreadsUp, yieldCurve);
    IsdaCompliantCreditCurve curveDown = _curveBuilder.calibrateCreditCurve(curvePoints, spreadsDown, yieldCurve);
    double up = _pricer.pv(pricingCDS, yieldCurve, curveUp, cdsSpread, priceType);
    double down = _pricer.pv(pricingCDS, yieldCurve, curveDown, cdsSpread, priceType);
    return up - down;
  }

  private double fdCentral(
      CdsAnalytic pricingCDS,
      double cdsSpread,
      CdsAnalytic[] curvePoints,
      double[] spreads,
      double[] deltaSpreads,
      IsdaCompliantYieldCurve yieldCurve,
      CdsPriceType priceType) {

    int n = curvePoints.length;
    double[] spreadUp = new double[n];
    double[] spreadDown = new double[n];
    for (int i = 0; i < n; i++) {
      spreadUp[i] = spreads[i] + deltaSpreads[i];
      spreadDown[i] = spreads[i] - deltaSpreads[i];
    }
    IsdaCompliantCreditCurve curveUp = _curveBuilder.calibrateCreditCurve(curvePoints, spreadUp, yieldCurve);
    IsdaCompliantCreditCurve curveDown = _curveBuilder.calibrateCreditCurve(curvePoints, spreadDown, yieldCurve);
    double up = _pricer.pv(pricingCDS, yieldCurve, curveUp, cdsSpread, priceType);
    double down = _pricer.pv(pricingCDS, yieldCurve, curveDown, cdsSpread, priceType);

    return up - down;
  }

  private double fdForward(
      CdsAnalytic pricingCDS,
      double cdsSpread,
      CdsAnalytic[] curvePoints,
      double[] spreads,
      double[] deltaSpreads,
      IsdaCompliantYieldCurve yieldCurve,
      CdsPriceType priceType) {

    int n = curvePoints.length;
    double[] spreadUp = new double[n];
    for (int i = 0; i < n; i++) {
      spreadUp[i] = spreads[i] + deltaSpreads[i];
    }
    IsdaCompliantCreditCurve curveUp = _curveBuilder.calibrateCreditCurve(curvePoints, spreadUp, yieldCurve);
    IsdaCompliantCreditCurve curveMid = _curveBuilder.calibrateCreditCurve(curvePoints, spreads, yieldCurve);
    double up = _pricer.pv(pricingCDS, yieldCurve, curveUp, cdsSpread, priceType);
    double mid = _pricer.pv(pricingCDS, yieldCurve, curveMid, cdsSpread, priceType);

    return up - mid;
  }

  private double fdBackwards(
      CdsAnalytic pricingCDS,
      double cdsSpread,
      CdsAnalytic[] curvePoints,
      double[] spreads,
      double[] deltaSpreads,
      IsdaCompliantYieldCurve yieldCurve,
      CdsPriceType priceType) {

    int n = curvePoints.length;
    double[] spreadDown = new double[n];
    for (int i = 0; i < n; i++) {
      spreadDown[i] = spreads[i] - deltaSpreads[i];
    }
    IsdaCompliantCreditCurve curveMid = _curveBuilder.calibrateCreditCurve(curvePoints, spreads, yieldCurve);
    IsdaCompliantCreditCurve curveDown = _curveBuilder.calibrateCreditCurve(curvePoints, spreadDown, yieldCurve);
    double mid = _pricer.pv(pricingCDS, yieldCurve, curveMid, cdsSpread, priceType);
    double down = _pricer.pv(pricingCDS, yieldCurve, curveDown, cdsSpread, priceType);

    return mid - down;
  }

  private double bumpedSpread(double spread, double amount, ShiftType shiftType) {
    return shiftType.applyShift(spread, amount);
  }

  private double[] makeBumpedSpreads(double[] spreads, double amount, ShiftType shiftType) {
    int n = spreads.length;
    double[] res = new double[n];

    if (shiftType == ShiftType.ABSOLUTE) {
      for (int i = 0; i < n; i++) {
        res[i] = spreads[i] + amount;
      }
    } else if (shiftType == ShiftType.RELATIVE) {
      double a = 1 + amount;
      for (int i = 0; i < n; i++) {
        res[i] = spreads[i] * a;
      }
    } else {
      throw new IllegalArgumentException("ShiftType " + shiftType + " is not supported");
    }
    return res;
  }

  /**
   * Bump a market quote by a specified amount, eps. If the quote is a spread type (ParSpread or QuotedSpread) the amount is simply bumped by given amount;
   * however if the quote is PointsUpFront this is first converted to a QuotedSpread, bumped by eps, then converted back to PointsUpFront 
   * @param cds analytic description of a CDS traded at a certain time
   * @param quote The market quote 
   * @param yieldCurve The yield curve
   * @param eps Bump amount (as a fraction, so a 1bps bumps is 1e-4)
   * @return The bumped quote 
   */
  public CdsQuoteConvention bumpQuote(CdsAnalytic cds, CdsQuoteConvention quote, IsdaCompliantYieldCurve yieldCurve, double eps) {
    if (quote instanceof CdsParSpread) {
      return new CdsParSpread(quote.getCoupon() + eps);
    } else if (quote instanceof CdsQuotedSpread) {
      CdsQuotedSpread qSpread = (CdsQuotedSpread) quote;
      return new CdsQuotedSpread(qSpread.getCoupon(), qSpread.getQuotedSpread() + eps);
    } else if (quote instanceof PointsUpFront) {
      PointsUpFront puf = (PointsUpFront) quote;
      double bumpedQSpread = _pufConverter.pufToQuotedSpread(cds, puf.getCoupon(), yieldCurve, puf.getPointsUpFront()) +
          eps;
      return new PointsUpFront(puf.getCoupon(), _pufConverter.quotedSpreadToPUF(cds, puf.getCoupon(), yieldCurve, bumpedQSpread));
    } else {
      throw new IllegalArgumentException("unknow type " + quote.getClass());
    }
  }

  /**
   * Bump a set of market quotes by a specified amount, eps. If an individual quote is a spread type (ParSpread or QuotedSpread) the amount is simply bumped 
   * by given amount; however if the quote is PointsUpFront this is first converted to a QuotedSpread, bumped by eps, then converted back to PointsUpFront. The 
   * set of quotes may be of mixed type.  
   * @param cds Set of analytic descriptions of  CDSs traded at a certain times
   * @param quotes The market quotes 
   * @param yieldCurve he yield curve
   * @param eps Bump amount (as a fraction, so a 1bps bumps is 1e-4)
   * @return The bumped quotes
   */
  public CdsQuoteConvention[] bumpQuotes(
      CdsAnalytic[] cds,
      CdsQuoteConvention[] quotes,
      IsdaCompliantYieldCurve yieldCurve,
      double eps) {

    int n = cds.length;
    CdsQuoteConvention[] res = new CdsQuoteConvention[n];
    for (int i = 0; i < n; i++) {
      res[i] = bumpQuote(cds[i], quotes[i], yieldCurve, eps);
    }
    return res;
  }

  private CdsQuoteConvention[] bumpQuoteAtIndex(
      CdsAnalytic[] cds,
      CdsQuoteConvention[] quotes,
      IsdaCompliantYieldCurve yieldCurve,
      double eps,
      int index) {

    int n = cds.length;
    CdsQuoteConvention[] res = new CdsQuoteConvention[n];
    System.arraycopy(quotes, 0, res, 0, n);
    res[index] = bumpQuote(cds[index], quotes[index], yieldCurve, eps);
    return res;
  }

  private double[] makeBumpedSpreads(double[] spreads, double amount, ShiftType shiftType, int index) {
    int n = spreads.length;
    double[] res = new double[n];
    System.arraycopy(spreads, 0, res, 0, n);

    switch (shiftType) {
      case ABSOLUTE:
        res[index] += amount;
        break;
      case RELATIVE:
        res[index] += res[index] * amount;
        break;
      default:
        throw new IllegalArgumentException("ShiftType " + shiftType + " is not supported");
    }
    return res;
  }
}
