package com.contaplus.api.auth;

import com.contaplus.api.security.JwtService;
import com.contaplus.api.store.Store;
import com.contaplus.api.store.StoreRepository;
import com.contaplus.api.user.User;
import com.contaplus.api.user.UserRepository;
import com.contaplus.api.user.UserRole;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final GoogleIdTokenVerifier googleVerifier;

    public AuthService(
            UserRepository userRepository,
            StoreRepository storeRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            @Value("${spring.security.oauth2.client.registration.google.client-id:}") String googleClientId
    ) {
        this.userRepository = userRepository;
        this.storeRepository = storeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;

        if (googleClientId != null && !googleClientId.isBlank()) {
            this.googleVerifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();
        } else {
            this.googleVerifier = null;
        }
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email já cadastrado: " + request.email());
        }

        Store store = new Store(request.storeName());
        storeRepository.save(store);

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name(),
                request.cnpj(),
                UserRole.OWNER,
                store
        );
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, toUserResponse(user));
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Credenciais inválidas"));

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, toUserResponse(user));
    }

    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        if (googleVerifier == null) {
            throw new GoogleAuthNotConfiguredException("Autenticação Google não configurada");
        }

        GoogleIdToken idToken;
        try {
            idToken = googleVerifier.verify(request.idToken());
        } catch (GeneralSecurityException | IOException e) {
            throw new InvalidGoogleTokenException("Token Google inválido");
        }

        if (idToken == null) {
            throw new InvalidGoogleTokenException("Token Google inválido");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String googleId = payload.getSubject();
        String name = (String) payload.get("name");

        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            if (user.getGoogleId() == null) {
                user.linkGoogleAccount(googleId);
                userRepository.save(user);
            }
        } else {
            String storeName = name != null ? "Loja de " + name : "Minha Loja";
            Store store = new Store(storeName);
            storeRepository.save(store);

            user = User.createWithGoogle(email, name != null ? name : email, googleId, store);
            userRepository.save(user);
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, toUserResponse(user));
    }

    public UserResponse getCurrentUser(User user) {
        return toUserResponse(user);
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getCnpj(),
                user.getRole().name(),
                user.getStore().getId(),
                user.getStore().getName()
        );
    }
}
