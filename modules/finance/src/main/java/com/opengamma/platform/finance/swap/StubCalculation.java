/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.swap;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
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

import com.opengamma.basics.index.IborIndex;
import com.opengamma.platform.finance.rate.FixedRate;
import com.opengamma.platform.finance.rate.IborInterpolatedRate;
import com.opengamma.platform.finance.rate.IborRate;
import com.opengamma.platform.finance.rate.Rate;

/**
 * Defines the rates applicable in the initial or final stub in an IBOR-like swap leg.
 * <p>
 * A standard swap leg consists of a regular periodic schedule and one or two stub periods at each end.
 * This class defines what floating rate to use during a stub.
 * <p>
 * The rate may be specified in three ways.
 * <ul>
 * <li>A fixed rate, applicable for the whole stub
 * <li>A single floating rate
 * <li>Linear interpolation between two floating rates
 * </ul>
 */
@BeanDefinition
public final class StubCalculation
    implements ImmutableBean, Serializable {

  /**
   * An instance that has no special rate handling.
   */
  public static final StubCalculation NONE = new StubCalculation(null, null, null);

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The fixed rate to use in the stub, with a 5% rate expressed as 0.05.
   * <p>
   * In certain circumstances two counterparties agree a fixed rate for the stub.
   * It is used in place of an observed fixing.
   * Other calculation elements, such as gearing or spread, still apply.
   * <p>
   * If this property is non-null, then {@code index} and {@code indexInterpolated} must be null.
   */
  @PropertyDefinition
  private final Double fixedRate;
  /**
   * The IBOR-like index to be used for the stub.
   * <p>
   * This will be used throughout the stub unless {@code indexInterpolated} is non-null.
   * <p>
   * If this property is non-null, then {@code rate} must be null.
   */
  @PropertyDefinition
  private final IborIndex index;
  /**
   * The second IBOR-like index to be used for the stub, linearly interpolated.
   * <p>
   * This will be used with {@code index} to linearly interpolate the rate.
   * <p>
   * If this property is non-null, then {@code rate} must be null and {@code index} must be non-null.
   */
  @PropertyDefinition
  private final IborIndex indexInterpolated;

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code FloatingRateStub} with a single fixed rate.
   * 
   * @param fixedRate  the fixed rate for the stub
   * @return the stub
   */
  public static StubCalculation of(double fixedRate) {
    return new StubCalculation(fixedRate, null, null);
  }

  /**
   * Obtains a {@code FloatingRateStub} with a single floating rate.
   * 
   * @param index  the index that applies to the stub
   * @return the stub
   */
  public static StubCalculation of(IborIndex index) {
    return new StubCalculation(null, index, null);
  }

  /**
   * Obtains a {@code FloatingRateStub} with linear interpolation of two floating rates.
   * 
   * @param startIndex  the index that applies at the start of the stub
   * @param endIndex  the index that applies at the end of the stub
   * @return the stub
   */
  public static StubCalculation of(IborIndex startIndex, IborIndex endIndex) {
    return new StubCalculation(null, startIndex, endIndex);
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    if (fixedRate != null && index != null) {
      throw new IllegalArgumentException("Either rate or index may be specified, not both");
    }
    if (indexInterpolated != null && index == null) {
      throw new IllegalArgumentException("When indexInterpolated is non-null index must also be non-null");
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Creates the {@code Rate} for the stub.
   * 
   * @param fixingDate  the fixing date
   * @param defaultIndex  the default index to use if the stub has no rules
   * @return the rate
   */
  Rate createRate(LocalDate fixingDate, IborIndex defaultIndex) {
    if (isInterpolated()) {
      return IborInterpolatedRate.of(index, getIndexInterpolated(), fixingDate);
    } else if (isFloatingRate()) {
      return IborRate.of(index, fixingDate);
    } else if (isFixedRate()) {
      return FixedRate.of(fixedRate);
    } else {
      return IborRate.of(defaultIndex, fixingDate);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the stub has a fixed rate.
   * 
   * @return true if a fixed stub rate applies
   */
  public boolean isFixedRate() {
    return fixedRate != null;
  }

  /**
   * Checks if the stub has a floating rate.
   * 
   * @return true if a floating stub rate applies
   */
  public boolean isFloatingRate() {
    return index != null;
  }

  /**
   * Checks if the stub has an interpolated rate.
   * <p>
   * An interpolated rate exists when there are two different rates that need linear interpolation.
   * 
   * @return true if linear interpolation applies
   */
  public boolean isInterpolated() {
    return index != null && indexInterpolated != null && !index.equals(indexInterpolated);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code StubCalculation}.
   * @return the meta-bean, not null
   */
  public static StubCalculation.Meta meta() {
    return StubCalculation.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(StubCalculation.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static StubCalculation.Builder builder() {
    return new StubCalculation.Builder();
  }

  private StubCalculation(
      Double fixedRate,
      IborIndex index,
      IborIndex indexInterpolated) {
    this.fixedRate = fixedRate;
    this.index = index;
    this.indexInterpolated = indexInterpolated;
    validate();
  }

  @Override
  public StubCalculation.Meta metaBean() {
    return StubCalculation.Meta.INSTANCE;
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
   * Gets the fixed rate to use in the stub, with a 5% rate expressed as 0.05.
   * <p>
   * In certain circumstances two counterparties agree a fixed rate for the stub.
   * It is used in place of an observed fixing.
   * Other calculation elements, such as gearing or spread, still apply.
   * <p>
   * If this property is non-null, then {@code index} and {@code indexInterpolated} must be null.
   * @return the value of the property
   */
  public Double getFixedRate() {
    return fixedRate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the IBOR-like index to be used for the stub.
   * <p>
   * This will be used throughout the stub unless {@code indexInterpolated} is non-null.
   * <p>
   * If this property is non-null, then {@code rate} must be null.
   * @return the value of the property
   */
  public IborIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the second IBOR-like index to be used for the stub, linearly interpolated.
   * <p>
   * This will be used with {@code index} to linearly interpolate the rate.
   * <p>
   * If this property is non-null, then {@code rate} must be null and {@code index} must be non-null.
   * @return the value of the property
   */
  public IborIndex getIndexInterpolated() {
    return indexInterpolated;
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
      StubCalculation other = (StubCalculation) obj;
      return JodaBeanUtils.equal(getFixedRate(), other.getFixedRate()) &&
          JodaBeanUtils.equal(getIndex(), other.getIndex()) &&
          JodaBeanUtils.equal(getIndexInterpolated(), other.getIndexInterpolated());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash += hash * 31 + JodaBeanUtils.hashCode(getFixedRate());
    hash += hash * 31 + JodaBeanUtils.hashCode(getIndex());
    hash += hash * 31 + JodaBeanUtils.hashCode(getIndexInterpolated());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("StubCalculation{");
    buf.append("fixedRate").append('=').append(getFixedRate()).append(',').append(' ');
    buf.append("index").append('=').append(getIndex()).append(',').append(' ');
    buf.append("indexInterpolated").append('=').append(JodaBeanUtils.toString(getIndexInterpolated()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code StubCalculation}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code fixedRate} property.
     */
    private final MetaProperty<Double> fixedRate = DirectMetaProperty.ofImmutable(
        this, "fixedRate", StubCalculation.class, Double.class);
    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<IborIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", StubCalculation.class, IborIndex.class);
    /**
     * The meta-property for the {@code indexInterpolated} property.
     */
    private final MetaProperty<IborIndex> indexInterpolated = DirectMetaProperty.ofImmutable(
        this, "indexInterpolated", StubCalculation.class, IborIndex.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "fixedRate",
        "index",
        "indexInterpolated");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 747425396:  // fixedRate
          return fixedRate;
        case 100346066:  // index
          return index;
        case -1934091915:  // indexInterpolated
          return indexInterpolated;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public StubCalculation.Builder builder() {
      return new StubCalculation.Builder();
    }

    @Override
    public Class<? extends StubCalculation> beanType() {
      return StubCalculation.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code fixedRate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> fixedRate() {
      return fixedRate;
    }

    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code indexInterpolated} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborIndex> indexInterpolated() {
      return indexInterpolated;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 747425396:  // fixedRate
          return ((StubCalculation) bean).getFixedRate();
        case 100346066:  // index
          return ((StubCalculation) bean).getIndex();
        case -1934091915:  // indexInterpolated
          return ((StubCalculation) bean).getIndexInterpolated();
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
   * The bean-builder for {@code StubCalculation}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<StubCalculation> {

    private Double fixedRate;
    private IborIndex index;
    private IborIndex indexInterpolated;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(StubCalculation beanToCopy) {
      this.fixedRate = beanToCopy.getFixedRate();
      this.index = beanToCopy.getIndex();
      this.indexInterpolated = beanToCopy.getIndexInterpolated();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 747425396:  // fixedRate
          return fixedRate;
        case 100346066:  // index
          return index;
        case -1934091915:  // indexInterpolated
          return indexInterpolated;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 747425396:  // fixedRate
          this.fixedRate = (Double) newValue;
          break;
        case 100346066:  // index
          this.index = (IborIndex) newValue;
          break;
        case -1934091915:  // indexInterpolated
          this.indexInterpolated = (IborIndex) newValue;
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
    public StubCalculation build() {
      return new StubCalculation(
          fixedRate,
          index,
          indexInterpolated);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code fixedRate} property in the builder.
     * @param fixedRate  the new value
     * @return this, for chaining, not null
     */
    public Builder fixedRate(Double fixedRate) {
      this.fixedRate = fixedRate;
      return this;
    }

    /**
     * Sets the {@code index} property in the builder.
     * @param index  the new value
     * @return this, for chaining, not null
     */
    public Builder index(IborIndex index) {
      this.index = index;
      return this;
    }

    /**
     * Sets the {@code indexInterpolated} property in the builder.
     * @param indexInterpolated  the new value
     * @return this, for chaining, not null
     */
    public Builder indexInterpolated(IborIndex indexInterpolated) {
      this.indexInterpolated = indexInterpolated;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("StubCalculation.Builder{");
      buf.append("fixedRate").append('=').append(JodaBeanUtils.toString(fixedRate)).append(',').append(' ');
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("indexInterpolated").append('=').append(JodaBeanUtils.toString(indexInterpolated));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
