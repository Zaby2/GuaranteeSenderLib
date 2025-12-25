package ru.bsh.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.bsh.dto.Request;

@RestController
@Slf4j
public class TestController {

    @PostMapping("/test")
    public ResponseEntity<Object> test(@RequestBody Request request) {
        log.info("Получен id = {}", request.getId());
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @PostMapping("/test/2")
    public ResponseEntity<Object> test2(@RequestBody Request request) {
        log.info("Получен id = {}", request.getId());
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
