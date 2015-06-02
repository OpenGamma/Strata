/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.finance.rate.fra.FraTemplate;
import com.opengamma.strata.function.interpolator.CurveExtrapolators;
import com.opengamma.strata.function.interpolator.CurveInterpolators;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveParameterMetadata;
import com.opengamma.strata.market.curve.ParRates;
import com.opengamma.strata.market.curve.config.CurveConfig;
import com.opengamma.strata.market.curve.config.CurveGroupConfig;
import com.opengamma.strata.market.curve.config.FraCurveNode;
import com.opengamma.strata.market.curve.config.InterpolatedCurveConfig;
import com.opengamma.strata.market.id.ParRatesId;
import com.opengamma.strata.market.id.QuoteId;
import com.opengamma.strata.market.key.QuoteKey;

@Test
public class ParRatesMarketDataFunctionTest {

  private static final LocalDate VALUATION_DATE = date(2011, 3, 8);

  /**
   * Test that the curve node requirements are extracted and returned.
   */
  public void requirements() {
    FraCurveNode node1x4 = fraNode(1, "a");
    FraCurveNode node2x5 = fraNode(2, "b");
    FraCurveNode node3x6 = fraNode(3, "c");

    InterpolatedCurveConfig curve = InterpolatedCurveConfig.builder()
        .name(CurveName.of("curve"))
        .interpolator(CurveInterpolators.DOUBLE_QUADRATIC)
        .leftExtrapolator(CurveExtrapolators.FLAT)
        .rightExtrapolator(CurveExtrapolators.FLAT)
        .nodes(node1x4, node2x5, node3x6)
        .build();

    CurveGroupConfig groupConfig = CurveGroupConfig.builder()
        .name(CurveGroupName.of("curve group"))
        .addDiscountingCurve(curve, Currency.USD)
        .build();

    MarketDataConfig marketDataConfig = MarketDataConfig.builder()
        .add(groupConfig.getName(), groupConfig)
        .build();

    ParRatesMarketDataFunction marketDataFunction = new ParRatesMarketDataFunction();
    ParRatesId parRatesId = ParRatesId.of(groupConfig.getName(), curve.getName(), MarketDataFeed.NONE);
    MarketDataRequirements requirements = marketDataFunction.requirements(parRatesId, marketDataConfig);

    assertThat(requirements.getObservables())
        .contains(QuoteId.of(StandardId.of("test", "a")))
        .contains(QuoteId.of(StandardId.of("test", "b")))
        .contains(QuoteId.of(StandardId.of("test", "c")));
  }

  /**
   * Test that the requirements are empty if there is no curve group configuration corresponding to the par rates ID
   */
  public void requirementsMissingGroupConfig() {
    ParRatesMarketDataFunction marketDataFunction = new ParRatesMarketDataFunction();
    ParRatesId parRatesId = ParRatesId.of(CurveGroupName.of("curve group"), CurveName.of("curve"), MarketDataFeed.NONE);
    MarketDataRequirements requirements = marketDataFunction.requirements(parRatesId, MarketDataConfig.empty());
    assertThat(requirements.getObservables()).isEmpty();
  }

  /**
   * Test that requirements are empty if the curve group config exists but not the curve
   */
  public void requirementsMissingCurveConfig() {
    ParRatesMarketDataFunction marketDataFunction = new ParRatesMarketDataFunction();
    ParRatesId parRatesId = ParRatesId.of(CurveGroupName.of("curve group"), CurveName.of("curve"), MarketDataFeed.NONE);
    CurveGroupConfig groupConfig = CurveGroupConfig.builder().name(CurveGroupName.of("curve group")).build();
    MarketDataConfig marketDataConfig = MarketDataConfig.builder().add(groupConfig.getName(), groupConfig).build();
    MarketDataRequirements requirements = marketDataFunction.requirements(parRatesId, marketDataConfig);
    assertThat(requirements.getObservables()).isEmpty();
  }

