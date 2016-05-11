/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.view.DiscountFactors;
import com.opengamma.strata.market.view.FxForwardRates;
import com.opengamma.strata.market.view.FxIndexRates;
import com.opengamma.strata.market.view.IborIndexRates;
import com.opengamma.strata.market.view.OvernightIndexRates;
import com.opengamma.strata.market.view.PriceIndexValues;

/**
 * A simple rates provider for overnight rates.
 */
@BeanDefinition
public class SimpleRatesProvider
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
  public <T> T data(MarketDataKey<T> key) {
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
  public CurveCurrencyParameterSensitivities curveParameterSensitivity(PointSensitivities pointSensitivities) {
    throw new UnsupportedOperationException();
  }

  @Override
  public MultiCurrencyAmount currencyExposure(PointSensitivities pointSensitivities) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<Curve> findCurve(CurveName name) {
    return Optional.empty();
  }

  @Override
  public LocalDateDoubleTimeSeries timeSeries(Index index) {
    throw new UnsupportedOperationException();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SimpleRatesProvider}.
   * @return the meta-bean, not null
   */
  public static SimpleRatesProvider.Meta meta() {
    return SimpleRatesProvider.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SimpleRatesProvider.Meta.INSTANCE);
  }

  @Override
  public SimpleRatesProvider.Meta metaBean() {
    return SimpleRatesProvider.Meta.INSTANCE;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
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

  /**
   * Gets the the {@code valuationDate} property.
   * @return the property, not null
   */
  public final Property<LocalDate> valuationDate() {
    return metaBean().valuationDate().createProperty(this);
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

  /**
   * Gets the the {@code dayCount} property.
   * @return the property, not null
   */
  public final Property<DayCount> dayCount() {
    return metaBean().dayCount().createProperty(this);
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

  /**
   * Gets the the {@code discountFactors} property.
   * @return the property, not null
   */
  public final Property<DiscountFactors> discountFactors() {
    return metaBean().discountFactors().createProperty(this);
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

  /**
   * Gets the the {@code fxIndexRates} property.
   * @return the property, not null
   */
  public final Property<FxIndexRates> fxIndexRates() {
    return metaBean().fxIndexRates().createProperty(this);
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

  /**
   * Gets the the {@code fxForwardRates} property.
   * @return the property, not null
   */
  public final Property<FxForwardRates> fxForwardRates() {
    return metaBean().fxForwardRates().createProperty(this);
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

  /**
   * Gets the the {@code iborRates} property.
   * @return the property, not null
   */
  public final Property<IborIndexRates> iborRates() {
    return metaBean().iborRates().createProperty(this);
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

  /**
   * Gets the the {@code overnightRates} property.
   * @return the property, not null
   */
  public final Property<OvernightIndexRates> overnightRates() {
    return metaBean().overnightRates().createProperty(this);
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

  /**
   * Gets the the {@code priceIndexValues} property.
   * @return the property, not null
   */
  public final Property<PriceIndexValues> priceIndexValues() {
    return metaBean().priceIndexValues().createProperty(this);
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
    int len = buf.length();
    toString(buf);
    if (buf.length() > len) {
      buf.setLength(buf.length() - 2);
    }
    buf.append('}');
    return buf.toString();
  }

  protected void toString(StringBuilder buf) {
    buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(getValuationDate())).append(',').append(' ');
    buf.append("dayCount").append('=').append(JodaBeanUtils.toString(getDayCount())).append(',').append(' ');
    buf.append("discountFactors").append('=').append(JodaBeanUtils.toString(getDiscountFactors())).append(',').append(' ');
    buf.append("fxIndexRates").append('=').append(JodaBeanUtils.toString(getFxIndexRates())).append(',').append(' ');
    buf.append("fxForwardRates").append('=').append(JodaBeanUtils.toString(getFxForwardRates())).append(',').append(' ');
    buf.append("iborRates").append('=').append(JodaBeanUtils.toString(getIborRates())).append(',').append(' ');
    buf.append("overnightRates").append('=').append(JodaBeanUtils.toString(getOvernightRates())).append(',').append(' ');
    buf.append("priceIndexValues").append('=').append(JodaBeanUtils.toString(getPriceIndexValues())).append(',').append(' ');
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SimpleRatesProvider}.
   */
  public static class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code valuationDate} property.
     */
    private final MetaProperty<LocalDate> valuationDate = DirectMetaProperty.ofReadWrite(
        this, "valuationDate", SimpleRatesProvider.class, LocalDate.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofReadWrite(
        this, "dayCount", SimpleRatesProvider.class, DayCount.class);
    /**
     * The meta-property for the {@code discountFactors} property.
     */
    private final MetaProperty<DiscountFactors> discountFactors = DirectMetaProperty.ofReadWrite(
        this, "discountFactors", SimpleRatesProvider.class, DiscountFactors.class);
    /**
     * The meta-property for the {@code fxIndexRates} property.
     */
    private final MetaProperty<FxIndexRates> fxIndexRates = DirectMetaProperty.ofReadWrite(
        this, "fxIndexRates", SimpleRatesProvider.class, FxIndexRates.class);
    /**
     * The meta-property for the {@code fxForwardRates} property.
     */
    private final MetaProperty<FxForwardRates> fxForwardRates = DirectMetaProperty.ofReadWrite(
        this, "fxForwardRates", SimpleRatesProvider.class, FxForwardRates.class);
    /**
     * The meta-property for the {@code iborRates} property.
     */
    private final MetaProperty<IborIndexRates> iborRates = DirectMetaProperty.ofReadWrite(
        this, "iborRates", SimpleRatesProvider.class, IborIndexRates.class);
    /**
     * The meta-property for the {@code overnightRates} property.
     */
    private final MetaProperty<OvernightIndexRates> overnightRates = DirectMetaProperty.ofReadWrite(
        this, "overnightRates", SimpleRatesProvider.class, OvernightIndexRates.class);
    /**
     * The meta-property for the {@code priceIndexValues} property.
     */
    private final MetaProperty<PriceIndexValues> priceIndexValues = DirectMetaProperty.ofReadWrite(
        this, "priceIndexValues", SimpleRatesProvider.class, PriceIndexValues.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "valuationDate",
        "dayCount",
        "discountFactors",
        "fxIndexRates",
        "fxForwardRates",
        "iborRates",
        "overnightRates",
        "priceIndexValues");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          return valuationDate;
        case 1905311443:  // dayCount
          return dayCount;
        case -91613053:  // discountFactors
          return discountFactors;
        case 2123789395:  // fxIndexRates
          return fxIndexRates;
        case -1002932800:  // fxForwardRates
          return fxForwardRates;
        case 1263680567:  // iborRates
          return iborRates;
        case 300027439:  // overnightRates
          return overnightRates;
        case 1422773131:  // priceIndexValues
          return priceIndexValues;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends SimpleRatesProvider> builder() {
      return new DirectBeanBuilder<SimpleRatesProvider>(new SimpleRatesProvider());
    }

    @Override
    public Class<? extends SimpleRatesProvider> beanType() {
      return SimpleRatesProvider.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code valuationDate} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<LocalDate> valuationDate() {
      return valuationDate;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    /**
     * The meta-property for the {@code discountFactors} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<DiscountFactors> discountFactors() {
      return discountFactors;
    }

    /**
     * The meta-property for the {@code fxIndexRates} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<FxIndexRates> fxIndexRates() {
      return fxIndexRates;
    }

    /**
     * The meta-property for the {@code fxForwardRates} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<FxForwardRates> fxForwardRates() {
      return fxForwardRates;
    }

    /**
     * The meta-property for the {@code iborRates} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<IborIndexRates> iborRates() {
      return iborRates;
    }

    /**
     * The meta-property for the {@code overnightRates} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<OvernightIndexRates> overnightRates() {
      return overnightRates;
    }

    /**
     * The meta-property for the {@code priceIndexValues} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<PriceIndexValues> priceIndexValues() {
      return priceIndexValues;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          return ((SimpleRatesProvider) bean).getValuationDate();
        case 1905311443:  // dayCount
          return ((SimpleRatesProvider) bean).getDayCount();
        case -91613053:  // discountFactors
          return ((SimpleRatesProvider) bean).getDiscountFactors();
        case 2123789395:  // fxIndexRates
          return ((SimpleRatesProvider) bean).getFxIndexRates();
        case -1002932800:  // fxForwardRates
          return ((SimpleRatesProvider) bean).getFxForwardRates();
        case 1263680567:  // iborRates
          return ((SimpleRatesProvider) bean).getIborRates();
        case 300027439:  // overnightRates
          return ((SimpleRatesProvider) bean).getOvernightRates();
        case 1422773131:  // priceIndexValues
          return ((SimpleRatesProvider) bean).getPriceIndexValues();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          ((SimpleRatesProvider) bean).setValuationDate((LocalDate) newValue);
          return;
        case 1905311443:  // dayCount
          ((SimpleRatesProvider) bean).setDayCount((DayCount) newValue);
          return;
        case -91613053:  // discountFactors
          ((SimpleRatesProvider) bean).setDiscountFactors((DiscountFactors) newValue);
          return;
        case 2123789395:  // fxIndexRates
          ((SimpleRatesProvider) bean).setFxIndexRates((FxIndexRates) newValue);
          return;
        case -1002932800:  // fxForwardRates
          ((SimpleRatesProvider) bean).setFxForwardRates((FxForwardRates) newValue);
          return;
        case 1263680567:  // iborRates
          ((SimpleRatesProvider) bean).setIborRates((IborIndexRates) newValue);
          return;
        case 300027439:  // overnightRates
          ((SimpleRatesProvider) bean).setOvernightRates((OvernightIndexRates) newValue);
          return;
        case 1422773131:  // priceIndexValues
          ((SimpleRatesProvider) bean).setPriceIndexValues((PriceIndexValues) newValue);
          return;
      }
      super.propertySet(bean, propertyName, newValue, quiet);
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
