======================================================
Writing Functions for the OpenGamma Calculation Engine
======================================================

Function Basics
^^^^^^^^^^^^^^^

Functions are the basic building block of calculations in the OpenGamma calculation engine. A function is simply
a Java object whose methods are invoked by the OpenGamma engine. For example, the following class could be used
as a very simple function:

.. code:: java

    public class HelloWorldFn {

        public String getMessage() {
            return "Hello, World!";
        }
    }

Anatomy of a function
---------------------
Functions are not required to implement any OpenGamma interface, but there are a number of characteristics a
class should have in order to work correctly as a function in the engine.

Functions should implement an interface
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The OpenGamma engine transparently provides services to functions such as caching, profiling and tracing of
calculations. These services can only be provided if the function implements an interface. For example, the function
above should be written as follows:

.. code:: java

    public interface HelloWorldFn {

        String getMessage();
    }

    public class DefaultHelloWorldFn implements HelloWorldFn {

        public String getMessage() {
            return "Hello, World!";
        }
    }

Functions must be immutable
~~~~~~~~~~~~~~~~~~~~~~~~~~~
Function classes should be immutable, and therefore stateless and thread safe. The OpenGamma calculation engine is
multithreaded so functions can be called concurrently on multiple threads.

Function dependencies
~~~~~~~~~~~~~~~~~~~~~
A function's dependencies should be passed into the constructor and stored in fields. If a function depends on
other functions, it should use the function's interface type, not its implementation type.

Function construction
~~~~~~~~~~~~~~~~~~~~~
Function instances are created by the OpenGamma engine. The engine locates or creates everything needed by the
function and invokes its constructor. Therefore a function must have at least one public constructor.

If a function has more than one constructor, one of them must be annotated with ``@Inject`` from the package
``javax.inject``. The engine will use the annotated constructor to create instances of the function.

More information about configuring functions is available here [TODO link to function config docs].

Function methods
----------------
A function, like any other Java type, can define any number of methods. Methods providing related functionality
will often be defined on the same function. For example, the following are methods on OpenGamma's standard function
for interest rate swaps:

.. code:: java

    Result<Double> calculateParRate(Environment env, InterestRateSwapSecurity security);

    Result<MultipleCurrencyAmount> calculatePV(Environment env, InterestRateSwapSecurity security);

    Result<ReferenceAmount<Pair<String, Currency>>> calculatePV01(Environment env, InterestRateSwapSecurity security);

``Environment`` parameter
~~~~~~~~~~~~~~~~~~~~~~~~~
By convention, the first parameter of function methods should be of type ``Environment``. This is not mandatory,
but most non-trivial functions will require access to data in the environment. The environment has methods
``getValuationDate()`` and ``getValuationTime()``. These are the *only* source a function should use to
get the current date and time. The environment also provides market data and arguments for functions that
implement scenarios (documented elsewhere [TODO link]).

``Result`` return type
~~~~~~~~~~~~~~~~~~~~~~
By convention, function methods should have a return type of ``Result<T>``, where ``T`` is the type of the
calculation result. The ``Result`` class can contain the result of a successful calculation or the details
of any problems that caused a calculation to fail.

If a function method is unable to complete its calculation it should use the ``Result.failure()`` method to
create a result containing details of the problem.

If a function calls another function it should check whether the result is successful before using its value.
There is no need to catch exceptions when invoking another function. If a function method throws an exception
it is caught by the engine and wrapped in a ``Result``.

Functions that provide outputs
------------------------------
Functions can be grouped into two categories - functions that produce intermediate values as part of
a calculation and functions that produce values which are of interest to the user. Values that are returned to
the user in the calculation results are referred to as "outputs". An output can be a risk measure, e.g. present value
or PV01, a trade attribute, e.g. quantity or description, or any other arbitrary value the user is interested in.

If a method of a function interface can produce an output it should have an annotation with the name of the output, e.g.
``@Output("Present Value")``. This allows the engine to choose the correct function when a user wants to
calculate present value (see the view configuration documentation for details [TODO link]).

Methods that produce outputs are invoked directly by the calculation engine. Therefore the engine must know how
to provide all the method arguments. Typically an method that produces an output will have two parameters, the
``Environment`` and the trade which is the subject of the calculation. For example, a method capable of calculating
the present value of an equity trade might have the following signature:

.. code:: java

    @Output("Present Value")
    Result<Double> calculatePresentValue(Environment env, EquityTrade trade);

The annotation tells the engine that the method can calculate an output called "Present Value" and the engine can
infer that it's the present value for an ``EquityTrade``.

Engine Services
^^^^^^^^^^^^^^^
The OpenGamma calculation engine provides a number of higher level services to functions.

