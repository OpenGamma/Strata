/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.collect.Guavate.ensureOnlyOne;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Stream;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxConvertible;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.surface.Surface;

/**
 * Currency-based parameter sensitivity for parameterized market data, such as a curve.
 * <p>
 * Parameter sensitivity is the sensitivity of a value to the parameters of a
 * {@linkplain ParameterizedData parameterized market data} object that is used to determine the value.
 * Common {@code ParameterizedData} implementations include {@link Curve} and {@link Surface}.
 * <p>
 * The sensitivity is expressed as an array, with one entry for each parameter in the {@code ParameterizedData}.
 * The sensitivity represents a monetary value in the specified currency.
 * <p>
 * A single {@code CurrencyParameterSensitivity} represents the sensitivity to a single {@code ParameterizedData} instance.
 * However, a {@code ParameterizedData} instance can itself be backed by more than one underlying instance.
 * For example, a curve formed from two underlying curves.
 * Information about the split between these underlying instances can optionally be stored.
 */
@BeanDefinition(builderScope = "private")
public final class CurrencyParameterSensitivity
    implements FxConvertible<CurrencyParameterSensitivity>, ImmutableBean, Serializable {

  /**
   * The market data name.
   * <p>
   * This name is used in the market data system to identify the data that the sensitivities refer to.
   */
  @PropertyDefinition(validate = "notNull")
  private final MarketDataName<?> marketDataName;
  /**
   * The list of parameter metadata.
   * <p>
   * There is one entry for each parameter.
   */
  @PropertyDefinition(validate = "notNull", builderType = "List<? extends ParameterMetadata>")
  private final ImmutableList<ParameterMetadata> parameterMetadata;
  /**
   * The currency of the sensitivity.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency currency;
  /**
   * The parameter sensitivity values.
   * <p>
   * There is one sensitivity value for each parameter.
   */
  @PropertyDefinition(validate = "notNull")
  private final DoubleArray sensitivity;
  /**
   * The split of parameters between the underlying parameterized data.
   * <p>
   * A single {@code CurrencyParameterSensitivity} represents the sensitivity to a single {@link ParameterizedData} instance.
   * However, a {@code ParameterizedData} instance can itself be backed by more than one underlying instance.
   * For example, a curve formed from two underlying curves.
   * This list is present, it represents how to split this sensitivity between the underlying instances.
   */
  @PropertyDefinition(get = "optional", type = "List<>")
  private final ImmutableList<ParameterSize> parameterSplit;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the market data name, metadata, currency and sensitivity.
   * <p>
   * The market data name identifies the {@link ParameterizedData} instance that was queried.
   * The parameter metadata provides information on each parameter.
   * The size of the parameter metadata list must match the size of the sensitivity array.
   * 
   * @param marketDataName  the name of the market data that the sensitivity refers to
   * @param parameterMetadata  the parameter metadata
   * @param currency  the currency of the sensitivity
   * @param sensitivity  the sensitivity values, one for each parameter
   * @return the sensitivity object
   */
  public static CurrencyParameterSensitivity of(
      MarketDataName<?> marketDataName,
      List<? extends ParameterMetadata> parameterMetadata,
      Currency currency,
      DoubleArray sensitivity) {

    return new CurrencyParameterSensitivity(marketDataName, parameterMetadata, currency, sensitivity, null);
  }

  /**
   * Obtains an instance from the market data name, currency and sensitivity.
   * <p>
   * The market data name identifies the {@link ParameterizedData} instance that was queried.
   * The parameter metadata will be empty.
   * The size of the parameter metadata list must match the size of the sensitivity array.
   * 
   * @param marketDataName  the name of the market data that the sensitivity refers to
   * @param currency  the currency of the sensitivity
   * @param sensitivity  the sensitivity values, one for each parameter
   * @return the sensitivity object
   */
  public static CurrencyParameterSensitivity of(
      MarketDataName<?> marketDataName,
      Currency currency,
      DoubleArray sensitivity) {

    return of(marketDataName, ParameterMetadata.listOfEmpty(sensitivity.size()), currency, sensitivity);
  }

  /**
   * Obtains an instance from the market data name, metadata, currency, sensitivity and parameter split.
   * <p>
   * The market data name identifies the {@link ParameterizedData} instance that was queried.
   * The parameter metadata provides information on each parameter.
   * The size of the parameter metadata list must match the size of the sensitivity array.
   * <p>
   * The parameter split allows the sensitivity to represent the split between two or more
   * underlying {@link ParameterizedData} instances. The sum of the parameters in the split
   * must equal the size of the sensitivity array, and each name must be unique.
   * 
   * @param marketDataName  the name of the market data that the sensitivity refers to
   * @param parameterMetadata  the parameter metadata
   * @param currency  the currency of the sensitivity
   * @param sensitivity  the sensitivity values, one for each parameter
   * @param parameterSplit  the split between the underlying {@code ParameterizedData} instances
   * @return the sensitivity object
   */
  public static CurrencyParameterSensitivity of(
      MarketDataName<?> marketDataName,
      List<? extends ParameterMetadata> parameterMetadata,
      Currency currency,
      DoubleArray sensitivity,
      List<ParameterSize> parameterSplit) {

    return new CurrencyParameterSensitivity(marketDataName, parameterMetadata, currency, sensitivity, parameterSplit);
  }

  //-------------------------------------------------------------------------
  /**
   * Combines two or more instances to form a single sensitivity instance.
   * <p>
   * The result will store information about the separate instances allowing it to be {@link #split()} later.
   * 
   * @param marketDataName  the combined name of the market data that the sensitivity refers to
   * @param sensitivities  the sensitivity instances to combine, two or more
   * @return the combined sensitivity object
   */
  public static CurrencyParameterSensitivity combine(
      MarketDataName<?> marketDataName,
      CurrencyParameterSensitivity... sensitivities) {

    ArgChecker.notEmpty(sensitivities, "sensitivities");
    if (sensitivities.length < 2) {
      throw new IllegalArgumentException("At least two sensitivity instances must be specified");
    }
    Currency currency = Stream.of(sensitivities).map(s -> s.getCurrency()).distinct().reduce(ensureOnlyOne()).get();
    int size = Stream.of(sensitivities).mapToInt(s -> s.getParameterCount()).sum();
    double[] combinedSensitivities = new double[size];
    ImmutableList.Builder<ParameterMetadata> combinedMeta = ImmutableList.builder();
    ImmutableList.Builder<ParameterSize> split = ImmutableList.builder();
    int count = 0;
    for (int i = 0; i < sensitivities.length; i++) {
      CurrencyParameterSensitivity sens = sensitivities[i];
      System.arraycopy(sens.getSensitivity().toArrayUnsafe(), 0, combinedSensitivities, count, sens.getParameterCount());
      combinedMeta.addAll(sens.getParameterMetadata());
      split.add(ParameterSize.of(sens.getMarketDataName(), sens.getParameterCount()));
      count += sens.getParameterCount();
    }
    
    return new CurrencyParameterSensitivity(
        marketDataName, combinedMeta.build(), currency, DoubleArray.ofUnsafe(combinedSensitivities), split.build());
  }

  @ImmutableValidator
  private void validate() {
    if (sensitivity.size() != parameterMetadata.size()) {
      throw new IllegalArgumentException("Length of sensitivity and parameter metadata must match");
    }
    if (parameterSplit != null) {
      long total = parameterSplit.stream().mapToInt(p -> p.getParameterCount()).sum();
      if (sensitivity.size() != total) {
        throw new IllegalArgumentException("Length of sensitivity and parameter split must match");
      }
      if (parameterSplit.stream().map(p -> p.getName()).distinct().count() != parameterSplit.size()) {
        throw new IllegalArgumentException("Parameter split must not contain duplicate market data names");
      }
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the number of parameters.
   * <p>
   * This returns the number of parameters in the {@link ParameterizedData} instance
   * which is the same size as the sensitivity array.
   * 
   * @return the number of parameters
   */
  public int getParameterCount() {
    return sensitivity.size();
  }

  /**
   * Gets the parameter metadata at the specified index.
   * <p>
   * If there is no specific parameter metadata, an empty instance will be returned.
   * 
   * @param parameterIndex  the zero-based index of the parameter to get
   * @return the metadata of the parameter
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    return parameterMetadata.get(parameterIndex);
  }

  /**
   * Compares the key of two sensitivity objects, excluding the parameter sensitivity values.
   * 
   * @param other  the other sensitivity object
   * @return positive if greater, zero if equal, negative if less
   */
  public int compareKey(CurrencyParameterSensitivity other) {
    return ComparisonChain.start()
        .compare(marketDataName, other.marketDataName)
        .compare(currency, other.currency)
        .result();
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this sensitivity to an equivalent in the specified currency.
   * <p>
   * Any FX conversion that is required will use rates from the provider.
   * 
   * @param resultCurrency  the currency of the result
   * @param rateProvider  the provider of FX rates
   * @return the sensitivity object expressed in terms of the result currency
   * @throws RuntimeException if no FX rate could be found
   */
  @Override
  public CurrencyParameterSensitivity convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    if (currency.equals(resultCurrency)) {
      return this;
    }
    double fxRate = rateProvider.fxRate(currency, resultCurrency);
    return mapSensitivity(s -> s * fxRate, resultCurrency);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an instance with the sensitivity values multiplied by the specified factor.
   * <p>
   * Each value in the sensitivity array will be multiplied by the factor.
   * 
   * @param factor  the multiplicative factor
   * @return an instance based on this one, with each sensitivity multiplied by the factor
   */
  public CurrencyParameterSensitivity multipliedBy(double factor) {
    return mapSensitivity(s -> s * factor);
  }

  /**
   * Returns an instance with the specified operation applied to the sensitivity values.
   * <p>
   * Each value in the sensitivity array will be operated on.
   * For example, the operator could multiply the sensitivities by a constant, or take the inverse.
   * <pre>
   *   inverse = base.mapSensitivity(value -> 1 / value);
   * </pre>
   *
   * @param operator  the operator to be applied to the sensitivities
   * @return an instance based on this one, with the operator applied to the sensitivity values
   */
  public CurrencyParameterSensitivity mapSensitivity(DoubleUnaryOperator operator) {
    return mapSensitivity(operator, currency);
  }

  // maps the sensitivities and potentially changes the currency
  private CurrencyParameterSensitivity mapSensitivity(DoubleUnaryOperator operator, Currency currency) {
    return new CurrencyParameterSensitivity(marketDataName, parameterMetadata, currency, sensitivity.map(operator), parameterSplit);
  }

  /**
   * Returns an instance with new parameter sensitivity values.
   * 
   * @param sensitivity  the new sensitivity values
   * @return an instance based on this one, with the specified sensitivity values
   */
  public CurrencyParameterSensitivity withSensitivity(DoubleArray sensitivity) {
    return new CurrencyParameterSensitivity(marketDataName, parameterMetadata, currency, sensitivity, parameterSplit);
  }

  /**
   * Returns an instance with the specified sensitivity array added to the array in this instance.
   * <p>
   * The specified array must match the size of the array in this instance.
   * 
   * @param otherSensitivty  the other parameter sensitivity
   * @return an instance based on this one, with the other instance added
   * @throws IllegalArgumentException if the market data name, metadata or parameter split differs
   */
  public CurrencyParameterSensitivity plus(DoubleArray otherSensitivty) {
    if (otherSensitivty.size() != sensitivity.size()) {
      throw new IllegalArgumentException(Messages.format(
          "Sensitivity array size {} must match size {}", otherSensitivty.size(), sensitivity.size()));
    }
    return withSensitivity(sensitivity.plus(otherSensitivty));
  }

  /**
   * Returns an instance with the specified sensitivity array added to the array in this instance.
   * <p>
   * The specified instance must have the same name, metadata, currency and parameter split as this instance.
   * 
   * @param otherSensitivty  the other parameter sensitivity
   * @return an instance based on this one, with the other instance added
   * @throws IllegalArgumentException if the market data name, metadata or parameter split differs
   */
  public CurrencyParameterSensitivity plus(CurrencyParameterSensitivity otherSensitivty) {
    if (!marketDataName.equals(otherSensitivty.marketDataName) ||
        !parameterMetadata.equals(otherSensitivty.parameterMetadata) ||
        !currency.equals(otherSensitivty.currency) ||
        (parameterSplit != null && !parameterSplit.equals(otherSensitivty.parameterSplit))) {
      throw new IllegalArgumentException("Two sensitivity instances can only be added if name, metadata and split are equal");
    }
    return plus(otherSensitivty.getSensitivity());
  }

  //-------------------------------------------------------------------------
  /**
   * Splits this sensitivity instance.
   * <p>
   * A single sensitivity instance may be based on more than one underlying {@link ParameterizedData},
   * as represented by {@link #getParameterSplit()}. Calling this method returns a list
   * where the sensitivity of this instance has been split into multiple instances as per
   * the parameter split definition. In the common case where there is a single underlying
   * {@code ParameterizedData}, the list will be of size one containing this instance.
   * 
   * @return this sensitivity split as per the defined parameter split, ordered as per this instance
   */
  public ImmutableList<CurrencyParameterSensitivity> split() {
    if (parameterSplit == null) {
      return ImmutableList.of(this);
    }
    ImmutableList.Builder<CurrencyParameterSensitivity> builder = ImmutableList.builder();
    int count = 0;
    for (ParameterSize size : parameterSplit) {
      List<ParameterMetadata> splitMetadata = parameterMetadata.subList(count, count + size.getParameterCount());
      DoubleArray splitSensitivity = sensitivity.subArray(count, count + size.getParameterCount());
      builder.add(CurrencyParameterSensitivity.of(size.getName(), splitMetadata, currency, splitSensitivity));
      count += size.getParameterCount();
    }
    return builder.build();
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the total of the sensitivity values.
   * 
   * @return the total sensitivity values
   */
  public CurrencyAmount total() {
    return CurrencyAmount.of(currency, sensitivity.sum());
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this instance to the equivalent unit sensitivity.
   * <p>
   * The result has the same sensitivity values, but no longer records the currency.
   * 
   * @return an instance based on this one, with the currency removed
   */
  public UnitParameterSensitivity toUnitParameterSensitivity() {
    return UnitParameterSensitivity.of(marketDataName, parameterMetadata, sensitivity);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CurrencyParameterSensitivity}.
   * @return the meta-bean, not null
   */
  public static CurrencyParameterSensitivity.Meta meta() {
    return CurrencyParameterSensitivity.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CurrencyParameterSensitivity.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private CurrencyParameterSensitivity(
      MarketDataName<?> marketDataName,
      List<? extends ParameterMetadata> parameterMetadata,
      Currency currency,
      DoubleArray sensitivity,
      List<ParameterSize> parameterSplit) {
    JodaBeanUtils.notNull(marketDataName, "marketDataName");
    JodaBeanUtils.notNull(parameterMetadata, "parameterMetadata");
    JodaBeanUtils.notNull(currency, "currency");
    JodaBeanUtils.notNull(sensitivity, "sensitivity");
    this.marketDataName = marketDataName;
    this.parameterMetadata = ImmutableList.copyOf(parameterMetadata);
    this.currency = currency;
    this.sensitivity = sensitivity;
    this.parameterSplit = (parameterSplit != null ? ImmutableList.copyOf(parameterSplit) : null);
    validate();
  }

  @Override
  public CurrencyParameterSensitivity.Meta metaBean() {
    return CurrencyParameterSensitivity.Meta.INSTANCE;
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
   * Gets the market data name.
   * <p>
   * This name is used in the market data system to identify the data that the sensitivities refer to.
   * @return the value of the property, not null
   */
  public MarketDataName<?> getMarketDataName() {
    return marketDataName;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the list of parameter metadata.
   * <p>
   * There is one entry for each parameter.
   * @return the value of the property, not null
   */
  public ImmutableList<ParameterMetadata> getParameterMetadata() {
    return parameterMetadata;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency of the sensitivity.
   * @return the value of the property, not null
   */
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the parameter sensitivity values.
   * <p>
   * There is one sensitivity value for each parameter.
   * @return the value of the property, not null
   */
  public DoubleArray getSensitivity() {
    return sensitivity;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the split of parameters between the underlying parameterized data.
   * <p>
   * A single {@code CurrencyParameterSensitivity} represents the sensitivity to a single {@link ParameterizedData} instance.
   * However, a {@code ParameterizedData} instance can itself be backed by more than one underlying instance.
   * For example, a curve formed from two underlying curves.
   * This list is present, it represents how to split this sensitivity between the underlying instances.
   * @return the optional value of the property, not null
   */
  public Optional<List<ParameterSize>> getParameterSplit() {
    return Optional.ofNullable(parameterSplit);
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CurrencyParameterSensitivity other = (CurrencyParameterSensitivity) obj;
      return JodaBeanUtils.equal(marketDataName, other.marketDataName) &&
          JodaBeanUtils.equal(parameterMetadata, other.parameterMetadata) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(sensitivity, other.sensitivity) &&
          JodaBeanUtils.equal(parameterSplit, other.parameterSplit);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(marketDataName);
    hash = hash * 31 + JodaBeanUtils.hashCode(parameterMetadata);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(sensitivity);
    hash = hash * 31 + JodaBeanUtils.hashCode(parameterSplit);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("CurrencyParameterSensitivity{");
    buf.append("marketDataName").append('=').append(marketDataName).append(',').append(' ');
    buf.append("parameterMetadata").append('=').append(parameterMetadata).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("sensitivity").append('=').append(sensitivity).append(',').append(' ');
    buf.append("parameterSplit").append('=').append(JodaBeanUtils.toString(parameterSplit));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CurrencyParameterSensitivity}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code marketDataName} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<MarketDataName<?>> marketDataName = DirectMetaProperty.ofImmutable(
        this, "marketDataName", CurrencyParameterSensitivity.class, (Class) MarketDataName.class);
    /**
     * The meta-property for the {@code parameterMetadata} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<ParameterMetadata>> parameterMetadata = DirectMetaProperty.ofImmutable(
        this, "parameterMetadata", CurrencyParameterSensitivity.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", CurrencyParameterSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code sensitivity} property.
     */
    private final MetaProperty<DoubleArray> sensitivity = DirectMetaProperty.ofImmutable(
        this, "sensitivity", CurrencyParameterSensitivity.class, DoubleArray.class);
    /**
     * The meta-property for the {@code parameterSplit} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<ParameterSize>> parameterSplit = DirectMetaProperty.ofImmutable(
        this, "parameterSplit", CurrencyParameterSensitivity.class, (Class) List.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "marketDataName",
        "parameterMetadata",
        "currency",
        "sensitivity",
        "parameterSplit");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 842855857:  // marketDataName
          return marketDataName;
        case -1169106440:  // parameterMetadata
          return parameterMetadata;
        case 575402001:  // currency
          return currency;
        case 564403871:  // sensitivity
          return sensitivity;
        case 1122130161:  // parameterSplit
          return parameterSplit;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends CurrencyParameterSensitivity> builder() {
      return new CurrencyParameterSensitivity.Builder();
    }

    @Override
    public Class<? extends CurrencyParameterSensitivity> beanType() {
      return CurrencyParameterSensitivity.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code marketDataName} property.
     * @return the meta-property, not null
     */
    public MetaProperty<MarketDataName<?>> marketDataName() {
      return marketDataName;
    }

    /**
     * The meta-property for the {@code parameterMetadata} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<ParameterMetadata>> parameterMetadata() {
      return parameterMetadata;
    }

    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code sensitivity} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DoubleArray> sensitivity() {
      return sensitivity;
    }

    /**
     * The meta-property for the {@code parameterSplit} property.
     * @return the meta-property, not null
     */
    public MetaProperty<List<ParameterSize>> parameterSplit() {
      return parameterSplit;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 842855857:  // marketDataName
          return ((CurrencyParameterSensitivity) bean).getMarketDataName();
        case -1169106440:  // parameterMetadata
          return ((CurrencyParameterSensitivity) bean).getParameterMetadata();
        case 575402001:  // currency
          return ((CurrencyParameterSensitivity) bean).getCurrency();
        case 564403871:  // sensitivity
          return ((CurrencyParameterSensitivity) bean).getSensitivity();
        case 1122130161:  // parameterSplit
          return ((CurrencyParameterSensitivity) bean).parameterSplit;
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
   * The bean-builder for {@code CurrencyParameterSensitivity}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<CurrencyParameterSensitivity> {

    private MarketDataName<?> marketDataName;
    private List<? extends ParameterMetadata> parameterMetadata = ImmutableList.of();
    private Currency currency;
    private DoubleArray sensitivity;
    private List<ParameterSize> parameterSplit;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 842855857:  // marketDataName
          return marketDataName;
        case -1169106440:  // parameterMetadata
          return parameterMetadata;
        case 575402001:  // currency
          return currency;
        case 564403871:  // sensitivity
          return sensitivity;
        case 1122130161:  // parameterSplit
          return parameterSplit;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 842855857:  // marketDataName
          this.marketDataName = (MarketDataName<?>) newValue;
          break;
        case -1169106440:  // parameterMetadata
          this.parameterMetadata = (List<? extends ParameterMetadata>) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 564403871:  // sensitivity
          this.sensitivity = (DoubleArray) newValue;
          break;
        case 1122130161:  // parameterSplit
          this.parameterSplit = (List<ParameterSize>) newValue;
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
    public CurrencyParameterSensitivity build() {
      return new CurrencyParameterSensitivity(
          marketDataName,
          parameterMetadata,
          currency,
          sensitivity,
          parameterSplit);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("CurrencyParameterSensitivity.Builder{");
      buf.append("marketDataName").append('=').append(JodaBeanUtils.toString(marketDataName)).append(',').append(' ');
      buf.append("parameterMetadata").append('=').append(JodaBeanUtils.toString(parameterMetadata)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity)).append(',').append(' ');
      buf.append("parameterSplit").append('=').append(JodaBeanUtils.toString(parameterSplit));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
