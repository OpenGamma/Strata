/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.data.ObservableId;

/**
 * Provides access to market data across one or more scenarios.
 * <p>
 * Market data is looked up using subclasses of {@link MarketDataId}.
 * All data is valid for a single date, defined by {@link #getValuationDate()}.
 * <p>
 * There are two ways to access the available market data.
 * <p>
 * The first way is to use the access methods on this interface that return the data
 * associated with a single identifier for all scenarios. The two key methods are
 * {@link #getValue(MarketDataId)} and {@link #getScenarioValue(ScenarioMarketDataId)}.
 * <p>
 * The second way is to use the method {@link #scenarios()} or {@link #scenario(int)}.
 * These return all the data associated with a single scenario.
 * This approach is convenient for single scenario pricers.
 * <p>
 * The standard implementation is {@link ImmutableScenarioMarketData}.
 */
public interface ScenarioMarketData {

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a valuation date, map of values and time-series.
   * <p>
   * The valuation date and map of values must have the same number of scenarios.
   * 
   * @param scenarioCount  the number of scenarios
   * @param valuationDate  the valuation dates associated with all scenarios
   * @param values  the market data values, one for each scenario
   * @param timeSeries  the time-series
   * @return a set of market data containing the values in the map
   */
  public static ScenarioMarketData of(
      int scenarioCount,
      LocalDate valuationDate,
      Map<? extends MarketDataId<?>, MarketDataBox<?>> values,
      Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries) {

    return of(scenarioCount, MarketDataBox.ofSingleValue(valuationDate), values, timeSeries);
  }

  /**
   * Obtains an instance from a valuation date, map of values and time-series.
   * <p>
   * The valuation date and map of values must have the same number of scenarios.
   * 
   * @param scenarioCount  the number of scenarios
   * @param valuationDate  the valuation dates associated with the market data, one for each scenario
   * @param values  the market data values, one for each scenario
   * @param timeSeries  the time-series
   * @return a set of market data containing the values in the map
   */
  public static ScenarioMarketData of(
      int scenarioCount,
      MarketDataBox<LocalDate> valuationDate,
      Map<? extends MarketDataId<?>, MarketDataBox<?>> values,
      Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries) {

    return ImmutableScenarioMarketData.of(scenarioCount, valuationDate, values, timeSeries);
  }

  /**
   * Obtains an instance by wrapping a single set of market data.
   * <p>
   * The result will consist of a {@code ScenarioMarketData} that returns the specified
   * market data for each scenario.
   * <p>
   * This can be used in association with the
   * {@link #withPerturbation(MarketDataId, ScenarioPerturbation, ReferenceData) withPerturbation}
   * method to take a base set of market data and create a complete set of perturbations.
   * See {@code MarketDataFactory} for the ability to apply multiple perturbations, including
   * perturbations to calibration inputs, such as quotes.
   * 
   * @param scenarioCount  the number of scenarios, one or more
   * @param marketData  the single set of market data
   * @return a set of market data containing the values in the map
   */
  public static ScenarioMarketData of(int scenarioCount, MarketData marketData) {
    return RepeatedScenarioMarketData.of(scenarioCount, marketData);
  }

  /**
   * Obtains a market data instance that contains no data and has no scenarios.
   *
   * @return an empty instance
   */
  public static ScenarioMarketData empty() {
    return ImmutableScenarioMarketData.empty();
  }

  //-------------------------------------------------------------------------
  /**
   * Gets a box that can provide the valuation date of each scenario.
   *
   * @return the valuation dates of the scenarios
   */
  public abstract MarketDataBox<LocalDate> getValuationDate();

  /**
   * Gets the number of scenarios.
   *
   * @return the number of scenarios
   */
  public abstract int getScenarioCount();

