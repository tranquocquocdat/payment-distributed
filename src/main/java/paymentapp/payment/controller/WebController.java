package paymentapp.payment.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class WebController {

    // Serve UI at root of context path
    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }

    // Dashboard alias
    @GetMapping("/dashboard")
    public String dashboard() {
        return "forward:/index.html";
    }

    // UI routes - forward to index.html for SPA behavior
    @GetMapping("/ui")
    public String ui() {
        return "forward:/index.html";
    }

    // Handle any other routes that should show UI
    @GetMapping("/app")
    public String app() {
        return "forward:/index.html";
    }
}