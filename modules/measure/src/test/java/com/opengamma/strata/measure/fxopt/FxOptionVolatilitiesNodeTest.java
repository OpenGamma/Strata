/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.option.DeltaStrike;
import com.opengamma.strata.market.option.SimpleStrike;
import com.opengamma.strata.market.option.Strike;
import com.opengamma.strata.pricer.fxopt.FxVolatilitySurfaceYearFractionParameterMetadata;

/**
 * Test {@link FxOptionVolatilitiesNode}.
 */
public class FxOptionVolatilitiesNodeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final CurrencyPair EUR_GBP = CurrencyPair.of(EUR, GBP);
  private static final CurrencyPair GBP_USD = CurrencyPair.of(GBP, USD);
  private static final String LABEL = new String("LABEL");
  private static final HolidayCalendarId LO_TA = GBLO.combinedWith(EUTA);
  private static final HolidayCalendarId LO_NY = GBLO.combinedWith(USNY);
  private static final DaysAdjustment SPOT_DATE_OFFSET = DaysAdjustment.ofBusinessDays(2, LO_TA);
  private static final BusinessDayAdjustment BDA = BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, LO_TA);
  private static final DaysAdjustment EXPIRY_DATE_OFFSET = DaysAdjustment.ofBusinessDays(-3, LO_TA);
  private static final QuoteId QUOTE_ID = QuoteId.of(StandardId.of("OG", "TEST"));
  private static final Strike STRIKE = SimpleStrike.of(0.95);

  @Test
  public void test_builder() {
    FxOptionVolatilitiesNode test = FxOptionVolatilitiesNode.builder()
        .currencyPair(EUR_GBP)
        .label(LABEL)
        .spotDateOffset(SPOT_DATE_OFFSET)
        .businessDayAdjustment(BDA)
        .expiryDateOffset(EXPIRY_DATE_OFFSET)
        .quoteValueType(ValueType.BLACK_VOLATILITY)
        .quoteId(QUOTE_ID)
        .tenor(Tenor.TENOR_3M)
        .strike(STRIKE)
        .build();
    assertThat(test.getBusinessDayAdjustment()).isEqualTo(BDA);
    assertThat(test.getCurrencyPair()).isEqualTo(EUR_GBP);
    assertThat(test.getLabel()).isEqualTo(LABEL);
    assertThat(test.getQuoteValueType()).isEqualTo(ValueType.BLACK_VOLATILITY);
    assertThat(test.getSpotDateOffset()).isEqualTo(SPOT_DATE_OFFSET);
    assertThat(test.getExpiryDateOffset()).isEqualTo(EXPIRY_DATE_OFFSET);
    assertThat(test.getStrike()).isEqualTo(STRIKE);
    assertThat(test.getTenor()).isEqualTo(Tenor.TENOR_3M);
  }

  @Test
  public void test_builder_noExp() {
    FxOptionVolatilitiesNode test = FxOptionVolatilitiesNode.builder()
        .currencyPair(EUR_GBP)
        .label(LABEL)
        .spotDateOffset(SPOT_DATE_OFFSET)
        .businessDayAdjustment(BDA)
        .quoteValueType(ValueType.BLACK_VOLATILITY)
        .quoteId(QUOTE_ID)
        .tenor(Tenor.TENOR_3M)
        .strike(STRIKE)
        .build();
    assertThat(test.getBusinessDayAdjustment()).isEqualTo(BDA);
    assertThat(test.getCurrencyPair()).isEqualTo(EUR_GBP);
    assertThat(test.getLabel()).isEqualTo(LABEL);
    assertThat(test.getQuoteValueType()).isEqualTo(ValueType.BLACK_VOLATILITY);
    assertThat(test.getSpotDateOffset()).isEqualTo(SPOT_DATE_OFFSET);
    assertThat(test.getExpiryDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(-2, LO_TA));
    assertThat(test.getStrike()).isEqualTo(STRIKE);
    assertThat(test.getTenor()).isEqualTo(Tenor.TENOR_3M);
  }

  @Test
  public void test_of() {
    FxOptionVolatilitiesNode test = FxOptionVolatilitiesNode.of(
        EUR_GBP, SPOT_DATE_OFFSET, BDA, ValueType.BLACK_VOLATILITY, QUOTE_ID, Tenor.TENOR_3M, STRIKE);
    assertThat(test.getBusinessDayAdjustment()).isEqualTo(BDA);
    assertThat(test.getCurrencyPair()).isEqualTo(EUR_GBP);
    assertThat(test.getLabel()).isEqualTo(QUOTE_ID.toString());
    assertThat(test.getQuoteValueType()).isEqualTo(ValueType.BLACK_VOLATILITY);
    assertThat(test.getSpotDateOffset()).isEqualTo(SPOT_DATE_OFFSET);
    assertThat(test.getExpiryDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(-2, LO_TA));
    assertThat(test.getStrike()).isEqualTo(STRIKE);
    assertThat(test.getTenor()).isEqualTo(Tenor.TENOR_3M);
  }

  @Test
  public void test_expiry() {
    FxOptionVolatilitiesNode test = FxOptionVolatilitiesNode.of(
        EUR_GBP, SPOT_DATE_OFFSET, BDA, ValueType.BLACK_VOLATILITY, QUOTE_ID, Tenor.TENOR_3M, STRIKE);
    ZonedDateTime dateTime = LocalDate.of(2016, 1, 23).atStartOfDay(ZoneId.of("Europe/London"));
    DaysAdjustment expAdj = DaysAdjustment.ofBusinessDays(-2, LO_TA);
    double computed = test.timeToExpiry(dateTime, ACT_365F, REF_DATA);
    double expected = ACT_365F.relativeYearFraction(
        dateTime.toLocalDate(),
        expAdj.adjust(BDA.adjust(SPOT_DATE_OFFSET.adjust(dateTime.toLocalDate(), REF_DATA).plus(Tenor.TENOR_3M), REF_DATA),
            REF_DATA));
    assertThat(computed).isEqualTo(expected);
    FxVolatilitySurfaceYearFractionParameterMetadata metadata = test.metadata(dateTime, ACT_365F, REF_DATA);
    assertThat(metadata.getYearFraction()).isEqualTo(computed);
    assertThat(metadata.getYearFractionTenor()).hasValue(Tenor.TENOR_3M);
    assertThat(metadata.getStrike()).isEqualTo(STRIKE);
    assertThat(metadata.getCurrencyPair()).isEqualTo(EUR_GBP);
  }

  @Test
  public void test_expiry_standard() {
    DaysAdjustment spotLag = DaysAdjustment.ofBusinessDays(2, LO_NY);
    BusinessDayAdjustment bda = BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, LO_NY);
    FxOptionVolatilitiesNode[] nodes = new FxOptionVolatilitiesNode[] {
        FxOptionVolatilitiesNode.of(GBP_USD, spotLag, bda, ValueType.BLACK_VOLATILITY, QUOTE_ID, Tenor.TENOR_2M, STRIKE),
        FxOptionVolatilitiesNode.of(GBP_USD, spotLag, bda, ValueType.BLACK_VOLATILITY, QUOTE_ID, Tenor.TENOR_10M, STRIKE),
        FxOptionVolatilitiesNode.of(GBP_USD, spotLag, bda, ValueType.BLACK_VOLATILITY, QUOTE_ID, Tenor.TENOR_4M, STRIKE)};
    ZonedDateTime[] valDates = new ZonedDateTime[] {
        LocalDate.of(2017, 10, 25).atStartOfDay(ZoneId.of("Europe/London")),
        LocalDate.of(2017, 10, 25).atStartOfDay(ZoneId.of("Europe/London")),
        LocalDate.of(2017, 10, 27).atStartOfDay(ZoneId.of("Europe/London"))};
    LocalDate[] expDates = new LocalDate[] {LocalDate.of(2017, 12, 21), LocalDate.of(2018, 8, 23), LocalDate.of(2018, 2, 26)};
    for (int i = 0; i < expDates.length; ++i) {
      double computed = nodes[i].timeToExpiry(valDates[i], ACT_365F, REF_DATA);
      double expected = ACT_365F.relativeYearFraction(valDates[i].toLocalDate(), expDates[i]);
      assertThat(computed).isEqualTo(expected);
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    FxOptionVolatilitiesNode test1 = FxOptionVolatilitiesNode.of(
        EUR_GBP, SPOT_DATE_OFFSET, BDA, ValueType.BLACK_VOLATILITY, QUOTE_ID, Tenor.TENOR_3M, STRIKE);
    coverImmutableBean(test1);
    FxOptionVolatilitiesNode test2 = FxOptionVolatilitiesNode.of(
        CurrencyPair.of(GBP, USD),
        DaysAdjustment.NONE,
        BusinessDayAdjustment.NONE,
        ValueType.RISK_REVERSAL,
        QuoteId.of(StandardId.of("OG", "foo")),
        Tenor.TENOR_6M, DeltaStrike.of(0.1));
    coverBeanEquals(test1, test2);
  }

  @Test
  public void serialization() {
    FxOptionVolatilitiesNode test = FxOptionVolatilitiesNode.of(
        EUR_GBP, SPOT_DATE_OFFSET, BDA, ValueType.BLACK_VOLATILITY, QUOTE_ID, Tenor.TENOR_3M, STRIKE);
    assertSerialization(test);
  }

}
