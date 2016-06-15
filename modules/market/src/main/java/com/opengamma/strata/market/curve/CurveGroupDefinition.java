/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.Guavate.toImmutableMap;
import static java.util.stream.Collectors.toSet;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Collection;
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
import org.joda.beans.ImmutablePreBuild;
import org.joda.beans.ImmutableValidator;
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
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.product.ResolvedTrade;

/**
 * Provides the definition of how to calibrate a group of curves.
 * <p>
 * A curve group contains one or more entries, each of which contains the definition of a curve
 * and a set of currencies and indices specifying how the curve is to be used.
 * The currencies are used to specify that the curve is to be used as a discount curve.
 * The indices are used to specify that the curve is to be used as a forward curve.
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
   * Definitions which specify how the curves are calibrated.
   * <p>
   * Curve definitions are required for curves that need to be calibrated. A definition is not necessary if
   * the curve is not built by the Strata curve calibrator.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<NodalCurveDefinition> curveDefinitions;
  /**
   * The flag indicating if the Jacobian matrices should be computed and stored in metadata or not.
   */
  @PropertyDefinition
  private final boolean computeJacobian;
  /**
   * The flag indicating if present value sensitivity to market quotes should be computed and stored in metadata or not.
   */
  @PropertyDefinition
  private final boolean computePvSensitivityToMarketQuote;

  /**
   * Entries for the curves, keyed by the curve name.
   */
  private final ImmutableMap<CurveName, CurveGroupEntry> entriesByName;
  /**
   * Definitions for the curves, keyed by the curve name.
   */
  private final ImmutableMap<CurveName, NodalCurveDefinition> curveDefinitionsByName;

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
   * Returns a curve group definition with the specified name and containing the specified entries. 
   * <p>
   * The Jacobian matrices are computed. The Present Value sensitivity to Market quotes are not computed.
   *
   * @param name  the name of the curve group
   * @param entries  entries describing the curves in the group
   * @param curveDefinitions  definitions which specify how the curves are calibrated
   * @return a curve group definition with the specified name and containing the specified entries
   */
  public static CurveGroupDefinition of(
      CurveGroupName name,
      Collection<CurveGroupEntry> entries,
      Collection<NodalCurveDefinition> curveDefinitions) {

    return new CurveGroupDefinition(name, entries, curveDefinitions, true, false);
  }

  /**
   * Package-private constructor used by the builder.
   *
   * @param name  the name of the curve group
   * @param entries  details of the curves in the group
   * @param curveDefinitions  definitions which specify how the curves are calibrated
   */
  @ImmutableConstructor
  CurveGroupDefinition(
      CurveGroupName name,
      Collection<CurveGroupEntry> entries,
      Collection<NodalCurveDefinition> curveDefinitions,
      boolean computeJacobian,
      boolean computePvSensitivityToMarketQuote) {

    this.name = ArgChecker.notNull(name, "name");
    this.entries = ImmutableList.copyOf(entries);
    this.curveDefinitions = ImmutableList.copyOf(curveDefinitions);
    entriesByName = entries.stream().collect(toImmutableMap(entry -> entry.getCurveName(), entry -> entry));
    curveDefinitionsByName = curveDefinitions.stream().collect(toImmutableMap(def -> def.getName(), def -> def));
    this.computeJacobian = computeJacobian;
    this.computePvSensitivityToMarketQuote = computePvSensitivityToMarketQuote;
    validate();
  }

  @ImmutableValidator
  private void validate() {
    Set<CurveName> missingEntries = Sets.difference(curveDefinitionsByName.keySet(), entriesByName.keySet());
    if (!missingEntries.isEmpty()) {
      throw new IllegalArgumentException("An entry must be provided for every curve definition but the following " +
          "curves have a definition but no entry: " + missingEntries);
    }
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.computePvSensitivityToMarketQuote) {
      builder.computeJacobian = true;
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the entry for the curve with the specified name.
   * <p>
   * If the curve is not found, optional empty is returned.
   *
   * @param curveName  the name of the curve
   * @return the entry for the curve with the specified name
   */
  public Optional<CurveGroupEntry> findEntry(CurveName curveName) {
    return Optional.ofNullable(entriesByName.get(curveName));
  }

  /**
   * Finds the definition for the curve with the specified name.
   * <p>
   * If the curve is not found, optional empty is returned.
   *
   * @param curveName  the name of the curve
   * @return the definition for the curve with the specified name
   */
  public Optional<NodalCurveDefinition> findCurveDefinition(CurveName curveName) {
    return Optional.ofNullable(curveDefinitionsByName.get(curveName));
  }

  //-------------------------------------------------------------------------
  /**
   * Creates the curve metadata for each definition.
   * <p>
   * This method returns a list of metadata, one for each curve definition.
   *
   * @param valuationDate  the valuation date
   * @param refData  the reference data
   * @return the metadata
   */
  public ImmutableList<CurveMetadata> metadata(LocalDate valuationDate, ReferenceData refData) {
    return curveDefinitionsByName.values().stream()
        .map(curveDef -> curveDef.metadata(valuationDate, refData))
        .collect(toImmutableList());
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the total number of parameters in the group.
   * <p>
   * This returns the total number of parameters in the group, which equals the number of nodes.
   * The result of {@link #resolvedTrades(LocalDate, MarketData, ReferenceData)}, and
   * {@link #initialGuesses(LocalDate, MarketData)} will be of this size.
   * 
   * @return the number of parameters
   */
  public int getTotalParameterCount() {
    return curveDefinitionsByName.entrySet().stream().mapToInt(entry -> entry.getValue().getParameterCount()).sum();
  }

  /**
   * Creates a list of trades representing the instrument at each node.
   * <p>
   * This uses the observed market data to build the trade that each node represents.
   * The result combines the list of trades from each curve in order.
   * Each trade is created with a quantity of 1.
   *
   * @param valuationDate  the valuation date used when calibrating the curve
   * @param marketData  the market data required to build a trade for the instrument
   * @param refData  the reference data, used to resolve the trades
   * @return the list of all trades
   */
  public ImmutableList<ResolvedTrade> resolvedTrades(
      LocalDate valuationDate,
      MarketData marketData,
      ReferenceData refData) {

    return curveDefinitionsByName.values().stream()
        .flatMap(curveDef -> curveDef.getNodes().stream())
        .map(node -> node.resolvedTrade(valuationDate, 1d, marketData, refData))
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
  public ImmutableList<Double> initialGuesses(LocalDate valuationDate, MarketData marketData) {
    ImmutableList.Builder<Double> result = ImmutableList.builder();
    for (NodalCurveDefinition defn : curveDefinitions) {
      ValueType valueType = defn.getYValueType();
      for (CurveNode node : defn.getNodes()) {
        result.add(node.initialGuess(valuationDate, marketData, valueType));
      }
    }
    return result.build();
  }

  /**
   * Returns a copy of this object containing the specified curve definitions.
   * <p>
   * Curves are ignored if there is no entry in this definition with the same curve name.
   *
   * @param curveDefinitions  curve definitions
   * @return a copy of this object containing the specified curve definitions
   */
  public CurveGroupDefinition withCurveDefinitions(List<NodalCurveDefinition> curveDefinitions) {
    Set<CurveName> curveNames = entries.stream().map(entry -> entry.getCurveName()).collect(toSet());
    List<NodalCurveDefinition> filteredDefinitions =
        curveDefinitions.stream().filter(def -> curveNames.contains(def.getName())).collect(toImmutableList());
    return new CurveGroupDefinition(name, entries, filteredDefinitions, computeJacobian, computePvSensitivityToMarketQuote);
  }

  /**
   * Returns a copy of this definition with a different name.
   *
   * @param name  the name of the new curve group definition
   * @return a copy of this curve group definition with a different name
   */
  public CurveGroupDefinition withName(CurveGroupName name) {
    return new CurveGroupDefinition(name, entries, curveDefinitions, computeJacobian, computePvSensitivityToMarketQuote);
  }

  public CurveGroupDefinitionBuilder toBuilder() {
    return new CurveGroupDefinitionBuilder(
        name,
        entriesByName,
        curveDefinitionsByName,
        computeJacobian,
        computePvSensitivityToMarketQuote);
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
  /**
   * Gets definitions which specify how the curves are calibrated.
   * <p>
   * Curve definitions are required for curves that need to be calibrated. A definition is not necessary if
   * the curve is not built by the Strata curve calibrator.
   * @return the value of the property, not null
   */
  public ImmutableList<NodalCurveDefinition> getCurveDefinitions() {
    return curveDefinitions;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the flag indicating if the Jacobian matrices should be computed and stored in metadata or not.
   * @return the value of the property
   */
  public boolean isComputeJacobian() {
    return computeJacobian;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the flag indicating if present value sensitivity to market quotes should be computed and stored in metadata or not.
   * @return the value of the property
   */
  public boolean isComputePvSensitivityToMarketQuote() {
    return computePvSensitivityToMarketQuote;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CurveGroupDefinition other = (CurveGroupDefinition) obj;
      return JodaBeanUtils.equal(name, other.name) &&
          JodaBeanUtils.equal(entries, other.entries) &&
          JodaBeanUtils.equal(curveDefinitions, other.curveDefinitions) &&
          (computeJacobian == other.computeJacobian) &&
          (computePvSensitivityToMarketQuote == other.computePvSensitivityToMarketQuote);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(name);
    hash = hash * 31 + JodaBeanUtils.hashCode(entries);
    hash = hash * 31 + JodaBeanUtils.hashCode(curveDefinitions);
    hash = hash * 31 + JodaBeanUtils.hashCode(computeJacobian);
    hash = hash * 31 + JodaBeanUtils.hashCode(computePvSensitivityToMarketQuote);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("CurveGroupDefinition{");
    buf.append("name").append('=').append(name).append(',').append(' ');
    buf.append("entries").append('=').append(entries).append(',').append(' ');
    buf.append("curveDefinitions").append('=').append(curveDefinitions).append(',').append(' ');
    buf.append("computeJacobian").append('=').append(computeJacobian).append(',').append(' ');
    buf.append("computePvSensitivityToMarketQuote").append('=').append(JodaBeanUtils.toString(computePvSensitivityToMarketQuote));
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
     * The meta-property for the {@code curveDefinitions} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<NodalCurveDefinition>> curveDefinitions = DirectMetaProperty.ofImmutable(
        this, "curveDefinitions", CurveGroupDefinition.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code computeJacobian} property.
     */
    private final MetaProperty<Boolean> computeJacobian = DirectMetaProperty.ofImmutable(
        this, "computeJacobian", CurveGroupDefinition.class, Boolean.TYPE);
    /**
     * The meta-property for the {@code computePvSensitivityToMarketQuote} property.
     */
    private final MetaProperty<Boolean> computePvSensitivityToMarketQuote = DirectMetaProperty.ofImmutable(
        this, "computePvSensitivityToMarketQuote", CurveGroupDefinition.class, Boolean.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "name",
        "entries",
        "curveDefinitions",
        "computeJacobian",
        "computePvSensitivityToMarketQuote");

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
        case -336166639:  // curveDefinitions
          return curveDefinitions;
        case -1730091410:  // computeJacobian
          return computeJacobian;
        case -2061625469:  // computePvSensitivityToMarketQuote
          return computePvSensitivityToMarketQuote;
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

    /**
     * The meta-property for the {@code curveDefinitions} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<NodalCurveDefinition>> curveDefinitions() {
      return curveDefinitions;
    }

    /**
     * The meta-property for the {@code computeJacobian} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Boolean> computeJacobian() {
      return computeJacobian;
    }

    /**
     * The meta-property for the {@code computePvSensitivityToMarketQuote} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Boolean> computePvSensitivityToMarketQuote() {
      return computePvSensitivityToMarketQuote;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return ((CurveGroupDefinition) bean).getName();
        case -1591573360:  // entries
          return ((CurveGroupDefinition) bean).getEntries();
        case -336166639:  // curveDefinitions
          return ((CurveGroupDefinition) bean).getCurveDefinitions();
        case -1730091410:  // computeJacobian
          return ((CurveGroupDefinition) bean).isComputeJacobian();
        case -2061625469:  // computePvSensitivityToMarketQuote
          return ((CurveGroupDefinition) bean).isComputePvSensitivityToMarketQuote();
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
    private List<NodalCurveDefinition> curveDefinitions = ImmutableList.of();
    private boolean computeJacobian;
    private boolean computePvSensitivityToMarketQuote;

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
        case -336166639:  // curveDefinitions
          return curveDefinitions;
        case -1730091410:  // computeJacobian
          return computeJacobian;
        case -2061625469:  // computePvSensitivityToMarketQuote
          return computePvSensitivityToMarketQuote;
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
        case -336166639:  // curveDefinitions
          this.curveDefinitions = (List<NodalCurveDefinition>) newValue;
          break;
        case -1730091410:  // computeJacobian
          this.computeJacobian = (Boolean) newValue;
          break;
        case -2061625469:  // computePvSensitivityToMarketQuote
          this.computePvSensitivityToMarketQuote = (Boolean) newValue;
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
      preBuild(this);
      return new CurveGroupDefinition(
          name,
          entries,
          curveDefinitions,
          computeJacobian,
          computePvSensitivityToMarketQuote);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("CurveGroupDefinition.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("entries").append('=').append(JodaBeanUtils.toString(entries)).append(',').append(' ');
      buf.append("curveDefinitions").append('=').append(JodaBeanUtils.toString(curveDefinitions)).append(',').append(' ');
      buf.append("computeJacobian").append('=').append(JodaBeanUtils.toString(computeJacobian)).append(',').append(' ');
      buf.append("computePvSensitivityToMarketQuote").append('=').append(JodaBeanUtils.toString(computePvSensitivityToMarketQuote));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
