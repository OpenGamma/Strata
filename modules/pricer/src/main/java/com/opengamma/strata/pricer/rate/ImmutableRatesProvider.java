/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.fx.DiscountFxForwardRates;
import com.opengamma.strata.pricer.fx.ForwardFxIndexRates;
import com.opengamma.strata.pricer.fx.FxForwardRates;
import com.opengamma.strata.pricer.fx.FxIndexRates;

/**
 * The default immutable rates provider, used to calculate analytic measures.
 * <p>
 * This provides the environmental information against which pricing occurs.
 * This includes FX rates, discount factors and forward curves.
 */
@BeanDefinition(builderScope = "private", constructorScope = "package")
public final class ImmutableRatesProvider
    implements RatesProvider, ImmutableBean, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The valuation date.
   * All curves and other data items in this provider are calibrated for this date.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate valuationDate;
  /**
   * The provider of foreign exchange rates.
   * Conversions where both currencies are the same always succeed.
   */
  @PropertyDefinition(validate = "notNull")
  private final FxRateProvider fxRateProvider;
  /**
   * The discount curves, defaulted to an empty map.
   * The curve data, predicting the future, associated with each currency.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<Currency, Curve> discountCurves;
  /**
   * The forward curves, defaulted to an empty map.
   * The curve data, predicting the future, associated with each index.
   * This is used for Ibor, Overnight and Price indices.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<Index, Curve> indexCurves;
  /**
   * The time-series, defaulted to an empty map.
   * The historic data associated with each index.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<Index, LocalDateDoubleTimeSeries> timeSeries;

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.fxRateProvider = FxMatrix.empty();
  }

  //-------------------------------------------------------------------------
  /**
   * Combines a number of rates providers.
   * <p>
   * If the two providers have curves or time series for the same currency or index, 
   * an {@link IllegalAccessException} is thrown.
   * The FxRateProviders is not populated with the given provider; no attempt is done on merging the embedded FX providers.
   * 
   * @param fx  the FX provider for the resulting rate provider
   * @param providers  the rates providers to be merged
   * @return the combined rates provider
   */
  public static ImmutableRatesProvider combined(FxRateProvider fx, ImmutableRatesProvider... providers) {
    ArgChecker.isTrue(providers.length > 0, "at least one provider requested");
    ImmutableRatesProvider merged = ImmutableRatesProvider.builder(providers[0].getValuationDate()).build();
    for (ImmutableRatesProvider provider : providers) {
      merged = merged.combinedWith(provider, fx);
    }
    return merged;
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a builder specifying the valuation date.
   * 
   * @param valuationDate  the valuation date
   * @return the builder
   */
  public static ImmutableRatesProviderBuilder builder(LocalDate valuationDate) {
    return new ImmutableRatesProviderBuilder(valuationDate);
  }

  /**
   * Converts this instance to a builder allowing changes to be made.
   * 
   * @return the builder
   */
  public ImmutableRatesProviderBuilder toBuilder() {
    return new ImmutableRatesProviderBuilder(valuationDate)
        .fxRateProvider(fxRateProvider)
        .discountCurves(discountCurves)
        .indexCurves(indexCurves)
        .timeSeries(timeSeries);
  }

  //-------------------------------------------------------------------------
  @Override
  public ImmutableSet<Currency> getDiscountCurrencies() {
    return discountCurves.keySet();
  }

  @Override
  public ImmutableSet<IborIndex> getIborIndices() {
    return indexCurves.keySet().stream()
        .filter(IborIndex.class::isInstance)
        .map(IborIndex.class::cast)
        .collect(toImmutableSet());
  }

  @Override
  public ImmutableSet<OvernightIndex> getOvernightIndices() {
    return indexCurves.keySet().stream()
        .filter(OvernightIndex.class::isInstance)
        .map(OvernightIndex.class::cast)
        .collect(toImmutableSet());
  }

  @Override
  public ImmutableSet<PriceIndex> getPriceIndices() {
    return indexCurves.keySet().stream()
        .filter(PriceIndex.class::isInstance)
        .map(PriceIndex.class::cast)
        .collect(toImmutableSet());
  }

  //-------------------------------------------------------------------------
  @Override
  public <T> Optional<T> findData(MarketDataName<T> name) {
    if (name instanceof CurveName) {
      return Stream.concat(discountCurves.values().stream(), indexCurves.values().stream())
          .filter(c -> c.getName().equals(name))
          .map(v -> name.getMarketDataType().cast(v))
          .findFirst();
    }
    return Optional.empty();
  }

  //-------------------------------------------------------------------------
  @Override
  public <T> T data(MarketDataId<T> id) {
    throw new IllegalArgumentException("Unknown identifier: " + id.toString());
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDateDoubleTimeSeries timeSeries(Index index) {
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
    return fxRateProvider.fxRate(baseCurrency, counterCurrency);
  }

  //-------------------------------------------------------------------------
  @Override
  public DiscountFactors discountFactors(Currency currency) {
    Curve curve = discountCurves.get(currency);
    if (curve == null) {
      throw new IllegalArgumentException("Unable to find discount curve: " + currency);
    }
    return DiscountFactors.of(currency, valuationDate, curve);
  }

  //-------------------------------------------------------------------------
  @Override
  public FxIndexRates fxIndexRates(FxIndex index) {
    LocalDateDoubleTimeSeries fixings = timeSeries(index);
    FxForwardRates fxForwardRates = fxForwardRates(index.getCurrencyPair());
    return ForwardFxIndexRates.of(index, fxForwardRates, fixings);
  }

  //-------------------------------------------------------------------------
  @Override
  public FxForwardRates fxForwardRates(CurrencyPair currencyPair) {
    DiscountFactors base = discountFactors(currencyPair.getBase());
    DiscountFactors counter = discountFactors(currencyPair.getCounter());
    return DiscountFxForwardRates.of(currencyPair, fxRateProvider, base, counter);
  };

  //-------------------------------------------------------------------------
  @Override
  public IborIndexRates iborIndexRates(IborIndex index) {
    LocalDateDoubleTimeSeries fixings = timeSeries(index);
    Curve curve = indexCurve(index);
    return IborIndexRates.of(index, valuationDate, curve, fixings);
  }

  @Override
  public OvernightIndexRates overnightIndexRates(OvernightIndex index) {
    LocalDateDoubleTimeSeries fixings = timeSeries(index);
    Curve curve = indexCurve(index);
    return OvernightIndexRates.of(index, valuationDate, curve, fixings);
  }

  @Override
  public PriceIndexValues priceIndexValues(PriceIndex index) {
    LocalDateDoubleTimeSeries fixings = timeSeries(index);
    Curve curve = indexCurve(index);
    return PriceIndexValues.of(index, valuationDate, curve, fixings);
  }

  //-------------------------------------------------------------------------
  /**
   * Combines this provider with another.
   * <p> 
   * If the two providers have curves or time series for the same currency or index,
   * an {@link IllegalAccessException} is thrown. No attempt is made to combine the
   * FX providers, instead one is supplied.
   * 
   * @param other  the other rates provider
   * @param fxProvider  the FX rate provider to use
   * @return the combined provider
   */
  public ImmutableRatesProvider combinedWith(ImmutableRatesProvider other, FxRateProvider fxProvider) {
    ImmutableRatesProviderBuilder merged = other.toBuilder();
    // discount
    ImmutableMap<Currency, Curve> dscMap1 = discountCurves;
    ImmutableMap<Currency, Curve> dscMap2 = other.discountCurves;
    for (Entry<Currency, Curve> entry : dscMap1.entrySet()) {
      ArgChecker.isTrue(!dscMap2.containsKey(entry.getKey()),
          "conflict on discount curve, currency '{}' appears twice in the providers", entry.getKey());
      merged.discountCurve(entry.getKey(), entry.getValue());
    }
    // forward
    ImmutableMap<Index, Curve> indexMap1 = indexCurves;
    ImmutableMap<Index, Curve> indexMap2 = other.indexCurves;
    for (Entry<Index, Curve> entry : indexMap1.entrySet()) {
      ArgChecker.isTrue(!indexMap2.containsKey(entry.getKey()),
          "conflict on index curve, index '{}' appears twice in the providers", entry.getKey());
      merged.indexCurve(entry.getKey(), entry.getValue());
    }
    // time series
    Map<Index, LocalDateDoubleTimeSeries> tsMap1 = timeSeries;
    Map<Index, LocalDateDoubleTimeSeries> tsMap2 = other.timeSeries;
    for (Entry<Index, LocalDateDoubleTimeSeries> entry : tsMap1.entrySet()) {
      ArgChecker.isTrue(!tsMap2.containsKey(entry.getKey()),
          "conflict on time series, index '{}' appears twice in the providers", entry.getKey());
      merged.timeSeries(entry.getKey(), entry.getValue());
    }
    merged.fxRateProvider(fxProvider);
    return merged.build();
  }

  //-------------------------------------------------------------------------
  @Override
  public ImmutableRatesProvider toImmutableRatesProvider() {
    return this;
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
   * Creates an instance.
   * @param valuationDate  the value of the property, not null
   * @param fxRateProvider  the value of the property, not null
   * @param discountCurves  the value of the property, not null
   * @param indexCurves  the value of the property, not null
   * @param timeSeries  the value of the property, not null
   */
  ImmutableRatesProvider(
      LocalDate valuationDate,
      FxRateProvider fxRateProvider,
      Map<Currency, Curve> discountCurves,
      Map<Index, Curve> indexCurves,
      Map<Index, LocalDateDoubleTimeSeries> timeSeries) {
    JodaBeanUtils.notNull(valuationDate, "valuationDate");
    JodaBeanUtils.notNull(fxRateProvider, "fxRateProvider");
    JodaBeanUtils.notNull(discountCurves, "discountCurves");
    JodaBeanUtils.notNull(indexCurves, "indexCurves");
    JodaBeanUtils.notNull(timeSeries, "timeSeries");
    this.valuationDate = valuationDate;
    this.fxRateProvider = fxRateProvider;
    this.discountCurves = ImmutableMap.copyOf(discountCurves);
    this.indexCurves = ImmutableMap.copyOf(indexCurves);
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
   * Gets the provider of foreign exchange rates.
   * Conversions where both currencies are the same always succeed.
   * @return the value of the property, not null
   */
  public FxRateProvider getFxRateProvider() {
    return fxRateProvider;
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
   * This is used for Ibor, Overnight and Price indices.
   * @return the value of the property, not null
   */
  public ImmutableMap<Index, Curve> getIndexCurves() {
    return indexCurves;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the time-series, defaulted to an empty map.
   * The historic data associated with each index.
   * @return the value of the property, not null
   */
  public ImmutableMap<Index, LocalDateDoubleTimeSeries> getTimeSeries() {
    return timeSeries;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ImmutableRatesProvider other = (ImmutableRatesProvider) obj;
      return JodaBeanUtils.equal(valuationDate, other.valuationDate) &&
          JodaBeanUtils.equal(fxRateProvider, other.fxRateProvider) &&
          JodaBeanUtils.equal(discountCurves, other.discountCurves) &&
          JodaBeanUtils.equal(indexCurves, other.indexCurves) &&
          JodaBeanUtils.equal(timeSeries, other.timeSeries);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(fxRateProvider);
    hash = hash * 31 + JodaBeanUtils.hashCode(discountCurves);
    hash = hash * 31 + JodaBeanUtils.hashCode(indexCurves);
    hash = hash * 31 + JodaBeanUtils.hashCode(timeSeries);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("ImmutableRatesProvider{");
    buf.append("valuationDate").append('=').append(valuationDate).append(',').append(' ');
    buf.append("fxRateProvider").append('=').append(fxRateProvider).append(',').append(' ');
    buf.append("discountCurves").append('=').append(discountCurves).append(',').append(' ');
    buf.append("indexCurves").append('=').append(indexCurves).append(',').append(' ');
    buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(timeSeries));
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
     * The meta-property for the {@code fxRateProvider} property.
     */
    private final MetaProperty<FxRateProvider> fxRateProvider = DirectMetaProperty.ofImmutable(
        this, "fxRateProvider", ImmutableRatesProvider.class, FxRateProvider.class);
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
        "fxRateProvider",
        "discountCurves",
        "indexCurves",
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
        case -1499624221:  // fxRateProvider
          return fxRateProvider;
        case -624113147:  // discountCurves
          return discountCurves;
        case 886361302:  // indexCurves
          return indexCurves;
        case 779431844:  // timeSeries
          return timeSeries;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends ImmutableRatesProvider> builder() {
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
     * The meta-property for the {@code fxRateProvider} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxRateProvider> fxRateProvider() {
      return fxRateProvider;
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
        case -1499624221:  // fxRateProvider
          return ((ImmutableRatesProvider) bean).getFxRateProvider();
        case -624113147:  // discountCurves
          return ((ImmutableRatesProvider) bean).getDiscountCurves();
        case 886361302:  // indexCurves
          return ((ImmutableRatesProvider) bean).getIndexCurves();
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
  private static final class Builder extends DirectPrivateBeanBuilder<ImmutableRatesProvider> {

    private LocalDate valuationDate;
    private FxRateProvider fxRateProvider;
    private Map<Currency, Curve> discountCurves = ImmutableMap.of();
    private Map<Index, Curve> indexCurves = ImmutableMap.of();
    private Map<Index, LocalDateDoubleTimeSeries> timeSeries = ImmutableMap.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
      super(meta());
      applyDefaults(this);
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          return valuationDate;
        case -1499624221:  // fxRateProvider
          return fxRateProvider;
        case -624113147:  // discountCurves
          return discountCurves;
        case 886361302:  // indexCurves
          return indexCurves;
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
        case -1499624221:  // fxRateProvider
          this.fxRateProvider = (FxRateProvider) newValue;
          break;
        case -624113147:  // discountCurves
          this.discountCurves = (Map<Currency, Curve>) newValue;
          break;
        case 886361302:  // indexCurves
          this.indexCurves = (Map<Index, Curve>) newValue;
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
    public ImmutableRatesProvider build() {
      return new ImmutableRatesProvider(
          valuationDate,
          fxRateProvider,
          discountCurves,
          indexCurves,
          timeSeries);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("ImmutableRatesProvider.Builder{");
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
      buf.append("fxRateProvider").append('=').append(JodaBeanUtils.toString(fxRateProvider)).append(',').append(' ');
      buf.append("discountCurves").append('=').append(JodaBeanUtils.toString(discountCurves)).append(',').append(' ');
      buf.append("indexCurves").append('=').append(JodaBeanUtils.toString(indexCurves)).append(',').append(' ');
      buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(timeSeries));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
