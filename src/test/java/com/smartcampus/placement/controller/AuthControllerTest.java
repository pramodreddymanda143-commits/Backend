package com.smartcampus.placement.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcampus.placement.repository.ApplicationRepository;
import com.smartcampus.placement.repository.AdminAccountRepository;
import com.smartcampus.placement.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private AdminAccountRepository admins;
  @Autowired private StudentRepository students;
  @Autowired private ApplicationRepository applications;
  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void clearAdminAccount() {
    admins.deleteAll();
    applications.deleteAll();
    students.deleteAll();
  }

  @Test
  void placementApiRequiresAnAuthenticatedAdmin() throws Exception {
    mockMvc.perform(get("/api/students")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/api/auth/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authenticated").value(false));
  }

  @Test
  void firstAdminCanRegisterSignInAndSignOut() throws Exception {
    String email = "admin@example.com";
    String password = "a-strong-password-123";
    String registration = objectMapper.writeValueAsString(new Credentials(email, password));

    mockMvc
        .perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(registration))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.message").value("Admin account created. Sign in to continue."));

    var savedAdmin = admins.findById(1L).orElseThrow();
    org.junit.jupiter.api.Assertions.assertNotEquals(password, savedAdmin.getPasswordHash());
    org.junit.jupiter.api.Assertions.assertTrue(savedAdmin.getPasswordHash().startsWith("$2"));

    mockMvc
        .perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new Credentials("another@example.com", password))))
        .andExpect(status().isConflict());

    mockMvc
        .perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new Credentials(email, "incorrect-password"))))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new Credentials("missing@example.com", "a-strong-password-123"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Invalid email or password"));

    MvcResult login =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new Credentials(email, password))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email))
            .andReturn();
    MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

    mockMvc
        .perform(get("/api/auth/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authenticated").value(true))
        .andExpect(jsonPath("$.email").value(email));

    mockMvc
        .perform(post("/api/auth/logout").with(csrf()).session(session))
        .andExpect(status().isNoContent());
    mockMvc.perform(get("/api/students")).andExpect(status().isUnauthorized());
  }

  @Test
  void studentCanCreateAccountSignInAndOnlyAccessStudentRoutes() throws Exception {
    var registration =
        new StudentRegistration("Test Student", "student@example.com", "CSE", 8.2, "Java, React");

    mockMvc
        .perform(
            post("/api/auth/student/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registration)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.message").value("Student account created. Sign in with your email to continue."));

    mockMvc
        .perform(
            post("/api/auth/student/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new StudentRegistration("Test Student", "STUDENT@example.com", "CSE", 8.2, ""))))
        .andExpect(status().isConflict());

    MvcResult login =
        mockMvc
            .perform(
                post("/api/auth/student/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new StudentLogin("STUDENT@example.com"))))
            .andExpect(status().isOk())
            .andReturn();
    MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
    Long studentId = students.findByEmail("student@example.com").orElseThrow().getId();

    mockMvc
        .perform(get("/api/auth/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authenticated").value(true))
        .andExpect(jsonPath("$.role").value("student"))
        .andExpect(jsonPath("$.studentId").value(studentId))
        .andExpect(jsonPath("$.studentName").value("Test Student"));

    mockMvc.perform(get("/api/jobs").session(session)).andExpect(status().isOk());
    mockMvc
        .perform(get("/api/applications/student/" + studentId).session(session))
        .andExpect(status().isOk());
    mockMvc.perform(get("/api/dashboard").session(session)).andExpect(status().isForbidden());
    mockMvc.perform(get("/api/students").session(session)).andExpect(status().isForbidden());

    mockMvc
        .perform(post("/api/auth/logout").with(csrf()).session(session))
        .andExpect(status().isNoContent());
  }

  @Test
  void studentLoginRejectsAnUnknownEmail() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/student/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new StudentLogin("missing@example.com"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Student account not found. Create an account first."));
  }

  private record Credentials(String email, String password) {}

  private record StudentRegistration(String name, String email, String branch, Double cgpa, String skills) {}

  private record StudentLogin(String email) {}
}
