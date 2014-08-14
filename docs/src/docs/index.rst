.. RV-Predict documentation master file, created by
   sphinx-quickstart on Wed Jul  2 18:03:41 2014.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

Welcome!
========

Welcome to RV-Predict's documentation!
--------------------------------------

RV-Predict is the only dynamic data race detector that is both sound and
maximal. *Dynamic* means that it executes the program in order to extract
an execution trace to analyze. *Sound* means that it only reports races
which are real (i.e., no false positives). And *maximal* means that it finds
all the races that can be found by any other sound race detector analyzing
the same execution trace. The technology underlying RV-Predict is best
explained in the following paper:

Jeff Huang, Patrick O'Neil Meredith, and Grigore Rosu. 2014.
`Maximal sound predictive race detection with control flow abstraction`_.
In Proceedings of the 35th ACM SIGPLAN Conference on
Programming Language Design and Implementation (PLDI '14).
ACM, New York, NY, USA, 337-348. DOI=10.1145/2594291.2594315

.. _Maximal sound predictive race detection with control flow abstraction: http://dx.doi.org/10.1145/2594291.2594315

Contents
--------------------------------------

.. toctree::
   self

.. toctree::

   :maxdepth: 2
   quickstart
   runningexamples

Indices and tables
--------------------------------------

* :ref:`genindex`
* :ref:`search`

