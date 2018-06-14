/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.bond;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationFunction;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.product.SecuritizedProductPortfolioItem;
import com.opengamma.strata.product.bond.FixedCouponBond;
import com.opengamma.strata.product.bond.FixedCouponBondPosition;
import com.opengamma.strata.product.bond.FixedCouponBondTrade;
import com.opengamma.strata.product.bond.ResolvedFixedCouponBondTrade;

/**
 * Perform calculations on a single {@code FixedCouponBondTrade} or {@code FixedCouponBondPosition}
 * for each of a set of scenarios.
 * <p>
 * This uses the standard discounting calculation method.
 * An instance of {@link LegalEntityDiscountingMarketDataLookup} must be specified.
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
 *   <li>{@linkplain Measures#PV01_CALIBRATED_SUM PV01 calibrated sum}
 *   <li>{@linkplain Measures#PV01_CALIBRATED_BUCKETED PV01 calibrated bucketed}
 *   <li>{@linkplain Measures#CURRENCY_EXPOSURE Currency exposure}
 *   <li>{@linkplain Measures#CURRENT_CASH Current cash}
 *   <li>{@linkplain Measures#RESOLVED_TARGET Resolved trade}
 * </ul>
 * 
 * <h4>Price</h4>
 * Strata uses <i>decimal prices</i> for bonds in the trade model, pricers and market data.
 * For example, a price of 99.32% is represented in Strata by 0.9932.
 * 
 * @param <T> the trade or position type
 */
public class FixedCouponBondTradeCalculationFunction<T extends SecuritizedProductPortfolioItem<FixedCouponBond> & Resolvable<ResolvedFixedCouponBondTrade>>
    implements CalculationFunction<T> {

  /**
   * The trade instance
   */
  public static final FixedCouponBondTradeCalculationFunction<FixedCouponBondTrade> TRADE =
      new FixedCouponBondTradeCalculationFunction<>(FixedCouponBondTrade.class);
  /**
   * The position instance
   */
  public static final FixedCouponBondTradeCalculationFunction<FixedCouponBondPosition> POSITION =
      new FixedCouponBondTradeCalculationFunction<>(FixedCouponBondPosition.class);

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measures.PRESENT_VALUE, FixedCouponBondMeasureCalculations.DEFAULT::presentValue)
          .put(Measures.PV01_CALIBRATED_SUM, FixedCouponBondMeasureCalculations.DEFAULT::pv01CalibratedSum)
          .put(Measures.PV01_CALIBRATED_BUCKETED, FixedCouponBondMeasureCalculations.DEFAULT::pv01CalibratedBucketed)
          .put(Measures.CURRENCY_EXPOSURE, FixedCouponBondMeasureCalculations.DEFAULT::currencyExposure)
          .put(Measures.CURRENT_CASH, FixedCouponBondMeasureCalculations.DEFAULT::currentCash)
          .put(Measures.RESOLVED_TARGET, (rt, smd) -> rt)
          .build();

  private static final ImmutableSet<Measure> MEASURES = CALCULATORS.keySet();

  /**
   * The trade or position type.
   */
  private final Class<T> targetType;

  /**
   * Creates an instance.
   * 
   * @param targetType  the trade or position type
   */
  private FixedCouponBondTradeCalculationFunction(Class<T> targetType) {
    this.targetType = ArgChecker.notNull(targetType, "targetType");
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<T> targetType() {
    return targetType;
  }

  @Override
  public Set<Measure> supportedMeasures() {
    return MEASURES;
  }

  @Override
  public Optional<String> identifier(T target) {
    return target.getInfo().getId().map(id -> id.toString());
  }

  @Override
  public Currency naturalCurrency(T target, ReferenceData refData) {
    return target.getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(
      T target,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData) {

    // extract data from product
    FixedCouponBond product = target.getProduct();

    // use lookup to build requirements
    LegalEntityDiscountingMarketDataLookup bondLookup = parameters.getParameter(LegalEntityDiscountingMarketDataLookup.class);
    return bondLookup.requirements(product.getSecurityId(), product.getLegalEntityId(), product.getCurrency());
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      T target,
      Set<Measure> measures,
      CalculationParameters parameters,
      ScenarioMarketData scenarioMarketData,
      ReferenceData refData) {

    // resolve the trade once for all measures and all scenarios
    ResolvedFixedCouponBondTrade resolved = target.resolve(refData);

    // use lookup to query market data
    LegalEntityDiscountingMarketDataLookup bondLookup = parameters.getParameter(LegalEntityDiscountingMarketDataLookup.class);
    LegalEntityDiscountingScenarioMarketData marketData = bondLookup.marketDataView(scenarioMarketData);

    // loop around measures, calculating all scenarios for one measure
    Map<Measure, Result<?>> results = new HashMap<>();
    for (Measure measure : measures) {
      results.put(measure, calculate(measure, resolved, marketData));
    }
    return results;
  }

  // calculate one measure
  private Result<?> calculate(
      Measure measure,
      ResolvedFixedCouponBondTrade resolved,
      LegalEntityDiscountingScenarioMarketData marketData) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result.failure(FailureReason.UNSUPPORTED, "Unsupported measure for FixedCouponBond: {}", measure);
    }
    return Result.of(() -> calculator.calculate(resolved, marketData));
  }

  //-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract Object calculate(
        ResolvedFixedCouponBondTrade resolved,
        LegalEntityDiscountingScenarioMarketData marketData);
  }

}
