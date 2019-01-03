package com.pinyougou.cart.service.impl;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.List;

public class UserDetailServiceImpl implements UserDetailsService {
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        //获得角色权限集合
        List<GrantedAuthority> authorities = new ArrayList<>();
        //设置一个角色;正式的话。可以再根据用户名查询该用户的角色列表
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        //因为整合了cas,所以认证的工作交由cas进行，密码可以设置位空
        //该认证授权类再整合了CAS之后只剩授权角色集合的查询价值。
        return new User(username,"",authorities);
    }
}
