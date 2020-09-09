/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import java.time.YearMonth;
import java.util.Iterator;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.SequenceDate;
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
import com.opengamma.strata.product.index.OvernightFutureTrade;
import com.opengamma.strata.product.index.type.OvernightFutureContractSpec;
import com.opengamma.strata.product.index.type.OvernightFutureContractSpecs;
import com.opengamma.strata.product.index.type.OvernightFutureTemplate;

/**
 * Tests {@link OvernightFutureCurveNode}.
 */
public class OvernightFutureCurveNodeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = date(2015, 6, 30);
  private static final OvernightFutureContractSpec SPEC = OvernightFutureContractSpecs.USD_SOFR_3M_IMM_CME;
  private static final YearMonth YEAR_MONTH = YearMonth.of(2015, 9);
  private static final OvernightFutureTemplate TEMPLATE = OvernightFutureTemplate.of(SequenceDate.base(YEAR_MONTH), SPEC);
  private static final OvernightFutureTemplate TEMPLATE2 =
      OvernightFutureTemplate.of(SequenceDate.full(YEAR_MONTH.plusMonths(3)), SPEC);
  private static final StandardId STANDARD_ID = StandardId.of("OG-Ticker", "OG-EDH6");
  private static final QuoteId QUOTE_ID = QuoteId.of(STANDARD_ID);
  private static final double SPREAD = 0.0001;
  private static final String LABEL = "Label";

  private static final double TOLERANCE_RATE = 1.0E-8;

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    OvernightFutureCurveNode test = OvernightFutureCurveNode.builder()
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
    OvernightFutureCurveNode test = OvernightFutureCurveNode.of(TEMPLATE, QUOTE_ID);
    assertThat(test.getRateId()).isEqualTo(QUOTE_ID);
    assertThat(test.getAdditionalSpread()).isEqualTo(0.0d);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
  }

  @Test
  public void test_of_withSpread() {
    OvernightFutureCurveNode test = OvernightFutureCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    assertThat(test.getRateId()).isEqualTo(QUOTE_ID);
    assertThat(test.getAdditionalSpread()).isEqualTo(SPREAD);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
  }

  @Test
  public void test_of_withSpreadAndLabel() {
    OvernightFutureCurveNode test = OvernightFutureCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD, LABEL);
    assertThat(test.getRateId()).isEqualTo(QUOTE_ID);
    assertThat(test.getAdditionalSpread()).isEqualTo(SPREAD);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
  }

  @Test
  public void test_requirements() {
    OvernightFutureCurveNode test = OvernightFutureCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    Set<ObservableId> set = test.requirements();
    Iterator<ObservableId> itr = set.iterator();
    assertThat(itr.next()).isEqualTo(QUOTE_ID);
    assertThat(itr.hasNext()).isFalse();
  }

  @Test
  public void test_trade() {
    OvernightFutureCurveNode node = OvernightFutureCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    double price = 0.99;
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE).addValue(QUOTE_ID, price).build();
    OvernightFutureTrade trade = node.trade(1d, marketData, REF_DATA);
    OvernightFutureTrade expected = SPEC.createTrade(
        VAL_DATE, SecurityId.of(STANDARD_ID), SequenceDate.base(YEAR_MONTH), 1L, price + SPREAD, REF_DATA);
    assertThat(trade).isEqualTo(expected);
  }

  @Test
  public void test_trade_noMarketData() {
    OvernightFutureCurveNode node = OvernightFutureCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    MarketData marketData = MarketData.empty(VAL_DATE);
    assertThatExceptionOfType(MarketDataNotFoundException.class)
        .isThrownBy(() -> node.trade(1d, marketData, REF_DATA));
  }

  @Test
  public void test_initialGuess() {
    OvernightFutureCurveNode node = OvernightFutureCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    double price = 0.99;
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE).addValue(QUOTE_ID, price).build();
    assertThat(node.initialGuess(marketData, ValueType.ZERO_RATE)).isCloseTo(1.0 - price, offset(TOLERANCE_RATE));
    assertThat(node.initialGuess(marketData, ValueType.FORWARD_RATE)).isCloseTo(1.0 - price, offset(TOLERANCE_RATE));
    assertThat(node.initialGuess(marketData, ValueType.DISCOUNT_FACTOR)).isEqualTo(1d);
    assertThat(node.initialGuess(marketData, ValueType.UNKNOWN)).isCloseTo(0.0d, offset(TOLERANCE_RATE));
  }

  @Test
  public void test_metadata_end() {
    OvernightFutureCurveNode node = OvernightFutureCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD, LABEL);
    LocalDate maturityDate = LocalDate.of(2015, 12, 16); // 3rd Wednesday Dec
    LocalDate referenceDate = TEMPLATE.calculateReferenceDateFromTradeDate(VAL_DATE, REF_DATA);
    ParameterMetadata metadata = node.metadata(VAL_DATE, REF_DATA);
    assertThat(metadata.getLabel()).isEqualTo(LABEL);
    assertThat(metadata instanceof YearMonthDateParameterMetadata).isTrue();
    assertThat(((YearMonthDateParameterMetadata) metadata).getDate()).isEqualTo(maturityDate);
    assertThat(((YearMonthDateParameterMetadata) metadata).getYearMonth()).isEqualTo(YearMonth.from(referenceDate));
  }

  @Test
  public void test_metadata_fixed() {
    LocalDate nodeDate = VAL_DATE.plusMonths(1);
    OvernightFutureCurveNode node =
        OvernightFutureCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD, LABEL).withDate(CurveNodeDate.of(nodeDate));
    DatedParameterMetadata metadata = node.metadata(VAL_DATE, REF_DATA);
    assertThat(metadata.getDate()).isEqualTo(nodeDate);
    assertThat(metadata.getLabel()).isEqualTo(node.getLabel());
  }

  @Test
  public void test_metadata_last_fixing() {
    OvernightFutureCurveNode node =
        OvernightFutureCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD, LABEL).withDate(CurveNodeDate.LAST_FIXING);
    ImmutableMarketData marketData = ImmutableMarketData.builder(VAL_DATE).addValue(QUOTE_ID, 0.0d).build();
    OvernightFutureTrade trade = node.trade(1d, marketData, REF_DATA);
    LocalDate fixingDate = trade.getProduct().getEndDate();
    DatedParameterMetadata metadata = node.metadata(VAL_DATE, REF_DATA);
    assertThat(metadata.getDate()).isEqualTo(fixingDate);
    LocalDate referenceDate = SPEC.calculateReferenceDate(VAL_DATE, SequenceDate.base(YEAR_MONTH), REF_DATA);
    assertThat(((YearMonthDateParameterMetadata) metadata).getYearMonth()).isEqualTo(YearMonth.from(referenceDate));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    OvernightFutureCurveNode test = OvernightFutureCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    coverImmutableBean(test);
    OvernightFutureCurveNode test2 = OvernightFutureCurveNode.of(
        TEMPLATE2,
        QuoteId.of(StandardId.of("OG-Ticker", "Unknown")));
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    OvernightFutureCurveNode test = OvernightFutureCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    assertSerialization(test);
  }

}
