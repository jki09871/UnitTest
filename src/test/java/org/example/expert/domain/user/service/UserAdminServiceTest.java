package org.example.expert.domain.user.service;

import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.dto.request.UserRoleChangeRequest;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith({MockitoExtension.class})
class UserAdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserAdminService userAdminService;


    @Test
    void 사용자_권한_변경_성공() {
        // given
        User user = new User("alden200@gamil.com", "1234", UserRole.USER);
        UserRoleChangeRequest changeRequest = new UserRoleChangeRequest("ADMIN");
        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        System.out.println("user = " + user.getUserRole());

        // when
        userAdminService.changeUserRole(anyLong(), changeRequest);

        // then
        verify(userRepository, times(1)).findById(anyLong());
        assertEquals(UserRole.ADMIN, user.getUserRole());  // 역할이 ADMIN으로 업데이트되었는지 확인
        System.out.println("user = " + user.getUserRole());

    }


    @Test
    void 사용자_권한_변경_실패() {
        // given
        User user = new User("alden200@gamil.com", "1234", UserRole.USER);
        UserRoleChangeRequest changeRequest = new UserRoleChangeRequest("ADMIN");
        given(userRepository.findById(anyLong())).willReturn(Optional.empty());
        System.out.println("user = " + user.getUserRole());

        // when
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
            userAdminService.changeUserRole(anyLong(), changeRequest);
        });

        // then
        assertEquals("User not found", exception.getMessage());
    }
}