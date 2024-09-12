package org.example.expert.domain.auth.service;

import org.example.expert.config.JwtUtil;
import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.auth.dto.request.SigninRequest;
import org.example.expert.domain.auth.dto.request.SignupRequest;
import org.example.expert.domain.auth.dto.response.SigninResponse;
import org.example.expert.domain.auth.exception.AuthException;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @InjectMocks
    private AuthService authService;

    @Nested
    class Signup {

        @Test
        void 이미_존재하는_이메일() {
            // given
            SignupRequest signupRequest
                    = new SignupRequest("123@naver.com", "1234", "ADMIN");

            given(userRepository.existsByEmail(anyString())).willReturn(true);

            // when
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
                authService.signup(signupRequest);
            });

            // then
            assertEquals("이미 존재하는 이메일입니다.", exception.getMessage());
        }

        @Test
        void 회원가입_성공() {
            // given
            Long userId = 1L;
            SignupRequest signupRequest = new SignupRequest("123@naver.com", "1234", "ADMIN");
            User user = new User(signupRequest.getEmail(), "encodedPassword", UserRole.ADMIN);
            ReflectionTestUtils.setField(user, "id", userId);

            // 스터빙
            given(userRepository.existsByEmail(anyString())).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(user);
            given(jwtUtil.createToken(anyLong(), anyString(), any(UserRole.class))).willReturn("dummyToken");

            // when
            authService.signup(signupRequest);

            // then
            verify(userRepository, times(1)).existsByEmail(anyString());
            verify(passwordEncoder, times(1)).encode(anyString());
            verify(userRepository, times(1)).save(any(User.class));  // save 호출 시 어떤 User든 검증
        }
    }

    @Nested
    class SignIn {

        @Test
        void 가입되지_않은_유저() {
            // given
            SigninRequest signupRequest = new SigninRequest("123@naver.com", "1234");

            given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());
            // when

            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
                authService.signin(signupRequest);
            });

            // then
            assertEquals("가입되지 않은 유저입니다.", exception.getMessage());
        }

        @Test
        void 로그인_비밀번호_불일치() {
            // given
            SigninRequest signupRequest = new SigninRequest("123@naver.com", "1234");
            User user = new User(signupRequest.getEmail(), "encodedPassword", UserRole.ADMIN);
            given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

            // then
            AuthException authException = assertThrows(AuthException.class, () -> {
                authService.signin(signupRequest);
            });

            // when
            assertEquals("잘못된 비밀번호입니다.", authException.getMessage());
        }

        @Test
        void 로그인_성공() {
            // given
            SigninRequest signupRequest = new SigninRequest("123@naver.com", "1234");
            User user = new User(signupRequest.getEmail(), "encodedPassword", UserRole.ADMIN);

            given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
            given(jwtUtil.createToken(user.getId(), user.getEmail(), user.getUserRole())).willReturn("dummyToken");
            // then
            SigninResponse signin = authService.signin(signupRequest);

            // when
            assertEquals("dummyToken", signin.getBearerToken());
        }
    }
}