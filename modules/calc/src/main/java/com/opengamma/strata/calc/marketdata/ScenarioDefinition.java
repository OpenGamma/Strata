/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.IntStream;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
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
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.MapStream;

/**
 * A scenario definition defines how to create multiple sets of market data for running calculations over
 * a set of scenarios. The scenario data is created by applying perturbations to a set of base market data.
 * A different set of perturbations is used for each scenario.
 * <p>
 * Each scenario definition contains market data filters and perturbations. Filters
 * are used to choose items of market data that are shocked in the scenario, and the perturbations
 * define those shocks.
 * <p>
 * Perturbations are applied in the order they are defined in scenario. An item of market data
 * can only be perturbed once, so if multiple mappings apply to it, only the first will be used.
 */
@BeanDefinition
public final class ScenarioDefinition implements ImmutableBean {

  /** The market data filters and perturbations that define the scenarios. */
  @PropertyDefinition(validate = "notNull", builderType = "List<? extends PerturbationMapping<?>>")
  private final ImmutableList<PerturbationMapping<?>> mappings;

  /** The names of the scenarios. */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<String> scenarioNames;

  /** An empty scenario definition. */
  private static final ScenarioDefinition EMPTY = ScenarioDefinition.builder().build();

  /**
   * Returns an empty scenario definition.
   *
   * @return an empty scenario definition
   */
  public static ScenarioDefinition empty() {
    return EMPTY;
  }

  /**
   * Returns a scenario definition containing the perturbations in {@code mappings}.
   * <p>
   * Each mapping must contain the same number of perturbations. The definition will contain the
   * same number of scenarios as the number of perturbations in each mapping.
   * <p>
   * The first scenario contains the first perturbation from each mapping, the second scenario contains
   * the second perturbation from each mapping, and so on.
   * <p>
   * Given three mappings, A, B and C, each containing two perturbations, 1 and 2, there will be two
   * scenarios generated:
   * <pre>
   * |            |  A   |  B   |  C   |
   * |------------|------|------|------|
   * | Scenario 1 | A[1] | B[1] | C[1] |
   * | Scenario 2 | A[2] | B[2] | C[2] |
   * </pre>
   * For example, consider the following perturbation mappings:
   * <ul>
   *   <li>Filter: USD Curves, Shocks: [-10bp, 0, +10bp]</li>
   *   <li>Filter: EUR/USD Rate, Shocks: [+5%, 0, -5%]</li>
   * </ul>
   * The scenario definition would contain the following three scenarios:
   * <pre>
   * |            | USD Curves | EUR/USD Rate |
   * |------------|------------|--------------|
   * | Scenario 1 |     -10bp  |     +5%      |
   * | Scenario 2 |       0    |      0       |
   * | Scenario 3 |     +10bp  |     -5%      |
   * </pre>
   *
   * @param mapping  the filters and perturbations that define the scenario. Each mapping must contain the same
   *   number of perturbations
   * @return a scenario definition containing the perturbations in the mappings
   */
  public static ScenarioDefinition ofMappings(List<? extends PerturbationMapping<?>> mapping) {
    ArgChecker.notEmpty(mapping, "mappings");

    int numScenarios = countScenarios(mapping, false);

    for (int i = 1; i < mapping.size(); i++) {
      if (mapping.get(i).getScenarioCount() != numScenarios) {
        throw new IllegalArgumentException(
            "All mappings must have the same number of perturbations. First mapping" +
                " has " + numScenarios + " perturbations, mapping " + i + " has " +
                mapping.get(i).getScenarioCount());
      }
    }
    return new ScenarioDefinition(mapping, generateNames(numScenarios));
  }

