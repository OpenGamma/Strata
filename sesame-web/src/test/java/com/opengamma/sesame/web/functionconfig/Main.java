/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.web.functionconfig;

import static spark.Spark.delete;
import static spark.Spark.externalStaticFileLocation;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.opengamma.DataDuplicationException;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.config.impl.ConfigItem;
import com.opengamma.core.convention.ConventionSource;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.core.region.RegionSource;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.financial.analytics.curve.exposure.ExposureFunctions;
import com.opengamma.financial.convention.ConventionBundleSource;
import com.opengamma.financial.currency.SimpleCurrencyMatrix;
import com.opengamma.financial.security.irs.InterestRateSwapSecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.id.UniqueId;
import com.opengamma.master.config.ConfigDocument;
import com.opengamma.master.config.ConfigMaster;
import com.opengamma.master.config.impl.InMemoryConfigMaster;
import com.opengamma.master.historicaltimeseries.HistoricalTimeSeriesResolver;
import com.opengamma.sesame.ConfigDbMarketExposureSelectorFn;
import com.opengamma.sesame.DefaultCurveDefinitionFn;
import com.opengamma.sesame.DefaultCurveNodeConverterFn;
import com.opengamma.sesame.DefaultCurveSpecificationFn;
import com.opengamma.sesame.DefaultCurveSpecificationMarketDataFn;
import com.opengamma.sesame.DefaultDiscountingMulticurveBundleFn;
import com.opengamma.sesame.DefaultFXMatrixFn;
import com.opengamma.sesame.DefaultHistoricalTimeSeriesFn;
import com.opengamma.sesame.ExposureFunctionsDiscountingMulticurveCombinerFn;
import com.opengamma.sesame.InterpolatedMulticurveBundleFn;
import com.opengamma.sesame.OutputName;
import com.opengamma.sesame.config.SimpleFunctionModelConfig;
import com.opengamma.sesame.config.ViewColumn;
import com.opengamma.sesame.config.ViewOutput;
import com.opengamma.sesame.equity.DefaultEquityPresentValueFn;
import com.opengamma.sesame.equity.EquityPresentValueFn;
import com.opengamma.sesame.function.AvailableImplementations;
import com.opengamma.sesame.function.AvailableImplementationsImpl;
import com.opengamma.sesame.function.AvailableOutputs;
import com.opengamma.sesame.function.AvailableOutputsImpl;
import com.opengamma.sesame.graph.convert.ArgumentConverter;
import com.opengamma.sesame.graph.convert.DefaultArgumentConverter;
import com.opengamma.sesame.irfuture.DefaultInterestRateFutureFn;
import com.opengamma.sesame.irfuture.InterestRateFutureDiscountingCalculatorFactory;
import com.opengamma.sesame.irfuture.InterestRateFutureFn;
import com.opengamma.sesame.irs.DiscountingInterestRateSwapCalculatorFactory;
import com.opengamma.sesame.irs.DiscountingInterestRateSwapFn;
import com.opengamma.sesame.irs.InterestRateSwapFn;
import com.opengamma.sesame.marketdata.DefaultHistoricalMarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.trade.InterestRateFutureTrade;

import spark.Request;
import spark.Response;
import spark.Route;

/**
 * This is a temporary test server to save the hassle of setting up real web config for jersey etc.
 * When this project is merged into the rest of the web infrastructure that will be taken care of.
 */
public class Main {

