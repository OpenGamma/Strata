/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit.cds;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.curve.node.CdsCurveNode;
import com.opengamma.strata.product.credit.cds.CdsCalibrationTrade;
import com.opengamma.strata.product.credit.cds.CdsQuote;
import com.opengamma.strata.product.credit.cds.ResolvedCdsTrade;
import com.opengamma.strata.product.credit.cds.type.CdsQuoteConvention;

/**
 * ISDA compliant credit curve calibrator.
 * <p>
 * A single credit curve is calibrated for credit default swaps on a legal entity.
 * <p>
 * The curve is defined using two or more {@linkplain CdsCurveNode nodes}.
 * Each node primarily defines enough information to produce a reference CDS trade.
 * All of the curve nodes must be based on a common legal entity and currency.
 * <p>
 * Calibration involves pricing, and re-pricing, these trades to find the best fit using a root finder.
 * Relevant discount curve and recovery rate curve are required to complete the calibration.
 */
public abstract class IsdaCompliantCreditCurveCalibrator {

  /**
   * Default arbitrage handling.
   */
  private static final ArbitrageHandling DEFAULT_ARBITRAGE_HANDLING = ArbitrageHandling.IGNORE;
  /**
   * Default pricing formula.
   */
  private static final AccrualOnDefaultFormulae DEFAULT_FORMULA = AccrualOnDefaultFormulae.ORIGINAL_ISDA;

  /**
   * The arbitrage handling.
   */
  private final ArbitrageHandling arbHandling;
  /**
   * The pricing formula.
   */
  private final AccrualOnDefaultFormulae formula;

  //-------------------------------------------------------------------------
  protected IsdaCompliantCreditCurveCalibrator() {
    this(DEFAULT_FORMULA, DEFAULT_ARBITRAGE_HANDLING);
  }

  protected IsdaCompliantCreditCurveCalibrator(AccrualOnDefaultFormulae formula) {
    this(formula, DEFAULT_ARBITRAGE_HANDLING);
  }

  protected IsdaCompliantCreditCurveCalibrator(AccrualOnDefaultFormulae formula, ArbitrageHandling arbHandling) {
    this.arbHandling = ArgChecker.notNull(arbHandling, "arbHandling");
    this.formula = ArgChecker.notNull(formula, "formula");
  }

  //-------------------------------------------------------------------------
  protected ArbitrageHandling getArbHanding() {
    return arbHandling;
  }

