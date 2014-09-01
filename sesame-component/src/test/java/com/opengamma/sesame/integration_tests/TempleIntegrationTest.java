/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.integration_tests;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.testng.Assert.fail;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.core.config.impl.ConfigItem;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.financial.analytics.conversion.FXForwardSecurityConverter;
import com.opengamma.financial.analytics.curve.ConfigDBCurveConstructionConfigurationSource;
import com.opengamma.financial.analytics.curve.exposure.ConfigDBInstrumentExposuresProvider;
import com.opengamma.financial.analytics.curve.exposure.ExposureFunctions;
import com.opengamma.financial.currency.AbstractCurrencyMatrix;
import com.opengamma.financial.currency.CurrencyMatrixValue;
import com.opengamma.financial.currency.CurrencyMatrixValue.CurrencyMatrixExternalId;
import com.opengamma.id.ExternalId;
import com.opengamma.sesame.ConfigDbMarketExposureSelectorFn;
import com.opengamma.sesame.DefaultCurrencyPairsFn;
import com.opengamma.sesame.DefaultCurveDefinitionFn;
import com.opengamma.sesame.DefaultCurveNodeConverterFn;
import com.opengamma.sesame.DefaultCurveSpecificationFn;
import com.opengamma.sesame.DefaultCurveSpecificationMarketDataFn;
import com.opengamma.sesame.DefaultDiscountingMulticurveBundleFn;
import com.opengamma.sesame.DefaultDiscountingMulticurveBundleResolverFn;
import com.opengamma.sesame.DefaultFXMatrixFn;
import com.opengamma.sesame.DefaultFXReturnSeriesFn;
import com.opengamma.sesame.DefaultHistoricalTimeSeriesFn;
import com.opengamma.sesame.DiscountingMulticurveBundleResolverFn;
import com.opengamma.sesame.ExposureFunctionsDiscountingMulticurveCombinerFn;
import com.opengamma.sesame.component.CapturedResultsLoader;
import com.opengamma.sesame.engine.ResultRow;
import com.opengamma.sesame.engine.Results;
import com.opengamma.sesame.engine.ViewInputs;
import com.opengamma.sesame.engine.ViewOutputs;
import com.opengamma.sesame.engine.ViewResultsDeserializer;
import com.opengamma.sesame.equity.DefaultEquityPresentValueFn;
import com.opengamma.sesame.equity.EquityPresentValueFn;
import com.opengamma.sesame.fra.FRAFn;
import com.opengamma.sesame.function.AvailableImplementations;
import com.opengamma.sesame.function.AvailableImplementationsImpl;
import com.opengamma.sesame.function.AvailableOutputs;
import com.opengamma.sesame.function.AvailableOutputsImpl;
import com.opengamma.sesame.fxforward.DiscountingFXForwardPVFn;
import com.opengamma.sesame.fxforward.DiscountingFXForwardSpotPnLSeriesFn;
import com.opengamma.sesame.fxforward.DiscountingFXForwardYCNSPnLSeriesFn;
import com.opengamma.sesame.fxforward.DiscountingFXForwardYieldCurveNodeSensitivitiesFn;
import com.opengamma.sesame.fxforward.FXForwardDiscountingCalculatorFn;
import com.opengamma.sesame.fxforward.FXForwardPVFn;
import com.opengamma.sesame.fxforward.FXForwardPnLSeriesFn;
import com.opengamma.sesame.fxforward.FXForwardYCNSPnLSeriesFn;
import com.opengamma.sesame.fxforward.FXForwardYieldCurveNodeSensitivitiesFn;
import com.opengamma.sesame.irs.InterestRateSwapFn;
import com.opengamma.sesame.marketdata.DefaultHistoricalMarketDataFn;
import com.opengamma.sesame.marketdata.DefaultMarketDataFn;
import com.opengamma.sesame.marketdata.FixedHistoricalMarketDataFactory;
import com.opengamma.sesame.pnl.DefaultHistoricalPnLFXConverterFn;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;

/**
 * Integration testing that FX Forwards behave as expected
 * by the Temple project.
 */
@Test(groups = TestGroup.UNIT)
public class TempleIntegrationTest {

  @Test
  public void testViewRunsAsExpected() throws FileNotFoundException {

    ViewInputs viewInputs = deserializeComponent(ViewInputs.class, "/integration_tests/templeViewInputs.xml");
    ViewOutputs viewOutputs = deserializeComponent(ViewOutputs.class, "/integration_tests/templeViewOutputs.xml");

    CapturedResultsLoader loader =
        new CapturedResultsLoader(viewInputs, createAvailableOutputs(), createAvailableImplementations());

    // These are not captured by the standard process as links are
    // executed before proxies are available. When this is changed
    // all should be ok and these can be removed
    loader.addExtraConfigData(ConfigItem.of(createExposureFunction()));
    loader.addExtraConfigData("BloombergLiveData", ConfigItem.of(new TempCurrencyMatrix()));

    Results results = loader.runViewFromInputs();

    compareResults(results, viewOutputs);
  }

