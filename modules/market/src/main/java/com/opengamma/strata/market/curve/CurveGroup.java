/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalInt;
import java.util.Set;

import org.joda.beans.Bean;
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Simple wrapper class holding the results of a multicurve calibration.
 */
@BeanDefinition
public final class CurveGroup implements ImmutableBean {

  /** The calibrated multicurves. */
  @PropertyDefinition(validate = "notNull")
  private final MulticurveProviderDiscount multicurveProvider;

  /** The curve building blocks used to calibrate the curves. */
  @PropertyDefinition(validate = "notNull")
  private final CurveBuildingBlockBundle curveBuildingBlockBundle;

  /**
   * Curve node index maps keyed by curve name. This isn't guaranteed to contain an entry for every curve.
   * The values are maps of curve node ID to curve node index.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<String, Map<CurveNodeId, Integer>> curveNodeIndices;

  /**
   * @param multicurveProvider  the calibrated multicurve
   * @param curveBuildingBlockBundle  data about the multicurve
   */
  public CurveGroup(
      MulticurveProviderDiscount multicurveProvider,
      CurveBuildingBlockBundle curveBuildingBlockBundle) {

    this(multicurveProvider, ImmutableMap.<String, List<? extends CurveNodeId>>of(), curveBuildingBlockBundle);
  }

  /**
   * Creates a new instance which includes the IDs of the nodes in the curves.
   * <p>
   * The first element in the list is the ID of the first node, the second element is the ID of the second node
   * and so on. There can be fewer IDs in the list than nodes in the curve.
   * <p>
   * Node IDs are used by the scenario framework to select curve nodes when applying perturbations.
   * If node IDs aren't provided for a curve it isn't possible to apply scenario perturbations that apply
   * to individual points on the curve.
   *
   * @param multicurveProvider  the multicurve
   * @param curveNodeIds   objects identifying the nodes in each curve, keyed by curve name. Nodes IDs are optional
   * @param curveBuildingBlockBundle   data about the multicurve
   */
  public CurveGroup(
      MulticurveProviderDiscount multicurveProvider,
      Map<String, List<? extends CurveNodeId>> curveNodeIds,
      CurveBuildingBlockBundle curveBuildingBlockBundle) {

    ArgChecker.notNull(multicurveProvider, "multicurveProvider");
    ArgChecker.notNull(curveBuildingBlockBundle, "curveBuildingBlockBundle");
    ArgChecker.notNull(curveNodeIds, "curveNodeIds");

    this.multicurveProvider = multicurveProvider;
    this.curveBuildingBlockBundle = curveBuildingBlockBundle;

    ImmutableMap.Builder<String, Map<CurveNodeId, Integer>> curvesBuilder = ImmutableMap.builder();

    for (Map.Entry<String, List<? extends CurveNodeId>> entry : curveNodeIds.entrySet()) {
      ImmutableMap.Builder<CurveNodeId, Integer> indexBuilder = ImmutableMap.builder();
      List<? extends CurveNodeId> ids = entry.getValue();
      ImmutableSet<? extends CurveNodeId> idSet = ImmutableSet.copyOf(ids);
      String curveName = entry.getKey();

      // If we silently convert to a set and there are duplicate IDs it could have unexpected results.
      // The scenario perturbations could be applied to the wrong nodes.
      if (ids.size() != idSet.size()) {
        throw new IllegalArgumentException("Curve node IDs for curve " + curveName + " contains duplicates " + ids);
      }
      for (int i = 0; i < ids.size(); i++) {
        indexBuilder.put(ids.get(i), i);
      }
      curvesBuilder.put(curveName, indexBuilder.build());
    }
    curveNodeIndices = curvesBuilder.build();
  }

