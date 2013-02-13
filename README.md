# webnf - the web non-framework

## The use case

Set up a project talking to port 80

## But really

Letting any number of services run in one or more JVMs.  Services will
be configured using a plugin-extensible DSL, mostly in project.clj.

## DSL

DSL shall have following properties:

- Basically s-exprs, symbols in call position resolved with single active symbol table
- {} [] #{} can be aliased to a call in the symbol table.
- Each expression sets up the symbol table for subexpressions (better nesting)
- Expressions are analyzed for required maven coordinates, needed
  privileges and other requirements.
- Therefore marshallable between project contexts (leiningen.eval/eval-in)



## Why isnt 


Every project needs an outer shell, that connects it to the platform
below the runtime. The entry points, configuration and ideally
provision live there. 

Leiningen is a popular shell entry point, and one toplevel command
really should be enough for a lot of projects.
Luckily, with its new profile-based design it is also the ultimate
toplevel command and we want to tap into that.

    A bold claim? True.

The intention is having `simple` functionality items (which includes
other projects at this abstraction level) organized and ready for use.

Then we 

## Usage

FIXME

## License

Copyright © 2013 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
