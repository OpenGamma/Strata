/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.market.sensitivity.CurveSensitivitiesType.ZERO_RATE_DELTA;
import static com.opengamma.strata.market.sensitivity.CurveSensitivitiesType.ZERO_RATE_GAMMA;
import static com.opengamma.strata.product.AttributeType.DESCRIPTION;
import static com.opengamma.strata.product.AttributeType.NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.TenorParameterMetadata;
import com.opengamma.strata.product.PortfolioItemInfo;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.ProductType;

/**
 * Test {@link CurveSensitivities}.
 */
public class CurveSensitivitiesTest {

  private static final TenorParameterMetadata TENOR_MD_1M = TenorParameterMetadata.of(Tenor.TENOR_1M);
  private static final TenorParameterMetadata TENOR_MD_1W = TenorParameterMetadata.of(Tenor.TENOR_1W);
  private static final TenorParameterMetadata TENOR_MD_1Y = TenorParameterMetadata.of(Tenor.TENOR_1Y);
  private static final TenorParameterMetadata TENOR_MD_2Y = TenorParameterMetadata.of(Tenor.TENOR_2Y);
  private static final TenorParameterMetadata TENOR_MD_3Y = TenorParameterMetadata.of(Tenor.TENOR_3Y);
  private static final TenorParameterMetadata TENOR_MD_4Y = TenorParameterMetadata.of(Tenor.TENOR_4Y);
  private static final TenorParameterMetadata TENOR_MD_5Y = TenorParameterMetadata.of(Tenor.TENOR_5Y);

  private static final DoubleArray VECTOR_USD1 = DoubleArray.of(100, 200, 300, 123);
  private static final DoubleArray VECTOR_USD2 = DoubleArray.of(1000, 250, 321, 123);
  private static final DoubleArray VECTOR_EUR1 = DoubleArray.of(1000, 250, 321, 123, 321);
  private static final DoubleArray VECTOR_EUR1_IN_USD =
      DoubleArray.of(1000 * 1.6, 250 * 1.6, 321 * 1.6, 123 * 1.6, 321 * 1.6);
  private static final Currency USD = Currency.USD;
  private static final Currency EUR = Currency.EUR;
  private static final FxRate FX_RATE = FxRate.of(EUR, USD, 1.6d);
  private static final MarketDataName<?> NAME1 = CurveName.of("NAME-1");
  private static final MarketDataName<?> NAME2 = CurveName.of("NAME-2");
  private static final List<ParameterMetadata> METADATA1 =
      ImmutableList.of(TENOR_MD_1Y, TENOR_MD_2Y, TENOR_MD_3Y, TENOR_MD_4Y);
  private static final List<ParameterMetadata> METADATA2 =
      ImmutableList.of(TENOR_MD_1Y, TENOR_MD_2Y, TENOR_MD_3Y, TENOR_MD_4Y, TENOR_MD_5Y);

  private static final CurrencyParameterSensitivity ENTRY_USD =
      CurrencyParameterSensitivity.of(NAME1, METADATA1, USD, VECTOR_USD1);
  private static final CurrencyParameterSensitivity ENTRY_USD2 =
      CurrencyParameterSensitivity.of(NAME1, METADATA1, USD, VECTOR_USD2);
  private static final CurrencyParameterSensitivity ENTRY_EUR =
      CurrencyParameterSensitivity.of(NAME2, METADATA2, EUR, VECTOR_EUR1);
  private static final CurrencyParameterSensitivity ENTRY_EUR_IN_USD =
      CurrencyParameterSensitivity.of(NAME2, METADATA2, USD, VECTOR_EUR1_IN_USD);

  private static final StandardId ID2 = StandardId.of("A", "B");
  private static final CurrencyParameterSensitivities SENSI1 = CurrencyParameterSensitivities.of(ENTRY_USD);
  private static final CurrencyParameterSensitivities SENSI2 = CurrencyParameterSensitivities.of(ENTRY_USD2, ENTRY_EUR);
  private static final PortfolioItemInfo INFO1 = PortfolioItemInfo.empty().withAttribute(DESCRIPTION, "1");
  private static final PortfolioItemInfo INFO2 = PortfolioItemInfo.empty()
      .withId(ID2)
      .withAttribute(NAME, "2")
      .withAttribute(DESCRIPTION, "2");

  //-------------------------------------------------------------------------
  @Test
  public void test_empty() {
    CurveSensitivities test = CurveSensitivities.empty();
    assertThat(test.getInfo()).isEqualTo(PortfolioItemInfo.empty());
    assertThat(test.getTypedSensitivities()).isEmpty();
  }

