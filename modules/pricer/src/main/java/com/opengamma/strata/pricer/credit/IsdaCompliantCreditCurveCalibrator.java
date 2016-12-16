
/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Guavate;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveParameterSize;
import com.opengamma.strata.market.curve.IsdaCreditCurveDefinition;
import com.opengamma.strata.market.curve.JacobianCalibrationMatrix;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.curve.node.CdsIsdaCreditCurveNode;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.math.impl.matrix.CommonsMatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.pricer.common.PriceType;
import com.opengamma.strata.product.credit.CdsCalibrationTrade;
import com.opengamma.strata.product.credit.CdsQuote;
import com.opengamma.strata.product.credit.ResolvedCdsTrade;
import com.opengamma.strata.product.credit.type.CdsQuoteConvention;

/**
 * ISDA compliant credit curve calibrator.
 * <p>
 * A single credit curve is calibrated for credit default swaps on a legal entity.
 * <p>
 * The curve is defined using one or more {@linkplain CdsCurveNode nodes}.
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
  private static final AccrualOnDefaultFormula DEFAULT_FORMULA = AccrualOnDefaultFormula.ORIGINAL_ISDA;
  /**
   * The matrix algebra used for matrix inversion.
   */
  private static final MatrixAlgebra MATRIX_ALGEBRA = new CommonsMatrixAlgebra();

  /**
   * The arbitrage handling.
   */
  private final ArbitrageHandling arbHandling;
  /**
   * The accrual-on-default formula.
   */
  private final AccrualOnDefaultFormula formula;
  /**
   * The trade pricer.
   */
  private final IsdaCdsTradePricer tradePricer;

  //-------------------------------------------------------------------------
  protected IsdaCompliantCreditCurveCalibrator() {
    this(DEFAULT_FORMULA, DEFAULT_ARBITRAGE_HANDLING);
  }

  protected IsdaCompliantCreditCurveCalibrator(AccrualOnDefaultFormula formula) {
    this(formula, DEFAULT_ARBITRAGE_HANDLING);
  }

  protected IsdaCompliantCreditCurveCalibrator(AccrualOnDefaultFormula formula, ArbitrageHandling arbHandling) {
    this.arbHandling = ArgChecker.notNull(arbHandling, "arbHandling");
    this.formula = ArgChecker.notNull(formula, "formula");
    this.tradePricer = new IsdaCdsTradePricer(formula);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the arbitrage handling. 
   * <p>
   * See {@link ArbitrageHandling} for detail.
   * 
   * @return
   */
  protected ArbitrageHandling getArbHanding() {
    return arbHandling;
  }

  /**
   * Obtains the accrual-on-default formula. 
   * <p>
   * See {@link AccrualOnDefaultFormula} for detail.
   * 
   * @return
   */
  protected AccrualOnDefaultFormula getAccOnDefaultFormula() {
    return formula;
  }

  /**
   * Obtains the trade pricer used in this calibration. 
   * 
   * @return the trade pricer
   */
  protected IsdaCdsTradePricer getTradePricer() {
    return tradePricer;
  }

  //-------------------------------------------------------------------------
  /**
   * Calibrates the ISDA compliant credit curve to the market data.
   * <p>
   * This creates the single credit curve for a legal entity.
   * The curve nodes in {@code IsdaCreditCurveDefinition} should be single-name credit default swaps on this legal entity.
   * <p>
   * The relevant discount curve and recovery rate curve must be stored in {@code ratesProvider}. 
   * The day count convention for the resulting credit curve is the same as that of the discount curve.
   * 
   * @param curveDefinition  the curve definition
   * @param marketData  the market data
   * @param ratesProvider  the rates provider
   * @param refData  the reference data
   * @return the ISDA compliant credit curve
   */
  public LegalEntitySurvivalProbabilities calibrate(
      IsdaCreditCurveDefinition curveDefinition,
      MarketData marketData,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    ArgChecker.isTrue(curveDefinition.getCurveValuationDate().equals(ratesProvider.getValuationDate()),
        "ratesProvider and curveDefinition must be based on the same valuation date");
    ImmutableList<CdsIsdaCreditCurveNode> curveNodes = curveDefinition.getCurveNodes().stream()
        .filter(n -> n instanceof CdsIsdaCreditCurveNode)
        .map(n -> (CdsIsdaCreditCurveNode) n)
        .collect(Guavate.toImmutableList());
    return calibrate(
        curveNodes,
        curveDefinition.getName(),
        marketData,
        ratesProvider,
        curveDefinition.getDayCount(),
        curveDefinition.getCurrency(),
        curveDefinition.isComputeJacobian(),
        refData);
  }

  LegalEntitySurvivalProbabilities calibrate(
      List<CdsIsdaCreditCurveNode> curveNodes,
      CurveName name,
      MarketData marketData,
      CreditRatesProvider ratesProvider,
      DayCount definitionDayCount,
      Currency definitionCurrency,
      boolean computeJacobian,
      ReferenceData refData) {

    Iterator<StandardId> legalEntities =
        curveNodes.stream().map(CdsIsdaCreditCurveNode::getLegalEntityId).collect(Collectors.toSet()).iterator();
    StandardId legalEntityId = legalEntities.next();
    ArgChecker.isFalse(legalEntities.hasNext(), "legal entity must be common to curve nodes");
    Iterator<Currency> currencies =
        curveNodes.stream().map(n -> n.getTemplate().getConvention().getCurrency()).collect(Collectors.toSet()).iterator();
    Currency currency = currencies.next();
    ArgChecker.isFalse(currencies.hasNext(), "currency must be common to curve nodes");
    ArgChecker.isTrue(definitionCurrency.equals(currency),
        "curve definition currency must be the same as the currency of CDS");
    Iterator<CdsQuoteConvention> quoteConventions =
        curveNodes.stream().map(n -> n.getQuoteConvention()).collect(Collectors.toSet()).iterator();
    CdsQuoteConvention quoteConvention = quoteConventions.next();
    ArgChecker.isFalse(quoteConventions.hasNext(), "quote convention must be common to curve nodes");
    LocalDate valuationDate = marketData.getValuationDate();
    ArgChecker.isTrue(valuationDate.equals(marketData.getValuationDate()),
        "ratesProvider and marketDate must be based on the same valuation date");
    CreditDiscountFactors discountFactors = ratesProvider.discountFactors(currency);
    ArgChecker.isTrue(definitionDayCount.equals(discountFactors.getDayCount()),
        "credit curve and discount curve must be based on the same day count convention");
    RecoveryRates recoveryRates = ratesProvider.recoveryRates(legalEntityId);

    int nNodes = curveNodes.size();
    double[] coupons = new double[nNodes];
    double[] pufs = new double[nNodes];
    double[][] diag = new double[nNodes][nNodes];
    ResolvedCdsTrade[] trades = new ResolvedCdsTrade[nNodes];
    for (int i = 0; i < nNodes; i++) {
      CdsCalibrationTrade tradeCalibration = curveNodes.get(i).trade(1d, marketData, refData);
      trades[i] = tradeCalibration.getUnderlyingTrade().resolve(refData);
      double[] temp = getStandardQuoteForm(
          trades[i],
          tradeCalibration.getQuote(),
          valuationDate,
          discountFactors,
          recoveryRates,
          computeJacobian,
          refData);
      coupons[i] = temp[0];
      pufs[i] = temp[1];
      diag[i][i] = temp[2];
    }
    NodalCurve nodalCurve = calibrate(
        trades, coupons, pufs, name, valuationDate, discountFactors, recoveryRates, refData);

    if (computeJacobian) {
      LegalEntitySurvivalProbabilities creditCurve = LegalEntitySurvivalProbabilities.of(
          legalEntityId, IsdaCompliantZeroRateDiscountFactors.of(currency, valuationDate, nodalCurve));
      CreditRatesProvider ratesProviderNew = ratesProvider.toBuilder()
          .creditCurves(ImmutableMap.of(Pair.of(legalEntityId, currency), creditCurve))
          .build();
      Function<ResolvedCdsTrade, DoubleArray> sensiFunc = quoteConvention.equals(CdsQuoteConvention.PAR_SPREAD)
          ? getParSpreadSensitivityFunction(ratesProviderNew, refData)
          : getPointsUpfrontSensitivityFunction(ratesProviderNew, refData);
      DoubleMatrix sensi = DoubleMatrix.ofArrayObjects(nNodes, nNodes, i -> sensiFunc.apply(trades[i]));
      sensi = (DoubleMatrix) MATRIX_ALGEBRA.multiply(DoubleMatrix.ofUnsafe(diag), sensi);
      JacobianCalibrationMatrix jacobian = JacobianCalibrationMatrix.of(
          ImmutableList.of(CurveParameterSize.of(name, nNodes)), MATRIX_ALGEBRA.getInverse(sensi));
      nodalCurve = nodalCurve.withMetadata(nodalCurve.getMetadata().withInfo(CurveInfoType.JACOBIAN, jacobian));
    }

    return LegalEntitySurvivalProbabilities.of(
        legalEntityId, IsdaCompliantZeroRateDiscountFactors.of(currency, valuationDate, nodalCurve));
  }

  private Function<ResolvedCdsTrade, DoubleArray> getPointsUpfrontSensitivityFunction(
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    Function<ResolvedCdsTrade, DoubleArray> func = new Function<ResolvedCdsTrade, DoubleArray>() {
      @Override
      public DoubleArray apply(ResolvedCdsTrade trade) {
        PointSensitivities point = tradePricer.priceSensitivity(trade, ratesProvider, refData);
        return ratesProvider.parameterSensitivity(point).getSensitivities().get(0).getSensitivity();
      }
    };
    return func;
  }

  private Function<ResolvedCdsTrade, DoubleArray> getParSpreadSensitivityFunction(
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    Function<ResolvedCdsTrade, DoubleArray> func = new Function<ResolvedCdsTrade, DoubleArray>() {
      @Override
      public DoubleArray apply(ResolvedCdsTrade trade) {
        PointSensitivities point = tradePricer.parSpreadSensitivity(trade, ratesProvider, refData);
        return ratesProvider.parameterSensitivity(point).getSensitivities().get(0).getSensitivity();
      }
    };
    return func;
  }

  abstract NodalCurve calibrate(
      ResolvedCdsTrade[] calibrationCDSs,
      double[] flactionalSpreads,
      double[] pointsUpfront,
      CurveName name,
      LocalDate valuationDate,
      CreditDiscountFactors discountFactors,
      RecoveryRates recoveryRates,
      ReferenceData refData);

  private double[] getStandardQuoteForm(ResolvedCdsTrade calibrationCds, CdsQuote marketQuote, LocalDate valuationDate,
      CreditDiscountFactors discountFactors, RecoveryRates recoveryRates, boolean computeJacobian, ReferenceData refData) {

    double[] res = new double[3];
    res[2] = 1d;
    if (marketQuote.getQuoteConvention().equals(CdsQuoteConvention.PAR_SPREAD)) {
      res[0] = marketQuote.getQuotedValue();
    } else if (marketQuote.getQuoteConvention().equals(CdsQuoteConvention.QUOTED_SPREAD)) {
      double qSpread = marketQuote.getQuotedValue();
      CurveName curveName = CurveName.of("quoteConvertCurve");
      NodalCurve tempCreditCurve =
          calibrate(new ResolvedCdsTrade[] {calibrationCds}, new double[] {qSpread}, new double[1], curveName,
              valuationDate, discountFactors, recoveryRates, refData);
      Currency currency = calibrationCds.getProduct().getCurrency();
      StandardId legalEntityId = calibrationCds.getProduct().getLegalEntityId();
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
      res[0] = calibrationCds.getProduct().getFixedRate();
      res[1] = tradePricer.price(calibrationCds, rates, PriceType.CLEAN, refData);
      if (computeJacobian) {
        CurrencyParameterSensitivities pufSensi =
            rates.parameterSensitivity(tradePricer.priceSensitivity(calibrationCds, rates, refData));
        CurrencyParameterSensitivities spSensi =
            rates.parameterSensitivity(tradePricer.parSpreadSensitivity(calibrationCds, rates, refData));
        res[2] = spSensi.getSensitivity(curveName, currency).getSensitivity().get(0) /
            pufSensi.getSensitivity(curveName, currency).getSensitivity().get(0);
      }
    } else if (marketQuote.getQuoteConvention().equals(CdsQuoteConvention.POINTS_UPFRONT)) {
      res[0] = calibrationCds.getProduct().getFixedRate();
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
