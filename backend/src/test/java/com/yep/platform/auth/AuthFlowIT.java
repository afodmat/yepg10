package com.yep.platform.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "yep.jwt.secret=test-secret-test-secret-test-secret-123456",
        "yep.cookies.secure=false"})
@AutoConfigureMockMvc
@Testcontainers
class AuthFlowIT {
    @Container @ServiceConnection static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    static String registerJson(String email, String password, int age) {
        return """
            {"firstName":"Amina","lastName":"Kato","email":"%s","phone":"+256700000000","country":"Uganda","age":%d,"password":"%s"}
            """.formatted(email, age, password);
    }
    static String loginJson(String email, String password) { return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password); }

    @Test void registerStoresHashedPasswordAndPendingParticipant() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerJson("reg@yep.test", "Passw0rdOK", 24)))
                .andExpect(status().isCreated());
        var row = jdbc.queryForMap("select password_hash, role, status from users where email = 'reg@yep.test'");
        org.junit.jupiter.api.Assertions.assertNotEquals("Passw0rdOK", row.get("password_hash"));
        org.junit.jupiter.api.Assertions.assertEquals("PARTICIPANT", row.get("role"));
        org.junit.jupiter.api.Assertions.assertEquals("PENDING", row.get("status"));
    }

    @Test void clientCannotChooseRole() throws Exception {
        String body = registerJson("sneaky@yep.test", "Passw0rdOK", 25).replace("{", "{\"role\":\"ADMIN\",");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        org.junit.jupiter.api.Assertions.assertEquals("PARTICIPANT", jdbc.queryForObject("select role from users where email='sneaky@yep.test'", String.class));
    }

    @Test void registrationValidatesInput() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerJson("bad@yep.test", "short", 17)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.password").exists()).andExpect(jsonPath("$.fieldErrors.age").exists());
    }

    @Test void pendingAccountCannotLogInAndWrongPasswordIsGeneric() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerJson("pend@yep.test", "Passw0rdOK", 24)));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson("pend@yep.test", "Passw0rdOK"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson("pend@yep.test", "WrongPass1"))).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson("nobody@yep.test", "WrongPass1"))).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test void activeUserCanLogInAndReadProfileAndCookiesAreHttpOnly() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerJson("ok@yep.test", "Passw0rdOK", 24)));
        jdbc.update("update users set status='ACTIVE' where email='ok@yep.test'");
        var res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson("ok@yep.test", "Passw0rdOK")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("PARTICIPANT")).andReturn().getResponse();
        var access = res.getCookie("yep_at");
        org.junit.jupiter.api.Assertions.assertTrue(access.isHttpOnly());
        mvc.perform(get("/api/users/me").cookie(access)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value("ok@yep.test"));
    }

    @Test void protectedEndpointsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/users/me").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
    }

    @Test void participantCannotReachAdminArea() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerJson("p@yep.test", "Passw0rdOK", 24)));
        jdbc.update("update users set status='ACTIVE' where email='p@yep.test'");
        var access = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson("p@yep.test", "Passw0rdOK")))
                .andReturn().getResponse().getCookie("yep_at");
        mvc.perform(get("/api/admin/stats").cookie(access)).andExpect(status().isForbidden());
    }
}
