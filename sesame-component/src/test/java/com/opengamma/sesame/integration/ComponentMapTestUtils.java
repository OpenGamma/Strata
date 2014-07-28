/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.integration;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.opengamma.component.tool.ToolContextUtils;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.convention.ConventionSource;
import com.opengamma.core.exchange.ExchangeSource;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.core.legalentity.LegalEntitySource;
import com.opengamma.core.marketdatasnapshot.MarketDataSnapshotSource;
import com.opengamma.core.position.PositionSource;
import com.opengamma.core.region.RegionSource;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.financial.convention.ConventionBundleSource;
import com.opengamma.financial.tool.ToolContext;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.util.ArgumentChecker;

/**
 * Utils for remote component maps.
 */
public final class ComponentMapTestUtils {

  private ComponentMapTestUtils() {
  }

  //-------------------------------------------------------------------------
  /**
   * Loads the component map from a tool context.
   * 
   * @param location  the location of the component config, can be a classpath:
   *  or file: resource or the URL or a remote server
   * @return the available components, keyed by type
   */
  public static ComponentMap fromToolContext(String location) {
    ArgumentChecker.notEmpty(location, "location");
    ImmutableClassToInstanceMap.Builder<Object> builder = ImmutableClassToInstanceMap.builder();
    ToolContext toolContext = ToolContextUtils.getToolContext(location, ToolContext.class);

    builder.put(ConfigSource.class, toolContext.getConfigSource());
    builder.put(ConventionBundleSource.class, toolContext.getConventionBundleSource());
    builder.put(ConventionSource.class, toolContext.getConventionSource());
    builder.put(ExchangeSource.class, toolContext.getExchangeSource());
    builder.put(HolidaySource.class, toolContext.getHolidaySource());
    builder.put(LegalEntitySource.class, toolContext.getLegalEntitySource());
    builder.put(PositionSource.class, toolContext.getPositionSource());
    builder.put(RegionSource.class, toolContext.getRegionSource());
    builder.put(SecuritySource.class, toolContext.getSecuritySource());
    builder.put(HistoricalTimeSeriesSource.class, toolContext.getHistoricalTimeSeriesSource());
    builder.put(MarketDataSnapshotSource.class, toolContext.getMarketDataSnapshotSource());
    return ComponentMap.of(builder.build());
  }

}