Market Data
-----------
Provision of market data is obviously a key feature of a risk system. The OpenGamma platform includes
functions to provide market data: ``MarketDataFn`` (single values) and ``HistoricalMarketDataFn`` (time series of
values). A function that requires market data should declare a constructor parameter taking one of the market data
functions and the engine will provide it.

Caching of calculated values
----------------------------
Arguably the most important service provided by the calculation engine is the caching of calculated values.
If a value is expensive to calculate and is calculated more than once then it is a candidate for caching.
Caching is enabled by adding the annotation ``@Cacheable`` to the method declaration. No other changes are required.

Functions shouldn't ever need to implement their own caching. Function methods should be written naively so they
calculate a value every time they are invoked. If a cached value is available the engine will return it
and skip the calculation.

Caching in the OpenGamma engine is based on memoization. If a function method is invoked multiple times with
the same set of arguments then the same result is returned. This requires the parameter types to have sensible
implementations of ``equals()`` and ``hashCode()``.

If any of the parameters don't have a sensible definition of equality then caching will fail and the value
will be recalculated every time the method is called.

If any method arguments are mutable they must not be mutated in the function. Doing so would
cause undefined caching behaviour and potentially incorrect results. For this reason immutable types are
preferred as method parameters and arrays are specifically discouraged.

In order to take advantage of caching, a function method must be invoked through its interface. This means
that a function calling one of its own methods ( e.g. ``this.foo()``) will not benefit from caching.

Example Function
^^^^^^^^^^^^^^^^
This section demonstrates the implementation of a function to calculate an extremely simple but realistic risk
measure, present value of an equity security.

Function interface
------------------
The first task is to define an interface for the function. We only need one method which calculates the present value.
Assume the following definition of an equity trade:

.. code:: java

    public class EquityTrade {

        /**
         * @return  the quantity of the trade
         */
        public int getQuantity() {
            ...
        }

        /**
         * @return  the ID of the underlying equity security, e.g. BLOOMBERG_TICKER~AAPL US Equity
         */
        public ExternalIdBundle getSecurityId() {
            ...
        }
    }

The method must have an annotation to specify the output it produces and parameters for the environment and
the trade. The calculated value is a double (ignoring currency for simplicity), so the return type should be
``Result<Double>``. The method can have any name, so we can choose a descriptive one:

.. code:: java

    public interface EquityPresentValueFn {

        /**
         * @param env    the calculation environment
         * @param trade  the trade
         * @return       the present value of the trade
         */
        @Output("Present Value")
        Result<Double> calculatePresentValue(Environment env, EquityTrade trade);
    }

Function implementation
-----------------------
The present value of an equity depends on two things:

* The size of the trade - available from ``trade.getQuantity()``
* The current price of the underlying security - this requires market data

In order to request market data, the function needs a reference to ``MarketDataFn``. Therefore it must declare
a constructor parameter.

.. code:: java

    public class DefaultEquityPresentValueFn implements EquityPresentValueFn {

        private final MarketDataFn marketDataFn;

        public DefaultEquityPresentValueFn(MarketDataFn marketDataFn) {
            this.marketDataFn = marketDataFn;
        }

        public Result<Double> calculatePresentValue(Environment env, EquityTrade trade) {
            Result<Double> securityPrice = marketDataFn.getMarketValue(env, trade.getSecurityId());

            if (!securityPrice.isSuccess) {
                return Result.failure(securityPrice);
            }
            double presentValue = securityPrice.getValue() * trade.getQuantity();
            return Result.success(presentValue);
        }
    }

Testing
-------
Functions are normal Java classes which can be unit tested outside of the OpenGamma calculation engine. The functions
provided by the OpenGamma platform (e.g. market data functions) are also normal Java types which can be created
or mocked independently of the engine.

.. code:: java

    @Test
    public void equityPresentValue() {
        ExternalIdBundle securityId = ExternalId.of("BLOOMBERG_TICKER", "AAPL US Equity").toBundle();
        MarketDataFn marketDataFn = mock(MarketDataFn.class);
        Environment env = mock(Environment.class);
        when(marketDataFn.getMarketValue(env, securityId).thenReturn(94.8);
        EquityPresentValueFn presentValueFn = new DefaultEquityPresentValueFn(marketDataFn);
        EquityTrade trade = new EquityTrade(10_000, securityId);
        assertEquals(presentValueFn.calculatePresentValue(env, trade), 948000.0, 0.0001);
    }


TODO Topics not covered yet
^^^^^^^^^^^^^^^^^^^^^^^^^^^
* non-portfolio outputs
* functions should request all market data if possible, even in event of failure
* functions should only get data from the engine, don't go directly to the outside world
* modifying the environment before calling another function
* scenarios / decorators
* call tracing