  /**
   * Test that requirements are empty if the curve config is found but is not of the expected type
   */
  public void requirementsNotInterpolatedCurve() {
    ParRatesMarketDataFunction marketDataFunction = new ParRatesMarketDataFunction();
    ParRatesId parRatesId = ParRatesId.of(CurveGroupName.of("curve group"), CurveName.of("curve"), MarketDataFeed.NONE);
    CurveConfig curve = mock(CurveConfig.class);
    when(curve.getName()).thenReturn(CurveName.of("curve"));
    CurveGroupConfig groupConfig = CurveGroupConfig.builder()
        .name(CurveGroupName.of("curve group"))
        .addDiscountingCurve(curve, Currency.USD)
        .build();
    MarketDataConfig marketDataConfig = MarketDataConfig.builder().add(groupConfig.getName(), groupConfig).build();
    MarketDataRequirements requirements = marketDataFunction.requirements(parRatesId, marketDataConfig);
    assertThat(requirements.getObservables()).isEmpty();
  }

  /**
   * Test that ParRates are correctly built from market data.
   */
  public void build() {
    FraCurveNode node1x4 = fraNode(1, "a");
    FraCurveNode node2x5 = fraNode(2, "b");
    FraCurveNode node3x6 = fraNode(3, "c");

    InterpolatedCurveConfig curve = InterpolatedCurveConfig.builder()
        .name(CurveName.of("curve"))
        .interpolator(CurveInterpolators.DOUBLE_QUADRATIC)
        .leftExtrapolator(CurveExtrapolators.FLAT)
        .rightExtrapolator(CurveExtrapolators.FLAT)
        .nodes(node1x4, node2x5, node3x6)
        .build();

    CurveGroupConfig groupConfig = CurveGroupConfig.builder()
        .name(CurveGroupName.of("curve group"))
        .addDiscountingCurve(curve, Currency.USD)
        .build();

    MarketDataConfig marketDataConfig = MarketDataConfig.builder()
        .add(groupConfig.getName(), groupConfig)
        .build();

    QuoteId idA = QuoteId.of(StandardId.of("test", "a"));
    QuoteId idB = QuoteId.of(StandardId.of("test", "b"));
    QuoteId idC = QuoteId.of(StandardId.of("test", "c"));

    BaseMarketData marketData = BaseMarketData.builder(VALUATION_DATE)
        .addValue(idA, 1d)
        .addValue(idB, 2d)
        .addValue(idC, 3d)
        .build();

    ParRatesMarketDataFunction marketDataFunction = new ParRatesMarketDataFunction();
    ParRatesId parRatesId = ParRatesId.of(groupConfig.getName(), curve.getName(), MarketDataFeed.NONE);
    Result<ParRates> result = marketDataFunction.build(parRatesId, marketData, marketDataConfig);

    assertThat(result).isSuccess();
    ParRates parRates = result.getValue();
    assertThat(parRates.getRates().get(idA)).isEqualTo(1d);
    assertThat(parRates.getRates().get(idB)).isEqualTo(2d);
    assertThat(parRates.getRates().get(idC)).isEqualTo(3d);

    List<CurveParameterMetadata> expectedMetadata = ImmutableList.of(
        node1x4.metadata(VALUATION_DATE),
        node2x5.metadata(VALUATION_DATE),
        node3x6.metadata(VALUATION_DATE));
    assertThat(parRates.getCurveMetadata().getParameters()).hasValue(expectedMetadata);
  }

  /**
   * Test that a failure is returned if there is no config for the curve group.
   */
  public void buildMissingGroupConfig() {
    ParRatesMarketDataFunction marketDataFunction = new ParRatesMarketDataFunction();
    ParRatesId parRatesId = ParRatesId.of(CurveGroupName.of("curve group"), CurveName.of("curve"), MarketDataFeed.NONE);
    BaseMarketData emptyData = BaseMarketData.empty(VALUATION_DATE);
    Result<ParRates> result = marketDataFunction.build(parRatesId, emptyData, MarketDataConfig.empty());
    assertThat(result).hasFailureMessageMatching("No configuration found for curve group .*");
  }

