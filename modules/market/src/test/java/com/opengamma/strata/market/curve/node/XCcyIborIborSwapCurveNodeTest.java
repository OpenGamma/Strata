/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.node;

import static com.opengamma.strata.basics.date.Tenor.TENOR_10Y;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.Period;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.data.FxRateId;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.data.ObservableSource;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveNodeDate;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.DatedParameterMetadata;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.TenorDateParameterMetadata;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swap.type.XCcyIborIborSwapConventions;
import com.opengamma.strata.product.swap.type.XCcyIborIborSwapTemplate;

/**
 * Test {@link XCcyIborIborSwapCurveNode}.
 */
public class XCcyIborIborSwapCurveNodeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = date(2015, 6, 30);
  private static final XCcyIborIborSwapTemplate TEMPLATE =
      XCcyIborIborSwapTemplate.of(Period.ZERO, TENOR_10Y, XCcyIborIborSwapConventions.EUR_EURIBOR_3M_USD_LIBOR_3M);
  private static final XCcyIborIborSwapTemplate TEMPLATE2 =
      XCcyIborIborSwapTemplate.of(Period.ofMonths(1), TENOR_10Y, XCcyIborIborSwapConventions.EUR_EURIBOR_3M_USD_LIBOR_3M);
  private static final QuoteId SPREAD_ID = QuoteId.of(StandardId.of("OG-Ticker", "USD-EUR-XCS-10Y"));
  private static final QuoteId SPREAD_ID2 = QuoteId.of(StandardId.of("OG-Ticker", "Test"));
  private static final ObservableSource OBS_SOURCE = ObservableSource.of("Vendor");
  private static final FxRateId FX_RATE_ID = FxRateId.of(TEMPLATE.getCurrencyPair());
  private static final FxRateId FX_RATE_ID2 = FxRateId.of(TEMPLATE.getCurrencyPair(), OBS_SOURCE);
  private static final double SPREAD_XCS = 0.00125;
  private static final FxRate FX_EUR_USD = FxRate.of(Currency.EUR, Currency.USD, 1.25);
  private static final double SPREAD_ADJ = 0.0015;
  private static final String LABEL = "Label";
  private static final String LABEL_AUTO = "10Y";
  private static final MarketData MARKET_DATA = ImmutableMarketData.builder(VAL_DATE)
      .addValue(SPREAD_ID, SPREAD_XCS)
      .addValue(FX_RATE_ID, FX_EUR_USD)
      .build();

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    XCcyIborIborSwapCurveNode test = XCcyIborIborSwapCurveNode.builder()
        .label(LABEL)
        .template(TEMPLATE)
        .fxRateId(FX_RATE_ID2)
        .spreadId(SPREAD_ID)
        .additionalSpread(SPREAD_ADJ)
        .build();
    assertThat(test.getLabel()).isEqualTo(LABEL);
    assertThat(test.getFxRateId()).isEqualTo(FX_RATE_ID2);
    assertThat(test.getSpreadId()).isEqualTo(SPREAD_ID);
    assertThat(test.getAdditionalSpread()).isEqualTo(SPREAD_ADJ);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
    assertThat(test.getDate()).isEqualTo(CurveNodeDate.END);
  }

  @Test
  public void test_builder_defaults() {
    XCcyIborIborSwapCurveNode test = XCcyIborIborSwapCurveNode.builder()
        .template(TEMPLATE)
        .spreadId(SPREAD_ID)
        .build();
    assertThat(test.getLabel()).isEqualTo(LABEL_AUTO);
    assertThat(test.getFxRateId()).isEqualTo(FX_RATE_ID);
    assertThat(test.getSpreadId()).isEqualTo(SPREAD_ID);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
    assertThat(test.getDate()).isEqualTo(CurveNodeDate.END);
  }

  @Test
  public void test_builder_noTemplate() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> XCcyIborIborSwapCurveNode.builder().label(LABEL).spreadId(SPREAD_ID).build());
  }

  @Test
  public void test_of_noSpread() {
    XCcyIborIborSwapCurveNode test = XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_ID);
    assertThat(test.getLabel()).isEqualTo(LABEL_AUTO);
    assertThat(test.getSpreadId()).isEqualTo(SPREAD_ID);
    assertThat(test.getAdditionalSpread()).isEqualTo(0.0d);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
  }

  @Test
  public void test_of_withSpread() {
    XCcyIborIborSwapCurveNode test = XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_ID, SPREAD_ADJ);
    assertThat(test.getLabel()).isEqualTo(LABEL_AUTO);
    assertThat(test.getSpreadId()).isEqualTo(SPREAD_ID);
    assertThat(test.getAdditionalSpread()).isEqualTo(SPREAD_ADJ);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
  }

  @Test
  public void test_of_withSpreadAndLabel() {
    XCcyIborIborSwapCurveNode test = XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_ID, SPREAD_ADJ, LABEL);
    assertThat(test.getLabel()).isEqualTo(LABEL);
    assertThat(test.getSpreadId()).isEqualTo(SPREAD_ID);
    assertThat(test.getAdditionalSpread()).isEqualTo(SPREAD_ADJ);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
  }

  @Test
  public void test_requirements() {
    XCcyIborIborSwapCurveNode test = XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_ID, SPREAD_ADJ);
    Set<? extends MarketDataId<?>> setExpected = ImmutableSet.of(SPREAD_ID, FX_RATE_ID);
    Set<? extends MarketDataId<?>> set = test.requirements();
    assertThat(set.equals(setExpected)).isTrue();
  }

  @Test
  public void test_trade() {
    XCcyIborIborSwapCurveNode node = XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_ID, SPREAD_ADJ);
    double quantity = -1234.56;
    SwapTrade trade = node.trade(quantity, MARKET_DATA, REF_DATA);
    double rate = FX_EUR_USD.fxRate(Currency.EUR, Currency.USD);
    SwapTrade expected = TEMPLATE.createTrade(
        VAL_DATE, BUY, -quantity, -quantity * rate, SPREAD_XCS + SPREAD_ADJ, REF_DATA);
    assertThat(trade).isEqualTo(expected);
    assertThat(node.resolvedTrade(quantity, MARKET_DATA, REF_DATA)).isEqualTo(trade.resolve(REF_DATA));
  }

  @Test
  public void test_trade_noMarketData() {
    XCcyIborIborSwapCurveNode node = XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_ID, SPREAD_ADJ);
    MarketData marketData = MarketData.empty(VAL_DATE);
    assertThatExceptionOfType(MarketDataNotFoundException.class)
        .isThrownBy(() -> node.trade(1d, marketData, REF_DATA));
  }

  @Test
  public void test_sampleResolvedTrade() {
    XCcyIborIborSwapCurveNode node = XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_ID, SPREAD_ADJ);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    ResolvedSwapTrade trade = node.sampleResolvedTrade(valuationDate, FX_EUR_USD, REF_DATA);
    ResolvedSwapTrade expected =
        TEMPLATE.createTrade(valuationDate, BuySell.SELL, 1d, 1.25d, SPREAD_ADJ, REF_DATA).resolve(REF_DATA);
    assertThat(trade).isEqualTo(expected);
  }

  @Test
  public void test_initialGuess() {
    XCcyIborIborSwapCurveNode node = XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_ID, SPREAD_ADJ);
    assertThat(node.initialGuess(MARKET_DATA, ValueType.ZERO_RATE)).isEqualTo(0d);
    assertThat(node.initialGuess(MARKET_DATA, ValueType.DISCOUNT_FACTOR)).isEqualTo(1.0d);
  }

  @Test
  public void test_metadata_end() {
    XCcyIborIborSwapCurveNode node = XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_ID, SPREAD_ADJ);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    ParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    // 2015-01-22 is Thursday, start is 2015-01-26, but 2025-01-26 is Sunday, so end is 2025-01-27
    assertThat(((TenorDateParameterMetadata) metadata).getDate()).isEqualTo(LocalDate.of(2025, 1, 27));
    assertThat(((TenorDateParameterMetadata) metadata).getTenor()).isEqualTo(Tenor.TENOR_10Y);
  }

  @Test
  public void test_metadata_fixed() {
    LocalDate nodeDate = VAL_DATE.plusMonths(1);
    XCcyIborIborSwapCurveNode node =
        XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_ID, SPREAD_ADJ, LABEL).withDate(CurveNodeDate.of(nodeDate));
    DatedParameterMetadata metadata = node.metadata(VAL_DATE, REF_DATA);
    assertThat(metadata.getDate()).isEqualTo(nodeDate);
    assertThat(metadata.getLabel()).isEqualTo(node.getLabel());
  }

  @Test
  public void test_metadata_last_fixing() {
    XCcyIborIborSwapCurveNode node =
        XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_ID, SPREAD_ADJ, LABEL).withDate(CurveNodeDate.LAST_FIXING);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    DatedParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    LocalDate fixingExpected = LocalDate.of(2024, 10, 24);
    assertThat(metadata.getDate()).isEqualTo(fixingExpected);
    assertThat(((TenorDateParameterMetadata) metadata).getTenor()).isEqualTo(TENOR_10Y);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    XCcyIborIborSwapCurveNode test = XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_ID, SPREAD_ADJ);
    coverImmutableBean(test);
    XCcyIborIborSwapCurveNode test2 = XCcyIborIborSwapCurveNode.builder()
        .label(LABEL)
        .template(TEMPLATE2)
        .fxRateId(FX_RATE_ID2)
        .spreadId(SPREAD_ID2)
        .additionalSpread(0.1)
        .date(CurveNodeDate.LAST_FIXING)
        .build();
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    XCcyIborIborSwapCurveNode test = XCcyIborIborSwapCurveNode.of(TEMPLATE, SPREAD_ID, SPREAD_ADJ);
    assertSerialization(test);
  }

}
