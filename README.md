# Directed Acyclic Graph Executor Engine
_English version in progress_

The directed acyclic graph (DAG) executor engine decomposes business logic into multiple sub-logics. Each sub-logic corresponds to a node in the DAG. The dependency relationships between the sub-logics correspond to the directed edges between the nodes.  A single execution of business logic amounts to a single execution of a graph task.

The core of the executor engine consists of topological sorting and multithreading synchronization, which supports maximizing the parallel execution of nodes while minimizing the number of thread context switches.