/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.definition;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.Guavate.toImmutableMap;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.basics.market.ObservableValues;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.value.ValueType;

/**
 * Provides the definition of how to calibrate a group of curves.
 * <p>
 * A curve group contains one or more entries, each of which contains the definition of a curve
 * and a set of market data keys specifying how the curve is to be used.
 * <p>
 * A curve can be used for multiple purposes and therefore the curve itself contains
 * no information about how it is used.
 * <p>
 * In the simple case a curve is only used for a single purpose. For example, if a curve is
 * used for discounting it will be associated with one key of type {@code DiscountCurveKey}.
 * <p>
 * A single curve can also be used as both a discounting curve and a forward curve.
 * In that case its key set would contain a {@code DiscountCurveKey} and a {@code RateIndexCurveKey}.
 * <p>
 * Every curve must be associated with at least once key.
 */
@BeanDefinition(builderScope = "private")
public final class CurveGroupDefinition
    implements ImmutableBean, Serializable {

  /**
   * The name of the curve group.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveGroupName name;
  /**
   * The configuration for building the curves in the group.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<CurveGroupEntry> entries;
  /**
   * Entries for the curves, keyed by the curve name.
   */
  private final ImmutableMap<CurveName, CurveGroupEntry> entriesByName;

  //-------------------------------------------------------------------------
  /**
   * Returns a mutable builder for building the definition for a curve group.
   *
   * @return a mutable builder for building the definition for a curve group
   */
  public static CurveGroupDefinitionBuilder builder() {
    return new CurveGroupDefinitionBuilder();
  }

  /**
   * Package-private constructor used by the builder.
   *
   * @param name  the name of the curve group
   * @param entries  details of the curves in the group
   */
  @ImmutableConstructor
  CurveGroupDefinition(CurveGroupName name, List<CurveGroupEntry> entries) {
    this.name = ArgChecker.notNull(name, "name");
    this.entries = ImmutableList.copyOf(entries);
    entriesByName = entries.stream().collect(toImmutableMap(entry -> entry.getCurveDefinition().getName(), entry -> entry));
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the entry for the curve group with the specified name.
   * <p>
   * If the group is not found, optional empty is returned.
   *
   * @param curveName  the name of the curve
   * @return the entry for the curve group with the specified name
   */
  public Optional<CurveGroupEntry> findEntry(CurveName curveName) {
    return Optional.ofNullable(entriesByName.get(curveName));
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the total number of parameters in the group.
   * <p>
   * This returns the total number of parameters in the group, which equals the number of nodes.
   * The result of {@link #trades(LocalDate, ObservableValues)} and
   * {@link #initialGuesses(LocalDate, ObservableValues)} will be of this size.
   * 
   * @return the number of parameters
   */
  public int getTotalParameterCount() {
    return entries.stream()
        .mapToInt(entry -> entry.getCurveDefinition().getParameterCount())
        .sum();
  }

  /**
   * Creates a list of trades representing the instrument at each node.
   * <p>
   * This uses the observed market data to build the trade that each node represents.
   * The result combines the list of trades from each curve in order.
   *
   * @param valuationDate  the valuation date used when calibrating the curve
   * @param marketData  the market data required to build a trade for the instrument
   * @return the list of all trades
   */
  public ImmutableList<Trade> trades(LocalDate valuationDate, ObservableValues marketData) {
    return entries.stream()
        .map(CurveGroupEntry::getCurveDefinition)
        .flatMap(curveDef -> curveDef.getNodes().stream())
        .map(node -> node.trade(valuationDate, marketData))
        .collect(toImmutableList());
  }

  /**
   * Gets the list of all initial guesses.
   * <p>
   * This returns a list that combines the list of initial guesses from each curve in order.
   * 
   * @param valuationDate  the valuation date used when calibrating the curve
   * @param marketData  the market data required to build a trade for the instrument
   * @return the list of all initial guesses
   */
  public ImmutableList<Double> initialGuesses(LocalDate valuationDate, ObservableValues marketData) {
    ImmutableList.Builder<Double> result = ImmutableList.builder();
    for (CurveGroupEntry entry : entries) {
      NodalCurveDefinition defn = entry.getCurveDefinition();
      ValueType valueType = defn.getYValueType();
      for (CurveNode node : defn.getNodes()) {
        result.add(node.initialGuess(valuationDate, marketData, valueType));
      }
    }
    return result.build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CurveGroupDefinition}.
   * @return the meta-bean, not null
   */
  public static CurveGroupDefinition.Meta meta() {
    return CurveGroupDefinition.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CurveGroupDefinition.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public CurveGroupDefinition.Meta metaBean() {
    return CurveGroupDefinition.Meta.INSTANCE;
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
   * Gets the name of the curve group.
   * @return the value of the property, not null
   */
  public CurveGroupName getName() {
    return name;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the configuration for building the curves in the group.
   * @return the value of the property, not null
   */
  public ImmutableList<CurveGroupEntry> getEntries() {
    return entries;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CurveGroupDefinition other = (CurveGroupDefinition) obj;
      return JodaBeanUtils.equal(getName(), other.getName()) &&
          JodaBeanUtils.equal(getEntries(), other.getEntries());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getName());
    hash = hash * 31 + JodaBeanUtils.hashCode(getEntries());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("CurveGroupDefinition{");
    buf.append("name").append('=').append(getName()).append(',').append(' ');
    buf.append("entries").append('=').append(JodaBeanUtils.toString(getEntries()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CurveGroupDefinition}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<CurveGroupName> name = DirectMetaProperty.ofImmutable(
        this, "name", CurveGroupDefinition.class, CurveGroupName.class);
    /**
     * The meta-property for the {@code entries} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<CurveGroupEntry>> entries = DirectMetaProperty.ofImmutable(
        this, "entries", CurveGroupDefinition.class, (Class) ImmutableList.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "name",
        "entries");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case -1591573360:  // entries
          return entries;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends CurveGroupDefinition> builder() {
      return new CurveGroupDefinition.Builder();
    }

    @Override
    public Class<? extends CurveGroupDefinition> beanType() {
      return CurveGroupDefinition.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code name} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveGroupName> name() {
      return name;
    }

    /**
     * The meta-property for the {@code entries} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<CurveGroupEntry>> entries() {
      return entries;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return ((CurveGroupDefinition) bean).getName();
        case -1591573360:  // entries
          return ((CurveGroupDefinition) bean).getEntries();
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
   * The bean-builder for {@code CurveGroupDefinition}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<CurveGroupDefinition> {

    private CurveGroupName name;
    private List<CurveGroupEntry> entries = ImmutableList.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case -1591573360:  // entries
          return entries;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          this.name = (CurveGroupName) newValue;
          break;
        case -1591573360:  // entries
          this.entries = (List<CurveGroupEntry>) newValue;
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
    public CurveGroupDefinition build() {
      return new CurveGroupDefinition(
          name,
          entries);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("CurveGroupDefinition.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("entries").append('=').append(JodaBeanUtils.toString(entries));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
