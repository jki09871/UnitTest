package org.example.expert.domain.manager.service;

import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.manager.dto.request.ManagerSaveRequest;
import org.example.expert.domain.manager.dto.response.ManagerResponse;
import org.example.expert.domain.manager.dto.response.ManagerSaveResponse;
import org.example.expert.domain.manager.entity.Manager;
import org.example.expert.domain.manager.repository.ManagerRepository;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.example.expert.domain.user.service.UserService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ManagerServiceTest {

    @Mock
    private ManagerRepository managerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TodoRepository todoRepository;
    @InjectMocks
    private ManagerService managerService;

    @Nested
    class SaveManager {

        @Test
        void todo_찾기_실패() {
            // given
            long managerUserId = 2L;

            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId);
            given(todoRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    managerService.saveManager(authUser, anyLong(), managerSaveRequest)
            );

            assertEquals("Todo not found", exception.getMessage());
        }

        @Test
        void todo의_user가_null인_경우_예외가_발생한다() {
            // given
            long todoId = 1L;
            long managerUserId = 2L;

            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);

            Todo todo = new Todo();
            ReflectionTestUtils.setField(todo, "user", null);

            ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId);

            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    managerService.saveManager(authUser, todoId, managerSaveRequest)
            );

            assertEquals("담당자를 등록하려고 하는 유저가 일정을 만든 유저가 유효하지 않습니다.", exception.getMessage());
        }

        @Test
        void 작성자가_본인을_담당자로_지정했을때() {
            // given
            long managerUserId = 1L;

            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            User user = User.fromAuthUser(authUser);  // 일정을 만든 유저

            Todo todo = new Todo("Test Title", "Test Contents", "Sunny", user);

            User managerUser = new User("b@b.com", "password", UserRole.USER);  // 매니저로 등록할 유저
            ReflectionTestUtils.setField(managerUser, "id", managerUserId);

            ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId); // request dto 생성

            given(todoRepository.findById(anyLong())).willReturn(Optional.of(todo));
            given(userRepository.findById(anyLong())).willReturn(Optional.of(managerUser));

            // when & then: 유저와 매니저의 ID가 같을 때 InvalidRequestException 발생하는지 확인
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
                managerService.saveManager(authUser, anyLong(), managerSaveRequest);
            });

            assertEquals("일정 작성자는 본인을 담당자로 등록할 수 없습니다.", exception.getMessage());
        }


        @Test
        void todo_등록시_user_찾기_실패() {
            // given
            Long managerUserId = 1L;

            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            User user = User.fromAuthUser(authUser);  // 일정을 만든 유저
            Todo todo = new Todo("Test Title", "Test Contents", "Sunny", user);

            ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId); // request dto 생성
            given(todoRepository.findById(anyLong())).willReturn(Optional.of(todo));
            given(userRepository.findById(anyLong())).willReturn(Optional.empty());

            // when
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
                managerService.saveManager(authUser, anyLong(), managerSaveRequest);
            });

            // then
            assertEquals("등록하려고 하는 담당자 유저가 존재하지 않습니다.", exception.getMessage());
        }

        @Test
            // 테스트코드 샘플
        void todo가_정상적으로_등록된다() {
            // given
            long todoId = 1L;
            long managerUserId = 2L;

            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            User user = User.fromAuthUser(authUser);  // 일정을 만든 유저

            Todo todo = new Todo("Test Title", "Test Contents", "Sunny", user);

            User managerUser = new User("b@b.com", "password", UserRole.USER);  // 매니저로 등록할 유저
            ReflectionTestUtils.setField(managerUser, "id", managerUserId);

            ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId); // request dto 생성

            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
            given(userRepository.findById(managerUserId)).willReturn(Optional.of(managerUser));
            given(managerRepository.save(any(Manager.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            ManagerSaveResponse response = managerService.saveManager(authUser, todoId, managerSaveRequest);

            // then
            assertNotNull(response);
            assertEquals(managerUser.getId(), response.getUser().getId());
            assertEquals(managerUser.getEmail(), response.getUser().getEmail());
        }

    }

    @Nested
    class GetManager {

        @Test
        public void manager_목록_조회_시_Todo가_없다면_IRE_에러를_던진다() {
            // given
            long todoId = 1L;
            given(todoRepository.findById(todoId)).willReturn(Optional.empty());

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                    () -> managerService.getManagers(todoId));
            assertEquals("Todo not found", exception.getMessage());
        }

        @Test // 테스트코드 샘플
        public void manager_목록_조회에_성공한다() {
            // given
            long todoId = 1L;
            User user = new User("user1@example.com", "password", UserRole.USER);
            Todo todo = new Todo("Title", "Contents", "Sunny", user);
            ReflectionTestUtils.setField(todo, "id", todoId);

            Manager mockManager = new Manager(todo.getUser(), todo);
            List<Manager> managerList = List.of(mockManager);

            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
            given(managerRepository.findByTodoIdWithUser(todoId)).willReturn(managerList);

            // when
            List<ManagerResponse> managerResponses = managerService.getManagers(todoId);

            // then
            assertEquals(1, managerResponses.size());
            assertEquals(mockManager.getId(), managerResponses.get(0).getId());
            assertEquals(mockManager.getUser().getEmail(), managerResponses.get(0).getUser().getEmail());
        }
    }


    @Nested
    class DeleteManager {

        @Test
        void User_찾기_실패() {
            Long userId = 1L;
            Long todoId = 1L;
            Long managerId = 1L;

            // given
            given(userRepository.findById(anyLong())).willReturn(Optional.empty());

            // when
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
                managerService.deleteManager(userId, todoId, managerId);
            });

            // then
            assertEquals("User not found", exception.getMessage());
        }


        @Test
        void todo_찾기_실패() {
            Long userId = 1L;
            Long todoId = 1L;
            Long managerId = 1L;

            User user = new User();
            // given
            given(userRepository.findById(anyLong())).willReturn(Optional.of(user));

            // when
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
                managerService.deleteManager(userId, todoId, managerId);
            });

            // then
            assertEquals("Todo not found", exception.getMessage());
        }

        @Test
        void 일정_만든_유저_찾기_실패() {
            Long userId = 1L;
            Long todoId = 1L;
            Long managerId = 1L;

            User user = new User("123@naver.com", "123", UserRole.USER);
            Todo todo = new Todo("title", "contents", "Sunny", null);

            // given
            given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
            given(todoRepository.findById(anyLong())).willReturn(Optional.of(todo));

            // when
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
                managerService.deleteManager(userId, todoId, managerId);
            });

            // then
            assertEquals("해당 일정을 만든 유저가 유효하지 않습니다.", exception.getMessage());
        }

        @Test
        void UserId_todoUserId가_불일치() {
            Long userId = 1L;  // 요청 보낸 유저의 아이디
            Long todoId = 1L;  // 할 일 ID
            Long managerId = 1L;  // 매니저 ID

            User user = new User("a@a.com", "password", UserRole.USER);  // 요청한 유저
            ReflectionTestUtils.setField(user, "id", userId);  // 유저 ID 설정

            User differentUser = new User("b@b.com", "password", UserRole.USER);  // 다른 유저
            ReflectionTestUtils.setField(differentUser, "id", 2L);  // 다른 유저 ID 설정

            Todo todo = new Todo("Test Title", "Test Contents", "Sunny", differentUser);  // 다른 유저가 작성한 일정


            // given
            given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
            given(todoRepository.findById(anyLong())).willReturn(Optional.of(todo));

            // when
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
                managerService.deleteManager(userId, todoId, managerId);
            });

            // then
            assertEquals("해당 일정을 만든 유저가 유효하지 않습니다.", exception.getMessage());
        }

        @Test
        void todo_매니저_찾기_실패() {
            Long userId = 1L;
            Long todoId = 1L;
            Long managerId = 1L;

            User user = new User("123@naver.com", "123", UserRole.USER);
            Todo todo = new Todo("title", "contents", "Sunny", user);

            // given
            given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
            given(todoRepository.findById(anyLong())).willReturn(Optional.of(todo));
            given(managerRepository.findById(anyLong())).willReturn(Optional.empty());

            // when
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
                managerService.deleteManager(userId, todoId, managerId);
            });

            // then
            assertEquals("Manager not found", exception.getMessage());
        }


        @Test
        void 해당_일정에_등록된_담당자_불일치() {
            Long userId = 1L;
            Long todoId = 1L;
            Long managerId = 1L;

            // 유저 생성
            User user = new User("123@naver.com", "123", UserRole.USER);
            ReflectionTestUtils.setField(user, "id", userId);  // userId를 설정

            // 다른 ID를 가진 Todo 생성
            Todo todo = new Todo("title", "contents", "Sunny", user);
            ReflectionTestUtils.setField(todo, "id", todoId);

            Todo differentTodo = new Todo("title", "contents", "Sunny", user);
            ReflectionTestUtils.setField(todo, "id", 2L);

            Manager manager = new Manager(user, differentTodo);

            given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
            given(todoRepository.findById(anyLong())).willReturn(Optional.of(todo));
            given(managerRepository.findById(anyLong())).willReturn(Optional.of(manager));

            // when: 예외가 발생해야 하는 조건에서 deleteManager를 호출
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
                managerService.deleteManager(userId, todoId, managerId);
            });

            // then: 예외 메시지 확인
            assertEquals("해당 일정에 등록된 담당자가 아닙니다.", exception.getMessage());
        }

        @Test
        void 일정_삭제_성공() {
            Long userId = 1L;
            Long todoId = 1L;
            Long managerId = 1L;

            // 유저 생성
            User user = new User("123@naver.com", "123", UserRole.USER);
            ReflectionTestUtils.setField(user, "id", userId);  // userId를 설정

            // 다른 ID를 가진 Todo 생성
            Todo todo = new Todo("title", "contents", "Sunny", user);
            ReflectionTestUtils.setField(todo, "id", todoId);


            Manager manager = new Manager(user, todo);

            given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
            given(todoRepository.findById(anyLong())).willReturn(Optional.of(todo));
            given(managerRepository.findById(anyLong())).willReturn(Optional.of(manager));

            // when: 예외가 발생해야 하는 조건에서 deleteManager를 호출
            managerService.deleteManager(userId, todoId, managerId);


            // then: 예외 메시지 확인

            verify(userRepository, times(1)).findById(anyLong());
            verify(todoRepository, times(1)).findById(anyLong());
            verify(managerRepository, times(1)).findById(anyLong());
        }

    }
}

