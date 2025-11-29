package ru.bsh.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bsh.GuaranteeSenderProxyImpl;
import ru.bsh.dto.Request;

@Service
@RequiredArgsConstructor
public class TestService {

    private final GuaranteeSenderProxyImpl<Request> proxy;

    public void test() {
        proxy.send(new Request());
    }
}
