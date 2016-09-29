package com.opengamma.strata.pricer.credit.cds;

import java.time.LocalDate;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.product.credit.cds.CdsQuote;
import com.opengamma.strata.product.credit.cds.ResolvedCdsTrade;
import com.opengamma.strata.product.credit.cds.type.CdsQuoteConvention;

public abstract class IsdaCompliantCreditCurveCalibrator {

  private static final ArbitrageHandling DEFAULT_ARBITRAGE_HANDLING = ArbitrageHandling.Ignore;
  private static final AccrualOnDefaultFormulae DEFAULT_FORMULA = AccrualOnDefaultFormulae.ORIGINAL_ISDA;

  private final ArbitrageHandling _arbHandling;
  private final AccrualOnDefaultFormulae _formula;

  protected IsdaCompliantCreditCurveCalibrator() {
    _arbHandling = DEFAULT_ARBITRAGE_HANDLING;
    _formula = DEFAULT_FORMULA;
  }

  protected IsdaCompliantCreditCurveCalibrator(final AccrualOnDefaultFormulae formula) {
//      ArgumentChecker.notNull(formula, "formula");
    _arbHandling = DEFAULT_ARBITRAGE_HANDLING;
    _formula = formula;
  }

  protected IsdaCompliantCreditCurveCalibrator(final AccrualOnDefaultFormulae formula, final ArbitrageHandling arbHandling) {
//      ArgumentChecker.notNull(formula, "formula");
//      ArgumentChecker.notNull(arbHandling, "arbHandling");
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
   *  curve will be flat.
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

  /**
   * Bootstrapper the credit curve from a set of reference/calibration CDSs with market quotes 
   * @param calibrationCDSs The market CDSs - these are the reference instruments used to build the credit curve 
   * @param marketQuotes The market quotes of the CDSs 
   * @param yieldCurve The yield (or discount) curve 
   * @return The credit curve 
   */
  public LegalEntitySurvivalProbabilities calibrate(ResolvedCdsTrade[] calibrationCDSs,
      CdsQuote[] marketQuotes, CreditRatesProvider ratesProvider, LocalDate settlementDate,
      ReferenceData refData) {
//    ArgumentChecker.noNulls(marketQuotes, "marketQuotes");
    final int n = marketQuotes.length;
    final double[] coupons = new double[n];
    final double[] pufs = new double[n];
    for (int i = 0; i < n; i++) {
      final double[] temp = getStandardQuoteForm(calibrationCDSs[i], marketQuotes[i], ratesProvider, settlementDate, refData);
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

  /**
   * Put any CDS market quote into the form needed for the curve builder, namely coupon and points up-front (which can be zero)
   * @param calibrationCDS
   * @param marketQuote
   * @param yieldCurve
   * @return The market quotes in the form required by the curve builder
   */
  private double[] getStandardQuoteForm(ResolvedCdsTrade calibrationCDS, CdsQuote marketQuote,
      CreditRatesProvider ratesProvider, LocalDate settlementDate,
      ReferenceData refData) {
    IsdaCdsProductPricer pricer = new IsdaCdsProductPricer(_formula);

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
