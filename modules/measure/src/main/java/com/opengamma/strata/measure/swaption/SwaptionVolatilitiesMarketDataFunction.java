/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.swaption;

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
import com.opengamma.strata.pricer.swaption.SwaptionVolatilities;
import com.opengamma.strata.pricer.swaption.SwaptionVolatilitiesId; 

/**
 * 
 */
public class SwaptionVolatilitiesMarketDataFunction 
implements MarketDataFunction<SwaptionVolatilities, SwaptionVolatilitiesId> {

  @Override
  public MarketDataRequirements requirements(SwaptionVolatilitiesId id, MarketDataConfig marketDataConfig) {

    SwaptionVolatilitiesDefinition volatilitiesDefinition = marketDataConfig.get(
        SwaptionVolatilitiesDefinition.class, id.getName().getName());
    return MarketDataRequirements.builder()
        .addValues(volatilitiesDefinition.volatilitiesInputs())
        .build();
  }

  @Override
  public MarketDataBox<SwaptionVolatilities> build(SwaptionVolatilitiesId id, MarketDataConfig marketDataConfig,
      ScenarioMarketData marketData, ReferenceData refData) {

    SwaptionVolatilitiesDefinition volatilitiesDefinition =
        marketDataConfig.get(SwaptionVolatilitiesDefinition.class, id.getName().getName());
    ValuationZoneTimeDefinition zoneTimeDefinition = marketDataConfig.get(ValuationZoneTimeDefinition.class);
    int nScenarios = marketData.getScenarioCount();
    MarketDataBox<LocalDate> valuationDates = marketData.getValuationDate();
    MarketDataBox<ZonedDateTime> valuationDateTimes = zoneTimeDefinition.toZonedDateTime(valuationDates);

    int nParameters = volatilitiesDefinition.getParameterCount();
    ImmutableList<MarketDataBox<Double>> inputs = volatilitiesDefinition.volatilitiesInputs().stream()
        .map(q -> marketData.getValue(q))
        .collect(toImmutableList());
    ImmutableList<SwaptionVolatilities> vols = IntStream.range(0, nScenarios)
        .mapToObj(i -> volatilitiesDefinition.volatilities(
            valuationDateTimes.getValue(i),
            DoubleArray.of(nParameters, n -> inputs.get(n).getValue(i)),
            null,// TODO scenario rates provider
            refData))
        .collect(toImmutableList());

    return nScenarios > 1 ? MarketDataBox.ofScenarioValues(vols) : MarketDataBox.ofSingleValue(vols.get(0));
  }

  @Override
  public Class<SwaptionVolatilitiesId> getMarketDataIdType() {
    return SwaptionVolatilitiesId.class;
  }
}
