/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.data.ObservableId;

/**
 * A set of market data which combines two underlying sets of data.
 * <p>
 * If the same item of data is available in both sets, it will be taken from the first.
 * <p>
 * The underlying sets must contain the same number of scenarios, or one of them must have one scenario.
 * If one of the underlying sets of data has one scenario the combined set will have the scenario count
 * of the other set.
 */
@BeanDefinition(style = "light", constructorScope = "package")
final class CombinedScenarioMarketData
    implements ScenarioMarketData, ImmutableBean, Serializable {

  /**
   * The first set of market data.
   */
  @PropertyDefinition(validate = "notNull")
  private final ScenarioMarketData underlying1;
  /**
   * The second set of market data.
   */
  @PropertyDefinition(validate = "notNull")
  private final ScenarioMarketData underlying2;
  /**
   * The number of scenarios for which market data is available.
   */
  @PropertyDefinition(overrideGet = true)
  private final int scenarioCount;

  //-------------------------------------------------------------------------
  /**
   * Creates a new instance.
   *
   * @param underlying1  the first underlying set of market data
   * @param underlying2  the second underlying set of market data
   */
  CombinedScenarioMarketData(ScenarioMarketData underlying1, ScenarioMarketData underlying2) {
    this.underlying1 = underlying1;
    this.underlying2 = underlying2;

    if (underlying1.getScenarioCount() == 1) {
      scenarioCount = underlying2.getScenarioCount();
    } else if (underlying2.getScenarioCount() == 1) {
      scenarioCount = underlying1.getScenarioCount();
    } else if (underlying1.getScenarioCount() == underlying2.getScenarioCount()) {
      scenarioCount = underlying1.getScenarioCount();
    } else {
      throw new IllegalArgumentException(
          Messages.format(
              "When combining scenario market data, both sets of data must have the same number of scenarios or one " +
                  "of them must have one scenario. Found {} and {} scenarios",
              underlying1.getScenarioCount(),
              underlying2.getScenarioCount()));
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public MarketDataBox<LocalDate> getValuationDate() {
    return underlying1.getValuationDate();
  }

  @Override
  public boolean containsValue(MarketDataId<?> id) {
    return underlying1.containsValue(id) || underlying2.containsValue(id);
  }

  @Override
  public <T> MarketDataBox<T> getValue(MarketDataId<T> id) {
    Optional<MarketDataBox<T>> value1 = underlying1.findValue(id);
    return value1.isPresent() ? value1.get() : underlying2.getValue(id);
  }

  @Override
  public <T> Optional<MarketDataBox<T>> findValue(MarketDataId<T> id) {
    Optional<MarketDataBox<T>> value1 = underlying1.findValue(id);
    return value1.isPresent() ? value1 : underlying2.findValue(id);
  }

  @Override
  public Set<MarketDataId<?>> getIds() {
    return ImmutableSet.<MarketDataId<?>>builder()
        .addAll(underlying1.getIds())
        .addAll(underlying2.getIds())
        .build();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Set<MarketDataId<T>> findIds(MarketDataName<T> name) {
    return ImmutableSet.<MarketDataId<T>>builder()
        .addAll(underlying1.findIds(name))
        .addAll(underlying2.findIds(name))
        .build();
  }

  @Override
  public LocalDateDoubleTimeSeries getTimeSeries(ObservableId id) {
    LocalDateDoubleTimeSeries timeSeries = underlying1.getTimeSeries(id);
    return !timeSeries.isEmpty() ? timeSeries : underlying2.getTimeSeries(id);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CombinedScenarioMarketData}.
   */
  private static MetaBean META_BEAN = LightMetaBean.of(CombinedScenarioMarketData.class);

  /**
   * The meta-bean for {@code CombinedScenarioMarketData}.
   * @return the meta-bean, not null
   */
  public static MetaBean meta() {
    return META_BEAN;
  }

  static {
    JodaBeanUtils.registerMetaBean(META_BEAN);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Creates an instance.
   * @param underlying1  the value of the property, not null
   * @param underlying2  the value of the property, not null
   * @param scenarioCount  the value of the property
   */
  CombinedScenarioMarketData(
      ScenarioMarketData underlying1,
      ScenarioMarketData underlying2,
      int scenarioCount) {
    JodaBeanUtils.notNull(underlying1, "underlying1");
    JodaBeanUtils.notNull(underlying2, "underlying2");
    this.underlying1 = underlying1;
    this.underlying2 = underlying2;
    this.scenarioCount = scenarioCount;
  }

  @Override
  public MetaBean metaBean() {
    return META_BEAN;
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
   * Gets the first set of market data.
   * @return the value of the property, not null
   */
  public ScenarioMarketData getUnderlying1() {
    return underlying1;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the second set of market data.
   * @return the value of the property, not null
   */
  public ScenarioMarketData getUnderlying2() {
    return underlying2;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the number of scenarios for which market data is available.
   * @return the value of the property
   */
  @Override
  public int getScenarioCount() {
    return scenarioCount;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CombinedScenarioMarketData other = (CombinedScenarioMarketData) obj;
      return JodaBeanUtils.equal(underlying1, other.underlying1) &&
          JodaBeanUtils.equal(underlying2, other.underlying2) &&
          (scenarioCount == other.scenarioCount);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(underlying1);
    hash = hash * 31 + JodaBeanUtils.hashCode(underlying2);
    hash = hash * 31 + JodaBeanUtils.hashCode(scenarioCount);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("CombinedScenarioMarketData{");
    buf.append("underlying1").append('=').append(underlying1).append(',').append(' ');
    buf.append("underlying2").append('=').append(underlying2).append(',').append(' ');
    buf.append("scenarioCount").append('=').append(JodaBeanUtils.toString(scenarioCount));
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
