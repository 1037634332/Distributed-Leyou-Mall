package com.leyou.user.service.impl;

import com.leyou.user.mapper.UserMapper;
import com.leyou.user.pojo.User;
import com.leyou.user.service.UserService;
import com.leyou.utils.CodecUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;


/**
 * @Author: 98050
 * @Time: 2018-10-21 18:42
 * @Feature:
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String KEY_PREFIX = "user:code:phone";

    private Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Override
    public Boolean checkData(String data, Integer type) {
        User user = new User();
        switch (type){
            case 1 :
                user.setUsername(data);
                break;
            case 2 :
                user.setPhone(data);
                break;
            default:
                return null;
        }
        return this.userMapper.selectCount(user) == 0;
    }


    @Override
    public Boolean register(User user) {
        String key = KEY_PREFIX + user.getPhone();
        user.setId(null);
        user.setCreated(new Date());
        String salt = CodecUtils.generateSalt();
        user.setSalt(salt);
        //3.密码加密
        user.setPassword(CodecUtils.md5Hex(user.getPassword(),salt));
        //4.写入数据库
        boolean result = this.userMapper.insertSelective(user) == 1;
        return result;
    }

    /**
     * 用户验证
     * @param username
     * @param password
     * @return
     */
    @Override
    public User queryUser(String username, String password) {
        //1.查询
        User record = new User();
        record.setUsername(username);
        User user = this.userMapper.selectOne(record);

        //2.校验用户名
        if (user == null){
            return null;
        }
        //3. 校验密码
        String result = CodecUtils.md5Hex(password,user.getSalt());
        if (StringUtils.equals(result,user.getPassword())){
            return user;
        }

        //4.用户名密码都正确
        return null;
    }
}
