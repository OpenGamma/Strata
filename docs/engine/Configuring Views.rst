=====================================================
Configuring Views in the OpenGamma Calculation Engine
=====================================================

View Basics
===========
A view is what defines the calculations performed by the OpenGamma calculation engine.
The calculation results produced by the engine can be thought of as a grid. There is one row for each trade
and one column for each calculation.

The configuration of a view defines what is calculated for each column
and specifies how the calculation is performed.

View Configuration
==================
At a high level, view configuration specifies three things:

* What value is calculated for each column (Outputs_)
* Which functions are used to calculate each value (`Function implementations`_)
* The configuration of those functions (`Function arguments`_)

Outputs
-------
The first thing to consider when configuring a column is what value it should contain. It could be a calculated
risk measure, e.g. present value or PV01, an attribute of the trade, e.g. quantity, currency or description, or
it could be any other piece of information the user wishes to see.

Values that can be displayed in columns are known as "outputs". The user chooses the data in a column by specifying
the name of the column's output.

Functions declare what outputs they can calculate (see here [TODO link to function docs] for details).
Functions are registered with the engine so the engine can choose the appropriate functions based on the
output name and the type of the trade.

Function implementations
------------------------
Functions in the OpenGamma engine are defined by interfaces (see here [TODO link to function docs]). If there
is more than one implementation of a function interface registered with the engine the user must specify which
implementation should be used.

If there is only one implementation of a function interface the engine can infer that it should be used
and there is no need to specify anything in the configuration.

Function arguments
------------------
Function classes can declare constructor or method arguments that aren't functions or services that the engine
can provide. The user must specify values for these arguments in the view configuration.

The ViewConfig class
====================
The ``ViewConfig`` class defines all the configuration for a view. It contains:

* The view name
* The default configuration, shared by all columns (an instance of FunctionModelConfig_)
* Configuration for each column (instances of ViewColumn_)

.. _FunctionModelConfig:

The FunctionModelConfig class
-----------------------------
The ``FunctionModelConfig`` specifies the function implementations and function arguments. It can be thought of
as two maps:

* Function interface class to function implementation class
* Function parameter (constructor or method) to value

``FunctionModelConfig`` is the basic building block of view configuration.

.. _ViewColumn:

The ViewColumn class
--------------------
The ``ViewColumn`` class defines the configuration for a single column. It contains:

* The column name
* The default configuration shared by all trade types (an instance of ``ViewOutput``)
* The configuration for each trade type (instances of ``ViewOutput``)

.. _ViewOutput:

The ViewOutput class
--------------------
The ``ViewOutput`` class defines the configuration for a single column and trade type. It contains:

* The output name
* The configuration (an instance of ``FunctionModelConfig``)

The ConfigBuilder class
-----------------------
The ``ConfigBuilder`` class is the primary way to define view configuration. It provides helper methods to
allow ``ViewConfig`` and ``FunctionModelConfig`` instances to be created in a declarative style.

It has a method ``configureView`` which returns ``ViewConfig``. This is the root of the configuration.
It also has a ``column`` method returning a ``ViewColumn`` and a ``config`` method returning ``FunctionModelConfig``.

For the purposes of the examples, assume that all methods of ``ConfigBuilder`` have been statically imported.

Examples
########

The simplest configuration that can produce a usable view is:

.. code:: java

    ViewConfig viewConfig = configureView("Example View", column(OutputNames.PRESENT_VALUE));

This defines a view called "Example View" with a single column named "Present Value". The column contains the
output named "Present Value". This assumes there are functions available to calculate present value for all
trade types used with the view. It also assumes there is only one implementation of each of these functions so the
engine can infer which implementation to use.

Multiple function implementations
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
If there are multiple implementations of a function available the user must specify which one to use.

In this example the default configuration for the view is provided. It specifies that ``DiscountingInterestRateSwapFn``
should be used wherever an instance of ``InterestRateSwapFn`` is required.

.. code:: java

    FunctionModelConfig defaultConfig = config(implementations(InterestRateSwapFn.class, DiscountingInterestRateSwapFn.class));
    ViewConfig viewConfig = configureView("Example View", defaultConfig, column("PV", OutputNames.PRESENT_VALUE));

In this example there are three implementation types specified for three functions.

.. code:: java

    FunctionModelConfig defaultConfig =
        config(
            implementations(
                InterestRateSwapFn.class, DiscountingInterestRateSwapFn.class,
                InterestRateSwapCalculator.class, DiscountingInterestRateSwapCalculator.class,
                InstrumentExposuresProvider.class, ConfigDBInstrumentExposuresProvider.class));

    ViewConfig viewConfig = configureView("Example View", defaultConfig, column("PV", OutputNames.PRESENT_VALUE));

