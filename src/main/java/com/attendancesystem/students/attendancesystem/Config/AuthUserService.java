package com.attendancesystem.students.attendancesystem.Config;

import com.attendancesystem.students.attendancesystem.Service.SharedService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;
@Service
@AllArgsConstructor
public class AuthUserService implements UserDetailsService {

    @Autowired
    private SharedService sharedService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        com.attendancesystem.students.attendancesystem.Model.UserDetails user = sharedService.getUserByUserEmail(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        else {
            return new User(
                    user.getUserEmail(),
                    user.getPassword(),
                    user.getRole().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())
            );
        }
    }
}
