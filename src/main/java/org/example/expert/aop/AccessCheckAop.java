package org.example.expert.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.regex.Pattern;

@Slf4j
@Aspect
public class AccessCheckAop {

    @Pointcut("@annotation(org.example.expert.annotation.AccessRecord)")
    private void accessRecordAnnotation(){}

    @Before("accessRecordAnnotation()")
    public void logApiRequest() {

        // 요청 정보를 가져오기 위해 HttpServletRequest 사용
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        if (request != null) {

            // 요청한 사용자의 ID (여기서는 Authorization 헤더에서 추출한다고 가정)
            Long userId = (Long) request.getAttribute("userId");  // JWT 토큰이나 세션 등에서 추출 가능


            String requestTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // 요청 URL
            String requestUrl = request.getRequestURL().toString();

            // 로그 기록
            log.info("User ID: {}, Request Time: {}, Request URL: {}", userId, requestTime, requestUrl);
        }
    }
}
