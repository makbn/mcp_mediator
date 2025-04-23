package io.github.makbn.mcp.mediator.query.request.result;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;


@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WikipediaQueryResult extends GenericQueryResult{
    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class WikipediaQueryResultPage {
        String title;
        String snippet;
        String url;
        String lastUpdated;
    }

    @Singular
    List<WikipediaQueryResultPage> results;
}
