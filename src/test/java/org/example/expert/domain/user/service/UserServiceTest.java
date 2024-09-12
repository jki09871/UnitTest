package org.example.expert.domain.user.service;

import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.dto.request.UserChangePasswordRequest;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.example.expert.domain.user.service.UserService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Spy
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Nested
    class GetUsers {
        @Test
        public void 유저_찾기_실패() {
            // given
            given(userRepository.findById(anyLong())).willReturn(Optional.empty());

            // when
            InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                    () -> {
                        userService.getUser(anyLong());
                    });

            // then
            assertEquals("User not found", exception.getMessage());

        }

        @Test
        public void 유저_찾기_성공() {
            // given
            Long userId = 1L;
            User user = new User("alden200@gmail.com", "1234", UserRole.USER);
            ReflectionTestUtils.setField(user, "id", userId);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when
            UserResponse response = userService.getUser(userId);

            // then
            assertNotNull(response);
            assertEquals(userId, response.getId());
            assertEquals("alden200@gmail.com", response.getEmail());

        }
    }

    @Nested
    class ChangePassword {

        @Test
        public void 비밀번호_변경() {
            // given
            User user = new User("alden200@gmail.com", "Abcdefgh1!", UserRole.USER);
            UserChangePasswordRequest userChangePasswordRequest = new UserChangePasswordRequest("Abcdefgh1!", "Abcdefgh1@");

            given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(userChangePasswordRequest.getNewPassword(), user.getPassword())).willReturn(false);  // 새 비밀번호가 다름
            given(passwordEncoder.matches(userChangePasswordRequest.getOldPassword(), user.getPassword())).willReturn(true);   // 기존 비밀번호가 맞음
            given(passwordEncoder.encode(userChangePasswordRequest.getNewPassword())).willReturn("encodedNewPassword");
            // when

            userService.changePassword(anyLong(), userChangePasswordRequest);

            // then
            verify(userRepository, times(1)).findById(anyLong());
        }

        @Test
        public void 비밀번호_변경시_유저_찾기_실패() {
            // given
            User user = new User("alden200@gmail.com", "Abcdefgh1!", UserRole.USER);
            UserChangePasswordRequest userChangePasswordRequest = new UserChangePasswordRequest("Abcdefgh1!", "Abcdefgh1@");

            given(userRepository.findById(anyLong())).willReturn(Optional.empty());
            // when

            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
            {
                userService.changePassword(anyLong(), userChangePasswordRequest);
            });
            // then

            assertEquals("User not found", exception.getMessage());
        }

        @Test
        public void 비밀번호_변경시_기존_비밀번호와_동일() {
            // given
            User user = new User("alden200@gmail.com", "Abcdefgh1!", UserRole.USER);
            UserChangePasswordRequest userChangePasswordRequest = new UserChangePasswordRequest("Abcdefgh1!", "Abcdefgh1@");
            given(passwordEncoder.matches(userChangePasswordRequest.getNewPassword(), user.getPassword())).willReturn(true);
            given(userRepository.findById(anyLong())).willReturn(Optional.of(user));

            // when
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
            {
                userService.changePassword(anyLong(), userChangePasswordRequest);
            });
            // then

            assertEquals("새 비밀번호는 기존 비밀번호와 같을 수 없습니다.", exception.getMessage());
        }

        @Test
        public void 비밀번호_변경시_기존_비밀번호_검증_실패() {
            // given
            User user = new User("alden200@gmail.com", "Abcdefgh1!", UserRole.USER);
            UserChangePasswordRequest userChangePasswordRequest = new UserChangePasswordRequest("Abcdefgh1!", "Abcdefgh1@");
            given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(userChangePasswordRequest.getNewPassword(), user.getPassword())).willReturn(false);
            given(passwordEncoder.matches(userChangePasswordRequest.getOldPassword(), user.getPassword())).willReturn(false);

            // when
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
            {
                userService.changePassword(anyLong(), userChangePasswordRequest);
            });
            // then

            assertEquals("잘못된 비밀번호입니다.", exception.getMessage());
        }

    }

    @Nested
    class passwordValidation {
        // private Method 테스트는 일반적으로 권장되지 않지만 할 수는 있음
        @Test
        public void 비밀번호_유효성검사_성공() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            // given
            UserChangePasswordRequest userChangePasswordRequest = new UserChangePasswordRequest("Abcdefgh1!", "Abcdefgh1@");  // 유효하지 않은 비밀번호

            // 리플렉션을 통해 프라이빗 메서드에 접근
            Method method = UserService.class.getDeclaredMethod("passwordValidation", UserChangePasswordRequest.class);
            method.setAccessible(true);  // 프라이빗 메서드 접근 가능하게 설정

            // when&then
            method.invoke(userService, userChangePasswordRequest);  // 올바른 userService 인스턴스에서 메서드 호출


        }

        @Test
        public void 비밀번호_유효성검사_실패() throws NoSuchMethodException {
            // given
            UserChangePasswordRequest userChangePasswordRequest = new UserChangePasswordRequest("Abcdefgh1!", "short");  // 유효하지 않은 비밀번호

            // 리플렉션을 통해 프라이빗 메서드에 접근
            Method method = UserService.class.getDeclaredMethod("passwordValidation", UserChangePasswordRequest.class);
            method.setAccessible(true);  // 프라이빗 메서드 접근 가능하게 설정

            // when & then: 비밀번호가 유효하지 않으면 예외 발생
            InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> {
                method.invoke(userService, userChangePasswordRequest);  // 올바른 userService 인스턴스에서 메서드 호출
            });

            // then: 실제 예외는 InvocationTargetException 내부에 있기 때문에 getCause()로 추출
            Throwable targetException = exception.getCause();
            assertEquals(InvalidRequestException.class, targetException.getClass());
            assertEquals("새 비밀번호는 8자 이상이어야 하고, 숫자와 대문자를 포함해야 합니다.", targetException.getMessage());
        }
    }
}
