/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.rate;

import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.calc.marketdata.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.data.ObservableSource;
import com.opengamma.strata.data.scenario.ImmutableScenarioMarketData;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.InterpolatedNodalCurveDefinition;
import com.opengamma.strata.market.curve.RatesCurveGroupDefinition;
import com.opengamma.strata.market.curve.RatesCurveInputs;
import com.opengamma.strata.market.curve.RatesCurveInputsId;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.curve.node.FraCurveNode;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.product.fra.type.FraTemplate;

/**
 * Test {@link RatesCurveInputsMarketDataFunction}.
 */
@Test
public class RatesCurveInputsMarketDataFunctionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = date(2011, 3, 8);

  /**
   * Test that the curve node requirements are extracted and returned.
   */
  public void requirements() {
    FraCurveNode node1x4 = fraNode(1, "a");
    FraCurveNode node2x5 = fraNode(2, "b");
    FraCurveNode node3x6 = fraNode(3, "c");

    InterpolatedNodalCurveDefinition curve = InterpolatedNodalCurveDefinition.builder()
        .name(CurveName.of("curve"))
        .interpolator(CurveInterpolators.DOUBLE_QUADRATIC)
        .extrapolatorLeft(CurveExtrapolators.FLAT)
        .extrapolatorRight(CurveExtrapolators.FLAT)
        .nodes(node1x4, node2x5, node3x6)
        .build();

    RatesCurveGroupDefinition groupDefn = RatesCurveGroupDefinition.builder()
        .name(CurveGroupName.of("curve group"))
        .addDiscountCurve(curve, Currency.USD)
        .build();

    MarketDataConfig marketDataConfig = MarketDataConfig.builder()
        .add(groupDefn.getName(), groupDefn)
        .build();

    RatesCurveInputsMarketDataFunction marketDataFunction = new RatesCurveInputsMarketDataFunction();
    RatesCurveInputsId curveInputsId = RatesCurveInputsId.of(groupDefn.getName(), curve.getName(), ObservableSource.NONE);
    MarketDataRequirements requirements = marketDataFunction.requirements(curveInputsId, marketDataConfig);

    assertThat(requirements.getObservables())
        .contains(QuoteId.of(StandardId.of("test", "a")))
        .contains(QuoteId.of(StandardId.of("test", "b")))
        .contains(QuoteId.of(StandardId.of("test", "c")));
  }

  /**
   * Test that an exception is thrown if there is no curve group configuration corresponding to the ID
   */
  public void requirementsMissingGroupConfig() {
    RatesCurveInputsMarketDataFunction marketDataFunction = new RatesCurveInputsMarketDataFunction();
    RatesCurveInputsId curveInputsId =
        RatesCurveInputsId.of(CurveGroupName.of("curve group"), CurveName.of("curve"), ObservableSource.NONE);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> marketDataFunction.requirements(curveInputsId, MarketDataConfig.empty()));
  }

  /**
   * Test that requirements are empty if the curve group config exists but not the curve
   */
  public void requirementsMissingCurveDefinition() {
    RatesCurveInputsMarketDataFunction marketDataFunction = new RatesCurveInputsMarketDataFunction();
    RatesCurveInputsId curveInputsId =
        RatesCurveInputsId.of(CurveGroupName.of("curve group"), CurveName.of("curve"), ObservableSource.NONE);
    RatesCurveGroupDefinition groupDefn = RatesCurveGroupDefinition.builder().name(CurveGroupName.of("curve group")).build();
    MarketDataConfig marketDataConfig = MarketDataConfig.builder().add(groupDefn.getName(), groupDefn).build();
    MarketDataRequirements requirements = marketDataFunction.requirements(curveInputsId, marketDataConfig);
    assertThat(requirements.getObservables()).isEmpty();
  }

  /**
   * Test that inputs are correctly built from market data.
   */
  public void build() {
    FraCurveNode node1x4 = fraNode(1, "a");
    FraCurveNode node2x5 = fraNode(2, "b");
    FraCurveNode node3x6 = fraNode(3, "c");

    InterpolatedNodalCurveDefinition curveDefn = InterpolatedNodalCurveDefinition.builder()
        .name(CurveName.of("curve"))
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(DayCounts.ACT_ACT_ISDA)
        .interpolator(CurveInterpolators.DOUBLE_QUADRATIC)
        .extrapolatorLeft(CurveExtrapolators.FLAT)
        .extrapolatorRight(CurveExtrapolators.FLAT)
        .nodes(node1x4, node2x5, node3x6)
        .build();

    RatesCurveGroupDefinition groupDefn = RatesCurveGroupDefinition.builder()
        .name(CurveGroupName.of("curve group"))
        .addDiscountCurve(curveDefn, Currency.USD)
        .build();

    MarketDataConfig marketDataConfig = MarketDataConfig.builder()
        .add(groupDefn.getName(), groupDefn)
        .build();

    QuoteId idA = QuoteId.of(StandardId.of("test", "a"));
    QuoteId idB = QuoteId.of(StandardId.of("test", "b"));
    QuoteId idC = QuoteId.of(StandardId.of("test", "c"));

    ScenarioMarketData marketData = ImmutableScenarioMarketData.builder(VAL_DATE)
        .addValue(idA, 1d)
        .addValue(idB, 2d)
        .addValue(idC, 3d)
        .build();

    RatesCurveInputsMarketDataFunction marketDataFunction = new RatesCurveInputsMarketDataFunction();
    RatesCurveInputsId curveInputsId = RatesCurveInputsId.of(groupDefn.getName(), curveDefn.getName(), ObservableSource.NONE);
    MarketDataBox<RatesCurveInputs> result = marketDataFunction.build(curveInputsId, marketDataConfig, marketData, REF_DATA);

    RatesCurveInputs curveInputs = result.getSingleValue();
    assertThat(curveInputs.getMarketData().get(idA)).isEqualTo(1d);
    assertThat(curveInputs.getMarketData().get(idB)).isEqualTo(2d);
    assertThat(curveInputs.getMarketData().get(idC)).isEqualTo(3d);

    List<ParameterMetadata> expectedMetadata = ImmutableList.of(
        node1x4.metadata(VAL_DATE, REF_DATA),
        node2x5.metadata(VAL_DATE, REF_DATA),
        node3x6.metadata(VAL_DATE, REF_DATA));
    assertThat(curveInputs.getCurveMetadata().getParameterMetadata()).hasValue(expectedMetadata);
  }

  /**
   * Test that a failure is returned if there is no config for the curve group.
   */
  public void buildMissingGroupConfig() {
    RatesCurveInputsMarketDataFunction marketDataFunction = new RatesCurveInputsMarketDataFunction();
    RatesCurveInputsId curveInputsId =
        RatesCurveInputsId.of(CurveGroupName.of("curve group"), CurveName.of("curve"), ObservableSource.NONE);
    ScenarioMarketData emptyData = ScenarioMarketData.empty();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> marketDataFunction.build(curveInputsId, MarketDataConfig.empty(), emptyData, REF_DATA))
        .withMessageStartingWith("No configuration found for type ");
  }

  /**
   * Test that a failure is returned if there is config for the curve group but it doesn't contain the named curve.
   */
  public void buildMissingCurveDefinition() {
    RatesCurveInputsMarketDataFunction marketDataFunction = new RatesCurveInputsMarketDataFunction();
    RatesCurveInputsId curveInputsId =
        RatesCurveInputsId.of(CurveGroupName.of("curve group"), CurveName.of("curve"), ObservableSource.NONE);
    RatesCurveGroupDefinition groupDefn = RatesCurveGroupDefinition.builder().name(CurveGroupName.of("curve group")).build();
    MarketDataConfig marketDataConfig = MarketDataConfig.builder().add(groupDefn.getName(), groupDefn).build();
    ScenarioMarketData emptyData = ScenarioMarketData.empty();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> marketDataFunction.build(curveInputsId, marketDataConfig, emptyData, REF_DATA))
        .withMessageStartingWith("No curve named ");
  }

  /**
   * Test that a failure is returned if the observable data isn't available.
   */
  public void buildMissingMarketData() {
    FraCurveNode node1x4 = fraNode(1, "a");
    FraCurveNode node2x5 = fraNode(2, "b");
    FraCurveNode node3x6 = fraNode(3, "c");

    InterpolatedNodalCurveDefinition curve = InterpolatedNodalCurveDefinition.builder()
        .name(CurveName.of("curve"))
        .interpolator(CurveInterpolators.DOUBLE_QUADRATIC)
        .extrapolatorLeft(CurveExtrapolators.FLAT)
        .extrapolatorRight(CurveExtrapolators.FLAT)
        .nodes(node1x4, node2x5, node3x6)
        .build();

    RatesCurveGroupDefinition groupDefn = RatesCurveGroupDefinition.builder()
        .name(CurveGroupName.of("curve group"))
        .addDiscountCurve(curve, Currency.USD)
        .build();

    MarketDataConfig marketDataConfig = MarketDataConfig.builder()
        .add(groupDefn.getName(), groupDefn)
        .build();

    ScenarioMarketData emptyData = ScenarioMarketData.of(1, date(2016, 6, 30), ImmutableMap.of(), ImmutableMap.of());

    RatesCurveInputsMarketDataFunction marketDataFunction = new RatesCurveInputsMarketDataFunction();
    RatesCurveInputsId curveInputsId = RatesCurveInputsId.of(groupDefn.getName(), curve.getName(), ObservableSource.NONE);

    assertThatExceptionOfType(MarketDataNotFoundException.class)
        .isThrownBy(() -> marketDataFunction.build(curveInputsId, marketDataConfig, emptyData, REF_DATA));
  }

  //-------------------------------------------------------------------------
  private static FraCurveNode fraNode(int startTenor, String marketDataId) {
    Period periodToStart = Period.ofMonths(startTenor);
    FraTemplate template = FraTemplate.of(periodToStart, IborIndices.USD_LIBOR_3M);
    return FraCurveNode.of(template, QuoteId.of(StandardId.of("test", marketDataId)));
  }

}
