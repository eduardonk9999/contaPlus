package com.contaplus.api.security;

import com.contaplus.api.user.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StoreAuthorizationService {

    public void validateStoreAccess(UUID storeId) {
        User user = getCurrentUser();
        if (user == null) {
            throw new UnauthorizedStoreAccessException("Usuário não autenticado");
        }

        if (!user.getStore().getId().equals(storeId)) {
            throw new UnauthorizedStoreAccessException(
                "Acesso negado: você não tem permissão para acessar dados desta loja"
            );
        }
    }

    public UUID getCurrentUserStoreId() {
        User user = getCurrentUser();
        if (user == null) {
            throw new UnauthorizedStoreAccessException("Usuário não autenticado");
        }
        return user.getStore().getId();
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            return (User) auth.getPrincipal();
        }
        return null;
    }

    public boolean isOwner() {
        User user = getCurrentUser();
        return user != null && user.getRole().name().equals("OWNER");
    }

    public boolean isEmployee() {
        User user = getCurrentUser();
        return user != null && user.getRole().name().equals("EMPLOYEE");
    }
}