  @Test
  public void test_of_single() {
    CurveSensitivities test = sut();
    assertThat(test.getId()).isEqualTo(Optional.empty());
    assertThat(test.getInfo()).isEqualTo(INFO1);
    assertThat(test.getTypedSensitivities()).isEqualTo(ImmutableMap.of(ZERO_RATE_DELTA, SENSI1));
    assertThat(test.getTypedSensitivity(ZERO_RATE_DELTA)).isEqualTo(SENSI1);
    assertThatIllegalArgumentException().isThrownBy(() -> test.getTypedSensitivity(ZERO_RATE_GAMMA));
    assertThat(test.findTypedSensitivity(ZERO_RATE_DELTA)).isEqualTo(Optional.of(SENSI1));
    assertThat(test.findTypedSensitivity(ZERO_RATE_GAMMA)).isEqualTo(Optional.empty());
  }

  @Test
  public void test_of_map() {
    CurveSensitivities test = sut2();
    assertThat(test.getId()).isEqualTo(Optional.of(ID2));
    assertThat(test.getInfo()).isEqualTo(INFO2);
    assertThat(test.getTypedSensitivities()).isEqualTo(ImmutableMap.of(ZERO_RATE_DELTA, SENSI1, ZERO_RATE_GAMMA, SENSI2));
    assertThat(test.getTypedSensitivity(ZERO_RATE_DELTA)).isEqualTo(SENSI1);
    assertThat(test.getTypedSensitivity(ZERO_RATE_GAMMA)).isEqualTo(SENSI2);
  }

