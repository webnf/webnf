package webnf.server;

import java.io.IOException;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;

public class JettyHandlerWrapper extends HandlerWrapper {
	public final JettyInterceptor start, finish;
	
	public JettyHandlerWrapper(Handler handler, JettyInterceptor start, JettyInterceptor finish) {
		this.start = start;
		this.finish = finish;
		setHandler(handler);
	}

	@Override
	public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
		try {
			notifyCeptor(start, baseRequest);
			super.handle(target, baseRequest, request, response);
		} catch(Error|IOException|ServletException|RuntimeException e) {
			if (!response.isCommitted() && !baseRequest.getHttpChannelState().isAsync()) {
				response.setStatus(500);
			}
			throw e;
		} finally {
			if (baseRequest.getDispatcherType().equals(DispatcherType.REQUEST)) {
                if (baseRequest.getHttpChannelState().isAsync())
                {
                    if (baseRequest.getHttpChannelState().isInitial()) {
                        baseRequest.getAsyncContext().addListener(new AsyncListener() {
							@Override
							public void onTimeout(AsyncEvent arg) throws IOException {
							}
							@Override
							public void onStartAsync(AsyncEvent arg) throws IOException {
								arg.getAsyncContext().addListener(this);
							}
							@Override
							public void onError(AsyncEvent arg) throws IOException {
					            if (!response.isCommitted())
					                response.setStatus(500);
							}
							@Override
							public void onComplete(AsyncEvent arg) throws IOException {
								notifyCeptor(finish, baseRequest);
							}
						});
                    }
                } else {
                	notifyCeptor(finish, baseRequest);
                }
			}
		}
	}

	private static void notifyCeptor(JettyInterceptor ceptor, Request baseRequest) {
		if (ceptor != null) {
			ceptor.onEvent(baseRequest, baseRequest.getResponse());
		}
	}
}
