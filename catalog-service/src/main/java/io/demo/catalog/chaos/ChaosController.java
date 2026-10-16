package io.demo.catalog.chaos;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/chaos")
public class ChaosController {

    private final ChaosState chaos;

    public ChaosController(ChaosState chaos) {
        this.chaos = chaos;
    }

    @GetMapping
    public Status status() {
        return new Status(chaos.latencyMs(), chaos.errorRate());
    }

    @PostMapping
    public Status set(@RequestParam(defaultValue = "0") long latencyMs,
                      @RequestParam(defaultValue = "0") double errorRate) {
        chaos.set(latencyMs, errorRate);
        return status();
    }

    @PostMapping("/reset")
    public Status reset() {
        chaos.reset();
        return status();
    }

    public record Status(long latencyMs, double errorRate) {
    }
}
