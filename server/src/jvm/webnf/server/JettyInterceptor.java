package webnf.server;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

public interface JettyInterceptor {
	void onEvent(Request request, Response response);
}
