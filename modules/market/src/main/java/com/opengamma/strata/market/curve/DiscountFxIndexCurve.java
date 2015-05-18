/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * Provides access to discount factors for a currency.
 * <p>
 * This provides discount factors for a single currency.
 * <p>
 * This implementation is based on an underlying curve that is stored with maturities
 * and zero-coupon continuously-compounded rates.
 */
@BeanDefinition(builderScope = "private")
public final class DiscountFxIndexCurve
    implements FxIndexCurve, ImmutableBean, Serializable {

  /**
   * The index that the rates are for.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final FxIndex index;
  /**
   * The time-series.
   * This covers known historical fixings and may be empty.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDateDoubleTimeSeries timeSeries;
  /**
   * The provider of FX rates.
   */
  @PropertyDefinition(validate = "notNull")
  private final FxRateProvider fxRateProvider;
  /**
   * The discount factors for the base currency of the index.
   */
  @PropertyDefinition(validate = "notNull")
  private final DiscountFactorCurve baseCurrencyDiscountFactors;
  /**
   * The discount factors for the counter currency of the index.
   */
  @PropertyDefinition(validate = "notNull")
  private final DiscountFactorCurve counterCurrencyDiscountFactors;
  /**
   * The valuation date.
   */
  private final LocalDate valuationDate;  // not a property, derived and cached from input data

  //-------------------------------------------------------------------------
  /**
   * Creates a new FX index rates instance.
   * <p>
   * The instance is based on the dicount factors for each currency.
   * 
   * @param index  the index
   * @param knownFixings  the known historical fixings
   * @param fxRateProvider  the provider of FX rates
   * @param baseCurrencyFactors  the discount factors in the base currency of the index
   * @param counterCurrencyFactors  the discount factors in the counter currency of the index
   * @return the curve
   */
  public static DiscountFxIndexCurve of(
      FxIndex index,
      LocalDateDoubleTimeSeries knownFixings,
      FxRateProvider fxRateProvider,
      DiscountFactorCurve baseCurrencyFactors,
      DiscountFactorCurve counterCurrencyFactors) {

    return new DiscountFxIndexCurve(
        index, knownFixings, fxRateProvider, baseCurrencyFactors, counterCurrencyFactors);
  }

  //-------------------------------------------------------------------------
  // restricted constructor
  @ImmutableConstructor
  private DiscountFxIndexCurve(
      FxIndex index,
      LocalDateDoubleTimeSeries timeSeries,
      FxRateProvider fxRateProvider,
      DiscountFactorCurve baseCurrencyDiscountFactors,
      DiscountFactorCurve counterCurrencyDiscountFactors) {
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(timeSeries, "timeSeries");
    JodaBeanUtils.notNull(fxRateProvider, "fxRateProvider");
    JodaBeanUtils.notNull(baseCurrencyDiscountFactors, "baseCurrencyDiscountFactors");
    JodaBeanUtils.notNull(counterCurrencyDiscountFactors, "counterCurrencyDiscountFactors");
    if (!baseCurrencyDiscountFactors.getValuationDate().equals(counterCurrencyDiscountFactors.getValuationDate())) {
      throw new IllegalArgumentException("Curves must have the same valuation date");
    }
    this.index = index;
    this.timeSeries = timeSeries;
    this.fxRateProvider = fxRateProvider;
    this.baseCurrencyDiscountFactors = baseCurrencyDiscountFactors;
    this.counterCurrencyDiscountFactors = counterCurrencyDiscountFactors;
    this.valuationDate = baseCurrencyDiscountFactors.getValuationDate();
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDate getValuationDate() {
    return valuationDate;
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(Currency baseCurrency, LocalDate fixingDate) {
    ArgChecker.isTrue(
        index.getCurrencyPair().contains(baseCurrency),
        "Currency {} invalid for FxIndex {}", baseCurrency, index);

    double fxIndexRate = !fixingDate.isAfter(valuationDate) ? historicRate(fixingDate) : forwardRate(fixingDate);
    boolean inverse = baseCurrency.equals(index.getCurrencyPair().getCounter());
    return (inverse ? 1d / fxIndexRate : fxIndexRate);
  }

  // historic rate
  private double historicRate(LocalDate fixingDate) {
    OptionalDouble fixedRate = timeSeries.get(fixingDate);
    if (fixedRate.isPresent()) {
      return fixedRate.getAsDouble();
    } else if (fixingDate.isBefore(valuationDate)) { // the fixing is required
      throw new IllegalArgumentException(Messages.format("Unable to get fixing for {} on date {}", index, fixingDate));
    } else {
      return forwardRate(fixingDate);
    }
  }

  // forward rate
  private double forwardRate(LocalDate fixingDate) {
    // derive rate from discount factors based off index currency pair
    // inverse dealt with outside this method
    LocalDate maturityDate = index.calculateMaturityFromFixing(fixingDate);
    double dfCcyBaseAtMaturity = baseCurrencyDiscountFactors.discountFactor(maturityDate);
    double dfCcyCounterAtMaturity = counterCurrencyDiscountFactors.discountFactor(maturityDate);
    return fxRateProvider.fxRate(index.getCurrencyPair()) * (dfCcyBaseAtMaturity / dfCcyCounterAtMaturity);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code DiscountFxIndexCurve}.
   * @return the meta-bean, not null
   */
  public static DiscountFxIndexCurve.Meta meta() {
    return DiscountFxIndexCurve.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(DiscountFxIndexCurve.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public DiscountFxIndexCurve.Meta metaBean() {
    return DiscountFxIndexCurve.Meta.INSTANCE;
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
   * Gets the index that the rates are for.
   * @return the value of the property, not null
   */
  @Override
  public FxIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the time-series.
   * This covers known historical fixings and may be empty.
   * @return the value of the property, not null
   */
  public LocalDateDoubleTimeSeries getTimeSeries() {
    return timeSeries;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the provider of FX rates.
   * @return the value of the property, not null
   */
  public FxRateProvider getFxRateProvider() {
    return fxRateProvider;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the discount factors for the base currency of the index.
   * @return the value of the property, not null
   */
  public DiscountFactorCurve getBaseCurrencyDiscountFactors() {
    return baseCurrencyDiscountFactors;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the discount factors for the counter currency of the index.
   * @return the value of the property, not null
   */
  public DiscountFactorCurve getCounterCurrencyDiscountFactors() {
    return counterCurrencyDiscountFactors;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      DiscountFxIndexCurve other = (DiscountFxIndexCurve) obj;
      return JodaBeanUtils.equal(getIndex(), other.getIndex()) &&
          JodaBeanUtils.equal(getTimeSeries(), other.getTimeSeries()) &&
          JodaBeanUtils.equal(getFxRateProvider(), other.getFxRateProvider()) &&
          JodaBeanUtils.equal(getBaseCurrencyDiscountFactors(), other.getBaseCurrencyDiscountFactors()) &&
          JodaBeanUtils.equal(getCounterCurrencyDiscountFactors(), other.getCounterCurrencyDiscountFactors());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getIndex());
    hash = hash * 31 + JodaBeanUtils.hashCode(getTimeSeries());
    hash = hash * 31 + JodaBeanUtils.hashCode(getFxRateProvider());
    hash = hash * 31 + JodaBeanUtils.hashCode(getBaseCurrencyDiscountFactors());
    hash = hash * 31 + JodaBeanUtils.hashCode(getCounterCurrencyDiscountFactors());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("DiscountFxIndexCurve{");
    buf.append("index").append('=').append(getIndex()).append(',').append(' ');
    buf.append("timeSeries").append('=').append(getTimeSeries()).append(',').append(' ');
    buf.append("fxRateProvider").append('=').append(getFxRateProvider()).append(',').append(' ');
    buf.append("baseCurrencyDiscountFactors").append('=').append(getBaseCurrencyDiscountFactors()).append(',').append(' ');
    buf.append("counterCurrencyDiscountFactors").append('=').append(JodaBeanUtils.toString(getCounterCurrencyDiscountFactors()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code DiscountFxIndexCurve}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<FxIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", DiscountFxIndexCurve.class, FxIndex.class);
    /**
     * The meta-property for the {@code timeSeries} property.
     */
    private final MetaProperty<LocalDateDoubleTimeSeries> timeSeries = DirectMetaProperty.ofImmutable(
        this, "timeSeries", DiscountFxIndexCurve.class, LocalDateDoubleTimeSeries.class);
    /**
     * The meta-property for the {@code fxRateProvider} property.
     */
    private final MetaProperty<FxRateProvider> fxRateProvider = DirectMetaProperty.ofImmutable(
        this, "fxRateProvider", DiscountFxIndexCurve.class, FxRateProvider.class);
    /**
     * The meta-property for the {@code baseCurrencyDiscountFactors} property.
     */
    private final MetaProperty<DiscountFactorCurve> baseCurrencyDiscountFactors = DirectMetaProperty.ofImmutable(
        this, "baseCurrencyDiscountFactors", DiscountFxIndexCurve.class, DiscountFactorCurve.class);
    /**
     * The meta-property for the {@code counterCurrencyDiscountFactors} property.
     */
    private final MetaProperty<DiscountFactorCurve> counterCurrencyDiscountFactors = DirectMetaProperty.ofImmutable(
        this, "counterCurrencyDiscountFactors", DiscountFxIndexCurve.class, DiscountFactorCurve.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "timeSeries",
        "fxRateProvider",
        "baseCurrencyDiscountFactors",
        "counterCurrencyDiscountFactors");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case 779431844:  // timeSeries
          return timeSeries;
        case -1499624221:  // fxRateProvider
          return fxRateProvider;
        case 1151357473:  // baseCurrencyDiscountFactors
          return baseCurrencyDiscountFactors;
        case -453959018:  // counterCurrencyDiscountFactors
          return counterCurrencyDiscountFactors;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends DiscountFxIndexCurve> builder() {
      return new DiscountFxIndexCurve.Builder();
    }

    @Override
    public Class<? extends DiscountFxIndexCurve> beanType() {
      return DiscountFxIndexCurve.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code timeSeries} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDateDoubleTimeSeries> timeSeries() {
      return timeSeries;
    }

    /**
     * The meta-property for the {@code fxRateProvider} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxRateProvider> fxRateProvider() {
      return fxRateProvider;
    }

    /**
     * The meta-property for the {@code baseCurrencyDiscountFactors} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DiscountFactorCurve> baseCurrencyDiscountFactors() {
      return baseCurrencyDiscountFactors;
    }

    /**
     * The meta-property for the {@code counterCurrencyDiscountFactors} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DiscountFactorCurve> counterCurrencyDiscountFactors() {
      return counterCurrencyDiscountFactors;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return ((DiscountFxIndexCurve) bean).getIndex();
        case 779431844:  // timeSeries
          return ((DiscountFxIndexCurve) bean).getTimeSeries();
        case -1499624221:  // fxRateProvider
          return ((DiscountFxIndexCurve) bean).getFxRateProvider();
        case 1151357473:  // baseCurrencyDiscountFactors
          return ((DiscountFxIndexCurve) bean).getBaseCurrencyDiscountFactors();
        case -453959018:  // counterCurrencyDiscountFactors
          return ((DiscountFxIndexCurve) bean).getCounterCurrencyDiscountFactors();
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
   * The bean-builder for {@code DiscountFxIndexCurve}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<DiscountFxIndexCurve> {

    private FxIndex index;
    private LocalDateDoubleTimeSeries timeSeries;
    private FxRateProvider fxRateProvider;
    private DiscountFactorCurve baseCurrencyDiscountFactors;
    private DiscountFactorCurve counterCurrencyDiscountFactors;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case 779431844:  // timeSeries
          return timeSeries;
        case -1499624221:  // fxRateProvider
          return fxRateProvider;
        case 1151357473:  // baseCurrencyDiscountFactors
          return baseCurrencyDiscountFactors;
        case -453959018:  // counterCurrencyDiscountFactors
          return counterCurrencyDiscountFactors;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          this.index = (FxIndex) newValue;
          break;
        case 779431844:  // timeSeries
          this.timeSeries = (LocalDateDoubleTimeSeries) newValue;
          break;
        case -1499624221:  // fxRateProvider
          this.fxRateProvider = (FxRateProvider) newValue;
          break;
        case 1151357473:  // baseCurrencyDiscountFactors
          this.baseCurrencyDiscountFactors = (DiscountFactorCurve) newValue;
          break;
        case -453959018:  // counterCurrencyDiscountFactors
          this.counterCurrencyDiscountFactors = (DiscountFactorCurve) newValue;
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
    public DiscountFxIndexCurve build() {
      return new DiscountFxIndexCurve(
          index,
          timeSeries,
          fxRateProvider,
          baseCurrencyDiscountFactors,
          counterCurrencyDiscountFactors);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("DiscountFxIndexCurve.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(timeSeries)).append(',').append(' ');
      buf.append("fxRateProvider").append('=').append(JodaBeanUtils.toString(fxRateProvider)).append(',').append(' ');
      buf.append("baseCurrencyDiscountFactors").append('=').append(JodaBeanUtils.toString(baseCurrencyDiscountFactors)).append(',').append(' ');
      buf.append("counterCurrencyDiscountFactors").append('=').append(JodaBeanUtils.toString(counterCurrencyDiscountFactors));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
