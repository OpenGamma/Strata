/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;

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
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.surface.Surface;

/**
 * Unit parameter sensitivity for parameterized market data, such as a curve.
 * <p>
 * Parameter sensitivity is the sensitivity of a value to the parameters of a
 * {@linkplain ParameterizedData parameterized market data} object that is used to determine the value.
 * Common {@code ParameterizedData} implementations include {@link Curve} and {@link Surface}.
 * <p>
 * The sensitivity is expressed as an array, with one entry for each parameter in the {@code ParameterizedData}.
 * The sensitivity has no associated currency.
 */
@BeanDefinition(builderScope = "private")
public final class UnitParameterSensitivity
    implements ImmutableBean {

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
  private final List<ParameterMetadata> parameterMetadata;
  /**
   * The parameter sensitivity values.
   * <p>
   * There is one sensitivity value for each parameter.
   */
  @PropertyDefinition(validate = "notNull")
  private final DoubleArray sensitivity;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the market data name, metadata and sensitivity.
   * <p>
   * The market data name identifies the {@link ParameterizedData} instance that was queried.
   * The parameter metadata provides information on each parameter.
   * The size of the parameter metadata list must match the size of the sensitivity array.
   * 
   * @param marketDataName  the name of the market data that the sensitivity refers to
   * @param parameterMetadata  the parameter metadata
   * @param sensitivity  the sensitivity values, one for each parameter
   * @return the sensitivity object
   */
  public static UnitParameterSensitivity of(
      MarketDataName<?> marketDataName,
      List<? extends ParameterMetadata> parameterMetadata,
      DoubleArray sensitivity) {

    return new UnitParameterSensitivity(marketDataName, parameterMetadata, sensitivity);
  }

  /**
   * Obtains an instance from the market data name, currency and sensitivity.
   * <p>
   * The market data name identifies the {@link ParameterizedData} instance that was queried.
   * The parameter metadata will be empty.
   * The size of the parameter metadata list must match the size of the sensitivity array.
   * 
   * @param marketDataName  the name of the market data that the sensitivity refers to
   * @param sensitivity  the sensitivity values, one for each parameter
   * @return the sensitivity object
   */
  public static UnitParameterSensitivity of(MarketDataName<?> marketDataName, DoubleArray sensitivity) {
    return of(marketDataName, ParameterMetadata.listOfEmpty(sensitivity.size()), sensitivity);
  }

  @ImmutableValidator
  private void validate() {
    if (sensitivity.size() != parameterMetadata.size()) {
      throw new IllegalArgumentException("Length of sensitivity and parameter metadata must match");
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
  public int compareKey(UnitParameterSensitivity other) {
    return ComparisonChain.start()
        .compare(marketDataName, other.marketDataName)
        .result();
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this sensitivity to a monetary value, multiplying by the specified factor.
   * <p>
   * Each value in the sensitivity array will be multiplied by the specified factor.
   * 
   * @param currency  the currency of the amount
   * @param amount  the amount to multiply by
   * @return the resulting sensitivity object
   */
  public CurrencyParameterSensitivity multipliedBy(Currency currency, double amount) {
    return CurrencyParameterSensitivity.of(marketDataName, parameterMetadata, currency, sensitivity.multipliedBy(amount));
  }

  /**
   * Returns an instance with the sensitivity values multiplied by the specified factor.
   * <p>
   * Each value in the sensitivity array will be multiplied by the factor.
   * 
   * @param factor  the multiplicative factor
   * @return an instance based on this one, with each sensitivity multiplied by the factor
   */
  public UnitParameterSensitivity multipliedBy(double factor) {
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
  public UnitParameterSensitivity mapSensitivity(DoubleUnaryOperator operator) {
    return new UnitParameterSensitivity(marketDataName, parameterMetadata, sensitivity.map(operator));
  }

  /**
   * Returns an instance with new parameter sensitivity values.
   * 
   * @param sensitivity  the new sensitivity values
   * @return an instance based on this one, with the specified sensitivity values
   */
  public UnitParameterSensitivity withSensitivity(DoubleArray sensitivity) {
    return new UnitParameterSensitivity(marketDataName, parameterMetadata, sensitivity);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the total of the sensitivity values.
   * 
   * @return the total sensitivity values
   */
  public double total() {
    return sensitivity.sum();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code UnitParameterSensitivity}.
   * @return the meta-bean, not null
   */
  public static UnitParameterSensitivity.Meta meta() {
    return UnitParameterSensitivity.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(UnitParameterSensitivity.Meta.INSTANCE);
  }

  private UnitParameterSensitivity(
      MarketDataName<?> marketDataName,
      List<? extends ParameterMetadata> parameterMetadata,
      DoubleArray sensitivity) {
    JodaBeanUtils.notNull(marketDataName, "marketDataName");
    JodaBeanUtils.notNull(parameterMetadata, "parameterMetadata");
    JodaBeanUtils.notNull(sensitivity, "sensitivity");
    this.marketDataName = marketDataName;
    this.parameterMetadata = ImmutableList.copyOf(parameterMetadata);
    this.sensitivity = sensitivity;
    validate();
  }

  @Override
  public UnitParameterSensitivity.Meta metaBean() {
    return UnitParameterSensitivity.Meta.INSTANCE;
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
  public List<ParameterMetadata> getParameterMetadata() {
    return parameterMetadata;
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
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      UnitParameterSensitivity other = (UnitParameterSensitivity) obj;
      return JodaBeanUtils.equal(marketDataName, other.marketDataName) &&
          JodaBeanUtils.equal(parameterMetadata, other.parameterMetadata) &&
          JodaBeanUtils.equal(sensitivity, other.sensitivity);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(marketDataName);
    hash = hash * 31 + JodaBeanUtils.hashCode(parameterMetadata);
    hash = hash * 31 + JodaBeanUtils.hashCode(sensitivity);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("UnitParameterSensitivity{");
    buf.append("marketDataName").append('=').append(marketDataName).append(',').append(' ');
    buf.append("parameterMetadata").append('=').append(parameterMetadata).append(',').append(' ');
    buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code UnitParameterSensitivity}.
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
        this, "marketDataName", UnitParameterSensitivity.class, (Class) MarketDataName.class);
    /**
     * The meta-property for the {@code parameterMetadata} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<ParameterMetadata>> parameterMetadata = DirectMetaProperty.ofImmutable(
        this, "parameterMetadata", UnitParameterSensitivity.class, (Class) List.class);
    /**
     * The meta-property for the {@code sensitivity} property.
     */
    private final MetaProperty<DoubleArray> sensitivity = DirectMetaProperty.ofImmutable(
        this, "sensitivity", UnitParameterSensitivity.class, DoubleArray.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "marketDataName",
        "parameterMetadata",
        "sensitivity");

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
        case 564403871:  // sensitivity
          return sensitivity;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends UnitParameterSensitivity> builder() {
      return new UnitParameterSensitivity.Builder();
    }

    @Override
    public Class<? extends UnitParameterSensitivity> beanType() {
      return UnitParameterSensitivity.class;
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
    public MetaProperty<List<ParameterMetadata>> parameterMetadata() {
      return parameterMetadata;
    }

    /**
     * The meta-property for the {@code sensitivity} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DoubleArray> sensitivity() {
      return sensitivity;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 842855857:  // marketDataName
          return ((UnitParameterSensitivity) bean).getMarketDataName();
        case -1169106440:  // parameterMetadata
          return ((UnitParameterSensitivity) bean).getParameterMetadata();
        case 564403871:  // sensitivity
          return ((UnitParameterSensitivity) bean).getSensitivity();
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
   * The bean-builder for {@code UnitParameterSensitivity}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<UnitParameterSensitivity> {

    private MarketDataName<?> marketDataName;
    private List<? extends ParameterMetadata> parameterMetadata = ImmutableList.of();
    private DoubleArray sensitivity;

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
        case 564403871:  // sensitivity
          return sensitivity;
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
        case 564403871:  // sensitivity
          this.sensitivity = (DoubleArray) newValue;
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
    public UnitParameterSensitivity build() {
      return new UnitParameterSensitivity(
          marketDataName,
          parameterMetadata,
          sensitivity);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("UnitParameterSensitivity.Builder{");
      buf.append("marketDataName").append('=').append(JodaBeanUtils.toString(marketDataName)).append(',').append(' ');
      buf.append("parameterMetadata").append('=').append(JodaBeanUtils.toString(parameterMetadata)).append(',').append(' ');
      buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
