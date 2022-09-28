package com.imooc.diners.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import com.imooc.commons.constant.ApiConstant;
import com.imooc.commons.exception.ParameterException;
import com.imooc.commons.model.domain.ResultInfo;
import com.imooc.commons.model.vo.SignInDinerInfo;
import com.imooc.commons.utils.AssertUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 签到业务逻辑层
 */
@Service
public class SignService {

    @Value("${service.name.ms-oauth-server}")
    private String oauthServerName;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private RestTemplate restTemplate;

    /**
     * 获取当月签到情况
     *
     * @param accessToken
     * @param dateStr
     * @return
     */
    public Map<String, Boolean> getSignInfo(String accessToken, String dateStr) {
        // 获取登录用户信息
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        // 获取日期
        Date date = getDate(dateStr);
        // 构建 Key
        String signKey = buildSignKey(dinerInfo.getId(), date);
        // 构建一个自动排序的 Map
        Map<String, Boolean> signInfo = new TreeMap<>();
        // 获取某月的总天数（考虑闰年）
        int dayOfMonth = DateUtil.lengthOfMonth(DateUtil.month(date) + 1,
                DateUtil.isLeapYear(DateUtil.dayOfYear(date)));
        // bitfield user:sign:5:202011 u30 0
        BitFieldSubCommands bitFieldSubCommands = BitFieldSubCommands.create()
                .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                .valueAt(0);
        List<Long> list = redisTemplate.opsForValue().bitField(signKey, bitFieldSubCommands);
        if (list == null || list.isEmpty()) {
            return signInfo;
        }
        long v = list.get(0) == null ? 0 : list.get(0);
        // 从低位到高位进行遍历，为 0 表示未签到，为 1 表示已签到
        for (int i = dayOfMonth; i > 0; i--) {
            /*
                签到：  yyyy-MM-01 true
                未签到：yyyy-MM-01 false
             */
            LocalDateTime localDateTime = LocalDateTimeUtil.of(date).withDayOfMonth(i);
            boolean flag = v >> 1 << 1 != v;
            signInfo.put(DateUtil.format(localDateTime, "yyyy-MM-dd"), flag);
            v >>= 1;
        }
        return signInfo;
    }

    /**
     * 获取用户当月签到总次数
     * @param accessToken
     * @param dateStr
     * @return
     */
    public long getTotalCount(String accessToken, String dateStr){
        //获取用户登录信息
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        //获取日期
        Date date = getDate(dateStr);
        //构建key
        String key = buildSignKey(dinerInfo.getId(),date);
        // e.g. BITCOUNT user:sign:5:202011
        return (Long) redisTemplate.execute(
                (RedisCallback<Long>) con -> con.bitCount(key.getBytes())
        );

    }

    /**
     * 用户签到
     * @param accessToken
     * @param dataStr
     * @return
     */
    public int doSign(String accessToken, String dataStr){
        //获取用户登录信息
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);

        //获取日期
        Date date = getDate(dataStr);

        //获取日期对应一个月中的多少号，是第几天
        int offset = DateUtil.dayOfMonth(date)-1;
        //构建bitmap的key "user:sign:5:202209"
        String signKey = buildSignKey(dinerInfo.getId(),date);
        //查看是否已经签到
        boolean isSigned = redisTemplate.opsForValue().getBit(signKey,offset);
        AssertUtil.isTrue(isSigned,"当前日期已签到");
        //签到
        redisTemplate.opsForValue().setBit(signKey,offset,true);
        //统计一下连续签到的次数
        int count  = getSignCount(dinerInfo.getId(),date);
        return count;
    }

    /**
     * 统计当月连续签到的次数
     * @param id
     * @param date
     * @return
     */
    private int getSignCount(Integer id, Date date) {
        int dayOfMonth = DateUtil.dayOfMonth(date);
        String key = buildSignKey(id,date);
        //bitfield user:sign:5:202209 get u31 0
        BitFieldSubCommands bitFieldSubCommands = BitFieldSubCommands.create()
                .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                .valueAt(0);

        List<Long> list = redisTemplate.opsForValue().bitField(key,bitFieldSubCommands);
        if (list == null || list.isEmpty()){
            return 0;
        }
        int signCount = 0;
        long v = list.get(0) == null ? 0:list.get(0);
        for (int i = dayOfMonth;i>0;i--){
            //右移再左移若等于自己，则说明最低位为0
            if (v>>1<<1 == v){
                if (i!=dayOfMonth){
                    break;
                }
            }else{
                signCount++;
            }
            v = v>>1;
        }
        return signCount;
    }



    /**
     * 根据日期构建Redis中存储的key
     * @param id
     * @param date
     * @return "user:sign:5:202209"的形式
     */
    private String buildSignKey(Integer id, Date date) {
        return String.format("user:sign:%d:%s",id,DateUtil.format(date,"yyyyMM"));
    }

    /**
     * 根据字符串获取日期
     * @param dataStr
     * @return
     */
    private Date getDate(String dataStr) {
        if (StrUtil.isBlank(dataStr)){
            return new Date();
        }
        try {
            return DateUtil.parseDate(dataStr);
        }catch (Exception e){
            throw new ParameterException("请传入yyyy-MM-dd的日期格式");
        }

    }

    /**
     * 获取用户登录信息
     * @param accessToken
     * @return
     */
    public SignInDinerInfo loadSignInDinerInfo(String accessToken){
        //登录校验
        AssertUtil.mustLogin(accessToken);
        String url = oauthServerName + "user/me?access_token={accessToken}";
        ResultInfo resultInfo = restTemplate.getForObject(url,ResultInfo.class,accessToken);
        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE){
            throw new ParameterException(resultInfo.getMessage());
        }
        SignInDinerInfo dinerInfo = BeanUtil.fillBeanWithMap((LinkedHashMap) resultInfo.getData(),
                new SignInDinerInfo(), false);
        if (dinerInfo==null){
            throw new ParameterException(ApiConstant.NO_LOGIN_CODE,ApiConstant.NO_LOGIN_MESSAGE);
        }
        return dinerInfo;
    }
}
