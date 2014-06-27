/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;
import org.threeten.bp.LocalDate;
import org.threeten.bp.OffsetTime;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.config.impl.ConfigItem;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.position.Counterparty;
import com.opengamma.core.position.Trade;
import com.opengamma.core.position.impl.SimpleCounterparty;
import com.opengamma.core.position.impl.SimpleTrade;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.analytics.curve.CurveGroupConfiguration;
import com.opengamma.financial.analytics.curve.exposure.CurrencyExposureFunction;
import com.opengamma.financial.analytics.curve.exposure.ExposureFunction;
import com.opengamma.financial.analytics.curve.exposure.ExposureFunctions;
import com.opengamma.financial.analytics.curve.exposure.SecurityTypeExposureFunction;
import com.opengamma.financial.security.fra.FRASecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.id.UniqueId;
import com.opengamma.master.config.ConfigMaster;
import com.opengamma.master.config.ConfigMasterUtils;
import com.opengamma.master.config.impl.InMemoryConfigMaster;
import com.opengamma.master.config.impl.MasterConfigSource;
import com.opengamma.master.security.impl.InMemorySecurityMaster;
import com.opengamma.master.security.impl.MasterSecuritySource;
import com.opengamma.service.ServiceContext;
import com.opengamma.service.ThreadLocalServiceContext;
import com.opengamma.service.VersionCorrectionProvider;
import com.opengamma.sesame.engine.FixedInstantVersionCorrectionProvider;
import com.opengamma.util.money.Currency;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.time.DateUtils;

/**
 * Unit test for MarketExposureSelector.
 */
@Test(groups = TestGroup.UNIT)
public class MarketExposureSelectorTest {

  @Test
  public void testEmptyCurveConfigs() {

    ConfigMaster configMaster = new InMemoryConfigMaster();
    ConfigSource configSource = new MasterConfigSource(configMaster);
    SecuritySource securitySource = new MasterSecuritySource(new InMemorySecurityMaster());

    String name = "test";
    List<String> exposureFunctions = Lists.newArrayList(CurrencyExposureFunction.NAME);
    Map<ExternalId, String> idsToNames = Maps.newHashMap();
    ExposureFunctions exposures = new ExposureFunctions(name, exposureFunctions, idsToNames);
    ConfigMasterUtils.storeByName(configMaster, ConfigItem.of(exposures));
    
    MarketExposureSelector selector = new MarketExposureSelector(exposures, securitySource, configSource);

    FRASecurity security = getFRASecurity();
    Trade trade = new SimpleTrade(security,
                                  BigDecimal.ONE,
                                  new SimpleCounterparty(ExternalId.of(Counterparty.DEFAULT_SCHEME, "TEST")),
                                  LocalDate.now(),
                                  OffsetTime.now());
    
    Set<CurveConstructionConfiguration> configs = selector.determineCurveConfigurations(trade);
    assertTrue("Expected curve configs to be empty", configs.isEmpty());
    
    configs = selector.findCurveConfigurationsForSecurity(security);
    assertTrue("Expected curve configs to be empty", configs.isEmpty());
  }
  
  @Test
  public void testMultipleCurveConfigs() {
    ConfigMaster configMaster = new InMemoryConfigMaster();
    ConfigSource configSource = new MasterConfigSource(configMaster);
    SecuritySource securitySource = new MasterSecuritySource(new InMemorySecurityMaster());

    FRASecurity security = getFRASecurity();
    
    String name = "test";
    List<String> exposureFunctions = Lists.newArrayList(SecurityTypeExposureFunction.NAME, CurrencyExposureFunction.NAME);
    Map<ExternalId, String> idsToNames = new HashMap<>();
    String securityTypeCurveConfigName = "SecurityTypeConfig";
    idsToNames.put(ExternalId.of(ExposureFunction.SECURITY_IDENTIFIER, security.getSecurityType()), securityTypeCurveConfigName);
    String currencyCurveConfigName = "CurrencyConfig";
    idsToNames.put(ExternalId.of(Currency.OBJECT_SCHEME, security.getCurrency().getCode()), currencyCurveConfigName);
    ExposureFunctions exposures = new ExposureFunctions(name, exposureFunctions, idsToNames);
    ConfigMasterUtils.storeByName(configMaster, ConfigItem.of(exposures));
    
    CurveConstructionConfiguration securityTypeCurveConfig =
        new CurveConstructionConfiguration(securityTypeCurveConfigName,
                                           new ArrayList<CurveGroupConfiguration>(),
                                           new ArrayList<String>());
    ConfigMasterUtils.storeByName(configMaster, ConfigItem.of(securityTypeCurveConfig));
    
    CurveConstructionConfiguration currencyCurveConfig =
        new CurveConstructionConfiguration(currencyCurveConfigName,
                                           new ArrayList<CurveGroupConfiguration>(),
                                           new ArrayList<String>());
    ConfigMasterUtils.storeByName(configMaster, ConfigItem.of(currencyCurveConfig));

    /* This must be called after saving config instances, otherwise the version correction provider won't find them */
    ImmutableMap.Builder<Class<?>, Object> builder = ImmutableMap.builder();
    builder.put(ConfigSource.class, configSource);
    builder.put(SecuritySource.class, securitySource);
    ServiceContext serviceContext = ServiceContext
        .of(builder.build())
        .with(VersionCorrectionProvider.class, new FixedInstantVersionCorrectionProvider());
    ThreadLocalServiceContext.init(serviceContext);

    MarketExposureSelector selector = new MarketExposureSelector(exposures, securitySource, configSource);
    
    Trade trade = new SimpleTrade(security,
                                  BigDecimal.ONE,
                                  new SimpleCounterparty(ExternalId.of(Counterparty.DEFAULT_SCHEME, "TEST")),
                                  LocalDate.now(),
                                  OffsetTime.now());
    Set<CurveConstructionConfiguration> configs = selector.determineCurveConfigurations(trade);
    assertEquals("Expected single curve config", 1, configs.size());
    assertTrue("Expected configs to contain security type config", configs.contains(securityTypeCurveConfig));
    assertFalse("Expected configs to not contain currency config", configs.contains(currencyCurveConfig));
  }

  private static FRASecurity getFRASecurity() {
    FRASecurity security = new FRASecurity(Currency.USD,
                                                 ExternalId.of("Test", "US"),
                                                 DateUtils.getUTCDate(2013, 3, 1),
                                                 DateUtils.getUTCDate(2013, 6, 1),
                                                 0.02,
                                                 1000,
                                                 ExternalSchemes.bloombergTickerSecurityId("US0003 Index"),
                                                 DateUtils.getUTCDate(2013, 6, 1));
    security.setUniqueId(UniqueId.of(UniqueId.EXTERNAL_SCHEME.getName(), "1234"));
    return security;
  }
}
