package top.codelong.service.impl;

import org.springframework.stereotype.Service;
import top.codelong.findsdk.annotation.ApiInterface;
import top.codelong.findsdk.annotation.ApiMethod;
import top.codelong.findsdk.enums.HttpTypeEnum;
import top.codelong.service.TestService;

@ApiInterface(interfaceName = "测试接口")
@Service
public class TestServiceImpl implements TestService {
    @ApiMethod(url = "/test")
    public String test(String name) {
        return "1  " + name;
    }

    @ApiMethod(isAuth = 1, isHttp = 1, httpType = HttpTypeEnum.GET, url = "/test2")
    public String test2(String name) {
        return name;
    }
}
