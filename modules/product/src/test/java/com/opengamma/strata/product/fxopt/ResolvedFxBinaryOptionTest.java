/**
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.strata.product.fxopt;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.*;

import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.common.PutCall;
import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * Test {@link ResolvedFxBinaryOption}.
 */

@Test
public class ResolvedFxBinaryOptionTest {
    private static final ZonedDateTime EXPIRY_DATE_TIME = ZonedDateTime.of(2015, 2, 14, 12, 15, 0, 0, ZoneOffset.UTC);
    private static final LongShort LONG = LongShort.LONG;
    private static final LocalDate UNADJUSTED_PAYMENT_DATE = LocalDate.of(2015, 2, 16);
    private static final AdjustableDate PAYMENT_DATE = AdjustableDate.of(UNADJUSTED_PAYMENT_DATE);
    private static final double NOTIONAL = 1.0e6;
    private static final FxIndex FX = FxIndex.of("EUR/USD-ECB");
    private static final CurrencyAmount USD_AMOUNT = CurrencyAmount.of(USD, NOTIONAL);
    private static final AdjustablePayment FX_PAYMENT = AdjustablePayment.of(USD_AMOUNT, PAYMENT_DATE);
    private static final PutCall PUT_CALL = PutCall.CALL;
    private static final double STRIKE = 1.06;

    //-------------------------------------------------------------------------
    public void test_builder() {
        ResolvedFxBinaryOption test = sut();
        assertEquals(test.getExpiry(), EXPIRY_DATE_TIME);
        assertEquals(test.getExpiryDate(), EXPIRY_DATE_TIME.toLocalDate());
        assertEquals(test.getLongShort(), LONG);
        assertEquals(test.getUnderlying(), FX);
        assertEquals(test.getCurrencyPair(), FX.getCurrencyPair());
        assertEquals(test.getPaymentCurrencyAmount(), FX_PAYMENT);
        assertEquals(test.getStrike(), STRIKE);
    }

    public void test_builder_earlyPaymentDate() {
        assertThrowsIllegalArg(() -> ResolvedFxBinaryOption.builder()
                .longShort(LONG)
                .expiry(LocalDate.of(2015, 2, 21).atStartOfDay(ZoneOffset.UTC))
                .underlying(FX)
                .paymentCurrencyAmount(FX_PAYMENT)
                .strike(STRIKE)
                .build());
    }

    //-------------------------------------------------------------------------
    public void coverage() {
        coverImmutableBean(sut());
        coverBeanEquals(sut(), sut2());
    }

    public void test_serialization() {
        assertSerialization(sut());
    }

    //-------------------------------------------------------------------------
    static ResolvedFxBinaryOption sut() {
        return ResolvedFxBinaryOption.builder()
                .longShort(LONG)
                .expiry(EXPIRY_DATE_TIME)
                .underlying(FX)
                .paymentCurrencyAmount(FX_PAYMENT)
                .putCall(PUT_CALL)
                .strike(STRIKE)
                .build();
    }

    static ResolvedFxBinaryOption sut2() {
        FxIndex fxRate = FxIndex.of("EUR/GBP-ECB");
        double fxStrike = 1.25;
        return ResolvedFxBinaryOption.builder()
                .longShort(LongShort.SHORT)
                .expiry(EXPIRY_DATE_TIME.plusSeconds(1))
                .underlying(fxRate)
                .paymentCurrencyAmount(AdjustablePayment.of(CurrencyAmount.of(GBP, NOTIONAL), PAYMENT_DATE))
                .putCall(PutCall.PUT)
                .strike(fxStrike)
                .build();
    }

}
