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

Actually Ring. Plus lot's of tidbits, that don't fit into ring's scope
and some that might.

### Why not Pedestal?

async-servlet keeps true to the original ring lifecycle and just
allowing you to extend it past the return in a way closely modelled
onto servlet 3.0

It is done in a way that allows you to utilize middlewares if they
only care about status and headers (since you just return a special
response body to start async)
