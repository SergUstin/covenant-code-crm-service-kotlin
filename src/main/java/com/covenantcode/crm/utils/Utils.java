package com.covenantcode.crm.utils;

import com.covenantcode.crm.entity.User;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;

@UtilityClass
public class Utils {

    public Long extractId(Authentication authentication){
        return ((User) authentication.getPrincipal()).getId();
    }
}