/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.date.DayCounts.NL_365;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.basics.index.PriceIndices.US_CPI_U;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.bond.CapitalIndexedBondYieldConvention.GB_IL_FLOAT;
import static com.opengamma.strata.product.bond.CapitalIndexedBondYieldConvention.US_IL_REAL;
import static com.opengamma.strata.product.swap.PriceIndexCalculationMethod.INTERPOLATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.rate.RateComputation;
import com.opengamma.strata.product.swap.InflationRateCalculation;

/**
 * Test {@link ResolvedCapitalIndexedBond}. 
 * <p>
 * The accrued interest method is test in the pricer test.
 */
public class ResolvedCapitalIndexedBondTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LegalEntityId LEGAL_ENTITY = LegalEntityId.of("OG-Ticker", "US-Govt");
  private static final double COUPON = 0.01;
  private static final InflationRateCalculation RATE_CALC = InflationRateCalculation.builder()
      .gearing(ValueSchedule.of(COUPON))
      .index(US_CPI_U)
      .lag(Period.ofMonths(3))
      .indexCalculationMethod(INTERPOLATED)
      .firstIndexValue(198.475)
      .build();
  private static final double NOTIONAL = 10_000_000d;
  private static final BusinessDayAdjustment SCHEDULE_ADJ =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, USNY);
  private static final DaysAdjustment SETTLE_OFFSET = DaysAdjustment.ofBusinessDays(2, USNY);

  private static final CapitalIndexedBondPaymentPeriod[] PERIODIC = new CapitalIndexedBondPaymentPeriod[4];
  static {
    LocalDate[] unAdjDates = new LocalDate[] {LocalDate.of(2008, 1, 13), LocalDate.of(2008, 7, 13),
        LocalDate.of(2009, 1, 13), LocalDate.of(2009, 7, 13), LocalDate.of(2010, 1, 13)};
    for (int i = 0; i < 4; ++i) {
      LocalDate start = SCHEDULE_ADJ.adjust(unAdjDates[i], REF_DATA);
      LocalDate end = SCHEDULE_ADJ.adjust(unAdjDates[i + 1], REF_DATA);
      RateComputation rateComputation = RATE_CALC.createRateComputation(end);
      PERIODIC[i] = CapitalIndexedBondPaymentPeriod.builder()
          .currency(USD)
          .startDate(start)
          .endDate(end)
          .unadjustedStartDate(unAdjDates[i])
          .unadjustedEndDate(unAdjDates[i + 1])
          .detachmentDate(end)
          .realCoupon(COUPON)
          .rateComputation(rateComputation)
          .notional(NOTIONAL)
          .build();
    }
  }
  private static final CapitalIndexedBondPaymentPeriod NOMINAL =
      PERIODIC[3].withUnitCoupon(PERIODIC[0].getStartDate(), PERIODIC[0].getUnadjustedStartDate());

  @Test
  public void test_builder() {
    ResolvedCapitalIndexedBond test = sut();
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getDayCount()).isEqualTo(ACT_ACT_ISDA);
    assertThat(test.getStartDate()).isEqualTo(PERIODIC[0].getStartDate());
    assertThat(test.getEndDate()).isEqualTo(PERIODIC[3].getEndDate());
    assertThat(test.getUnadjustedStartDate()).isEqualTo(PERIODIC[0].getUnadjustedStartDate());
    assertThat(test.getUnadjustedEndDate()).isEqualTo(PERIODIC[3].getUnadjustedEndDate());
    assertThat(test.getLegalEntityId()).isEqualTo(LEGAL_ENTITY);
    assertThat(test.getNominalPayment()).isEqualTo(NOMINAL);
    assertThat(test.getNotional()).isEqualTo(NOTIONAL);
    assertThat(test.getPeriodicPayments().toArray()).isEqualTo(PERIODIC);
    assertThat(test.getSettlementDateOffset()).isEqualTo(SETTLE_OFFSET);
    assertThat(test.getYieldConvention()).isEqualTo(US_IL_REAL);
    assertThat(test.hasExCouponPeriod()).isFalse();
    assertThat(test.getFirstIndexValue()).isEqualTo(RATE_CALC.getFirstIndexValue().getAsDouble());
    assertThat(test.findPeriod(PERIODIC[0].getUnadjustedStartDate())).isEqualTo(Optional.of(test.getPeriodicPayments().get(0)));
    assertThat(test.findPeriod(LocalDate.MIN)).isEqualTo(Optional.empty());
    assertThat(test.findPeriodIndex(PERIODIC[0].getUnadjustedStartDate())).isEqualTo(OptionalInt.of(0));
    assertThat(test.findPeriodIndex(PERIODIC[1].getUnadjustedStartDate())).isEqualTo(OptionalInt.of(1));
    assertThat(test.findPeriodIndex(LocalDate.MIN)).isEqualTo(OptionalInt.empty());
    assertThat(test.calculateSettlementDateFromValuation(date(2015, 6, 30), REF_DATA))
        .isEqualTo(SETTLE_OFFSET.adjust(date(2015, 6, 30), REF_DATA));
  }

  @Test
  public void test_builder_fail() {
    CapitalIndexedBondPaymentPeriod period = CapitalIndexedBondPaymentPeriod.builder()
        .startDate(PERIODIC[2].getStartDate())
        .endDate(PERIODIC[2].getEndDate())
        .currency(GBP)
        .notional(NOTIONAL)
        .rateComputation(PERIODIC[2].getRateComputation())
        .realCoupon(COUPON)
        .build();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ResolvedCapitalIndexedBond.builder()
            .dayCount(ACT_ACT_ISDA)
            .legalEntityId(LEGAL_ENTITY)
            .nominalPayment(NOMINAL)
            .periodicPayments(PERIODIC[0], PERIODIC[1], period, PERIODIC[3])
            .settlementDateOffset(SETTLE_OFFSET)
            .yieldConvention(US_IL_REAL)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_yearFraction_scheduleInfo() {
    ResolvedCapitalIndexedBond base = sut();
    CapitalIndexedBondPaymentPeriod period = base.getPeriodicPayments().get(0);
    AtomicBoolean eom = new AtomicBoolean(false);
    DayCount dc = new DayCount() {
      @Override
      public double yearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
        assertThat(scheduleInfo.getStartDate()).isEqualTo(base.getUnadjustedStartDate());
        assertThat(scheduleInfo.getEndDate()).isEqualTo(base.getUnadjustedEndDate());
        assertThat(scheduleInfo.getPeriodEndDate(firstDate)).isEqualTo(period.getUnadjustedEndDate());
        assertThat(scheduleInfo.getFrequency()).isEqualTo(base.getFrequency());
        assertThat(scheduleInfo.isEndOfMonthConvention()).isEqualTo(eom.get());
        return 0.5;
      }

      @Override
      public int days(LocalDate firstDate, LocalDate secondDate) {
        return 182;
      }

      @Override
      public String getName() {
        return "";
      }
    };
    ResolvedCapitalIndexedBond test = base.toBuilder().dayCount(dc).build();
    assertThat(test.yearFraction(period.getUnadjustedStartDate(), period.getUnadjustedEndDate())).isEqualTo(0.5);
    // test with EOM=true
    ResolvedCapitalIndexedBond test2 = test.toBuilder().rollConvention(RollConventions.EOM).build();
    eom.set(true);
    assertThat(test2.yearFraction(period.getUnadjustedStartDate(), period.getUnadjustedEndDate())).isEqualTo(0.5);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  @Test
  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static ResolvedCapitalIndexedBond sut() {
    return ResolvedCapitalIndexedBond.builder()
        .securityId(CapitalIndexedBondTest.sut().getSecurityId())
        .dayCount(ACT_ACT_ISDA)
        .legalEntityId(LEGAL_ENTITY)
        .nominalPayment(NOMINAL)
        .periodicPayments(PERIODIC)
        .frequency(CapitalIndexedBondTest.sut().getAccrualSchedule().getFrequency())
        .rollConvention(CapitalIndexedBondTest.sut().getAccrualSchedule().calculatedRollConvention())
        .settlementDateOffset(SETTLE_OFFSET)
        .yieldConvention(US_IL_REAL)
        .rateCalculation(RATE_CALC)
        .build();
  }

  static ResolvedCapitalIndexedBond sut2() {
    return ResolvedCapitalIndexedBond.builder()
        .securityId(CapitalIndexedBondTest.sut2().getSecurityId())
        .dayCount(NL_365)
        .legalEntityId(LegalEntityId.of("OG-Ticker", "US-Govt1"))
        .nominalPayment(PERIODIC[1].withUnitCoupon(PERIODIC[0].getStartDate(), PERIODIC[0].getUnadjustedStartDate()))
        .periodicPayments(PERIODIC[0], PERIODIC[1])
        .frequency(CapitalIndexedBondTest.sut2().getAccrualSchedule().getFrequency())
        .rollConvention(CapitalIndexedBondTest.sut2().getAccrualSchedule().calculatedRollConvention())
        .settlementDateOffset(DaysAdjustment.ofBusinessDays(3, GBLO))
        .yieldConvention(GB_IL_FLOAT)
        .rateCalculation(CapitalIndexedBondTest.sut2().getRateCalculation())
        .build();
  }

}
