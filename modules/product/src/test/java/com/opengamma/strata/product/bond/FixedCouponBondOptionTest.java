/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.LongShort.LONG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.product.common.LongShort;

/**
 * Test {@link FixedCouponBondOption}.
 */
public class FixedCouponBondOptionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final BusinessDayAdjustment BDA =
      BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, HolidayCalendarIds.EUTA);
  private static final LocalDate EXPIRY_DATE_LD = date(2022, 3, 16);
  private static final AdjustableDate EXPIRY_DATE = AdjustableDate.of(EXPIRY_DATE_LD, BDA);
  private static final LocalTime EXPIRY_TIME = LocalTime.of(11, 0);
  private static final ZoneId EXPIRY_ZONE = ZoneId.of("Europe/Brussels");
  private static final LocalDate SETTLEMENT_DATE_LD = date(2022, 3, 18);
  private static final AdjustableDate SETTLEMENT_DATE = AdjustableDate.of(SETTLEMENT_DATE_LD, BDA);
  private static final double QUANTITY_CALL = 12;
  private static final double QUANTITY_PUT = -23;
  private static final double STRIKE = 1.2345;
  private static final FixedCouponBond PRODUCT = FixedCouponBondTest.sut();
  private static final FixedCouponBond PRODUCT2 = FixedCouponBondTest.sut2();
  private static final FixedCouponBondOption OPTION_LONG_CALL = FixedCouponBondOption.builder()
      .longShort(LONG)
      .underlying(PRODUCT)
      .expiryDate(EXPIRY_DATE)
      .expiryTime(EXPIRY_TIME)
      .expiryZone(EXPIRY_ZONE)
      .quantity(QUANTITY_CALL)
      .cleanStrikePrice(STRIKE)
      .settlementDate(SETTLEMENT_DATE)
      .build();

  //-------------------------------------------------------------------------
  @Test
  public void builder() {
    FixedCouponBondOption test = FixedCouponBondOption.builder()
        .longShort(LONG)
        .underlying(PRODUCT)
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .quantity(QUANTITY_CALL)
        .cleanStrikePrice(STRIKE)
        .settlementDate(SETTLEMENT_DATE)
        .build();
    assertThat(test.getLongShort()).isEqualTo(LONG);
    assertThat(test.getUnderlying()).isEqualTo(PRODUCT);
    assertThat(test.getExpiryDate()).isEqualTo(EXPIRY_DATE);
    assertThat(test.getExpiryTime()).isEqualTo(EXPIRY_TIME);
    assertThat(test.getExpiryZone()).isEqualTo(EXPIRY_ZONE);
    assertThat(test.getQuantity()).isEqualTo(QUANTITY_CALL);
    assertThat(test.getCleanStrikePrice()).isEqualTo(STRIKE);
    assertThat(test.getSettlementDate()).isEqualTo(SETTLEMENT_DATE);
  }

  @Test
  public void dateorder() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FixedCouponBondOption.builder()
            .longShort(LONG)
            .underlying(PRODUCT)
            .expiryDate(SETTLEMENT_DATE)
            .expiryTime(EXPIRY_TIME)
            .expiryZone(EXPIRY_ZONE)
            .quantity(QUANTITY_CALL)
            .cleanStrikePrice(STRIKE)
            .settlementDate(EXPIRY_DATE)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void resolve() {
    FixedCouponBondOption test = OPTION_LONG_CALL;
    ResolvedFixedCouponBondOption resolved = test.resolve(REF_DATA);
    ResolvedFixedCouponBondOption expected = ResolvedFixedCouponBondOption.builder()
        .longShort(LONG)
        .underlying(PRODUCT.resolve(REF_DATA))
        .expiry(EXPIRY_DATE.adjusted(REF_DATA).atTime(EXPIRY_TIME).atZone(EXPIRY_ZONE))
        .quantity(QUANTITY_CALL)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLEMENT_DATE.adjusted(REF_DATA), STRIKE))
        .build();
    assertThat(resolved).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(OPTION_LONG_CALL);
    AdjustableDate expiryDate2 = AdjustableDate.of(EXPIRY_DATE_LD.plusDays(7), BDA);
    AdjustableDate settlementDate2 = AdjustableDate.of(SETTLEMENT_DATE_LD.plusDays(7), BDA);
    FixedCouponBondOption option2 = FixedCouponBondOption.builder()
        .longShort(LongShort.SHORT)
        .underlying(PRODUCT2)
        .expiryDate(expiryDate2)
        .expiryTime(LocalTime.of(12, 0))
        .expiryZone(ZoneId.of("America/New_York"))
        .quantity(QUANTITY_PUT)
        .cleanStrikePrice(2.1345)
        .settlementDate(settlementDate2)
        .build();
    coverBeanEquals(OPTION_LONG_CALL, option2);
  }

  @Test
  public void test_serialization() {
    assertSerialization(OPTION_LONG_CALL);
  }

}
