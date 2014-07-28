/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.integration;

import static com.opengamma.util.money.Currency.EUR;
import static com.opengamma.util.money.Currency.USD;
import static com.opengamma.util.result.FailureStatus.MISSING_DATA;
import static com.opengamma.util.result.SuccessStatus.SUCCESS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.hamcrest.MatcherAssert;
import org.testng.annotations.Test;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableMap;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.financial.analytics.DoubleLabelledMatrix1D;
import com.opengamma.financial.security.fx.FXForwardSecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.id.UniqueId;
import com.opengamma.master.historicaltimeseries.HistoricalTimeSeriesResolver;
import com.opengamma.master.historicaltimeseries.impl.RemoteHistoricalTimeSeriesResolver;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.SimpleEnvironment;
import com.opengamma.sesame.cache.CachingProxyDecorator;
import com.opengamma.sesame.cache.ExecutingMethodsThreadLocal;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.scenarios.curvedata.FunctionTestUtils;
import com.opengamma.sesame.fxforward.FXForwardYieldCurveNodeSensitivitiesFn;
import com.opengamma.sesame.fxforward.FxForwardYieldCurveNodeSensitivitiesFnTest;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.sesame.marketdata.FixedHistoricalMarketDataSource;
import com.opengamma.sesame.proxy.TimingProxy;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultStatus;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.INTEGRATION)
public class FxForwardYieldCurveNodeSensitivitiesFnIntegrationTest {

  private static final ZonedDateTime s_valuationTime = ZonedDateTime.of(2013, 11, 7, 11, 0, 0, 0, ZoneOffset.UTC);

  @Test(groups = TestGroup.INTEGRATION, enabled = false)
  public void executeAgainstRemoteServerWithNoData() throws IOException {
    Result<DoubleLabelledMatrix1D> ycns = executeAgainstRemoteServer();
    assertNotNull(ycns);
    MatcherAssert.assertThat(ycns.getStatus(), is((ResultStatus) MISSING_DATA));
  }

  @Test(groups = TestGroup.INTEGRATION, enabled = false)
  public void executeAgainstRemoteServerWithData() throws IOException {
    Result<DoubleLabelledMatrix1D> ycns = executeAgainstRemoteServer();
    assertNotNull(ycns);
    assertThat(ycns.getStatus(), is((ResultStatus) SUCCESS));
  }

  private Result<DoubleLabelledMatrix1D> executeAgainstRemoteServer() {
    String serverUrl = "http://localhost:8080";
    ComponentMap serverComponents = ComponentMap.loadComponents(serverUrl);
    HistoricalTimeSeriesSource timeSeriesSource = serverComponents.getComponent(HistoricalTimeSeriesSource.class);
    LocalDate date = LocalDate.of(2013, 11, 7);
    FixedHistoricalMarketDataSource dataSource = new FixedHistoricalMarketDataSource(timeSeriesSource, date, "BLOOMBERG", null);

    URI htsResolverUri = URI.create(serverUrl + "/jax/components/HistoricalTimeSeriesResolver/shared");
    HistoricalTimeSeriesResolver htsResolver = new RemoteHistoricalTimeSeriesResolver(htsResolverUri);
    Map<Class<?>, Object> comps = ImmutableMap.<Class<?>, Object>of(HistoricalTimeSeriesResolver.class, htsResolver);
    ComponentMap componentMap = serverComponents.with(comps);

    CachingProxyDecorator cachingDecorator = new CachingProxyDecorator(
        FunctionTestUtils.createCache(),
        new ExecutingMethodsThreadLocal());
    FXForwardYieldCurveNodeSensitivitiesFn ycnsFunction =
        FunctionModel.build(
            FXForwardYieldCurveNodeSensitivitiesFn.class,
            FxForwardYieldCurveNodeSensitivitiesFnTest.createFunctionConfig(),
            componentMap,
            TimingProxy.INSTANCE,
            cachingDecorator);
    ExternalId regionId = ExternalId.of(ExternalSchemes.FINANCIAL, "US");
    ZonedDateTime forwardDate = ZonedDateTime.of(2014, 11, 7, 12, 0, 0, 0, ZoneOffset.UTC);
    FXForwardSecurity security = new FXForwardSecurity(EUR, 10_000_000, USD, 14_000_000, forwardDate, regionId);
    security.setUniqueId(UniqueId.of("sec", "123"));
    //TracingProxy.start(new FullTracer());
    Result<DoubleLabelledMatrix1D> result = null;
    Environment env = new SimpleEnvironment(s_valuationTime, dataSource);

    for (int i = 0; i < 100; i++) {
      result = ycnsFunction.calculateYieldCurveNodeSensitivities(env, security);
      System.out.println();
    }
    //System.out.println(TracingProxy.end().prettyPrint());
    return result;
  }


}
