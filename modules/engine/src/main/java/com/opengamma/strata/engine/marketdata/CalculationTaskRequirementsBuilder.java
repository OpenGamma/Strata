/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.engine.marketdata.scenarios.LocalScenarioDefinition;

/**
 * Mutable builder for creating instances of {@link CalculationRequirements}.
 */
public final class CalculationTaskRequirementsBuilder {

  /** IDs identifying the observable market data values required for the calculations. */
  private final Set<ObservableId> observables = new HashSet<>();

  /** IDs identifying the non-observable market data values required for the calculations. */
  private final Set<MarketDataId<?>> nonObservables = new HashSet<>();

  /** IDs identifying the time series of market data values required for the calculations. */
  private final Set<ObservableId> timeSeries = new HashSet<>();

  /** The currencies used in the outputs of the calculations. */
  private final Set<Currency> outputCurrencies = new HashSet<>();

  private final List<LocalScenarioDefinition> localScenarios = new ArrayList<>();

  /**
   * Adds requirements for time series of observable market data.
   *
   * @param ids  IDs of the data
   * @return this builder
   */
  public CalculationTaskRequirementsBuilder addTimeSeries(Collection<? extends ObservableId> ids) {
    ArgChecker.notNull(ids, "ids");
    timeSeries.addAll(ids);
    return this;
  }

  /**
   * Adds requirements for time series of observable market data.
   *
   * @param ids  IDs of the data
   * @return this builder
   */
  public CalculationTaskRequirementsBuilder addTimeSeries(ObservableId... ids) {
    return addTimeSeries(Arrays.asList(ids));
  }

  /**
   * Adds requirements for single values of market data.
   *
   * @param ids  IDs of the data
   * @return this builder
   */
  public CalculationTaskRequirementsBuilder addValues(Collection<? extends MarketDataId<?>> ids) {
    ArgChecker.notNull(ids, "ids");

    for (MarketDataId<?> id : ids) {
      if (id instanceof ObservableId) {
        observables.add((ObservableId) id);
      } else {
        nonObservables.add(id);
      }
    }
    return this;
  }

  /**
   * Adds requirements for single values of market data.
   *
   * @param ids  IDs of the data
   * @return this builder
   */
  public CalculationTaskRequirementsBuilder addValues(MarketDataId<?>... ids) {
    return addValues(Arrays.asList(ids));
  }

  /**
   * Adds all requirements from an instance of {@code CalculationRequirements} to this builder.
   *
   * @param requirements  a set of requirements
   * @return this builder
   */
  public CalculationTaskRequirementsBuilder addRequirements(CalculationRequirements requirements) {
    ArgChecker.notNull(requirements, "requirements");
    observables.addAll(requirements.getObservables());
    nonObservables.addAll(requirements.getNonObservables());
    timeSeries.addAll(requirements.getTimeSeries());
    outputCurrencies.addAll(requirements.getOutputCurrencies());
    return this;
  }

  public CalculationTaskRequirementsBuilder addLocalScenarios(LocalScenarioDefinition... scenarios) {
    localScenarios.addAll(ImmutableList.copyOf(scenarios));
    return this;
  }

  public CalculationTaskRequirementsBuilder addLocalScenarios(List<LocalScenarioDefinition> scenarios) {
    localScenarios.addAll(scenarios);
    return this;
  }

  /**
   * Returns a set of market data requirements built from the data in this builder.
   *
   * @return a set of market data requirements built from the data in this builder
   */
  public CalculationTaskRequirements build() {
    return new CalculationTaskRequirements(observables, nonObservables, timeSeries, outputCurrencies, localScenarios);
  }
}
