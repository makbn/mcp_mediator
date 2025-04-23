package io.github.makbn.mcp.mediator.docker.handler;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class WikipediaQueryRequestHandler extends McpMediatorHttpRequestHandler<WikipediaQueryRequest, WikipediaQueryResult> {
    private static final String WIKIPEDIA_API_URL = "https://en.wikipedia.org/w/api.php";

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
                new BasicNameValuePair("action", "query"),
                new BasicNameValuePair("format", "json"),
                new BasicNameValuePair("prop", "info|revisions"),
                new BasicNameValuePair("generator", "search"),
                new BasicNameValuePair("rvprop", "timestamp"),
                new BasicNameValuePair("inprop", "url"),
                new BasicNameValuePair("gsrsearch", request.getQuery())
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
    protected WikipediaQueryResult convertResponseToResult(ClassicHttpResponse response) throws IOException, ParseException {
        JsonNode root = objectMapper.readTree(EntityUtils.toString(response.getEntity()));
        JsonNode pages = root.path("query").path("pages");

        WikipediaQueryResult.WikipediaQueryResultBuilder resultBuilder = WikipediaQueryResult.builder();

        if (pages.isMissingNode()) {
            return resultBuilder.build();
        }

        for (JsonNode page : pages) {
            WikipediaQueryResult.WikipediaQueryResultPage.WikipediaQueryResultPageBuilder pageBuilder = WikipediaQueryResult.WikipediaQueryResultPage.builder();
            String title = page.path("title").asText();
            pageBuilder.title(title);
            pageBuilder.url(page.path("fullurl").asText("https://en.wikipedia.org/wiki/" + URLEncoder.encode(title, StandardCharsets.UTF_8)));

            JsonNode revisions = page.path("revisions");
            if (revisions.isArray() && !revisions.isEmpty()) {
                pageBuilder.lastUpdated(revisions.get(0).path("timestamp").asText());
            }
            // TODO: implement chain logic
            pageBuilder.snippet("N/A (snippet not included in this response)");
            resultBuilder.result(pageBuilder.build());
        }

        return resultBuilder.build();
    }
}
