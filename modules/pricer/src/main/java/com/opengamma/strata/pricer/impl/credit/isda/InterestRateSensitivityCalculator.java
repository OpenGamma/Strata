/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import com.opengamma.strata.collect.ArgChecker;

/**
 * 
 */
public class InterestRateSensitivityCalculator {

  private static final MarketQuoteConverter CONVERTER = new MarketQuoteConverter();
  private static final double ONE_BPS = 1e-4;

  private final AnalyticCdsPricer _pricer;

  public InterestRateSensitivityCalculator() {
    _pricer = new AnalyticCdsPricer();
  }

  public InterestRateSensitivityCalculator(AccrualOnDefaultFormulae formula) {
    _pricer = new AnalyticCdsPricer(formula);
  }

  /**
   * The IR01 (Interest-Rate 01) is by definition the change in the price of a CDS when the
   * market interest rates (these are money-market and swap rates) all increased by 1bps.
   * This assumes that the quoted (or flat) spread is invariant to a change in the yield curve.
   * 
   * @param cds analytic description of a CDS traded at a certain time 
   * @param quote This can be a QuotedSpread or PointsUpFront. For quoted spread this is taken as invariant;
   *  for PUF this is converted to a quoted spread (which is then invariant). ParSpread is not supported 
   * @param yieldCurveBuilder yield curve builder 
   * @param marketRates the money-market and swap rates (in the correct order for the yield curve
   *  builder, i.e. in ascending order of maturity)
   * @return the parallel IR01 
   */
  public double parallelIR01(
      CdsAnalytic cds,
      CdsQuoteConvention quote,
      IsdaCompliantYieldCurveBuild yieldCurveBuilder,
      double[] marketRates) {

    ArgChecker.notNull(cds, "cds");
    ArgChecker.notNull(quote, "quote");
    ArgChecker.notNull(yieldCurveBuilder, "yieldCurveBuilder");
    ArgChecker.notEmpty(marketRates, "marketRates");
    IsdaCompliantYieldCurve ycUP = bumpYieldCurve(yieldCurveBuilder, marketRates, ONE_BPS);
    IsdaCompliantYieldCurve yc = yieldCurveBuilder.build(marketRates);

    if (quote instanceof CdsQuotedSpread) {
      CdsQuotedSpread qs = (CdsQuotedSpread) quote;
      double puf = CONVERTER.quotedSpreadToPUF(cds, qs.getCoupon(), yc, qs.getQuotedSpread());
      double pufUp = CONVERTER.quotedSpreadToPUF(cds, qs.getCoupon(), ycUP, qs.getQuotedSpread());
      return pufUp - puf;
    } else if (quote instanceof PointsUpFront) {
      CdsQuotedSpread qs = CONVERTER.convert(cds, (PointsUpFront) quote, yc);
      PointsUpFront pufUp = CONVERTER.convert(cds, qs, ycUP);
      return pufUp.getPointsUpFront() - ((PointsUpFront) quote).getPointsUpFront();
    } else if (quote instanceof CdsParSpread) {
      throw new UnsupportedOperationException(
          "This type of claculation don't make sense for par spreads. Use a fixed credit curve method.");
    } else {
      throw new IllegalArgumentException("Unknown quote type: " + quote.getClass());
    }
  }

  /**
   * The IR01 (Interest-Rate 01) is by definition the change in the price of a CDS when the
   * market interest rates (these are money-market and swap rates) all increased by 1bps.
   * This assumes that the quoted (or flat) spread is invariant to a change in the yield curve.
   * In addition the bumps are applied directly to the yield curve and NOT the instruments.
   * 
   * @param cds analytic description of a CDS traded at a certain time 
   * @param quote This can be a QuotedSpread or PointsUpFront. For quoted spread this is taken as invariant;
   *  for PUF this is converted to a quoted spread (which is then invariant). ParSpread is not supported 
   * @param yieldCurve yield curve 
   * @param marketRates the money-market and swap rates (in the correct order for the yield curve
   *  builder, i.e. in ascending order of maturity)
   * @return the parallel IR01 
   */
  public double parallelIR01(
      CdsAnalytic cds,
      CdsQuoteConvention quote,
      IsdaCompliantYieldCurve yieldCurve,
      double[] marketRates) {

    ArgChecker.notNull(cds, "cds");
    ArgChecker.notNull(quote, "quote");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    ArgChecker.notEmpty(marketRates, "marketRates");
    IsdaCompliantYieldCurve ycUP = bumpYieldCurve(yieldCurve, ONE_BPS);

    if (quote instanceof CdsQuotedSpread) {
      CdsQuotedSpread qs = (CdsQuotedSpread) quote;
      double puf = CONVERTER.quotedSpreadToPUF(cds, qs.getCoupon(), yieldCurve, qs.getQuotedSpread());
      double pufUp = CONVERTER.quotedSpreadToPUF(cds, qs.getCoupon(), ycUP, qs.getQuotedSpread());
      return pufUp - puf;
    } else if (quote instanceof PointsUpFront) {
      CdsQuotedSpread qs = CONVERTER.convert(cds, (PointsUpFront) quote, yieldCurve);
      PointsUpFront pufUp = CONVERTER.convert(cds, qs, ycUP);
      return pufUp.getPointsUpFront() - ((PointsUpFront) quote).getPointsUpFront();
    } else if (quote instanceof CdsParSpread) {
      throw new UnsupportedOperationException(
          "This type of claculation don't make sense for par spreads. Use a fixed credit curve method.");
    } else {
      throw new IllegalArgumentException("Unknown quote type: " + quote.getClass());
    }
  }

