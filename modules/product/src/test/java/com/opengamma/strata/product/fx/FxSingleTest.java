/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Optional;

import org.joda.beans.ser.JodaBeanSer;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;

/**
 * Test {@link FxSingle}.
 */
@Test
public class FxSingleTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1_000);
  private static final CurrencyAmount GBP_M1000 = CurrencyAmount.of(GBP, -1_000);
  private static final CurrencyAmount USD_P1600 = CurrencyAmount.of(USD, 1_600);
  private static final CurrencyAmount USD_M1600 = CurrencyAmount.of(USD, -1_600);
  private static final CurrencyAmount EUR_P1600 = CurrencyAmount.of(EUR, 1_800);
  private static final LocalDate DATE_2015_06_29 = date(2015, 6, 29);
  private static final LocalDate DATE_2015_06_30 = date(2015, 6, 30);
  private static final BusinessDayAdjustment BDA = BusinessDayAdjustment.of(FOLLOWING, GBLO);

  //-------------------------------------------------------------------------
  public void test_of_rightOrderPayments() {
    FxSingle test = FxSingle.of(Payment.of(GBP_P1000, DATE_2015_06_30), Payment.of(USD_M1600, DATE_2015_06_29), BDA);
    assertEquals(test.getBaseCurrencyPayment(), Payment.of(GBP_P1000, DATE_2015_06_30));
    assertEquals(test.getCounterCurrencyPayment(), Payment.of(USD_M1600, DATE_2015_06_29));
    assertEquals(test.getBaseCurrencyAmount(), GBP_P1000);
    assertEquals(test.getCounterCurrencyAmount(), USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getPaymentDateAdjustment(), Optional.of(BDA));
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getPayCurrencyAmount(), USD_M1600);
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
    assertEquals(test.isCrossCurrency(), true);
    assertEquals(test.allPaymentCurrencies(), ImmutableSet.of(GBP, USD));
    assertEquals(test.allCurrencies(), ImmutableSet.of(GBP, USD));
  }

  public void test_of_switchOrderPayments() {
    FxSingle test = FxSingle.of(Payment.of(USD_M1600, DATE_2015_06_30), Payment.of(GBP_P1000, DATE_2015_06_30));
    assertEquals(test.getBaseCurrencyAmount(), GBP_P1000);
    assertEquals(test.getCounterCurrencyAmount(), USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getPayCurrencyAmount(), USD_M1600);
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  //-------------------------------------------------------------------------
  public void test_of_rightOrder() {
    FxSingle test = sut();
    assertEquals(test.getBaseCurrencyPayment(), Payment.of(GBP_P1000, DATE_2015_06_30));
    assertEquals(test.getCounterCurrencyPayment(), Payment.of(USD_M1600, DATE_2015_06_30));
    assertEquals(test.getBaseCurrencyAmount(), GBP_P1000);
    assertEquals(test.getCounterCurrencyAmount(), USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getPaymentDateAdjustment(), Optional.empty());
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getPayCurrencyAmount(), USD_M1600);
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
    assertEquals(test.isCrossCurrency(), true);
    assertEquals(test.allPaymentCurrencies(), ImmutableSet.of(GBP, USD));
    assertEquals(test.allCurrencies(), ImmutableSet.of(GBP, USD));
  }

  public void test_of_switchOrder() {
    FxSingle test = FxSingle.of(USD_M1600, GBP_P1000, DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyAmount(), GBP_P1000);
    assertEquals(test.getCounterCurrencyAmount(), USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  public void test_of_bothZero() {
    FxSingle test = FxSingle.of(CurrencyAmount.zero(GBP), CurrencyAmount.zero(USD), DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyAmount(), CurrencyAmount.zero(GBP));
    assertEquals(test.getCounterCurrencyAmount(), CurrencyAmount.zero(USD));
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getPayCurrencyAmount(), CurrencyAmount.zero(GBP));
    assertEquals(test.getReceiveCurrencyAmount(), CurrencyAmount.zero(USD));
  }

  public void test_of_positiveNegative() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxSingle.of(GBP_P1000, USD_P1600, DATE_2015_06_30));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxSingle.of(GBP_M1000, USD_M1600, DATE_2015_06_30));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxSingle.of(CurrencyAmount.zero(GBP), USD_M1600, DATE_2015_06_30));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxSingle.of(CurrencyAmount.zero(GBP), USD_P1600, DATE_2015_06_30));
  }

  public void test_of_sameCurrency() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxSingle.of(GBP_P1000, GBP_M1000, DATE_2015_06_30));
  }

  public void test_of_withAdjustment() {
    FxSingle test = FxSingle.of(GBP_P1000, USD_M1600, DATE_2015_06_30, BDA);
    assertEquals(test.getBaseCurrencyAmount(), GBP_P1000);
    assertEquals(test.getCounterCurrencyAmount(), USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getPaymentDateAdjustment(), Optional.of(BDA));
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  //-------------------------------------------------------------------------
  public void test_of_rate_rightOrder() {
    FxSingle test = FxSingle.of(GBP_P1000, FxRate.of(GBP, USD, 1.6d), DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyAmount(), GBP_P1000);
    assertEquals(test.getCounterCurrencyAmount(), USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getPaymentDateAdjustment(), Optional.empty());
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  public void test_of_rate_switchOrder() {
    FxSingle test = FxSingle.of(USD_M1600, FxRate.of(USD, GBP, 1d / 1.6d), DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyAmount(), GBP_P1000);
    assertEquals(test.getCounterCurrencyAmount(), USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  public void test_of_rate_bothZero() {
    FxSingle test = FxSingle.of(CurrencyAmount.zero(GBP), FxRate.of(USD, GBP, 1.6d), DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyAmount(), CurrencyAmount.zero(GBP));
    assertEquals(test.getCounterCurrencyAmount().getAmount(), CurrencyAmount.zero(USD).getAmount(), 1e-12);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), CurrencyAmount.of(USD, 0d));
  }

  public void test_of_rate_wrongCurrency() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxSingle.of(GBP_P1000, FxRate.of(USD, EUR, 1.45d), DATE_2015_06_30));
  }

  public void test_of_rate_withAdjustment() {
    FxSingle test = FxSingle.of(GBP_P1000, FxRate.of(GBP, USD, 1.6d), DATE_2015_06_30, BDA);
    assertEquals(test.getBaseCurrencyAmount(), GBP_P1000);
    assertEquals(test.getCounterCurrencyAmount(), USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getPaymentDateAdjustment(), Optional.of(BDA));
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  //-------------------------------------------------------------------------
  public void test_builder_rightOrder() {
    FxSingle test = FxSingle.meta().builder()
        .set(FxSingle.meta().baseCurrencyPayment(), Payment.of(GBP_P1000, DATE_2015_06_30))
        .set(FxSingle.meta().counterCurrencyPayment(), Payment.of(USD_M1600, DATE_2015_06_30))
        .build();
    assertEquals(test.getBaseCurrencyAmount(), GBP_P1000);
    assertEquals(test.getCounterCurrencyAmount(), USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  public void test_builder_switchOrder() {
    FxSingle test = FxSingle.meta().builder()
        .set(FxSingle.meta().baseCurrencyPayment(), Payment.of(USD_M1600, DATE_2015_06_30))
        .set(FxSingle.meta().counterCurrencyPayment(), Payment.of(GBP_P1000, DATE_2015_06_30))
        .build();
    assertEquals(test.getBaseCurrencyAmount(), GBP_P1000);
    assertEquals(test.getCounterCurrencyAmount(), USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  public void test_builder_bothPositive() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxSingle.meta().builder()
        .set(FxSingle.meta().baseCurrencyPayment(), Payment.of(GBP_P1000, DATE_2015_06_30))
        .set(FxSingle.meta().counterCurrencyPayment(), Payment.of(USD_P1600, DATE_2015_06_30))
        .build());
  }

  public void test_builder_bothNegative() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxSingle.meta().builder()
        .set(FxSingle.meta().baseCurrencyPayment(), Payment.of(GBP_M1000, DATE_2015_06_30))
        .set(FxSingle.meta().counterCurrencyPayment(), Payment.of(USD_M1600, DATE_2015_06_30))
        .build());
  }

  public void test_builder_sameCurrency() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxSingle.meta().builder()
        .set(FxSingle.meta().baseCurrencyPayment(), Payment.of(GBP_P1000, DATE_2015_06_30))
        .set(FxSingle.meta().counterCurrencyPayment(), Payment.of(GBP_M1000, DATE_2015_06_30))
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    FxSingle fwd = sut();
    ResolvedFxSingle test = fwd.resolve(REF_DATA);
    assertEquals(test.getBaseCurrencyPayment(), Payment.of(GBP_P1000, DATE_2015_06_30));
    assertEquals(test.getCounterCurrencyPayment(), Payment.of(USD_M1600, DATE_2015_06_30));
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  public void test_serialization() {
    assertSerialization(sut());
    String xml = JodaBeanSer.PRETTY.xmlWriter().write(sut());
    assertEquals(JodaBeanSer.PRETTY.xmlReader().read(xml), sut());

    String newXml = "<bean type='" + FxSingle.class.getName() + "'>" +
        "<baseCurrencyPayment><value>GBP 1000</value><date>2015-06-30</date></baseCurrencyPayment>" +
        "<counterCurrencyPayment><value>USD -1600</value><date>2015-06-30</date></counterCurrencyPayment>" +
        "</bean>";
    assertEquals(JodaBeanSer.PRETTY.xmlReader().read(newXml), sut());

    String oldXml = "<bean type='" + FxSingle.class.getName() + "'>" +
        "<baseCurrencyAmount>GBP 1000</baseCurrencyAmount>" +
        "<counterCurrencyAmount>USD -1600</counterCurrencyAmount>" +
        "<paymentDate>2015-06-30</paymentDate>" +
        "</bean>";
    assertEquals(JodaBeanSer.PRETTY.xmlReader().read(oldXml), sut());
  }

  //-------------------------------------------------------------------------
  static FxSingle sut() {
    return FxSingle.of(GBP_P1000, USD_M1600, DATE_2015_06_30);
  }

  static FxSingle sut2() {
    return FxSingle.of(GBP_M1000, EUR_P1600, DATE_2015_06_29);
  }

}