  /**
   * Test that a failure is returned if there is config for the curve group but it doesn't contain the named curve.
   */
  public void buildMissingCurveConfig() {
    ParRatesMarketDataFunction marketDataFunction = new ParRatesMarketDataFunction();
    ParRatesId parRatesId = ParRatesId.of(CurveGroupName.of("curve group"), CurveName.of("curve"), MarketDataFeed.NONE);
    CurveGroupConfig groupConfig = CurveGroupConfig.builder().name(CurveGroupName.of("curve group")).build();
    MarketDataConfig marketDataConfig = MarketDataConfig.builder().add(groupConfig.getName(), groupConfig).build();
    BaseMarketData emptyData = BaseMarketData.empty(VALUATION_DATE);
    Result<ParRates> result = marketDataFunction.build(parRatesId, emptyData, marketDataConfig);
    assertThat(result).hasFailureMessageMatching("No curve named .*");
  }

  /**
   * Test that a failure is returned if the curve config isn't InterpolatedCurveConfig.
   */
  public void buildNotInterpolatedCurve() {
    ParRatesMarketDataFunction marketDataFunction = new ParRatesMarketDataFunction();
    ParRatesId parRatesId = ParRatesId.of(CurveGroupName.of("curve group"), CurveName.of("curve"), MarketDataFeed.NONE);
    CurveConfig curve = mock(CurveConfig.class);
    when(curve.getName()).thenReturn(CurveName.of("curve"));
    CurveGroupConfig groupConfig = CurveGroupConfig.builder()
        .name(CurveGroupName.of("curve group"))
        .addDiscountingCurve(curve, Currency.USD)
        .build();
    MarketDataConfig marketDataConfig = MarketDataConfig.builder().add(groupConfig.getName(), groupConfig).build();
    BaseMarketData emptyData = BaseMarketData.empty(VALUATION_DATE);
    Result<ParRates> result = marketDataFunction.build(parRatesId, emptyData, marketDataConfig);
    assertThat(result).hasFailureMessageMatching(".*for curve configuration of type.*");
  }

  /**
   * Test that a failure is returned if the observable data isn't available.
   */
  public void buildMissingMarketData() {
    FraCurveNode node1x4 = fraNode(1, "a");
    FraCurveNode node2x5 = fraNode(2, "b");
    FraCurveNode node3x6 = fraNode(3, "c");

    InterpolatedCurveConfig curve = InterpolatedCurveConfig.builder()
        .name(CurveName.of("curve"))
        .interpolator(CurveInterpolators.DOUBLE_QUADRATIC)
        .leftExtrapolator(CurveExtrapolators.FLAT)
        .rightExtrapolator(CurveExtrapolators.FLAT)
        .nodes(node1x4, node2x5, node3x6)
        .build();

    CurveGroupConfig groupConfig = CurveGroupConfig.builder()
        .name(CurveGroupName.of("curve group"))
        .addDiscountingCurve(curve, Currency.USD)
        .build();

    MarketDataConfig marketDataConfig = MarketDataConfig.builder()
        .add(groupConfig.getName(), groupConfig)
        .build();

    BaseMarketData emptyData = BaseMarketData.empty(VALUATION_DATE);

    ParRatesMarketDataFunction marketDataFunction = new ParRatesMarketDataFunction();
    ParRatesId parRatesId = ParRatesId.of(groupConfig.getName(), curve.getName(), MarketDataFeed.NONE);
    Result<ParRates> result = marketDataFunction.build(parRatesId, emptyData, marketDataConfig);
    assertThat(result).hasFailureMessageMatching("No market data available for .*");
  }

  //--------------------------------------------------------------------------------------------------------

  private static FraCurveNode fraNode(int startTenor, String marketDataId) {
    Period periodToStart = Period.ofMonths(startTenor);
    FraTemplate template = FraTemplate.of(periodToStart, IborIndices.USD_LIBOR_3M);
    return FraCurveNode.of(template, QuoteKey.of(StandardId.of("test", marketDataId)));
  }
}
