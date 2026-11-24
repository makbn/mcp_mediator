package demo;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;

@SpringBootApplication
public class App {
    @Service
    public static class Tools {
        @McpTool(name = "double_it", description = "doubles")
        public int doubleIt(@McpToolParam(description = "n") int n) { return n * 2; }
    }
}