  //-------------------------------------------------------------------------
  /**
   * Returns a stream of market data, one for each scenario.
   * <p>
   * The stream will return instances of {@link MarketData}, where each represents
   * a single scenario view of the complete set of data.
   *
   * @return the stream of market data, one for the each scenario
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public default Stream<MarketData> scenarios() {
    return IntStream.range(0, getScenarioCount())
        .mapToObj(scenarioIndex -> SingleScenarioMarketData.of(this, scenarioIndex));
  }

  /**
   * Returns market data for a single scenario.
   * <p>
   * This returns a view of the market data for the single specified scenario.
   *
   * @param scenarioIndex  the scenario index
   * @return the market data for the specified scenario
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public default MarketData scenario(int scenarioIndex) {
    return SingleScenarioMarketData.of(this, scenarioIndex);
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this market data contains a value for the specified identifier.
   *
   * @param id  the identifier to find
   * @return true if the market data contains a value for the identifier
   */
  public default boolean containsValue(MarketDataId<?> id) {
    return findValue(id).isPresent();
  }

  /**
   * Gets the market data value associated with the specified identifier.
   * <p>
   * The result is a box that provides data for all scenarios.
   * If this market data instance contains the identifier, the value will be returned.
   * Otherwise, an exception will be thrown.
   *
   * @param <T>  the type of the market data value
   * @param id  the identifier to find
   * @return the market data value box providing data for all scenarios
   * @throws MarketDataNotFoundException if the identifier is not found
   */
  public default <T> MarketDataBox<T> getValue(MarketDataId<T> id) {
    return findValue(id)
        .orElseThrow(() -> new MarketDataNotFoundException(Messages.format(
            "Market data not found for '{}' of type '{}'", id, id.getClass().getSimpleName())));
  }

  /**
   * Finds the market data value associated with the specified identifier.
   * <p>
   * The result is a box that provides data for all scenarios.
   * If this market data instance contains the identifier, the value will be returned.
   * Otherwise, an empty optional will be returned.
   *
   * @param <T>  the type of the market data value
   * @param id  the identifier to find
   * @return the market data value box providing data for all scenarios, empty if not found
   */
  public abstract <T> Optional<MarketDataBox<T>> findValue(MarketDataId<T> id);

  //-------------------------------------------------------------------------
  /**
   * Gets the market data identifiers.
   *
   * @return the set of market data identifiers
   */
  public abstract Set<MarketDataId<?>> getIds();

  /**
   * Finds the market data identifiers associated with the specified name.
   * <p>
   * This returns the unique identifiers that refer to the specified name.
   * There may be more than one identifier associated with a name as the name is not unique.
   *
   * @param <T>  the type of the market data value
   * @param name  the name to find
   * @return the set of market data identifiers, empty if name not found
   */
  public abstract <T> Set<MarketDataId<T>> findIds(MarketDataName<T> name);

  //-------------------------------------------------------------------------
  /**
   * Gets an object containing market data for multiple scenarios.
   * <p>
   * There are many possible ways to store scenario market data for a data type. For example, if the single
   * values are doubles, the scenario value might simply be a {@code List<Double>} or it might be a wrapper
   * class that stores the values more efficiently in a {@code double[]}.
   * <p>
   * If the market data contains a single value for the identifier or a scenario value of the wrong type,
   * a value of the required type is created by invoking {@link ScenarioMarketDataId#createScenarioValue}.
   * <p>
   * Normally this should not be necessary. It is assumed the required scenario values will be created by the
   * perturbations that create scenario data. However there is no mechanism in the market data system to guarantee
   * that scenario values of a particular type are available. If they are not they are created on demand.
   * <p>
   * Values returned from this method might be cached for efficiency.
   *
   * @param id  the identifier to find
   * @param <T>  the type of the individual market data values used when performing calculations for one scenario
   * @param <U>  the type of the object containing the market data for all scenarios
   * @return an object containing market data for multiple scenarios
   * @throws IllegalArgumentException if no value is found
   */
  @SuppressWarnings("unchecked")
  public default <T, U extends ScenarioArray<T>> U getScenarioValue(ScenarioMarketDataId<T, U> id) {
    MarketDataBox<T> box = getValue(id.getMarketDataId());

    if (box.isSingleValue()) {
      return id.createScenarioValue(box, getScenarioCount());
    }
    ScenarioArray<T> scenarioValue = box.getScenarioValue();
    if (id.getScenarioMarketDataType().isInstance(scenarioValue)) {
      return (U) scenarioValue;
    }
    return id.createScenarioValue(box, getScenarioCount());
  }

