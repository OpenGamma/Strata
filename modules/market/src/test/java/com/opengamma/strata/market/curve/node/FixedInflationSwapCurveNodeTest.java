/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.PriceIndices;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveNodeDate;
import com.opengamma.strata.market.observable.IndexQuoteId;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.DatedParameterMetadata;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.TenorDateParameterMetadata;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swap.type.FixedInflationSwapConventions;
import com.opengamma.strata.product.swap.type.FixedInflationSwapTemplate;

/**
 * Test {@link FixedInflationSwapCurveNode}.
 */
public class FixedInflationSwapCurveNodeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = date(2015, 6, 30);

  private static final FixedInflationSwapTemplate TEMPLATE =
      FixedInflationSwapTemplate.of(TENOR_10Y, FixedInflationSwapConventions.EUR_FIXED_ZC_EU_EXT_CPI);
  private static final QuoteId QUOTE_ID = QuoteId.of(StandardId.of("OG-Ticker", "EU-EXT-CPI"));
  private static final double SPREAD = 0.0015;
  private static final String LABEL = "Label";
  private static final String LABEL_AUTO = "10Y";

  private static final double TOLERANCE_GUESS = 1.0E-10;

  @Test
  public void test_builder() {
    FixedInflationSwapCurveNode test = FixedInflationSwapCurveNode.builder()
        .label(LABEL)
        .template(TEMPLATE)
        .rateId(QUOTE_ID)
        .additionalSpread(SPREAD)
        .build();
    assertThat(test.getLabel()).isEqualTo(LABEL);
    assertThat(test.getRateId()).isEqualTo(QUOTE_ID);
    assertThat(test.getAdditionalSpread()).isEqualTo(SPREAD);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
    assertThat(test.getDate()).isEqualTo(CurveNodeDate.END);
  }

  @Test
  public void test_of_noSpread() {
    FixedInflationSwapCurveNode test = FixedInflationSwapCurveNode.of(TEMPLATE, QUOTE_ID);
    assertThat(test.getLabel()).isEqualTo(LABEL_AUTO);
    assertThat(test.getRateId()).isEqualTo(QUOTE_ID);
    assertThat(test.getAdditionalSpread()).isEqualTo(0.0d);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
  }

  @Test
  public void test_of_withSpread() {
    FixedInflationSwapCurveNode test = FixedInflationSwapCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    assertThat(test.getLabel()).isEqualTo(LABEL_AUTO);
    assertThat(test.getRateId()).isEqualTo(QUOTE_ID);
    assertThat(test.getAdditionalSpread()).isEqualTo(SPREAD);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
  }

  @Test
  public void test_of_withSpreadAndLabel() {
    FixedInflationSwapCurveNode test = FixedInflationSwapCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD, LABEL);
    assertThat(test.getLabel()).isEqualTo(LABEL);
    assertThat(test.getRateId()).isEqualTo(QUOTE_ID);
    assertThat(test.getAdditionalSpread()).isEqualTo(SPREAD);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
  }

  @Test
  public void test_requirements() {
    FixedInflationSwapCurveNode test = FixedInflationSwapCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    Set<ObservableId> set = test.requirements();
    Iterator<ObservableId> itr = set.iterator();
    assertThat(itr.next()).isEqualTo(QUOTE_ID);
    assertThat(itr.hasNext()).isFalse();
  }

  @Test
  public void test_trade() {
    FixedInflationSwapCurveNode node = FixedInflationSwapCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    LocalDate tradeDate = LocalDate.of(2015, 1, 22);
    double rate = 0.125;
    double quantity = -1234.56;
    MarketData marketData = ImmutableMarketData.builder(tradeDate).addValue(QUOTE_ID, rate).build();
    SwapTrade trade = node.trade(quantity, marketData, REF_DATA);
    SwapTrade expected = TEMPLATE.createTrade(tradeDate, BUY, -quantity, rate + SPREAD, REF_DATA);
    assertThat(trade).isEqualTo(expected);
  }

  @Test
  public void test_trade_noMarketData() {
    FixedInflationSwapCurveNode node = FixedInflationSwapCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    MarketData marketData = MarketData.empty(valuationDate);
    assertThatExceptionOfType(MarketDataNotFoundException.class)
        .isThrownBy(() -> node.trade(1d, marketData, REF_DATA));
  }

  @Test
  public void test_sampleResolvedTrade() {
    FixedInflationSwapCurveNode node = FixedInflationSwapCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    ResolvedSwapTrade trade = node.sampleResolvedTrade(valuationDate, FxRateProvider.minimal(), REF_DATA);
    ResolvedSwapTrade expected = TEMPLATE.createTrade(valuationDate, BuySell.SELL, 1d, SPREAD, REF_DATA).resolve(REF_DATA);
    assertThat(trade).isEqualTo(expected);
  }

  @Test
  public void test_initialGuess() {
    FixedInflationSwapCurveNode node = FixedInflationSwapCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    double rate = 0.035;
    double lastPriceIndex = 123.4;
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.builder()
        .put(LocalDate.of(2024, 10, 31), lastPriceIndex).build();
    MarketData marketData = ImmutableMarketData.builder(valuationDate).addValue(QUOTE_ID, rate)
        .addTimeSeries(IndexQuoteId.of(PriceIndices.EU_EXT_CPI), ts).build();
    assertThat(node.initialGuess(marketData, ValueType.ZERO_RATE)).isEqualTo(rate);
    double priceIndexGuess = lastPriceIndex * Math.pow(1.0d + rate, TENOR_10Y.get(ChronoUnit.YEARS));
    assertThat(node.initialGuess(marketData, ValueType.PRICE_INDEX)).isCloseTo(priceIndexGuess, offset(TOLERANCE_GUESS));
  }

  @Test
  public void test_initialGuess_wrongType() {
    FixedInflationSwapCurveNode node = FixedInflationSwapCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE).build();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> node.initialGuess(marketData, ValueType.BLACK_VOLATILITY));
  }

  @Test
  public void test_metadata_end() {
    FixedInflationSwapCurveNode node = FixedInflationSwapCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    ParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    // 2015-01-22 is Thursday, start is 2015-01-26, but 2025-01-26 is Sunday, so end is 2025-01-27
    assertThat(((TenorDateParameterMetadata) metadata).getDate()).isEqualTo(LocalDate.of(2025, 1, 27));
    assertThat(((TenorDateParameterMetadata) metadata).getTenor()).isEqualTo(Tenor.TENOR_10Y);
  }

  @Test
  public void test_metadata_fixed() {
    FixedInflationSwapCurveNode node =
        FixedInflationSwapCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD, LABEL).withDate(CurveNodeDate.of(VAL_DATE));
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    DatedParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    assertThat(metadata.getDate()).isEqualTo(VAL_DATE);
    assertThat(metadata.getLabel()).isEqualTo(node.getLabel());
  }

  @Test
  public void test_metadata_last_fixing() {
    FixedInflationSwapCurveNode node =
        FixedInflationSwapCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD, LABEL).withDate(CurveNodeDate.LAST_FIXING);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    LocalDate fixingExpected = LocalDate.of(2024, 10, 31); // Last day of the month
    DatedParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    assertThat(metadata.getDate()).isEqualTo(fixingExpected);
    assertThat(metadata.getLabel()).isEqualTo(node.getLabel());
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    FixedInflationSwapCurveNode test = FixedInflationSwapCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    coverImmutableBean(test);
    FixedInflationSwapCurveNode test2 = FixedInflationSwapCurveNode.of(
        FixedInflationSwapTemplate.of(TENOR_10Y, FixedInflationSwapConventions.USD_FIXED_ZC_US_CPI),
        QuoteId.of(StandardId.of("OG-Ticker", "Deposit2")));
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    FixedInflationSwapCurveNode test = FixedInflationSwapCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    assertSerialization(test);
  }

}