  protected AccrualOnDefaultFormulae getAccOnDefaultFormula() {
    return formula;
  }

//  //-------------------------------------------------------------------------
//  public LegalEntitySurvivalProbabilities calibrate(
//      ResolvedCdsTrade calibrationCDS, // TODO create and use CurveNode
//      CdsQuote marketQuote, // TODO use MarketData
//      CreditRatesProvider ratesProvider,       // TODO should contain discount curve and recovery rate curve
//      ReferenceData refData) { // TODO use MarketData 
//    double puf;
//    double coupon;
//    if (marketQuote.getQuoteConvention().equals(CdsQuoteConvention.PAR_SPREAD)) { // TODO simplify
//      puf = 0.0;
//      coupon = marketQuote.getQuotedValue();
//    } else if (marketQuote.getQuoteConvention().equals(CdsQuoteConvention.QUOTED_SPREAD)) {
//      puf = 0.0;
//      coupon = marketQuote.getQuotedValue();
//    } else if (marketQuote.getQuoteConvention().equals(CdsQuoteConvention.POINTS_UPFRONT)) {
//      puf = marketQuote.getQuotedValue();
//      coupon = calibrationCDS.getProduct().getFixedRate();
//    } else {
//      throw new IllegalArgumentException("Unknown CDSQuoteConvention type " + marketQuote.getClass());
//    }
//
//    return calibrate(new ResolvedCdsTrade[] {calibrationCDS}, new double[] {coupon}, ratesProvider, new double[] {puf}, refData);
//  }
//
//  public LegalEntitySurvivalProbabilities calibrate(ResolvedCdsTrade[] calibrationCDSs,
//      CdsQuote[] marketQuotes, CreditRatesProvider ratesProvider,
//      ReferenceData refData) {
////    ArgumentChecker.noNulls(marketQuotes, "marketQuotes");
//    final int n = marketQuotes.length;
//    final double[] coupons = new double[n];
//    final double[] pufs = new double[n];
//    for (int i = 0; i < n; i++) {
//      final double[] temp = getStandardQuoteForm(calibrationCDSs[i], marketQuotes[i], ratesProvider, refData);
//      coupons[i] = temp[0];
//      pufs[i] = temp[1];
//    }
//    return calibrate(calibrationCDSs, coupons, ratesProvider, pufs, refData);
//  }
//
//  public abstract LegalEntitySurvivalProbabilities calibrate(ResolvedCdsTrade[] calibrationCDSs, double[] premiums,
//      CreditRatesProvider ratesProvider, double[] pointsUpfront,
//      ReferenceData refData);
//
//  private double[] getStandardQuoteForm(ResolvedCdsTrade calibrationCDS, CdsQuote marketQuote,
//      CreditRatesProvider ratesProvider, ReferenceData refData) {
//    IsdaCdsProductPricer pricer = new IsdaCdsProductPricer(formula);
//
//    double[] res = new double[2];
//    if (marketQuote.getQuoteConvention().equals(CdsQuoteConvention.PAR_SPREAD)) {
//      res[0] = marketQuote.getQuotedValue();
//    } else if (marketQuote.getQuoteConvention().equals(CdsQuoteConvention.QUOTED_SPREAD)) {
////      CdsQuotedSpread temp = (CdsQuotedSpread) marketQuote;
////      double coupon = temp.getCoupon();
//      double qSpread = marketQuote.getQuotedValue();
//      LegalEntitySurvivalProbabilities cc =
//          calibrate(new ResolvedCdsTrade[] {calibrationCDS}, new double[] {qSpread}, ratesProvider, new double[1], refData);
//      CreditRatesProvider rates = ratesProvider.toBuilder()
//          .creditCurves(ImmutableMap
//              .of(Pair.of(calibrationCDS.getProduct().getLegalEntityId(), calibrationCDS.getProduct().getCurrency()), cc))
//          .build();
//      res[0] = calibrationCDS.getProduct().getFixedRate();
//      res[1] = pricer.price(calibrationCDS.getProduct(), rates, calibrationCDS.getInfo().getSettlementDate().get(),
//          PriceType.CLEAN, refData);
//    } else if (marketQuote.getQuoteConvention().equals(CdsQuoteConvention.POINTS_UPFRONT)) {
////      final PointsUpFront temp = (PointsUpFront) marketQuote;
//      res[0] = calibrationCDS.getProduct().getFixedRate();
//      res[1] = marketQuote.getQuotedValue();
//    } else {
//      throw new IllegalArgumentException("Unknown CDSQuoteConvention type " + marketQuote.getClass());
//    }
//    return res;
//  }

  //-------------------------------------------------------------------------
  /**
   * Calibrates the ISDA compliant credit curve to the market data.
   * <p>
   * This creates the single credit curve for a legal entity.
   * <p>
   * The relevant discount curve and recovery rate curve must be stored in {@code ratesProvider}. 
   * The day count convention for the resulting credit curve is the same as that of the discount curve.
   * 
   * @param curveNode  the curve node
   * @param curveDcc  the curve day count
   * @param name  the curve name
   * @param marketData  the market data
   * @param ratesProvider  the rates provider
   * @param refData  the reference data
   * @return the ISDA compliant credit curve
   */
  public LegalEntitySurvivalProbabilities calibrate(
      List<CdsCurveNode> curveNode,
      CurveName name,
      MarketData marketData,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    Iterator<StandardId> legalEntities =
        curveNode.stream().map(CdsCurveNode::getLegalEntityId).collect(Collectors.toSet()).iterator();
    StandardId legalEntityId = legalEntities.next();
    ArgChecker.isFalse(legalEntities.hasNext(), "legal entity must be common to curve nodes");
    Iterator<Currency> currencies =
        curveNode.stream().map(n -> n.getTemplate().getConvention().getCurrency()).collect(Collectors.toSet()).iterator();
    Currency currency = currencies.next();
    ArgChecker.isFalse(currencies.hasNext(), "currency must be common to curve nodes");
    LocalDate valuationDate = marketData.getValuationDate();
    ArgChecker.isTrue(valuationDate.equals(marketData.getValuationDate()),
        "ratesProvider and marketDate must be based on the same valuation date");
    CreditDiscountFactors discountFactors = ratesProvider.discountFactors(currency);
    RecoveryRates recoveryRates = ratesProvider.recoveryRates(legalEntityId);

    int nNodes = curveNode.size();
    double[] coupons = new double[nNodes];
    double[] pufs = new double[nNodes];
    ResolvedCdsTrade[] trades = new ResolvedCdsTrade[nNodes];
    for (int i = 0; i < nNodes; i++) {
      CdsCalibrationTrade tradeCalibration = curveNode.get(i).trade(1d, marketData, refData);
      trades[i] = tradeCalibration.getUnderlyingTrade().resolve(refData);
      double[] temp =
          getStandardQuoteForm(trades[i], tradeCalibration.getQuote(), valuationDate, discountFactors, recoveryRates, refData);
      coupons[i] = temp[0];
      pufs[i] = temp[1];
    }

    NodalCurve nodalCurve =
        calibrate(trades, coupons, pufs, name, valuationDate, discountFactors, recoveryRates, refData);
    return LegalEntitySurvivalProbabilities.of(
        legalEntityId, IsdaCompliantZeroRateDiscountFactors.of(currency, valuationDate, nodalCurve));
  }

