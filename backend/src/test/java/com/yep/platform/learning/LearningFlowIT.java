package com.yep.platform.learning;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yep.platform.user.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** End-to-end journey: facilitator publishes an activity with an assessment; a participant enrolls, learns, passes and is certified. */
@SpringBootTest(properties = {"yep.jwt.secret=test-secret-test-secret-test-secret-123456", "yep.cookies.secure=false"})
@AutoConfigureMockMvc
@Testcontainers
class LearningFlowIT {
    @Container @ServiceConnection static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired ProfileRepository profiles;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;

    private void user(String email, Role role) {
        var u = users.save(new User(email, encoder.encode("Passw0rdOK"), role, UserStatus.ACTIVE));
        profiles.save(new Profile(u, "Test", role.name(), null, "Uganda", (short) 25));
    }
    private Cookie login(String email) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"%s\",\"password\":\"Passw0rdOK\"}".formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie("yep_at");
    }
    private JsonNode body(org.springframework.test.web.servlet.ResultActions r) throws Exception { return json.readTree(r.andReturn().getResponse().getContentAsString()); }

    @Test void participantCompletesActivityAndGetsVerifiableCertificate() throws Exception {
        user("fac@yep.test", Role.FACILITATOR); user("learner@yep.test", Role.PARTICIPANT);
        var fac = login("fac@yep.test"); var learner = login("learner@yep.test");

        // participants cannot create activities
        mvc.perform(post("/api/activities").cookie(learner).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());

        long activityId = body(mvc.perform(post("/api/activities").cookie(fac).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
            {"title":"Test Activity","description":"d","category":"Digital Skills","level":"Level 1","capacity":10,"lessonsTotal":2,"status":"OPEN"}"""))
            .andExpect(status().isCreated())).get("id").asLong();
        mvc.perform(put("/api/activities/%d/assessment".formatted(activityId)).cookie(fac).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
            {"title":"Quiz","passMark":50,"maxAttempts":2,"questions":[{"text":"2+2?","points":1,"options":[{"text":"4","correct":true},{"text":"5","correct":false}]}]}""")).andExpect(status().isOk());

        // enroll once; second time conflicts; the same list shows it under "mine"
        mvc.perform(post("/api/activities/%d/enroll".formatted(activityId)).cookie(learner).with(csrf())).andExpect(status().isCreated());
        mvc.perform(post("/api/activities/%d/enroll".formatted(activityId)).cookie(learner).with(csrf())).andExpect(status().isConflict());
        mvc.perform(get("/api/enrollments/mine").cookie(learner)).andExpect(status().isOk()).andExpect(jsonPath("$[0].progressPct").value(0));

        // participants never see which answer is correct
        var assessment = body(mvc.perform(get("/api/activities/%d/assessment".formatted(activityId)).cookie(learner)).andExpect(status().isOk()));
        assertTrue(assessment.at("/questions/0/options/0/correct").isMissingNode());
        long assessmentId = assessment.get("id").asLong();
        long qid = assessment.at("/questions/0/id").asLong();
        long correctOption = jdbc.queryForObject("select id from question_options where question_id = ? and correct", Long.class, qid);

        // finish lessons, then pass the assessment → completion, certificate, achievement
        for (int i = 0; i < 2; i++) mvc.perform(post("/api/activities/%d/lessons/complete".formatted(activityId)).cookie(learner).with(csrf())).andExpect(status().isOk());
        mvc.perform(post("/api/assessments/%d/submit".formatted(assessmentId)).cookie(learner).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"answers\":{\"%d\":%d}}".formatted(qid, correctOption))).andExpect(status().isOk()).andExpect(jsonPath("$.passed").value(true));
        mvc.perform(get("/api/enrollments/mine").cookie(learner)).andExpect(jsonPath("$[0].status").value("COMPLETED")).andExpect(jsonPath("$[0].progressPct").value(100));

        var certs = body(mvc.perform(get("/api/certificates").cookie(learner)).andExpect(status().isOk()));
        assertEquals(1, certs.size());
        String code = certs.get(0).get("verificationCode").asText();
        mvc.perform(get("/api/certificates/verify/" + code)).andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(true));   // public
        mvc.perform(get("/api/certificates/verify/NOPE")).andExpect(jsonPath("$.valid").value(false));
        var pdf = mvc.perform(get("/api/certificates/%d/pdf".formatted(certs.get(0).get("id").asLong())).cookie(learner)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertEquals("%PDF", new String(pdf, 0, 4));
        mvc.perform(get("/api/achievements").cookie(learner)).andExpect(jsonPath("$[?(@.name=='Digital Pioneer')].earned").value(true));
    }

    @Test void publicVisitorsSeeOnlyPublishedActivities() throws Exception {
        user("fac2@yep.test", Role.FACILITATOR);
        var fac = login("fac2@yep.test");
        mvc.perform(post("/api/activities").cookie(fac).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
            {"title":"Hidden Draft","description":"d","category":"Digital Skills","level":"Level 1","capacity":10,"lessonsTotal":2,"status":"DRAFT"}""")).andExpect(status().isCreated());
        mvc.perform(get("/api/activities?q=Hidden")).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/activities?q=Hidden").cookie(fac)).andExpect(jsonPath("$.total").value(1));
    }
}
