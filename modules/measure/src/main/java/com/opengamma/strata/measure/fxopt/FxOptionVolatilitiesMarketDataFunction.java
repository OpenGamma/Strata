/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.calc.marketdata.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.MarketDataFunction;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.measure.ValuationZoneTimeDefinition;
import com.opengamma.strata.pricer.fxopt.FxOptionVolatilities;
import com.opengamma.strata.pricer.fxopt.FxOptionVolatilitiesId;

/**
 * Market data function that builds FX option volatilities.
 * <p>
 * This function creates FX option volatilities, turning {@code FxOptionVolatilitiesId} into {@code FxOptionVolatilities}.
 */
public class FxOptionVolatilitiesMarketDataFunction
    implements MarketDataFunction<FxOptionVolatilities, FxOptionVolatilitiesId> {

  @Override
  public MarketDataRequirements requirements(FxOptionVolatilitiesId id, MarketDataConfig marketDataConfig) {

    FxOptionVolatilitiesDefinition volatilitiesDefinition = marketDataConfig.get(
        FxOptionVolatilitiesDefinition.class, id.getName().getName());
    return MarketDataRequirements.builder()
        .addValues(volatilitiesDefinition.volatilitiesInputs())
        .build();
  }

  @Override
  public MarketDataBox<FxOptionVolatilities> build(
      FxOptionVolatilitiesId id,
      MarketDataConfig marketDataConfig,
      ScenarioMarketData marketData,
      ReferenceData refData) {

    FxOptionVolatilitiesDefinition volatilitiesDefinition =
        marketDataConfig.get(FxOptionVolatilitiesDefinition.class, id.getName().getName());
    ValuationZoneTimeDefinition zoneTimeDefinition = marketDataConfig.get(ValuationZoneTimeDefinition.class);
    int nScenarios = marketData.getScenarioCount();
    MarketDataBox<LocalDate> valuationDates = marketData.getValuationDate();
    MarketDataBox<ZonedDateTime> valuationDateTimes = zoneTimeDefinition.toZonedDateTime(valuationDates);

    int nParameters = volatilitiesDefinition.getParameterCount();
    ImmutableList<MarketDataBox<Double>> inputs = volatilitiesDefinition.volatilitiesInputs().stream()
        .map(q -> marketData.getValue(q))
        .collect(toImmutableList());
    ImmutableList<FxOptionVolatilities> vols = IntStream.range(0, nScenarios)
        .mapToObj(scenarioIndex -> volatilitiesDefinition.volatilities(
            valuationDateTimes.getValue(scenarioIndex),
            DoubleArray.of(nParameters, paramIndex -> inputs.get(paramIndex).getValue(scenarioIndex)),
            refData))
        .collect(toImmutableList());

    return nScenarios > 1 ? MarketDataBox.ofScenarioValues(vols) : MarketDataBox.ofSingleValue(vols.get(0));
  }

  @Override
  public Class<FxOptionVolatilitiesId> getMarketDataIdType() {
    return FxOptionVolatilitiesId.class;
  }

}
