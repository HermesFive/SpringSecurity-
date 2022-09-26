"# SpringSecurity-" 
### 发送验证码的逻辑
- 拿到手机号，验证该手机号是否已经注册
- 没有注册的手机号，我们随机生成一个6位数的验证码，将手机号作为key连同验证码一起存入Redis，并设置过期时间(60s)
  ```java
    //生成6位验证码
        String code = RandomUtil.randomNumbers(6);

        //调用短信服务发送短信

        //发送成功，将验证码保存至Redis,设置失效时间60s
        String key = RedisKeyConstant.verify_code.getKey() + phone;
        redisTemplate.opsForValue().set(key,code,60, TimeUnit.SECONDS);
    ```
- 调用短信服务发送验证码
- 验证用户注册时输入的手机号和验证码：用手机号作为key去Redis中查找验证码，然后将其与用户输入的验证码进行比对。
```java
String code = redisTemplate.opsForValue().get(key);
```
