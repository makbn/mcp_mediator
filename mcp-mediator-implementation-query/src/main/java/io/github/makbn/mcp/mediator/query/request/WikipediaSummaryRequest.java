package io.github.makbn.mcp.mediator.query.request;

import io.github.makbn.mcp.mediator.api.McpMediatorRequest;
import io.github.makbn.mcp.mediator.api.McpTool;
import io.github.makbn.mcp.mediator.query.request.result.WikipediaQueryResult;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@McpTool(
        name = "summary_wikipedia",
        description = "Get summary of a Wikipedia page using the title of the page",
        schema = WikipediaSummaryRequest.class,
        annotations = {@McpTool.McpAnnotation(
                title = "Calls Wikipedia public APIs to fetch summary of a page by its title",
                readOnlyHint = true,
                idempotentHint = true,
                openWorldHint = true
        )})
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WikipediaSummaryRequest implements McpMediatorRequest<WikipediaQueryResult> {
    String title;
}
