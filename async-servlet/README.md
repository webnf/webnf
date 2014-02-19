# async-servlet

A servlet implementation, bringing servlet 3.0 async capabilities to clojure.

## Usage

webnf.AsyncServlet takes following ServletConfig parameters:

- `webnf.handler.service`: Name of a var holding the servlet's ring handler
- `webnf.handler.init`: Name of a var called when servlet is initialized
- `webnf.handler.destroy`: Name of a var called when servlet is destroyed

Handler is allowed to return a function as a response body.  This
function will be called with an AsyncContext and can return a map of
up to three callbacks

- `:error` -- callback when request errors
- `:timeout` -- callback when request times out
- `:complete` -- callback when request completes

It is allowed to use the AsyncContext at any time, preferrably through
the helpers defined in `webnf.async-servlet`: `status`, `headers`, `flush`,
`chunk`, `complete`.

`webnf.async-servlet/log-listener` is a base set of
callbacks to return from the async handler, to be assoc'ed on.

## License

Copyright © 2013 Herwig Hochleitner

Distributed under the Eclipse Public License, the same as Clojure.
