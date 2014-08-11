/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata.spec;

import java.util.Map;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.threeten.bp.LocalDate;

import com.opengamma.util.ArgumentChecker;

/**
 * 
 */
@BeanDefinition(hierarchy = "immutable")
public class FixedHistoricalMarketDataSpecification extends HistoricalMarketDataSpecification
    implements MarketDataSpecification {
 
  private static final long serialVersionUID = 1L;
  
  @PropertyDefinition(validate = "notNull")
  private final LocalDate _snapshotDate;

  /**
   * Creates an instance with a snapshotDate
   * 
   * @param snapshotDate the snapshot date, not null
   */
  public FixedHistoricalMarketDataSpecification(LocalDate snapshotDate) {
   this(null, snapshotDate);
  }

  
  /**
   * Creates an instance with a timeSeriesResolverKey and snapshotDate
   * 
   * @param timeSeriesResolverKey the timeseries resolver key, not null
   * @param snapshotDate the snapshot date, not null
   */
  public FixedHistoricalMarketDataSpecification(String timeSeriesResolverKey, LocalDate snapshotDate) {
    super(timeSeriesResolverKey);
    _snapshotDate = ArgumentChecker.notNull(snapshotDate, "snapshotDate");
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FixedHistoricalMarketDataSpecification}.
   * @return the meta-bean, not null
   */
  public static FixedHistoricalMarketDataSpecification.Meta meta() {
    return FixedHistoricalMarketDataSpecification.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FixedHistoricalMarketDataSpecification.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static FixedHistoricalMarketDataSpecification.Builder builder() {
    return new FixedHistoricalMarketDataSpecification.Builder();
  }

  /**
   * Restricted constructor.
   * @param builder  the builder to copy from, not null
   */
  protected FixedHistoricalMarketDataSpecification(FixedHistoricalMarketDataSpecification.Builder builder) {
    super(builder);
    JodaBeanUtils.notNull(builder._snapshotDate, "snapshotDate");
    this._snapshotDate = builder._snapshotDate;
  }

  @Override
  public FixedHistoricalMarketDataSpecification.Meta metaBean() {
    return FixedHistoricalMarketDataSpecification.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the snapshotDate.
   * @return the value of the property, not null
   */
  public LocalDate getSnapshotDate() {
    return _snapshotDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      FixedHistoricalMarketDataSpecification other = (FixedHistoricalMarketDataSpecification) obj;
      return JodaBeanUtils.equal(getSnapshotDate(), other.getSnapshotDate()) &&
          super.equals(obj);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash += hash * 31 + JodaBeanUtils.hashCode(getSnapshotDate());
    return hash ^ super.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("FixedHistoricalMarketDataSpecification{");
    int len = buf.length();
    toString(buf);
    if (buf.length() > len) {
      buf.setLength(buf.length() - 2);
    }
    buf.append('}');
    return buf.toString();
  }

  @Override
  protected void toString(StringBuilder buf) {
    super.toString(buf);
    buf.append("snapshotDate").append('=').append(JodaBeanUtils.toString(getSnapshotDate())).append(',').append(' ');
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FixedHistoricalMarketDataSpecification}.
   */
  public static class Meta extends HistoricalMarketDataSpecification.Meta {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code snapshotDate} property.
     */
    private final MetaProperty<LocalDate> _snapshotDate = DirectMetaProperty.ofImmutable(
        this, "snapshotDate", FixedHistoricalMarketDataSpecification.class, LocalDate.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> _metaPropertyMap$ = new DirectMetaPropertyMap(
        this, (DirectMetaPropertyMap) super.metaPropertyMap(),
        "snapshotDate");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -932005998:  // snapshotDate
          return _snapshotDate;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public FixedHistoricalMarketDataSpecification.Builder builder() {
      return new FixedHistoricalMarketDataSpecification.Builder();
    }

    @Override
    public Class<? extends FixedHistoricalMarketDataSpecification> beanType() {
      return FixedHistoricalMarketDataSpecification.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return _metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code snapshotDate} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<LocalDate> snapshotDate() {
      return _snapshotDate;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -932005998:  // snapshotDate
          return ((FixedHistoricalMarketDataSpecification) bean).getSnapshotDate();
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
   * The bean-builder for {@code FixedHistoricalMarketDataSpecification}.
   */
  public static class Builder extends HistoricalMarketDataSpecification.Builder {

    private LocalDate _snapshotDate;

    /**
     * Restricted constructor.
     */
    protected Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    protected Builder(FixedHistoricalMarketDataSpecification beanToCopy) {
      this._snapshotDate = beanToCopy.getSnapshotDate();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -932005998:  // snapshotDate
          return _snapshotDate;
        default:
          return super.get(propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -932005998:  // snapshotDate
          this._snapshotDate = (LocalDate) newValue;
          break;
        default:
          super.set(propertyName, newValue);
          break;
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
    public FixedHistoricalMarketDataSpecification build() {
      return new FixedHistoricalMarketDataSpecification(this);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code snapshotDate} property in the builder.
     * @param snapshotDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder snapshotDate(LocalDate snapshotDate) {
      JodaBeanUtils.notNull(snapshotDate, "snapshotDate");
      this._snapshotDate = snapshotDate;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("FixedHistoricalMarketDataSpecification.Builder{");
      int len = buf.length();
      toString(buf);
      if (buf.length() > len) {
        buf.setLength(buf.length() - 2);
      }
      buf.append('}');
      return buf.toString();
    }

    @Override
    protected void toString(StringBuilder buf) {
      super.toString(buf);
      buf.append("snapshotDate").append('=').append(JodaBeanUtils.toString(_snapshotDate)).append(',').append(' ');
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