  private static final Gson _gson = new Gson();

  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws IOException {
    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    availableOutputs.register(EquityPresentValueFn.class, InterestRateSwapFn.class, InterestRateFutureFn.class);
    Set<Class<?>> availableComponents = ImmutableSet.of(HolidaySource.class,
                                                        ConventionSource.class,
                                                        MarketDataFn.class,
                                                        HolidaySource.class,
                                                        RegionSource.class,
                                                        ConfigSource.class,
                                                        SecuritySource.class,
                                                        HistoricalTimeSeriesSource.class,
                                                        ConventionBundleSource.class,
                                                        HistoricalTimeSeriesResolver.class);
    AvailableImplementations availableImplementations = new AvailableImplementationsImpl();
    availableImplementations.register(DefaultEquityPresentValueFn.class,
                                      ExposureFunctionsDiscountingMulticurveCombinerFn.class,
                                      DefaultDiscountingMulticurveBundleFn.class,
                                      DiscountingInterestRateSwapFn.class,
                                      DefaultInterestRateFutureFn.class,
                                      InterestRateFutureDiscountingCalculatorFactory.class,
                                      DiscountingInterestRateSwapCalculatorFactory.class,
                                      InterpolatedMulticurveBundleFn.class,
                                      DefaultHistoricalTimeSeriesFn.class,
                                      DefaultFXMatrixFn.class,
                                      DefaultHistoricalMarketDataFn.class,
                                      DefaultCurveDefinitionFn.class,
                                      DefaultCurveSpecificationFn.class,
                                      DefaultCurveSpecificationMarketDataFn.class,
                                      ConfigDbMarketExposureSelectorFn.class,
                                      DefaultCurveNodeConverterFn.class);
    ConfigMaster configMaster = createConfigMaster();
    ArgumentConverter argumentConverter = new DefaultArgumentConverter();
    final ColumnConfigResource resource = new ColumnConfigResource(availableOutputs,
                                                                   availableImplementations,
                                                                   availableComponents,
                                                                   SimpleFunctionModelConfig.EMPTY,
                                                                   argumentConverter,
                                                                   configMaster);

    //--------------------------------------------------------------------

    externalStaticFileLocation("sesame-war-main/src/main/webapp/WEB-INF/pages/config");

    // route for listing all columns
    get(new Route("/columns") {
      @Override
      public Object handle(Request request, Response response) {
        response.type("application/json");
        return _gson.toJson(resource.getColumnsPageModel());
      }
    });

    // route for adding a new column
    post(new Route("/columns") {
      @Override
      public Object handle(Request request, Response response) {
        response.type("text/plain");
        try {
          return resource.addColumn(request.body()).getObjectId().toString();
        } catch (DataDuplicationException e) {
          response.status(409);
          return e.getMessage();
        }
      }
    });

    // gets the default configuration for new outputs
    get(new Route("/columns/defaultconfig") {
      @Override
      public Object handle(Request request, Response response) {
        response.type("application/json");
        return _gson.toJson(resource.getDefaultConfig());
      }
    });

    // gets the outputs for a column
    get(new Route("/columns/:columnId") {
      @Override
      public Object handle(Request request, Response response) {
        response.type("application/json");
        String columnId = request.params(":columnId");
        return _gson.toJson(resource.getColumnPageModel(UniqueId.parse(columnId)));
      }
    });

    // sends (but doesn't save) new config and receives an updated model
    post(new Route("/columns/:columnId") {
      @Override
      public Object handle(Request request, Response response) {
        response.type("application/json");
        String columnId = request.params(":columnId");
        return _gson.toJson(resource.getConfigPageModel(UniqueId.parse(columnId), request.body()));
      }
    });

    delete(new Route("/columns/:columnId") {
      @Override
      public Object handle(Request request, Response response) {
        String columnId = request.params(":columnId");
        resource.deleteColumn(UniqueId.parse(columnId));
        return "";
      }
    });

    // get the config for an output
    get(new Route("/columns/:columnId/:inputType") {
      @Override
      public Object handle(Request request, Response response) {
        response.type("application/json");
        String columnId = request.params(":columnId");
        String inputType = request.params(":inputType");
        return _gson.toJson(resource.getConfig(UniqueId.parse(columnId), inputType));
      }
    });

    // saves an output to a column
    put(new Route("/columns/:columnId/:inputType") {
      @Override
      public Object handle(Request request, Response response) {
        response.type("text/plain");
        String columnId = request.params(":columnId");
        String inputType = request.params(":inputType");
        UniqueId configId = resource.saveConfig(UniqueId.parse(columnId), inputType, request.body());
        return configId.getObjectId().toString();
      }
    });

    // deletes the config for one output
    delete(new Route("/columns/:columnId/:inputType") {
      @Override
      public Object handle(Request request, Response response) {
        response.type("text/plain");
        String columnId = request.params(":columnId");
        String inputType = request.params(":inputType");
        resource.deleteConfig(UniqueId.parse(columnId), inputType);
        return "";
      }
    });

    // TODO endpoint update column name but leave config unchanged
  }

  private static ConfigMaster createConfigMaster() {
    InMemoryConfigMaster configMaster = new InMemoryConfigMaster();
    configMaster.add(createColumn("PV01", InterestRateSwapSecurity.class, InterestRateFutureTrade.class));
    configMaster.add(createColumn("Par Rate", InterestRateSwapSecurity.class, InterestRateFutureTrade.class));
    configMaster.add(createColumn("Security Market Price", InterestRateFutureTrade.class));
    ExposureFunctions exposureFunctions = new ExposureFunctions("Default Exposure Functions",
                                                                ImmutableList.of("EF1", "EF2"),
                                                                ImmutableMap.<ExternalId, String>of());
    configMaster.add(new ConfigDocument(ConfigItem.of(exposureFunctions)));
    configMaster.add(new ConfigDocument(ConfigItem.of(new SimpleCurrencyMatrix(), "Default Currency Matrix")));
    return configMaster;
  }

  private static ConfigDocument createColumn(String name, Class<?>... inputTypes) {
    Map<Class<?>, ViewOutput> outputs = new HashMap<>();

    for (Class<?> inputType : inputTypes) {
      outputs.put(inputType, new ViewOutput(OutputName.of(name), SimpleFunctionModelConfig.EMPTY));
    }
    ViewColumn column = new ViewColumn(name, null, outputs);
    ConfigItem<ViewColumn> item = ConfigItem.of(column);
    item.setName(column.getName());
    return new ConfigDocument(item);
  }
}
