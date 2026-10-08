package com.smartcampus.placement.controller;

import com.smartcampus.placement.entity.AdminAccount;
import com.smartcampus.placement.entity.Student;
import com.smartcampus.placement.repository.AdminAccountRepository;
import com.smartcampus.placement.repository.StudentRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AdminAccountRepository admins;
  private final StudentRepository students;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final SecurityContextRepository securityContextRepository;

  public AuthController(
      AdminAccountRepository admins,
      StudentRepository students,
      PasswordEncoder passwordEncoder,
      AuthenticationManager authenticationManager,
      SecurityContextRepository securityContextRepository) {
    this.admins = admins;
    this.students = students;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
    this.securityContextRepository = securityContextRepository;
  }

  @GetMapping("/csrf")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken());
  }

  @GetMapping("/status")
  public Map<String, Boolean> status() {
    return Map.of("hasAdmin", admins.existsById(1L));
  }

  @PostMapping("/register")
  public ResponseEntity<Map<String, String>> register(@Valid @RequestBody Registration request) {
    if (admins.existsById(1L)) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(Map.of("error", "An admin account has already been registered"));
    }
    if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Password must be no more than 72 UTF-8 bytes"));
    }

    AdminAccount admin =
        new AdminAccount(request.email().trim().toLowerCase(), passwordEncoder.encode(request.password()));
    try {
      admins.saveAndFlush(admin);
    } catch (DataIntegrityViolationException exception) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(Map.of("error", "An admin account has already been registered"));
    }
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(Map.of("message", "Admin account created. Sign in to continue."));
  }

  @PostMapping("/student/register")
  public ResponseEntity<Map<String, String>> registerStudent(
      @Valid @RequestBody StudentRegistration request) {
    String email = request.email().trim().toLowerCase();
    if (students.findByEmailIgnoreCase(email).isPresent()) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(Map.of("error", "A student account already exists for this email."));
    }

    Student student =
        new Student(
            request.name().trim(),
            email,
            request.branch().trim(),
            request.cgpa(),
            request.skills() == null ? "" : request.skills().trim());
    students.save(student);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(Map.of("message", "Student account created. Sign in with your email to continue."));
  }

  @PostMapping("/student/login")
  public ResponseEntity<Map<String, String>> loginStudent(
      @Valid @RequestBody StudentLogin request,
      HttpServletRequest servletRequest,
      HttpServletResponse servletResponse) {
    Student student = students.findByEmailIgnoreCase(request.email().trim()).orElse(null);
    if (student == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Student account not found. Create an account first."));
    }

    User principal =
        new User(student.getEmail(), "", List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(
            principal, null, principal.getAuthorities());
    establishSession(authentication, servletRequest, servletResponse);
    return ResponseEntity.ok(Map.of("email", student.getEmail()));
  }

  @PostMapping("/login")
  public ResponseEntity<Map<String, String>> login(
      @Valid @RequestBody Login request,
      HttpServletRequest servletRequest,
      HttpServletResponse servletResponse) {
    Authentication authentication;
    try {
      authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(
                  request.email().trim().toLowerCase(), request.password()));
    } catch (AuthenticationException exception) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Invalid email or password"));
    }

    establishSession(authentication, servletRequest, servletResponse);
    return ResponseEntity.ok(Map.of("email", authentication.getName()));
  }

  @GetMapping("/me")
  public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
    if (authentication == null
        || authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ANONYMOUS"))) {
      return ResponseEntity.ok(Map.of("authenticated", false));
    }

    Map<String, Object> user = new LinkedHashMap<>();
    user.put("email", authentication.getName());
    user.put("authenticated", true);
    boolean isStudent =
        authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_STUDENT"));
    if (!isStudent) {
      user.put("role", "admin");
      return ResponseEntity.ok(user);
    }

    return students
        .findByEmailIgnoreCase(authentication.getName())
        .map(
            student -> {
              user.put("role", "student");
              user.put("studentId", student.getId());
              user.put("studentName", student.getName());
              user.put("branch", student.getBranch());
              user.put("cgpa", student.getCgpa());
              user.put("skills", student.getSkills());
              return ResponseEntity.ok(user);
            })
        .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Student account not found.")));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    SecurityContextHolder.clearContext();
    return ResponseEntity.noContent().build();
  }

  private void establishSession(
      Authentication authentication,
      HttpServletRequest servletRequest,
      HttpServletResponse servletResponse) {
    if (servletRequest.getSession(false) != null) {
      servletRequest.changeSessionId();
    }
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    securityContextRepository.saveContext(context, servletRequest, servletResponse);
  }

  public record Registration(
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(min = 12, max = 72) String password) {}

  public record Login(
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(max = 72) String password) {}

  public record StudentRegistration(
      @NotBlank @Size(max = 120) String name,
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(max = 40) String branch,
      @NotNull @DecimalMin("0.0") @DecimalMax("10.0") Double cgpa,
      @Size(max = 1000) String skills) {}

  public record StudentLogin(@NotBlank @Email @Size(max = 254) String email) {}
}
