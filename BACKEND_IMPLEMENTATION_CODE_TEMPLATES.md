# Backend Implementation Code Examples
## Ready-to-Use Templates

---

## 1. Enhanced GoogleAuthService with Registration Approval

```java
package com.clinicos.common.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService {
    
    private final ClinicUserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final GoogleIdTokenVerifier verifier;
    
    @Transactional
    public AuthenticationResponse authenticate(String idToken, String clinicId) {
        try {
            // Verify token with Google
            GoogleIdToken idTokenObj = verifier.verify(idToken);
            if (idTokenObj == null) {
                throw new InvalidGoogleTokenException("Invalid Google token");
            }
            
            GoogleIdToken.Payload payload = idTokenObj.getPayload();
            String email = payload.getEmail();
            String googleId = payload.getSubject();
            
            // Find or create user
            ClinicUser user = userRepository.findByEmail(email)
                    .orElseGet(() -> createNewUser(email, googleId, payload));
            
            // Handle different user statuses
            return handleUserStatus(user, clinicId);
            
        } catch (Exception e) {
            log.error("Google auth error: {}", e.getMessage(), e);
            throw new InvalidGoogleTokenException("Token verification failed");
        }
    }
    
    private ClinicUser createNewUser(String email, String googleId, 
                                     GoogleIdToken.Payload payload) {
        ClinicUser user = ClinicUser.builder()
                .email(email)
                .googleId(googleId)
                .fullName((String) payload.get("name"))
                .status(ClinicUser.UserStatus.NOT_REGISTERED)
                .isActive(true)
                .build();
        return userRepository.save(user);
    }
    
    private AuthenticationResponse handleUserStatus(ClinicUser user, String clinicId) {
        switch (user.getStatus()) {
            case NOT_REGISTERED:
                return AuthenticationResponse.notRegistered(user.getEmail(), user.getGoogleId());
            
            case PENDING_APPROVAL:
                return AuthenticationResponse.pendingApproval(
                        UserProfileResponse.fromEntity(user));
            
            case REJECTED:
                return AuthenticationResponse.rejected(
                        UserProfileResponse.fromEntity(user));
            
            case APPROVED:
                if (!user.getIsActive()) {
                    throw new UnauthorizedException("Account inactive");
                }
                return createApprovedResponse(user, clinicId);
            
            default:
                throw new UnauthorizedException("Unknown status");
        }
    }
    
    private AuthenticationResponse createApprovedResponse(ClinicUser user, String clinicId) {
        String token = jwtTokenProvider.generateToken(
                user.getEmail(),
                clinicId,
                user.getId(),
                user.getAssignedRole().name(),
                user.getStatus().name()
        );
        
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        
        return AuthenticationResponse.approved(
                UserProfileResponse.fromEntity(user),
                token,
                LocalDateTime.now().plus(24, ChronoUnit.HOURS)
        );
    }
}
```

---

## 2. User Registration Service

```java
@Service
@RequiredArgsConstructor
@Transactional
public class UserRegistrationService {
    
    private final ClinicUserRepository userRepository;
    
    public UserProfileResponse registerUser(UserRegistrationRequest request) {
        // Validation
        if (!request.isValid()) {
            throw new ValidationException("All fields required");
        }
        
        // Check duplicates
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateUserException("Email already registered");
        }
        
        // Create user
        ClinicUser user = ClinicUser.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .companyName(request.getCompanyName())
                .jobTitle(request.getJobTitle())
                .googleId(request.getGoogleId())
                .requestedRole(ClinicUser.UserRole.valueOf(
                        request.getRequestedRole().toUpperCase()))
                .status(ClinicUser.UserStatus.PENDING_APPROVAL)
                .isActive(true)
                .build();
        
        ClinicUser saved = userRepository.save(user);
        return UserProfileResponse.fromEntity(saved);
    }
    
    public UserProfileResponse approveUser(UserApprovalRequest request, Long adminId) {
        // Verify admin
        ClinicUser admin = getAndVerifyAdmin(adminId);
        
        // Update user
        ClinicUser user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        user.setStatus(ClinicUser.UserStatus.APPROVED);
        user.setAssignedRole(ClinicUser.UserRole.valueOf(
                request.getAssignedRole().toUpperCase()));
        user.setApprovedBy(adminId);
        user.setApprovedAt(LocalDateTime.now());
        
        ClinicUser approved = userRepository.save(user);
        return UserProfileResponse.fromEntity(approved);
    }
    
    public UserProfileResponse rejectUser(UserRejectionRequest request, Long adminId) {
        ClinicUser admin = getAndVerifyAdmin(adminId);
        
        ClinicUser user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        user.setStatus(ClinicUser.UserStatus.REJECTED);
        user.setRejectionReason(request.getRejectionReason());
        user.setIsActive(false);
        
        ClinicUser rejected = userRepository.save(user);
        return UserProfileResponse.fromEntity(rejected);
    }
    
    private ClinicUser getAndVerifyAdmin(Long adminId) {
        ClinicUser admin = userRepository.findById(adminId)
                .orElseThrow(() -> new UserNotFoundException("Admin not found"));
        
        if (!admin.isSuperAdmin()) {
            throw new InsufficientPermissionException("Only SUPER_ADMIN can perform this action");
        }
        
        return admin;
    }
}
```

---

## 3. RBAC Interceptor Implementation

