package com.imooc.diners.Controller;

import com.imooc.commons.model.domain.ResultInfo;
import com.imooc.commons.utils.ResultInfoUtil;
import com.imooc.diners.service.SignService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("sign")
public class SignController {

    @Resource
    private SignService signService;

    @Resource
    private HttpServletRequest request;

    @GetMapping("count")
    public ResultInfo getSignCount(String access_token, String date){
        Long count   = signService.getTotalCount(access_token,date);
        return ResultInfoUtil.buildSuccess(request.getServletPath(),count);
    }

    @PostMapping
    public ResultInfo sign(String accessToken, @RequestParam(required = false) String date){
        int count = signService.doSign(accessToken,date);

        return ResultInfoUtil.buildSuccess(request.getServletPath(),count);
    }

}
