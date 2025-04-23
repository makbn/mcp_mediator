package io.github.makbn.mcp.mediator.query.request;

import io.github.makbn.mcp.mediator.api.McpMediatorRequest;
import io.github.makbn.mcp.mediator.api.McpTool;
import io.github.makbn.mcp.mediator.query.request.result.WikipediaQueryResult;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@McpTool(
        name = "search_wikipedia",
        description = "Search and query Wikipedia website using the given query for information about a topic, " +
                "person, place, event, etc",
        schema = WikipediaQueryRequest.class,
        annotations = {@McpTool.McpAnnotation(
                title = "Query Wikipedia public APIs for information about a topic by searching both text an topic",
                readOnlyHint = true,
                idempotentHint = true,
                openWorldHint = true
        )})
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WikipediaQueryRequest implements McpMediatorRequest<WikipediaQueryResult> {
    String query;
}
