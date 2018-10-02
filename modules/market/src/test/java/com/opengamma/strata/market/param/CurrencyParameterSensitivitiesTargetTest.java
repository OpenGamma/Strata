/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.market.param.CurrencyParameterSensitivityType.ZERO_RATE_DELTA;
import static com.opengamma.strata.market.param.CurrencyParameterSensitivityType.ZERO_RATE_GAMMA;
import static org.testng.Assert.assertEquals;

import java.util.List;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.product.AttributeType;
import com.opengamma.strata.product.PortfolioItemInfo;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.ProductType;

/**
 * Test {@link CurrencyParameterSensitivitiesTarget}.
 */
@Test
public class CurrencyParameterSensitivitiesTargetTest {

  private static final DoubleArray VECTOR_USD1 = DoubleArray.of(100, 200, 300, 123);
  private static final DoubleArray VECTOR_USD2 = DoubleArray.of(1000, 250, 321, 123);
  private static final DoubleArray VECTOR_EUR1 = DoubleArray.of(1000, 250, 321, 123, 321);
  private static final DoubleArray VECTOR_EUR1_IN_USD = DoubleArray.of(1000 * 1.6, 250 * 1.6, 321 * 1.6, 123 * 1.6, 321 * 1.6);
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

  private static final StandardId ID = StandardId.of("A", "B");
  private static final CurrencyParameterSensitivities SENSI1 = CurrencyParameterSensitivities.of(ENTRY_USD);
  private static final CurrencyParameterSensitivities SENSI2 =
      CurrencyParameterSensitivities.of(ImmutableList.of(ENTRY_USD2, ENTRY_EUR));
  private static final PortfolioItemInfo INFO1 =
      PortfolioItemInfo.empty().withAttribute(AttributeType.DESCRIPTION, "1");
  private static final PortfolioItemInfo INFO2 =
      PortfolioItemInfo.empty().withId(ID).withAttribute(AttributeType.NAME, "2");

  //-------------------------------------------------------------------------
  public void test_of_single() {
    CurrencyParameterSensitivitiesTarget test = sut();
    assertEquals(test.getId(), Optional.empty());
    assertEquals(test.getInfo(), INFO1);
    assertEquals(test.getTypedSensitivities(), ImmutableMap.of(ZERO_RATE_DELTA, SENSI1));
  }

  public void test_of_map() {
    CurrencyParameterSensitivitiesTarget test = sut2();
    assertEquals(test.getId(), Optional.of(ID));
    assertEquals(test.getInfo(), INFO2);
    assertEquals(test.getTypedSensitivities(), ImmutableMap.of(ZERO_RATE_DELTA, SENSI1, ZERO_RATE_GAMMA, SENSI2));
  }

  //-------------------------------------------------------------------------
  public void test_convertedTo_singleCurrency() {
    CurrencyParameterSensitivitiesTarget base = sut();
    CurrencyParameterSensitivitiesTarget test = base.convertedTo(USD, FxMatrix.empty());
    assertEquals(test.getTypedSensitivities().get(ZERO_RATE_DELTA).getSensitivities(), ImmutableList.of(ENTRY_USD));
  }

  public void test_convertedTo_multipleCurrency() {
    CurrencyParameterSensitivitiesTarget base = sut2();
    CurrencyParameterSensitivitiesTarget test = base.convertedTo(USD, FX_RATE);
    assertEquals(test.getTypedSensitivities().get(ZERO_RATE_DELTA).getSensitivities(), ImmutableList.of(ENTRY_USD));
    assertEquals(
        test.getTypedSensitivities().get(ZERO_RATE_GAMMA).getSensitivities(),
        ImmutableList.of(ENTRY_USD2, ENTRY_EUR_IN_USD));
  }

  //-------------------------------------------------------------------------
  public void test_summarize() {
    CurrencyParameterSensitivitiesTarget base = sut2();
    PortfolioItemSummary test = base.summarize();
    assertEquals(test.getId(), Optional.of(ID));
    assertEquals(test.getPortfolioItemType(), PortfolioItemType.SENSITIVITIES);
    assertEquals(test.getProductType(), ProductType.SENSITIVITIES);
    assertEquals(test.getCurrencies(), ImmutableSet.of(EUR, USD));
    assertEquals(test.getDescription(), "CurrencyParameterSensitivities[ZeroRateDelta, ZeroRateGamma]");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  private CurrencyParameterSensitivitiesTarget sut() {
    return CurrencyParameterSensitivitiesTarget.of(INFO1, ZERO_RATE_DELTA, SENSI1);
  }

  private CurrencyParameterSensitivitiesTarget sut2() {
    return CurrencyParameterSensitivitiesTarget.of(
        INFO2,
        ImmutableMap.of(ZERO_RATE_DELTA, SENSI1, ZERO_RATE_GAMMA, SENSI2));
  }

}