  private ExposureFunctions createExposureFunction() {
    return new ExposureFunctions(
        "Temple Exposure Config", ImmutableList.of("Currency"),
        ImmutableMap.of(
            ExternalId.of(Currency.OBJECT_SCHEME, "USD"), "Temple USD",
            ExternalId.of(Currency.OBJECT_SCHEME, "KRW"), "Temple Implied Deposit KRW"));
  }

  private <T> T deserializeComponent(Class<T> clss, String fileName) throws FileNotFoundException {
    ViewResultsDeserializer resultsDeserializer =
        new ViewResultsDeserializer(getClass().getResourceAsStream(fileName));
    return resultsDeserializer.deserialize(clss);
  }

  private void compareResults(Results results, ViewOutputs originalOutputs) {

    assertThat(results.getColumnNames(), is(originalOutputs.getColumnNames()));
    assertThat(results.getNonPortfolioResults(), is(originalOutputs.getNonPortfolioResults()));

    List<ResultRow> originalOutputsRows = originalOutputs.getRows();
    List<String> errors = new ArrayList<>();

    for (int row = 0; row < originalOutputsRows.size(); row++) {

      ResultRow originalResultRow = originalOutputsRows.get(row);
      ResultRow calculatedRow = results.getRows().get(row);

      for (int col = 0; col < originalOutputs.getColumnNames().size(); col++) {

        Result<Object> originalResult = originalResultRow.get(col).getResult();
        Result<Object> calculatedResult = calculatedRow.get(col).getResult();

        if (!originalResult.equals(calculatedResult)) {
          errors.add("Row: " + originalResultRow.getInput() + ", Col: " + originalOutputs.getColumnNames().get(col) +
          "\nExpected: " + originalResult.toString() + "\nbut got: " + calculatedResult.toString());
        }
      }
    }

    if (!errors.isEmpty()) {
      fail(errors.toString());
    }
  }

  private AvailableOutputs createAvailableOutputs() {
    AvailableOutputs available = new AvailableOutputsImpl();
    available.register(DiscountingMulticurveBundleResolverFn.class,
                       EquityPresentValueFn.class,
                       FRAFn.class,
                       InterestRateSwapFn.class,
                       FXForwardPnLSeriesFn.class,
                       FXForwardPVFn.class,
                       FXForwardYCNSPnLSeriesFn.class,
                       FXForwardYieldCurveNodeSensitivitiesFn.class);
    return available;
  }

  private AvailableImplementations createAvailableImplementations() {
    AvailableImplementations available = new AvailableImplementationsImpl();
    available.register(
        DiscountingFXForwardYieldCurveNodeSensitivitiesFn.class,
        DiscountingFXForwardSpotPnLSeriesFn.class,
        DiscountingFXForwardYCNSPnLSeriesFn.class,
        DiscountingFXForwardPVFn.class,
        DefaultFXReturnSeriesFn.class,
        DefaultCurrencyPairsFn.class,
        DefaultEquityPresentValueFn.class,
        FXForwardSecurityConverter.class,
        ConfigDBInstrumentExposuresProvider.class,
        DefaultCurveSpecificationMarketDataFn.class,
        DefaultFXMatrixFn.class,
        DefaultCurveDefinitionFn.class,
        DefaultDiscountingMulticurveBundleFn.class,
        DefaultDiscountingMulticurveBundleResolverFn.class,
        DefaultCurveSpecificationFn.class,
        ConfigDBCurveConstructionConfigurationSource.class,
        DefaultHistoricalTimeSeriesFn.class,
        FXForwardDiscountingCalculatorFn.class,
        ConfigDbMarketExposureSelectorFn.class,
        ExposureFunctionsDiscountingMulticurveCombinerFn.class,
        FixedHistoricalMarketDataFactory.class,
        DefaultMarketDataFn.class,
        DefaultHistoricalMarketDataFn.class,
        DefaultCurveNodeConverterFn.class,
        DefaultHistoricalPnLFXConverterFn.class);
    return available;
  }

  private class TempCurrencyMatrix extends AbstractCurrencyMatrix {

    private TempCurrencyMatrix() {
      CurrencyMatrixExternalId value = CurrencyMatrixValue.of(
          ExternalSchemes.bloombergTickerSecurityId("KRW Curncy").toBundle(), "Market_Value");
      addConversion(Currency.of("KRW"), Currency.USD, value);
      addConversion(Currency.USD, Currency.of("KRW"), value.getReciprocal());
    }
  }
}
