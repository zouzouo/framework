package com.huang.framework.authority.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huang.framework.utils.http.ContentCachingRequestWrapper;
import com.huang.framework.authority.entity.SecurityConstants;
import com.huang.framework.authority.handler.HAuthenticationFailureHandler;
import com.huang.framework.service.AbstractCheckSmsCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * 短信验证码验证过滤器
 * @author -Huang
 * @create 2020-03-13 11:03
 */
@Component
public class SmsCodeAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private HAuthenticationFailureHandler authenticationFailureHandler;

    @Autowired
    private AbstractCheckSmsCode abstractCheckSmsCode;

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest,
                                    HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        //从请求中获取url，如果url等于配置的短信登录url，并且为POST请求 进行校验逻辑
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(httpServletRequest);
        if (StringUtils.equalsIgnoreCase(SecurityConstants.DEFAULT_MOBILE_LOGIN_URL, httpServletRequest.getRequestURI())
                && StringUtils.equalsIgnoreCase(httpServletRequest.getMethod(), HttpMethod.POST.toString())) {
            try {
                //校验验证码
                validateCode(requestWrapper);
            } catch (AuthenticationException e) {
                authenticationFailureHandler.onAuthenticationFailure(httpServletRequest, httpServletResponse, e);
                return;
            }
        }
        filterChain.doFilter(requestWrapper, httpServletResponse);
    }

    private void validateCode(HttpServletRequest httpServletRequest){
        ServletInputStream is = null;
        String mobile = null;
        String code = null;
        try {
            is = httpServletRequest.getInputStream();
            ObjectMapper mapper = new ObjectMapper();
            Map<String,String> authenticationBean = mapper.readValue(is,Map.class);
            mobile = authenticationBean.get(SecurityConstants.DEFAULT_PARAMETER_NAME_MOBILE);
            code = authenticationBean.get(SecurityConstants.DEFAULT_PARAMETER_NAME_CODE_SMS);

            abstractCheckSmsCode.checkCode(mobile,code);
        } catch (Exception e) {
            e.printStackTrace();
            throw new InternalAuthenticationServiceException(e.getMessage());
        }
    }
}