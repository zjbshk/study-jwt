package cn.infomany.interceptor;

import cn.infomany.annotation.JwtIgnore;
import cn.infomany.common.exception.CustomException;
import cn.infomany.common.response.ResultCode;
import cn.infomany.model.Audience;
import cn.infomany.util.JwtTokenUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * ========================
 * token验证拦截器
 * Created with IntelliJ IDEA.
 * User：pyy
 * Date：2019/7/18 9:46
 * Version: v1.0
 * ========================
 */
@Slf4j
public class JwtInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    private Audience audience;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 忽略带JwtIgnore注解的请求, 不做后续token认证校验
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            JwtIgnore jwtIgnore = handlerMethod.getMethodAnnotation(JwtIgnore.class);
            if (jwtIgnore != null) {
                return true;
            }
        }

        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }

        // 获取请求头信息authorization信息
        final String authHeader = request.getHeader(JwtTokenUtil.AUTH_HEADER_KEY);
        log.info("## authHeader= {}", authHeader);

        if (StringUtils.isBlank(authHeader) || !authHeader.startsWith(JwtTokenUtil.TOKEN_PREFIX)) {
            log.info("### 用户未登录，请先登录 ###");
            throw new CustomException(ResultCode.USER_NOT_LOGGED_IN);
        }

        // 获取token
        final String token = authHeader.substring(JwtTokenUtil.TOKEN_PREFIX.length());

        if (audience == null) {
            BeanFactory factory = WebApplicationContextUtils.getRequiredWebApplicationContext(request.getServletContext());
            audience = factory.getBean(Audience.class);
        }

        // 验证token是否有效--无效已做异常抛出，由全局异常处理后返回对应信息
        Claims claims = JwtTokenUtil.parseJWT(token, audience.getBase64Secret());

        // 将token中携带的有效数据放到请求中
        for (Map.Entry<String, Object> stringObjectEntry : claims.entrySet()) {
            request.setAttribute(stringObjectEntry.getKey(), stringObjectEntry.getValue());
        }

        return true;
    }

}