  /**
   * Returns the index of a node in a named curve or an empty optional if the curve name or node ID isn't known.
   * <p>
   * The node ID must correspond to one of the IDs in the {@code curveNodeIds} constructor arguments.
   *
   * @param curveName  the name of a curve in the curve group
   * @param nodeId  the ID of a curve node
   * @return the index of the curve node with the specified ID, empty if there is no curve in the group
   *   with the specified name or there is no node in the named curve with the specified ID
   */
  public OptionalInt curveNodeIndex(String curveName, CurveNodeId nodeId) {
    Map<CurveNodeId, Integer> indices = curveNodeIndices.get(curveName);
    return indices == null ?
        OptionalInt.empty() :
        OptionalInt.of(indices.get(nodeId));
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CurveGroup}.
   * @return the meta-bean, not null
   */
  public static CurveGroup.Meta meta() {
    return CurveGroup.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CurveGroup.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static CurveGroup.Builder builder() {
    return new CurveGroup.Builder();
  }

  private CurveGroup(
      MulticurveProviderDiscount multicurveProvider,
      CurveBuildingBlockBundle curveBuildingBlockBundle,
      Map<String, Map<CurveNodeId, Integer>> curveNodeIndices) {
    JodaBeanUtils.notNull(multicurveProvider, "multicurveProvider");
    JodaBeanUtils.notNull(curveBuildingBlockBundle, "curveBuildingBlockBundle");
    JodaBeanUtils.notNull(curveNodeIndices, "curveNodeIndices");
    this.multicurveProvider = multicurveProvider;
    this.curveBuildingBlockBundle = curveBuildingBlockBundle;
    this.curveNodeIndices = ImmutableMap.copyOf(curveNodeIndices);
  }

  @Override
  public CurveGroup.Meta metaBean() {
    return CurveGroup.Meta.INSTANCE;
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
   * Gets the calibrated multicurves.
   * @return the value of the property, not null
   */
  public MulticurveProviderDiscount getMulticurveProvider() {
    return multicurveProvider;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the curve building blocks used to calibrate the curves.
   * @return the value of the property, not null
   */
  public CurveBuildingBlockBundle getCurveBuildingBlockBundle() {
    return curveBuildingBlockBundle;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets curve node index maps keyed by curve name. This isn't guaranteed to contain an entry for every curve.
   * The values are maps of curve node ID to curve node index.
   * @return the value of the property, not null
   */
  public ImmutableMap<String, Map<CurveNodeId, Integer>> getCurveNodeIndices() {
    return curveNodeIndices;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CurveGroup other = (CurveGroup) obj;
      return JodaBeanUtils.equal(getMulticurveProvider(), other.getMulticurveProvider()) &&
          JodaBeanUtils.equal(getCurveBuildingBlockBundle(), other.getCurveBuildingBlockBundle()) &&
          JodaBeanUtils.equal(getCurveNodeIndices(), other.getCurveNodeIndices());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getMulticurveProvider());
    hash = hash * 31 + JodaBeanUtils.hashCode(getCurveBuildingBlockBundle());
    hash = hash * 31 + JodaBeanUtils.hashCode(getCurveNodeIndices());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("CurveGroup{");
    buf.append("multicurveProvider").append('=').append(getMulticurveProvider()).append(',').append(' ');
    buf.append("curveBuildingBlockBundle").append('=').append(getCurveBuildingBlockBundle()).append(',').append(' ');
    buf.append("curveNodeIndices").append('=').append(JodaBeanUtils.toString(getCurveNodeIndices()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CurveGroup}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code multicurveProvider} property.
     */
    private final MetaProperty<MulticurveProviderDiscount> multicurveProvider = DirectMetaProperty.ofImmutable(
        this, "multicurveProvider", CurveGroup.class, MulticurveProviderDiscount.class);
    /**
     * The meta-property for the {@code curveBuildingBlockBundle} property.
     */
    private final MetaProperty<CurveBuildingBlockBundle> curveBuildingBlockBundle = DirectMetaProperty.ofImmutable(
        this, "curveBuildingBlockBundle", CurveGroup.class, CurveBuildingBlockBundle.class);
    /**
     * The meta-property for the {@code curveNodeIndices} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<String, Map<CurveNodeId, Integer>>> curveNodeIndices = DirectMetaProperty.ofImmutable(
        this, "curveNodeIndices", CurveGroup.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "multicurveProvider",
        "curveBuildingBlockBundle",
        "curveNodeIndices");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 87636839:  // multicurveProvider
          return multicurveProvider;
        case 1604389548:  // curveBuildingBlockBundle
          return curveBuildingBlockBundle;
        case -501851434:  // curveNodeIndices
          return curveNodeIndices;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public CurveGroup.Builder builder() {
      return new CurveGroup.Builder();
    }

    @Override
    public Class<? extends CurveGroup> beanType() {
      return CurveGroup.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code multicurveProvider} property.
     * @return the meta-property, not null
     */
    public MetaProperty<MulticurveProviderDiscount> multicurveProvider() {
      return multicurveProvider;
    }

    /**
     * The meta-property for the {@code curveBuildingBlockBundle} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveBuildingBlockBundle> curveBuildingBlockBundle() {
      return curveBuildingBlockBundle;
    }

    /**
     * The meta-property for the {@code curveNodeIndices} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<String, Map<CurveNodeId, Integer>>> curveNodeIndices() {
      return curveNodeIndices;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 87636839:  // multicurveProvider
          return ((CurveGroup) bean).getMulticurveProvider();
        case 1604389548:  // curveBuildingBlockBundle
          return ((CurveGroup) bean).getCurveBuildingBlockBundle();
        case -501851434:  // curveNodeIndices
          return ((CurveGroup) bean).getCurveNodeIndices();
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
   * The bean-builder for {@code CurveGroup}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<CurveGroup> {

    private MulticurveProviderDiscount multicurveProvider;
    private CurveBuildingBlockBundle curveBuildingBlockBundle;
    private Map<String, Map<CurveNodeId, Integer>> curveNodeIndices = ImmutableMap.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(CurveGroup beanToCopy) {
      this.multicurveProvider = beanToCopy.getMulticurveProvider();
      this.curveBuildingBlockBundle = beanToCopy.getCurveBuildingBlockBundle();
      this.curveNodeIndices = beanToCopy.getCurveNodeIndices();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 87636839:  // multicurveProvider
          return multicurveProvider;
        case 1604389548:  // curveBuildingBlockBundle
          return curveBuildingBlockBundle;
        case -501851434:  // curveNodeIndices
          return curveNodeIndices;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 87636839:  // multicurveProvider
          this.multicurveProvider = (MulticurveProviderDiscount) newValue;
          break;
        case 1604389548:  // curveBuildingBlockBundle
          this.curveBuildingBlockBundle = (CurveBuildingBlockBundle) newValue;
          break;
        case -501851434:  // curveNodeIndices
          this.curveNodeIndices = (Map<String, Map<CurveNodeId, Integer>>) newValue;
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
    public CurveGroup build() {
      return new CurveGroup(
          multicurveProvider,
          curveBuildingBlockBundle,
          curveNodeIndices);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code multicurveProvider} property in the builder.
     * @param multicurveProvider  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder multicurveProvider(MulticurveProviderDiscount multicurveProvider) {
      JodaBeanUtils.notNull(multicurveProvider, "multicurveProvider");
      this.multicurveProvider = multicurveProvider;
      return this;
    }

    /**
     * Sets the {@code curveBuildingBlockBundle} property in the builder.
     * @param curveBuildingBlockBundle  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder curveBuildingBlockBundle(CurveBuildingBlockBundle curveBuildingBlockBundle) {
      JodaBeanUtils.notNull(curveBuildingBlockBundle, "curveBuildingBlockBundle");
      this.curveBuildingBlockBundle = curveBuildingBlockBundle;
      return this;
    }

    /**
     * Sets the {@code curveNodeIndices} property in the builder.
     * @param curveNodeIndices  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder curveNodeIndices(Map<String, Map<CurveNodeId, Integer>> curveNodeIndices) {
      JodaBeanUtils.notNull(curveNodeIndices, "curveNodeIndices");
      this.curveNodeIndices = curveNodeIndices;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("CurveGroup.Builder{");
      buf.append("multicurveProvider").append('=').append(JodaBeanUtils.toString(multicurveProvider)).append(',').append(' ');
      buf.append("curveBuildingBlockBundle").append('=').append(JodaBeanUtils.toString(curveBuildingBlockBundle)).append(',').append(' ');
      buf.append("curveNodeIndices").append('=').append(JodaBeanUtils.toString(curveNodeIndices));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}

