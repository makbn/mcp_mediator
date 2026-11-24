# MCP Mediator – Revamp Plan (Spring Boot 4.x / Spring AI MCP compatibility)

## 0. Status

| Phase | State |
|---|---|
| 0 Baseline, 1 Platform bump (Boot 4.0/4.1/4.2 matrix green) | done |
| 2 Annotation model (both families, `@McpToolParam`/`@McpArg`) | done |
| 3 Server features: resources, templates, prompts, completions, logging/capabilities | done (tests: `AnnotationFamiliesParityTest`) |
| 4 Spring: bean scanning, auto-config, back-off, proxies | done (tests: `McpMediatorAutoConfigurationTest`) |
| 5 Exchange/`CallToolRequest`/progress-token injection, tool title + hints | done (`ToolContextAndMetadataTest`) |
| 5b `McpSyncRequestContext`, client handlers, async methods, output schema, streamable HTTP | done (`ToolContextAndMetadataTest`, `AsyncAndOutputSchemaTest`, `McpClientHandlersTest`, `StreamableHttpSampleAppTest`) |
| 5c `McpMeta`, multi-arg client handlers, SSE auto-mount, live sampling round trip | done (`LiveClientRoundTripTest`, `SseTransportTest`) |
| 5d `McpAsyncRequestContext`, `clients=` routing, stateless transport | done (`ToolContextAndMetadataTest`, `McpClientHandlersTest`, `StatelessHttpTest`) |
| 6 Examples | non-Spring example, Spring Boot sample app (`StreamableHttpSampleAppTest`), Spring context tests |
| 7 Docs | README section done |

Fixed along the way: draft-04 `id` in tool schemas rejected by SDK validator, `required` list used raw param names,
generated request-class name collisions, `@McpArgument` unusable on parameters, non-public bean methods not invokable,
complex argument types (records/POJOs/collections) not converted, networknt/Jackson 3 clash with Boot 4.

Differential verification: `SpringAi2ReferenceParityTest` compares against Spring AI 2.1's own providers (found and
fixed: `List<String>` params were advertised as arrays of objects, String-returning prompts must be `assistant`
messages). Spring AI 2.x annotations live in `org.springframework.ai.mcp.annotation`; both generations are supported.
MCP SDK is now 2.2.0 (required by Spring AI 2.1).

