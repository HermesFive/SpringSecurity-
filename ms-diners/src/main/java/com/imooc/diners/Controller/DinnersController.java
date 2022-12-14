package com.imooc.diners.Controller;

import com.imooc.commons.model.domain.ResultInfo;
import com.imooc.commons.model.dto.DinersDTO;
import com.imooc.commons.model.vo.ShortDinerInfo;
import com.imooc.commons.utils.ResultInfoUtil;
import com.imooc.diners.service.DinnersService;
import io.swagger.annotations.Api;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@Api(tags = "食客相关的接口")
public class DinnersController {

    @Resource
    private DinnersService dinnersService;

    @Resource
    private HttpServletRequest request;

    /**
     * 根据 ids 查询食客信息
     *
     * @param ids
     * @return
     */
    @GetMapping("findByIds")
    public ResultInfo<List<ShortDinerInfo>> findByIds(String ids) {
        List<ShortDinerInfo> dinerInfos = dinnersService.findByIds(ids);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), dinerInfos);
    }

    /**
     * 登录
     * @param account
     * @param password
     * @return
     */
    @GetMapping("signin")
    public ResultInfo signIn(String account, String password){
        return dinnersService.signIn(account,password, request.getServletPath());
    }

    /**
     * 校验手机号是否注册
     * @param phone
     * @return
     */
    @GetMapping("checkPhone")
    public ResultInfo checkPhone(String phone){
        dinnersService.checkPhoneIsRegistered(phone);
        return ResultInfoUtil.buildSuccess(request.getServletPath());
    }

    /**
     * 注册
     * @param dinersDTO
     * @return
     */
    @PostMapping("register")
    public ResultInfo register(@RequestBody DinersDTO dinersDTO){
        return dinnersService.register(dinersDTO, request.getServletPath());
    }
}
