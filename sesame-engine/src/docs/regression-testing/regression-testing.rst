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
reason, it is turned off by default and must be explicitly switched
on for a particular cycle.

Where a call is currently made to set up and run a view:

.. code:: java

    View view = _viewFactory.createView(...);
    CycleMarketDataFactory cycleMarketDataFactory = ...;
    ...
    CycleArguments cycleArguments =
        new CycleArguments(valuationTime, VersionCorrection.LATEST, cycleMarketDataFactory);

    Results results = view.run(cycleArguments, securities);

The '''CycleArguments''' constructor call should be replaced with:

.. code:: java

    CycleArguments cycleArguments =
        new CycleArguments(valuationTime, VersionCorrection.LATEST, cycleMarketDataFactory, true);



Replaying the data
==================





Automatically testing for regressions
=====================================



