/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentMap;

import org.joda.beans.BeanBuilder;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.impl.BufferingBeanBuilder;
import org.joda.beans.impl.StandaloneMetaProperty;
import org.joda.beans.ser.DefaultDeserializer;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;

/**
 * Deserialize {@code FxSingle} handling old format.
 */
final class FxSingleDeserializer extends DefaultDeserializer {

  private static final MetaProperty<Payment> BASE_CURRENCY_PAYMENT = FxSingle.meta().baseCurrencyPayment();
  private static final MetaProperty<Payment> COUNTER_CURRENCY_PAYMENT = FxSingle.meta().counterCurrencyPayment();
  private static final MetaProperty<BusinessDayAdjustment> PAYMENT_ADJUSTMENT_DATE = FxSingle.meta().paymentDateAdjustment();
  private static final MetaProperty<CurrencyAmount> BASE_CURRENCY_AMOUNT =
      StandaloneMetaProperty.of("baseCurrencyAmount", FxSingle.meta(), CurrencyAmount.class);
  private static final MetaProperty<CurrencyAmount> COUNTER_CURRENCY_AMOUNT =
      StandaloneMetaProperty.of("counterCurrencyAmount", FxSingle.meta(), CurrencyAmount.class);
  private static final MetaProperty<LocalDate> PAYMENT_DATE =
      StandaloneMetaProperty.of("paymentDate", FxSingle.meta(), LocalDate.class);

  //-------------------------------------------------------------------------
  // restricted constructor
  FxSingleDeserializer() {
  }

  @Override
  public BeanBuilder<?> createBuilder(Class<?> beanType, MetaBean metaBean) {
    return BufferingBeanBuilder.of(metaBean);
  }

  @Override
  public MetaProperty<?> findMetaProperty(Class<?> beanType, MetaBean metaBean, String propertyName) {
    try {
      return metaBean.metaProperty(propertyName);
    } catch (NoSuchElementException ex) {
      if (BASE_CURRENCY_AMOUNT.name().equals(propertyName)) {
        return BASE_CURRENCY_AMOUNT;
      }
      if (COUNTER_CURRENCY_AMOUNT.name().equals(propertyName)) {
        return COUNTER_CURRENCY_AMOUNT;
      }
      if (PAYMENT_DATE.name().equals(propertyName)) {
        return PAYMENT_DATE;
      }
      throw ex;
    }
  }

  @Override
  public Object build(Class<?> beanType, BeanBuilder<?> builder) {
    BufferingBeanBuilder<?> bld = (BufferingBeanBuilder<?>) builder;
    ConcurrentMap<MetaProperty<?>, Object> buffer = bld.getBuffer();
    BusinessDayAdjustment bda = (BusinessDayAdjustment) buffer.getOrDefault(PAYMENT_ADJUSTMENT_DATE, null);
    if (buffer.containsKey(BASE_CURRENCY_AMOUNT) &&
        buffer.containsKey(COUNTER_CURRENCY_AMOUNT) &&
        buffer.containsKey(PAYMENT_DATE)) {

      CurrencyAmount baseAmount = (CurrencyAmount) builder.get(BASE_CURRENCY_AMOUNT);
      CurrencyAmount counterAmount = (CurrencyAmount) builder.get(COUNTER_CURRENCY_AMOUNT);
      LocalDate paymentDate = (LocalDate) builder.get(PAYMENT_DATE);
      return bda != null ?
          FxSingle.of(baseAmount, counterAmount, paymentDate, bda) :
          FxSingle.of(baseAmount, counterAmount, paymentDate);

    } else {
      Payment basePayment = (Payment) buffer.get(BASE_CURRENCY_PAYMENT);
      Payment counterPayment = (Payment) buffer.get(COUNTER_CURRENCY_PAYMENT);
      return bda != null ? FxSingle.of(basePayment, counterPayment, bda) : FxSingle.of(basePayment, counterPayment);
    }
  }

}
