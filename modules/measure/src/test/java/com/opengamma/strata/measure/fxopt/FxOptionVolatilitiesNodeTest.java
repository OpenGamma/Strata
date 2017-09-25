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
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

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

/**
 * Test {@link FxOptionVolatilitiesNode}.
 */
@Test
public class FxOptionVolatilitiesNodeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final CurrencyPair EUR_GBP = CurrencyPair.of(EUR, GBP);
  private static final String LABEL = new String("LABEL");
  private static final HolidayCalendarId CALENDAR = GBLO.combinedWith(EUTA);
  private static final DaysAdjustment SPOT_DATE_OFFSET = DaysAdjustment.ofBusinessDays(2, CALENDAR);
  private static final BusinessDayAdjustment BUS_ADJ = BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, CALENDAR);
  private static final QuoteId QUOTE_ID = QuoteId.of(StandardId.of("OG", "TEST"));
  private static final Strike STRIKE = SimpleStrike.of(0.95);

  public void test_of() {
    FxOptionVolatilitiesNode test = FxOptionVolatilitiesNode.of(
        EUR_GBP, LABEL, SPOT_DATE_OFFSET, BUS_ADJ, ValueType.BLACK_VOLATILITY, QUOTE_ID, Tenor.TENOR_3M, STRIKE);
    assertEquals(test.getBusinessDayAdjustment(), BUS_ADJ);
    assertEquals(test.getCurrencyPair(), EUR_GBP);
    assertEquals(test.getLabel(), LABEL);
    assertEquals(test.getQuoteValueType(), ValueType.BLACK_VOLATILITY);
    assertEquals(test.getSpotDateOffset(), SPOT_DATE_OFFSET);
    assertEquals(test.getStrike(), STRIKE);
    assertEquals(test.getTenor(), Tenor.TENOR_3M);
  }

  public void test_expiry() {
    FxOptionVolatilitiesNode test = FxOptionVolatilitiesNode.of(
        EUR_GBP, SPOT_DATE_OFFSET, BUS_ADJ, ValueType.BLACK_VOLATILITY, QUOTE_ID, Tenor.TENOR_3M, STRIKE);
    ZonedDateTime dateTime = LocalDate.of(2016, 1, 23).atStartOfDay(ZoneId.of("Z"));
    double computed = test.timeToExpiry(dateTime, ACT_365F, REF_DATA);
    double expected = ACT_365F.relativeYearFraction(
        dateTime.toLocalDate(),
        BUS_ADJ.adjust(SPOT_DATE_OFFSET.adjust(dateTime.toLocalDate(), REF_DATA).plus(Tenor.TENOR_3M), REF_DATA));
    assertEquals(computed, expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FxOptionVolatilitiesNode test1 = FxOptionVolatilitiesNode.of(
        EUR_GBP, SPOT_DATE_OFFSET, BUS_ADJ, ValueType.BLACK_VOLATILITY, QUOTE_ID, Tenor.TENOR_3M, STRIKE);
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

  public void serialization() {
    FxOptionVolatilitiesNode test = FxOptionVolatilitiesNode.of(
        EUR_GBP, SPOT_DATE_OFFSET, BUS_ADJ, ValueType.BLACK_VOLATILITY, QUOTE_ID, Tenor.TENOR_3M, STRIKE);
    assertSerialization(test);
  }

}