  /**
   * Returns a scenario definition containing the perturbations in {@code mappings}.
   * <p>
   * Each mapping must contain the same number of perturbations. The definition will contain the
   * same number of scenarios as the number of perturbations in each mapping.
   * <p>
   * The first scenario contains the first perturbation from each mapping, the second scenario contains
   * the second perturbation from each mapping, and so on.
   * <p>
   * Given three mappings, A, B and C, each containing two perturbations, 1 and 2, there will be two
   * scenarios generated:
   * <pre>
   * |            |  A   |  B   |  C   |
   * |------------|------|------|------|
   * | Scenario 1 | A[1] | B[1] | C[1] |
   * | Scenario 2 | A[2] | B[2] | C[2] |
   * </pre>
   * For example, consider the following perturbation mappings:
   * <ul>
   *   <li>Filter: USD Curves, Shocks: [-10bp, 0, +10bp]</li>
   *   <li>Filter: EUR/USD Rate, Shocks: [+5%, 0, -5%]</li>
   * </ul>
   * The scenario definition would contain the following three scenarios:
   * <pre>
   * |            | USD Curves | EUR/USD Rate |
   * |------------|------------|--------------|
   * | Scenario 1 |     -10bp  |     +5%      |
   * | Scenario 2 |       0    |      0       |
   * | Scenario 3 |     +10bp  |     -5%      |
   * </pre>
   *
   * @param mappings  the filters and perturbations that define the scenario. Each mapping must contain the same
   *   number of perturbations
   * @return a scenario definition containing the perturbations in the mappings
   */
  public static ScenarioDefinition ofMappings(PerturbationMapping<?>... mappings) {
    return ofMappings(Arrays.asList(mappings));
  }

  /**
   * Returns a scenario definition containing the perturbations in {@code mappings}.
   * <p>
   * Each mapping must contain the same number of perturbations. The definition will contain the
   * same number of scenarios as the number of perturbations in each mapping.
   * <p>
   * The first scenario contains the first perturbation from each mapping, the second scenario contains
   * the second perturbation from each mapping, and so on.
   * <p>
   * The set of scenario names must contain the same number of elements as the mappings.
   * <p>
   * Given three mappings, A, B and C, each containing two perturbations, 1 and 2, there will be two
   * scenarios generated:
   * <pre>
   * |            |  A   |  B   |  C   |
   * |------------|------|------|------|
   * | Scenario 1 | A[1] | B[1] | C[1] |
   * | Scenario 2 | A[2] | B[2] | C[2] |
   * </pre>
   * For example, consider the following perturbation mappings:
   * <ul>
   *   <li>Filter: USD Curves, Shocks: [-10bp, 0, +10bp]</li>
   *   <li>Filter: EUR/USD Rate, Shocks: [+5%, 0, -5%]</li>
   * </ul>
   * The scenario definition would contain the following three scenarios:
   * <pre>
   * |            | USD Curves | EUR/USD Rate |
   * |------------|------------|--------------|
   * | Scenario 1 |     -10bp  |     +5%      |
   * | Scenario 2 |       0    |      0       |
   * | Scenario 3 |     +10bp  |     -5%      |
   *
   * @param mappings  the filters and perturbations that define the scenario. Each mapping must contain the same
   *   number of perturbations
   * @param scenarioNames  the names of the scenarios. This must be the same size as the list of perturbations
   *   in each mapping and the names must be unique
   * @return a scenario definition containing the perturbations in the mappings
   * @throws IllegalArgumentException if there are any duplicate scenario names
   */
  public static ScenarioDefinition ofMappings(
      List<? extends PerturbationMapping<?>> mappings,
      List<String> scenarioNames) {

    ArgChecker.notNull(scenarioNames, "scenarioNames");

    int numScenarios = scenarioNames.size();

    for (int i = 0; i < mappings.size(); i++) {
      if (mappings.get(i).getScenarioCount() != numScenarios) {
        throw new IllegalArgumentException(
            "Each mapping must contain the same number of scenarios as the definition. There are " +
                numScenarios + " scenarios in the definition, mapping " + i + " has " +
                mappings.get(i).getScenarioCount() + " scenarios.");
      }
    }
    return new ScenarioDefinition(mappings, scenarioNames);
  }

