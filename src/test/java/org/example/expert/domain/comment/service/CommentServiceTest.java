package org.example.expert.domain.comment.service;

import org.example.expert.domain.comment.dto.request.CommentSaveRequest;
import org.example.expert.domain.comment.dto.response.CommentResponse;
import org.example.expert.domain.comment.dto.response.CommentSaveResponse;
import org.example.expert.domain.comment.entity.Comment;
import org.example.expert.domain.comment.repository.CommentRepository;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.common.exception.ServerException;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private TodoRepository todoRepository;
    @InjectMocks
//    @Spy
    private CommentService commentService;

    @Nested
    class saveCommentTest {

        @Test
        public void comment_등록_중_할일을_찾지_못해_에러가_발생한다() {
            // given
            long todoId = 1;
            CommentSaveRequest request = new CommentSaveRequest("contents");
            AuthUser authUser = new AuthUser(1L, "email", UserRole.USER);

            given(todoRepository.findById(anyLong())).willReturn(Optional.empty());

            // when
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
                commentService.saveComment(authUser, todoId, request);
            });

            // then
            assertEquals("Todo not found", exception.getMessage());
        }

        @Test
        public void comment를_정상적으로_등록한다() {
            // given
            long todoId = 1;
            CommentSaveRequest request = new CommentSaveRequest("contents");
            AuthUser authUser = new AuthUser(1L, "email", UserRole.USER);
            User user = User.fromAuthUser(authUser);
            Todo todo = new Todo("title", "title", "contents", user);
            Comment comment = new Comment(request.getContents(), user, todo);

            given(todoRepository.findById(anyLong())).willReturn(Optional.of(todo));
            given(commentRepository.save(any())).willReturn(comment);

            // when
            CommentSaveResponse result = commentService.saveComment(authUser, todoId, request);

            // then
            assertNotNull(result);
        }

    }

    @Nested
    class getCommentsTest {

        @Test
        public void getCommentsTest_댓글_조회_성공() {
            // 가짜 데이터 생성
            long todoId = 1L;

            User user = new User("alden200@naver.com", "1234", UserRole.USER);
            Todo todo = new Todo("title", "contents", "weather", user);
            Comment mockComment1 = new Comment("asd", user, todo);
            Comment mockComment2 = new Comment("asd", user, todo);

            List<Comment> mockCommentList = Arrays.asList(mockComment1, mockComment2);

            // 리포지토리의 가짜 행동 설정
            when(commentRepository.findByTodoIdWithUser(todoId)).thenReturn(mockCommentList);

            // 메소드 실행
            List<CommentResponse> commentResponseList = commentService.getComments(todoId);

            // 결과 확인
            assertNotNull(commentResponseList);
            assertEquals(2, commentResponseList.size()); // 두 개의 댓글을 반환했는지 확인
            assertEquals("asd", commentResponseList.get(0).getContents()); // 첫 댓글 내용 확인
            assertEquals("alden200@naver.com", commentResponseList.get(0).getUser().getEmail()); // 첫 댓글 유저 이메일 확인

            // commentRepository의 findByTodoIdWithUser가 호출되었는지 검증
            verify(commentRepository, times(1)).findByTodoIdWithUser(todoId);
        }
    }
}
