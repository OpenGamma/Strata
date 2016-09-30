package com.opengamma.strata.pricer.credit.cds;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.node.CdsCurveNode;
import com.opengamma.strata.product.credit.cds.CdsCalibrationTrade;
import com.opengamma.strata.product.credit.cds.CdsQuote;
import com.opengamma.strata.product.credit.cds.ResolvedCdsTrade;
import com.opengamma.strata.product.credit.cds.type.CdsQuoteConvention;

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
    this.arbHandling = DEFAULT_ARBITRAGE_HANDLING;
    this.formula = DEFAULT_FORMULA;
  }

  protected IsdaCompliantCreditCurveCalibrator(AccrualOnDefaultFormulae formula) {
    this.arbHandling = DEFAULT_ARBITRAGE_HANDLING;
    this.formula = formula;
  }

  protected IsdaCompliantCreditCurveCalibrator(AccrualOnDefaultFormulae formula, ArbitrageHandling arbHandling) {
    this.arbHandling = arbHandling;
    this.formula = formula;
  }

  //-------------------------------------------------------------------------
  protected ArbitrageHandling getArbHanding() {
    return arbHandling;
  }

  protected AccrualOnDefaultFormulae getAccOnDefaultFormula() {
    return formula;
  }

  //-------------------------------------------------------------------------
  /**
   * Bootstrapper the credit curve from a single market CDS quote. Obviously the resulting credit (hazard)
   *  curve will be flat.
   *  
   * @param calibrationCDS The single market CDS - this is the reference instruments used to build the credit curve 
   * @param marketQuote The market quote of the CDS 
   * @param yieldCurve The yield (or discount) curve  
   * @return The credit curve  
   */
  public LegalEntitySurvivalProbabilities calibrate(
      ResolvedCdsTrade calibrationCDS, // TODO create and use CurveNode
      CdsQuote marketQuote, // TODO use MarketData
      CreditRatesProvider ratesProvider,       // TODO should contain discount curve and recovery rate curve
      ReferenceData refData) { // TODO use MarketData 
    double puf;
    double coupon;
    if (marketQuote.getQuoteConvention().equals(CdsQuoteConvention.PAR_SPREAD)) { // TODO simplify
      puf = 0.0;
      coupon = marketQuote.getQuotedValue();
    } else if (marketQuote.getQuoteConvention().equals(CdsQuoteConvention.QUOTED_SPREAD)) {
      puf = 0.0;
      coupon = marketQuote.getQuotedValue();
    } else if (marketQuote.getQuoteConvention().equals(CdsQuoteConvention.POINTS_UPFRONT)) {
      puf = marketQuote.getQuotedValue();
      coupon = calibrationCDS.getProduct().getFixedRate();
    } else {
      throw new IllegalArgumentException("Unknown CDSQuoteConvention type " + marketQuote.getClass());
    }

    return calibrate(new ResolvedCdsTrade[] {calibrationCDS}, new double[] {coupon}, ratesProvider, new double[] {puf}, refData);
  }

  public LegalEntitySurvivalProbabilities calibrate(
      List<CdsCurveNode> curveNode,
      DayCount curveDcc,
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

    // TODO valuation date match
    LocalDate valuationDate = marketData.getValuationDate();
    int nNodes = curveNode.size();
    double[] coupons = new double[nNodes];
    double[] pufs = new double[nNodes];

    ResolvedCdsTrade[] trades = new ResolvedCdsTrade[nNodes];

    CreditDiscountFactors discountFactors = ratesProvider.discountFactors(currency);
    RecoveryRates recoveryRates = ratesProvider.recoveryRates(legalEntityId);

    for (int i = 0; i < nNodes; i++) {
      CdsCalibrationTrade tradeCalibration = curveNode.get(i).trade(1d, marketData, refData);
      trades[i] = tradeCalibration.getUnderlyingTrade().resolve(refData);
      double[] temp =
          getStandardQuoteForm(trades[i], tradeCalibration.getQuote(), valuationDate, discountFactors, recoveryRates, refData);
      coupons[i] = temp[0];
      pufs[i] = temp[1];
    }

    InterpolatedNodalCurve nodalCurve = calibrate(trades, coupons, pufs, valuationDate, discountFactors, recoveryRates, refData);
    return LegalEntitySurvivalProbabilities.of(legalEntityId,
        IsdaCompliantZeroRateDiscountFactors.of(currency, valuationDate, nodalCurve));
  }

  /**
   * Bootstrapper the credit curve from a set of reference/calibration CDSs with market quotes 
   * @param calibrationCDSs The market CDSs - these are the reference instruments used to build the credit curve 
   * @param marketQuotes The market quotes of the CDSs 
   * @param yieldCurve The yield (or discount) curve 
   * @return The credit curve 
   */
  public LegalEntitySurvivalProbabilities calibrate(ResolvedCdsTrade[] calibrationCDSs,
      CdsQuote[] marketQuotes, CreditRatesProvider ratesProvider,
      ReferenceData refData) {
//    ArgumentChecker.noNulls(marketQuotes, "marketQuotes");
    final int n = marketQuotes.length;
    final double[] coupons = new double[n];
    final double[] pufs = new double[n];
    for (int i = 0; i < n; i++) {
      final double[] temp = getStandardQuoteForm(calibrationCDSs[i], marketQuotes[i], ratesProvider, refData);
      coupons[i] = temp[0];
      pufs[i] = temp[1];
    }
    return calibrate(calibrationCDSs, coupons, ratesProvider, pufs, refData);
  }

  /**
   * Bootstrapper the credit curve from a set of reference/calibration CDSs quoted with points up-front and standard premiums 
   * @param calibrationCDSs The market CDSs - these are the reference instruments used to build the credit curve 
   * @param premiums The standard premiums (coupons) as fractions (these are 0.01 or 0.05 in North America) 
   * @param yieldCurve  The yield (or discount) curve  
   * @param pointsUpfront points up-front as fractions of notional 
   * @return The credit curve
   */
  public abstract LegalEntitySurvivalProbabilities calibrate(ResolvedCdsTrade[] calibrationCDSs, double[] premiums,
      CreditRatesProvider ratesProvider, double[] pointsUpfront,
      ReferenceData refData);

  abstract InterpolatedNodalCurve calibrate(ResolvedCdsTrade[] calibrationCDSs, double[] premiums,
      double[] pointsUpfront, LocalDate valuationDate, CreditDiscountFactors discountFactors, RecoveryRates recoveryRates,
      ReferenceData refData);

  private double[] getStandardQuoteForm(ResolvedCdsTrade calibrationCDS, CdsQuote marketQuote,
      CreditRatesProvider ratesProvider, ReferenceData refData) {
    IsdaCdsProductPricer pricer = new IsdaCdsProductPricer(formula);

    double[] res = new double[2];
    if (marketQuote.getQuoteConvention().equals(CdsQuoteConvention.PAR_SPREAD)) {
      res[0] = marketQuote.getQuotedValue();
    } else if (marketQuote.getQuoteConvention().equals(CdsQuoteConvention.QUOTED_SPREAD)) {
//      CdsQuotedSpread temp = (CdsQuotedSpread) marketQuote;
//      double coupon = temp.getCoupon();
      double qSpread = marketQuote.getQuotedValue();
      LegalEntitySurvivalProbabilities cc =
          calibrate(new ResolvedCdsTrade[] {calibrationCDS}, new double[] {qSpread}, ratesProvider, new double[1], refData);
      CreditRatesProvider rates = ratesProvider.toBuilder()
          .creditCurves(ImmutableMap
              .of(Pair.of(calibrationCDS.getProduct().getLegalEntityId(), calibrationCDS.getProduct().getCurrency()), cc))
          .build();
      res[0] = calibrationCDS.getProduct().getFixedRate();
      res[1] = pricer.price(calibrationCDS.getProduct(), rates, calibrationCDS.getInfo().getSettlementDate().get(),
          PriceType.CLEAN, refData);
    } else if (marketQuote.getQuoteConvention().equals(CdsQuoteConvention.POINTS_UPFRONT)) {
//      final PointsUpFront temp = (PointsUpFront) marketQuote;
      res[0] = calibrationCDS.getProduct().getFixedRate();
      res[1] = marketQuote.getQuotedValue();
    } else {
      throw new IllegalArgumentException("Unknown CDSQuoteConvention type " + marketQuote.getClass());
    }
    return res;
  }

  private double[] getStandardQuoteForm(ResolvedCdsTrade calibrationCDS, CdsQuote marketQuote, LocalDate valuationDate,
      CreditDiscountFactors discountFactors, RecoveryRates recoveryRates, ReferenceData refData) {
    IsdaCdsProductPricer pricer = new IsdaCdsProductPricer(formula);

    double[] res = new double[2];
    if (marketQuote.getQuoteConvention().equals(CdsQuoteConvention.PAR_SPREAD)) {
      res[0] = marketQuote.getQuotedValue();
    } else if (marketQuote.getQuoteConvention().equals(CdsQuoteConvention.QUOTED_SPREAD)) {
//      CdsQuotedSpread temp = (CdsQuotedSpread) marketQuote;
//      double coupon = temp.getCoupon();
      double qSpread = marketQuote.getQuotedValue();
      InterpolatedNodalCurve cc =
          calibrate(new ResolvedCdsTrade[] {calibrationCDS}, new double[] {qSpread}, new double[1], valuationDate,
              discountFactors, recoveryRates, refData);
      Currency currency = calibrationCDS.getProduct().getCurrency();
      StandardId legalEntityId = calibrationCDS.getProduct().getLegalEntityId();
      CreditRatesProvider rates = CreditRatesProvider.builder()
          .valuationDate(null)
          .discountCurves(ImmutableMap.of(currency, discountFactors))
          .recoveryRateCurves(ImmutableMap.of(legalEntityId, recoveryRates))
          .creditCurves(
              ImmutableMap.of(
                  Pair.of(legalEntityId, currency),
                  LegalEntitySurvivalProbabilities.of(
                      legalEntityId,
                      IsdaCompliantZeroRateDiscountFactors.of(currency, valuationDate, cc))))
          .build();
      res[0] = calibrationCDS.getProduct().getFixedRate();
      res[1] = pricer.price(calibrationCDS.getProduct(), rates, calibrationCDS.getInfo().getSettlementDate().get(),
          PriceType.CLEAN, refData);
    } else if (marketQuote.getQuoteConvention().equals(CdsQuoteConvention.POINTS_UPFRONT)) {
//      final PointsUpFront temp = (PointsUpFront) marketQuote;
      res[0] = calibrationCDS.getProduct().getFixedRate();
      res[1] = marketQuote.getQuotedValue();
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
    IGNORE,
    /**
     * An exception is throw if an arbitrage is found
     */
    FAIL,
    /**
     * If a particular spread implies a negative forward hazard rate, the hazard rate is set to zero, and the calibration 
     * continues. The resultant curve will of course not exactly reprice the input CDSs, but will find new spreads that
     * just avoid arbitrage.   
     */
    ZERO_HAZARD_RATE
  }

}
