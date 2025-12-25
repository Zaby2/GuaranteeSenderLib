package ru.bsh.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.bsh.service.TestService;

@RestController
@RequiredArgsConstructor
public class ClientTestController {

    private final TestService testService;

    @GetMapping("client/test")
    public void test() {
        testService.test();
    }
}