Function constructor / method arguments
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
When the OpenGamma engine creates function instances it must provide arguments to the function's constructor.
These can be one of three types:

* Other functions created by the engine
* Service objects provided by the engine
* Arguments provided by the user

Users arguments are provided in the configuration:

.. code:: java

    FunctionModelConfig defaultConfig =
        config(
            arguments(
                function(
                    FixedHistoricalMarketDataFactory.class,
                    argument("currencyMatrixConfigName", "BloombergLiveData"),
                    argument("dataSource", "BLOOMBERG")),
                function(
                    DefaultHistoricalMarketDataFn.class,
                    argument("currencyMatrix", currencyMatrix),
                    argument("dataSource", "BLOOMBERG"))));

    ViewConfig viewConfig = configureView(...));

These arguments are also used if an output function invoked by the engine has arguments other than the environment
and the trade.

Multiple columns
^^^^^^^^^^^^^^^^
In this example the view has multiple columns.

.. code:: java

    FunctionModelConfig defaultConfig = config(...);

    ViewConfig viewConfig =
        configureView(
            "Example View", defaultConfig,
            column("PV", OutputNames.PRESENT_VALUE),
            column("PV01", OutputNames.PV01),
            column("Yield Curve Node Sensitivities", OutputNames.YIELD_CURVE_NODE_SENSITIVITIES));

Overriding default configuration for a column
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
All examples so far have shown the configuration defined at the level of the view and shared by all columns. If
a column requires different configuration from the default view config, it can define its own.

Values specified in the column configuration override those in the default view configuration.
Any values not specified in the column configuration are inherited from the view configuration.

.. code:: java

    ViewConfig viewConfig =
        configureView(
            "Example View",
            // default config shared by all columns
            config(
                implementations(
                    DiscountingMulticurveBundleFn.class, DefaultDiscountingMulticurveBundleFn.class)),
            column(
                "PV", OutputNames.PRESENT_VALUE,
                // column specific config that uses a different function implementation
                config(
                    implementations(
                        DiscountingMulticurveBundleFn.class, InterpolatedMulticurveBundleFn.class))),
            // these columns use the function implementation defined in the view config
            column("PV01", OutputNames.PV01),
            column("Yield Curve Node Sensitivities", OutputNames.YIELD_CURVE_NODE_SENSITIVITIES));

This example also shows defining the view configuration inline rather than separately as in previous examples.

Different configuration within a column
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Typically different functions will be used to calculate an output for different trade types within the same column.
For example, the present value column might use ``InterestRateSwapFn.calculatePV()`` for a swap and
``SwaptionFn.calculatePV()`` for a swaption.

If these functions need different configuration, it can be specified at the level of the ViewOutput_. For example,
this defines a PV column where the swap and swaption calculations use a different root finder configuration
but inherit the rest of their configuration from the view.

.. code:: java

    FunctionModelConfig defaultConfig = config(...);

    ViewConfig viewConfig =
        configureView(
            "Example View", defaultConfig,
            column(
                "PV", OutputNames.PRESENT_VALUE,
                output(
                    InterestRateSwapSecurity.class,
                    config(
                        arguments(
                            function(
                                RootFinderConfiguration.class,
                                argument("rootFinderAbsoluteTolerance", 1e-9),
                                argument("rootFinderRelativeTolerance", 1e-9),
                                argument("rootFinderMaxIterations", 1000))))),
                output(
                    SwaptionSecurity.class,
                    config(
                        arguments(
                            function(
                                RootFinderConfiguration.class,
                                argument("rootFinderAbsoluteTolerance", 1e-8),
                                argument("rootFinderRelativeTolerance", 1e-8),
                                argument("rootFinderMaxIterations", 2000)))))));

Different output names within a column
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
It is possible to show different outputs in the same column depending on the trade type. For example this
defines a PV column where the output ``OutputNames.PRESENT_VALUE`` is used for swaps and
``OutputNames.FX_PRESENT_VALUE`` is used for FX forwards:

.. code:: java

    FunctionModelConfig defaultConfig =

    ViewConfig viewConfig =
        configureView(
            "Example View", defaultConfig,
            column(
                "PV",
                output(OutputNames.PRESENT_VALUE, InterestRateSwapSecurity.class),
                output(OutputNames.FX_PRESENT_VALUE, FXForwardSecurity.class)));

This could also be achieved using the following configuration, which uses ``OutputNames.PRESENT_VALUE`` for
all trade types except FX forward, for which an override is specified:

.. code:: java

    FunctionModelConfig defaultConfig =

    ViewConfig viewConfig =
        configureView(
            "Example View", defaultConfig,
            column(
                "PV", OutputNames.PRESENT_VALUE,
                output(OutputNames.FX_PRESENT_VALUE, FXForwardSecurity.class)));