  /**
   * Returns set of market data which combines the data from this set of data with another set.
   * <p>
   * If the same item of data is available in both sets, it will be taken from this set.
   * <p>
   * Both sets of data must contain the same number of scenarios, or one of them must have one scenario.
   * If one of the sets of data has one scenario, the combined set will have the scenario count
   * of the other set.
   *
   * @param other  another set of market data
   * @return a set of market data combining the data in this set with the data in the other
   */
  public default ScenarioMarketData combinedWith(ScenarioMarketData other) {
    return new CombinedScenarioMarketData(this, other);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the time-series identifiers.
   * <p>
   * Time series are not affected by scenarios, therefore there is a single time-series
   * for each identifier which is shared between all scenarios.
   *
   * @return the set of observable identifiers
   */
  public abstract Set<ObservableId> getTimeSeriesIds();

  /**
   * Gets the time-series associated with the specified identifier, empty if not found.
   * <p>
   * Time series are not affected by scenarios, therefore there is a single time-series
   * for each identifier which is shared between all scenarios.
   *
   * @param id  the identifier to find
   * @return the time-series, empty if no time-series found
   */
  public abstract LocalDateDoubleTimeSeries getTimeSeries(ObservableId id);

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this market data with the specified value.
   * <p>
   * When the result is queried for the specified identifier, the specified value will be returned.
   * <p>
   * The number of scenarios in the box must match this market data.
   * <p>
   * For example, this method could be used to replace a curve with a bumped curve.
   *
   * @param <T>  the type of the market data value
   * @param id  the identifier
   * @param value  the value to associate with the identifier
   * @return the derived market data with the specified identifier and value
   * @throws IllegalArgumentException if the scenario count does not match
   */
  public default <T> ScenarioMarketData withValue(MarketDataId<T> id, MarketDataBox<T> value) {
    return ExtendedScenarioMarketData.of(id, value, this);
  }

  /**
   * Returns a copy of this market data with the specified value perturbed.
   * <p>
   * This finds the market data value using the identifier, throwing an exception if not found.
   * It then perturbs the value and returns a new instance containing the value.
   * <p>
   * The number of scenarios of the perturbation must match this market data.
   * <p>
   * This method is intended for one off perturbations of calibrated market data, such as curves.
   * See {@code MarketDataFactory} for the ability to apply multiple perturbations, including
   * perturbations to calibration inputs, such as quotes.
   * <p>
   * This instance is immutable and unaffected by this method call.
   * 
   * @param <T>  the type of the market data value
   * @param id  the identifier to perturb
   * @param perturbation  the perturbation to apply
   * @param refData  the reference data
   * @return a parameterized data instance based on this with the specified perturbation applied
   * @throws IllegalArgumentException if the scenario count does not match
   * @throws MarketDataNotFoundException if the identifier is not found
   * @throws RuntimeException if unable to perform the perturbation
   */
  public default <T> ScenarioMarketData withPerturbation(
      MarketDataId<T> id,
      ScenarioPerturbation<T> perturbation,
      ReferenceData refData) {

    if (perturbation.getScenarioCount() != 1 && perturbation.getScenarioCount() != getScenarioCount()) {
      throw new IllegalArgumentException(Messages.format(
          "Scenario count mismatch: perturbation has {} scenarios but this market data has {}",
          perturbation.getScenarioCount(), getScenarioCount()));
    }
    return withValue(id, perturbation.applyTo(getValue(id), refData));
  }

}
