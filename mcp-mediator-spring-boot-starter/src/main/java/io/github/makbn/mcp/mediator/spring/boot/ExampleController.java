package io.github.makbn.mcp.mediator.spring.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@SpringBootApplication
public class ExampleController {

    public static void main(String[] args) {
        SpringApplication.run(ExampleController.class, args);
    }


    @GetMapping("/employees")
    List<String> all() {
        return List.of("John Doe", "Jane Doe");
    }

    @PostMapping("/employees")
    String newEmployee(@RequestBody String newEmployee) {
        return newEmployee;
    }

    @GetMapping("/employees/{id}")
    String one(@PathVariable Long id) {

        return "Matt";
    }

    @PutMapping("/employees/{id}")
    String replaceEmployee(@RequestBody String newEmployee, @PathVariable Long id) {

        return "Updated";
    }

    @DeleteMapping("/employees/{id}")
    void deleteEmployee(@PathVariable Long id) {

    }
}
