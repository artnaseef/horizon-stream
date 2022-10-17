package org.opennms.horizon.notifications.config.keycloak;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class HttpClientRetryUtil {

    public static final HttpClientRetryUtil INSTANCE = new HttpClientRetryUtil();

    /**
     * Execute the given HttpUriRequest (e.g. HttpGet, HttpPost, ...) with a single retry, as needed.  Only a single
     * retry is attempted.  Note that retry is never performed on exception thrown by the original request processing.
     *
     * The postRequestOp has the following responsibilities:
     *  - Return true when retry is required, and false otherwise.
     *  - Update the request as-needed for the retry (e.g. updating Authorization header with a refreshed bearer token)
     *
     * @param httpClient HttpClient that will execute the request.
     * @param request HttpUriRequest to send.
     * @param postRequestOp function that takes the HttpResponse and original request, determines whether a retry is
     *                      required, updates the request as-needed, and returns true when a retry is needed.
     * @return response of the original request, if no retry was processed, or the retry otherwise.
     * @throws IOException
     */
    public HttpResponse executeWithRetry(
            HttpClient httpClient,
            HttpUriRequest request,
            PostRequestOp postRequestOp) throws IOException {

        HttpResponse httpResponse = httpClient.execute(request);

        boolean retryInd = postRequestOp.process(request, httpResponse);

        if (retryInd) {
            EntityUtils.consumeQuietly(httpResponse.getEntity());
            httpResponse = httpClient.execute(request);
        }

        return httpResponse;
    }
}
