/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.credit;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationFunction;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.calc.runner.FunctionUtils;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.pricer.credit.IsdaIndexCreditCurveInputsId;
import com.opengamma.strata.pricer.credit.IsdaIndexRecoveryRateId;
import com.opengamma.strata.pricer.credit.IsdaSingleNameCreditCurveInputsId;
import com.opengamma.strata.pricer.credit.IsdaSingleNameRecoveryRateId;
import com.opengamma.strata.pricer.credit.IsdaYieldCurveInputsId;
import com.opengamma.strata.product.credit.Cds;
import com.opengamma.strata.product.credit.CdsTrade;
import com.opengamma.strata.product.credit.IndexReferenceInformation;
import com.opengamma.strata.product.credit.ReferenceInformation;
import com.opengamma.strata.product.credit.ResolvedCdsTrade;
import com.opengamma.strata.product.credit.SingleNameReferenceInformation;

/**
 * Perform calculations on a single {@code CdsTrade} for each of a set of scenarios.
 * <p>
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
 *   <li>{@linkplain Measures#PRESENT_VALUE_MULTI_CCY Present value with no currency conversion}
 *   <li>{@linkplain Measures#IR01_PARALLEL_ZERO Scalar IR01, based on zero rates}
 *   <li>{@linkplain Measures#IR01_BUCKETED_ZERO Vector curve node IR01, based on zero rates}
 *   <li>{@linkplain Measures#IR01_PARALLEL_PAR Scalar IR01, based on par interest rates}
 *   <li>{@linkplain Measures#IR01_BUCKETED_PAR Vector curve node IR01, based on par interest rates}
 *   <li>{@linkplain Measures#CS01_PARALLEL_PAR Scalar CS01, based on credit par rates}
 *   <li>{@linkplain Measures#CS01_BUCKETED_PAR Vector curve node CS01, based on credit par rates}
 *   <li>{@linkplain Measures#CS01_PARALLEL_HAZARD Scalar CS01, based on hazard rates}
 *   <li>{@linkplain Measures#CS01_BUCKETED_HAZARD Vector curve node CS01, based on hazard rates}
 *   <li>{@linkplain Measures#RECOVERY01 Recovery01}
 *   <li>{@linkplain Measures#JUMP_TO_DEFAULT Jump to Default}
 *   <li>{@linkplain Measures#PAR_RATE Par rate}
 * </ul>
 * <p>
 * The "natural" currency is the currency of the fee leg.
 */
public class CdsTradeCalculationFunction
    implements CalculationFunction<CdsTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measures.PRESENT_VALUE, CdsMeasureCalculations::presentValue)
          .put(Measures.IR01_PARALLEL_ZERO, CdsMeasureCalculations::ir01ParallelZero)
          .put(Measures.IR01_BUCKETED_ZERO, CdsMeasureCalculations::ir01BucketedZero)
          .put(Measures.IR01_PARALLEL_PAR, CdsMeasureCalculations::ir01ParallelPar)
          .put(Measures.IR01_BUCKETED_PAR, CdsMeasureCalculations::ir01BucketedPar)
          .put(Measures.CS01_PARALLEL_PAR, CdsMeasureCalculations::cs01ParallelPar)
          .put(Measures.CS01_BUCKETED_PAR, CdsMeasureCalculations::cs01BucketedPar)
          .put(Measures.CS01_PARALLEL_HAZARD, CdsMeasureCalculations::cs01ParallelHazard)
          .put(Measures.CS01_BUCKETED_HAZARD, CdsMeasureCalculations::cs01BucketedHazard)
          .put(Measures.RECOVERY01, CdsMeasureCalculations::recovery01)
          .put(Measures.JUMP_TO_DEFAULT, CdsMeasureCalculations::jumpToDefault)
          .put(Measures.PAR_RATE, CdsMeasureCalculations::parRate)
          .build();

  private static final ImmutableSet<Measure> MEASURES = ImmutableSet.<Measure>builder()
      .addAll(CALCULATORS.keySet())
      .add(Measures.PRESENT_VALUE_MULTI_CCY)
      .build();

  /**
   * Creates an instance.
   */
  public CdsTradeCalculationFunction() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<CdsTrade> targetType() {
    return CdsTrade.class;
  }

  @Override
  public Set<Measure> supportedMeasures() {
    return MEASURES;
  }

  @Override
  public Currency naturalCurrency(CdsTrade trade, ReferenceData refData) {
    return trade.getProduct().getFeeLeg().getPeriodicPayments().getNotional().getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(
      CdsTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData) {

    Cds cds = trade.getProduct();

    Currency notionalCurrency = cds.getFeeLeg().getPeriodicPayments().getNotional().getCurrency();
    Currency feeCurrency = cds.getFeeLeg().getUpfrontFee().getCurrency();

    Set<MarketDataId<?>> rateCurveIds = ImmutableSet.of(
        IsdaYieldCurveInputsId.of(notionalCurrency),
        IsdaYieldCurveInputsId.of(feeCurrency));

    Set<Currency> currencies = ImmutableSet.of(notionalCurrency, feeCurrency);
    ReferenceInformation refInfo = cds.getReferenceInformation();
    if (refInfo instanceof SingleNameReferenceInformation) {
      SingleNameReferenceInformation singleNameRefInfo = (SingleNameReferenceInformation) refInfo;
      Set<MarketDataId<?>> keys = ImmutableSet.of(
          IsdaSingleNameCreditCurveInputsId.of(singleNameRefInfo),
          IsdaSingleNameRecoveryRateId.of(singleNameRefInfo));
      return FunctionRequirements.builder()
          .valueRequirements(Sets.union(rateCurveIds, keys))
          .outputCurrencies(currencies)
          .build();

    } else if (refInfo instanceof IndexReferenceInformation) {
      IndexReferenceInformation indexRefInfo = (IndexReferenceInformation) refInfo;
      Set<MarketDataId<?>> keys = ImmutableSet.of(
          IsdaIndexCreditCurveInputsId.of(indexRefInfo),
          IsdaIndexRecoveryRateId.of(indexRefInfo));
      return FunctionRequirements.builder()
          .valueRequirements(Sets.union(rateCurveIds, keys))
          .outputCurrencies(currencies)
          .build();

    } else {
      throw new IllegalStateException("Unknown reference information type: " + refInfo.getType());
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      CdsTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ScenarioMarketData scenarioMarketData,
      ReferenceData refData) {

    // resolve the trade once for all measures and all scenarios
    ResolvedCdsTrade resolved = trade.resolve(refData);

    // loop around measures, calculating all scenarios for one measure
    Map<Measure, Result<?>> results = new HashMap<>();
    for (Measure measure : measures) {
      results.put(measure, calculate(measure, resolved, scenarioMarketData));
    }
    // The calculated value is the same for these two measures but they are handled differently WRT FX conversion
    FunctionUtils.duplicateResult(Measures.PRESENT_VALUE, Measures.PRESENT_VALUE_MULTI_CCY, results);
    return results;
  }

  // calculate one measure
  private Result<?> calculate(
      Measure measure,
      ResolvedCdsTrade trade,
      ScenarioMarketData scenarioMarketData) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result.failure(FailureReason.INVALID_INPUT, "Unsupported measure: {}", measure);
    }
    return Result.of(() -> calculator.calculate(trade, scenarioMarketData));
  }

  //-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract ScenarioArray<?> calculate(
        ResolvedCdsTrade trade,
        ScenarioMarketData marketData);
  }

}
