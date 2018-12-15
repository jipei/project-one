package com.pinyougou.shop.service.impl;

import com.pinyougou.pojo.TbSeller;
import com.pinyougou.sellergoods.service.SellerService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.List;

/**
 * 动态认证授权类：可以根据用户输入的用户名进行认证和查询其他角色权限
 */
public class UserDetailServiceImpl implements UserDetailsService {
    //@Reference
    private SellerService sellerService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        TbSeller seller = sellerService.findOne(username);

        //如果商家存在并且审核通过才返回user
        if (seller!=null&&"1".equals(seller.getStatus())){
            //获取角色权限集合
            List<GrantedAuthority> authorities= new ArrayList<>();
            //设置一个角色：正式的话：可以再根据用户名查询该用户的角色列表
            authorities.add(new SimpleGrantedAuthority("ROLE_SELLER"));
            return new User(username,seller.getPassword(),authorities);
        }
       return null;
    }

    public void setSellerService(SellerService sellerService) {
        this.sellerService=sellerService;
    }
}
