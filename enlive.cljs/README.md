# webnf/enlive.cljs

This is a ClojureScript port of enlive. Not some syntax transformation
the way enfocus does (or used to do), but a straight port of the
actual engine. This is great because that way, the selector semantics is exactly
the same and edge cases like [root :> :p] work just fine out of the box.

## FIXME

There has to be a good way to do the transformation output, without
cloning the source and using mutating operations. OM comes to mind,
maybe react.js' shadow DOM can do something for us.

Create a unified clojure and clojurescript version, for flexible
client- and serverside templating.

## License

Copyright © 2013 Herwig Hochleitner

Distributed under the Eclipse Public License, the same as Clojure.
