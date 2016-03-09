/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.id;

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

import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.FieldName;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.basics.market.StandardId;
import com.opengamma.strata.market.key.IndexRateKey;

/**
 * A market data ID identifying the current and historical values for an {@link Index}.
 * <p>
 * The forward curve of the index is identified with a separate ID, such as an {@link IborIndexCurveId}.
 */
@BeanDefinition(builderScope = "private")
public final class IndexRateId implements ObservableId, ImmutableBean, Serializable {

  /** The index. */
  @PropertyDefinition(validate = "notNull")
  private final Index index;

  /**
   * The field name in the market data record that contains the market data item, for example
   * {@linkplain FieldName#MARKET_VALUE market value}.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final FieldName fieldName;

  /** The market data feed from which the market data should be retrieved. */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final MarketDataFeed marketDataFeed;

  /**
   * Returns an ID for market data for the specified index.
   * <p>
   * The field name containing the data is {@link FieldName#MARKET_VALUE} and the market
   * data feed is {@link MarketDataFeed#NONE}.
   *
   * @param index  the index
   * @return an ID for the market data for the specified index
   */
  public static IndexRateId of(Index index) {
    return new IndexRateId(index, FieldName.MARKET_VALUE, MarketDataFeed.NONE);
  }

  /**
   * Returns an ID for market data for the specified index.
   * <p>
   * The field name containing the data is {@link FieldName#MARKET_VALUE}.
   *
   * @param index  the index
   * @param feed  the market data feed from which the market data should be retrieved
   * @return an ID for the market data for the specified index
   */
  public static IndexRateId of(Index index, MarketDataFeed feed) {
    return new IndexRateId(index, FieldName.MARKET_VALUE, feed);
  }

  /**
   * Returns an ID for the curve for the specified index.
   *
   * @param index  the index
   * @param feed  the market data feed from which the market data should be retrieved
   * @param fieldName  the field name in the market data record that contains the market data item
   * @return an ID for the market data for the specified index
   */
  public static IndexRateId of(Index index, MarketDataFeed feed, FieldName fieldName) {
    return new IndexRateId(index, fieldName, feed);
  }

  @Override
  public StandardId getStandardId() {
    return index.getStandardId();
  }

  @Override
  public IndexRateKey toMarketDataKey() {
    return IndexRateKey.of(index, fieldName);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IndexRateId}.
   * @return the meta-bean, not null
   */
  public static IndexRateId.Meta meta() {
    return IndexRateId.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IndexRateId.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private IndexRateId(
      Index index,
      FieldName fieldName,
      MarketDataFeed marketDataFeed) {
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(fieldName, "fieldName");
    JodaBeanUtils.notNull(marketDataFeed, "marketDataFeed");
    this.index = index;
    this.fieldName = fieldName;
    this.marketDataFeed = marketDataFeed;
  }

  @Override
  public IndexRateId.Meta metaBean() {
    return IndexRateId.Meta.INSTANCE;
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
   * Gets the index.
   * @return the value of the property, not null
   */
  public Index getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the field name in the market data record that contains the market data item, for example
   * {@linkplain FieldName#MARKET_VALUE market value}.
   * @return the value of the property, not null
   */
  @Override
  public FieldName getFieldName() {
    return fieldName;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the market data feed from which the market data should be retrieved.
   * @return the value of the property, not null
   */
  @Override
  public MarketDataFeed getMarketDataFeed() {
    return marketDataFeed;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      IndexRateId other = (IndexRateId) obj;
      return JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(fieldName, other.fieldName) &&
          JodaBeanUtils.equal(marketDataFeed, other.marketDataFeed);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(fieldName);
    hash = hash * 31 + JodaBeanUtils.hashCode(marketDataFeed);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("IndexRateId{");
    buf.append("index").append('=').append(index).append(',').append(' ');
    buf.append("fieldName").append('=').append(fieldName).append(',').append(' ');
    buf.append("marketDataFeed").append('=').append(JodaBeanUtils.toString(marketDataFeed));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IndexRateId}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<Index> index = DirectMetaProperty.ofImmutable(
        this, "index", IndexRateId.class, Index.class);
    /**
     * The meta-property for the {@code fieldName} property.
     */
    private final MetaProperty<FieldName> fieldName = DirectMetaProperty.ofImmutable(
        this, "fieldName", IndexRateId.class, FieldName.class);
    /**
     * The meta-property for the {@code marketDataFeed} property.
     */
    private final MetaProperty<MarketDataFeed> marketDataFeed = DirectMetaProperty.ofImmutable(
        this, "marketDataFeed", IndexRateId.class, MarketDataFeed.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "fieldName",
        "marketDataFeed");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case 1265009317:  // fieldName
          return fieldName;
        case 842621124:  // marketDataFeed
          return marketDataFeed;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends IndexRateId> builder() {
      return new IndexRateId.Builder();
    }

    @Override
    public Class<? extends IndexRateId> beanType() {
      return IndexRateId.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Index> index() {
      return index;
    }

    /**
     * The meta-property for the {@code fieldName} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FieldName> fieldName() {
      return fieldName;
    }

    /**
     * The meta-property for the {@code marketDataFeed} property.
     * @return the meta-property, not null
     */
    public MetaProperty<MarketDataFeed> marketDataFeed() {
      return marketDataFeed;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return ((IndexRateId) bean).getIndex();
        case 1265009317:  // fieldName
          return ((IndexRateId) bean).getFieldName();
        case 842621124:  // marketDataFeed
          return ((IndexRateId) bean).getMarketDataFeed();
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
   * The bean-builder for {@code IndexRateId}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<IndexRateId> {

    private Index index;
    private FieldName fieldName;
    private MarketDataFeed marketDataFeed;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case 1265009317:  // fieldName
          return fieldName;
        case 842621124:  // marketDataFeed
          return marketDataFeed;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          this.index = (Index) newValue;
          break;
        case 1265009317:  // fieldName
          this.fieldName = (FieldName) newValue;
          break;
        case 842621124:  // marketDataFeed
          this.marketDataFeed = (MarketDataFeed) newValue;
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
    public IndexRateId build() {
      return new IndexRateId(
          index,
          fieldName,
          marketDataFeed);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("IndexRateId.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("fieldName").append('=').append(JodaBeanUtils.toString(fieldName)).append(',').append(' ');
      buf.append("marketDataFeed").append('=').append(JodaBeanUtils.toString(marketDataFeed));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
