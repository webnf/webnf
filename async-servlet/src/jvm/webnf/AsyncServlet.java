package webnf;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Var;

public class AsyncServlet extends HttpServlet {
	private static final long serialVersionUID = 0L;
	public static final Var RESOLVE = Var.intern(
			Symbol.intern("webnf.async-servlet.impl"),
			Symbol.intern("resolve-config-var"));
	public static final Var SERVICE = Var.intern(
			Symbol.intern("webnf.async-servlet.impl"),
			Symbol.intern("handle-servlet-request"));
	private volatile Var on_init, on_service, on_destroy;
	
	public AsyncServlet() {
		try {
			RT.load("webnf/async_servlet/impl");
		} catch (Exception e) {
			throw new RuntimeException("During load webnf.async_servlet", e);
		}
	}
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		log("Initializing AsyncServlet " + config.getServletName());
		log("webnf.handler.service: " + config.getInitParameter("webnf.handler.service"));
		if (3 > config.getServletContext().getEffectiveMajorVersion()) {
			throw new ServletException("This servlet requires javax.servlet >= 3.0");
		}
		on_service = (Var) RESOLVE.invoke(config, "webnf.handler.service");
		if (on_service == null) {
			throw new ServletException("Could not find service handler: " +
					config.getInitParameter("webnf.handler.service"));
		}
		on_init = (Var) RESOLVE.invoke(config, "webnf.handler.init");
		on_destroy = (Var) RESOLVE.invoke(config, "webnf.handler.destroy");
		if (on_init != null) {
			on_init.invoke();
		}
	}
	
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) 
			throws ServletException, IOException {
		SERVICE.invoke(on_service, req, resp);
	}
	
	@Override
	public void destroy() {
		log("Destroying AsyncServlet");
		if (on_destroy != null) {
			on_destroy.invoke();
		}
	}
}