  /**
   * Counts the number of scenarios implied by the mappings and the {@code allCombinations} flag.
   *
   * @param mappings  the mappings that make up the scenarios
   * @param allCombinations  whether the scenarios are generated by taking all combinations of perturbations
   *   formed by taking one from each mapping
   * @return the number of scenarios
   */
  private static int countScenarios(List<? extends PerturbationMapping<?>> mappings, boolean allCombinations) {
    ArgChecker.notEmpty(mappings, "mappings");

    if (allCombinations) {
      return mappings.stream()
          .mapToInt(PerturbationMapping::getScenarioCount)
          .reduce(1, (s1, s2) -> s1 * s2);
    } else {
      return mappings.get(0).getScenarioCount();
    }
  }

  /**
   * Returns a list created by repeating the items in the input list multiple times, with each item repeated
   * in groups.
   * <p>
   * For example, given a list [1, 2, 3, 4], total count 12, group size 3 the result is
   * [1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4].
   * <p>
   * This is used when creating scenarios from every possible combination of a set of perturbations.
   *
   * @param inputs  an input list whose elements are repeated in the output
   * @param totalCount  the number of elements in the output list
   * @param groupSize  the number of times each element should be repeated in each group
   * @param <T>  the type of the elements
   * @return a list created by repeating the elements of the input list
   */
  static <T> List<T> repeatItems(List<T> inputs, int totalCount, int groupSize) {
    ImmutableList.Builder<T> builder = ImmutableList.builder();

    for (int i = 0; i < (totalCount / groupSize / inputs.size()); i++) {
      for (T input : inputs) {
        builder.addAll(Collections.nCopies(groupSize, input));
      }
    }
    return builder.build();
  }

  /**
   * Generates simple names for the scenarios of the form 'Scenario 1' etc.
   */
  private static ImmutableList<String> generateNames(int numScenarios) {
    return IntStream.range(1, numScenarios + 1)
        .mapToObj(i -> "Scenario " + i)
        .collect(toImmutableList());
  }

  // validates that there are no duplicate scenario names
  @ImmutableValidator
  private void validate() {
    Map<String, List<String>> nameMap = scenarioNames.stream().collect(groupingBy(name -> name));
    List<String> duplicateNames = MapStream.of(nameMap)
        .filterValues(names -> names.size() > 1)
        .map((name, names) -> name)
        .collect(toImmutableList());

    if (!duplicateNames.isEmpty()) {
      String duplicates = duplicateNames.stream().collect(joining(", "));
      throw new IllegalArgumentException("Scenario names must be unique but duplicates were found: " + duplicates);
    }
  }

