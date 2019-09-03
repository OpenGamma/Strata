/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.node;

import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
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
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
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
import com.opengamma.strata.product.fx.FxSwapTrade;
import com.opengamma.strata.product.fx.type.FxSwapTemplate;
import com.opengamma.strata.product.fx.type.ImmutableFxSwapConvention;

/**
 * Test {@link FxSwapCurveNode}.
 */
public class FxSwapCurveNodeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = date(2015, 6, 30);
  private static final CurrencyPair EUR_USD = CurrencyPair.of(Currency.EUR, Currency.USD);
  private static final HolidayCalendarId EUTA_USNY = EUTA.combinedWith(USNY);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, EUTA_USNY);
  private static final ImmutableFxSwapConvention CONVENTION = ImmutableFxSwapConvention.of(EUR_USD, PLUS_TWO_DAYS);
  private static final Period NEAR_PERIOD = Period.ofMonths(3);
  private static final Period FAR_PERIOD = Period.ofMonths(6);
  private static final FxSwapTemplate TEMPLATE = FxSwapTemplate.of(NEAR_PERIOD, FAR_PERIOD, CONVENTION);

  private static final ObservableSource OBS_SOURCE = ObservableSource.of("Vendor");
  private static final FxRateId FX_RATE_ID = FxRateId.of(EUR_USD);
  private static final FxRateId FX_RATE_ID2 = FxRateId.of(EUR_USD, OBS_SOURCE);
  private static final QuoteId QUOTE_ID_PTS = QuoteId.of(StandardId.of("OG-Ticker", "EUR_USD_3M_6M"));
  private static final QuoteId QUOTE_ID_PTS2 = QuoteId.of(StandardId.of("OG-Ticker", "EUR_USD_3M_6M2"));
  private static final FxRate FX_RATE_NEAR = FxRate.of(EUR_USD, 1.30d);
  private static final double FX_RATE_PTS = 0.0050d;
  private static final String LABEL = "Label";
  private static final String LABEL_AUTO = "6M";
  private static final MarketData MARKET_DATA = ImmutableMarketData.builder(VAL_DATE)
      .addValue(FX_RATE_ID, FX_RATE_NEAR)
      .addValue(QUOTE_ID_PTS, FX_RATE_PTS)
      .build();

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    FxSwapCurveNode test = FxSwapCurveNode.builder()
        .label(LABEL)
        .template(TEMPLATE)
        .fxRateId(FX_RATE_ID2)
        .farForwardPointsId(QUOTE_ID_PTS)
        .date(CurveNodeDate.LAST_FIXING)
        .build();
    assertThat(test.getLabel()).isEqualTo(LABEL);
    assertThat(test.getFxRateId()).isEqualTo(FX_RATE_ID2);
    assertThat(test.getFarForwardPointsId()).isEqualTo(QUOTE_ID_PTS);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
    assertThat(test.getDate()).isEqualTo(CurveNodeDate.LAST_FIXING);
  }

  @Test
  public void test_builder_defaults() {
    FxSwapCurveNode test = FxSwapCurveNode.builder()
        .template(TEMPLATE)
        .farForwardPointsId(QUOTE_ID_PTS)
        .build();
    assertThat(test.getLabel()).isEqualTo(LABEL_AUTO);
    assertThat(test.getFxRateId()).isEqualTo(FX_RATE_ID);
    assertThat(test.getFarForwardPointsId()).isEqualTo(QUOTE_ID_PTS);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
    assertThat(test.getDate()).isEqualTo(CurveNodeDate.END);
  }

  @Test
  public void test_builder_noTemplate() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxSwapCurveNode.builder().label(LABEL).farForwardPointsId(QUOTE_ID_PTS).build());
  }

  @Test
  public void test_of() {
    FxSwapCurveNode test = FxSwapCurveNode.of(TEMPLATE, QUOTE_ID_PTS);
    assertThat(test.getLabel()).isEqualTo(LABEL_AUTO);
    assertThat(test.getFxRateId()).isEqualTo(FX_RATE_ID);
    assertThat(test.getFarForwardPointsId()).isEqualTo(QUOTE_ID_PTS);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
  }

  @Test
  public void test_of_withLabel() {
    FxSwapCurveNode test = FxSwapCurveNode.of(TEMPLATE, QUOTE_ID_PTS, LABEL);
    assertThat(test.getLabel()).isEqualTo(LABEL);
    assertThat(test.getFxRateId()).isEqualTo(FX_RATE_ID);
    assertThat(test.getFarForwardPointsId()).isEqualTo(QUOTE_ID_PTS);
    assertThat(test.getTemplate()).isEqualTo(TEMPLATE);
  }

  @Test
  public void test_requirements() {
    FxSwapCurveNode test = FxSwapCurveNode.of(TEMPLATE, QUOTE_ID_PTS);
    Set<? extends MarketDataId<?>> setExpected = ImmutableSet.of(FX_RATE_ID, QUOTE_ID_PTS);
    Set<? extends MarketDataId<?>> set = test.requirements();
    assertThat(set.equals(setExpected)).isTrue();
  }

  @Test
  public void test_trade() {
    FxSwapCurveNode node = FxSwapCurveNode.of(TEMPLATE, QUOTE_ID_PTS);
    FxSwapTrade trade = node.trade(1d, MARKET_DATA, REF_DATA);
    double rate = FX_RATE_NEAR.fxRate(EUR_USD);
    FxSwapTrade expected = TEMPLATE.createTrade(VAL_DATE, BuySell.BUY, 1.0, rate, FX_RATE_PTS, REF_DATA);
    assertThat(trade).isEqualTo(expected);
    assertThat(node.resolvedTrade(1d, MARKET_DATA, REF_DATA)).isEqualTo(trade.resolve(REF_DATA));
  }

  @Test
  public void test_trade_noMarketData() {
    FxSwapCurveNode node = FxSwapCurveNode.of(TEMPLATE, QUOTE_ID_PTS);
    MarketData marketData = MarketData.empty(VAL_DATE);
    assertThatExceptionOfType(MarketDataNotFoundException.class)
        .isThrownBy(() -> node.trade(1d, marketData, REF_DATA));
  }

  @Test
  public void test_initialGuess() {
    FxSwapCurveNode node = FxSwapCurveNode.of(TEMPLATE, QUOTE_ID_PTS);
    assertThat(node.initialGuess(MARKET_DATA, ValueType.ZERO_RATE)).isEqualTo(0.0d);
    assertThat(node.initialGuess(MARKET_DATA, ValueType.DISCOUNT_FACTOR)).isEqualTo(1.0d);
  }

  @Test
  public void test_metadata_end() {
    FxSwapCurveNode node = FxSwapCurveNode.of(TEMPLATE, QUOTE_ID_PTS);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    LocalDate endDate = CONVENTION.getBusinessDayAdjustment()
        .adjust(CONVENTION.getSpotDateOffset().adjust(valuationDate, REF_DATA).plus(FAR_PERIOD), REF_DATA);
    ParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    assertThat(((TenorDateParameterMetadata) metadata).getDate()).isEqualTo(endDate);
    assertThat(((TenorDateParameterMetadata) metadata).getTenor()).isEqualTo(Tenor.of(FAR_PERIOD));
  }

  @Test
  public void test_metadata_fixed() {
    LocalDate nodeDate = VAL_DATE.plusMonths(1);
    FxSwapCurveNode node = FxSwapCurveNode.of(TEMPLATE, QUOTE_ID_PTS).withDate(CurveNodeDate.of(nodeDate));
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    DatedParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    assertThat(metadata.getDate()).isEqualTo(nodeDate);
    assertThat(metadata.getLabel()).isEqualTo(node.getLabel());
  }

  @Test
  public void test_metadata_last_fixing() {
    FxSwapCurveNode node = FxSwapCurveNode.of(TEMPLATE, QUOTE_ID_PTS).withDate(CurveNodeDate.LAST_FIXING);
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> node.metadata(VAL_DATE, REF_DATA));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    FxSwapCurveNode test = FxSwapCurveNode.of(TEMPLATE, QUOTE_ID_PTS);
    coverImmutableBean(test);
    FxSwapCurveNode test2 = FxSwapCurveNode.builder()
        .label(LABEL)
        .template(FxSwapTemplate.of(Period.ZERO, FAR_PERIOD, CONVENTION))
        .fxRateId(FX_RATE_ID2)
        .farForwardPointsId(QUOTE_ID_PTS2)
        .date(CurveNodeDate.LAST_FIXING)
        .build();
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    FxSwapCurveNode test = FxSwapCurveNode.of(TEMPLATE, QUOTE_ID_PTS);
    assertSerialization(test);
  }

}
