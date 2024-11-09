/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.capfloor;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.index.OvernightIndices.EUR_EONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.EUR_ESTR;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.OvernightIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.basics.value.ValueStep;
import com.opengamma.strata.product.rate.OvernightCompoundedRateComputation;
import com.opengamma.strata.product.swap.OvernightAccrualMethod;
import com.opengamma.strata.product.swap.OvernightRateCalculation;

/**
 * Test {@link OvernightInArrearsCapFloorLeg}.
 */
public class OvernightInArrearsCapFloorLegTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate START = LocalDate.of(2011, 3, 17);
  private static final LocalDate END = LocalDate.of(2012, 3, 17);
  private static final OvernightRateCalculation RATE_CALCULATION = OvernightRateCalculation.of(EUR_ESTR);
  private static final Frequency FREQUENCY = Frequency.P3M;
  private static final BusinessDayAdjustment BUSS_ADJ =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, EUTA);
  private static final PeriodicSchedule SCHEDULE = PeriodicSchedule.builder()
      .startDate(START)
      .endDate(END)
      .frequency(FREQUENCY)
      .businessDayAdjustment(BUSS_ADJ)
      .stubConvention(StubConvention.NONE)
      .build();
  private static final DaysAdjustment PAYMENT_OFFSET = DaysAdjustment.ofBusinessDays(2, EUTA);

  private static final double[] NOTIONALS = new double[] {1.0e6, 1.2e6, 0.8e6, 1.0e6};
  private static final double[] STRIKES = new double[] {0.03, 0.0275, 0.02, 0.0345};
  private static final ValueSchedule CAP = ValueSchedule.of(0.0325);
  private static final List<ValueStep> FLOOR_STEPS = new ArrayList<ValueStep>();
  private static final List<ValueStep> NOTIONAL_STEPS = new ArrayList<ValueStep>();
  static {
    FLOOR_STEPS.add(ValueStep.of(1, ValueAdjustment.ofReplace(STRIKES[1])));
    FLOOR_STEPS.add(ValueStep.of(2, ValueAdjustment.ofReplace(STRIKES[2])));
    FLOOR_STEPS.add(ValueStep.of(3, ValueAdjustment.ofReplace(STRIKES[3])));
    NOTIONAL_STEPS.add(ValueStep.of(1, ValueAdjustment.ofReplace(NOTIONALS[1])));
    NOTIONAL_STEPS.add(ValueStep.of(2, ValueAdjustment.ofReplace(NOTIONALS[2])));
    NOTIONAL_STEPS.add(ValueStep.of(3, ValueAdjustment.ofReplace(NOTIONALS[3])));
  }
  private static final ValueSchedule FLOOR = ValueSchedule.of(STRIKES[0], FLOOR_STEPS);
  private static final ValueSchedule NOTIONAL = ValueSchedule.of(NOTIONALS[0], NOTIONAL_STEPS);

  @Test
  public void test_builder_full() {
    OvernightInArrearsCapFloorLeg test = OvernightInArrearsCapFloorLeg.builder()
        .calculation(RATE_CALCULATION)
        .capSchedule(CAP)
        .currency(GBP)
        .notional(NOTIONAL)
        .paymentDateOffset(PAYMENT_OFFSET)
        .paymentSchedule(SCHEDULE)
        .payReceive(PAY)
        .build();
    assertThat(test.getCalculation()).isEqualTo(RATE_CALCULATION);
    assertThat(test.getCapSchedule().get()).isEqualTo(CAP);
    assertThat(test.getFloorSchedule()).isNotPresent();
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getNotional()).isEqualTo(NOTIONAL);
    assertThat(test.getPaymentDateOffset()).isEqualTo(PAYMENT_OFFSET);
    assertThat(test.getPaymentSchedule()).isEqualTo(SCHEDULE);
    assertThat(test.getPayReceive()).isEqualTo(PAY);
    assertThat(test.getStartDate()).isEqualTo(AdjustableDate.of(START, BUSS_ADJ));
    assertThat(test.getEndDate()).isEqualTo(AdjustableDate.of(END, BUSS_ADJ));
    assertThat(test.getIndex()).isEqualTo(EUR_ESTR);
  }

  @Test
  public void test_builder_min() {
    OvernightInArrearsCapFloorLeg test = OvernightInArrearsCapFloorLeg.builder()
        .calculation(RATE_CALCULATION)
        .floorSchedule(FLOOR)
        .notional(NOTIONAL)
        .paymentSchedule(SCHEDULE)
        .payReceive(RECEIVE)
        .build();
    assertThat(test.getCalculation()).isEqualTo(RATE_CALCULATION);
    assertThat(test.getCapSchedule()).isNotPresent();
    assertThat(test.getFloorSchedule().get()).isEqualTo(FLOOR);
    assertThat(test.getCurrency()).isEqualTo(EUR);
    assertThat(test.getNotional()).isEqualTo(NOTIONAL);
    assertThat(test.getPaymentDateOffset()).isEqualTo(DaysAdjustment.NONE);
    assertThat(test.getPaymentSchedule()).isEqualTo(SCHEDULE);
    assertThat(test.getPayReceive()).isEqualTo(RECEIVE);
    assertThat(test.getStartDate()).isEqualTo(AdjustableDate.of(START, BUSS_ADJ));
    assertThat(test.getEndDate()).isEqualTo(AdjustableDate.of(END, BUSS_ADJ));
  }

  @Test
  public void test_builder_fail() {
    // cap and floor present 
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OvernightInArrearsCapFloorLeg.builder()
            .calculation(RATE_CALCULATION)
            .capSchedule(CAP)
            .floorSchedule(FLOOR)
            .notional(NOTIONAL)
            .paymentSchedule(SCHEDULE)
            .payReceive(RECEIVE)
            .build());
    // cap and floor missing
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OvernightInArrearsCapFloorLeg.builder()
            .calculation(RATE_CALCULATION)
            .notional(NOTIONAL)
            .paymentSchedule(PeriodicSchedule.builder()
                .startDate(START)
                .endDate(END)
                .frequency(FREQUENCY)
                .businessDayAdjustment(BUSS_ADJ)
                .build())
            .payReceive(RECEIVE)
            .build());
    // stub type
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OvernightInArrearsCapFloorLeg.builder()
            .calculation(RATE_CALCULATION)
            .capSchedule(CAP)
            .currency(GBP)
            .notional(NOTIONAL)
            .paymentDateOffset(PAYMENT_OFFSET)
            .paymentSchedule(PeriodicSchedule.builder()
                .startDate(START)
                .endDate(END)
                .frequency(FREQUENCY)
                .businessDayAdjustment(BUSS_ADJ)
                .stubConvention(StubConvention.SHORT_FINAL)
                .build())
            .payReceive(PAY)
            .build());
    // accrual method not compounded
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OvernightInArrearsCapFloorLeg.builder()
            .calculation(OvernightRateCalculation.builder()
                .index(EUR_ESTR)
                .accrualMethod(OvernightAccrualMethod.AVERAGED_DAILY)
                .build())
            .floorSchedule(FLOOR)
            .notional(NOTIONAL)
            .paymentSchedule(SCHEDULE)
            .payReceive(RECEIVE)
            .build());
  }

  @Test
  public void test_resolve_cap() {
    OvernightRateCalculation rateCalc = OvernightRateCalculation.of(EUR_EONIA);
    OvernightInArrearsCapFloorLeg base = OvernightInArrearsCapFloorLeg.builder()
        .calculation(rateCalc)
        .capSchedule(CAP)
        .notional(NOTIONAL)
        .paymentDateOffset(PAYMENT_OFFSET)
        .paymentSchedule(SCHEDULE)
        .payReceive(RECEIVE)
        .build();
    LocalDate[] unadjustedDates = new LocalDate[]{
        START, START.plusMonths(3),
        START.plusMonths(6),
        START.plusMonths(9),
        START.plusMonths(12)};
    OvernightInArrearsCapletFloorletPeriod[] periods = new OvernightInArrearsCapletFloorletPeriod[4];
    for (int i = 0; i < 4; ++i) {
      LocalDate start = BUSS_ADJ.adjust(unadjustedDates[i], REF_DATA);
      LocalDate end = BUSS_ADJ.adjust(unadjustedDates[i + 1], REF_DATA);
      double yearFraction = rateCalc.getDayCount().relativeYearFraction(start, end);
      periods[i] = OvernightInArrearsCapletFloorletPeriod.builder()
          .caplet(CAP.getInitialValue())
          .currency(EUR)
          .startDate(start)
          .endDate(end)
          .unadjustedStartDate(unadjustedDates[i])
          .unadjustedEndDate(unadjustedDates[i + 1])
          .paymentDate(PAYMENT_OFFSET.adjust(end, REF_DATA))
          .notional(NOTIONALS[i])
          .overnightRate(OvernightCompoundedRateComputation.of(EUR_EONIA, start, end, REF_DATA))
          .yearFraction(yearFraction)
          .build();
    }
    ResolvedOvernightInArrearsCapFloorLeg expected = ResolvedOvernightInArrearsCapFloorLeg.builder()
        .capletFloorletPeriods(periods)
        .payReceive(RECEIVE)
        .build();
    ResolvedOvernightInArrearsCapFloorLeg computed = base.resolve(REF_DATA);
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_resolve_floor() {
    OvernightInArrearsCapFloorLeg base = OvernightInArrearsCapFloorLeg.builder()
        .calculation(RATE_CALCULATION)
        .floorSchedule(FLOOR)
        .currency(EUR)
        .notional(NOTIONAL)
        .paymentDateOffset(PAYMENT_OFFSET)
        .paymentSchedule(SCHEDULE)
        .payReceive(PAY)
        .build();
    LocalDate[] unadjustedDates = new LocalDate[] {
        START,
        START.plusMonths(3),
        START.plusMonths(6),
        START.plusMonths(9),
        START.plusMonths(12)};
    OvernightInArrearsCapletFloorletPeriod[] periods = new OvernightInArrearsCapletFloorletPeriod[4];
    for (int i = 0; i < 4; ++i) {
      LocalDate start = BUSS_ADJ.adjust(unadjustedDates[i], REF_DATA);
      LocalDate end = BUSS_ADJ.adjust(unadjustedDates[i + 1], REF_DATA);
      double yearFraction = RATE_CALCULATION.getDayCount().relativeYearFraction(start, end);
      periods[i] = OvernightInArrearsCapletFloorletPeriod.builder()
          .floorlet(STRIKES[i])
          .currency(EUR)
          .startDate(start)
          .endDate(end)
          .unadjustedStartDate(unadjustedDates[i])
          .unadjustedEndDate(unadjustedDates[i + 1])
          .paymentDate(PAYMENT_OFFSET.adjust(end, REF_DATA))
          .notional(-NOTIONALS[i])
          .overnightRate(OvernightCompoundedRateComputation.of(EUR_ESTR, start, end, REF_DATA))
          .yearFraction(yearFraction)
          .build();
    }
    ResolvedOvernightInArrearsCapFloorLeg expected = ResolvedOvernightInArrearsCapFloorLeg.builder()
        .capletFloorletPeriods(periods)
        .payReceive(PAY)
        .build();
    ResolvedOvernightInArrearsCapFloorLeg computed = base.resolve(REF_DATA);
    assertThat(computed).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    OvernightInArrearsCapFloorLeg test1 = OvernightInArrearsCapFloorLeg.builder()
        .calculation(RATE_CALCULATION)
        .floorSchedule(FLOOR)
        .notional(NOTIONAL)
        .paymentSchedule(SCHEDULE)
        .payReceive(RECEIVE)
        .build();
    coverImmutableBean(test1);
    OvernightInArrearsCapFloorLeg test2 = OvernightInArrearsCapFloorLeg.builder()
        .calculation(OvernightRateCalculation.of(OvernightIndices.GBP_SONIA))
        .capSchedule(CAP)
        .notional(ValueSchedule.of(1000))
        .paymentDateOffset(PAYMENT_OFFSET)
        .paymentSchedule(PeriodicSchedule.builder()
            .startDate(START)
            .endDate(END)
            .frequency(Frequency.P6M)
            .businessDayAdjustment(BUSS_ADJ)
            .build())
        .payReceive(PAY)
        .build();
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    OvernightInArrearsCapFloorLeg test = OvernightInArrearsCapFloorLeg.builder()
        .calculation(RATE_CALCULATION)
        .floorSchedule(FLOOR)
        .notional(NOTIONAL)
        .paymentSchedule(SCHEDULE)
        .payReceive(RECEIVE)
        .build();
    assertSerialization(test);
  }

}
