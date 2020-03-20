package com.huang.framework.authority.auth2;

import com.huang.framework.authority.config.SmsAuthenticationConfig;
import com.huang.framework.authority.filter.CustomAuthenticationFilter;
import com.huang.framework.authority.filter.SmsCodeAuthenticationFilter;
import com.huang.framework.authority.handler.GlobalAccessDeniedHandler;
import com.huang.framework.authority.handler.GlobalAuthenticationEntryPoint;
import com.huang.framework.authority.handler.GlobalAuthenticationFailureHandler;
import com.huang.framework.authority.handler.GlobalAuthenticationSuccessHandler;
import com.huang.framework.authority.service.AbstractCheckSmsCode;
import com.huang.framework.authority.service.SecurityConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 资源服务配置
 * 添加自定义登录配置 短信、第三方
 * @author -Huang
 * @create 2020-03-18 19:02
 */
@Configuration
@EnableResourceServer
public class OAuth2ResourceServerConfig extends ResourceServerConfigurerAdapter {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private AbstractCheckSmsCode abstractCheckSmsCode;

    @Autowired
    private GlobalAuthenticationSuccessHandler globalAuthenticationSuccessHandler;

    @Autowired
    private GlobalAuthenticationFailureHandler globalAuthenticationFailureHandler;

    @Autowired
    private GlobalAccessDeniedHandler globalAccessDeniedHandler;

    @Autowired
    private GlobalAuthenticationEntryPoint globalAuthenticationEntryPoint;

    @Autowired
    private SmsAuthenticationConfig smsAuthenticationConfig;

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
        super.configure(resources);
    }

//    @Bean
//    public BCryptPasswordEncoder bCryptPasswordEncoder(){
//        return new BCryptPasswordEncoder();
//    }


    @Override
    public void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .anyRequest()
                .authenticated();

        //配置短信验证码过滤器
        http.addFilterBefore(new SmsCodeAuthenticationFilter(abstractCheckSmsCode), UsernamePasswordAuthenticationFilter.class);

        //表单登录登录配置
        http.formLogin()
                .loginProcessingUrl(SecurityConstants.DEFAULT_LOGIN_URL_USERNAME_PASSWORD)
                .successHandler(globalAuthenticationSuccessHandler)
                .failureHandler(globalAuthenticationFailureHandler);

        //添加自定义json登陆处理、短信登陆配置
        http.addFilter(customAuthenticationFilter()).apply(smsAuthenticationConfig);

        //访问异常以及权限异常处理器配置
        http.exceptionHandling()
                .accessDeniedHandler(globalAccessDeniedHandler)
                .authenticationEntryPoint(globalAuthenticationEntryPoint);

        // 禁用 SESSION、JSESSIONID
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

    /**
     * 自定义用户名密码json登录过滤器
     * @return
     * @throws Exception
     */
    @Bean
    CustomAuthenticationFilter customAuthenticationFilter() throws Exception {
        CustomAuthenticationFilter filter = new CustomAuthenticationFilter();
        filter.setAuthenticationSuccessHandler(globalAuthenticationSuccessHandler);
        filter.setAuthenticationFailureHandler(globalAuthenticationFailureHandler);
        filter.setAuthenticationManager(authenticationManager);
        return filter;
    }
}
