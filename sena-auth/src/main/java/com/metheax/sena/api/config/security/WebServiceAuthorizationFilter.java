package com.metheax.sena.api.config.security;

import com.metheax.sena.api.domain.BaseAPIResponse;
import com.metheax.sena.api.service.MetheaAuthenticationService;
import com.metheax.sena.config.security.GrantedPermission;
import com.metheax.sena.config.security.PrincipalAuthentication;
import com.metheax.sena.constant.MetheaConstant;
import com.metheax.sena.domain.entity.PublicPermission;
import com.metheax.sena.repository.WhiteURIPermissionRepository;
import com.metheax.sena.service.KeyStoreService;
import com.metheax.sena.utils.auth.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.util.ObjectUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Author : Kuylim Tith
 * Date : 09/04/2018
 */
public class WebServiceAuthorizationFilter extends BasicAuthenticationFilter {

    private final MetheaAuthenticationService metheaAuthenticationService;
    private final KeyStoreService keyStoreService;
    private final WhiteURIPermissionRepository whiteURLRepository;
    private static final String WHITE_URL = "auth";

    @Inject
    public WebServiceAuthorizationFilter(MetheaAuthenticationService metheaAuthenticationService,
                                         AuthenticationManager authenticationManager, KeyStoreService keyStoreService,
                                         WhiteURIPermissionRepository whiteURLRepository) {
        super(authenticationManager);
        this.keyStoreService = keyStoreService;
        this.metheaAuthenticationService = metheaAuthenticationService;
        this.whiteURLRepository = whiteURLRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws IOException, ServletException {
        boolean isNotAuthorize = true;
        PrincipalAuthentication authentication = null;
        String token = req.getHeader(SecurityConstants.HEADER_STRING);
        List<PublicPermission> whiteURIs = whiteURLRepository.findAllByStatus(MetheaConstant.ACTIVE_STATUS);
        PublicPermission tmp = null;
        // Initial white list
        if (CollectionUtils.isNotEmpty(whiteURIs)) {
            Map<String, PublicPermission> map = whiteURIs.stream()
                    .collect(Collectors.toMap(x -> x.getResource().getResourceName(), o -> o));
            // check white parent config
            String wURI = StringUtils.EMPTY;
            for (String str : req.getRequestURI().split(MetheaConstant.SLASH)) {
                wURI = wURI.concat(MetheaConstant.SLASH).concat(StringUtils.stripToEmpty(str));
                StringBuilder builder = new StringBuilder(wURI);
                String key = (builder.deleteCharAt(0) + MetheaConstant.SLASH_STAR);
                if (map.containsKey(key)) {
                    tmp = map.get(key);
                    break;
                }
            }
            // check exact white uri
            if (map.containsKey(req.getRequestURI()) && ObjectUtils.isEmpty(tmp)) {
                tmp = map.get(req.getRequestURI());
            } else if (map.containsKey(req.getRequestURI() + MetheaConstant.SLASH_STAR) && ObjectUtils.isEmpty(tmp)) {
                tmp = map.get(req.getRequestURI() + MetheaConstant.SLASH_STAR);
            } else if (map.containsKey(req.getRequestURI() + MetheaConstant.DOUBLE_STAR) && ObjectUtils.isEmpty(tmp)) {
                tmp = map.get(req.getRequestURI() + MetheaConstant.DOUBLE_STAR);
            }
        }

        // validate allow method
        if (!ObjectUtils.isEmpty(tmp)) {
            if (tmp.getAllowedMethod().contains(req.getMethod())) {
                authentication = metheaAuthenticationService.loadUserByUsername(MetheaConstant.PUBLIC_USER);
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(authentication, null, authentication.getAuthorities()));
                isNotAuthorize = false;
            }
        }

        // validate token
        if (!StringUtils.isEmpty(token) && !WHITE_URL.equals(req.getRequestURI().split(MetheaConstant.SLASH)[1]) && isNotAuthorize) {

            String subject = JwtUtil.decodeToken(token.replace(SecurityConstants.TOKEN_PREFIX, StringUtils.EMPTY),
                    keyStoreService.loadTokenKeyPair().getPrivate());
            if (!StringUtils.isEmpty(subject)) {
                authentication = metheaAuthenticationService.loadUserByUsername(subject.split(MetheaConstant.COLON)[0]);
                if (metheaAuthenticationService.validateUserRevokedToken(subject)) {
                    constructUnAuthorizeResponse(res, false);
                    return;
                }
            } else {
                constructUnAuthorizeResponse(res, false);
                return;
            }
        }
        // validate permission
        if (!ObjectUtils.isEmpty(authentication) && isNotAuthorize) {
            String uri = StringUtils.EMPTY;
            String requestURI = req.getRequestURI();
            List<GrantedPermission> grantedPermissions = authentication.getGrantedPermissions();
            List<String> grantedURIs = CollectionUtils.emptyIfNull(grantedPermissions).stream()
                    .map(GrantedPermission::getGrantedPermission)
                    .collect(Collectors.toList());

            //>>>>> is it contain parent configuration?
            for (String str : requestURI.split(MetheaConstant.SLASH)) {
                uri = uri.concat(MetheaConstant.SLASH).concat(StringUtils.stripToEmpty(str));
                StringBuilder builder = new StringBuilder(uri);
                if (grantedURIs.contains(requestURI) || grantedURIs.contains((builder.deleteCharAt(0) + MetheaConstant.SLASH_STAR))) {
                    isNotAuthorize = false;
                }
            }
            //>>>>> is it config with this specific URI, will check on second condition when request "/"
            if (grantedURIs.contains(requestURI) || grantedURIs.contains(requestURI + MetheaConstant.SLASH_STAR)
                    || grantedURIs.contains(requestURI + MetheaConstant.DOUBLE_STAR)) {
                isNotAuthorize = false;
            }
            if (isNotAuthorize) {
                constructUnAuthorizeResponse(res, true);
                return;
            }
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(authentication, null, authentication.getAuthorities()));
        }
        chain.doFilter(req, res);
    }

    private void constructUnAuthorizeResponse(HttpServletResponse res, boolean authenticated) throws IOException {
        BaseAPIResponse apiResponse = new BaseAPIResponse();
        if(authenticated) {
            apiResponse.setMessage("No permission to access this resource.");
            apiResponse.setStatus(HttpStatus.FORBIDDEN);
        } else {
            apiResponse.setMessage("Invalid token.");
            apiResponse.setStatus(HttpStatus.UNAUTHORIZED);
        }

        ObjectMapper mapper = JsonMapper.builder().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS).build();
        String jsonFormat = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(apiResponse);
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setContentLength(jsonFormat.length());
        res.getWriter().write(jsonFormat);
    }
}
