/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import static org.testng.AssertJUnit.assertNotNull;

import org.testng.annotations.Test;

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
import com.opengamma.util.test.TestGroup;

/**
 * This requires an examples-simulated server running on http://localhost:8080
 */
@Test(groups = TestGroup.INTEGRATION)
public class ComponentMapTest {

  @Test(enabled = false)
  public void connect() {
    ComponentMap componentMap = ComponentMap.loadComponents("http://localhost:8080");
    assertNotNull(componentMap);
    assertNotNull(componentMap.getComponent(ConfigSource.class));
    assertNotNull(componentMap.getComponent(ConventionBundleSource.class));
    assertNotNull(componentMap.getComponent(ConventionSource.class));
    assertNotNull(componentMap.getComponent(ExchangeSource.class));
    assertNotNull(componentMap.getComponent(HolidaySource.class));
    assertNotNull(componentMap.getComponent(LegalEntitySource.class));
    assertNotNull(componentMap.getComponent(PositionSource.class));
    assertNotNull(componentMap.getComponent(RegionSource.class));
    assertNotNull(componentMap.getComponent(SecuritySource.class));
    assertNotNull(componentMap.getComponent(HistoricalTimeSeriesSource.class));
    assertNotNull(componentMap.getComponent(MarketDataSnapshotSource.class));
  }
}
