# A Web Non-Framework

webnf aims to give you a full web stack, while getting out of the way
and out of your repl. It assumes that you know your way around clojure
and gives you:

- Meta dependencies with lots of popular libraries to just use it (tm)
  and worry about minimal dependency sets before packaging
- Lots of missing bits and pieces, some of this should be patches to various projects and will be deprecated if they are incorporated
- An AsyncServlet working in every Servlet 3.0 container, for long-polling and streaming
- A jetty runner utilizing servlet infrastructure to do:
  - virtual hosting
  - detailed request logging
  - classloader per webapp
  - calling out via fcgi e.g. to php
- A ClojureScript port of enlive

This started as one developer's library code shared between various
web projects and then grew some architectural spines in the form of
the vhosting servlet runner.

## Dependency curation

Webnf maintains a couplpe of dependency-only projects, that aim to fulfill a role similar to how the old clojure-contrib project was used:

    To get a set of workable state-of-the-art dependencies fast.

Since old clojure-contrib wasn't actually meant for that task, but as
an umbrella to distribute code with a license compatible to clojure,
there was a shift towards new contrib. People learned to use github
and clojars to share code.

webnf.deps aims to provide some of that "start project once" feel, but
also to establish a canon of libraries.

### Guiding principles

- webnf.deps aims to provide the working set that's there for you to require.

- The primary focus is generally applicable libraries.
  - Algorithms
  - Data Formats (parsers, emitters, algo impls on them)
  - very popular web APIs
  - tools that don't require any setup

- When two dependencies provide similar functionality, only one should be included.
  - The pure clojure version wins.
  - Overlapping concerns between libaries are permitted, if they share data formats.

- Licensing in a dependency is a secondary consideration, but you should be free to use and modify it.


## Help wanted

We are looking for code that should be useful to most clojure coders,
especially when working on web apps.

## Will it ever be done?

There are two important milestones, before webnf should be considered near 1.0:

1. There needs to be a leiningen plugin for the jetty server itself
   and for projects to run on it. Maybe we can take a page out of
   immutant's book.

2. A way to undeploy a webapp and free all of its permgen. This is
   kind of a long shot and not directly related to webnf, but it's
   crucial in order to make hosting on a shared JVM solid.  One
   developer has plans to use the debugging APIs to find all objects
   keeping a classloader alive.

## F.A.Q

### Why not Immutant?

It happens that one developer was very comfortable with jetty and
didn't want any EE. This only affects webnf/server. async-servlet and
the other stuff should be fine in immutant or any modern servlet
container, really.

### Why not Ring?

Actually Ring. Plus lots of tidbits, that don't fit into ring's scope
and some that might.

### Why not Pedestal?

async-servlet keeps true to the original ring lifecycle and just
allowing you to extend it past the return in a way closely modelled
onto servlet 3.0

It is done in a way that allows you to utilize middlewares if they
only care about status and headers (since you just return a special
response body to start async)
