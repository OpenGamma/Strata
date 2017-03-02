/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.Messages;

/**
 * Defines the computation of a rate of interest interpolated from two Ibor indices.
 * <p>
 * An interest rate determined from two Ibor indices by linear interpolation.
 * Both indices are observed on the same fixing date and they must have the same currency.
 * For example, linear interpolation between 'GBP-LIBOR-1M' and 'GBP-LIBOR-3M'.
 */
@BeanDefinition(builderScope = "private")
public final class IborInterpolatedRateComputation
    implements RateComputation, ImmutableBean, Serializable {

  /**
   * The shorter Ibor index observation.
   * <p>
   * The rate to be paid is based on this index
   * It will be a well known market index such as 'GBP-LIBOR-1M'.
   */
  @PropertyDefinition(validate = "notNull")
  private final IborIndexObservation shortObservation;
  /**
   * The longer Ibor index observation.
   * <p>
   * The rate to be paid is based on this index
   * It will be a well known market index such as 'GBP-LIBOR-3M'.
   */
  @PropertyDefinition(validate = "notNull")
  private final IborIndexObservation longObservation;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance from two indices and fixing date.
   * <p>
   * The indices may be passed in any order.
   * 
   * @param index1  the first index
   * @param index2  the second index
   * @param fixingDate  the fixing date
   * @param refData  the reference data to use when resolving holiday calendars
   * @return the interpolated rate computation
   */
  public static IborInterpolatedRateComputation of(
      IborIndex index1,
      IborIndex index2,
      LocalDate fixingDate,
      ReferenceData refData) {

    boolean inOrder = indicesInOrder(index1, index2, fixingDate);
    IborIndexObservation obs1 = IborIndexObservation.of(index1, fixingDate, refData);
    IborIndexObservation obs2 = IborIndexObservation.of(index2, fixingDate, refData);
    return new IborInterpolatedRateComputation(inOrder ? obs1 : obs2, inOrder ? obs2 : obs1);
  }

  /**
   * Creates an instance from the two underlying index observations.
   * <p>
   * The two observations must be for two different indexes in the same currency on the same fixing date.
   * The index with the shorter tenor must be passed as the first argument.
   * 
   * @param shortObservation  the short underlying index observation
   * @param longObservation  the long underlying index observation
   * @return the rate computation
   * @throws IllegalArgumentException if the indices are not short, then long
   */
  public static IborInterpolatedRateComputation of(
      IborIndexObservation shortObservation,
      IborIndexObservation longObservation) {

    return new IborInterpolatedRateComputation(shortObservation, longObservation);
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    IborIndex shortIndex = shortObservation.getIndex();
    IborIndex longIndex = longObservation.getIndex();
    if (!shortIndex.getCurrency().equals(longIndex.getCurrency())) {
      throw new IllegalArgumentException("Interpolation requires two indices in the same currency");
    }
    if (shortIndex.equals(longIndex)) {
      throw new IllegalArgumentException("Interpolation requires two different indices");
    }
    if (!shortObservation.getFixingDate().equals(longObservation.getFixingDate())) {
      throw new IllegalArgumentException("Interpolation requires observations with same fixing date");
    }
    if (!indicesInOrder(shortIndex, longIndex, shortObservation.getFixingDate())) {
      throw new IllegalArgumentException(Messages.format(
          "Interpolation indices passed in wrong order: {} {}", shortIndex, longIndex));
    }
  }

  // checks that the indices are in order
  private static boolean indicesInOrder(IborIndex index1, IborIndex index2, LocalDate fixingDate) {
    return fixingDate.plus(index1.getTenor()).isBefore(fixingDate.plus(index2.getTenor()));
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the fixing date.
   * 
   * @return the fixing date
   */
  public LocalDate getFixingDate() {
    // fixing date is the same for both observations
    return shortObservation.getFixingDate();
  }

  //-------------------------------------------------------------------------
  @Override
  public void collectIndices(ImmutableSet.Builder<Index> builder) {
    builder.add(shortObservation.getIndex());
    builder.add(longObservation.getIndex());
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IborInterpolatedRateComputation}.
   * @return the meta-bean, not null
   */
  public static IborInterpolatedRateComputation.Meta meta() {
    return IborInterpolatedRateComputation.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IborInterpolatedRateComputation.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private IborInterpolatedRateComputation(
      IborIndexObservation shortObservation,
      IborIndexObservation longObservation) {
    JodaBeanUtils.notNull(shortObservation, "shortObservation");
    JodaBeanUtils.notNull(longObservation, "longObservation");
    this.shortObservation = shortObservation;
    this.longObservation = longObservation;
    validate();
  }

  @Override
  public IborInterpolatedRateComputation.Meta metaBean() {
    return IborInterpolatedRateComputation.Meta.INSTANCE;
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
   * Gets the shorter Ibor index observation.
   * <p>
   * The rate to be paid is based on this index
   * It will be a well known market index such as 'GBP-LIBOR-1M'.
   * @return the value of the property, not null
   */
  public IborIndexObservation getShortObservation() {
    return shortObservation;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the longer Ibor index observation.
   * <p>
   * The rate to be paid is based on this index
   * It will be a well known market index such as 'GBP-LIBOR-3M'.
   * @return the value of the property, not null
   */
  public IborIndexObservation getLongObservation() {
    return longObservation;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      IborInterpolatedRateComputation other = (IborInterpolatedRateComputation) obj;
      return JodaBeanUtils.equal(shortObservation, other.shortObservation) &&
          JodaBeanUtils.equal(longObservation, other.longObservation);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(shortObservation);
    hash = hash * 31 + JodaBeanUtils.hashCode(longObservation);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("IborInterpolatedRateComputation{");
    buf.append("shortObservation").append('=').append(shortObservation).append(',').append(' ');
    buf.append("longObservation").append('=').append(JodaBeanUtils.toString(longObservation));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IborInterpolatedRateComputation}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code shortObservation} property.
     */
    private final MetaProperty<IborIndexObservation> shortObservation = DirectMetaProperty.ofImmutable(
        this, "shortObservation", IborInterpolatedRateComputation.class, IborIndexObservation.class);
    /**
     * The meta-property for the {@code longObservation} property.
     */
    private final MetaProperty<IborIndexObservation> longObservation = DirectMetaProperty.ofImmutable(
        this, "longObservation", IborInterpolatedRateComputation.class, IborIndexObservation.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "shortObservation",
        "longObservation");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -496986608:  // shortObservation
          return shortObservation;
        case -684321776:  // longObservation
          return longObservation;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends IborInterpolatedRateComputation> builder() {
      return new IborInterpolatedRateComputation.Builder();
    }

    @Override
    public Class<? extends IborInterpolatedRateComputation> beanType() {
      return IborInterpolatedRateComputation.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code shortObservation} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborIndexObservation> shortObservation() {
      return shortObservation;
    }

    /**
     * The meta-property for the {@code longObservation} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborIndexObservation> longObservation() {
      return longObservation;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -496986608:  // shortObservation
          return ((IborInterpolatedRateComputation) bean).getShortObservation();
        case -684321776:  // longObservation
          return ((IborInterpolatedRateComputation) bean).getLongObservation();
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
   * The bean-builder for {@code IborInterpolatedRateComputation}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<IborInterpolatedRateComputation> {

    private IborIndexObservation shortObservation;
    private IborIndexObservation longObservation;

    /**
     * Restricted constructor.
     */
    private Builder() {
      super(meta());
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -496986608:  // shortObservation
          return shortObservation;
        case -684321776:  // longObservation
          return longObservation;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -496986608:  // shortObservation
          this.shortObservation = (IborIndexObservation) newValue;
          break;
        case -684321776:  // longObservation
          this.longObservation = (IborIndexObservation) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public IborInterpolatedRateComputation build() {
      return new IborInterpolatedRateComputation(
          shortObservation,
          longObservation);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("IborInterpolatedRateComputation.Builder{");
      buf.append("shortObservation").append('=').append(JodaBeanUtils.toString(shortObservation)).append(',').append(' ');
      buf.append("longObservation").append('=').append(JodaBeanUtils.toString(longObservation));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