  /**
   * The IR01 (Interest-Rate 01) is by definition the change in the price of a CDS when the
   * market interest rates (these are money-market and swap rates) all increased by 1bps.
   * This assumes that the credit curve is invariant.
   * 
   * @param cds analytic description of a CDS traded at a certain time 
   * @param coupon The cds's coupon (as a fraction)
   * @param creditCurve the credit (or survival) curve 
   * @param yieldCurveBuilder yield curve builder 
   * @param marketRates the money-market and swap rates (in the correct order for the yield
   *  curve builder, i.e. in ascending order of maturity)
   * @return the parallel IR01 
   */
  public double parallelIR01(
      CdsAnalytic cds,
      double coupon,
      IsdaCompliantCreditCurve creditCurve,
      IsdaCompliantYieldCurveBuild yieldCurveBuilder,
      double[] marketRates) {

    ArgChecker.notNull(cds, "cds");
    ArgChecker.notNull(creditCurve, "creditCurve");
    ArgChecker.notNull(yieldCurveBuilder, "yieldCurveBuilder");
    ArgChecker.notEmpty(marketRates, "marketRates");
    IsdaCompliantYieldCurve ycUP = bumpYieldCurve(yieldCurveBuilder, marketRates, ONE_BPS);
    IsdaCompliantYieldCurve yc = yieldCurveBuilder.build(marketRates);
    return priceDiff(cds, creditCurve, coupon, ycUP, yc);
  }

  /**
   * The IR01 (Interest-Rate 01) is by definition the change in the price of a CDS when the
   * yield curve is bumped by 1bps.
   * <p>
   * Note, this bumps the yield curve not the underlying instrument market data.
   * See methods which take a {@code ISDACompliantYieldCurveBuild}
   * for bumping of the underlying instruments.
   *
   * @param cds analytic description of a CDS traded at a certain time
   * @param coupon The cds's coupon (as a fraction)
   * @param creditCurve the credit (or survival) curve
   * @param yieldCurve yield curve
   * @return the parallel IR01
   */
  public double parallelIR01(
      CdsAnalytic cds,
      double coupon,
      IsdaCompliantCreditCurve creditCurve,
      IsdaCompliantYieldCurve yieldCurve) {

    ArgChecker.notNull(cds, "cds");
    ArgChecker.notNull(creditCurve, "creditCurve");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    IsdaCompliantYieldCurve ycUP = bumpYieldCurve(yieldCurve, ONE_BPS);
    return priceDiff(cds, creditCurve, coupon, ycUP, yieldCurve);
  }

  /**
   * The bucketed IR01 (Interest-Rate 01) is by definition the vector of changes in the
   * price of a CDS when the market interest rates
   * (these are money-market and swap rates) increased by 1bps in turn.
   * 
   * @param cds analytic description of a CDS traded at a certain time 
   * @param coupon The cds's coupon (as a fraction)
   * @param creditCurve the credit (or survival) curve 
   * @param yieldCurveBuilder yield curve builder 
   * @param marketRates the money-market and swap rates (in the correct order for the yield
   *  curve builder, i.e. in ascending order of maturity)
   * @return the bucketed IR01 
   */
  public double[] bucketedIR01(
      CdsAnalytic cds,
      double coupon,
      IsdaCompliantCreditCurve creditCurve,
      IsdaCompliantYieldCurveBuild yieldCurveBuilder,
      double[] marketRates) {

    ArgChecker.notNull(cds, "cds");
    ArgChecker.notNull(creditCurve, "creditCurve");
    ArgChecker.notNull(yieldCurveBuilder, "yieldCurveBuilder");
    ArgChecker.notEmpty(marketRates, "marketRates");
    IsdaCompliantYieldCurve baseYC = yieldCurveBuilder.build(marketRates);
    int n = marketRates.length;
    IsdaCompliantYieldCurve[] bumpedYC = new IsdaCompliantYieldCurve[n];
    for (int i = 0; i < n; i++) {
      bumpedYC[i] = bumpYieldCurve(yieldCurveBuilder, marketRates, ONE_BPS, i);
    }
    return priceDiff(cds, creditCurve, coupon, bumpedYC, baseYC);
  }