  @Test
  public void test_withInfo() {
    CurveSensitivities base = sut();
    assertThat(base.getInfo()).isEqualTo(INFO1);
    CurveSensitivities test = base.withInfo(INFO2);
    assertThat(test.getInfo()).isEqualTo(INFO2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toMergedSensitivities() {
    CurveName curveName = CurveName.of("WEIRD");
    CurveSensitivities base = CurveSensitivities.builder(PortfolioItemInfo.empty())
        .add(ZERO_RATE_DELTA, curveName, GBP, TENOR_MD_1M, 4)
        .add(ZERO_RATE_DELTA, curveName, GBP, TENOR_MD_1W, 1)
        .add(ZERO_RATE_DELTA, curveName, GBP, TENOR_MD_1Y, 2)
        .build();
    CurveSensitivities other = CurveSensitivities.builder(PortfolioItemInfo.empty())
        .add(ZERO_RATE_DELTA, curveName, GBP, TENOR_MD_1W, 2)
        .build();
    CurveSensitivities test = Stream.of(base, other).collect(CurveSensitivities.toMergedSensitivities());
    assertThat(test.getInfo()).isEqualTo(PortfolioItemInfo.empty());
    assertThat(test.getTypedSensitivities()).hasSize(1);
    CurrencyParameterSensitivities sens = test.getTypedSensitivity(ZERO_RATE_DELTA);
    assertThat(sens.getSensitivities()).hasSize(1);
    CurrencyParameterSensitivity singleSens = sens.getSensitivity(curveName, GBP);
    assertThat(singleSens.getSensitivity()).isEqualTo(DoubleArray.of(3, 4, 2));
    assertThat(singleSens.getParameterMetadata(0)).isEqualTo(TENOR_MD_1W);
    assertThat(singleSens.getParameterMetadata(1)).isEqualTo(TENOR_MD_1M);
    assertThat(singleSens.getParameterMetadata(2)).isEqualTo(TENOR_MD_1Y);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_builder_empty() {
    CurveSensitivities test = CurveSensitivities.builder(PortfolioItemInfo.empty()).build();
    assertThat(test.getInfo()).isEqualTo(PortfolioItemInfo.empty());
    assertThat(test.getTypedSensitivities()).isEmpty();
  }

  @Test
  public void test_builder_tenors() {
    CurveName curveName = CurveName.of("GBP");
    CurrencyParameterSensitivity sens1Y = CurrencyParameterSensitivity.of(
        curveName, ImmutableList.of(TENOR_MD_1Y), GBP, DoubleArray.of(3));
    CurveSensitivities test = CurveSensitivities.builder(PortfolioItemInfo.empty())
        .add(ZERO_RATE_DELTA, curveName, GBP, TENOR_MD_1M, 4)
        .add(ZERO_RATE_DELTA, curveName, GBP, TENOR_MD_1W, 1)
        .add(ZERO_RATE_DELTA, curveName, GBP, TENOR_MD_1Y, 2)
        .add(ZERO_RATE_DELTA, curveName, GBP, TENOR_MD_1W, 2)
        .add(ZERO_RATE_DELTA, sens1Y)
        .build();
    assertThat(test.getInfo()).isEqualTo(PortfolioItemInfo.empty());
    assertThat(test.getTypedSensitivities()).hasSize(1);
    CurrencyParameterSensitivities sens = test.getTypedSensitivity(ZERO_RATE_DELTA);
    assertThat(sens.getSensitivities()).hasSize(1);
    CurrencyParameterSensitivity singleSens = sens.getSensitivity(curveName, GBP);
    assertThat(singleSens.getSensitivity()).isEqualTo(DoubleArray.of(3, 4, 5));
    assertThat(singleSens.getParameterMetadata(0)).isEqualTo(TENOR_MD_1W);
    assertThat(singleSens.getParameterMetadata(1)).isEqualTo(TENOR_MD_1M);
    assertThat(singleSens.getParameterMetadata(2)).isEqualTo(TENOR_MD_1Y);
  }

  @Test
  public void test_builder_mixCurrency() {
    CurveName curveName = CurveName.of("WEIRD");
    CurveSensitivities test = CurveSensitivities.builder(PortfolioItemInfo.empty())
        .add(ZERO_RATE_DELTA, curveName, GBP, TENOR_MD_1Y, 1)
        .add(ZERO_RATE_DELTA, curveName, USD, TENOR_MD_1Y, 2)
        .build();
    assertThat(test.getInfo()).isEqualTo(PortfolioItemInfo.empty());
    assertThat(test.getTypedSensitivities()).hasSize(1);
    CurrencyParameterSensitivities sens = test.getTypedSensitivity(ZERO_RATE_DELTA);
    assertThat(sens.getSensitivities()).hasSize(2);
    CurrencyParameterSensitivity sensGbp = sens.getSensitivity(curveName, GBP);
    assertThat(sensGbp.getSensitivity()).isEqualTo(DoubleArray.of(1));
    assertThat(sensGbp.getParameterMetadata(0)).isEqualTo(TENOR_MD_1Y);
    CurrencyParameterSensitivity sensUsd = sens.getSensitivity(curveName, USD);
    assertThat(sensUsd.getSensitivity()).isEqualTo(DoubleArray.of(2));
    assertThat(sensUsd.getParameterMetadata(0)).isEqualTo(TENOR_MD_1Y);
  }

  @Test
  public void test_builder_curveSensitivities() {
    CurveName curveName = CurveName.of("WEIRD");
    CurveSensitivities base = CurveSensitivities.builder(PortfolioItemInfo.empty())
        .add(ZERO_RATE_DELTA, curveName, GBP, TENOR_MD_1M, 4)
        .add(ZERO_RATE_DELTA, curveName, GBP, TENOR_MD_1W, 1)
        .add(ZERO_RATE_DELTA, curveName, GBP, TENOR_MD_1Y, 2)
        .build();
    CurveSensitivities other = CurveSensitivities.builder(PortfolioItemInfo.empty())
        .add(ZERO_RATE_DELTA, curveName, GBP, TENOR_MD_1W, 2)
        .build();
    CurveSensitivities test = CurveSensitivities.builder(PortfolioItemInfo.empty())
        .add(base)
        .add(other)
        .build();
    assertThat(test.getInfo()).isEqualTo(PortfolioItemInfo.empty());
    assertThat(test.getTypedSensitivities()).hasSize(1);
    CurrencyParameterSensitivities sens = test.getTypedSensitivity(ZERO_RATE_DELTA);
    assertThat(sens.getSensitivities()).hasSize(1);
    CurrencyParameterSensitivity singleSens = sens.getSensitivity(curveName, GBP);
    assertThat(singleSens.getSensitivity()).isEqualTo(DoubleArray.of(3, 4, 2));
    assertThat(singleSens.getParameterMetadata(0)).isEqualTo(TENOR_MD_1W);
    assertThat(singleSens.getParameterMetadata(1)).isEqualTo(TENOR_MD_1M);
    assertThat(singleSens.getParameterMetadata(2)).isEqualTo(TENOR_MD_1Y);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_mergedWith_map_empty() {
    CurveSensitivities base = sut();
    Map<CurveSensitivitiesType, CurrencyParameterSensitivities> additional = ImmutableMap.of();
    CurveSensitivities test = base.mergedWith(additional);
    assertThat(test).isEqualTo(base);
  }

  @Test
  public void test_mergedWith_map_mergeAndAdd() {
    CurveSensitivities base1 = sut();
    CurveSensitivities base2 = sut2();
    CurveSensitivities test = base1.mergedWith(base2.getTypedSensitivities());
    assertThat(test.getInfo()).isEqualTo(base1.getInfo());
    assertThat(test.getTypedSensitivities().keySet()).containsOnly(ZERO_RATE_DELTA, ZERO_RATE_GAMMA);
    assertThat(test.getTypedSensitivities().get(ZERO_RATE_DELTA)).isEqualTo(SENSI1.multipliedBy(2));
    assertThat(test.getTypedSensitivities().get(ZERO_RATE_GAMMA)).isEqualTo(SENSI2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_mergedWith_sens_empty() {
    CurveSensitivities base = sut();
    CurveSensitivities test = base.mergedWith(CurveSensitivities.empty());
    assertThat(test).isEqualTo(base);
  }

  @Test
  public void test_mergedWith_sens_mergeAndAdd() {
    CurveSensitivities base1 = sut();
    CurveSensitivities base2 = sut2();
    CurveSensitivities test = base1.mergedWith(base2);
    assertThat(test.getInfo()).isEqualTo(PortfolioItemInfo.empty()
        .withId(ID2)
        .withAttribute(NAME, "2")
        .withAttribute(DESCRIPTION, "1"));
    assertThat(test.getTypedSensitivities().keySet()).containsOnly(ZERO_RATE_DELTA, ZERO_RATE_GAMMA);
    assertThat(test.getTypedSensitivities().get(ZERO_RATE_DELTA)).isEqualTo(SENSI1.multipliedBy(2));
    assertThat(test.getTypedSensitivities().get(ZERO_RATE_GAMMA)).isEqualTo(SENSI2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withMarketDataNames() {
    CurveSensitivities base = sut();
    CurveSensitivities test = base.withMarketDataNames(name -> NAME2);
    assertThat(base.getTypedSensitivities().get(ZERO_RATE_DELTA).getSensitivities().get(0).getMarketDataName()).isEqualTo(NAME1);
    assertThat(test.getTypedSensitivities().get(ZERO_RATE_DELTA).getSensitivities().get(0).getMarketDataName()).isEqualTo(NAME2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withParameterMetadatas() {
    CurveSensitivities base = sut();
    CurveSensitivities test = base.withParameterMetadatas(md -> TENOR_MD_1Y);
    CurrencyParameterSensitivity testSens = test.getTypedSensitivities().get(ZERO_RATE_DELTA).getSensitivities().get(0);
    assertThat(testSens.getParameterMetadata()).containsExactly(TENOR_MD_1Y);
    assertThat(testSens.getSensitivity()).isEqualTo(DoubleArray.of(723));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_convertedTo_singleCurrency() {
    CurveSensitivities base = sut();
    CurveSensitivities test = base.convertedTo(USD, FxMatrix.empty());
    assertThat(test.getTypedSensitivities().get(ZERO_RATE_DELTA).getSensitivities()).containsExactly(ENTRY_USD);
  }

  @Test
  public void test_convertedTo_multipleCurrency() {
    CurveSensitivities base = sut2();
    CurveSensitivities test = base.convertedTo(USD, FX_RATE);
    assertThat(test.getTypedSensitivities().get(ZERO_RATE_DELTA).getSensitivities())
        .containsExactly(ENTRY_USD);
    assertThat(test.getTypedSensitivities().get(ZERO_RATE_GAMMA).getSensitivities())
        .containsExactly(ENTRY_USD2, ENTRY_EUR_IN_USD);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_summarize() {
    CurveSensitivities base = sut2();
    PortfolioItemSummary test = base.summarize();
    assertThat(test.getId()).isEqualTo(Optional.of(ID2));
    assertThat(test.getPortfolioItemType()).isEqualTo(PortfolioItemType.SENSITIVITIES);
    assertThat(test.getProductType()).isEqualTo(ProductType.SENSITIVITIES);
    assertThat(test.getCurrencies()).containsOnly(EUR, USD);
    assertThat(test.getDescription()).isEqualTo("CurveSensitivities[ZeroRateDelta, ZeroRateGamma]");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  private CurveSensitivities sut() {
    return CurveSensitivities.of(INFO1, ZERO_RATE_DELTA, SENSI1);
  }

  private CurveSensitivities sut2() {
    return CurveSensitivities.of(INFO2, ImmutableMap.of(ZERO_RATE_DELTA, SENSI1, ZERO_RATE_GAMMA, SENSI2));
  }

}
