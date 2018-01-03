/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.joda.beans.Bean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.TypedMetaBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.BasicBeanBuilder;
import org.joda.beans.impl.direct.MinimalMetaBean;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.fx.FxForwardRates;
import com.opengamma.strata.pricer.fx.FxIndexRates;

/**
 * A simple rates provider for overnight rates.
 */
@BeanDefinition(style = "minimal")
public final class SimpleRatesProvider
    implements RatesProvider, Bean {

  @PropertyDefinition(overrideGet = true)
  private LocalDate valuationDate;
  @PropertyDefinition
  private DayCount dayCount;
  @PropertyDefinition
  private DiscountFactors discountFactors;
  @PropertyDefinition
  private FxIndexRates fxIndexRates;
  @PropertyDefinition
  private FxForwardRates fxForwardRates;
  @PropertyDefinition
  private IborIndexRates iborRates;
  @PropertyDefinition
  private OvernightIndexRates overnightRates;
  @PropertyDefinition
  private PriceIndexValues priceIndexValues;

  public SimpleRatesProvider() {
  }

  public SimpleRatesProvider(LocalDate valuationDate) {
    this.valuationDate = valuationDate;
  }

  public SimpleRatesProvider(OvernightIndexRates overnightRates) {
    this.overnightRates = overnightRates;
  }

  public SimpleRatesProvider(LocalDate valuationDate, OvernightIndexRates overnightRates) {
    this.valuationDate = valuationDate;
    this.overnightRates = overnightRates;
  }

  public SimpleRatesProvider(DiscountFactors discountFactors) {
    this.discountFactors = discountFactors;
  }

  public SimpleRatesProvider(LocalDate valuationDate, DiscountFactors discountFactors) {
    this.valuationDate = valuationDate;
    this.discountFactors = discountFactors;
  }

  public SimpleRatesProvider(LocalDate valuationDate, DiscountFactors discountFactors, IborIndexRates iborRates) {
    this.valuationDate = valuationDate;
    this.discountFactors = discountFactors;
    this.iborRates = iborRates;
  }

  //-------------------------------------------------------------------------
  @Override
  public ImmutableSet<Currency> getDiscountCurrencies() {
    if (discountFactors != null) {
      return ImmutableSet.of(discountFactors.getCurrency());
    }
    return ImmutableSet.of();
  }

  @Override
  public ImmutableSet<IborIndex> getIborIndices() {
    if (iborRates != null) {
      return ImmutableSet.of(iborRates.getIndex());
    }
    return ImmutableSet.of();
  }

  @Override
  public ImmutableSet<OvernightIndex> getOvernightIndices() {
    if (overnightRates != null) {
      return ImmutableSet.of(overnightRates.getIndex());
    }
    return ImmutableSet.of();
  }

  @Override
  public ImmutableSet<PriceIndex> getPriceIndices() {
    if (priceIndexValues != null) {
      return ImmutableSet.of(priceIndexValues.getIndex());
    }
    return ImmutableSet.of();
  }

  @Override
  public ImmutableSet<Index> getTimeSeriesIndices() {
    return ImmutableSet.of();
  }

  //-------------------------------------------------------------------------
  @Override
  public <T> T data(MarketDataId<T> key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public double fxRate(Currency baseCurrency, Currency counterCurrency) {
    if (baseCurrency.equals(counterCurrency)) {
      return 1d;
    }
    throw new UnsupportedOperationException("FxRate not found: " + baseCurrency + "/" + counterCurrency);
  }

  @Override
  public DiscountFactors discountFactors(Currency currency) {
    return discountFactors;
  }

  @Override
  public FxIndexRates fxIndexRates(FxIndex index) {
    return fxIndexRates;
  }

  @Override
  public FxForwardRates fxForwardRates(CurrencyPair currencyPair) {
    return fxForwardRates;
  }

  @Override
  public IborIndexRates iborIndexRates(IborIndex index) {
    return iborRates;
  }

  @Override
  public OvernightIndexRates overnightIndexRates(OvernightIndex index) {
    return overnightRates;
  }

  @Override
  public PriceIndexValues priceIndexValues(PriceIndex index) {
    return priceIndexValues;
  }

  @Override
  public <T> Optional<T> findData(MarketDataName<T> name) {
    return Optional.empty();
  }

  @Override
  public LocalDateDoubleTimeSeries timeSeries(Index index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ImmutableRatesProvider toImmutableRatesProvider() {
    throw new UnsupportedOperationException();
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code SimpleRatesProvider}.
   */
  private static final TypedMetaBean<SimpleRatesProvider> META_BEAN =
      MinimalMetaBean.of(
          SimpleRatesProvider.class,
          new String[] {
              "valuationDate",
              "dayCount",
              "discountFactors",
              "fxIndexRates",
              "fxForwardRates",
              "iborRates",
              "overnightRates",
              "priceIndexValues"},
          () -> new BasicBeanBuilder<>(new SimpleRatesProvider()),
          Arrays.<Function<SimpleRatesProvider, Object>>asList(
              b -> b.getValuationDate(),
              b -> b.getDayCount(),
              b -> b.getDiscountFactors(),
              b -> b.getFxIndexRates(),
              b -> b.getFxForwardRates(),
              b -> b.getIborRates(),
              b -> b.getOvernightRates(),
              b -> b.getPriceIndexValues()),
          Arrays.<BiConsumer<SimpleRatesProvider, Object>>asList(
              (b, v) -> b.setValuationDate((LocalDate) v),
              (b, v) -> b.setDayCount((DayCount) v),
              (b, v) -> b.setDiscountFactors((DiscountFactors) v),
              (b, v) -> b.setFxIndexRates((FxIndexRates) v),
              (b, v) -> b.setFxForwardRates((FxForwardRates) v),
              (b, v) -> b.setIborRates((IborIndexRates) v),
              (b, v) -> b.setOvernightRates((OvernightIndexRates) v),
              (b, v) -> b.setPriceIndexValues((PriceIndexValues) v)));

  /**
   * The meta-bean for {@code SimpleRatesProvider}.
   * @return the meta-bean, not null
   */
  public static TypedMetaBean<SimpleRatesProvider> meta() {
    return META_BEAN;
  }

  static {
    MetaBean.register(META_BEAN);
  }

  @Override
  public TypedMetaBean<SimpleRatesProvider> metaBean() {
    return META_BEAN;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the valuationDate.
   * @return the value of the property
   */
  @Override
  public LocalDate getValuationDate() {
    return valuationDate;
  }

  /**
   * Sets the valuationDate.
   * @param valuationDate  the new value of the property
   */
  public void setValuationDate(LocalDate valuationDate) {
    this.valuationDate = valuationDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the dayCount.
   * @return the value of the property
   */
  public DayCount getDayCount() {
    return dayCount;
  }

  /**
   * Sets the dayCount.
   * @param dayCount  the new value of the property
   */
  public void setDayCount(DayCount dayCount) {
    this.dayCount = dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the discountFactors.
   * @return the value of the property
   */
  public DiscountFactors getDiscountFactors() {
    return discountFactors;
  }

  /**
   * Sets the discountFactors.
   * @param discountFactors  the new value of the property
   */
  public void setDiscountFactors(DiscountFactors discountFactors) {
    this.discountFactors = discountFactors;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the fxIndexRates.
   * @return the value of the property
   */
  public FxIndexRates getFxIndexRates() {
    return fxIndexRates;
  }

  /**
   * Sets the fxIndexRates.
   * @param fxIndexRates  the new value of the property
   */
  public void setFxIndexRates(FxIndexRates fxIndexRates) {
    this.fxIndexRates = fxIndexRates;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the fxForwardRates.
   * @return the value of the property
   */
  public FxForwardRates getFxForwardRates() {
    return fxForwardRates;
  }

  /**
   * Sets the fxForwardRates.
   * @param fxForwardRates  the new value of the property
   */
  public void setFxForwardRates(FxForwardRates fxForwardRates) {
    this.fxForwardRates = fxForwardRates;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the iborRates.
   * @return the value of the property
   */
  public IborIndexRates getIborRates() {
    return iborRates;
  }

  /**
   * Sets the iborRates.
   * @param iborRates  the new value of the property
   */
  public void setIborRates(IborIndexRates iborRates) {
    this.iborRates = iborRates;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the overnightRates.
   * @return the value of the property
   */
  public OvernightIndexRates getOvernightRates() {
    return overnightRates;
  }

  /**
   * Sets the overnightRates.
   * @param overnightRates  the new value of the property
   */
  public void setOvernightRates(OvernightIndexRates overnightRates) {
    this.overnightRates = overnightRates;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the priceIndexValues.
   * @return the value of the property
   */
  public PriceIndexValues getPriceIndexValues() {
    return priceIndexValues;
  }

  /**
   * Sets the priceIndexValues.
   * @param priceIndexValues  the new value of the property
   */
  public void setPriceIndexValues(PriceIndexValues priceIndexValues) {
    this.priceIndexValues = priceIndexValues;
  }

  //-----------------------------------------------------------------------
  @Override
  public SimpleRatesProvider clone() {
    return JodaBeanUtils.cloneAlways(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SimpleRatesProvider other = (SimpleRatesProvider) obj;
      return JodaBeanUtils.equal(getValuationDate(), other.getValuationDate()) &&
          JodaBeanUtils.equal(getDayCount(), other.getDayCount()) &&
          JodaBeanUtils.equal(getDiscountFactors(), other.getDiscountFactors()) &&
          JodaBeanUtils.equal(getFxIndexRates(), other.getFxIndexRates()) &&
          JodaBeanUtils.equal(getFxForwardRates(), other.getFxForwardRates()) &&
          JodaBeanUtils.equal(getIborRates(), other.getIborRates()) &&
          JodaBeanUtils.equal(getOvernightRates(), other.getOvernightRates()) &&
          JodaBeanUtils.equal(getPriceIndexValues(), other.getPriceIndexValues());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getValuationDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getDayCount());
    hash = hash * 31 + JodaBeanUtils.hashCode(getDiscountFactors());
    hash = hash * 31 + JodaBeanUtils.hashCode(getFxIndexRates());
    hash = hash * 31 + JodaBeanUtils.hashCode(getFxForwardRates());
    hash = hash * 31 + JodaBeanUtils.hashCode(getIborRates());
    hash = hash * 31 + JodaBeanUtils.hashCode(getOvernightRates());
    hash = hash * 31 + JodaBeanUtils.hashCode(getPriceIndexValues());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(288);
    buf.append("SimpleRatesProvider{");
    buf.append("valuationDate").append('=').append(getValuationDate()).append(',').append(' ');
    buf.append("dayCount").append('=').append(getDayCount()).append(',').append(' ');
    buf.append("discountFactors").append('=').append(getDiscountFactors()).append(',').append(' ');
    buf.append("fxIndexRates").append('=').append(getFxIndexRates()).append(',').append(' ');
    buf.append("fxForwardRates").append('=').append(getFxForwardRates()).append(',').append(' ');
    buf.append("iborRates").append('=').append(getIborRates()).append(',').append(' ');
    buf.append("overnightRates").append('=').append(getOvernightRates()).append(',').append(' ');
    buf.append("priceIndexValues").append('=').append(JodaBeanUtils.toString(getPriceIndexValues()));
    buf.append('}');
    return buf.toString();
  }

  //-------------------------- AUTOGENERATED END --------------------------
}
