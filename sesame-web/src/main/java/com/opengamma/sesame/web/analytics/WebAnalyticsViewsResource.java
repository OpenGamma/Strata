/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.web.analytics;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.column;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.configureView;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static com.opengamma.sesame.config.ConfigBuilder.output;
import static com.opengamma.util.money.Currency.EUR;
import static com.opengamma.util.money.Currency.GBP;
import static com.opengamma.util.money.Currency.JPY;
import static com.opengamma.util.money.Currency.USD;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.joda.beans.impl.flexi.FlexiBean;
import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;
import org.threeten.bp.ZonedDateTime;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.DataNotFoundException;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.core.link.ConfigLink;
import com.opengamma.engine.marketdata.spec.LiveMarketDataSpecification;
import com.opengamma.financial.analytics.conversion.FXForwardSecurityConverter;
import com.opengamma.financial.analytics.curve.ConfigDBCurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.CurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.exposure.ConfigDBInstrumentExposuresProvider;
import com.opengamma.financial.analytics.curve.exposure.ExposureFunctions;
import com.opengamma.financial.analytics.curve.exposure.InstrumentExposuresProvider;
import com.opengamma.financial.currency.CurrencyMatrix;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.financial.security.FinancialSecurityVisitor;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.financial.security.fx.FXForwardSecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ObjectId;
import com.opengamma.id.UniqueId;
import com.opengamma.master.security.ManageableSecurity;
import com.opengamma.sesame.ConfigDbMarketExposureSelectorFn;
import com.opengamma.sesame.CurrencyPairsFn;
import com.opengamma.sesame.CurveDefinitionFn;
import com.opengamma.sesame.CurveNodeConverterFn;
import com.opengamma.sesame.CurveSpecificationFn;
import com.opengamma.sesame.CurveSpecificationMarketDataFn;
import com.opengamma.sesame.DefaultCurrencyPairsFn;
import com.opengamma.sesame.DefaultCurveDefinitionFn;
import com.opengamma.sesame.DefaultCurveNodeConverterFn;
import com.opengamma.sesame.DefaultCurveSpecificationFn;
import com.opengamma.sesame.DefaultCurveSpecificationMarketDataFn;
import com.opengamma.sesame.DefaultDiscountingMulticurveBundleFn;
import com.opengamma.sesame.DefaultFXMatrixFn;
import com.opengamma.sesame.DiscountingMulticurveBundleFn;
import com.opengamma.sesame.DiscountingMulticurveCombinerFn;
import com.opengamma.sesame.ExposureFunctionsDiscountingMulticurveCombinerFn;
import com.opengamma.sesame.FXMatrixFn;
import com.opengamma.sesame.MarketExposureSelectorFn;
import com.opengamma.sesame.OutputNames;
import com.opengamma.sesame.RootFinderConfiguration;
import com.opengamma.sesame.TimeSeriesReturnConverter;
import com.opengamma.sesame.TimeSeriesReturnConverterFactory;
import com.opengamma.sesame.component.RetrievalPeriod;
import com.opengamma.sesame.component.StringSet;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.config.ViewConfig;
import com.opengamma.sesame.fxforward.DiscountingFXForwardPVFn;
import com.opengamma.sesame.fxforward.DiscountingFXForwardSpotPnLSeriesFn;
import com.opengamma.sesame.fxforward.FXForwardCalculatorFn;
import com.opengamma.sesame.fxforward.FXForwardDiscountingCalculatorFn;
import com.opengamma.sesame.fxforward.FXForwardPVFn;
import com.opengamma.sesame.marketdata.DefaultHistoricalMarketDataFn;
import com.opengamma.sesame.marketdata.DefaultMarketDataFn;
import com.opengamma.sesame.marketdata.HistoricalMarketDataFn;
import com.opengamma.sesame.pnl.DefaultHistoricalPnLFXConverterFn;
import com.opengamma.sesame.pnl.PnLPeriodBound;
import com.opengamma.sesame.server.FunctionServer;
import com.opengamma.sesame.server.FunctionServerRequest;
import com.opengamma.sesame.server.IndividualCycleOptions;
import com.opengamma.util.money.Currency;
import com.opengamma.util.time.LocalDateRange;

