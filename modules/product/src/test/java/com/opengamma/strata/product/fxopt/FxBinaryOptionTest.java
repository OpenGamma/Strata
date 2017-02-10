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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.product.common.PutCall;
import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.product.common.LongShort;

/**
 * Test {@link FxBinaryOption}.
 */
@Test
public class FxBinaryOptionTest {
    private static final ReferenceData REF_DATA = ReferenceData.standard();
    private static final LocalDate EXPIRY_DATE = LocalDate.of(2015, 2, 14);
    private static final LocalTime EXPIRY_TIME = LocalTime.of(12, 15);
    private static final ZoneId EXPIRY_ZONE = ZoneId.of("Z");
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
        FxBinaryOption test = sut();
        assertEquals(test.getExpiryDate(), EXPIRY_DATE);
        assertEquals(test.getExpiry(), ZonedDateTime.of(EXPIRY_DATE, EXPIRY_TIME, EXPIRY_ZONE));
        assertEquals(test.getExpiryZone(), EXPIRY_ZONE);
        assertEquals(test.getExpiryTime(), EXPIRY_TIME);
        assertEquals(test.getLongShort(), LONG);
        assertEquals(test.getUnderlying(), FX);
        assertEquals(test.getCurrencyPair(), FX.getCurrencyPair());
        assertEquals(test.getPaymentCurrencyAmount(), FX_PAYMENT);
        assertEquals(test.getStrike(), STRIKE);
    }

    public void test_builder_earlyPaymentDate() {
        assertThrowsIllegalArg(() -> FxBinaryOption.builder()
                .longShort(LONG)
                .expiryDate(LocalDate.of(2015, 2, 21))
                .expiryTime(EXPIRY_TIME)
                .expiryZone(EXPIRY_ZONE)
                .underlying(FX)
                .paymentCurrencyAmount(FX_PAYMENT)
                .strike(STRIKE)
                .build());
    }

    //-------------------------------------------------------------------------
    public void test_resolve() {
        FxBinaryOption base = sut();
        ResolvedFxBinaryOption expected = ResolvedFxBinaryOption.builder()
                .longShort(LONG)
                .expiry(EXPIRY_DATE.atTime(EXPIRY_TIME).atZone(EXPIRY_ZONE))
                .underlying(FX)
                .paymentCurrencyAmount(FX_PAYMENT)
                .putCall(PUT_CALL)
                .strike(STRIKE)
                .build();
        assertEquals(base.resolve(REF_DATA), expected);
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
    static FxBinaryOption sut() {
        return FxBinaryOption.builder()
                .longShort(LONG)
                .expiryDate(EXPIRY_DATE)
                .expiryTime(EXPIRY_TIME)
                .expiryZone(EXPIRY_ZONE)
                .underlying(FX)
                .paymentCurrencyAmount(FX_PAYMENT)
                .putCall(PUT_CALL)
                .strike(STRIKE)
                .build();
    }

    static FxBinaryOption sut2() {
        FxIndex fxRate = FxIndex.of("EUR/GBP-ECB");
        double fxStrike = 1.25;
        return FxBinaryOption.builder()
                .longShort(LongShort.SHORT)
                .expiryDate(LocalDate.of(2015, 2, 15))
                .expiryTime(LocalTime.of(12, 45))
                .expiryZone(ZoneId.of("GMT"))
                .underlying(fxRate)
                .paymentCurrencyAmount(AdjustablePayment.of(CurrencyAmount.of(GBP, NOTIONAL), PAYMENT_DATE))
                .putCall(PutCall.PUT)
                .strike(fxStrike)
                .build();
    }
}