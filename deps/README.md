Here dependency-only projects, that aim to fulfill a role similar to how the old clojure-contrib project was used:

    To get a set of workable state-of-the-art dependencies fast.

Since old clojure-contrib wasn't actually meant for that task, but as
an umbrella to distribute code with a license compatible to clojure,
there was a shift towards new contrib. People learned to use github
and clojars to share code.

webnf.deps aims to provide some of that "start project once" feel, but
also to establish a canon of libraries.

## Guiding principles

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
