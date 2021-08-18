/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.explain;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;
import org.joda.convert.StringConvert;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxConvertible;
import com.opengamma.strata.basics.currency.FxRateProvider;

/**
 * A map of explanatory values.
 * <p>
 * This is a loosely defined data structure that allows an explanation of a calculation to be represented.
 */
@BeanDefinition(builderScope = "private")
public final class ExplainMap
    implements FxConvertible<ExplainMap>, ImmutableBean, Serializable {

  /**
   * The map of explanatory values.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<ExplainKey<?>, Object> map;

  /**
   * Creates an instance from a populated map.
   * 
   * @param map  the map
   * @return the explanatory map
   */
  public static ExplainMap of(Map<ExplainKey<?>, Object> map) {
    return new ExplainMap(map);
  }

  /**
   * Creates an instance with no entries.
   *
   * @return the explanatory map
   */
  public static ExplainMap empty() {
    return new ExplainMap(ImmutableMap.of());
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a builder for creating the map.
   * 
   * @return the builder
   */
  public static ExplainMapBuilder builder() {
    return new ExplainMapBuilder();
  }

  //-------------------------------------------------------------------------
  /**
   * Gets a value by key.
   * 
   * @param <R>  the type of the key
   * @param key  the key to lookup
   * @return the value associated with the key
   */
  @SuppressWarnings("unchecked")
  public <R> Optional<R> get(ExplainKey<R> key) {
    return (Optional<R>) Optional.ofNullable(map.get(key));
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the explanation as a string.
   * <p>
   * This returns a multi-line string containing the string form of the entries.
   * 
   * @return the explanation as a string
   */
  public String explanationString() {
    StringBuilder buf = new StringBuilder(1024);
    buf.append("ExplainMap ");
    explanationString(buf, "");
    buf.append(System.lineSeparator());
    return buf.toString();
  }

  // append the explanation with indent
  private void explanationString(StringBuilder buf, String indent) {
    buf.append("{").append(System.lineSeparator());
    String entryIndent = indent + "  ";
    for (Entry<ExplainKey<?>, Object> entry : map.entrySet()) {
      buf.append(entryIndent).append(entry.getKey()).append(" = ");
      if (entry.getValue() instanceof List) {
        // list
        @SuppressWarnings("unchecked")
        List<ExplainMap> list = (List<ExplainMap>) entry.getValue();
        explanationString(buf, entryIndent, list);
      } else {
        // single entry
        try {
          buf.append(StringConvert.INSTANCE.convertToString(entry.getValue()));
        } catch (Exception ex) {
          buf.append(entry.getValue());
        }
      }
      buf.append(',').append(System.lineSeparator());
    }
    if (!map.entrySet().isEmpty()) {
      buf.deleteCharAt(buf.lastIndexOf(","));
    }
    buf.append(indent).append("}");
  }

  // append a list of entries
  private void explanationString(StringBuilder buf, String indent, List<ExplainMap> list) {
    if (list.isEmpty()) {
      buf.append("[]");
    } else {
      buf.append("[");
      for (ExplainMap child : list) {
        child.explanationString(buf, indent);
        buf.append(',');
      }
      buf.setCharAt(buf.length() - 1, ']');
    }
  }

  @Override
  public ExplainMap convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    ImmutableMap.Builder<ExplainKey<?>, Object> builder = ImmutableMap.builder();
    for (Entry<ExplainKey<?>, Object> explainEntry : map.entrySet()) {
      if (explainEntry.getValue() instanceof FxConvertible) {
        Object convertedValue = ((FxConvertible<?>) explainEntry.getValue()).convertedTo(resultCurrency, rateProvider);
        builder.put(explainEntry.getKey(), convertedValue);
      } else {
        builder.put(explainEntry.getKey(), explainEntry.getValue());
      }
    }
    return ExplainMap.of(builder.build());
  }

  //-------------------------------------------------------------------------
  /**
   * Returns whether the explanatory map contains no entries.
   *
   * @return true if no entries are present in the map, false otherwise
   */
  public boolean isEmpty() {
    return map.isEmpty();
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code ExplainMap}.
   * @return the meta-bean, not null
   */
  public static ExplainMap.Meta meta() {
    return ExplainMap.Meta.INSTANCE;
  }

  static {
    MetaBean.register(ExplainMap.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private ExplainMap(
      Map<ExplainKey<?>, Object> map) {
    JodaBeanUtils.notNull(map, "map");
    this.map = ImmutableMap.copyOf(map);
  }

  @Override
  public ExplainMap.Meta metaBean() {
    return ExplainMap.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the map of explanatory values.
   * @return the value of the property, not null
   */
  public ImmutableMap<ExplainKey<?>, Object> getMap() {
    return map;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ExplainMap other = (ExplainMap) obj;
      return JodaBeanUtils.equal(map, other.map);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(map);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("ExplainMap{");
    buf.append("map").append('=').append(JodaBeanUtils.toString(map));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ExplainMap}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code map} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<ExplainKey<?>, Object>> map = DirectMetaProperty.ofImmutable(
        this, "map", ExplainMap.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "map");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 107868:  // map
          return map;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends ExplainMap> builder() {
      return new ExplainMap.Builder();
    }

    @Override
    public Class<? extends ExplainMap> beanType() {
      return ExplainMap.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code map} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<ExplainKey<?>, Object>> map() {
      return map;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 107868:  // map
          return ((ExplainMap) bean).getMap();
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
   * The bean-builder for {@code ExplainMap}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<ExplainMap> {

    private Map<ExplainKey<?>, Object> map = ImmutableMap.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 107868:  // map
          return map;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 107868:  // map
          this.map = (Map<ExplainKey<?>, Object>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public ExplainMap build() {
      return new ExplainMap(
          map);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("ExplainMap.Builder{");
      buf.append("map").append('=').append(JodaBeanUtils.toString(map));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
