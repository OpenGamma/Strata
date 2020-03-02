/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.StandardId;

/**
 * Additional information about a trade.
 * <p>
 * This allows additional information about a trade to be associated.
 * It is kept in a separate object as the information is optional for pricing.
 */
@BeanDefinition(builderScope = "private", constructorScope = "package")
public final class TradeInfo
    implements PortfolioItemInfo, ImmutableBean, Serializable {

  /**
   * An empty instance of {@code TradeInfo}.
   */
  private static final TradeInfo EMPTY = TradeInfo.builder().build();

  /**
   * The primary identifier for the trade, optional.
   * <p>
   * The identifier is used to identify the trade.
   * It will typically be an identifier in an external data system.
   * <p>
   * A trade may have multiple active identifiers. Any identifier may be chosen here.
   * Certain uses of the identifier, such as storage in a database, require that the
   * identifier does not change over time, and this should be considered best practice.
   */
  @PropertyDefinition(get = "optional", overrideGet = true)
  private final StandardId id;
  /**
   * The counterparty identifier, optional.
   * <p>
   * An identifier used to specify the counterparty of the trade.
   */
  @PropertyDefinition(get = "optional")
  private final StandardId counterparty;
  /**
   * The trade date, optional.
   */
  @PropertyDefinition(get = "optional")
  private final LocalDate tradeDate;
  /**
   * The trade time, optional.
   */
  @PropertyDefinition(get = "optional")
  private final LocalTime tradeTime;
  /**
   * The trade time-zone, optional.
   */
  @PropertyDefinition(get = "optional")
  private final ZoneId zone;
  /**
   * The settlement date, optional.
   */
  @PropertyDefinition(get = "optional")
  private final LocalDate settlementDate;
  /**
   * The trade attributes.
   * <p>
   * Trade attributes provide the ability to associate arbitrary information in a key-value map.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<AttributeType<?>, Object> attributes;

  //-------------------------------------------------------------------------
  /**
   * Obtains an empty instance, with no values or attributes.
   * 
   * @return the empty instance
   */
  public static TradeInfo empty() {
    return EMPTY;
  }

  /**
   * Obtains an instance with the specified trade date.
   * 
   * @param tradeDate  the trade date
   * @return the trade information
   */
  public static TradeInfo of(LocalDate tradeDate) {
    return new TradeInfo(null, null, tradeDate, null, null, null, ImmutableMap.of());
  }

  /**
   * Obtains an instance based on the supplied info.
   *
   * @param info  the base info
   * @return the trade information
   */
  public static TradeInfo from(PortfolioItemInfo info) {
    if (info instanceof TradeInfo) {
      return ((TradeInfo) info);
    }
    return empty().combinedWith(info);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * 
   * @return the builder, not null
   */
  public static TradeInfoBuilder builder() {
    return new TradeInfoBuilder();
  }

  //-------------------------------------------------------------------------
  @Override
  public TradeInfo withId(StandardId identifier) {
    return new TradeInfo(identifier, counterparty, tradeDate, tradeTime, zone, settlementDate, attributes);
  }

  //-------------------------------------------------------------------------
  @Override
  public ImmutableSet<AttributeType<?>> getAttributeTypes() {
    return attributes.keySet();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Optional<T> findAttribute(AttributeType<T> type) {
    return Optional.ofNullable(type.fromStoredForm(attributes.get(type)));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> TradeInfo withAttribute(AttributeType<T> type, T value) {
    // ImmutableMap.Builder would not provide Map.put semantics
    Map<AttributeType<?>, Object> updatedAttributes = new HashMap<>(attributes);
    if (value == null) {
      updatedAttributes.remove(type);
    } else {
      updatedAttributes.put(type, type.toStoredForm(value));
    }
    return new TradeInfo(id, counterparty, tradeDate, tradeTime, zone, settlementDate, updatedAttributes);
  }

  @Override
  public TradeInfo withAttributes(Attributes other) {
    TradeInfoBuilder builder = toBuilder();
    for (AttributeType<?> attrType : other.getAttributeTypes()) {
      builder.addAttribute(attrType.captureWildcard(), other.getAttribute(attrType));
    }
    return builder.build();
  }

  @Override
  public TradeInfo combinedWith(PortfolioItemInfo other) {
    TradeInfoBuilder builder = toBuilder();
    other.getId().filter(ignored -> this.id == null).ifPresent(builder::id);
    if (other instanceof TradeInfo) {
      TradeInfo otherInfo = (TradeInfo) other;
      otherInfo.getCounterparty().filter(ignored -> this.counterparty == null).ifPresent(builder::counterparty);
      otherInfo.getTradeDate().filter(ignored -> this.tradeDate == null).ifPresent(builder::tradeDate);
      otherInfo.getTradeTime().filter(ignored -> this.tradeTime == null).ifPresent(builder::tradeTime);
      otherInfo.getZone().filter(ignored -> this.zone == null).ifPresent(builder::zone);
      otherInfo.getSettlementDate().filter(ignored -> this.settlementDate == null).ifPresent(builder::settlementDate);
    }
    for (AttributeType<?> attrType : other.getAttributeTypes()) {
      if (!attributes.keySet().contains(attrType)) {
        builder.addAttribute(attrType.captureWildcard(), other.getAttribute(attrType));
      }
    }
    return builder.build();
  }

  @Override
  public TradeInfo overrideWith(PortfolioItemInfo other) {
    TradeInfoBuilder builder = toBuilder();
    other.getId().ifPresent(builder::id);
    if (other instanceof TradeInfo) {
      TradeInfo otherInfo = (TradeInfo) other;
      otherInfo.getCounterparty().ifPresent(builder::counterparty);
      otherInfo.getTradeDate().ifPresent(builder::tradeDate);
      otherInfo.getTradeTime().ifPresent(builder::tradeTime);
      otherInfo.getZone().ifPresent(builder::zone);
      otherInfo.getSettlementDate().ifPresent(builder::settlementDate);
    }
    for (AttributeType<?> attrType : other.getAttributeTypes()) {
      builder.addAttribute(attrType.captureWildcard(), other.getAttribute(attrType));
    }
    return builder.build();
  }

  /**
   * Returns a builder populated with the values of this instance.
   *
   * @return a builder populated with the values of this instance
   */
  public TradeInfoBuilder toBuilder() {
    return new TradeInfoBuilder(id, counterparty, tradeDate, tradeTime, zone, settlementDate, attributes);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code TradeInfo}.
   * @return the meta-bean, not null
   */
  public static TradeInfo.Meta meta() {
    return TradeInfo.Meta.INSTANCE;
  }

  static {
    MetaBean.register(TradeInfo.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Creates an instance.
   * @param id  the value of the property
   * @param counterparty  the value of the property
   * @param tradeDate  the value of the property
   * @param tradeTime  the value of the property
   * @param zone  the value of the property
   * @param settlementDate  the value of the property
   * @param attributes  the value of the property, not null
   */
  TradeInfo(
      StandardId id,
      StandardId counterparty,
      LocalDate tradeDate,
      LocalTime tradeTime,
      ZoneId zone,
      LocalDate settlementDate,
      Map<AttributeType<?>, Object> attributes) {
    JodaBeanUtils.notNull(attributes, "attributes");
    this.id = id;
    this.counterparty = counterparty;
    this.tradeDate = tradeDate;
    this.tradeTime = tradeTime;
    this.zone = zone;
    this.settlementDate = settlementDate;
    this.attributes = ImmutableMap.copyOf(attributes);
  }

  @Override
  public TradeInfo.Meta metaBean() {
    return TradeInfo.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the primary identifier for the trade, optional.
   * <p>
   * The identifier is used to identify the trade.
   * It will typically be an identifier in an external data system.
   * <p>
   * A trade may have multiple active identifiers. Any identifier may be chosen here.
   * Certain uses of the identifier, such as storage in a database, require that the
   * identifier does not change over time, and this should be considered best practice.
   * @return the optional value of the property, not null
   */
  @Override
  public Optional<StandardId> getId() {
    return Optional.ofNullable(id);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the counterparty identifier, optional.
   * <p>
   * An identifier used to specify the counterparty of the trade.
   * @return the optional value of the property, not null
   */
  public Optional<StandardId> getCounterparty() {
    return Optional.ofNullable(counterparty);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the trade date, optional.
   * @return the optional value of the property, not null
   */
  public Optional<LocalDate> getTradeDate() {
    return Optional.ofNullable(tradeDate);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the trade time, optional.
   * @return the optional value of the property, not null
   */
  public Optional<LocalTime> getTradeTime() {
    return Optional.ofNullable(tradeTime);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the trade time-zone, optional.
   * @return the optional value of the property, not null
   */
  public Optional<ZoneId> getZone() {
    return Optional.ofNullable(zone);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the settlement date, optional.
   * @return the optional value of the property, not null
   */
  public Optional<LocalDate> getSettlementDate() {
    return Optional.ofNullable(settlementDate);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the trade attributes.
   * <p>
   * Trade attributes provide the ability to associate arbitrary information in a key-value map.
   * @return the value of the property, not null
   */
  public ImmutableMap<AttributeType<?>, Object> getAttributes() {
    return attributes;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      TradeInfo other = (TradeInfo) obj;
      return JodaBeanUtils.equal(id, other.id) &&
          JodaBeanUtils.equal(counterparty, other.counterparty) &&
          JodaBeanUtils.equal(tradeDate, other.tradeDate) &&
          JodaBeanUtils.equal(tradeTime, other.tradeTime) &&
          JodaBeanUtils.equal(zone, other.zone) &&
          JodaBeanUtils.equal(settlementDate, other.settlementDate) &&
          JodaBeanUtils.equal(attributes, other.attributes);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(id);
    hash = hash * 31 + JodaBeanUtils.hashCode(counterparty);
    hash = hash * 31 + JodaBeanUtils.hashCode(tradeDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(tradeTime);
    hash = hash * 31 + JodaBeanUtils.hashCode(zone);
    hash = hash * 31 + JodaBeanUtils.hashCode(settlementDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(attributes);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("TradeInfo{");
    buf.append("id").append('=').append(JodaBeanUtils.toString(id)).append(',').append(' ');
    buf.append("counterparty").append('=').append(JodaBeanUtils.toString(counterparty)).append(',').append(' ');
    buf.append("tradeDate").append('=').append(JodaBeanUtils.toString(tradeDate)).append(',').append(' ');
    buf.append("tradeTime").append('=').append(JodaBeanUtils.toString(tradeTime)).append(',').append(' ');
    buf.append("zone").append('=').append(JodaBeanUtils.toString(zone)).append(',').append(' ');
    buf.append("settlementDate").append('=').append(JodaBeanUtils.toString(settlementDate)).append(',').append(' ');
    buf.append("attributes").append('=').append(JodaBeanUtils.toString(attributes));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code TradeInfo}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code id} property.
     */
    private final MetaProperty<StandardId> id = DirectMetaProperty.ofImmutable(
        this, "id", TradeInfo.class, StandardId.class);
    /**
     * The meta-property for the {@code counterparty} property.
     */
    private final MetaProperty<StandardId> counterparty = DirectMetaProperty.ofImmutable(
        this, "counterparty", TradeInfo.class, StandardId.class);
    /**
     * The meta-property for the {@code tradeDate} property.
     */
    private final MetaProperty<LocalDate> tradeDate = DirectMetaProperty.ofImmutable(
        this, "tradeDate", TradeInfo.class, LocalDate.class);
    /**
     * The meta-property for the {@code tradeTime} property.
     */
    private final MetaProperty<LocalTime> tradeTime = DirectMetaProperty.ofImmutable(
        this, "tradeTime", TradeInfo.class, LocalTime.class);
    /**
     * The meta-property for the {@code zone} property.
     */
    private final MetaProperty<ZoneId> zone = DirectMetaProperty.ofImmutable(
        this, "zone", TradeInfo.class, ZoneId.class);
    /**
     * The meta-property for the {@code settlementDate} property.
     */
    private final MetaProperty<LocalDate> settlementDate = DirectMetaProperty.ofImmutable(
        this, "settlementDate", TradeInfo.class, LocalDate.class);
    /**
     * The meta-property for the {@code attributes} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<AttributeType<?>, Object>> attributes = DirectMetaProperty.ofImmutable(
        this, "attributes", TradeInfo.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "id",
        "counterparty",
        "tradeDate",
        "tradeTime",
        "zone",
        "settlementDate",
        "attributes");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          return id;
        case -1651301782:  // counterparty
          return counterparty;
        case 752419634:  // tradeDate
          return tradeDate;
        case 752903761:  // tradeTime
          return tradeTime;
        case 3744684:  // zone
          return zone;
        case -295948169:  // settlementDate
          return settlementDate;
        case 405645655:  // attributes
          return attributes;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends TradeInfo> builder() {
      return new TradeInfo.Builder();
    }

    @Override
    public Class<? extends TradeInfo> beanType() {
      return TradeInfo.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code id} property.
     * @return the meta-property, not null
     */
    public MetaProperty<StandardId> id() {
      return id;
    }

    /**
     * The meta-property for the {@code counterparty} property.
     * @return the meta-property, not null
     */
    public MetaProperty<StandardId> counterparty() {
      return counterparty;
    }

    /**
     * The meta-property for the {@code tradeDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> tradeDate() {
      return tradeDate;
    }

    /**
     * The meta-property for the {@code tradeTime} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalTime> tradeTime() {
      return tradeTime;
    }

    /**
     * The meta-property for the {@code zone} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ZoneId> zone() {
      return zone;
    }

    /**
     * The meta-property for the {@code settlementDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> settlementDate() {
      return settlementDate;
    }

    /**
     * The meta-property for the {@code attributes} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<AttributeType<?>, Object>> attributes() {
      return attributes;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          return ((TradeInfo) bean).id;
        case -1651301782:  // counterparty
          return ((TradeInfo) bean).counterparty;
        case 752419634:  // tradeDate
          return ((TradeInfo) bean).tradeDate;
        case 752903761:  // tradeTime
          return ((TradeInfo) bean).tradeTime;
        case 3744684:  // zone
          return ((TradeInfo) bean).zone;
        case -295948169:  // settlementDate
          return ((TradeInfo) bean).settlementDate;
        case 405645655:  // attributes
          return ((TradeInfo) bean).getAttributes();
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
   * The bean-builder for {@code TradeInfo}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<TradeInfo> {

    private StandardId id;
    private StandardId counterparty;
    private LocalDate tradeDate;
    private LocalTime tradeTime;
    private ZoneId zone;
    private LocalDate settlementDate;
    private Map<AttributeType<?>, Object> attributes = ImmutableMap.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          return id;
        case -1651301782:  // counterparty
          return counterparty;
        case 752419634:  // tradeDate
          return tradeDate;
        case 752903761:  // tradeTime
          return tradeTime;
        case 3744684:  // zone
          return zone;
        case -295948169:  // settlementDate
          return settlementDate;
        case 405645655:  // attributes
          return attributes;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          this.id = (StandardId) newValue;
          break;
        case -1651301782:  // counterparty
          this.counterparty = (StandardId) newValue;
          break;
        case 752419634:  // tradeDate
          this.tradeDate = (LocalDate) newValue;
          break;
        case 752903761:  // tradeTime
          this.tradeTime = (LocalTime) newValue;
          break;
        case 3744684:  // zone
          this.zone = (ZoneId) newValue;
          break;
        case -295948169:  // settlementDate
          this.settlementDate = (LocalDate) newValue;
          break;
        case 405645655:  // attributes
          this.attributes = (Map<AttributeType<?>, Object>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public TradeInfo build() {
      return new TradeInfo(
          id,
          counterparty,
          tradeDate,
          tradeTime,
          zone,
          settlementDate,
          attributes);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(256);
      buf.append("TradeInfo.Builder{");
      buf.append("id").append('=').append(JodaBeanUtils.toString(id)).append(',').append(' ');
      buf.append("counterparty").append('=').append(JodaBeanUtils.toString(counterparty)).append(',').append(' ');
      buf.append("tradeDate").append('=').append(JodaBeanUtils.toString(tradeDate)).append(',').append(' ');
      buf.append("tradeTime").append('=').append(JodaBeanUtils.toString(tradeTime)).append(',').append(' ');
      buf.append("zone").append('=').append(JodaBeanUtils.toString(zone)).append(',').append(' ');
      buf.append("settlementDate").append('=').append(JodaBeanUtils.toString(settlementDate)).append(',').append(' ');
      buf.append("attributes").append('=').append(JodaBeanUtils.toString(attributes));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
