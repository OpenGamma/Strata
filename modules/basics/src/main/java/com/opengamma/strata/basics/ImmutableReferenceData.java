/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.collect.Messages;

/**
 * An immutable set of reference data
 * <p>
 * This is the standard immutable implementation of {@link ReferenceData}.
 */
@BeanDefinition(builderScope = "private")
public final class ImmutableReferenceData
    implements ReferenceData, ImmutableBean, Serializable {

  /**
   * The empty instance.
   */
  private static final ImmutableReferenceData EMPTY = new ImmutableReferenceData(ImmutableMap.of());

  /**
   * The typed reference data values by identifier.
   */
  @PropertyDefinition(validate = "notNull", builderType = "Map<? extends ReferenceDataId<?>, ?>")
  private final ImmutableMap<ReferenceDataId<?>, Object> values;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a map of reference data.
   * <p>
   * Each entry in the map is a single piece of reference data, keyed by the matching identifier.
   * For example, a {@link HolidayCalendarId} associated with a {@link HolidayCalendar}.
   * The caller must ensure that the each entry in the map corresponds with the parameterized
   * type on the identifier.
   * <p>
   * The resulting {@code ImmutableReferenceData} instance does not include the
   * {@linkplain ReferenceData#minimal() minimal} set of reference data that is essential for pricing.
   * To include the minimal set, use {@link ReferenceData#of(Map)} or {@link #combinedWith(ReferenceData)}.
   *
   * @param values  the reference data values
   * @return the reference data instance
   * @throws ClassCastException if a value does not match the parameterized type associated with the identifier
   */
  public static ImmutableReferenceData of(Map<? extends ReferenceDataId<?>, ?> values) {
    // validation handles case where value does not match identifier
    values.forEach((id, value) -> validateEntry(id, value));
    return new ImmutableReferenceData(values);
  }

  /**
   * Obtains an instance from a single reference data entry.
   * <p>
   * This returns an instance containing a single entry based on the specified identifier and value.
   * This is primarily of interest to test cases.
   * <p>
   * The resulting {@code ImmutableReferenceData} instance does not include the
   * {@linkplain ReferenceData#minimal() minimal} set of reference data that is essential for pricing.
   *
   * @param <T>  the type of the reference data
   * @param id  the identifier
   * @param value  the reference data values
   * @return the reference data instance
   * @throws ClassCastException if a value does not match the parameterized type associated with the identifier
   */
  public static <T> ImmutableReferenceData of(ReferenceDataId<T> id, T value) {
    // validation handles edge case where input by raw or polluted types
    validateEntry(id, value);
    return new ImmutableReferenceData(ImmutableMap.of(id, value));
  }

  // validates a single entry
  private static void validateEntry(ReferenceDataId<?> id, Object value) {
    if (!id.getReferenceDataType().isInstance(value)) {
      if (value == null) {
        throw new IllegalArgumentException(Messages.format(
            "Value for identifier '{}' must not be null", id));
      }
      throw new ClassCastException(Messages.format(
          "Value for identifier '{}' does not implement expected type '{}': '{}'",
          id, id.getReferenceDataType().getSimpleName(), value));
    }
  }

  /**
   * Obtains an instance containing no reference data.
   *
   * @return empty reference data
   */
  public static ImmutableReferenceData empty() {
    return EMPTY;
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean containsValue(ReferenceDataId<?> id) {
    // overridden for performance
    return id.queryValueOrNull(this) != null;
  }

  @Override
  public <T> T getValue(ReferenceDataId<T> id) {
    // overridden for performance
    // no type check against id.getReferenceDataType() as checked in factory
    @SuppressWarnings("unchecked")
    T value = id.queryValueOrNull(this);
    if (value == null) {
      throw new ReferenceDataNotFoundException(msgValueNotFound(id));
    }
    return value;
  }

  // extracted to aid inlining performance
  private String msgValueNotFound(ReferenceDataId<?> id) {
    return Messages.format(
        "Reference data not found for identifier '{}' of type '{}'", id, id.getClass().getSimpleName());
  }

  @Override
  public <T> Optional<T> findValue(ReferenceDataId<T> id) {
    // no type check against id.getReferenceDataType() as checked in factory
    @SuppressWarnings("unchecked")
    T value = id.queryValueOrNull(this);
    return Optional.ofNullable(value);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T queryValueOrNull(ReferenceDataId<T> id) {
    // no type check against id.getReferenceDataType() as checked in factory
    return (T) values.get(id);
  }

  @Override
  public ReferenceData combinedWith(ReferenceData other) {
    if (other instanceof ImmutableReferenceData) {
      ImmutableReferenceData otherData = (ImmutableReferenceData) other;
      // hash map so that keys can overlap, with this instance taking priority
      Map<ReferenceDataId<?>, Object> combined = new HashMap<>();
      combined.putAll(otherData.values);
      combined.putAll(this.values);
      return new ImmutableReferenceData(combined);
    }
    return ReferenceData.super.combinedWith(other);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ImmutableReferenceData}.
   * @return the meta-bean, not null
   */
  public static ImmutableReferenceData.Meta meta() {
    return ImmutableReferenceData.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ImmutableReferenceData.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private ImmutableReferenceData(
      Map<? extends ReferenceDataId<?>, ?> values) {
    JodaBeanUtils.notNull(values, "values");
    this.values = ImmutableMap.copyOf(values);
  }

  @Override
  public ImmutableReferenceData.Meta metaBean() {
    return ImmutableReferenceData.Meta.INSTANCE;
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
   * Gets the typed reference data values by identifier.
   * @return the value of the property, not null
   */
  public ImmutableMap<ReferenceDataId<?>, Object> getValues() {
    return values;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ImmutableReferenceData other = (ImmutableReferenceData) obj;
      return JodaBeanUtils.equal(values, other.values);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(values);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("ImmutableReferenceData{");
    buf.append("values").append('=').append(JodaBeanUtils.toString(values));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ImmutableReferenceData}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code values} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<ReferenceDataId<?>, Object>> values = DirectMetaProperty.ofImmutable(
        this, "values", ImmutableReferenceData.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "values");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -823812830:  // values
          return values;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends ImmutableReferenceData> builder() {
      return new ImmutableReferenceData.Builder();
    }

    @Override
    public Class<? extends ImmutableReferenceData> beanType() {
      return ImmutableReferenceData.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code values} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<ReferenceDataId<?>, Object>> values() {
      return values;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -823812830:  // values
          return ((ImmutableReferenceData) bean).getValues();
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
   * The bean-builder for {@code ImmutableReferenceData}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<ImmutableReferenceData> {

    private Map<? extends ReferenceDataId<?>, ?> values = ImmutableMap.of();

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
        case -823812830:  // values
          return values;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -823812830:  // values
          this.values = (Map<? extends ReferenceDataId<?>, ?>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public ImmutableReferenceData build() {
      return new ImmutableReferenceData(
          values);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("ImmutableReferenceData.Builder{");
      buf.append("values").append('=').append(JodaBeanUtils.toString(values));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
