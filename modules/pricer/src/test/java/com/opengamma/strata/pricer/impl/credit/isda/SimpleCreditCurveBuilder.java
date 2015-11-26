/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import java.time.LocalDate;
import java.time.Period;
import java.util.function.Function;

import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.rootfinding.BracketRoot;
import com.opengamma.strata.math.impl.rootfinding.BrentSingleRootFinder;
import com.opengamma.strata.math.impl.rootfinding.RealSingleRootFinder;

/**
 * This is a bootstrapper for the credit curve that is consistent with ISDA in that it will produce the same curve from
 * the same inputs (up to numerical round-off).
 */
public class SimpleCreditCurveBuilder extends IsdaCompliantCreditCurveBuilder {
  // NOTE: class was moved from main area to test area
  // was deprecated in favour of ISDACompliantCreditCurveBuild

  private static final BracketRoot BRACKER = new BracketRoot();
  private static final RealSingleRootFinder ROOTFINDER = new BrentSingleRootFinder();

  private final AnalyticCdsPricer _pricer;

  public SimpleCreditCurveBuilder() {
    _pricer = new AnalyticCdsPricer();
  }

  public SimpleCreditCurveBuilder(final AccrualOnDefaultFormulae formula) {
    super(formula);
    _pricer = new AnalyticCdsPricer(formula);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IsdaCompliantCreditCurve calibrateCreditCurve(final CdsAnalytic cds, final double premium, final IsdaCompliantYieldCurve yieldCurve, final double pointsUpfront) {
    return calibrateCreditCurve(new CdsAnalytic[] {cds }, new double[] {premium }, yieldCurve, new double[] {pointsUpfront });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IsdaCompliantCreditCurve calibrateCreditCurve(final CdsAnalytic cds, final double marketFractionalSpread, final IsdaCompliantYieldCurve yieldCurve) {
    return calibrateCreditCurve(new CdsAnalytic[] {cds }, new double[] {marketFractionalSpread }, yieldCurve, new double[1]);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IsdaCompliantCreditCurve calibrateCreditCurve(final CdsAnalytic[] cds, final double[] fractionalSpreads, final IsdaCompliantYieldCurve yieldCurve) {
    ArgChecker.notNull(cds, "cds");
    final int n = cds.length;
    return calibrateCreditCurve(cds, fractionalSpreads, yieldCurve, new double[n]);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IsdaCompliantCreditCurve calibrateCreditCurve(final CdsAnalytic[] cds, final double[] premiums, final IsdaCompliantYieldCurve yieldCurve, final double[] pointsUpfront) {
    ArgChecker.noNulls(cds, "cds");
    ArgChecker.notEmpty(premiums, "empty fractionalSpreads");
    ArgChecker.notNull(yieldCurve, "null yieldCurve");
    final int n = cds.length;
    ArgChecker.isTrue(n == premiums.length, "Number of CDSs does not match number of spreads");
    final double proStart = cds[0].getEffectiveProtectionStart();
    for (int i = 1; i < n; i++) {
      ArgChecker.isTrue(proStart == cds[i].getEffectiveProtectionStart(), "all CDSs must has same protection start");
      ArgChecker.isTrue(cds[i].getProtectionEnd() > cds[i - 1].getProtectionEnd(), "protection end must be ascending");
    }

    // use continuous premiums as initial guess
    final double[] guess = new double[n];
    final double[] t = new double[n];
    for (int i = 0; i < n; i++) {
      guess[i] = premiums[i] / cds[i].getLGD();
      t[i] = cds[i].getProtectionEnd();
    }

    IsdaCompliantCreditCurve creditCurve = new IsdaCompliantCreditCurve(t, guess);
    for (int i = 0; i < n; i++) {
      final CDSPricer func = new CDSPricer(i, cds[i], premiums[i], creditCurve, yieldCurve, pointsUpfront[i]);
      final double[] bracket = BRACKER.getBracketedPoints(func, 0.8 * guess[i], 1.25 * guess[i], 0.0, Double.POSITIVE_INFINITY);
      final double zeroRate = ROOTFINDER.getRoot(func, bracket[0], bracket[1]);
      creditCurve = creditCurve.withRate(zeroRate, i);
    }

    return creditCurve;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IsdaCompliantCreditCurve calibrateCreditCurve(final LocalDate today, final LocalDate stepinDate, final LocalDate valueDate, final LocalDate startDate, final LocalDate endDate,
      final double fractionalParSpread, final boolean payAccOnDefault, final Period tenor, final StubConvention stubType, final boolean protectStart, final IsdaCompliantYieldCurve yieldCurve,
      final double recoveryRate) {
    return calibrateCreditCurve(today, stepinDate, valueDate, startDate, new LocalDate[] {endDate }, new double[] {fractionalParSpread }, payAccOnDefault, tenor, stubType, protectStart, yieldCurve,
        recoveryRate);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IsdaCompliantCreditCurve calibrateCreditCurve(final LocalDate today, final LocalDate stepinDate, final LocalDate valueDate, final LocalDate startDate, final LocalDate[] endDates,
      final double[] couponRates, final boolean payAccOnDefault, final Period tenor, final StubConvention stubType, final boolean protectStart, final IsdaCompliantYieldCurve yieldCurve,
      final double recoveryRate) {

    ArgChecker.notNull(today, "null today");
    ArgChecker.notNull(stepinDate, "null stepinDate");
    ArgChecker.notNull(valueDate, "null valueDate");
    ArgChecker.notNull(startDate, "null startDate");
    ArgChecker.noNulls(endDates, "null endDates");
    ArgChecker.notEmpty(couponRates, "no or null couponRates");
    ArgChecker.notNull(tenor, "null tenor");
    ArgChecker.notNull(stubType, "null stubType");
    ArgChecker.notNull(yieldCurve, "null yieldCurve");
    ArgChecker.inRange(recoveryRate, 0d, 1d, "recoveryRate");
    ArgChecker.isFalse(valueDate.isBefore(today), "Require valueDate >= today");
    ArgChecker.isFalse(stepinDate.isBefore(today), "Require stepin >= today");

    final int n = endDates.length;
    ArgChecker.isTrue(n == couponRates.length, "length of couponRates does not match endDates");

    final CdsAnalytic[] cds = new CdsAnalytic[n];
    for (int i = 0; i < n; i++) {
      cds[i] = new CdsAnalytic(today, stepinDate, valueDate, startDate, endDates[i], payAccOnDefault, tenor, stubType, protectStart, recoveryRate);
    }

    return calibrateCreditCurve(cds, couponRates, yieldCurve);
  }

  private class CDSPricer implements Function<Double, Double> {

    private final int _index;
    private final CdsAnalytic _cds;
    private final IsdaCompliantCreditCurve _creditCurve;
    private final IsdaCompliantYieldCurve _yieldCurve;
    private final double _spread;
    private final double _pointsUpfront;

    public CDSPricer(final int index, final CdsAnalytic cds, final double fracSpread, final IsdaCompliantCreditCurve creditCurve, final IsdaCompliantYieldCurve yieldCurve, final double pointsUpfront) {

      _index = index;
      _cds = cds;
      _yieldCurve = yieldCurve;
      _creditCurve = creditCurve;
      _spread = fracSpread;
      _pointsUpfront = pointsUpfront;
    }

    @Override
    public Double apply(final Double x) {
      final IsdaCompliantCreditCurve cc = _creditCurve.withRate(x, _index);
      return _pricer.pv(_cds, _yieldCurve, cc, _spread) - _pointsUpfront;
    }
  }

  //TODO
  @Override
  public IsdaCompliantCreditCurve calibrateCreditCurve(final CdsAnalytic calibrationCDS, final CdsQuoteConvention marketQuote, final IsdaCompliantYieldCurve yieldCurve) {
    double puf;
    double coupon;
    if (marketQuote instanceof CdsParSpread) {
      puf = 0.0;
      coupon = marketQuote.getCoupon();
    } else if (marketQuote instanceof CdsQuotedSpread) {
      puf = 0.0;
      coupon = ((CdsQuotedSpread) marketQuote).getQuotedSpread();
    } else if (marketQuote instanceof PointsUpFront) {
      final PointsUpFront temp = (PointsUpFront) marketQuote;
      puf = temp.getPointsUpFront();
      coupon = temp.getCoupon();
    } else {
      throw new IllegalArgumentException("Unknown CDSQuoteConvention type " + marketQuote.getClass());
    }

    return calibrateCreditCurve(new CdsAnalytic[] {calibrationCDS }, new double[] {coupon }, yieldCurve, new double[] {puf });
  }

  @Override
  public IsdaCompliantCreditCurve calibrateCreditCurve(final CdsAnalytic[] calibrationCDSs, final CdsQuoteConvention[] marketQuotes, final IsdaCompliantYieldCurve yieldCurve) {
    throw new UnsupportedOperationException();
  }

}
