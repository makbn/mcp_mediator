package io.github.makbn.mcp.mediator.query.handler;

import io.github.makbn.mcp.mediator.api.McpMediatorRequest;
import io.github.makbn.mcp.mediator.commons.handler.McpMediatorHttpRequestHandler;
import io.github.makbn.mcp.mediator.query.request.WikipediaQueryRequest;
import io.github.makbn.mcp.mediator.query.request.result.WikipediaQueryResult;
import lombok.NonNull;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class WikipediaSummaryRequestHandler extends McpMediatorHttpRequestHandler<WikipediaQueryRequest, WikipediaQueryResult> {
    private static final String WIKIPEDIA_API_URL = "https://en.wikipedia.org/api/rest_v1/page/summary/";

    @Override
    protected @NonNull Class<? extends HttpUriRequestBase> getHttpMethod(@NonNull WikipediaQueryRequest request) {
        return HttpGet.class;
    }

    @Override
    protected @NonNull String getUri(@NonNull WikipediaQueryRequest request) {
        return WIKIPEDIA_API_URL;
    }

    @Override
    protected Optional<HttpEntity> getRequestBody(@NonNull WikipediaQueryRequest request) {
        return Optional.empty();
    }

    @Override
    protected Optional<List<NameValuePair>> getParameters(@NonNull WikipediaQueryRequest request) {
        return Optional.of(List.of(

        ));
    }

    @Override
    public String getName() {
        return "wikipedia-query-request-handler";
    }

    @Override
    public boolean canHandle(McpMediatorRequest<?> request) {
        return request instanceof WikipediaQueryRequest;
    }

    @Override
    public Collection<Class<? extends WikipediaQueryRequest>> getAllSupportedRequestClass() {
        return List.of(WikipediaQueryRequest.class);
    }

    /**
     * The result returned by the Wikipedia API is different from the response we have for this request handler. We need
     * to convert it to the format we expect.
     *
     * @param response response from the Wikipedia API. We don't use it directly,
     *                 but we need to convert it to the format we expect.
     */
    @Override
    protected WikipediaQueryResult convertResponseToResult(ClassicHttpResponse response) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