  /**
   * Returns the number of scenarios.
   *
   * @return the number of scenarios
   */
  public int getScenarioCount() {
    return scenarioNames.size();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ScenarioDefinition}.
   * @return the meta-bean, not null
   */
  public static ScenarioDefinition.Meta meta() {
    return ScenarioDefinition.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ScenarioDefinition.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ScenarioDefinition.Builder builder() {
    return new ScenarioDefinition.Builder();
  }

  private ScenarioDefinition(
      List<? extends PerturbationMapping<?>> mappings,
      List<String> scenarioNames) {
    JodaBeanUtils.notNull(mappings, "mappings");
    JodaBeanUtils.notNull(scenarioNames, "scenarioNames");
    this.mappings = ImmutableList.copyOf(mappings);
    this.scenarioNames = ImmutableList.copyOf(scenarioNames);
    validate();
  }

  @Override
  public ScenarioDefinition.Meta metaBean() {
    return ScenarioDefinition.Meta.INSTANCE;
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
   * Gets the market data filters and perturbations that define the scenarios.
   * @return the value of the property, not null
   */
  public ImmutableList<PerturbationMapping<?>> getMappings() {
    return mappings;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the names of the scenarios.
   * @return the value of the property, not null
   */
  public ImmutableList<String> getScenarioNames() {
    return scenarioNames;
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
      ScenarioDefinition other = (ScenarioDefinition) obj;
      return JodaBeanUtils.equal(mappings, other.mappings) &&
          JodaBeanUtils.equal(scenarioNames, other.scenarioNames);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(mappings);
    hash = hash * 31 + JodaBeanUtils.hashCode(scenarioNames);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("ScenarioDefinition{");
    buf.append("mappings").append('=').append(mappings).append(',').append(' ');
    buf.append("scenarioNames").append('=').append(JodaBeanUtils.toString(scenarioNames));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ScenarioDefinition}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code mappings} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<PerturbationMapping<?>>> mappings = DirectMetaProperty.ofImmutable(
        this, "mappings", ScenarioDefinition.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code scenarioNames} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<String>> scenarioNames = DirectMetaProperty.ofImmutable(
        this, "scenarioNames", ScenarioDefinition.class, (Class) ImmutableList.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "mappings",
        "scenarioNames");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 194445669:  // mappings
          return mappings;
        case -1193464424:  // scenarioNames
          return scenarioNames;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ScenarioDefinition.Builder builder() {
      return new ScenarioDefinition.Builder();
    }

    @Override
    public Class<? extends ScenarioDefinition> beanType() {
      return ScenarioDefinition.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code mappings} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<PerturbationMapping<?>>> mappings() {
      return mappings;
    }

    /**
     * The meta-property for the {@code scenarioNames} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<String>> scenarioNames() {
      return scenarioNames;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 194445669:  // mappings
          return ((ScenarioDefinition) bean).getMappings();
        case -1193464424:  // scenarioNames
          return ((ScenarioDefinition) bean).getScenarioNames();
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
   * The bean-builder for {@code ScenarioDefinition}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ScenarioDefinition> {

    private List<? extends PerturbationMapping<?>> mappings = ImmutableList.of();
    private List<String> scenarioNames = ImmutableList.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ScenarioDefinition beanToCopy) {
      this.mappings = beanToCopy.getMappings();
      this.scenarioNames = beanToCopy.getScenarioNames();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 194445669:  // mappings
          return mappings;
        case -1193464424:  // scenarioNames
          return scenarioNames;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 194445669:  // mappings
          this.mappings = (List<? extends PerturbationMapping<?>>) newValue;
          break;
        case -1193464424:  // scenarioNames
          this.scenarioNames = (List<String>) newValue;
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
    public ScenarioDefinition build() {
      return new ScenarioDefinition(
          mappings,
          scenarioNames);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the market data filters and perturbations that define the scenarios.
     * @param mappings  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder mappings(List<? extends PerturbationMapping<?>> mappings) {
      JodaBeanUtils.notNull(mappings, "mappings");
      this.mappings = mappings;
      return this;
    }

    /**
     * Sets the {@code mappings} property in the builder
     * from an array of objects.
     * @param mappings  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder mappings(PerturbationMapping<?>... mappings) {
      return mappings(ImmutableList.copyOf(mappings));
    }

    /**
     * Sets the names of the scenarios.
     * @param scenarioNames  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder scenarioNames(List<String> scenarioNames) {
      JodaBeanUtils.notNull(scenarioNames, "scenarioNames");
      this.scenarioNames = scenarioNames;
      return this;
    }

    /**
     * Sets the {@code scenarioNames} property in the builder
     * from an array of objects.
     * @param scenarioNames  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder scenarioNames(String... scenarioNames) {
      return scenarioNames(ImmutableList.copyOf(scenarioNames));
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("ScenarioDefinition.Builder{");
      buf.append("mappings").append('=').append(JodaBeanUtils.toString(mappings)).append(',').append(' ');
      buf.append("scenarioNames").append('=').append(JodaBeanUtils.toString(scenarioNames));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
