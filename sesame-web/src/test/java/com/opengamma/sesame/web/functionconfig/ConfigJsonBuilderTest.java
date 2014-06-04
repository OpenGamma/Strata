package com.opengamma.sesame.web.functionconfig;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.mockito.internal.stubbing.answers.Returns;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.opengamma.core.config.Config;
import com.opengamma.core.config.impl.ConfigItem;
import com.opengamma.core.link.ConfigLink;
import com.opengamma.core.position.Trade;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.financial.security.irs.InterestRateSwapSecurity;
import com.opengamma.master.config.ConfigDocument;
import com.opengamma.master.config.ConfigMaster;
import com.opengamma.master.config.ConfigSearchRequest;
import com.opengamma.master.config.ConfigSearchResult;
import com.opengamma.sesame.EngineTestUtils;
import com.opengamma.sesame.OutputName;
import com.opengamma.sesame.config.EngineUtils;
import com.opengamma.sesame.config.SimpleFunctionArguments;
import com.opengamma.sesame.config.SimpleFunctionModelConfig;
import com.opengamma.sesame.function.AvailableImplementations;
import com.opengamma.sesame.function.AvailableImplementationsImpl;
import com.opengamma.sesame.function.AvailableOutputs;
import com.opengamma.sesame.function.AvailableOutputsImpl;
import com.opengamma.sesame.function.DefaultImplementationProvider;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.sesame.graph.convert.DefaultArgumentConverter;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class ConfigJsonBuilderTest {

  private static final String COLUMN_NAME = "Column Name";
  private static final FunctionMetadata FOO_META = EngineUtils.createMetadata(Fn.class, "foo");

  private final JsonElement _expected;

  public ConfigJsonBuilderTest() throws IOException, JSONException {
    String jsonString = IOUtils.toString(getClass().getResourceAsStream("ConfigJsonBuilderTest.json"));
    _expected = new JsonParser().parse(jsonString);
  }

  private void checkJson(Map<?, ?> json, String expectedJsonName) {
    Gson gson = new Gson();
    JsonElement element = _expected.getAsJsonObject().get(expectedJsonName);
    Map expectedJson = gson.fromJson(element, Map.class);
    EngineTestUtils.assertJsonEquals(expectedJson, json);
  }

  @Test
  public void fnWithMultipleImplsNoImplSelected() throws JSONException {
    AvailableImplementations availableImplementations = new AvailableImplementationsImpl();
    availableImplementations.register(ImplNoArgs.class, ImplWithArgs.class);
    FunctionModel model = FunctionModel.forFunction(FOO_META);
    ConfigJsonBuilder builder = new ConfigJsonBuilder(mock(AvailableOutputs.class),
                                                      availableImplementations,
                                                      mock(ConfigMaster.class),
                                                      new DefaultArgumentConverter());
    Map<String, Object> json = builder.getConfigPageModel(COLUMN_NAME, SimpleFunctionModelConfig.EMPTY, null, null, model);
    checkJson(json, "fnWithMultipleImplsNoImplSelected");
  }

  @Test
  public void fnWithMultipleImplsSelectedImplHasArgs() {
    AvailableImplementations availableImplementations = new AvailableImplementationsImpl();
    availableImplementations.register(ImplNoArgs.class, ImplWithArgs.class);
    SimpleFunctionModelConfig config =
        config(
            implementations(Fn.class, ImplWithArgs.class),
            arguments(
                function(
                    ImplWithArgs.class,
                    argument("arg1", "value1"),
                    argument("arg2", ImmutableList.of(1, 2, 3)))));
    FunctionModel model = FunctionModel.forFunction(FOO_META, config);
    ConfigJsonBuilder builder = new ConfigJsonBuilder(mock(AvailableOutputs.class),
                                                      availableImplementations,
                                                      mock(ConfigMaster.class),
                                                      new DefaultArgumentConverter());
    Map<String, Object> json = builder.getConfigPageModel(COLUMN_NAME, config, null, null, model);
    checkJson(json, "fnWithMultipleImplsSelectedImplHasArgs");
  }

  @Test
  public void fnWithMultipleImplsSelectedImplHasNoArgs() {
    AvailableImplementations availableImplementations = new AvailableImplementationsImpl();
    availableImplementations.register(ImplNoArgs.class, ImplWithArgs.class);
    SimpleFunctionModelConfig config = config(implementations(Fn.class, ImplNoArgs.class));
    FunctionModel model = FunctionModel.forFunction(FOO_META, config);
    ConfigJsonBuilder builder = new ConfigJsonBuilder(mock(AvailableOutputs.class),
                                                      availableImplementations,
                                                      mock(ConfigMaster.class),
                                                      new DefaultArgumentConverter());
    Map<String, Object> json = builder.getConfigPageModel(COLUMN_NAME, config, null, null, model);
    checkJson(json, "fnWithMultipleImplsSelectedImplHasNoArgs");
  }

  @Test
  public void concreteFunctionNoArgs() {
    FunctionMetadata metadata = EngineUtils.createMetadata(ConcreteNoArgs.class, "bar");
    FunctionModel model = FunctionModel.forFunction(metadata);
    ConfigJsonBuilder builder = new ConfigJsonBuilder(mock(AvailableOutputs.class),
                                                      new AvailableImplementationsImpl(),
                                                      mock(ConfigMaster.class),
                                                      new DefaultArgumentConverter());
    Map<String, Object> json = builder.getConfigPageModel(COLUMN_NAME, SimpleFunctionModelConfig.empty(), null, null, model);
    checkJson(json, "concreteFunctionNoArgs");
  }

  @Test
  public void concreteFunctionWithArgs() {
    FunctionMetadata metadata = EngineUtils.createMetadata(ConcreteWithArgs.class, "baz");
    SimpleFunctionModelConfig config =
        config(
            arguments(
                function(
                    ConcreteWithArgs.class,
                    argument("arg1", "value1"),
                    argument("arg2", ImmutableList.of(1, 2, 3)))));
    FunctionModel model = FunctionModel.forFunction(metadata, config);
    ConfigJsonBuilder builder = new ConfigJsonBuilder(mock(AvailableOutputs.class),
                                                      new AvailableImplementationsImpl(),
                                                      mock(ConfigMaster.class),
                                                      new DefaultArgumentConverter());
    Map<String, Object> json = builder.getConfigPageModel(COLUMN_NAME, config, null, null, model);
    checkJson(json, "concreteFunctionWithArgs");
  }

  @Test
  public void missingArgument() {
    AvailableImplementations availableImplementations = new AvailableImplementationsImpl();
    availableImplementations.register(ImplNoArgs.class, ImplWithArgs.class);
    SimpleFunctionModelConfig config = config(implementations(Fn.class, ImplWithArgs.class),
                                        arguments(function(ImplWithArgs.class,
                                                           argument("arg2", ImmutableList.of(1, 2, 3)))));
    FunctionModel model = FunctionModel.forFunction(FOO_META, config);
    ConfigJsonBuilder builder = new ConfigJsonBuilder(mock(AvailableOutputs.class),
                                                      availableImplementations,
                                                      mock(ConfigMaster.class),
                                                      new DefaultArgumentConverter());
    Map<String, Object> json = builder.getConfigPageModel(COLUMN_NAME, config, null, null, model);
    checkJson(json, "missingArgument");
  }

  @SuppressWarnings("unchecked")
  @Test
  public void createConfig() {
    Gson gson = new Gson();
    JsonElement element = _expected.getAsJsonObject().get("createConfig");
    Map<String, Object> map = gson.fromJson(element, Map.class);
    ConfigJsonBuilder builder = new ConfigJsonBuilder(new AvailableOutputsImpl(),
                                                      new AvailableImplementationsImpl(),
                                                      mock(ConfigMaster.class),
                                                      new DefaultArgumentConverter());
    SimpleFunctionModelConfig config = builder.getConfigFromJson(map);
    Map<String, Object> argsMap = ImmutableMap.<String, Object>of("arg1", "value1", "arg2", ImmutableList.of(1, 2, 3));
    SimpleFunctionArguments simpleFunctionArguments = new SimpleFunctionArguments(argsMap);
    Map<Class<?>, SimpleFunctionArguments> fnArgsMap =
        ImmutableMap.<Class<?>, SimpleFunctionArguments>of(ImplWithArgs.class, simpleFunctionArguments);
    SimpleFunctionModelConfig expected =
        new SimpleFunctionModelConfig(ImmutableMap.<Class<?>, Class<?>>of(Fn.class, ImplWithArgs.class), fnArgsMap);
    assertEquals(expected, config);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void createConfigWithConfigArg() {
    Gson gson = new Gson();
    JsonElement element = _expected.getAsJsonObject().get("createConfigWithConfigArg");
    Map<String, Object> map = gson.fromJson(element, Map.class);
    ConfigJsonBuilder builder = new ConfigJsonBuilder(new AvailableOutputsImpl(),
                                                      new AvailableImplementationsImpl(),
                                                      mock(ConfigMaster.class),
                                                      new DefaultArgumentConverter());
    SimpleFunctionModelConfig config = builder.getConfigFromJson(map);
    ConfigLink<ConfigObject> configLink = ConfigLink.resolvable("configName", ConfigObject.class);
    Map<String, Object> argsMap = ImmutableMap.<String, Object>of("configArg", configLink);
    SimpleFunctionArguments simpleFunctionArguments = new SimpleFunctionArguments(argsMap);
    Map<Class<?>, SimpleFunctionArguments> fnArgsMap =
        ImmutableMap.<Class<?>, SimpleFunctionArguments>of(UsesConfig.class, simpleFunctionArguments);
    SimpleFunctionModelConfig expected =
        new SimpleFunctionModelConfig(ImmutableMap.<Class<?>, Class<?>>of(Fn.class, UsesConfig.class), fnArgsMap);
    assertEquals(expected, config);
  }

  @Test
  public void columnJsonNoInputOrOutput() {
    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    AvailableImplementations availableImplementations = new AvailableImplementationsImpl();
    availableOutputs.register(Fn.class, Fn2.class, ConcreteNoArgs.class, ConcreteWithArgs.class);
    availableImplementations.register(ImplNoArgs.class, ImplWithArgs.class);
    ConfigJsonBuilder builder = new ConfigJsonBuilder(availableOutputs,
                                                      availableImplementations,
                                                      mock(ConfigMaster.class),
                                                      new DefaultArgumentConverter());
    Map<String, Object> json = builder.getConfigPageModel(COLUMN_NAME, SimpleFunctionModelConfig.EMPTY, null, null, null);
    checkJson(json, "columnJsonNoInputOrOutput");
  }

  @Test
  public void columnJsonInputNoOutput() {
    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    AvailableImplementations availableImplementations = new AvailableImplementationsImpl();
    availableOutputs.register(Fn.class, Fn2.class, ConcreteNoArgs.class, ConcreteWithArgs.class);
    availableImplementations.register(ImplNoArgs.class, ImplWithArgs.class);
    ConfigJsonBuilder builder = new ConfigJsonBuilder(availableOutputs,
                                                      availableImplementations,
                                                      mock(ConfigMaster.class),
                                                      new DefaultArgumentConverter());
    Map<String, Object> json = builder.getConfigPageModel(COLUMN_NAME, SimpleFunctionModelConfig.EMPTY, Trade.class, null, null);
    checkJson(json, "columnJsonInputNoOutput");
  }

  @Test
  public void columnJsonInputAndOutput() {
    AvailableOutputs availableOutputs = new AvailableOutputsImpl();
    AvailableImplementations availableImplementations = new AvailableImplementationsImpl();
    availableOutputs.register(Fn.class, Fn2.class, ConcreteNoArgs.class, ConcreteWithArgs.class);
    availableImplementations.register(ImplNoArgs.class, ImplWithArgs.class);
    ConfigJsonBuilder builder = new ConfigJsonBuilder(availableOutputs,
                                                      availableImplementations,
                                                      mock(ConfigMaster.class),
                                                      new DefaultArgumentConverter());
    Map<String, Object> json = builder.getConfigPageModel(COLUMN_NAME,
                                                          SimpleFunctionModelConfig.EMPTY,
                                                          Trade.class,
                                                          OutputName.of("Foo2"),
                                                          null);
    checkJson(json, "columnJsonInputAndOutput");
  }

  /**
   * Tests that an array of values is included if the argument type is annotated with @Config and there
   * are values of that type in the config master. if there is no value selected there the selected item
   * isn't included (empty or null?)
   */
  @Test
  public void configArgNoSelection() {
    AvailableImplementations availableImplementations = new AvailableImplementationsImpl();
    availableImplementations.register(UsesConfig.class);
    DefaultImplementationProvider defaultImpl = new DefaultImplementationProvider(availableImplementations);
    SimpleFunctionModelConfig config = new SimpleFunctionModelConfig(defaultImpl.getDefaultImplementations(),
                                                                     ImmutableMap.<Class<?>, SimpleFunctionArguments>of());
    FunctionModel model = FunctionModel.forFunction(FOO_META, config);
    ConfigMaster configMaster = mock(ConfigMaster.class);
    ConfigSearchRequest<?> searchRequest = new ConfigSearchRequest<>();
    searchRequest.setType(ConfigObject.class);
    List<ConfigDocument> configs = Lists.newArrayList(configDocument("foo"), configDocument("bar"), configDocument("baz"));
    when(configMaster.search(searchRequest)).thenAnswer(new Returns(new ConfigSearchResult<>(configs)));
    ConfigJsonBuilder builder = new ConfigJsonBuilder(mock(AvailableOutputs.class),
                                                      availableImplementations,
                                                      configMaster,
                                                      new DefaultArgumentConverter());
    Map<String, Object> functionJson = builder.getConfigPageModel(COLUMN_NAME, config, null, null, model);
    checkJson(functionJson, "configArgNoSelection");
  }

  /**
   * Tests that an array of values is included if the argument type is annotated with @Config and there
   * are values of that type in the config master. If there is a value selected it is included
   * TODO enable this test once the config link API has been changed to include the link target's name
   */
  @Test(enabled = false)
  public void configArgValueSelected() {
    AvailableImplementations availableImplementations = new AvailableImplementationsImpl();
    availableImplementations.register(UsesConfig.class);
    ConfigLink<ConfigObject> configLink = ConfigLink.resolvable("bar", ConfigObject.class);
    SimpleFunctionModelConfig config = config(implementations(Fn.class, UsesConfig.class),
                                        arguments(function(UsesConfig.class, argument("configArg", configLink))));
    FunctionModel model = FunctionModel.forFunction(FOO_META, config);
    ConfigMaster configMaster = mock(ConfigMaster.class);
    ConfigSearchRequest<?> searchRequest = new ConfigSearchRequest<>();
    searchRequest.setType(ConfigObject.class);
    List<ConfigDocument> configs = Lists.newArrayList(configDocument("foo"), configDocument("bar"), configDocument("baz"));
    when(configMaster.search(searchRequest)).thenAnswer(new Returns(new ConfigSearchResult<>(configs)));
    ConfigJsonBuilder builder = new ConfigJsonBuilder(mock(AvailableOutputs.class),
                                                      availableImplementations,
                                                      configMaster,
                                                      new DefaultArgumentConverter());
    Map<String, Object> json = builder.getConfigPageModel(COLUMN_NAME, config, null, null, model);
    checkJson(json, "configArgNoSelection");
  }

  @Test
  public void convertBetweenJsonAndConfig() {
    SimpleFunctionModelConfig config =
        config(
            implementations(
                Fn.class, ImplWithArgs.class,
                Fn2.class, Impl2.class),
            arguments(
                function(
                    ImplWithArgs.class,
                    argument("arg1", "argValue1"),
                    argument("arg2", ImmutableList.of(1, 2, 3))),
                function(
                    ConcreteWithArgs.class,
                    argument("arg1", "argValue3"),
                    argument("arg2", ImmutableList.of(1, 2, 3)))));

    ConfigJsonBuilder builder = new ConfigJsonBuilder(mock(AvailableOutputs.class),
                                                      mock(AvailableImplementations.class),
                                                      mock(ConfigMaster.class),
                                                      new DefaultArgumentConverter());
    Map<String, Object> json = builder.getJsonFromConfig(config);
    checkJson(json, "convertBetweenJsonAndConfig");
    assertEquals(config, builder.getConfigFromJson(json));
  }

  private ConfigDocument configDocument(String name) {
    return new ConfigDocument(ConfigItem.of(new ConfigObject(), name));
  }

  public interface Fn {

    @Output("Foo")
    String foo(Trade trade);
  }

  public static class ImplNoArgs implements Fn {

    @Override
    public String foo(Trade trade) {
      return null;
    }
  }

  public static class ImplWithArgs implements Fn {

    public ImplWithArgs(String arg1, List<Integer> arg2) {
    }

    @Override
    public String foo(Trade trade) {
      return null;
    }
  }

  public static class ConcreteNoArgs {

    @Output("Bar")
    public String bar(EquitySecurity equitySecurity) {
      return null;
    }
  }

  public static class ConcreteWithArgs {

    public ConcreteWithArgs(String arg1, List<Integer> arg2) {
    }

    @Output("Baz")
    public String baz(InterestRateSwapSecurity swapSecurity) {
      return null;
    }
  }

  public interface Fn2 {

    @Output("Foo2")
    String foo(Trade trade);
  }

  public static class Impl2 implements Fn2 {

    @Override
    public String foo(Trade trade) {
      return null;
    }
  }

  @Config
  public static final class ConfigObject {

  }

  public static final class UsesConfig implements Fn {

    public UsesConfig(ConfigObject configArg) {
    }

    @Override
    public String foo(Trade trade) {
      return null;
    }
  }
}