  /**
   * The bucketed IR01 (Interest-Rate 01) is by definition the vector of changes in the
   * price of a CDS when the points on the yield curve are bumped.
   *
   * Note, this bumps the yield curve not the underlying instrument market data.
   * See methods which take a {@code ISDACompliantYieldCurveBuild}
   * for bumping of the underlying instruments.
   *
   * @param cds analytic description of a CDS traded at a certain time
   * @param coupon The cds's coupon (as a fraction)
   * @param creditCurve the credit (or survival) curve
   * @param yieldCurve yield curve
   * @return the bucketed IR01
   */
  public double[] bucketedIR01(
      CdsAnalytic cds,
      double coupon,
      IsdaCompliantCreditCurve creditCurve,
      IsdaCompliantYieldCurve yieldCurve) {

    ArgChecker.notNull(cds, "cds");
    ArgChecker.notNull(creditCurve, "creditCurve");
    ArgChecker.notNull(yieldCurve, "yieldCurve");
    IsdaCompliantYieldCurve baseYC = yieldCurve;
    int n = yieldCurve.getNumberOfKnots();
    IsdaCompliantYieldCurve[] bumpedYC = new IsdaCompliantYieldCurve[n];
    for (int i = 0; i < n; i++) {
      bumpedYC[i] = bumpYieldCurve(yieldCurve, ONE_BPS, i);
    }
    return priceDiff(cds, creditCurve, coupon, bumpedYC, baseYC);
  }

  private double priceDiff(
      CdsAnalytic cds,
      IsdaCompliantCreditCurve creditCurve,
      double coupon,
      IsdaCompliantYieldCurve yc1,
      IsdaCompliantYieldCurve yc2) {

    double pv1 = _pricer.pv(cds, yc1, creditCurve, coupon, CdsPriceType.DIRTY);
    double pv2 = _pricer.pv(cds, yc2, creditCurve, coupon, CdsPriceType.DIRTY);
    return pv1 - pv2;
  }

  private double[] priceDiff(
      CdsAnalytic cds,
      IsdaCompliantCreditCurve creditCurve,
      double coupon,
      IsdaCompliantYieldCurve[] bumpedYC,
      IsdaCompliantYieldCurve baseYC) {

    double basePV = _pricer.pv(cds, baseYC, creditCurve, coupon, CdsPriceType.DIRTY);
    int n = bumpedYC.length;
    double[] res = new double[n];
    for (int i = 0; i < n; i++) {
      double pv = _pricer.pv(cds, bumpedYC[i], creditCurve, coupon, CdsPriceType.DIRTY);
      res[i] = pv - basePV;
    }
    return res;
  }

  private IsdaCompliantYieldCurve bumpYieldCurve(IsdaCompliantYieldCurveBuild builder, double[] rates, double bumpAmount) {
    int n = rates.length;
    double[] bumped = new double[n];
    System.arraycopy(rates, 0, bumped, 0, n);
    for (int i = 0; i < n; i++) {
      bumped[i] += bumpAmount;
    }
    return builder.build(bumped);
  }

  private IsdaCompliantYieldCurve bumpYieldCurve(IsdaCompliantYieldCurve curve, double bumpAmount) {
    int n = curve.getNumberOfKnots();
    double[] bumped = curve.getKnotZeroRates();
    for (int i = 0; i < n; i++) {
      bumped[i] += bumpAmount;
    }
    return curve.withRates(bumped);
  }

  private IsdaCompliantYieldCurve bumpYieldCurve(
      IsdaCompliantYieldCurveBuild builder,
      double[] rates,
      double bumpAmount,
      int index) {

    int n = rates.length;
    double[] bumped = new double[n];
    System.arraycopy(rates, 0, bumped, 0, n);
    bumped[index] += bumpAmount;
    return builder.build(bumped);
  }

  private IsdaCompliantYieldCurve bumpYieldCurve(IsdaCompliantYieldCurve curve, double bumpAmount, int index) {
    return curve.withRate(curve.getZeroRateAtIndex(index) + bumpAmount, index);
  }

}