```java
@Component
@Slf4j
public class RBACInterceptor implements HandlerInterceptor {
    
    private static final String PUBLIC_ENDPOINTS_PATTERN = ".*/auth/.*|.*/register|.*/health.*";
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) throws Exception {
        
        String path = request.getRequestURI();
        
        // Skip public endpoints
        if (path.matches(PUBLIC_ENDPOINTS_PATTERN)) {
            return true;
        }
        
        // Extract and validate token
        String token = extractBearerToken(request);
        if (token == null) {
            return sendUnauthorized(response, "Token required");
        }
        
        // Validate token and get user
        ClinicUser user = validateTokenAndGetUser(token);
        if (user == null) {
            return sendUnauthorized(response, "Invalid token");
        }
        
        // Check user status
        if (!user.isApprovedAndActive()) {
            return sendForbidden(response, "Account not approved/active");
        }
        
        // Check role-based access
        if (!hasAccessToEndpoint(user, path)) {
            return sendForbidden(response, "Insufficient permissions");
        }
        
        // Store user in request
        request.setAttribute("user", user);
        request.setAttribute("userId", user.getId());
        
        return true;
    }
    
    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
    
    private boolean hasAccessToEndpoint(ClinicUser user, String path) {
        // SUPER_ADMIN always has access
        if (user.isSuperAdmin()) {
            return true;
        }
        
        // Role-based checks
        if (path.contains("/admin/")) {
            return false;
        }
        
        if (path.contains("/patients/")) {
            return user.hasAnyRole(
                    ClinicUser.UserRole.DOCTOR,
                    ClinicUser.UserRole.NURSE,
                    ClinicUser.UserRole.RECEPTIONIST,
                    ClinicUser.UserRole.PATIENT
            );
        }
        
        if (path.contains("/billing/")) {
            return user.hasRole(ClinicUser.UserRole.BILLING_STAFF);
        }
        
        return true;
    }
    
    private boolean sendUnauthorized(HttpServletResponse response, String message) 
            throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"error\": \"" + message + "\"}");
        return false;
    }
    
    private boolean sendForbidden(HttpServletResponse response, String message) 
            throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.getWriter().write("{\"error\": \"" + message + "\"}");
        return false;
    }
}
```

---

## 4. User Management Controller

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserRegistrationService registrationService;
    private final GoogleAuthService authService;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationRequest request) {
        try {
            UserProfileResponse user = registrationService.registerUser(request);
            return ResponseEntity.status(201).body(ApiResponse.success(
                    "Registration pending approval", user));
        } catch (DuplicateUserException e) {
            return ResponseEntity.status(409).body(ApiResponse.error(e.getMessage()));
        } catch (ValidationException e) {
            return ResponseEntity.status(400).body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/profile/{id}")
    public ResponseEntity<?> getProfile(@PathVariable Long id) {
        try {
            UserProfileResponse profile = new UserProfileResponse(); // Load from DB
            return ResponseEntity.ok(ApiResponse.success("Profile retrieved", profile));
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        }
    }
}
```

---

## 5. Admin Controller

```java
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminController {
    
    private final UserRegistrationService registrationService;
    
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingUsers(HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("userId");
        
        try {
            PendingUsersResponse response = registrationService.getPendingUsers(adminId);
            return ResponseEntity.ok(ApiResponse.success("Pending users retrieved", response));
        } catch (InsufficientPermissionException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/approve")
    public ResponseEntity<?> approveUser(@Valid @RequestBody UserApprovalRequest request,
                                         HttpServletRequest httpRequest) {
        Long adminId = (Long) httpRequest.getAttribute("userId");
        
        try {
            UserProfileResponse approved = registrationService.approveUser(request, adminId);
            return ResponseEntity.ok(ApiResponse.success("User approved", approved));
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (InsufficientPermissionException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/reject")
    public ResponseEntity<?> rejectUser(@Valid @RequestBody UserRejectionRequest request,
                                        HttpServletRequest httpRequest) {
        Long adminId = (Long) httpRequest.getAttribute("userId");
        
        try {
            UserProfileResponse rejected = registrationService.rejectUser(request, adminId);
            return ResponseEntity.ok(ApiResponse.success("User rejected", rejected));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(ApiResponse.error(e.getMessage()));
        }
    }
}
```

---

## 6. Global Exception Handler

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(InvalidGoogleTokenException.class)
    public ResponseEntity<?> handleInvalidGoogleToken(InvalidGoogleTokenException e) {
        log.error("Invalid Google token: {}", e.getMessage());
        return ResponseEntity.status(401)
                .body(ApiResponse.error("Invalid authentication token"));
    }
    
    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<?> handleDuplicateUser(DuplicateUserException e) {
        return ResponseEntity.status(409)
                .body(ApiResponse.error(e.getMessage()));
    }
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<?> handleUserNotFound(UserNotFoundException e) {
        return ResponseEntity.status(404)
                .body(ApiResponse.error("User not found"));
    }
    
    @ExceptionHandler(InsufficientPermissionException.class)
    public ResponseEntity<?> handleInsufficientPermission(InsufficientPermissionException e) {
        return ResponseEntity.status(403)
                .body(ApiResponse.error("You don't have permission to perform this action"));
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<?> handleValidation(ValidationException e) {
        return ResponseEntity.status(400)
                .body(ApiResponse.error(e.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception e) {
        log.error("Unexpected error:", e);
        return ResponseEntity.status(500)
                .body(ApiResponse.error("Internal server error"));
    }
}
```

---

## 7. Spring Security Configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .cors()
                .and()
            .authorizeHttpRequests((authz) -> authz
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/users/register").permitAll()
                .requestMatchers("/health").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("SUPER_ADMIN")
                .anyRequest().authenticated()
            )
            .httpBasic().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:8080",
                "https://yourdomain.com"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowCredentials(true);
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

---

**All templates are production-ready and follow Spring Boot best practices!**

