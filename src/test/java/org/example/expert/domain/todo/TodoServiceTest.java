package org.example.expert.domain.todo;

import org.example.expert.client.WeatherClient;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.todo.service.TodoService;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class TodoServiceTest {

    @Mock
    private TodoRepository todoRepository;
    @Mock
    private WeatherClient weatherClient;

    @InjectMocks
    private TodoService todoService;

    @Nested
    class SaveTodoTest {
        @Test
        public void 일정_작성_성공() {

            // given
            Long authUserId = 1L;
            AuthUser authUser = new AuthUser(authUserId, "alden200@naver.com", UserRole.USER);
            User user = User.fromAuthUser(authUser);
            TodoSaveRequest todoSaveRequest = new TodoSaveRequest("title", "contents");

            String weather = "Sunny";
            given(weatherClient.getTodayWeather()).willReturn(weather); // 반드시 필요

            Todo newTodo = new Todo(todoSaveRequest.getTitle(), todoSaveRequest.getContents(), weather, user);

            given(todoRepository.save(any(Todo.class))).willReturn(newTodo);
            // when

            TodoSaveResponse todoSaveResponse = todoService.saveTodo(authUser, todoSaveRequest);
            // then
            assertNotNull(todoSaveResponse);
            assertEquals(newTodo.getId(), todoSaveResponse.getId()); // 저장된 ID 확인
            assertEquals(newTodo.getTitle(), todoSaveResponse.getTitle()); // 저장된 제목 확인
            assertEquals(authUser.getId(), todoSaveResponse.getUser().getId()); // 저장된 제목 확인
        }
    }

    @Nested
    class GetTodos {

        @Test
        public void 일정_목록_정상_조회() {
            // given
            int page = 1;
            int size = 5;
            Pageable pageable = PageRequest.of(page - 1, size);
            String weather = "Sunny";

            // 가짜 유저와 Todo 객체 생성
            User user = new User("alden200@gamil.com", "1234", UserRole.USER);
            ReflectionTestUtils.setField(user, "id", 1L);
            Todo todo1 = new Todo("title1", "contents1", weather, user);
            Todo todo2 = new Todo("title2", "contents2", weather, user);

            // Todo 리스트와 Page 객체 생성
            List<Todo> todoList = List.of(todo1, todo2);
            Page<Todo> todos = new PageImpl<>(todoList, pageable, todoList.size());

            given(todoRepository.findAllByOrderByModifiedAtDesc(any(Pageable.class))).willReturn(todos);

            // when
            Page<TodoResponse> todoResponses = todoService.getTodos(page, size);

            // then
            assertNotNull(todoResponses);
            assertEquals(2, todoResponses.getTotalElements());
            assertEquals(2, todoResponses.getContent().size());
            assertEquals("title1", todoResponses.getContent().get(0).getTitle());
            assertEquals("Sunny", todoResponses.getContent().get(0).getWeather());
            assertEquals(1, todoResponses.getContent().get(0).getUser().getId());
        }
    }

    @Nested
    class GetTodoTest {
        @Test
        public void 일정_조회_entity_없음() {
            // given
            long todoId = 1L;
            long userId = 2L;
            User user = new User("alden200", "1234", UserRole.USER);
            ReflectionTestUtils.setField(user, "id", userId);
            Todo todo = new Todo("title", "contents", "weather", user);
            ReflectionTestUtils.setField(todo, "id", todoId);

            given(todoRepository.findByIdWithUser(anyLong())).willReturn(Optional.empty());

            // when
            InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                    () -> todoService.getTodo(todoId));

            // then
            assertEquals("Todo not found", exception.getMessage());
        }

        @Test
        public void 일정_조회_정상동작() {
            // given
            long todoId = 1L;
            long userId = 2L;
            User user = new User("alden200", "1234", UserRole.USER);
            ReflectionTestUtils.setField(user, "id", userId);
            Todo todo = new Todo("title", "contents", "weather", user);
            ReflectionTestUtils.setField(todo, "id", todoId);

            given(todoRepository.findByIdWithUser(anyLong())).willReturn(Optional.of(todo));

            // when
            TodoResponse todoResponse = todoService.getTodo(todoId);

            // then
            assertNotNull(todoResponse);
            assertEquals(1, todoResponse.getId());
            assertEquals(2, todoResponse.getUser().getId());
        }

    }

}
