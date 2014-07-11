==================
Regression Testing
==================

Introduction
============

The engine supports the automated capture of all inputs and outputs
from a view. The captured data can then be used at some point in
the future to rerun the view. By comparing the outputs produced
for this run with those produced during the original run, it is
possible to capture regressions. These tests can be automated using
a test framework so that regressions can be detected on all code
changes.

At the moment, the capture of data has to be done programmatically
though in the future it is anticipated that it will be possible to
do this from a UI.

Capturing the data
==================

In order to capture all the data used within a calculation it is 
necessary to disable the caches that normally allow calculations 
to run fast. This means that running a cycle and capturing the 
input data will run much slower than a normal cycle. For this
reason, capture is turned off by default and must be explicitly
switched on for a particular cycle. Doing this will then
automatically disable the caching for the cycle.

Where a call is currently made to set up and run a view:

.. code:: java

    View view = _viewFactory.createView(...);
    CycleMarketDataFactory cycleMarketDataFactory = ...;
    ...
    CycleArguments cycleArguments =
        new CycleArguments(valuationTime, VersionCorrection.LATEST, cycleMarketDataFactory);

    Results results = view.run(cycleArguments, securities);

The ``CycleArguments`` constructor call should be replaced with:

.. code:: java

    CycleArguments cycleArguments =
        new CycleArguments(valuationTime, VersionCorrection.LATEST, cycleMarketDataFactory, true);

Doing this means that the ``Results`` object returned can now be queried
for its inputs:

.. code:: java

    ViewInputs inputs = results.getViewInputs();

This object contains methods for accessing all the input data, but for regression
testing we need to store the input data and the results. The following code
will do this:

.. code:: java

    ViewResultsSerializer serializer = new ViewResultsSerializer(results);
    OutputStream vios = new FileOutputStream("/path/to/output/inputs_file.xml");
    OutputStream voos = new FileOutputStream("/path/to/output/outputs_file.xml");
    serializer.serializeViewInputs(vios);
    serializer.serializeViewOutputs(voos);

The two files contain all the view inputs and the output results in a
Fudge-encoded xml file.

Replaying the data
==================

Now that the data is captured it can be replayed using the current state
of the code base. This ensures that even if code is changed to enable
new functionality, an existing configuration will still calculate the same
results. First load in the data from the previously saved file:

.. code:: java

    ViewResultsDeserializer deserializer =
        new ViewResultsDeserializer(new FileInputStream("/path/to/output/inputs_file.xml"));
    ViewInputs viewInputs = inputsDeserializer.deserialize(ViewInputs.class);

The view inputs can then be used with the ``CapturedResultsLoader``
to run the view:

.. code:: java

    CapturedResultsLoader loader =
        new CapturedResultsLoader(viewInputs, availableOutputs, availableImplementations);
    Results results = loader.runViewFromInputs();

In cases where some data is provided to the original view configuration via
links, it is necessary to add in the linked data manually. This is due to the
links being resolved before the data capture takes place and will be
corrected in a future release. For the time being the additional config data
can be added as follows:

.. code:: java

    CapturedResultsLoader loader =
    new CapturedResultsLoader(viewInputs, availableOutputs, availableImplementations);
    loader.addExtraConfigData("ExampleCurrencyMatrix", ConfigItem.of(new SimpleCurrencyMatrix()));
    Results results = loader.runViewFromInputs();

Automatically testing for regressions
=====================================

The above steps mean that automatically testing for regressions is
straightforward. The following code can easily be run as part of
a CI environment:

.. code:: java

  @Test
  public void testViewRunsAsExpected() throws FileNotFoundException {

    ViewInputs viewInputs = deserializeComponent(
        ViewInputs.class, "/path/to/original_inputs.xml");
    ViewOutputs viewOutputs = deserializeComponent(
        ViewOutputs.class, "/path/to/original_outputs.xml");

    CapturedResultsLoader loader =
        new CapturedResultsLoader(viewInputs, createAvailableOutputs(),
             createAvailableImplementations());

    Results results = loader.runViewFromInputs();

    compareResults(results, viewOutputs);
  }

  private void compareResults(Results results, ViewOutputs originalOutputs) {

    assertThat(results.getColumnNames(), is(originalOutputs.getColumnNames()));
    assertThat(results.getNonPortfolioResults(), is(originalOutputs.getNonPortfolioResults()));

    List<ResultRow> originalOutputsRows = originalOutputs.getRows();
    List<String> errors = new ArrayList<>();

    for (int row = 0; row < originalOutputsRows.size(); row++) {

      ResultRow originalResultRow = originalOutputsRows.get(row);
      ResultRow calculatedRow = results.getRows().get(row);

      for (int col = 0; col < originalOutputs.getColumnNames().size(); col++) {

        Result<Object> originalResult = originalResultRow.get(col).getResult();
        Result<Object> calculatedResult = calculatedRow.get(col).getResult();

        if (!originalResult.equals(calculatedResult)) {
          errors.add("Row: " + originalResultRow.getInput() + ", Col: " +
              originalOutputs.getColumnNames().get(col) +
              "\nExpected: " + originalResult.toString() +
              "\nbut got: " + calculatedResult.toString());
        }
      }
    }

    if (!errors.isEmpty()) {
      fail(errors.toString());
    }
  }

