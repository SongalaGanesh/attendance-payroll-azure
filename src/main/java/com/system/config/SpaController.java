package com.system.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping("/")
    public String home() {
        return "forward:/index.html";
    }

    @GetMapping("/login")
    public String login() {
        return "forward:/index.html";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "forward:/index.html";
    }

    @GetMapping("/dashboard/summary")
    public String summary() {
        return "forward:/index.html";
    }

    @GetMapping("/dashboard/employees")
    public String employees() {
        return "forward:/index.html";
    }

    @GetMapping("/dashboard/attendance")
    public String attendance() {
        return "forward:/index.html";
    }

    @GetMapping("/dashboard/leaves")
    public String leaves() {
        return "forward:/index.html";
    }

    @GetMapping("/dashboard/payroll")
    public String payroll() {
        return "forward:/index.html";
    }
}