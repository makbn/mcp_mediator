package io.github.makbn.mcp.mediator.commons.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.makbn.mcp.mediator.api.McpMediatorException;
import io.github.makbn.mcp.mediator.api.McpMediatorRequest;
import io.github.makbn.mcp.mediator.api.McpMediatorRequestHandler;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;


@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PROTECTED)
public abstract class McpMediatorHttpRequestHandler<T extends McpMediatorRequest<R>, R> implements McpMediatorRequestHandler<T, R> {
    CloseableHttpClient httpClient;
    ObjectMapper objectMapper;

    protected McpMediatorHttpRequestHandler() {
        this(HttpClients.createDefault(), JsonMapper.builder().build());
    }

    @Override
    public R handle(T request) throws McpMediatorException {
        return executeHttp(request);
    }

    protected R executeHttp(T request) {
        try {
            ClassicHttpRequest httpRequest = createHttpRequest(request);
            return httpClient.execute(httpRequest, classicHttpResponse -> {
                if (classicHttpResponse.getCode() == HttpStatus.SC_OK) {
                    return convertResponseToResult(classicHttpResponse);
                } else {
                    throw new McpMediatorException("HTTP request failed with status code: " + classicHttpResponse.getCode());
                }
            });
        } catch (Exception e) {
            throw new McpMediatorException("Failed to execute HTTP request", e);
        }
    }

    protected ClassicHttpRequest createHttpRequest(@NonNull T request) {
        ClassicRequestBuilder requestBuilder = createHttpMethodBuilder(request);
        setHeaders(requestBuilder, request);
        setCharset(requestBuilder, request);
        setParameters(requestBuilder, request);
        requestBuilder.setUri(getUri(request));
        getRequestBody(request).ifPresent(requestBuilder::setEntity);
        modifyRequest(requestBuilder, request);
        return requestBuilder.build();
    }

    protected void setParameters(@NonNull ClassicRequestBuilder requestBuilder, @NonNull T request) {
        getParameters(request).ifPresent(params ->
                params.forEach(param -> requestBuilder.addParameter(param.getName(), param.getValue())));
    }

    @SuppressWarnings("unused")
    protected void setCharset(@NonNull ClassicRequestBuilder requestBuilder, @NonNull T request) {
        requestBuilder.setCharset(StandardCharsets.UTF_8);
    }

    protected void setHeaders(@NonNull ClassicRequestBuilder requestBuilder, @NonNull T request) {
        getHeaders(request).stream()
                .filter(Objects::nonNull)
                .forEach(header -> requestBuilder.addHeader(header.getName(), header.getValue()));
    }

    @NonNull
    protected Collection<Header> getHeaders(@NonNull T request) {
        return Collections.emptyList();
    }

    @NonNull
    protected ClassicRequestBuilder createHttpMethodBuilder(@NonNull T request) {
        ClassicRequestBuilder requestBuilder;
        Class<? extends HttpUriRequestBase> baseMethod = getHttpMethod(request);

        if (HttpGet.class.isAssignableFrom(baseMethod)) {
            requestBuilder = ClassicRequestBuilder.get();
        } else if (HttpPost.class.isAssignableFrom(baseMethod)) {
            requestBuilder = ClassicRequestBuilder.post();
        } else if (HttpPut.class.isAssignableFrom(baseMethod)) {
            requestBuilder = ClassicRequestBuilder.put();
        } else if (HttpDelete.class.isAssignableFrom(baseMethod)) {
            requestBuilder = ClassicRequestBuilder.delete();
        } else if (HttpHead.class.isAssignableFrom(baseMethod)) {
            requestBuilder = ClassicRequestBuilder.head();
        } else if (HttpOptions.class.isAssignableFrom(baseMethod)) {
            requestBuilder = ClassicRequestBuilder.options();
        } else if (HttpPatch.class.isAssignableFrom(baseMethod)) {
            requestBuilder = ClassicRequestBuilder.patch();
        } else if (HttpTrace.class.isAssignableFrom(baseMethod)) {
            requestBuilder = ClassicRequestBuilder.trace();
        } else {
            throw new McpMediatorException("Unsupported HTTP method: " + baseMethod.getSimpleName());
        }
        return requestBuilder;
    }

    @SuppressWarnings("unused")
    protected void modifyRequest(@NonNull ClassicRequestBuilder requestBuilder, @NonNull T request) {
        // do nothing by default
    }

    @NonNull
    protected abstract Class<? extends HttpUriRequestBase> getHttpMethod(@NonNull T request);

    @NonNull
    protected abstract String getUri(@NonNull T request);

    protected abstract Optional<HttpEntity> getRequestBody(@NonNull T request);

    protected abstract Optional<List<NameValuePair>> getParameters(@NonNull T request);

    protected R convertResponseToResult(ClassicHttpResponse response) throws IOException, ParseException {
        return objectMapper.convertValue(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8), new TypeReference<R>() {
        });
    }

}
