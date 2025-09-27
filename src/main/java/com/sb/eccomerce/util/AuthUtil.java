package com.sb.eccomerce.util;

import com.sb.eccomerce.model.User;
import com.sb.eccomerce.repositries.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class AuthUtil {

    @Autowired
    UserRepository userRepository;

    public String loggedInEmail(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findUserByUsername(authentication.getName()).orElseThrow(()-> new UsernameNotFoundException("User Not Found with username: "+authentication.getName()));
        return user.getEmail();
    }

    public Long loggedInUserId(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findUserByUsername(authentication.getName()).orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: "+authentication.getName()));
        return user.getUserId();
    }

    public User loggedInUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findUserByUsername(authentication.getName()).orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: "+authentication.getName()));
        return user;
    }
}
