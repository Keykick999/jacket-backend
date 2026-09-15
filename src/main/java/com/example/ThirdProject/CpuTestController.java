package com.example.ThirdProject;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class CpuTestController {

    @GetMapping("/cpu")
    public String cpu() {
        long result = 0;

        for (long i = 0; i < 50_000_000_000L; i++) {
            result += i * i;
        }

        return String.valueOf(result);
    }
}