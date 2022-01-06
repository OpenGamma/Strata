/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.cms;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365_ACTUAL;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SAT_SUN;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_6M;
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
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.basics.value.ValueStep;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.FixingRelativeTo;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapIndex;
import com.opengamma.strata.product.swap.SwapIndices;
import com.opengamma.strata.product.swap.type.FixedFloatSwapConvention;

/**
 * Test {@link CmsLeg}.
 */
public class CmsLegTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final SwapIndex INDEX = SwapIndices.EUR_EURIBOR_1100_10Y;
  private static final LocalDate START = LocalDate.of(2015, 10, 21);
  private static final LocalDate END = LocalDate.of(2017, 10, 21);
  private static final Frequency FREQUENCY = Frequency.P12M;
  private static final BusinessDayAdjustment BUSS_ADJ =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, SAT_SUN);
  private static final PeriodicSchedule SCHEDULE = PeriodicSchedule.builder()
      .startDate(START)
      .endDate(END)
      .frequency(FREQUENCY)
      .businessDayAdjustment(BUSS_ADJ)
      .build();
  private static final BusinessDayAdjustment BUSS_ADJ_EUR =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, EUTA);
  private static final PeriodicSchedule SCHEDULE_EUR =
      PeriodicSchedule.of(START, END, FREQUENCY, BUSS_ADJ_EUR, StubConvention.NONE, RollConventions.NONE);
  private static final DaysAdjustment FIXING_OFFSET = DaysAdjustment.ofBusinessDays(-3, SAT_SUN);
  private static final DaysAdjustment PAYMENT_OFFSET = DaysAdjustment.ofBusinessDays(2, SAT_SUN);
  private static final ValueSchedule CAP = ValueSchedule.of(0.0125);
  private static final List<ValueStep> FLOOR_STEPS = new ArrayList<ValueStep>();
  private static final List<ValueStep> NOTIONAL_STEPS = new ArrayList<ValueStep>();
  static {
    FLOOR_STEPS.add(ValueStep.of(1, ValueAdjustment.ofReplace(0.02)));
    NOTIONAL_STEPS.add(ValueStep.of(1, ValueAdjustment.ofReplace(1.2e6)));
  }
  private static final ValueSchedule FLOOR = ValueSchedule.of(0.011, FLOOR_STEPS);
  private static final ValueSchedule NOTIONAL = ValueSchedule.of(1.0e6, NOTIONAL_STEPS);

  @Test
  public void test_builder_full() {
    CmsLeg test = CmsLeg.builder()
        .currency(GBP)
        .dayCount(ACT_365_ACTUAL)
        .fixingRelativeTo(FixingRelativeTo.PERIOD_END)
        .fixingDateOffset(FIXING_OFFSET)
        .paymentDateOffset(PAYMENT_OFFSET)
        .floorSchedule(FLOOR)
        .index(INDEX)
        .notional(NOTIONAL)
        .payReceive(PAY)
        .paymentSchedule(SCHEDULE)
        .build();
    assertThat(test.getPayReceive()).isEqualTo(PAY);
    assertThat(test.getCapSchedule().isPresent()).isFalse();
    assertThat(test.getFloorSchedule().get()).isEqualTo(FLOOR);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getNotional()).isEqualTo(NOTIONAL);
    assertThat(test.getDayCount()).isEqualTo(ACT_365_ACTUAL);
    assertThat(test.getStartDate()).isEqualTo(AdjustableDate.of(START, SCHEDULE.getBusinessDayAdjustment()));
    assertThat(test.getEndDate()).isEqualTo(SCHEDULE.calculatedEndDate());
    assertThat(test.getIndex()).isEqualTo(INDEX);
    assertThat(test.getPaymentSchedule()).isEqualTo(SCHEDULE);
    assertThat(test.getFixingRelativeTo()).isEqualTo(FixingRelativeTo.PERIOD_END);
    assertThat(test.getFixingDateOffset()).isEqualTo(FIXING_OFFSET);
    assertThat(test.getPaymentDateOffset()).isEqualTo(PAYMENT_OFFSET);
  }

  @Test
  public void test_builder_full_coupon() {
    CmsLeg test = CmsLeg.builder()
        .currency(GBP)
        .dayCount(ACT_365_ACTUAL)
        .fixingRelativeTo(FixingRelativeTo.PERIOD_END)
        .fixingDateOffset(FIXING_OFFSET)
        .paymentDateOffset(PAYMENT_OFFSET)
        .index(INDEX)
        .notional(NOTIONAL)
        .payReceive(PAY)
        .paymentSchedule(SCHEDULE)
        .build();
    assertThat(test.getPayReceive()).isEqualTo(PAY);
    assertThat(test.getCapSchedule().isPresent()).isFalse();
    assertThat(test.getFloorSchedule().isPresent()).isFalse();
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getNotional()).isEqualTo(NOTIONAL);
    assertThat(test.getDayCount()).isEqualTo(ACT_365_ACTUAL);
    assertThat(test.getStartDate()).isEqualTo(AdjustableDate.of(START, SCHEDULE.getBusinessDayAdjustment()));
    assertThat(test.getEndDate()).isEqualTo(SCHEDULE.calculatedEndDate());
    assertThat(test.getIndex()).isEqualTo(INDEX);
    assertThat(test.getPaymentSchedule()).isEqualTo(SCHEDULE);
    assertThat(test.getFixingRelativeTo()).isEqualTo(FixingRelativeTo.PERIOD_END);
    assertThat(test.getFixingDateOffset()).isEqualTo(FIXING_OFFSET);
    assertThat(test.getPaymentDateOffset()).isEqualTo(PAYMENT_OFFSET);
  }

  @Test
  public void test_builder_min() {
    CmsLeg test = sutCap();
    assertThat(test.getPayReceive()).isEqualTo(RECEIVE);
    assertThat(test.getCapSchedule().get()).isEqualTo(CAP);
    assertThat(test.getFloorSchedule().isPresent()).isFalse();
    assertThat(test.getCurrency()).isEqualTo(EUR_EURIBOR_6M.getCurrency());
    assertThat(test.getNotional()).isEqualTo(NOTIONAL);
    assertThat(test.getDayCount()).isEqualTo(EUR_EURIBOR_6M.getDayCount());
    assertThat(test.getStartDate()).isEqualTo(AdjustableDate.of(START, SCHEDULE_EUR.getBusinessDayAdjustment()));
    assertThat(test.getEndDate()).isEqualTo(SCHEDULE_EUR.calculatedEndDate());
    assertThat(test.getIndex()).isEqualTo(INDEX);
    assertThat(test.getPaymentSchedule()).isEqualTo(SCHEDULE_EUR);
    assertThat(test.getFixingRelativeTo()).isEqualTo(FixingRelativeTo.PERIOD_START);
    assertThat(test.getFixingDateOffset()).isEqualTo(EUR_EURIBOR_6M.getFixingDateOffset());
    assertThat(test.getPaymentDateOffset()).isEqualTo(DaysAdjustment.NONE);
  }

  @Test
  public void test_builder_min_coupon() {
    CmsLeg test = CmsLeg.builder()
        .index(INDEX)
        .notional(NOTIONAL)
        .payReceive(RECEIVE)
        .paymentSchedule(SCHEDULE_EUR)
        .build();
    assertThat(test.getPayReceive()).isEqualTo(RECEIVE);
    assertThat(test.getCapSchedule().isPresent()).isFalse();
    assertThat(test.getFloorSchedule().isPresent()).isFalse();
    assertThat(test.getCurrency()).isEqualTo(EUR_EURIBOR_6M.getCurrency());
    assertThat(test.getNotional()).isEqualTo(NOTIONAL);
    assertThat(test.getDayCount()).isEqualTo(EUR_EURIBOR_6M.getDayCount());
    assertThat(test.getStartDate()).isEqualTo(AdjustableDate.of(START, SCHEDULE_EUR.getBusinessDayAdjustment()));
    assertThat(test.getEndDate()).isEqualTo(SCHEDULE_EUR.calculatedEndDate());
    assertThat(test.getIndex()).isEqualTo(INDEX);
    assertThat(test.getPaymentSchedule()).isEqualTo(SCHEDULE_EUR);
    assertThat(test.getFixingRelativeTo()).isEqualTo(FixingRelativeTo.PERIOD_START);
    assertThat(test.getFixingDateOffset()).isEqualTo(EUR_EURIBOR_6M.getFixingDateOffset());
    assertThat(test.getPaymentDateOffset()).isEqualTo(DaysAdjustment.NONE);
  }

  @Test
  public void test_builder_fail() {
    // index is null
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CmsLeg.builder()
            .capSchedule(CAP)
            .notional(NOTIONAL)
            .payReceive(RECEIVE)
            .paymentSchedule(SCHEDULE_EUR)
            .build());
    // floorSchedule and capSchedule are present
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CmsLeg.builder()
            .capSchedule(CAP)
            .floorSchedule(FLOOR)
            .index(INDEX)
            .notional(NOTIONAL)
            .payReceive(RECEIVE)
            .paymentSchedule(SCHEDULE_EUR)
            .build());
    // stub is on
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CmsLeg
            .builder()
            .index(INDEX)
            .notional(NOTIONAL)
            .payReceive(RECEIVE)
            .paymentSchedule(
                PeriodicSchedule.of(START, END, FREQUENCY, BUSS_ADJ_EUR, StubConvention.SHORT_INITIAL, RollConventions.NONE))
            .build());
  }

  @Test
  public void test_resolve() {
    CmsLeg baseFloor = CmsLeg.builder()
        .floorSchedule(FLOOR)
        .index(INDEX)
        .notional(NOTIONAL)
        .payReceive(PAY)
        .paymentSchedule(SCHEDULE_EUR)
        .build();
    ResolvedCmsLeg resolvedFloor = baseFloor.resolve(REF_DATA);
    LocalDate end1 = LocalDate.of(2016, 10, 21);
    LocalDate fixing1 = EUR_EURIBOR_6M.calculateFixingFromEffective(START, REF_DATA);
    LocalDate fixing2 = EUR_EURIBOR_6M.calculateFixingFromEffective(end1, REF_DATA);
    LocalDate fixing3 = EUR_EURIBOR_6M.calculateFixingFromEffective(END, REF_DATA);
    LocalDate endDate = SCHEDULE_EUR.calculatedEndDate().adjusted(REF_DATA);

    CmsPeriod period1 = CmsPeriod.builder()
        .currency(EUR)
        .floorlet(FLOOR.getInitialValue())
        .notional(-NOTIONAL.getInitialValue())
        .index(INDEX)
        .startDate(START)
        .endDate(end1)
        .unadjustedStartDate(START)
        .unadjustedEndDate(end1)
        .fixingDate(fixing1)
        .paymentDate(end1)
        .yearFraction(EUR_EURIBOR_6M.getDayCount().yearFraction(START, end1))
        .dayCount(EUR_EURIBOR_6M.getDayCount())
        .underlyingSwap(createUnderlyingSwap(fixing1))
        .build();
    CmsPeriod period2 = CmsPeriod.builder()
        .currency(EUR)
        .floorlet(FLOOR.getSteps().get(0).getValue().getModifyingValue())
        .notional(-NOTIONAL.getSteps().get(0).getValue().getModifyingValue())
        .index(INDEX)
        .startDate(end1)
        .endDate(endDate)
        .unadjustedStartDate(end1)
        .unadjustedEndDate(END)
        .fixingDate(fixing2)
        .paymentDate(endDate)
        .yearFraction(EUR_EURIBOR_6M.getDayCount().yearFraction(end1, endDate))
        .dayCount(EUR_EURIBOR_6M.getDayCount())
        .underlyingSwap(createUnderlyingSwap(fixing2))
        .build();
    assertThat(resolvedFloor.getCurrency()).isEqualTo(EUR);
    assertThat(resolvedFloor.getStartDate()).isEqualTo(baseFloor.getStartDate().adjusted(REF_DATA));
    assertThat(resolvedFloor.getEndDate()).isEqualTo(baseFloor.getEndDate().adjusted(REF_DATA));
    assertThat(resolvedFloor.getIndex()).isEqualTo(INDEX);
    assertThat(resolvedFloor.getPayReceive()).isEqualTo(PAY);
    assertThat(resolvedFloor.getCmsPeriods()).hasSize(2);
    assertThat(resolvedFloor.getCmsPeriods().get(0)).isEqualTo(period1);
    assertThat(resolvedFloor.getCmsPeriods().get(1)).isEqualTo(period2);

    CmsLeg baseFloorEnd = CmsLeg.builder()
        .floorSchedule(FLOOR)
        .fixingRelativeTo(FixingRelativeTo.PERIOD_END)
        .index(INDEX)
        .notional(NOTIONAL)
        .payReceive(PAY)
        .paymentSchedule(SCHEDULE_EUR)
        .build();
    ResolvedCmsLeg resolvedFloorEnd = baseFloorEnd.resolve(REF_DATA);
    CmsPeriod period1End = CmsPeriod.builder()
        .currency(EUR)
        .floorlet(FLOOR.getInitialValue())
        .notional(-NOTIONAL.getInitialValue())
        .index(INDEX)
        .startDate(START)
        .endDate(end1)
        .unadjustedStartDate(START)
        .unadjustedEndDate(end1)
        .fixingDate(fixing2)
        .paymentDate(end1)
        .yearFraction(EUR_EURIBOR_6M.getDayCount().yearFraction(START, end1))
        .dayCount(EUR_EURIBOR_6M.getDayCount())
        .underlyingSwap(createUnderlyingSwap(fixing2))
        .build();
    CmsPeriod period2End = CmsPeriod.builder()
        .currency(EUR)
        .floorlet(FLOOR.getSteps().get(0).getValue().getModifyingValue())
        .notional(-NOTIONAL.getSteps().get(0).getValue().getModifyingValue())
        .index(INDEX)
        .startDate(end1)
        .endDate(endDate)
        .unadjustedStartDate(end1)
        .unadjustedEndDate(END)
        .fixingDate(fixing3)
        .paymentDate(endDate)
        .yearFraction(EUR_EURIBOR_6M.getDayCount().yearFraction(end1, endDate))
        .dayCount(EUR_EURIBOR_6M.getDayCount())
        .underlyingSwap(createUnderlyingSwap(fixing3))
        .build();
    assertThat(resolvedFloorEnd.getCurrency()).isEqualTo(EUR);
    assertThat(resolvedFloorEnd.getStartDate()).isEqualTo(baseFloor.getStartDate().adjusted(REF_DATA));
    assertThat(resolvedFloorEnd.getEndDate()).isEqualTo(baseFloor.getEndDate().adjusted(REF_DATA));
    assertThat(resolvedFloorEnd.getIndex()).isEqualTo(INDEX);
    assertThat(resolvedFloorEnd.getPayReceive()).isEqualTo(PAY);
    assertThat(resolvedFloorEnd.getCmsPeriods()).hasSize(2);
    assertThat(resolvedFloorEnd.getCmsPeriods().get(0)).isEqualTo(period1End);
    assertThat(resolvedFloorEnd.getCmsPeriods().get(1)).isEqualTo(period2End);

    CmsLeg baseCap = CmsLeg.builder()
        .index(INDEX)
        .capSchedule(CAP)
        .notional(NOTIONAL)
        .payReceive(PAY)
        .paymentSchedule(SCHEDULE_EUR)
        .paymentDateOffset(PAYMENT_OFFSET)
        .build();
    ResolvedCmsLeg resolvedCap = baseCap.resolve(REF_DATA);
    CmsPeriod periodCap1 = CmsPeriod.builder()
        .currency(EUR)
        .notional(-NOTIONAL.getInitialValue())
        .index(INDEX)
        .caplet(CAP.getInitialValue())
        .startDate(START)
        .endDate(end1)
        .unadjustedStartDate(START)
        .unadjustedEndDate(end1)
        .fixingDate(fixing1)
        .paymentDate(PAYMENT_OFFSET.adjust(end1, REF_DATA))
        .yearFraction(EUR_EURIBOR_6M.getDayCount().yearFraction(START, end1))
        .dayCount(EUR_EURIBOR_6M.getDayCount())
        .underlyingSwap(createUnderlyingSwap(fixing1))
        .build();
    CmsPeriod periodCap2 = CmsPeriod.builder()
        .currency(EUR)
        .notional(-NOTIONAL.getSteps().get(0).getValue().getModifyingValue())
        .index(INDEX)
        .caplet(CAP.getInitialValue())
        .startDate(end1)
        .endDate(endDate)
        .unadjustedStartDate(end1)
        .unadjustedEndDate(END)
        .fixingDate(fixing2)
        .paymentDate(PAYMENT_OFFSET.adjust(endDate, REF_DATA))
        .yearFraction(EUR_EURIBOR_6M.getDayCount().yearFraction(end1, endDate))
        .dayCount(EUR_EURIBOR_6M.getDayCount())
        .underlyingSwap(createUnderlyingSwap(fixing2))
        .build();
    assertThat(resolvedCap.getCurrency()).isEqualTo(EUR);
    assertThat(resolvedCap.getStartDate()).isEqualTo(baseCap.getStartDate().adjusted(REF_DATA));
    assertThat(resolvedCap.getEndDate()).isEqualTo(baseCap.getEndDate().adjusted(REF_DATA));
    assertThat(resolvedCap.getIndex()).isEqualTo(INDEX);
    assertThat(resolvedCap.getPayReceive()).isEqualTo(PAY);
    assertThat(resolvedCap.getCmsPeriods()).hasSize(2);
    assertThat(resolvedCap.getCmsPeriods().get(0)).isEqualTo(periodCap1);
    assertThat(resolvedCap.getCmsPeriods().get(1)).isEqualTo(periodCap2);
  }

  private ResolvedSwap createUnderlyingSwap(LocalDate fixingDate) {
    FixedFloatSwapConvention conv = INDEX.getTemplate().getConvention();
    LocalDate effectiveDate = conv.calculateSpotDateFromTradeDate(fixingDate, REF_DATA);
    LocalDate maturityDate = effectiveDate.plus(INDEX.getTemplate().getTenor());
    Swap swap = conv.toTrade(fixingDate, effectiveDate, maturityDate, BuySell.BUY, 1d, 1d).getProduct();
    return swap.resolve(REF_DATA);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(sutCap());
    coverBeanEquals(sutCap(), sutFloor());
  }

  @Test
  public void test_serialization() {
    assertSerialization(sutCap());
  }

  //-------------------------------------------------------------------------
  static CmsLeg sutCap() {
    return CmsLeg.builder()
        .capSchedule(CAP)
        .index(INDEX)
        .notional(NOTIONAL)
        .payReceive(RECEIVE)
        .paymentSchedule(SCHEDULE_EUR)
        .build();
  }

  static CmsLeg sutFloor() {
    return CmsLeg.builder()
        .floorSchedule(FLOOR)
        .index(SwapIndices.USD_LIBOR_1100_10Y)
        .notional(ValueSchedule.of(1.e6))
        .payReceive(PAY)
        .paymentSchedule(SCHEDULE)
        .fixingRelativeTo(FixingRelativeTo.PERIOD_END)
        .fixingDateOffset(FIXING_OFFSET)
        .paymentDateOffset(FIXING_OFFSET)
        .dayCount(ACT_365_ACTUAL)
        .build();
  }

}
