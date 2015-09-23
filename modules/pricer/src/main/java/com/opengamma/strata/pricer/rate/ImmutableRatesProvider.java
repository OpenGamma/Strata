/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.DiscountFxForwardRates;
import com.opengamma.strata.market.value.DiscountFxIndexRates;
import com.opengamma.strata.market.value.DiscountIborIndexRates;
import com.opengamma.strata.market.value.DiscountOvernightIndexRates;
import com.opengamma.strata.market.value.FxForwardRates;
import com.opengamma.strata.market.value.FxIndexRates;
import com.opengamma.strata.market.value.IborIndexRates;
import com.opengamma.strata.market.value.OvernightIndexRates;
import com.opengamma.strata.market.value.PriceIndexValues;
import com.opengamma.strata.market.value.SimpleDiscountFactors;
import com.opengamma.strata.market.value.ValueType;
import com.opengamma.strata.market.value.ZeroRateDiscountFactors;

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
  private final ImmutableMap<Currency, Curve> discountCurves;
  /**
   * The forward curves, defaulted to an empty map.
   * The curve data, predicting the future, associated with each index.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<Index, Curve> indexCurves;
  /**
   * The price index values, defaulted to an empty map.
   * The curve data, predicting the future, associated with each index.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<PriceIndex, PriceIndexValues> priceIndexValues;
  /**
   * The time-series, defaulted to an empty map.
   * The historic data associated with each index.
   */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final ImmutableMap<Index, LocalDateDoubleTimeSeries> timeSeries;

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.fxMatrix = FxMatrix.empty();
  }

  //-------------------------------------------------------------------------
  @Override
  public <T> T data(MarketDataKey<T> key) {
    throw new IllegalArgumentException("Unknown key: " + key.toString());
  }

  //-------------------------------------------------------------------------
  // finds the time-series
  private LocalDateDoubleTimeSeries timeSeries(Index index) {
    return timeSeries.getOrDefault(index, LocalDateDoubleTimeSeries.empty());
  }

  // finds the index curve
  private Curve indexCurve(Index index) {
    Curve curve = indexCurves.get(index);
    if (curve == null) {
      throw new IllegalArgumentException("Unable to find index curve: " + index);
    }
    return curve;
  }

  //-------------------------------------------------------------------------
  @Override
  public double fxRate(Currency baseCurrency, Currency counterCurrency) {
    if (baseCurrency.equals(counterCurrency)) {
      return 1d;
    }
    return fxMatrix.fxRate(baseCurrency, counterCurrency);
  }

  //-------------------------------------------------------------------------
  @Override
  public DiscountFactors discountFactors(Currency currency) {
    Curve curve = discountCurves.get(currency);
    if (curve == null) {
      throw new IllegalArgumentException("Unable to find discount curve: " + currency);
    }
    if (curve.getMetadata().getYValueType().equals(ValueType.DISCOUNT_FACTOR)) {
      return SimpleDiscountFactors.of(currency, valuationDate, curve);
    }
    return ZeroRateDiscountFactors.of(currency, valuationDate, curve);
  }

  //-------------------------------------------------------------------------
  @Override
  public FxIndexRates fxIndexRates(FxIndex index) {
    LocalDateDoubleTimeSeries timeSeries = timeSeries(index);
    FxForwardRates fxForwardRates = fxForwardRates(index.getCurrencyPair());
    return DiscountFxIndexRates.of(index, timeSeries, fxForwardRates);
  }

  //-------------------------------------------------------------------------
  @Override
  public FxForwardRates fxForwardRates(CurrencyPair currencyPair) {
    DiscountFactors base = discountFactors(currencyPair.getBase());
    DiscountFactors counter = discountFactors(currencyPair.getCounter());
    return DiscountFxForwardRates.of(currencyPair, fxMatrix, base, counter);
  };

  //-------------------------------------------------------------------------
  @Override
  public IborIndexRates iborIndexRates(IborIndex index) {
    LocalDateDoubleTimeSeries timeSeries = timeSeries(index);
    Curve curve = indexCurve(index);
    DiscountFactors dfc = ZeroRateDiscountFactors.of(index.getCurrency(), getValuationDate(), curve);
    return DiscountIborIndexRates.of(index, timeSeries, dfc);
  }

  @Override
  public OvernightIndexRates overnightIndexRates(OvernightIndex index) {
    LocalDateDoubleTimeSeries timeSeries = timeSeries(index);
    Curve curve = indexCurve(index);
    DiscountFactors dfc = ZeroRateDiscountFactors.of(index.getCurrency(), getValuationDate(), curve);
    return DiscountOvernightIndexRates.of(index, timeSeries, dfc);
  }

  @Override
  public PriceIndexValues priceIndexValues(PriceIndex index) {
    PriceIndexValues values = priceIndexValues.get(index);
    if (values == null) {
      throw new IllegalArgumentException("Unable to find index: " + index);
    }
    return values;
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
      Map<Currency, Curve> discountCurves,
      Map<Index, Curve> indexCurves,
      Map<PriceIndex, PriceIndexValues> priceIndexValues,
      Map<Index, LocalDateDoubleTimeSeries> timeSeries) {
    JodaBeanUtils.notNull(valuationDate, "valuationDate");
    JodaBeanUtils.notNull(fxMatrix, "fxMatrix");
    JodaBeanUtils.notNull(discountCurves, "discountCurves");
    JodaBeanUtils.notNull(indexCurves, "indexCurves");
    JodaBeanUtils.notNull(priceIndexValues, "priceIndexValues");
    JodaBeanUtils.notNull(timeSeries, "timeSeries");
    this.valuationDate = valuationDate;
    this.fxMatrix = fxMatrix;
    this.discountCurves = ImmutableMap.copyOf(discountCurves);
    this.indexCurves = ImmutableMap.copyOf(indexCurves);
    this.priceIndexValues = ImmutableMap.copyOf(priceIndexValues);
    this.timeSeries = ImmutableMap.copyOf(timeSeries);
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
  public ImmutableMap<Currency, Curve> getDiscountCurves() {
    return discountCurves;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the forward curves, defaulted to an empty map.
   * The curve data, predicting the future, associated with each index.
   * @return the value of the property, not null
   */
  public ImmutableMap<Index, Curve> getIndexCurves() {
    return indexCurves;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the price index values, defaulted to an empty map.
   * The curve data, predicting the future, associated with each index.
   * @return the value of the property, not null
   */
  public ImmutableMap<PriceIndex, PriceIndexValues> getPriceIndexValues() {
    return priceIndexValues;
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
          JodaBeanUtils.equal(getPriceIndexValues(), other.getPriceIndexValues()) &&
          JodaBeanUtils.equal(getTimeSeries(), other.getTimeSeries());
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
    hash = hash * 31 + JodaBeanUtils.hashCode(getPriceIndexValues());
    hash = hash * 31 + JodaBeanUtils.hashCode(getTimeSeries());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("ImmutableRatesProvider{");
    buf.append("valuationDate").append('=').append(getValuationDate()).append(',').append(' ');
    buf.append("fxMatrix").append('=').append(getFxMatrix()).append(',').append(' ');
    buf.append("discountCurves").append('=').append(getDiscountCurves()).append(',').append(' ');
    buf.append("indexCurves").append('=').append(getIndexCurves()).append(',').append(' ');
    buf.append("priceIndexValues").append('=').append(getPriceIndexValues()).append(',').append(' ');
    buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(getTimeSeries()));
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
    private final MetaProperty<ImmutableMap<Currency, Curve>> discountCurves = DirectMetaProperty.ofImmutable(
        this, "discountCurves", ImmutableRatesProvider.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code indexCurves} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<Index, Curve>> indexCurves = DirectMetaProperty.ofImmutable(
        this, "indexCurves", ImmutableRatesProvider.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code priceIndexValues} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<PriceIndex, PriceIndexValues>> priceIndexValues = DirectMetaProperty.ofImmutable(
        this, "priceIndexValues", ImmutableRatesProvider.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code timeSeries} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<Index, LocalDateDoubleTimeSeries>> timeSeries = DirectMetaProperty.ofImmutable(
        this, "timeSeries", ImmutableRatesProvider.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "valuationDate",
        "fxMatrix",
        "discountCurves",
        "indexCurves",
        "priceIndexValues",
        "timeSeries");

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
        case 1422773131:  // priceIndexValues
          return priceIndexValues;
        case 779431844:  // timeSeries
          return timeSeries;
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
    public MetaProperty<ImmutableMap<Currency, Curve>> discountCurves() {
      return discountCurves;
    }

    /**
     * The meta-property for the {@code indexCurves} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<Index, Curve>> indexCurves() {
      return indexCurves;
    }

    /**
     * The meta-property for the {@code priceIndexValues} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<PriceIndex, PriceIndexValues>> priceIndexValues() {
      return priceIndexValues;
    }

    /**
     * The meta-property for the {@code timeSeries} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<Index, LocalDateDoubleTimeSeries>> timeSeries() {
      return timeSeries;
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
        case 1422773131:  // priceIndexValues
          return ((ImmutableRatesProvider) bean).getPriceIndexValues();
        case 779431844:  // timeSeries
          return ((ImmutableRatesProvider) bean).getTimeSeries();
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
    private Map<Currency, Curve> discountCurves = ImmutableMap.of();
    private Map<Index, Curve> indexCurves = ImmutableMap.of();
    private Map<PriceIndex, PriceIndexValues> priceIndexValues = ImmutableMap.of();
    private Map<Index, LocalDateDoubleTimeSeries> timeSeries = ImmutableMap.of();

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
      this.priceIndexValues = beanToCopy.getPriceIndexValues();
      this.timeSeries = beanToCopy.getTimeSeries();
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
        case 1422773131:  // priceIndexValues
          return priceIndexValues;
        case 779431844:  // timeSeries
          return timeSeries;
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
          this.discountCurves = (Map<Currency, Curve>) newValue;
          break;
        case 886361302:  // indexCurves
          this.indexCurves = (Map<Index, Curve>) newValue;
          break;
        case 1422773131:  // priceIndexValues
          this.priceIndexValues = (Map<PriceIndex, PriceIndexValues>) newValue;
          break;
        case 779431844:  // timeSeries
          this.timeSeries = (Map<Index, LocalDateDoubleTimeSeries>) newValue;
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
          priceIndexValues,
          timeSeries);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the valuation date.
     * All curves and other data items in this provider are calibrated for this date.
     * @param valuationDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder valuationDate(LocalDate valuationDate) {
      JodaBeanUtils.notNull(valuationDate, "valuationDate");
      this.valuationDate = valuationDate;
      return this;
    }

    /**
     * Sets the matrix of foreign exchange rates, defaulted to an empty matrix.
     * @param fxMatrix  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fxMatrix(FxMatrix fxMatrix) {
      JodaBeanUtils.notNull(fxMatrix, "fxMatrix");
      this.fxMatrix = fxMatrix;
      return this;
    }

    /**
     * Sets the discount curves, defaulted to an empty map.
     * The curve data, predicting the future, associated with each currency.
     * @param discountCurves  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder discountCurves(Map<Currency, Curve> discountCurves) {
      JodaBeanUtils.notNull(discountCurves, "discountCurves");
      this.discountCurves = discountCurves;
      return this;
    }

    /**
     * Sets the forward curves, defaulted to an empty map.
     * The curve data, predicting the future, associated with each index.
     * @param indexCurves  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder indexCurves(Map<Index, Curve> indexCurves) {
      JodaBeanUtils.notNull(indexCurves, "indexCurves");
      this.indexCurves = indexCurves;
      return this;
    }

    /**
     * Sets the price index values, defaulted to an empty map.
     * The curve data, predicting the future, associated with each index.
     * @param priceIndexValues  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder priceIndexValues(Map<PriceIndex, PriceIndexValues> priceIndexValues) {
      JodaBeanUtils.notNull(priceIndexValues, "priceIndexValues");
      this.priceIndexValues = priceIndexValues;
      return this;
    }

    /**
     * Sets the time-series, defaulted to an empty map.
     * The historic data associated with each index.
     * @param timeSeries  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder timeSeries(Map<Index, LocalDateDoubleTimeSeries> timeSeries) {
      JodaBeanUtils.notNull(timeSeries, "timeSeries");
      this.timeSeries = timeSeries;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("ImmutableRatesProvider.Builder{");
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
      buf.append("fxMatrix").append('=').append(JodaBeanUtils.toString(fxMatrix)).append(',').append(' ');
      buf.append("discountCurves").append('=').append(JodaBeanUtils.toString(discountCurves)).append(',').append(' ');
      buf.append("indexCurves").append('=').append(JodaBeanUtils.toString(indexCurves)).append(',').append(' ');
      buf.append("priceIndexValues").append('=').append(JodaBeanUtils.toString(priceIndexValues)).append(',').append(' ');
      buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(timeSeries));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
