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
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.product.common.LongShort;

/**
 * Test {@link ResolvedFixedCouponBondOption}.
 */
public class ResolvedFixedCouponBondOptionTest {

  private static final LocalDate EXPIRY_DATE_LD = date(2022, 3, 16);
  private static final LocalTime EXPIRY_TIME = LocalTime.of(11, 0);
  private static final ZoneId EXPIRY_ZONE = ZoneId.of("Europe/Brussels");
  private static final ZonedDateTime EXPIRY = EXPIRY_DATE_LD.atTime(EXPIRY_TIME).atZone(EXPIRY_ZONE);
  private static final LocalDate SETTLEMENT_DATE_LD = date(2022, 3, 18);
  private static final double QUANTITY_CALL = 12;
  private static final double QUANTITY_PUT = -23;
  private static final double STRIKE = 1.2345;
  private static final ResolvedFixedCouponBondSettlement SETTLE =
      ResolvedFixedCouponBondSettlement.of(SETTLEMENT_DATE_LD, STRIKE);
  private static final ResolvedFixedCouponBond PRODUCT = ResolvedFixedCouponBondTest.sut();

  //-------------------------------------------------------------------------
  @Test
  public void builder() {
    ResolvedFixedCouponBondOption test = ResolvedFixedCouponBondOption.builder()
        .longShort(LONG)
        .underlying(PRODUCT)
        .expiry(EXPIRY)
        .quantity(QUANTITY_CALL)
        .settlement(SETTLE)
        .build();
    assertThat(test.getLongShort()).isEqualTo(LONG);
    assertThat(test.getUnderlying()).isEqualTo(PRODUCT);
    assertThat(test.getExpiry()).isEqualTo(EXPIRY);
    assertThat(test.getSettlement()).isEqualTo(SETTLE);
  }

  @Test
  public void dateorder() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ResolvedFixedCouponBondOption.builder()
            .longShort(LONG)
            .underlying(PRODUCT)
            .expiry(SETTLEMENT_DATE_LD.atTime(EXPIRY_TIME).atZone(EXPIRY_ZONE))
            .quantity(QUANTITY_CALL)
            .settlement(ResolvedFixedCouponBondSettlement.of(EXPIRY_DATE_LD, STRIKE))
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ResolvedFixedCouponBondOption test = ResolvedFixedCouponBondOption.builder()
        .longShort(LONG)
        .underlying(PRODUCT)
        .expiry(EXPIRY)
        .quantity(QUANTITY_CALL)
        .settlement(SETTLE)
        .build();
    coverImmutableBean(test);
    ResolvedFixedCouponBond product2 = ResolvedFixedCouponBondTest.sut2();
    ResolvedFixedCouponBondOption test2 = ResolvedFixedCouponBondOption.builder()
        .longShort(LongShort.SHORT)
        .underlying(product2)
        .expiry(EXPIRY.plusDays(7))
        .quantity(QUANTITY_PUT)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLEMENT_DATE_LD.plusDays(7), STRIKE + 0.01))
        .build();
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    ResolvedFixedCouponBondOption test = ResolvedFixedCouponBondOption.builder()
        .longShort(LONG)
        .underlying(PRODUCT)
        .expiry(EXPIRY)
        .quantity(QUANTITY_CALL)
        .settlement(SETTLE)
        .build();
    assertSerialization(test);
  }

}
