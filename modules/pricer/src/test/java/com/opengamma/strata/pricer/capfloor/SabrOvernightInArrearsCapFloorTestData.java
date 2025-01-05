/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.index.OvernightIndices.EUR_ESTR;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static com.opengamma.strata.product.common.PayReceive.PAY;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.PeriodAdditionConventions;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.date.TenorAdjustment;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.ImmutableIborIndex;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeriesBuilder;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.capfloor.OvernightInArrearsCapFloorLeg;
import com.opengamma.strata.product.capfloor.ResolvedOvernightInArrearsCapFloor;
import com.opengamma.strata.product.capfloor.ResolvedOvernightInArrearsCapFloorLeg;
import com.opengamma.strata.product.capfloor.ResolvedOvernightInArrearsCapFloorTrade;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.OvernightRateCalculation;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;

/**
 * Data for testing overnight rate in arrears cap/floor.
 */
public class SabrOvernightInArrearsCapFloorTestData {

  private static final ZonedDateTime VALUATION = dateUtc(2021, 12, 20);
  private static final ZonedDateTime VALUATION_AFTER = dateUtc(2023, 8, 18);
  private static final ZonedDateTime VALUATION_PAY = dateUtc(2024, 6, 24);
  // reference data
  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final IborIndex EUR_ESTRTERM_3M = ImmutableIborIndex.builder()
      .name("EUR-ESTRTERM-3M")
      .currency(EUR)
      .dayCount(DayCounts.ACT_360)
      .fixingCalendar(EUTA)
      .fixingTime(LocalTime.of(11, 0))
      .fixingZone(ZoneId.of("Europe/Brussels"))
      .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, EUTA))
      .effectiveDateOffset(DaysAdjustment.ofBusinessDays(2, EUTA))
      .maturityDateOffset(TenorAdjustment.of(
          Tenor.TENOR_3M,
          PeriodAdditionConventions.LAST_BUSINESS_DAY,
          BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, EUTA)))
      .build();
  // cap leg and floor leg
  private static final HolidayCalendar EUTA_IMPL = REF_DATA.getValue(EUTA);
  private static final LocalDate START_DATE = LocalDate.of(2022, 6, 22);
  private static final LocalDate END_DATE = LocalDate.of(2027, 6, 22);
  static final double NOTIONAL_VALUE = 1_000_000.0d;
  private static final ValueSchedule NOTIONAL = ValueSchedule.of(NOTIONAL_VALUE);
  private static final double STRIKE = 0.0155;
  private static final ValueSchedule STRIKE_SCHEDULE = ValueSchedule.of(STRIKE);
  private static final PeriodicSchedule PAY_SCHEDULE = PeriodicSchedule.of(
      START_DATE,
      END_DATE,
      Frequency.P12M,
      BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, EUTA),
      StubConvention.NONE,
      RollConventions.NONE);
  static final ResolvedOvernightInArrearsCapFloorLeg CAP_LEG = OvernightInArrearsCapFloorLeg.builder()
      .capSchedule(STRIKE_SCHEDULE)
      .calculation(OvernightRateCalculation.of(EUR_ESTR))
      .currency(EUR)
      .notional(NOTIONAL)
      .paymentSchedule(PAY_SCHEDULE)
      .payReceive(PayReceive.RECEIVE)
      .build().resolve(REF_DATA);
  static final ResolvedOvernightInArrearsCapFloorLeg FLOOR_LEG = OvernightInArrearsCapFloorLeg.builder()
      .floorSchedule(STRIKE_SCHEDULE)
      .calculation(OvernightRateCalculation.of(EUR_ESTR))
      .currency(EUR)
      .notional(NOTIONAL)
      .paymentSchedule(PAY_SCHEDULE)
      .payReceive(PayReceive.PAY)
      .build().resolve(REF_DATA);
  // caps
  static final ResolvedSwapLeg PAY_LEG = IborCapFloorDataSet.createFixedPayLeg(
      EUR_ESTRTERM_3M,
      START_DATE,
      END_DATE,
      0.0015,
      NOTIONAL_VALUE,
      PAY);
  static final ResolvedOvernightInArrearsCapFloor CAP_TWO_LEGS =
      ResolvedOvernightInArrearsCapFloor.of(CAP_LEG, PAY_LEG);
  static final ResolvedOvernightInArrearsCapFloor CAP_ONE_LEG = ResolvedOvernightInArrearsCapFloor.of(CAP_LEG);
  // cap trades
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(VALUATION.toLocalDate()).build();
  static final Payment PREMIUM = Payment.of(EUR, -NOTIONAL_VALUE * 0.19, VALUATION.toLocalDate());
  static final ResolvedOvernightInArrearsCapFloorTrade TRADE = ResolvedOvernightInArrearsCapFloorTrade.builder()
      .product(CAP_ONE_LEG)
      .build();
  static final ResolvedOvernightInArrearsCapFloorTrade TRADE_PAYLEG = ResolvedOvernightInArrearsCapFloorTrade.builder()
      .product(CAP_TWO_LEGS)
      .info(TRADE_INFO)
      .build();
  static final ResolvedOvernightInArrearsCapFloorTrade TRADE_PREMIUM = ResolvedOvernightInArrearsCapFloorTrade.builder()
      .product(CAP_ONE_LEG)
      .premium(PREMIUM)
      .info(TradeInfo.empty())
      .build();
  // valuation date before start date
  static final ImmutableRatesProvider RATES = IborCapletFloorletSabrRateVolatilityDataSet.getRatesProvider(
      VALUATION.toLocalDate(),
      EUR_ESTRTERM_3M,
      LocalDateDoubleTimeSeries.empty());
  static final SabrParametersIborCapletFloorletVolatilities VOLS = IborCapletFloorletSabrRateVolatilityDataSet
      .getVolatilities(VALUATION, EUR_ESTRTERM_3M);
  // valuation datas after start data
  private static final LocalDateDoubleTimeSeries TS_ESTR_AFTER_END;
  static {
    LocalDateDoubleTimeSeriesBuilder builder = LocalDateDoubleTimeSeries.builder();
    LocalDate currentDate = START_DATE;
    while (currentDate.isBefore(VALUATION_PAY.toLocalDate())) {
      builder.put(currentDate, 0.0150);
      currentDate = EUTA_IMPL.next(currentDate);
    }
    TS_ESTR_AFTER_END = builder.build();
  }
  static final ImmutableRatesProvider RATES_AFTER =
      IborCapletFloorletSabrRateVolatilityDataSet.getRatesProvider(
              VALUATION_AFTER.toLocalDate(),
              EUR_ESTRTERM_3M,
              LocalDateDoubleTimeSeries.empty()).toBuilder()
          .timeSeries(EUR_ESTR, TS_ESTR_AFTER_END)
          .build();
  static final SabrParametersIborCapletFloorletVolatilities VOLS_AFTER =
      IborCapletFloorletSabrRateVolatilityDataSet.getVolatilities(VALUATION_AFTER, EUR_ESTRTERM_3M);
  static final ImmutableRatesProvider RATES_PAY =
      IborCapletFloorletSabrRateVolatilityDataSet.getRatesProvider(
              VALUATION_PAY.toLocalDate(),
              EUR_ESTRTERM_3M,
              LocalDateDoubleTimeSeries.empty()).toBuilder()
          .timeSeries(EUR_ESTR, TS_ESTR_AFTER_END)
          .build();
  static final SabrParametersIborCapletFloorletVolatilities VOLS_PAY =
      IborCapletFloorletSabrRateVolatilityDataSet.getVolatilities(VALUATION_PAY, EUR_ESTRTERM_3M);

  private SabrOvernightInArrearsCapFloorTestData() {
  }

}
