/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.node;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.Iterator;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveNodeDate;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.DatedParameterMetadata;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.YearMonthDateParameterMetadata;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.index.IborFutureTrade;
import com.opengamma.strata.product.index.type.IborFutureConvention;
import com.opengamma.strata.product.index.type.IborFutureConventions;
import com.opengamma.strata.product.index.type.IborFutureTemplate;

/**
 * Tests {@link IborFutureCurveNode}.
 */
public class IborFutureCurveNodeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = date(2015, 6, 30);
  private static final IborFutureConvention CONVENTION = IborFutureConventions.USD_LIBOR_3M_QUARTERLY_IMM;
  private static final Period PERIOD_TO_START = Period.ofMonths(2);
  private static final int NUMBER = 2;
  private static final IborFutureTemplate TEMPLATE = IborFutureTemplate.of(PERIOD_TO_START, NUMBER, CONVENTION);
  private static final StandardId STANDARD_ID = StandardId.of("OG-Ticker", "OG-EDH6");
  private static final QuoteId QUOTE_ID = QuoteId.of(STANDARD_ID);
  private static final double SPREAD = 0.0001;
  private static final String LABEL = "Label";

  private static final double TOLERANCE_RATE = 1.0E-8;

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    IborFutureCurveNode test = IborFutureCurveNode.builder()
        .label(LABEL)
        .template(TEMPLATE)
        .rateId(QUOTE_ID)
        .additionalSpread(SPREAD)
        .build();
    assertThat(test.getRateId()).isEqualTo(QUOTE_ID);
    assertThat(test.getAdditionalSpread()).isEqualTo(SPREAD);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
    assertThat(test.getDate()).isEqualTo(CurveNodeDate.END);
  }

  @Test
  public void test_of_no_spread() {
    IborFutureCurveNode test = IborFutureCurveNode.of(TEMPLATE, QUOTE_ID);
    assertThat(test.getRateId()).isEqualTo(QUOTE_ID);
    assertThat(test.getAdditionalSpread()).isEqualTo(0.0d);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
  }

  @Test
  public void test_of_withSpread() {
    IborFutureCurveNode test = IborFutureCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    assertThat(test.getRateId()).isEqualTo(QUOTE_ID);
    assertThat(test.getAdditionalSpread()).isEqualTo(SPREAD);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
  }

  @Test
  public void test_of_withSpreadAndLabel() {
    IborFutureCurveNode test = IborFutureCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD, LABEL);
    assertThat(test.getRateId()).isEqualTo(QUOTE_ID);
    assertThat(test.getAdditionalSpread()).isEqualTo(SPREAD);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
  }

  @Test
  public void test_requirements() {
    IborFutureCurveNode test = IborFutureCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    Set<ObservableId> set = test.requirements();
    Iterator<ObservableId> itr = set.iterator();
    assertThat(itr.next()).isEqualTo(QUOTE_ID);
    assertThat(itr.hasNext()).isFalse();
  }

  @Test
  public void test_trade() {
    IborFutureCurveNode node = IborFutureCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    double price = 0.99;
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE).addValue(QUOTE_ID, price).build();
    IborFutureTrade trade = node.trade(1d, marketData, REF_DATA);
    IborFutureTrade expected = TEMPLATE.createTrade(
        VAL_DATE, SecurityId.of(STANDARD_ID), 1L, 1.0, price + SPREAD, REF_DATA);
    assertThat(trade).isEqualTo(expected);
  }

  @Test
  public void test_trade_noMarketData() {
    IborFutureCurveNode node = IborFutureCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    MarketData marketData = MarketData.empty(VAL_DATE);
    assertThatExceptionOfType(MarketDataNotFoundException.class)
        .isThrownBy(() -> node.trade(1d, marketData, REF_DATA));
  }

  @Test
  public void test_initialGuess() {
    IborFutureCurveNode node = IborFutureCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    double price = 0.99;
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE).addValue(QUOTE_ID, price).build();
    assertThat(node.initialGuess(marketData, ValueType.ZERO_RATE)).isCloseTo(1.0 - price, offset(TOLERANCE_RATE));
    assertThat(node.initialGuess(marketData, ValueType.FORWARD_RATE)).isCloseTo(1.0 - price, offset(TOLERANCE_RATE));
    double approximateMaturity = TEMPLATE.approximateMaturity(VAL_DATE);
    double df = Math.exp(-approximateMaturity * (1.0 - price));
    assertThat(node.initialGuess(marketData, ValueType.DISCOUNT_FACTOR)).isCloseTo(df, offset(TOLERANCE_RATE));
    assertThat(node.initialGuess(marketData, ValueType.UNKNOWN)).isCloseTo(0.0d, offset(TOLERANCE_RATE));
  }

  @Test
  public void test_metadata_end() {
    IborFutureCurveNode node = IborFutureCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD, LABEL);
    LocalDate date = LocalDate.of(2015, 10, 20);
    LocalDate referenceDate = TEMPLATE.calculateReferenceDateFromTradeDate(date, REF_DATA);
    LocalDate maturityDate = TEMPLATE.getIndex().calculateMaturityFromEffective(referenceDate, REF_DATA);
    ParameterMetadata metadata = node.metadata(date, REF_DATA);
    assertThat(metadata.getLabel()).isEqualTo(LABEL);
    assertThat(metadata instanceof YearMonthDateParameterMetadata).isTrue();
    assertThat(((YearMonthDateParameterMetadata) metadata).getDate()).isEqualTo(maturityDate);
    assertThat(((YearMonthDateParameterMetadata) metadata).getYearMonth()).isEqualTo(YearMonth.from(referenceDate));
  }

  @Test
  public void test_metadata_fixed() {
    LocalDate nodeDate = VAL_DATE.plusMonths(1);
    IborFutureCurveNode node =
        IborFutureCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD, LABEL).withDate(CurveNodeDate.of(nodeDate));
    DatedParameterMetadata metadata = node.metadata(VAL_DATE, REF_DATA);
    assertThat(metadata.getDate()).isEqualTo(nodeDate);
    assertThat(metadata.getLabel()).isEqualTo(node.getLabel());
  }

  @Test
  public void test_metadata_last_fixing() {
    IborFutureCurveNode node =
        IborFutureCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD, LABEL).withDate(CurveNodeDate.LAST_FIXING);
    ImmutableMarketData marketData = ImmutableMarketData.builder(VAL_DATE).addValue(QUOTE_ID, 0.0d).build();
    IborFutureTrade trade = node.trade(1d, marketData, REF_DATA);
    LocalDate fixingDate = trade.getProduct().getFixingDate();
    DatedParameterMetadata metadata = node.metadata(VAL_DATE, REF_DATA);
    assertThat(metadata.getDate()).isEqualTo(fixingDate);
    LocalDate referenceDate = TEMPLATE.calculateReferenceDateFromTradeDate(VAL_DATE, REF_DATA);
    assertThat(((YearMonthDateParameterMetadata) metadata).getYearMonth()).isEqualTo(YearMonth.from(referenceDate));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    IborFutureCurveNode test = IborFutureCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    coverImmutableBean(test);
    IborFutureCurveNode test2 = IborFutureCurveNode.of(
        IborFutureTemplate.of(PERIOD_TO_START, NUMBER, CONVENTION),
        QuoteId.of(StandardId.of("OG-Ticker", "Unknown")));
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    IborFutureCurveNode test = IborFutureCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    assertSerialization(test);
  }

}
