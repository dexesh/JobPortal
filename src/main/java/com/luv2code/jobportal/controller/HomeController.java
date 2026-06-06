package com.luv2code.jobportal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
   @GetMapping("/")
    public String home() {
       return "index";
    }
    @GetMapping("/instance")
public String instance() {
    return System.getenv("HOSTNAME");
}
}
