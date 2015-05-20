/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.DiscountFactors;
import com.opengamma.strata.market.curve.DiscountFxIndexRates;
import com.opengamma.strata.market.curve.DiscountIborIndexRates;
import com.opengamma.strata.market.curve.DiscountOvernightIndexRates;
import com.opengamma.strata.market.curve.FxIndexRates;
import com.opengamma.strata.market.curve.IborIndexRates;
import com.opengamma.strata.market.curve.OvernightIndexRates;
import com.opengamma.strata.market.curve.ZeroRateDiscountFactors;

/**
 * The default immutable rates provider, used to calculate analytic measures.
 * <p>
 * This provides the environmental information against which pricing occurs.
 * This includes FX rates, discount factors and forward curves.
 */
@BeanDefinition
public final class ImmutableRatesProvider
    extends AbstractRatesProvider
    implements ImmutableBean, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The valuation date.
   * All curves and other data items in this provider are calibrated for this date.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate valuationDate;
  /**
   * The matrix of foreign exchange rates, defaulted to an empty matrix.
   */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final FxMatrix fxMatrix;
  /**
   * The discount curves, defaulted to an empty map.
   * The curve data, predicting the future, associated with each currency.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<Currency, YieldAndDiscountCurve> discountCurves;
  /**
   * The forward curves, defaulted to an empty map.
   * The curve data, predicting the future, associated with each index.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<Index, YieldAndDiscountCurve> indexCurves;
  /**
   * The time-series, defaulted to an empty map.
   * The historic data associated with each index.
   */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final ImmutableMap<Index, LocalDateDoubleTimeSeries> timeSeries;
  /**
   * The additional data, defaulted to an empty map.
   * This allows application code to access additional market data.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<Class<?>, Object> additionalData;
  /**
   * The day count applicable to the models.
   */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final DayCount dayCount;

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.fxMatrix = FxMatrix.empty();
    builder.discountCurves = ImmutableMap.of();
    builder.indexCurves = ImmutableMap.of();
    builder.timeSeries = ImmutableMap.of();
    builder.additionalData = ImmutableMap.of();
  }

  @ImmutableValidator
  private void validate() {
    for (Entry<Class<?>, Object> entry : additionalData.entrySet()) {
      if (!entry.getKey().isInstance(entry.getValue())) {
        throw new IllegalArgumentException("Invalid additional data entry: " + entry.getKey().getName());
      }
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public <T> T data(Class<T> type) {
    ArgChecker.notNull(type, "type");
    // type safety checked in validate()
    @SuppressWarnings("unchecked")
    T result = (T) additionalData.get(type);
    if (result == null) {
      throw new IllegalArgumentException("Unknown type: " + type.getName());
    }
    return result;
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDateDoubleTimeSeries timeSeries(Index index) {
    ArgChecker.notNull(index, "index");
    LocalDateDoubleTimeSeries series = timeSeries.get(index);
    if (series == null) {
      throw new IllegalArgumentException("Unknown index: " + index.getName());
    }
    return series;
  }

  //-------------------------------------------------------------------------
  @Override
  public double fxRate(Currency baseCurrency, Currency counterCurrency) {
    ArgChecker.notNull(baseCurrency, "baseCurrency");
    ArgChecker.notNull(counterCurrency, "counterCurrency");
    if (baseCurrency.equals(counterCurrency)) {
      return 1d;
    }
    return fxMatrix.fxRate(baseCurrency, counterCurrency);
  }

  //-------------------------------------------------------------------------
  @Override
  public DiscountFactors discountFactors(Currency currency) {
    YieldCurve curve = (YieldCurve) discountCurves.get(currency);
    if (curve == null) {
      throw new IllegalArgumentException("Unable to find discount curve: " + currency);
    }
    return ZeroRateDiscountFactors.of(currency, valuationDate, dayCount, curve);
  }

  //-------------------------------------------------------------------------
  @Override
  public FxIndexRates fxIndexRates(FxIndex index) {
    DiscountFactors base = discountFactors(index.getCurrencyPair().getBase());
    DiscountFactors counter = discountFactors(index.getCurrencyPair().getCounter());
    return DiscountFxIndexRates.of(index, timeSeries(index), fxMatrix, base, counter);
  }

  //-------------------------------------------------------------------------
  @Override
  public IborIndexRates iborIndexRates(IborIndex index) {
    LocalDateDoubleTimeSeries timeSeries = timeSeries(index);
    YieldCurve curve = indexCurve(index);
    DiscountFactors dfc = ZeroRateDiscountFactors.of(index.getCurrency(), getValuationDate(), dayCount, curve);
    return DiscountIborIndexRates.of(index, timeSeries, dfc);
  }

  @Override
  public OvernightIndexRates overnightIndexRates(OvernightIndex index) {
    LocalDateDoubleTimeSeries timeSeries = timeSeries(index);
    YieldCurve curve = indexCurve(index);
    DiscountFactors dfc = ZeroRateDiscountFactors.of(index.getCurrency(), getValuationDate(), dayCount, curve);
    return DiscountOvernightIndexRates.of(index, timeSeries, dfc);
  }

  // lookup the discount curve for the currency
  private YieldCurve indexCurve(Index index) {
    YieldCurve curve = (YieldCurve) indexCurves.get(index);
    if (curve == null) {
      throw new IllegalArgumentException("Unable to find index curve: " + index);
    }
    return curve;
  }

  //-------------------------------------------------------------------------
  @Override
  public double relativeTime(LocalDate date) {
    ArgChecker.notNull(date, "date");
    return dayCount.relativeYearFraction(valuationDate, date);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ImmutableRatesProvider}.
   * @return the meta-bean, not null
   */
  public static ImmutableRatesProvider.Meta meta() {
    return ImmutableRatesProvider.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ImmutableRatesProvider.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ImmutableRatesProvider.Builder builder() {
    return new ImmutableRatesProvider.Builder();
  }

  private ImmutableRatesProvider(
      LocalDate valuationDate,
      FxMatrix fxMatrix,
      Map<Currency, YieldAndDiscountCurve> discountCurves,
      Map<Index, YieldAndDiscountCurve> indexCurves,
      Map<Index, LocalDateDoubleTimeSeries> timeSeries,
      Map<Class<?>, Object> additionalData,
      DayCount dayCount) {
    JodaBeanUtils.notNull(valuationDate, "valuationDate");
    JodaBeanUtils.notNull(fxMatrix, "fxMatrix");
    JodaBeanUtils.notNull(discountCurves, "discountCurves");
    JodaBeanUtils.notNull(indexCurves, "indexCurves");
    JodaBeanUtils.notNull(timeSeries, "timeSeries");
    JodaBeanUtils.notNull(additionalData, "additionalData");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    this.valuationDate = valuationDate;
    this.fxMatrix = fxMatrix;
    this.discountCurves = ImmutableMap.copyOf(discountCurves);
    this.indexCurves = ImmutableMap.copyOf(indexCurves);
    this.timeSeries = ImmutableMap.copyOf(timeSeries);
    this.additionalData = ImmutableMap.copyOf(additionalData);
    this.dayCount = dayCount;
    validate();
  }

  @Override
  public ImmutableRatesProvider.Meta metaBean() {
    return ImmutableRatesProvider.Meta.INSTANCE;
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
   * Gets the valuation date.
   * All curves and other data items in this provider are calibrated for this date.
   * @return the value of the property, not null
   */
  @Override
  public LocalDate getValuationDate() {
    return valuationDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the matrix of foreign exchange rates, defaulted to an empty matrix.
   * @return the value of the property, not null
   */
  private FxMatrix getFxMatrix() {
    return fxMatrix;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the discount curves, defaulted to an empty map.
   * The curve data, predicting the future, associated with each currency.
   * @return the value of the property, not null
   */
  public ImmutableMap<Currency, YieldAndDiscountCurve> getDiscountCurves() {
    return discountCurves;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the forward curves, defaulted to an empty map.
   * The curve data, predicting the future, associated with each index.
   * @return the value of the property, not null
   */
  public ImmutableMap<Index, YieldAndDiscountCurve> getIndexCurves() {
    return indexCurves;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the time-series, defaulted to an empty map.
   * The historic data associated with each index.
   * @return the value of the property, not null
   */
  private ImmutableMap<Index, LocalDateDoubleTimeSeries> getTimeSeries() {
    return timeSeries;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the additional data, defaulted to an empty map.
   * This allows application code to access additional market data.
   * @return the value of the property, not null
   */
  public ImmutableMap<Class<?>, Object> getAdditionalData() {
    return additionalData;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count applicable to the models.
   * @return the value of the property, not null
   */
  private DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ImmutableRatesProvider other = (ImmutableRatesProvider) obj;
      return JodaBeanUtils.equal(getValuationDate(), other.getValuationDate()) &&
          JodaBeanUtils.equal(getFxMatrix(), other.getFxMatrix()) &&
          JodaBeanUtils.equal(getDiscountCurves(), other.getDiscountCurves()) &&
          JodaBeanUtils.equal(getIndexCurves(), other.getIndexCurves()) &&
          JodaBeanUtils.equal(getTimeSeries(), other.getTimeSeries()) &&
          JodaBeanUtils.equal(getAdditionalData(), other.getAdditionalData()) &&
          JodaBeanUtils.equal(getDayCount(), other.getDayCount());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getValuationDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getFxMatrix());
    hash = hash * 31 + JodaBeanUtils.hashCode(getDiscountCurves());
    hash = hash * 31 + JodaBeanUtils.hashCode(getIndexCurves());
    hash = hash * 31 + JodaBeanUtils.hashCode(getTimeSeries());
    hash = hash * 31 + JodaBeanUtils.hashCode(getAdditionalData());
    hash = hash * 31 + JodaBeanUtils.hashCode(getDayCount());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("ImmutableRatesProvider{");
    buf.append("valuationDate").append('=').append(getValuationDate()).append(',').append(' ');
    buf.append("fxMatrix").append('=').append(getFxMatrix()).append(',').append(' ');
    buf.append("discountCurves").append('=').append(getDiscountCurves()).append(',').append(' ');
    buf.append("indexCurves").append('=').append(getIndexCurves()).append(',').append(' ');
    buf.append("timeSeries").append('=').append(getTimeSeries()).append(',').append(' ');
    buf.append("additionalData").append('=').append(getAdditionalData()).append(',').append(' ');
    buf.append("dayCount").append('=').append(JodaBeanUtils.toString(getDayCount()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ImmutableRatesProvider}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code valuationDate} property.
     */
    private final MetaProperty<LocalDate> valuationDate = DirectMetaProperty.ofImmutable(
        this, "valuationDate", ImmutableRatesProvider.class, LocalDate.class);
    /**
     * The meta-property for the {@code fxMatrix} property.
     */
    private final MetaProperty<FxMatrix> fxMatrix = DirectMetaProperty.ofImmutable(
        this, "fxMatrix", ImmutableRatesProvider.class, FxMatrix.class);
    /**
     * The meta-property for the {@code discountCurves} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<Currency, YieldAndDiscountCurve>> discountCurves = DirectMetaProperty.ofImmutable(
        this, "discountCurves", ImmutableRatesProvider.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code indexCurves} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<Index, YieldAndDiscountCurve>> indexCurves = DirectMetaProperty.ofImmutable(
        this, "indexCurves", ImmutableRatesProvider.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code timeSeries} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<Index, LocalDateDoubleTimeSeries>> timeSeries = DirectMetaProperty.ofImmutable(
        this, "timeSeries", ImmutableRatesProvider.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code additionalData} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<Class<?>, Object>> additionalData = DirectMetaProperty.ofImmutable(
        this, "additionalData", ImmutableRatesProvider.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", ImmutableRatesProvider.class, DayCount.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "valuationDate",
        "fxMatrix",
        "discountCurves",
        "indexCurves",
        "timeSeries",
        "additionalData",
        "dayCount");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          return valuationDate;
        case -1198118093:  // fxMatrix
          return fxMatrix;
        case -624113147:  // discountCurves
          return discountCurves;
        case 886361302:  // indexCurves
          return indexCurves;
        case 779431844:  // timeSeries
          return timeSeries;
        case -974458767:  // additionalData
          return additionalData;
        case 1905311443:  // dayCount
          return dayCount;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ImmutableRatesProvider.Builder builder() {
      return new ImmutableRatesProvider.Builder();
    }

    @Override
    public Class<? extends ImmutableRatesProvider> beanType() {
      return ImmutableRatesProvider.class;
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
    public MetaProperty<LocalDate> valuationDate() {
      return valuationDate;
    }

    /**
     * The meta-property for the {@code fxMatrix} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxMatrix> fxMatrix() {
      return fxMatrix;
    }

    /**
     * The meta-property for the {@code discountCurves} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<Currency, YieldAndDiscountCurve>> discountCurves() {
      return discountCurves;
    }

    /**
     * The meta-property for the {@code indexCurves} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<Index, YieldAndDiscountCurve>> indexCurves() {
      return indexCurves;
    }

    /**
     * The meta-property for the {@code timeSeries} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<Index, LocalDateDoubleTimeSeries>> timeSeries() {
      return timeSeries;
    }

    /**
     * The meta-property for the {@code additionalData} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<Class<?>, Object>> additionalData() {
      return additionalData;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          return ((ImmutableRatesProvider) bean).getValuationDate();
        case -1198118093:  // fxMatrix
          return ((ImmutableRatesProvider) bean).getFxMatrix();
        case -624113147:  // discountCurves
          return ((ImmutableRatesProvider) bean).getDiscountCurves();
        case 886361302:  // indexCurves
          return ((ImmutableRatesProvider) bean).getIndexCurves();
        case 779431844:  // timeSeries
          return ((ImmutableRatesProvider) bean).getTimeSeries();
        case -974458767:  // additionalData
          return ((ImmutableRatesProvider) bean).getAdditionalData();
        case 1905311443:  // dayCount
          return ((ImmutableRatesProvider) bean).getDayCount();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code ImmutableRatesProvider}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ImmutableRatesProvider> {

    private LocalDate valuationDate;
    private FxMatrix fxMatrix;
    private Map<Currency, YieldAndDiscountCurve> discountCurves = ImmutableMap.of();
    private Map<Index, YieldAndDiscountCurve> indexCurves = ImmutableMap.of();
    private Map<Index, LocalDateDoubleTimeSeries> timeSeries = ImmutableMap.of();
    private Map<Class<?>, Object> additionalData = ImmutableMap.of();
    private DayCount dayCount;

    /**
     * Restricted constructor.
     */
    private Builder() {
      applyDefaults(this);
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ImmutableRatesProvider beanToCopy) {
      this.valuationDate = beanToCopy.getValuationDate();
      this.fxMatrix = beanToCopy.getFxMatrix();
      this.discountCurves = beanToCopy.getDiscountCurves();
      this.indexCurves = beanToCopy.getIndexCurves();
      this.timeSeries = beanToCopy.getTimeSeries();
      this.additionalData = beanToCopy.getAdditionalData();
      this.dayCount = beanToCopy.getDayCount();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          return valuationDate;
        case -1198118093:  // fxMatrix
          return fxMatrix;
        case -624113147:  // discountCurves
          return discountCurves;
        case 886361302:  // indexCurves
          return indexCurves;
        case 779431844:  // timeSeries
          return timeSeries;
        case -974458767:  // additionalData
          return additionalData;
        case 1905311443:  // dayCount
          return dayCount;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          this.valuationDate = (LocalDate) newValue;
          break;
        case -1198118093:  // fxMatrix
          this.fxMatrix = (FxMatrix) newValue;
          break;
        case -624113147:  // discountCurves
          this.discountCurves = (Map<Currency, YieldAndDiscountCurve>) newValue;
          break;
        case 886361302:  // indexCurves
          this.indexCurves = (Map<Index, YieldAndDiscountCurve>) newValue;
          break;
        case 779431844:  // timeSeries
          this.timeSeries = (Map<Index, LocalDateDoubleTimeSeries>) newValue;
          break;
        case -974458767:  // additionalData
          this.additionalData = (Map<Class<?>, Object>) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public ImmutableRatesProvider build() {
      return new ImmutableRatesProvider(
          valuationDate,
          fxMatrix,
          discountCurves,
          indexCurves,
          timeSeries,
          additionalData,
          dayCount);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code valuationDate} property in the builder.
     * @param valuationDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder valuationDate(LocalDate valuationDate) {
      JodaBeanUtils.notNull(valuationDate, "valuationDate");
      this.valuationDate = valuationDate;
      return this;
    }

    /**
     * Sets the {@code fxMatrix} property in the builder.
     * @param fxMatrix  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fxMatrix(FxMatrix fxMatrix) {
      JodaBeanUtils.notNull(fxMatrix, "fxMatrix");
      this.fxMatrix = fxMatrix;
      return this;
    }

    /**
     * Sets the {@code discountCurves} property in the builder.
     * @param discountCurves  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder discountCurves(Map<Currency, YieldAndDiscountCurve> discountCurves) {
      JodaBeanUtils.notNull(discountCurves, "discountCurves");
      this.discountCurves = discountCurves;
      return this;
    }

    /**
     * Sets the {@code indexCurves} property in the builder.
     * @param indexCurves  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder indexCurves(Map<Index, YieldAndDiscountCurve> indexCurves) {
      JodaBeanUtils.notNull(indexCurves, "indexCurves");
      this.indexCurves = indexCurves;
      return this;
    }

    /**
     * Sets the {@code timeSeries} property in the builder.
     * @param timeSeries  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder timeSeries(Map<Index, LocalDateDoubleTimeSeries> timeSeries) {
      JodaBeanUtils.notNull(timeSeries, "timeSeries");
      this.timeSeries = timeSeries;
      return this;
    }

    /**
     * Sets the {@code additionalData} property in the builder.
     * @param additionalData  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder additionalData(Map<Class<?>, Object> additionalData) {
      JodaBeanUtils.notNull(additionalData, "additionalData");
      this.additionalData = additionalData;
      return this;
    }

    /**
     * Sets the {@code dayCount} property in the builder.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(256);
      buf.append("ImmutableRatesProvider.Builder{");
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
      buf.append("fxMatrix").append('=').append(JodaBeanUtils.toString(fxMatrix)).append(',').append(' ');
      buf.append("discountCurves").append('=').append(JodaBeanUtils.toString(discountCurves)).append(',').append(' ');
      buf.append("indexCurves").append('=').append(JodaBeanUtils.toString(indexCurves)).append(',').append(' ');
      buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(timeSeries)).append(',').append(' ');
      buf.append("additionalData").append('=').append(JodaBeanUtils.toString(additionalData)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
