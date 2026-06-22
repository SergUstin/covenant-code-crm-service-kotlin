package com.covenantcode.crm.service.impl;

import ch.qos.logback.core.status.Status;
import com.covenantcode.crm.dto.auth.AuthResponse;
import com.covenantcode.crm.dto.auth.LoginRequest;
import com.covenantcode.crm.dto.auth.RegisterRequest;
import com.covenantcode.crm.entity.Role;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.exception.ConflictException;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.exception.UnauthorizedException;
import com.covenantcode.crm.repository.RoleRepository;
import com.covenantcode.crm.repository.UserRepository;
import com.covenantcode.crm.security.JwtService;
import org.hibernate.type.descriptor.jdbc.NVarcharJdbcType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest request;

    @BeforeEach
    void setUp() {
        request = new RegisterRequest(
                "Ivan",
                "Ivanov",
                "ivan@example.com",
                "password123",
                "+79991234567"
        );
    }

    @Test
    public void register_success() {
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);

        Role role = new Role();
        role.setName(RoleName.MANAGER);
        when(roleRepository.findByName(RoleName.MANAGER)).thenReturn(Optional.of(role));

        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setFirstName(request.getFirstName());
        savedUser.setLastName(request.getLastName());
        savedUser.setEmail(request.getEmail());
        savedUser.setPassword("encoded-password");
        savedUser.setRole(role);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("mock.token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mock.token", response.getToken());
        assertEquals("MANAGER", response.getRole());

        verify(passwordEncoder).encode(request.getPassword());
        verify(userRepository).save(any(User.class));
        verify(jwtService).generateToken(any(User.class));
    }

    @Test
    public void register_emailAlreadyExists() {
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(roleRepository, passwordEncoder, jwtService);
    }

    @Test
    public void register_managerRoleNotFound() {
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleName.MANAGER)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
        verify(jwtService, never()).generateToken(any());
    }

    //мои тесты
    @Test
    @DisplayName("Успешный вход")
    void testLoginSuccess() {
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");
        String token = "mock.token";

        // mok пользователя из БД
        User mockBuild = User.builder()
                .id(1L)
                .firstName("Иван")
                .lastName("Иванов")
                .email(loginRequest.getEmail())
                .password("{bcrypt}$2a$10$hashedPassword")
                .role(Role.builder().name(RoleName.MANAGER).build())
                .enabled(true)
                .build();

        //mok аутентификации
        UserDetails mockManagerDetails = org.springframework.security.core.userdetails.User.builder()
                .username(loginRequest.getEmail())
                .password("{bcrypt}$2a$10$hashedPassword")
                .roles("MANAGER")
                .build();

        Authentication mockAuth = new UsernamePasswordAuthenticationToken(mockManagerDetails,
                null, mockManagerDetails.getAuthorities());

        //Настройка моков
        when(userRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(mockBuild));

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);

        when(jwtService.generateToken(mockBuild))
                .thenReturn(token);

        AuthResponse responseLogin = authService.login(loginRequest);

        //assert
        assertEquals(token, responseLogin.getToken());
        assertEquals(1L, responseLogin.getUserId());
        assertEquals("test@example.com", responseLogin.getEmail());
        assertEquals("Иван", responseLogin.getFirstName());
        assertEquals("Иванов", responseLogin.getLastName());
        assertEquals("MANAGER", responseLogin.getRole());

        //проверка
        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateToken(mockBuild);


    }

//    @Test
//    @DisplayName("Неверный пароль")
//    void testLoginWrongPasswordThrowsUnauthorizedException() {
//        // Подготовка
//        LoginRequest request = new LoginRequest("test@example.com",
//                "wrongPassword");
//
//        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
//                .thenThrow(new BadCredentialsException("Bad credentials"));
//
//        UnauthorizedException unauthorizedException = assertThrows(UnauthorizedException.class, () ->
//                authService.login(request));
//
//        assertEquals("Неверный email или пароль", unauthorizedException.getMessage());
//        verify(userRepository, never()).findByEmail(any()); // НЕ должен вызываться
//    }

    @Test
    void testLoginWithWrongCredentialsShouldThrowUnauthorizedExceptionAndNotCallUserRepository() {
        // Arrange: Готовим неверные данные для логина
        String wrongEmail = "wrong@example.com";
        String wrongPassword = "wrongPassword";
        LoginRequest request = new LoginRequest(wrongEmail, wrongPassword);

        // Настраиваем mock для AuthenticationManager, чтобы он бросал исключение при неверных данных
        doThrow(new AuthenticationException("Bad credentials") {})
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        // Act & Assert: Проверяем, что бросается UnauthorizedException и не вызывается findByEmail()
        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(request)
        );

        // Проверяем сообщение об ошибке
        assert exception.getMessage().equals("Неверный email или пароль");

        // Проверяем, что метод findByEmail() НЕ был вызван (так как аутентификация провалилась)
        verify(userRepository, never()).findByEmail(any());
    }

}