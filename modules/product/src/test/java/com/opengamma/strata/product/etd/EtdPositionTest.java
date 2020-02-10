/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.product.PortfolioItemInfo;
import com.opengamma.strata.product.PositionInfo;

/**
 * Test {@link EtdPosition}.
 */
public class EtdPositionTest {

  private static final PositionInfo POSITION_INFO = PositionInfo.of(StandardId.of("A", "B"));
  private static final EtdFutureSecurity SECURITY = EtdFutureSecurityTest.sut();

  @Test
  public void test_defaultMethods() {
    class TestEtd implements EtdPosition {
      private final double longQuantity;
      private final double shortQuantity;

      TestEtd(double longQuantity, double shortQuantity) {
        this.longQuantity = longQuantity;
        this.shortQuantity = shortQuantity;
      }

      @Override
      public PositionInfo getInfo() {
        return POSITION_INFO;
      }

      @Override
      public EtdSecurity getSecurity() {
        return SECURITY;
      }

      @Override
      public double getQuantity() {
        return longQuantity - shortQuantity;
      }

      @Override
      public double getLongQuantity() {
        return longQuantity;
      }

      @Override
      public double getShortQuantity() {
        return shortQuantity;
      }

      @Override
      public EtdPosition withInfo(PortfolioItemInfo info) {
        return this;
      }

      @Override
      public EtdPosition withQuantity(double netQuantity) {
        double longQuantity = netQuantity >= 0 ? netQuantity : 0;
        double shortQuantity = netQuantity >= 0 ? 0 : -netQuantity;
        return new TestEtd(longQuantity, shortQuantity);
      }
    }

    EtdPosition test = new TestEtd(3000, 2000);
    assertThat(test.getCurrency()).isEqualTo(SECURITY.getCurrency());
    assertThat(test.getType()).isEqualTo(SECURITY.getType());
    assertThat(test.getSecurityId()).isEqualTo(SECURITY.getSecurityId());
    assertThat(test.getQuantity()).isEqualTo(1000d);
    assertThat(test.getLongQuantity()).isEqualTo(3000d);
    assertThat(test.getShortQuantity()).isEqualTo(2000d);
    assertThat(test.withQuantity(129).getQuantity()).isEqualTo(129d);
    assertThat(test.withQuantity(-129).getQuantity()).isEqualTo(-129d);
    assertThat(test.withQuantities(300, 200).getQuantity()).isEqualTo(100d);
    assertThat(test.withQuantities(300, 200).getLongQuantity()).isEqualTo(100d);
    assertThat(test.withQuantities(300, 200).getShortQuantity()).isEqualTo(0d);
  }

}
