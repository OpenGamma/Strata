package com.opengamma.sesame.component;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.configureView;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.nonPortfolioOutput;
import static com.opengamma.sesame.config.ConfigBuilder.output;
import static com.opengamma.sesame.interestrate.InterestRateMockSources.createMarketDataFnFactory;
import static com.opengamma.sesame.interestrate.InterestRateMockSources.generateBaseComponents;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.component.ComponentFactory;
import com.opengamma.component.ComponentLogger;
import com.opengamma.component.ComponentRepository;
import com.opengamma.component.factory.EmbeddedJettyComponentFactory;
import com.opengamma.core.link.ConfigLink;
import com.opengamma.engine.marketdata.spec.FixedHistoricalMarketDataSpecification;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.sesame.DefaultDiscountingMulticurveBundleFn;
import com.opengamma.sesame.DefaultHistoricalTimeSeriesFn;
import com.opengamma.sesame.OutputNames;
import com.opengamma.sesame.RootFinderConfiguration;
import com.opengamma.sesame.config.ViewConfig;
import com.opengamma.sesame.engine.ResultItem;
import com.opengamma.sesame.engine.Results;
import com.opengamma.sesame.engine.ViewFactory;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.tuple.Pair;

import net.sf.ehcache.CacheManager;

/**
 * Tests that remoting to the new engine works. Starts up an engine on a
 * separate thread in the setup method and then makes requests to it via
 * REST. This test should run fast as all component parts are local and is
 * therefore classified as a UNIT test. However, in many respects it is
 * closer to an INTEGRATION test and therefore may need to be reclassified.
 */
@Test(groups = TestGroup.UNIT)
public class RemotingTest {

  public static final String CLASSIFIER = "test";
  private ComponentRepository _componentRepository;

  // test graph build and execution
  @Test
  public void testGraphBuild() {

    CurveConstructionConfiguration curveConstructionConfiguration =
        ConfigLink.of("USD_ON-OIS_LIBOR3M-FRAIRS_1U", CurveConstructionConfiguration.class).resolve();

    final String curveBundleOutputName = "Curve Bundle";
    ViewConfig viewConfig =
        configureView("Curve Bundle only",
                      nonPortfolioOutput(curveBundleOutputName,
                                         output(OutputNames.DISCOUNTING_MULTICURVE_BUNDLE,
                                                config(
                                                    arguments(
                                                        function(
                                                            RootFinderConfiguration.class,
                                                            argument("rootFinderAbsoluteTolerance", 1e-9),
                                                            argument("rootFinderRelativeTolerance", 1e-9),
                                                            argument("rootFinderMaxIterations", 1000)),
                                                        function(
                                                            DefaultHistoricalTimeSeriesFn.class,
                                                            argument("resolutionKey", "DEFAULT_TSS"),
                                                            argument("htsRetrievalPeriod",
                                                                     RetrievalPeriod.of((Period.ofYears(1))))),
                                                        function(
                                                            DefaultDiscountingMulticurveBundleFn.class,
                                                            argument("impliedCurveNames", StringSet.of()),
                                                            argument("curveConfig",
                                                                     curveConstructionConfiguration)))))));

    // Send the config to the server, along with version
    // correction, MD requirements, valuation date and
    // cycle specifics (once/multiple/infinite)
    // Proxy options?

    FunctionServer functionServer = new RemoteFunctionServer(URI.create("http://localhost:8080/jax"));
    FunctionServerRequest.Builder builder = FunctionServerRequest.builder()
        .viewConfig(viewConfig)
            //.withVersionCorrection(...)
            //.withSecurities(...)
        .valuationTime(ZonedDateTime.now())
        .marketDataSpec(new FixedHistoricalMarketDataSpecification(LocalDate.now().minusDays(2)));

    Results results = functionServer.executeOnce(builder.build());
    System.out.println(results);
    assertThat(results, is(not(nullValue())));

    final ResultItem resultItem = results.get(curveBundleOutputName);
    assertThat(resultItem, is(not(nullValue())));
    final Result<?> result = resultItem.getResult();
    assertThat(result, is(not(nullValue())));
    assertThat(result.isValueAvailable(), is(true));

    Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle> pair = (Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>) result.getValue();
    assertThat(pair.getFirst(), is(not(nullValue())));
    assertThat(pair.getSecond(), is(not(nullValue())));

  }

