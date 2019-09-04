/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.node;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SAT_SUN;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.market.curve.SwapIsdaCreditCurveNode;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.TenorDateParameterMetadata;

/**
 * Test {@link SwapIsdaCreditCurveNode}.
 */
public class SwapIsdaCreditCurveNodeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final ObservableId OBS_ID = QuoteId.of(StandardId.of("OG", "USD_IRS_10Y"));
  private static final Tenor TENOR = Tenor.TENOR_10Y;
  private static final BusinessDayAdjustment BUS_ADJ = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, SAT_SUN);
  private static final DaysAdjustment ADJ_3D = DaysAdjustment.ofBusinessDays(3, SAT_SUN);
  private static final Frequency FREQUENCY = Frequency.P6M;
  private static final LocalDate TRADE_DATE = LocalDate.of(2016, 9, 29);

  @Test
  public void test_of() {
    SwapIsdaCreditCurveNode test = SwapIsdaCreditCurveNode.of(OBS_ID, ADJ_3D, BUS_ADJ, TENOR, THIRTY_U_360, FREQUENCY);
    assertThat(test.getBusinessDayAdjustment()).isEqualTo(BUS_ADJ);
    assertThat(test.getDayCount()).isEqualTo(THIRTY_U_360);
    assertThat(test.getLabel()).isEqualTo(TENOR.toString());
    assertThat(test.getObservableId()).isEqualTo(OBS_ID);
    assertThat(test.getPaymentFrequency()).isEqualTo(FREQUENCY);
    assertThat(test.getSpotDateOffset()).isEqualTo(ADJ_3D);
    assertThat(test.getTenor()).isEqualTo(TENOR);
    assertThat(test.date(TRADE_DATE, REF_DATA)).isEqualTo(LocalDate.of(2026, 10, 5));
    assertThat(test.metadata(LocalDate.of(2026, 10, 5)))
        .isEqualTo(TenorDateParameterMetadata.of(LocalDate.of(2026, 10, 5), TENOR));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    SwapIsdaCreditCurveNode test1 = SwapIsdaCreditCurveNode.of(OBS_ID, ADJ_3D, BUS_ADJ, TENOR, THIRTY_U_360, FREQUENCY);
    coverImmutableBean(test1);
    SwapIsdaCreditCurveNode test2 = SwapIsdaCreditCurveNode.builder()
        .observableId(QuoteId.of(StandardId.of("OG", "foo")))
        .spotDateOffset(DaysAdjustment.NONE)
        .businessDayAdjustment(BusinessDayAdjustment.NONE)
        .tenor(Tenor.TENOR_15Y)
        .dayCount(DayCounts.ACT_365F)
        .paymentFrequency(Frequency.P3M)
        .label("test2")
        .build();
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    SwapIsdaCreditCurveNode test = SwapIsdaCreditCurveNode.of(OBS_ID, ADJ_3D, BUS_ADJ, TENOR, THIRTY_U_360, FREQUENCY);
    assertSerialization(test);
  }

}