/**
 * RESTful resource for all analytic views.
 * <p>
 * The analytics resource represents all analytic views.
 */
@Path("/analytics/views")
public class WebAnalyticsViewsResource extends AbstractWebAnalyticsResource {

  /**
   * Dummy ID for inline view.
   */
  private static final ObjectId OID_INLINE_1000 = ObjectId.of("Inline", "1000");

  /**
   * Creates the resource.
   * @param functionServer  the function server, not null
   */
  public WebAnalyticsViewsResource(FunctionServer functionServer) {
    super(functionServer);
  }

  //-------------------------------------------------------------------------
  @GET
  @Produces(MediaType.TEXT_HTML)
  public String getHTML() {
    FlexiBean out = createRootData();
    return getFreemarker().build(HTML_DIR + "views.ftl", out);
  }

  //-------------------------------------------------------------------------
  @Path("{viewId}")
  public WebAnalyticViewResource findView(@PathParam("viewId") String idStr) {
    data().setUriViewId(idStr);
    UniqueId oid = UniqueId.parse(idStr);
    if (oid.equalObjectId(OID_INLINE_1000)) {
      data().setCalculationRequest(createInline1000());
    } else {
      throw new DataNotFoundException("View not found: " + oid);
    }
    return new WebAnalyticViewResource(this);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates the output root data.
   * @return the output root data, not null
   */
  protected FlexiBean createRootData() {
    FlexiBean out = super.createRootData();
    List<ObjectId> list = ImmutableList.of(OID_INLINE_1000);
    out.put("views", list);
    return out;
  }

  //-------------------------------------------------------------------------
  /**
   * Builds a URI.
   * @param data  the data, not null
   * @return the URI, not null
   */
  public static URI uri(WebAnalyticsData data) {
    UriBuilder builder = data.getUriInfo().getBaseUriBuilder().path(WebAnalyticsViewsResource.class);
    return builder.build();
  }

  //-------------------------------------------------------------------------
  //-------------------------------------------------------------------------
  //-------------------------------------------------------------------------
  private static final AtomicLong s_nextId = new AtomicLong(0);

  private FunctionServerRequest<IndividualCycleOptions> createInline1000() {
    ZonedDateTime valuationTime = ZonedDateTime.now();
    LocalDate seriesStart = LocalDate.of(2013, 11, 12);
    LocalDate seriesEnd = LocalDate.of(2014, 1, 10);
    TimeSeriesReturnConverter spotVWConverter = TimeSeriesReturnConverterFactory.relativeVolatilityWeighted(0.93);
    LocalDateRange range = LocalDateRange.of(seriesStart, seriesEnd, true);
    Boolean useHistoricalSpot = false;
    ViewConfig viewConfig = configureView(
        "Example view",
        createFunctionConfig(),
        column(
            "Present Value",
            output(OutputNames.FX_PRESENT_VALUE, FXForwardSecurity.class),
            output(OutputNames.PRESENT_VALUE, EquitySecurity.class)),
        column("Spot PnL Series",
            output(OutputNames.PNL_SERIES, FXForwardSecurity.class,
               config(
                   arguments(
                       function(
                           DiscountingFXForwardSpotPnLSeriesFn.class,
                           argument("dateRange", range),
                           argument("outputCurrency", Optional.of(Currency.USD)),
                           argument("useHistoricalSpot", useHistoricalSpot),
                           argument("endDate", valuationTime.toLocalDate()),
                           argument("timeSeriesConverter", spotVWConverter)))))));
    
    IndividualCycleOptions cycleOptions = IndividualCycleOptions.builder()
        .valuationTime(valuationTime)
        .marketDataSpec(LiveMarketDataSpecification.of("Bloomberg"))
//        .marketDataSpec(new FixedHistoricalMarketDataSpecification(LocalDate.now().minusDays(2)))
        .build();
    
    ImmutableList<ManageableSecurity> securities = ImmutableList.<ManageableSecurity>of(
        createRandomFxForwardSecurity(), createRandomFxForwardSecurity(), createRandomFxForwardSecurity(),
        createEquitySecurity1(), createEquitySecurity2(), createEquitySecurity3(), createEquitySecurity4(), createEquitySecurity5());
    FunctionServerRequest<IndividualCycleOptions> request =
        FunctionServerRequest.<IndividualCycleOptions>builder()
            .viewConfig(viewConfig)
            .inputs(securities)
            .cycleOptions(cycleOptions)
            .build();
    return request;
  }

  private static FXForwardSecurity createRandomFxForwardSecurity() {
    ExternalId regionId = ExternalId.of(ExternalSchemes.FINANCIAL, "US");
    double usdAmount = 10_000_000 * Math.random();
    double eurAmount = usdAmount * (1.31 + 0.04 * Math.random());
    Currency payCcy;
    Currency recCcy;
    double payAmount;
    double recAmount;
    if (Math.random() < 0.5) {
      payAmount = usdAmount;
      payCcy = USD;
      recAmount = eurAmount;
      recCcy = EUR;
    } else {
      payAmount = eurAmount;
      payCcy = EUR;
      recAmount = usdAmount;
      recCcy = USD;
    }
    ZonedDateTime forwardDate = ZonedDateTime.now().plusWeeks((long) (104 * Math.random()));
    FXForwardSecurity security = new FXForwardSecurity(payCcy, payAmount, recCcy, recAmount, forwardDate, regionId);
    String id = Long.toString(s_nextId.getAndIncrement());
    security.setUniqueId(UniqueId.of("fxFwdSec", id));
    security.setName("FX forward " + id);
    return security;
  }

  private static EquitySecurity createEquitySecurity1() {
    ExternalId securityId = ExternalId.of(ExternalSchemes.BLOOMBERG_TICKER, "ACE US Equity");
    EquitySecurity sec = new EquitySecurity("NEW YORK STOCK EXCHANGE INC.", "XNYS", "ACE LTD", Currency.USD);
    sec.setExternalIdBundle(securityId.toBundle());
    sec.setShortName("ACE");
    sec.setName("ACE US Equity");
    return sec;
  }

  private static EquitySecurity createEquitySecurity2() {
    ExternalId securityId = ExternalId.of(ExternalSchemes.BLOOMBERG_TICKER, "IBM US Equity");
    EquitySecurity sec = new EquitySecurity("NEW YORK STOCK EXCHANGE INC.", "XNYS", "INTL BUSINESS MACHINES CORP", Currency.USD);
    sec.setExternalIdBundle(securityId.toBundle());
    sec.setShortName("IBM");
    sec.setName("IBM US Equity");
    return sec;
  }

  private static EquitySecurity createEquitySecurity3() {
    ExternalId securityId = ExternalId.of(ExternalSchemes.BLOOMBERG_TICKER, "AAPL US Equity");
    EquitySecurity sec = new EquitySecurity("NASDAQ/NGS (GLOBAL SELECT MARKET)", "XNGS", "APPLE", Currency.USD);
    sec.setExternalIdBundle(securityId.toBundle());
    sec.setShortName("AAPL");
    sec.setName("AAPL US Equity");
    return sec;
  }

  private static EquitySecurity createEquitySecurity4() {
    ExternalId securityId = ExternalId.of(ExternalSchemes.BLOOMBERG_TICKER, "VOD LN Equity");
    EquitySecurity sec = new EquitySecurity("LONDON STOCK EXCHANGE", "XLON", "VODAFONE GROUP PLC", Currency.USD);
    sec.setExternalIdBundle(securityId.toBundle());
    sec.setShortName("VOD");
    sec.setName("VOD LN Equity");
    return sec;
  }

  private static EquitySecurity createEquitySecurity5() {
    ExternalId securityId = ExternalId.of(ExternalSchemes.BLOOMBERG_TICKER, "HSBA LN Equity");
    EquitySecurity sec = new EquitySecurity("LONDON STOCK EXCHANGE", "XLON", "HSBC HOLDINGS PLC", Currency.USD);
    sec.setExternalIdBundle(securityId.toBundle());
    sec.setShortName("HSBA");
    sec.setName("HSBA LN Equity");
    return sec;
  }

  private static FunctionModelConfig createFunctionConfig() {
    ConfigLink<CurrencyMatrix> currencyMatrixLink = ConfigLink.resolvable("BloombergLiveData", CurrencyMatrix.class);
    ConfigLink<ExposureFunctions> exposureFunctionsLink = ConfigLink.resolvable("Temple Exposure Config", ExposureFunctions.class);
    
    return config(
        arguments(
            function(
                ConfigDbMarketExposureSelectorFn.class,
                argument("exposureConfig", exposureFunctionsLink)),
            function(
                RootFinderConfiguration.class,
                argument("rootFinderAbsoluteTolerance", 1e-9),
                argument("rootFinderRelativeTolerance", 1e-9),
                argument("rootFinderMaxIterations", 1000)),
            function(
                DefaultCurrencyPairsFn.class,
                argument(
                    "currencyPairs",
                    ImmutableSet.of(
                        CurrencyPair.of(USD, JPY),
                        CurrencyPair.of(EUR, USD),
                        CurrencyPair.of(GBP, USD)))),
            function(DefaultCurveNodeConverterFn.class,
                argument("timeSeriesDuration", RetrievalPeriod.of(Period.ofYears(1)))),
            function(DefaultHistoricalMarketDataFn.class,
                argument("dataSource", "BLOOMBERG")),
            function(
                DefaultDiscountingMulticurveBundleFn.class,
                argument("impliedCurveNames", StringSet.of("Temple Implied Deposit Curve EUR"))),
            function(DefaultHistoricalPnLFXConverterFn.class,
                argument("periodBound", PnLPeriodBound.START)),
            function(
                DefaultHistoricalMarketDataFn.class,
                argument("dataSource", "BLOOMBERG"),
                argument("currencyMatrix", currencyMatrixLink)),
            function(DefaultMarketDataFn.class,
                argument("currencyMatrix", currencyMatrixLink))),
        implementations(
            CurrencyPairsFn.class, DefaultCurrencyPairsFn.class,
            CurveConstructionConfigurationSource.class, ConfigDBCurveConstructionConfigurationSource.class,
            CurveDefinitionFn.class, DefaultCurveDefinitionFn.class,
            CurveNodeConverterFn.class, DefaultCurveNodeConverterFn.class,
            CurveSpecificationFn.class, DefaultCurveSpecificationFn.class,
            CurveSpecificationMarketDataFn.class, DefaultCurveSpecificationMarketDataFn.class,
            DiscountingMulticurveBundleFn.class, DefaultDiscountingMulticurveBundleFn.class,
            DiscountingMulticurveCombinerFn.class, ExposureFunctionsDiscountingMulticurveCombinerFn.class,
            FinancialSecurityVisitor.class, FXForwardSecurityConverter.class,
            FXForwardCalculatorFn.class, FXForwardDiscountingCalculatorFn.class,
            FXForwardPVFn.class, DiscountingFXForwardPVFn.class,
            FXMatrixFn.class, DefaultFXMatrixFn.class,
            HistoricalMarketDataFn.class, DefaultHistoricalMarketDataFn.class,
            InstrumentExposuresProvider.class, ConfigDBInstrumentExposuresProvider.class,
            MarketExposureSelectorFn.class, ConfigDbMarketExposureSelectorFn.class));
  }

}