  // test execution with streaming results

  @BeforeClass
  public void setUp() throws Exception {

    System.out.println("Starting setup");
    _componentRepository = new ComponentRepository(new ComponentLogger.Console(1));

    Map<Class<?>, Object> componentMap = generateBaseComponents();
    LinkedHashMap<String, String> properties = addComponentsToRepo(componentMap);

    //  initialise engine
    ViewFactoryComponentFactory engineComponentFactory = new ViewFactoryComponentFactory();
    engineComponentFactory.setClassifier(CLASSIFIER);
    engineComponentFactory.setCacheManager(CacheManager.create());
    register(engineComponentFactory, _componentRepository, properties);

    registerFunctionServerComponentFactory();

    // todo - keeping the below as we should port temple tests to be like this one
/*


    // initialise pricer
    NewEngineFXForwardPricingManagerComponentFactory pricingManagerComponentFactory =
        new NewEngineFXForwardPricingManagerComponentFactory();
    pricingManagerComponentFactory.setEngine(_componentRepository.getInstance(Engine.class, "main"));
    pricingManagerComponentFactory.setAvailableImplementations(_componentRepository.getInstance(AvailableImplementations.class,
                                                                                                "main"));
    pricingManagerComponentFactory.setAvailableOutputs(_componentRepository.getInstance(AvailableOutputs.class, "main"));
    register(pricingManagerComponentFactory, _componentRepository);*/

    // initialize server


    // initialise Jetty server
    System.out.println("Creating Jetty server");
    EmbeddedJettyComponentFactory jettyComponentFactory = new EmbeddedJettyComponentFactory();

    // TODO - can we supply the config required directly rather than a file?
    Resource resource = new ClassPathResource("web-engine");
    jettyComponentFactory.setResourceBase(resource);
    jettyComponentFactory.setLoginConfig(null);
    register(jettyComponentFactory, _componentRepository);
    _componentRepository.start();
  }

  private void registerFunctionServerComponentFactory() throws Exception {

    FunctionServerComponentFactory serverComponentFactory = new FunctionServerComponentFactory();
    serverComponentFactory.setClassifier(CLASSIFIER);
    serverComponentFactory.setViewFactory(_componentRepository.getInstance(ViewFactory.class, CLASSIFIER));
    serverComponentFactory.setMarketDataFnFactory(createMarketDataFnFactory());

    register(serverComponentFactory, _componentRepository);
  }

  @SuppressWarnings("unchecked")
  private LinkedHashMap<String, String> addComponentsToRepo(Map<Class<?>, Object> componentMap) {

    LinkedHashMap<String, String> props = new LinkedHashMap<>();

    for (Map.Entry<Class<?>, Object> entry : componentMap.entrySet()) {
      final Class<?> clss = entry.getKey();
      _componentRepository.registerComponent((Class<Object>) clss, CLASSIFIER, entry.getValue());
      props.put(clss.getSimpleName(), clss.getSimpleName() + "::" + CLASSIFIER);
    }
    return props;
  }

  @AfterClass
  public void tearDown() {
    System.out.println("Shutting down components");
    _componentRepository.stop();
  }

  private void register(ComponentFactory componentFactory,
                        ComponentRepository repo) throws Exception {
    register(componentFactory, repo, new LinkedHashMap<String, String>());
  }

  private void register(ComponentFactory componentFactory,
                        ComponentRepository repo, LinkedHashMap<String, String> configuration) throws Exception {
    componentFactory.init(repo, configuration);
  }

}