Added after review: `@McpIcon`, `requiredCapabilities`, `size`, `metaProvider`, the SDK "modern" server
(`MODERN_HTTP`, `McpModernServer`) with `McpModernSyncRequestContext` injection, and Spring AI's required-by-default
parameter semantics (found by diffing against Spring AI's Modern tool provider).

Coverage guard (`SpringAiAnnotationCoverageTest`) enumerates all Spring AI 2.1 annotation attributes; it found
`@McpToolParam.header` and `@McpArg.title`, now implemented.

## 1. Goal

Spring AI now ships its own annotations (`@McpTool`, `@McpResource`, `@McpPrompt`, `@McpComplete`, `@McpToolParam`, ...,
from `org.springaicommunity:mcp-annotations`). A Spring-only annotation layer in this project is therefore redundant.

New positioning, analogous to JSR-250 `@Resource` vs Spring `@Autowired`:

| Environment            | Annotations used                                               |
|------------------------|----------------------------------------------------------------|
| Plain Java (no Spring) | Mediator's own annotations (`io.github.makbn.mcp.mediator.api`) – dependency-free |
| Spring / Spring Boot   | Spring AI native annotations work **as-is**; mediator's own annotations also keep working |

Both families are normalised into one internal model (`McpAnnotationModel`), so every downstream feature
(schema generation, invocation, mediator registration, proxying, security) is written once.

## 2. Design

1. **Own annotation set (api module, zero deps)** – extend what exists, keep it backward compatible:
   `@McpService`, `@McpTool` (existing; add `title`, `annotations` kept, `generateOutputSchema`), new `@McpToolParam`
   (description/required/name), `@McpResource`, `@McpPrompt`, `@McpPromptArg`/param, `@McpComplete`,
   plus handler-side client annotations `@McpLogging`, `@McpSampling`, `@McpElicitation`, `@McpProgress` are mapped
   later (phase 5). Existing `@McpArgument` stays (deprecated alias of `@McpToolParam`).
2. **Annotation scanner (core)** – `McpAnnotationScanner` resolves, per method, a descriptor
   `ToolDescriptor | ResourceDescriptor | PromptDescriptor | CompleteDescriptor` from *either*
   family. Spring AI annotations are read **reflectively by fully-qualified name** (no compile-time dep in core),
   so core stays usable without Spring AI on the classpath.
3. **Special parameters** – `McpSyncRequestContext`-style context, `McpMeta`, progress token, `Exchange` injection:
   resolver ignores these in the JSON schema and injects them at call time.
4. **Mediator** – `DefaultMcpMediator` currently only registers tools. Extend capabilities to resources, resource templates,
   prompts, completions, logging, and list-changed notifications, via the MCP Java SDK (`io.modelcontextprotocol.sdk`).
5. **Spring module (`mcp-mediator-spring`)** – `BeanPostProcessor`/`SmartInitializingSingleton` scans beans for either annotation
   family and registers them (no `@McpService` required on Spring beans; `@McpService` stays optional metadata).
   If Spring AI's own MCP server starter is present, the mediator backs off (`@ConditionalOnMissingBean`) or contributes
   its handlers as Spring AI specification beans so both can coexist.
6. **Boot starter** – auto-config via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
   (Boot 3+/4 style), properties under `mcp.mediator.*`. Controller-to-MCP bridge kept (uses `RequestMappingHandlerMapping`,
   which is unchanged in Spring Framework 7).
7. **Backward compatibility** – public API (`McpServiceFactory`, `DefaultMcpMediator`, `McpMediatorConfigurationBuilder`,
   `@McpService`/`@McpTool` attributes) is unchanged; additions only. Behavioural fixes are listed in section 5.

## 3. Platform upgrade

* Spring Boot 4.2.x (Spring Framework 7, Jakarta EE 11, Jackson 3 default, Jackson 2 still supported by the MCP SDK
  `mcp-json-jackson2`) – verify with a test matrix on 4.0.x, 4.1.x and 4.2.x.
* Spring AI 2.x, MCP Java SDK 2.x, `mcp-annotations` 0.9.x. Java 21 baseline (already).
* Parent POM: version properties overridable with `-Dspring.boot.version=...` so CI can run the matrix.

## 4. Phases

| # | Phase | Deliverable | Verification |
|---|-------|-------------|--------------|
| 0 | Baseline | Build + existing tests green on current stack; record failures | `mvn verify` |
| 1 | Platform bump | POM bumped to Boot 4.x / Spring AI 2.x; code compiles; existing tests green | `mvn verify`, matrix 4.0/4.1/4.2 |
| 2 | Annotation model | New annotations in api, `McpAnnotationScanner` in core handling both families, `@McpToolParam`, special params | unit tests for both families on the same fixture class |
| 3 | Feature parity (server) | Resources, templates, prompts, completions, output schema, structured content, tool annotations, list-changed | unit + in-memory MCP client/server round trip tests |
| 4 | Spring integration | Bean scanning for both families, auto-config, back-off with Spring AI starter, `@Resource`/`@Autowired`-style injection of beans in tools | `@SpringBootTest` + `ApplicationContextRunner` tests |
| 5 | Client-side features | logging, sampling, elicitation, progress, roots handler annotations; HTTP transports (SSE, streamable) via Spring | integration tests |
| 6 | Examples | One non-Spring example (own annotations), one Spring Boot example (Spring AI annotations), one mixed | examples run in tests (`@SpringBootTest`) |
| 7 | Docs | README rewrite: when to use which annotation family, compatibility matrix, migration notes | review |

## 5. Known issues to fix on the way

* `McpMethodArgumentResolver.convertValue` only handles primitives/String; complex types (records, POJOs, `List`, `Map`,
  enums) need Jackson `convertValue`.
* `registerTypeConverter` is an instance method mutating a static map; make it static.
* `McpServiceFactory` only scans `getDeclaredMethods()` of the concrete class – misses proxied (CGLIB) beans and
  inherited methods; use `ClassUtils`-style user-class resolution.
* `McpMethodAdapter.getAnnotations()` throws `UnsupportedOperationException`.
* `DefaultMcpMediator` advertises only `tools` capability.
* Generated request class naming `generated.<method>_Request` collides for overloaded methods.
* Auto-config class lacks `AutoConfiguration.imports` registration and is an empty shell.
* Controller bridge ignores `@RequestBody`/`@PathVariable`/`@RequestParam` semantics – document or map.

## 6. Test strategy

* Unit tests per module (JUnit 5, already in BOM).
* Parity tests: the same fixture exercised through (a) own annotations, (b) Spring AI annotations; asserts identical
  tool/resource/prompt definitions.
* Round-trip tests using the MCP SDK in-memory/stdio transport client against `DefaultMcpMediator`.
* Spring tests with `ApplicationContextRunner` for conditions/back-off, `@SpringBootTest` for end-to-end.
* CI matrix for Spring Boot 4.0 / 4.1 / 4.2.

## 7. Risks

* Spring AI 2.x / `mcp-annotations` APIs move between milestones; reflective reading isolates core from this.
* Jackson 3 (`tools.jackson`) vs Jackson 2 – the mediator keeps Jackson 2 `ObjectMapper` in its public API; a Boot 4 app
  needs `spring-boot-jackson2` or the mediator must bridge. To be settled in phase 1 with a spike.
* 4.2 is currently RC; target GA when released.
