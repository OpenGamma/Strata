/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.date.Tenor;

/**
 * An immutable floating rate index name, such as Libor, Euribor or US Fed Fund.
 * <p>
 * This is the standard immutable implementation of {@link FloatingRateName}.
 */
@BeanDefinition(builderScope = "private")
public final class ImmutableFloatingRateName
    implements FloatingRateName, ImmutableBean, Serializable {

  /**
   * Special suffix that can be used to distinguish averaged indices.
   */
  private static final String AVERAGE_SUFFIX = "-AVG";

  /**
   * The external name, typically from FpML, such as 'GBP-LIBOR-BBA'.
   */
  @PropertyDefinition(validate = "notEmpty")
  private final String externalName;
  /**
   * The root of the name of the index, such as 'GBP-LIBOR', to which the tenor is appended.
   * This name matches that used by {@link IborIndex} or {@link OvernightIndex}.
   * Typically, multiple {@code FloatingRateName} names map to one Ibor or Overnight index.
   */
  @PropertyDefinition(validate = "notEmpty")
  private final String indexName;
  /**
   * The type of the index.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final FloatingRateType type;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified external name, index name and type.
   * 
   * @param externalName  the unique name
   * @param indexName  the name of the index
   * @param type  the type - Ibor, Overnight or Price
   * @return the name
   * @throws IllegalArgumentException if the name is not known
   */
  public static ImmutableFloatingRateName of(String externalName, String indexName, FloatingRateType type) {
    return new ImmutableFloatingRateName(externalName, indexName, type);
  }

  //-------------------------------------------------------------------------
  @Override
  public String getName() {
    return externalName;
  }

  @Override
  public Set<Tenor> getTenors() {
    if (!type.isIbor()) {
      return ImmutableSet.of();
    }
    return IborIndex.extendedEnum().lookupAll().values().stream()
        .filter(index -> index.getName().startsWith(indexName))
        .filter(index -> index.isActive())
        .map(index -> index.getTenor())
        .sorted()
        .collect(toImmutableSet());
  }

  @Override
  public FloatingRateName normalized() {
    if (type.isIbor() && indexName.endsWith("-")) {
      return FloatingRateName.of(indexName.substring(0, indexName.length() - 1));
    }
    return FloatingRateName.of(indexName);
  }

  //-------------------------------------------------------------------------
  @Override
  public IborIndex toIborIndex(Tenor tenor) {
    if (!type.isIbor()) {
      throw new IllegalStateException("Incorrect index type, expected Ibor: " + externalName);
    }
    return IborIndex.of(indexName + tenor.normalized().toString());
  }

  @Override
  public OvernightIndex toOvernightIndex() {
    if (!type.isOvernight()) {
      throw new IllegalStateException("Incorrect index type, expected Overnight: " + externalName);
    }
    if (indexName.endsWith(AVERAGE_SUFFIX)) {
      return OvernightIndex.of(indexName.substring(0, indexName.length() - 4));
    }
    return OvernightIndex.of(indexName);
  }

  @Override
  public PriceIndex toPriceIndex() {
    if (!type.isPrice()) {
      throw new IllegalStateException("Incorrect index type, expected Price: " + externalName);
    }
    return PriceIndex.of(indexName);
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof ImmutableFloatingRateName) {
      return externalName.equals(((ImmutableFloatingRateName) obj).externalName);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return externalName.hashCode();
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the name of the index.
   * 
   * @return the name of the index
   */
  @Override
  public String toString() {
    return getName();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ImmutableFloatingRateName}.
   * @return the meta-bean, not null
   */
  public static ImmutableFloatingRateName.Meta meta() {
    return ImmutableFloatingRateName.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ImmutableFloatingRateName.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private ImmutableFloatingRateName(
      String externalName,
      String indexName,
      FloatingRateType type) {
    JodaBeanUtils.notEmpty(externalName, "externalName");
    JodaBeanUtils.notEmpty(indexName, "indexName");
    JodaBeanUtils.notNull(type, "type");
    this.externalName = externalName;
    this.indexName = indexName;
    this.type = type;
  }

  @Override
  public ImmutableFloatingRateName.Meta metaBean() {
    return ImmutableFloatingRateName.Meta.INSTANCE;
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
   * Gets the external name, typically from FpML, such as 'GBP-LIBOR-BBA'.
   * @return the value of the property, not empty
   */
  public String getExternalName() {
    return externalName;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the root of the name of the index, such as 'GBP-LIBOR', to which the tenor is appended.
   * This name matches that used by {@link IborIndex} or {@link OvernightIndex}.
   * Typically, multiple {@code FloatingRateName} names map to one Ibor or Overnight index.
   * @return the value of the property, not empty
   */
  public String getIndexName() {
    return indexName;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the type of the index.
   * @return the value of the property, not null
   */
  @Override
  public FloatingRateType getType() {
    return type;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ImmutableFloatingRateName}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code externalName} property.
     */
    private final MetaProperty<String> externalName = DirectMetaProperty.ofImmutable(
        this, "externalName", ImmutableFloatingRateName.class, String.class);
    /**
     * The meta-property for the {@code indexName} property.
     */
    private final MetaProperty<String> indexName = DirectMetaProperty.ofImmutable(
        this, "indexName", ImmutableFloatingRateName.class, String.class);
    /**
     * The meta-property for the {@code type} property.
     */
    private final MetaProperty<FloatingRateType> type = DirectMetaProperty.ofImmutable(
        this, "type", ImmutableFloatingRateName.class, FloatingRateType.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "externalName",
        "indexName",
        "type");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1386121994:  // externalName
          return externalName;
        case -807707011:  // indexName
          return indexName;
        case 3575610:  // type
          return type;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends ImmutableFloatingRateName> builder() {
      return new ImmutableFloatingRateName.Builder();
    }

    @Override
    public Class<? extends ImmutableFloatingRateName> beanType() {
      return ImmutableFloatingRateName.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code externalName} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> externalName() {
      return externalName;
    }

    /**
     * The meta-property for the {@code indexName} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> indexName() {
      return indexName;
    }

    /**
     * The meta-property for the {@code type} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FloatingRateType> type() {
      return type;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1386121994:  // externalName
          return ((ImmutableFloatingRateName) bean).getExternalName();
        case -807707011:  // indexName
          return ((ImmutableFloatingRateName) bean).getIndexName();
        case 3575610:  // type
          return ((ImmutableFloatingRateName) bean).getType();
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
   * The bean-builder for {@code ImmutableFloatingRateName}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<ImmutableFloatingRateName> {

    private String externalName;
    private String indexName;
    private FloatingRateType type;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1386121994:  // externalName
          return externalName;
        case -807707011:  // indexName
          return indexName;
        case 3575610:  // type
          return type;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1386121994:  // externalName
          this.externalName = (String) newValue;
          break;
        case -807707011:  // indexName
          this.indexName = (String) newValue;
          break;
        case 3575610:  // type
          this.type = (FloatingRateType) newValue;
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
    public ImmutableFloatingRateName build() {
      return new ImmutableFloatingRateName(
          externalName,
          indexName,
          type);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("ImmutableFloatingRateName.Builder{");
      buf.append("externalName").append('=').append(JodaBeanUtils.toString(externalName)).append(',').append(' ');
      buf.append("indexName").append('=').append(JodaBeanUtils.toString(indexName)).append(',').append(' ');
      buf.append("type").append('=').append(JodaBeanUtils.toString(type));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
