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
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
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
@Test
public class CurveSensitivitiesTest {

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
  private static final List<ParameterMetadata> METADATA1 = ParameterMetadata.listOfEmpty(4);
  private static final List<ParameterMetadata> METADATA2 = ParameterMetadata.listOfEmpty(5);

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
  public void test_empty() {
    CurveSensitivities test = CurveSensitivities.empty();
    assertEquals(test.getInfo(), PortfolioItemInfo.empty());
    assertEquals(test.getTypedSensitivities(), ImmutableMap.of());
  }

  public void test_of_single() {
    CurveSensitivities test = sut();
    assertEquals(test.getId(), Optional.empty());
    assertEquals(test.getInfo(), INFO1);
    assertEquals(test.getTypedSensitivities(), ImmutableMap.of(ZERO_RATE_DELTA, SENSI1));
    assertEquals(test.getTypedSensitivity(ZERO_RATE_DELTA), SENSI1);
  }

  public void test_of_map() {
    CurveSensitivities test = sut2();
    assertEquals(test.getId(), Optional.of(ID2));
    assertEquals(test.getInfo(), INFO2);
    assertEquals(test.getTypedSensitivities(), ImmutableMap.of(ZERO_RATE_DELTA, SENSI1, ZERO_RATE_GAMMA, SENSI2));
    assertEquals(test.getTypedSensitivity(ZERO_RATE_DELTA), SENSI1);
    assertEquals(test.getTypedSensitivity(ZERO_RATE_GAMMA), SENSI2);
  }

  //-------------------------------------------------------------------------
  public void test_builder_empty() {
    CurveSensitivities test = CurveSensitivities.builder(PortfolioItemInfo.empty()).build();
    assertEquals(test.getInfo(), PortfolioItemInfo.empty());
    assertEquals(test.getTypedSensitivities(), ImmutableMap.of());
  }

  public void test_builder_tenors() {
    CurveName curveName = CurveName.of("GBP");
    CurveSensitivities test = CurveSensitivities.builder(PortfolioItemInfo.empty())
        .add(ZERO_RATE_DELTA, curveName, Tenor.TENOR_1M, CurrencyAmount.of(GBP, 4))
        .add(ZERO_RATE_DELTA, curveName, Tenor.TENOR_1W, CurrencyAmount.of(GBP, 1))
        .add(ZERO_RATE_DELTA, curveName, Tenor.TENOR_1Y, CurrencyAmount.of(GBP, 2))
        .add(ZERO_RATE_DELTA, curveName, Tenor.TENOR_1W, CurrencyAmount.of(GBP, 2))
        .add(ZERO_RATE_DELTA, curveName, Tenor.TENOR_1Y, CurrencyAmount.of(GBP, 3))
        .build();
    assertEquals(test.getInfo(), PortfolioItemInfo.empty());
    assertEquals(test.getTypedSensitivities().size(), 1);
    CurrencyParameterSensitivities sens = test.getTypedSensitivity(ZERO_RATE_DELTA);
    assertEquals(sens.getSensitivities().size(), 1);
    CurrencyParameterSensitivity singleSens = sens.getSensitivity(curveName, GBP);
    assertEquals(singleSens.getSensitivity(), DoubleArray.of(3, 4, 5));
    assertEquals(singleSens.getParameterMetadata(0), TenorParameterMetadata.of(Tenor.TENOR_1W));
    assertEquals(singleSens.getParameterMetadata(1), TenorParameterMetadata.of(Tenor.TENOR_1M));
    assertEquals(singleSens.getParameterMetadata(2), TenorParameterMetadata.of(Tenor.TENOR_1Y));
  }

  public void test_builder_mixCurrency() {
    CurveName curveName = CurveName.of("GBP");
    CurveSensitivitiesBuilder builder = CurveSensitivities.builder(PortfolioItemInfo.empty());
    builder.add(ZERO_RATE_DELTA, curveName, Tenor.TENOR_1Y, CurrencyAmount.of(GBP, 1));
    assertThrows(
        IllegalArgumentException.class,
        () -> builder.add(ZERO_RATE_DELTA, curveName, Tenor.TENOR_1Y, CurrencyAmount.of(USD, 1)));
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith_map_empty() {
    CurveSensitivities base = sut();
    Map<CurveSensitivitiesType, CurrencyParameterSensitivities> additional = ImmutableMap.of();
    CurveSensitivities test = base.combinedWith(additional);
    assertEquals(test, base);
  }

  public void test_combinedWith_map_mergeAndAdd() {
    CurveSensitivities base1 = sut();
    CurveSensitivities base2 = sut2();
    CurveSensitivities test = base1.combinedWith(base2.getTypedSensitivities());
    assertEquals(test.getInfo(), base1.getInfo());
    assertEquals(test.getTypedSensitivities().keySet(), ImmutableSet.of(ZERO_RATE_DELTA, ZERO_RATE_GAMMA));
    assertEquals(test.getTypedSensitivities().get(ZERO_RATE_DELTA), SENSI1.multipliedBy(2));
    assertEquals(test.getTypedSensitivities().get(ZERO_RATE_GAMMA), SENSI2);
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith_sens_empty() {
    CurveSensitivities base = sut();
    CurveSensitivities test = base.combinedWith(CurveSensitivities.empty());
    assertEquals(test, base);
  }

  public void test_combinedWith_sens_mergeAndAdd() {
    CurveSensitivities base1 = sut();
    CurveSensitivities base2 = sut2();
    CurveSensitivities test = base1.combinedWith(base2);
    assertEquals(test.getInfo(), PortfolioItemInfo.empty()
        .withId(ID2)
        .withAttribute(NAME, "2")
        .withAttribute(DESCRIPTION, "1"));
    assertEquals(test.getTypedSensitivities().keySet(), ImmutableSet.of(ZERO_RATE_DELTA, ZERO_RATE_GAMMA));
    assertEquals(test.getTypedSensitivities().get(ZERO_RATE_DELTA), SENSI1.multipliedBy(2));
    assertEquals(test.getTypedSensitivities().get(ZERO_RATE_GAMMA), SENSI2);
  }

  //-------------------------------------------------------------------------
  public void test_convertedTo_singleCurrency() {
    CurveSensitivities base = sut();
    CurveSensitivities test = base.convertedTo(USD, FxMatrix.empty());
    assertEquals(test.getTypedSensitivities().get(ZERO_RATE_DELTA).getSensitivities(), ImmutableList.of(ENTRY_USD));
  }

  public void test_convertedTo_multipleCurrency() {
    CurveSensitivities base = sut2();
    CurveSensitivities test = base.convertedTo(USD, FX_RATE);
    assertEquals(test.getTypedSensitivities().get(ZERO_RATE_DELTA).getSensitivities(), ImmutableList.of(ENTRY_USD));
    assertEquals(
        test.getTypedSensitivities().get(ZERO_RATE_GAMMA).getSensitivities(),
        ImmutableList.of(ENTRY_USD2, ENTRY_EUR_IN_USD));
  }

  //-------------------------------------------------------------------------
  public void test_summarize() {
    CurveSensitivities base = sut2();
    PortfolioItemSummary test = base.summarize();
    assertEquals(test.getId(), Optional.of(ID2));
    assertEquals(test.getPortfolioItemType(), PortfolioItemType.SENSITIVITIES);
    assertEquals(test.getProductType(), ProductType.SENSITIVITIES);
    assertEquals(test.getCurrencies(), ImmutableSet.of(EUR, USD));
    assertEquals(test.getDescription(), "CurveSensitivities[ZeroRateDelta, ZeroRateGamma]");
  }

  //-------------------------------------------------------------------------
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
