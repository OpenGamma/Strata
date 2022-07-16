/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.corporateaction;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationFunction;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.product.corporateaction.AnnouncementCorporateActionPosition;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Perform calculations on a single {@code GenericSecurityPosition} for each of a set of scenarios.
 * <p>
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
 * </ul>
 */
public class GenericCorporateActionPositionCalculationFunction
    implements CalculationFunction<AnnouncementCorporateActionPosition> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measures.GROSS_VALUE, CorporateActionMeasureCalculations::getGrossCurrencyAmount)
          .build();

  private static final ImmutableSet<Measure> MEASURES = CALCULATORS.keySet();

  /**
   * Creates an instance.
   */
  public GenericCorporateActionPositionCalculationFunction() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<AnnouncementCorporateActionPosition> targetType() {
    return AnnouncementCorporateActionPosition.class;
  }

  @Override
  public Set<Measure> supportedMeasures() {
    return MEASURES;
  }

  @Override
  public Optional<String> identifier(AnnouncementCorporateActionPosition target) {
    return target.getInfo().getId().map(id -> id.toString());
  }

  @Override
  public Currency naturalCurrency(AnnouncementCorporateActionPosition position, ReferenceData refData) {
    return position.getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(
      AnnouncementCorporateActionPosition position,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData) {

    return FunctionRequirements.empty();
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      AnnouncementCorporateActionPosition corporateActionAnnouncementPosition,
      Set<Measure> measures,
      CalculationParameters parameters,
      ScenarioMarketData scenarioMarketData,
      ReferenceData refData) {

    // loop around measures, calculating all scenarios for one measure
    Map<Measure, Result<?>> results = new HashMap<>();
    for (Measure measure : measures) {
      results.put(measure, calculate(measure, corporateActionAnnouncementPosition));
    }
    return results;
  }

  // calculate one measure
  private Result<?> calculate(
      Measure measure,
      AnnouncementCorporateActionPosition position) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result.failure(FailureReason.UNSUPPORTED, "Unsupported measure for GenericSecurityPosition: {}", measure);
    }
    return Result.of(() -> calculator.calculate(position));
  }

  //-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract CurrencyAmount calculate(
        AnnouncementCorporateActionPosition corporateActionAnnouncementPosition);
  }

}
