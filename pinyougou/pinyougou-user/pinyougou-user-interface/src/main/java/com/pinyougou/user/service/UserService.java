package com.pinyougou.user.service;

import com.pinyougou.pojo.TbUser;
import com.pinyougou.service.BaseService;
import com.pinyougou.vo.PageResult;

public interface UserService extends BaseService<TbUser> {

    PageResult search(Integer page, Integer rows, TbUser user);

    /**
     * 发送短信验证码
     * @param phone 手机号
     * @return
     */
    void sendSmsCode(String phone);

    /**
     * 校验验证码是否存在
     * @param phone 手机号
     * @param smsCode 验证码
     * @return true or false
     */
    boolean checkSmsCode(String phone, String smsCode);
}