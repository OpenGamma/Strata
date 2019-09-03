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
import static com.opengamma.strata.product.common.BuySell.SELL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.LabelDateParameterMetadata;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.TenorDateParameterMetadata;
import com.opengamma.strata.product.credit.CdsCalibrationTrade;
import com.opengamma.strata.product.credit.CdsQuote;
import com.opengamma.strata.product.credit.CdsTrade;
import com.opengamma.strata.product.credit.type.CdsConventions;
import com.opengamma.strata.product.credit.type.CdsQuoteConvention;
import com.opengamma.strata.product.credit.type.CdsTemplate;
import com.opengamma.strata.product.credit.type.DatesCdsTemplate;
import com.opengamma.strata.product.credit.type.TenorCdsTemplate;

/**
 * Test {@code CdsIsdaCreditCurveNode}.
 */
public class CdsIsdaCreditCurveNodeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = date(2015, 6, 30);
  private static final CdsTemplate TEMPLATE = TenorCdsTemplate.of(TENOR_10Y, CdsConventions.USD_STANDARD);
  private static final LocalDate START_DATE = LocalDate.of(2015, 5, 20);
  private static final LocalDate END_DATE = LocalDate.of(2020, 10, 20);
  private static final CdsTemplate TEMPLATE_NS = DatesCdsTemplate.of(START_DATE, END_DATE, CdsConventions.EUR_GB_STANDARD);
  private static final QuoteId QUOTE_ID = QuoteId.of(StandardId.of("OG-Ticker", "Cds1"));
  private static final String LABEL = "Label";
  private static final String LABEL_AUTO = "10Y";
  private static final StandardId LEGAL_ENTITY = StandardId.of("OG", "ABC");

  @Test
  public void test_builder() {
    CdsIsdaCreditCurveNode test = CdsIsdaCreditCurveNode.builder()
        .label(LABEL)
        .template(TEMPLATE)
        .observableId(QUOTE_ID)
        .quoteConvention(CdsQuoteConvention.PAR_SPREAD)
        .legalEntityId(LEGAL_ENTITY)
        .build();
    assertThat(test.getLabel()).isEqualTo(LABEL);
    assertThat(test.getLegalEntityId()).isEqualTo(LEGAL_ENTITY);
    assertThat(test.getObservableId()).isEqualTo(QUOTE_ID);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
    assertThat(test.date(VAL_DATE, REF_DATA)).isEqualTo(date(2025, 6, 20));
  }

  @Test
  public void test_of_quotedSpread() {
    CdsIsdaCreditCurveNode test = CdsIsdaCreditCurveNode.ofQuotedSpread(TEMPLATE, QUOTE_ID, LEGAL_ENTITY, 0.01);
    assertThat(test.getLabel()).isEqualTo(LABEL_AUTO);
    assertThat(test.getLegalEntityId()).isEqualTo(LEGAL_ENTITY);
    assertThat(test.getObservableId()).isEqualTo(QUOTE_ID);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
    assertThat(test.date(VAL_DATE, REF_DATA)).isEqualTo(date(2025, 6, 20));
  }

  @Test
  public void test_of_pardSpread() {
    CdsIsdaCreditCurveNode test = CdsIsdaCreditCurveNode.ofParSpread(TEMPLATE_NS, QUOTE_ID, LEGAL_ENTITY);
    assertThat(test.getLabel()).isEqualTo(END_DATE.toString());
    assertThat(test.getLegalEntityId()).isEqualTo(LEGAL_ENTITY);
    assertThat(test.getObservableId()).isEqualTo(QUOTE_ID);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE_NS);
    assertThat(test.date(VAL_DATE, REF_DATA)).isEqualTo(END_DATE);
  }

  @Test
  public void test_of_pointsUpfront() {
    CdsIsdaCreditCurveNode test = CdsIsdaCreditCurveNode.ofPointsUpfront(TEMPLATE, QUOTE_ID, LEGAL_ENTITY, 0.01);
    assertThat(test.getLabel()).isEqualTo(LABEL_AUTO);
    assertThat(test.getLegalEntityId()).isEqualTo(LEGAL_ENTITY);
    assertThat(test.getObservableId()).isEqualTo(QUOTE_ID);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
    assertThat(test.date(VAL_DATE, REF_DATA)).isEqualTo(date(2025, 6, 20));
  }

  @Test
  public void test_build_fail_noRate() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CdsIsdaCreditCurveNode.builder()
            .template(TEMPLATE)
            .observableId(QUOTE_ID)
            .legalEntityId(LEGAL_ENTITY)
            .quoteConvention(CdsQuoteConvention.QUOTED_SPREAD)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_trade() {
    CdsIsdaCreditCurveNode node = CdsIsdaCreditCurveNode.ofQuotedSpread(TEMPLATE, QUOTE_ID, LEGAL_ENTITY, 0.01);
    double rate = 0.0125;
    double quantity = -1234.56;
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE).addValue(QUOTE_ID, rate).build();
    CdsCalibrationTrade trade = node.trade(quantity, marketData, REF_DATA);
    CdsTrade expected = TEMPLATE.createTrade(LEGAL_ENTITY, VAL_DATE, SELL, -quantity, 0.01, REF_DATA);
    assertThat(trade.getUnderlyingTrade()).isEqualTo(expected);
    assertThat(trade.getQuote()).isEqualTo(CdsQuote.of(CdsQuoteConvention.QUOTED_SPREAD, rate));

    CdsIsdaCreditCurveNode node1 = CdsIsdaCreditCurveNode.ofParSpread(TEMPLATE, QUOTE_ID, LEGAL_ENTITY);
    CdsTrade expected1 = TEMPLATE.createTrade(LEGAL_ENTITY, VAL_DATE, SELL, -quantity, rate, REF_DATA);
    CdsCalibrationTrade trade1 = node1.trade(quantity, marketData, REF_DATA);
    assertThat(trade1.getUnderlyingTrade()).isEqualTo(expected1);
    assertThat(trade1.getQuote()).isEqualTo(CdsQuote.of(CdsQuoteConvention.PAR_SPREAD, rate));
  }

  @Test
  public void test_trade_noMarketData() {
    CdsIsdaCreditCurveNode node = CdsIsdaCreditCurveNode.ofParSpread(TEMPLATE, QUOTE_ID, LEGAL_ENTITY);
    MarketData marketData = MarketData.empty(VAL_DATE);
    assertThatExceptionOfType(MarketDataNotFoundException.class)
        .isThrownBy(() -> node.trade(1d, marketData, REF_DATA));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_metadata_tenor() {
    CdsIsdaCreditCurveNode node = CdsIsdaCreditCurveNode.ofQuotedSpread(TEMPLATE, QUOTE_ID, LEGAL_ENTITY, 0.01);
    LocalDate nodeDate = LocalDate.of(2015, 1, 22);
    ParameterMetadata metadata = node.metadata(nodeDate);
    assertThat(((TenorDateParameterMetadata) metadata).getDate()).isEqualTo(nodeDate);
    assertThat(((TenorDateParameterMetadata) metadata).getTenor()).isEqualTo(Tenor.TENOR_10Y);
  }

  @Test
  public void test_metadata_dates() {
    CdsIsdaCreditCurveNode node = CdsIsdaCreditCurveNode.ofParSpread(TEMPLATE_NS, QUOTE_ID, LEGAL_ENTITY);
    ParameterMetadata metadata = node.metadata(END_DATE);
    assertThat(((LabelDateParameterMetadata) metadata).getDate()).isEqualTo(END_DATE);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    CdsIsdaCreditCurveNode test1 = CdsIsdaCreditCurveNode.ofQuotedSpread(TEMPLATE, QUOTE_ID, LEGAL_ENTITY, 0.01);
    coverImmutableBean(test1);
    CdsIsdaCreditCurveNode test2 = CdsIsdaCreditCurveNode.ofPointsUpfront(
        TenorCdsTemplate.of(TENOR_10Y, CdsConventions.EUR_GB_STANDARD),
        QuoteId.of(StandardId.of("OG-Ticker", "Cds2")),
        StandardId.of("OG", "DEF"),
        0.01);
    QuoteId.of(StandardId.of("OG-Ticker", "Deposit2"));
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    CdsIsdaCreditCurveNode test = CdsIsdaCreditCurveNode.ofQuotedSpread(TEMPLATE, QUOTE_ID, LEGAL_ENTITY, 0.01);
    assertSerialization(test);
  }

}
