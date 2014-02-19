# webnf/server

This is a very lightweight servlet container based on jetty-webapp.
It can run:

- PHP webapps via FastCGI
  If you want to use this, run fpm and direct
  an fcgi-handler to it.  Set the working directory to the project you
  want to host.  Be careful with access rights. The handler doesn't
  understand .htaccess and will serve the whole working dir.
  
- Servlet according to specification 3.0

After the server is started, apps are attached as vhosts specifying
the http hosts they serve.

The Servlet class as a basic interface makes it possible, to run
multiple clojure runtimes, with different versions for different
projects, as long as the servlet api class are in a common base
classloader.

## Usage

The server is designed to be run on an agent. The initial state is
created by the function `webnf.server/server`, with the config syntax
from ring-jetty-adapter. It can be started and stopped by `send`ing
`start!` and `stop!`. At any point, apps can be added and removed via
`add-vhost!` and `remove-vhost!`.

## FIXME

There should be support code to run apps in separate classloaders.

The codebase is in recovery from adding detailed request tracking,
cleanup and doc are in order.

## License

Copyright © 2013 Herwig Hochleitner

Distributed under the Eclipse Public License, the same as Clojure.
