/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.array;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Stream;

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

/**
 * An immutable array of {@code double} values, boxed to {@code Double}.
 * <p>
 * This is used to wrap a {@code double[]} to meet the API of {@link Array}.
 * An instance is created using {@link DoubleArray#boxed()}.
 */
@BeanDefinition(constructorScope = "package", builderScope = "private")
final class BoxedDoubleArray
    implements Array<Double>, ImmutableBean, Serializable {

  /**
   * The underlying array of doubles.
   */
  @PropertyDefinition(get = "private")
  private final DoubleArray array;

  //-------------------------------------------------------------------------
  @Override
  public int size() {
    return array.size();
  }

  @Override
  public Double get(int index) {
    return array.get(index);
  }

  @Override
  public List<Double> toList() {
    return array.toList();
  }

  @Override
  public Stream<Double> stream() {
    return array.stream().boxed();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code BoxedDoubleArray}.
   * @return the meta-bean, not null
   */
  public static BoxedDoubleArray.Meta meta() {
    return BoxedDoubleArray.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(BoxedDoubleArray.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Creates an instance.
   * @param array  the value of the property
   */
  BoxedDoubleArray(
      DoubleArray array) {
    this.array = array;
  }

  @Override
  public BoxedDoubleArray.Meta metaBean() {
    return BoxedDoubleArray.Meta.INSTANCE;
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
   * Gets the underlying array of doubles.
   * @return the value of the property
   */
  private DoubleArray getArray() {
    return array;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      BoxedDoubleArray other = (BoxedDoubleArray) obj;
      return JodaBeanUtils.equal(array, other.array);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(array);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("BoxedDoubleArray{");
    buf.append("array").append('=').append(JodaBeanUtils.toString(array));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code BoxedDoubleArray}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code array} property.
     */
    private final MetaProperty<DoubleArray> array = DirectMetaProperty.ofImmutable(
        this, "array", BoxedDoubleArray.class, DoubleArray.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "array");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 93090393:  // array
          return array;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends BoxedDoubleArray> builder() {
      return new BoxedDoubleArray.Builder();
    }

    @Override
    public Class<? extends BoxedDoubleArray> beanType() {
      return BoxedDoubleArray.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code array} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DoubleArray> array() {
      return array;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 93090393:  // array
          return ((BoxedDoubleArray) bean).getArray();
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
   * The bean-builder for {@code BoxedDoubleArray}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<BoxedDoubleArray> {

    private DoubleArray array;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 93090393:  // array
          return array;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 93090393:  // array
          this.array = (DoubleArray) newValue;
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
    public BoxedDoubleArray build() {
      return new BoxedDoubleArray(
          array);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("BoxedDoubleArray.Builder{");
      buf.append("array").append('=').append(JodaBeanUtils.toString(array));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
