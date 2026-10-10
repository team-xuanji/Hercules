package team.magic.flute.hercules.manager.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Caches the raw request body for signed {@code /taskDispatch/} PUT requests
 * so that signature verification (which needs the wire bytes) can read them
 * after Spring's {@code @RequestBody} deserialization has already consumed the
 * input stream.
 *
 * <p>The cached bytes are exposed both via {@code request.getAttribute(RAW_BODY_ATTR)}
 * (read by {@code TaskDispatchController.isInvalidExecutorOp}) and via the
 * wrapper's {@code getInputStream}/{@code getReader}, so the rest of the
 * request pipeline reads the same bytes the signature was computed over.
 * GET requests and non-{@code /taskDispatch/} paths are passed through
 * untouched.
 */
@Component
@Slf4j
public class CachedBodyFilter extends OncePerRequestFilter {

    public static final String RAW_BODY_ATTR = "hercules.rawBody";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   javax.servlet.FilterChain filterChain) throws ServletException, IOException {
        if ("PUT".equalsIgnoreCase(request.getMethod())
                && request.getServletPath() != null
                && request.getServletPath().startsWith("/taskDispatch/")) {
            byte[] cached;
            try {
                cached = StreamUtils.copyToByteArray(request.getInputStream());
            } catch (IOException e) {
                log.warn("[CACHED_BODY] Failed to read request body; treating as empty.", e);
                cached = new byte[0];
            }
            request.setAttribute(RAW_BODY_ATTR, cached);
            filterChain.doFilter(new CachedBodyRequestWrapper(request, cached), response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Replays a fixed byte array for every read of the body, so downstream
     * consumers (@RequestBody marshalling, signature verification) see the
     * identical wire bytes regardless of read order.
     */
    static final class CachedBodyRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] body;

        CachedBodyRequestWrapper(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body == null ? new byte[0] : body;
        }

        @Override
        public ServletInputStream getInputStream() {
            final ByteArrayInputStream in = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return in.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int read() {
                    return in.read();
                }

                @Override
                public int read(byte[] b, int off, int len) {
                    return in.read(b, off, len);
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), getCharacterEncoding() == null
                    ? java.nio.charset.StandardCharsets.UTF_8
                    : java.nio.charset.Charset.forName(getCharacterEncoding())));
        }
    }
}