  abstract NodalCurve calibrate(ResolvedCdsTrade[] calibrationCDSs, double[] flactionalSpreads,
      double[] pointsUpfront, CurveName name, LocalDate valuationDate, CreditDiscountFactors discountFactors,
      RecoveryRates recoveryRates, ReferenceData refData);

  private double[] getStandardQuoteForm(ResolvedCdsTrade calibrationCDS, CdsQuote marketQuote, LocalDate valuationDate,
      CreditDiscountFactors discountFactors, RecoveryRates recoveryRates, ReferenceData refData) {
    IsdaCdsProductPricer pricer = new IsdaCdsProductPricer(formula);

    double[] res = new double[2];
    if (marketQuote.getQuoteConvention().equals(CdsQuoteConvention.PAR_SPREAD)) {
      res[0] = marketQuote.getQuotedValue();
    } else if (marketQuote.getQuoteConvention().equals(CdsQuoteConvention.QUOTED_SPREAD)) {
      double qSpread = marketQuote.getQuotedValue();
      NodalCurve tempCreditCurve =
          calibrate(new ResolvedCdsTrade[] {calibrationCDS}, new double[] {qSpread}, new double[1], CurveName.of("temp"),
              valuationDate, discountFactors, recoveryRates, refData);
      Currency currency = calibrationCDS.getProduct().getCurrency();
      StandardId legalEntityId = calibrationCDS.getProduct().getLegalEntityId();
      CreditRatesProvider rates = CreditRatesProvider.builder()
          .valuationDate(valuationDate)
          .discountCurves(ImmutableMap.of(currency, discountFactors))
          .recoveryRateCurves(ImmutableMap.of(legalEntityId, recoveryRates))
          .creditCurves(
              ImmutableMap.of(
                  Pair.of(legalEntityId, currency),
                  LegalEntitySurvivalProbabilities.of(
                      legalEntityId,
                      IsdaCompliantZeroRateDiscountFactors.of(currency, valuationDate, tempCreditCurve))))
          .build();
      res[0] = calibrationCDS.getProduct().getFixedRate();
      res[1] = pricer.price(calibrationCDS.getProduct(), rates, calibrationCDS.getInfo().getSettlementDate().get(),
          PriceType.CLEAN, refData);
    } else if (marketQuote.getQuoteConvention().equals(CdsQuoteConvention.POINTS_UPFRONT)) {
      res[0] = calibrationCDS.getProduct().getFixedRate();
      res[1] = marketQuote.getQuotedValue();
    } else {
      throw new IllegalArgumentException("Unknown CDSQuoteConvention type " + marketQuote.getClass());
    }
    return res;
  }

  /**
   * How should any arbitrage in the input data be handled. 
   */
  enum ArbitrageHandling {
    /**
     * Ignore.
     * <p>
     * If the market data has arbitrage, the curve will still build. 
     * The survival probability will not be monotonically decreasing 
     * (equivalently, some forward hazard rates will be negative). 
     */
    IGNORE,
    /**
     * Fail.
     * <p>
     * An exception is thrown if an arbitrage is found. 
     */
    FAIL,
    /**
     * Zero hazard rate.
     * <p>
     * If a particular spread implies a negative forward hazard rate, 
     * the hazard rate is set to zero, and the calibration continues. 
     * The resultant curve will not exactly reprice the input CDSs, but will find new spreads that just avoid arbitrage.   
     */
    ZERO_HAZARD_RATE
  }

}
