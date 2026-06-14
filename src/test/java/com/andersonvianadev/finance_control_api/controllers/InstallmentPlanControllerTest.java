package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.InstallmentPlanRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.InstallmentPlanResponseDTO;
import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.dtos.CalendarDTO;
import com.andersonvianadev.finance_control_api.domain.models.dtos.InstallmentGenerationMessage;
import com.andersonvianadev.finance_control_api.domain.models.enums.InstallmentStatus;
import com.andersonvianadev.finance_control_api.domain.models.enums.UserRole;
import com.andersonvianadev.finance_control_api.domain.services.ICalendarService;
import com.andersonvianadev.finance_control_api.domain.services.IUserService;
import com.andersonvianadev.finance_control_api.infra.exceptions.StandardException;
import com.andersonvianadev.finance_control_api.infra.messaging.ISqsMessageSender;
import com.andersonvianadev.finance_control_api.infra.messaging.consumer.InstallmentGenerationProcessor;
import com.andersonvianadev.finance_control_api.infra.repositories.CategoryRepository;
import org.springframework.context.ApplicationContext;
import com.andersonvianadev.finance_control_api.infra.repositories.ExpenseRepository;
import com.andersonvianadev.finance_control_api.infra.repositories.IncomeRepository;
import com.andersonvianadev.finance_control_api.infra.repositories.InstallmentPlanRepository;
import com.andersonvianadev.finance_control_api.infra.repositories.RecurringRuleRepository;
import com.andersonvianadev.finance_control_api.infra.repositories.UserRepository;
import com.andersonvianadev.finance_control_api.infra.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
class InstallmentPlanControllerTest {

    @TestConfiguration
    static class SqsTestConfig {

        @Bean
        @Primary
        public ISqsMessageSender sqsMessageSender(ApplicationContext ctx) {
            return new ISqsMessageSender() {
                @Override
                public <T> void send(String queueUrl, T body) {
                    if (body instanceof InstallmentGenerationMessage msg) {
                        ctx.getBean(InstallmentGenerationProcessor.class).process(msg);
                    }
                }
            };
        }
    }

    @MockitoBean
    private ICalendarService calendarService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IUserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private InstallmentPlanRepository installmentPlanRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private IncomeRepository incomeRepository;

    @Autowired
    private RecurringRuleRepository recurringRuleRepository;

    @BeforeEach
    void setup() {
        incomeRepository.deleteAll();
        expenseRepository.deleteAll();
        installmentPlanRepository.deleteAll();
        recurringRuleRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        lenient().when(calendarService.getDate(any(LocalDate.class)))
                .thenAnswer(inv -> new CalendarDTO(inv.getArgument(0), true, null, null));
    }

    @Test
    @DisplayName("Should create plan and all installments when request is valid")
    void create_WhenRequestIsValid_ShouldReturnAcceptedWithPlanAndFirstExpense() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        Category categorySaved = categoryRepository.save(
                Category.builder()
                        .name("Technology")
                        .description("Electronics and gadgets")
                        .icon("tech")
                        .owner(userSaved)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        InstallmentPlanRequestDTO request = new InstallmentPlanRequestDTO(
                new BigDecimal("18000.00"),
                12,
                LocalDate.of(2026, 7, 1),
                "MacBook Pro 14",
                categorySaved.getId()
        );

        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/installment-plans")
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(MockMvcResultMatchers.status().isAccepted())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        InstallmentPlanResponseDTO response = objectMapper.readValue(content, InstallmentPlanResponseDTO.class);

        assertNotNull(response.id());
        assertEquals("MacBook Pro 14", response.description());
        assertEquals(0, new BigDecimal("18000.00").compareTo(response.totalAmount()));
        assertEquals(0, new BigDecimal("1500.00").compareTo(response.installmentAmount()));
        assertEquals(12, response.totalInstallments());
        assertEquals(0, response.paidInstallments());
        assertEquals(InstallmentStatus.ACTIVE, response.status());
        assertEquals(categorySaved.getId(), response.category().id());

        assertEquals(1, response.expenses().size());
        assertEquals(1, response.expenses().get(0).installmentNumber());
        assertEquals(12, response.expenses().get(0).totalInstallments());
        assertEquals(0, new BigDecimal("1500.00").compareTo(response.expenses().get(0).price()));

        assertEquals(1, installmentPlanRepository.count());
        assertEquals(12, expenseRepository.count());
    }

    @Test
    @DisplayName("Should return 404 when category does not exist")
    void create_WhenCategoryDoesNotExist_ShouldReturnNotFound() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        InstallmentPlanRequestDTO request = new InstallmentPlanRequestDTO(
                new BigDecimal("18000.00"),
                12,
                LocalDate.of(2026, 7, 1),
                "MacBook Pro 14",
                UUID.randomUUID()
        );

        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/installment-plans")
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        StandardException exception = objectMapper.readValue(content, StandardException.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
        assertEquals(0, installmentPlanRepository.count());
        assertEquals(0, expenseRepository.count());
    }

    @Test
    @DisplayName("Should return 400 when totalInstallments is less than 2")
    void create_WhenTotalInstallmentsIsLessThan2_ShouldReturnBadRequest() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        InstallmentPlanRequestDTO request = new InstallmentPlanRequestDTO(
                new BigDecimal("18000.00"),
                1,
                LocalDate.of(2026, 7, 1),
                "MacBook Pro 14",
                UUID.randomUUID()
        );

        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/installment-plans")
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        StandardException exception = objectMapper.readValue(content, StandardException.class);

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.status());
    }

    @Test
    @DisplayName("Should adjust first installment date when firstDueDate falls on a non-working day")
    void create_WhenFirstDueDateIsNonWorkingDay_ShouldAdjustTransactionDateToNextWorkingDay() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        Category categorySaved = categoryRepository.save(
                Category.builder()
                        .name("Technology")
                        .description("Electronics and gadgets")
                        .icon("tech")
                        .owner(userSaved)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        LocalDate nonWorkingDay = LocalDate.of(2026, 6, 28); // Sunday
        LocalDate nextWorkingDay = LocalDate.of(2026, 6, 30); // Tuesday

        when(calendarService.getDate(nonWorkingDay))
                .thenReturn(new CalendarDTO(nonWorkingDay, false, "Domingo", nextWorkingDay));

        InstallmentPlanRequestDTO request = new InstallmentPlanRequestDTO(
                new BigDecimal("18000.00"),
                12,
                nonWorkingDay,
                "MacBook Pro 14",
                categorySaved.getId()
        );

        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/installment-plans")
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(MockMvcResultMatchers.status().isAccepted())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        InstallmentPlanResponseDTO response = objectMapper.readValue(content, InstallmentPlanResponseDTO.class);

        assertEquals(1, response.expenses().size());
        assertEquals(LocalDateTime.of(2026, 6, 30, 0, 0), response.expenses().get(0).transactionDate());
        assertEquals(1, response.expenses().get(0).installmentNumber());
        assertEquals(12, response.expenses().get(0).totalInstallments());
        assertEquals(12, expenseRepository.count());
    }

    // --- findById ---

    @Test
    @DisplayName("Should return 200 with plan when it exists and belongs to the user")
    void findById_WhenPlanExists_ShouldReturnOk() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        Category categorySaved = categoryRepository.save(
                Category.builder()
                        .name("Technology")
                        .description("Electronics and gadgets")
                        .icon("tech")
                        .owner(userSaved)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        InstallmentPlanRequestDTO request = new InstallmentPlanRequestDTO(
                new BigDecimal("18000.00"),
                12,
                LocalDate.of(2026, 7, 1),
                "MacBook Pro 14",
                categorySaved.getId()
        );

        MvcResult createResult = mockMvc.perform(MockMvcRequestBuilders.post("/installment-plans")
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isAccepted())
                .andReturn();

        InstallmentPlanResponseDTO created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), InstallmentPlanResponseDTO.class);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/installment-plans/" + created.id())
                        .with(user(userPrincipal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        InstallmentPlanResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), InstallmentPlanResponseDTO.class);

        assertEquals(created.id(), response.id());
        assertEquals("MacBook Pro 14", response.description());
        assertEquals(InstallmentStatus.ACTIVE, response.status());
        assertEquals(categorySaved.getId(), response.category().id());
        assertEquals(12, response.expenses().size());
        for (int i = 0; i < 12; i++) {
            assertEquals(i + 1, response.expenses().get(i).installmentNumber());
        }
    }

    @Test
    @DisplayName("Should return empty expenses list when plan was cancelled")
    void findById_WhenPlanIsCancelled_ShouldReturnEmptyExpenses() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        Category categorySaved = categoryRepository.save(
                Category.builder()
                        .name("Technology")
                        .description("Electronics and gadgets")
                        .icon("tech")
                        .owner(userSaved)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        InstallmentPlanRequestDTO request = new InstallmentPlanRequestDTO(
                new BigDecimal("18000.00"),
                12,
                LocalDate.of(2026, 7, 1),
                "MacBook Pro 14",
                categorySaved.getId()
        );

        MvcResult createResult = mockMvc.perform(MockMvcRequestBuilders.post("/installment-plans")
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isAccepted())
                .andReturn();

        InstallmentPlanResponseDTO created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), InstallmentPlanResponseDTO.class);

        mockMvc.perform(MockMvcRequestBuilders.delete("/installment-plans/" + created.id())
                        .with(user(userPrincipal)))
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/installment-plans/" + created.id())
                        .with(user(userPrincipal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        InstallmentPlanResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), InstallmentPlanResponseDTO.class);

        assertEquals(InstallmentStatus.CANCELLED, response.status());
        assertTrue(response.expenses().isEmpty());
    }

    @Test
    @DisplayName("Should return 404 when plan does not exist")
    void findById_WhenPlanDoesNotExist_ShouldReturnNotFound() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/installment-plans/" + UUID.randomUUID())
                        .with(user(userPrincipal)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        StandardException exception = objectMapper.readValue(content, StandardException.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 404 when plan belongs to another user")
    void findById_WhenPlanBelongsToAnotherUser_ShouldReturnNotFound() throws Exception {
        User owner = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        User other = userService.save(
                User.builder()
                        .name("Other")
                        .email("other@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        Category categorySaved = categoryRepository.save(
                Category.builder()
                        .name("Technology")
                        .description("Electronics and gadgets")
                        .icon("tech")
                        .owner(owner)
                        .build()
        );

        UserPrincipal ownerPrincipal = new UserPrincipal(owner);
        UserPrincipal otherPrincipal = new UserPrincipal(other);

        InstallmentPlanRequestDTO request = new InstallmentPlanRequestDTO(
                new BigDecimal("18000.00"),
                12,
                LocalDate.of(2026, 7, 1),
                "MacBook Pro 14",
                categorySaved.getId()
        );

        MvcResult createResult = mockMvc.perform(MockMvcRequestBuilders.post("/installment-plans")
                        .with(user(ownerPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isAccepted())
                .andReturn();

        InstallmentPlanResponseDTO created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), InstallmentPlanResponseDTO.class);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/installment-plans/" + created.id())
                        .with(user(otherPrincipal)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    // --- findAll ---

    @Test
    @DisplayName("Should return page with plans when user has plans")
    void findAll_WhenPlansExist_ShouldReturnPage() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        Category categorySaved = categoryRepository.save(
                Category.builder()
                        .name("Technology")
                        .description("Electronics and gadgets")
                        .icon("tech")
                        .owner(userSaved)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        for (int i = 1; i <= 2; i++) {
            InstallmentPlanRequestDTO request = new InstallmentPlanRequestDTO(
                    new BigDecimal("6000.00"),
                    3,
                    LocalDate.of(2026, 6 + i, 1),
                    "Plan " + i,
                    categorySaved.getId()
            );
            mockMvc.perform(MockMvcRequestBuilders.post("/installment-plans")
                            .with(user(userPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(MockMvcResultMatchers.status().isAccepted());
        }

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/installment-plans")
                        .with(user(userPrincipal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        tools.jackson.databind.JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

        assertEquals(2, body.get("totalElement").asInt());
        assertEquals(2, body.get("content").size());
        body.get("content").forEach(plan -> assertEquals(0, plan.get("expenses").size()));
    }

    @Test
    @DisplayName("Should return empty page when user has no plans")
    void findAll_WhenNoPlanExists_ShouldReturnEmptyPage() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/installment-plans")
                        .with(user(userPrincipal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        tools.jackson.databind.JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

        assertEquals(0, body.get("totalElement").asInt());
        assertEquals(0, body.get("content").size());
    }

    // --- cancel ---

    @Test
    @DisplayName("Should return 204 and delete all expenses when cancelling an active plan")
    void cancel_WhenPlanIsActive_ShouldReturn204AndDeleteAllExpenses() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        Category categorySaved = categoryRepository.save(
                Category.builder()
                        .name("Technology")
                        .description("Electronics and gadgets")
                        .icon("tech")
                        .owner(userSaved)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        InstallmentPlanRequestDTO request = new InstallmentPlanRequestDTO(
                new BigDecimal("18000.00"),
                12,
                LocalDate.of(2026, 7, 1),
                "MacBook Pro 14",
                categorySaved.getId()
        );

        MvcResult createResult = mockMvc.perform(MockMvcRequestBuilders.post("/installment-plans")
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isAccepted())
                .andReturn();

        InstallmentPlanResponseDTO created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), InstallmentPlanResponseDTO.class);

        assertEquals(12, expenseRepository.count());

        mockMvc.perform(MockMvcRequestBuilders.delete("/installment-plans/" + created.id())
                        .with(user(userPrincipal)))
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        assertEquals(0, expenseRepository.count());

        List<com.andersonvianadev.finance_control_api.domain.models.InstallmentPlan> plans =
                installmentPlanRepository.findAll();
        assertEquals(1, plans.size());
        assertEquals(InstallmentStatus.CANCELLED, plans.get(0).getStatus());
    }

    @Test
    @DisplayName("Should return 422 when cancelling an already cancelled plan")
    void cancel_WhenPlanIsAlreadyCancelled_ShouldReturn422() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        Category categorySaved = categoryRepository.save(
                Category.builder()
                        .name("Technology")
                        .description("Electronics and gadgets")
                        .icon("tech")
                        .owner(userSaved)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        InstallmentPlanRequestDTO request = new InstallmentPlanRequestDTO(
                new BigDecimal("18000.00"),
                12,
                LocalDate.of(2026, 7, 1),
                "MacBook Pro 14",
                categorySaved.getId()
        );

        MvcResult createResult = mockMvc.perform(MockMvcRequestBuilders.post("/installment-plans")
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isAccepted())
                .andReturn();

        InstallmentPlanResponseDTO created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), InstallmentPlanResponseDTO.class);

        mockMvc.perform(MockMvcRequestBuilders.delete("/installment-plans/" + created.id())
                        .with(user(userPrincipal)))
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.delete("/installment-plans/" + created.id())
                        .with(user(userPrincipal)))
                .andExpect(MockMvcResultMatchers.status().isUnprocessableEntity())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), exception.status());
        assertTrue(exception.error().contains("already cancelled"));
    }

    @Test
    @DisplayName("Should return 404 when plan to cancel does not exist")
    void cancel_WhenPlanDoesNotExist_ShouldReturn404() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.delete("/installment-plans/" + UUID.randomUUID())
                        .with(user(userPrincipal)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 404 when cancelling a plan that belongs to another user")
    void cancel_WhenPlanBelongsToAnotherUser_ShouldReturn404() throws Exception {
        User owner = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        User other = userService.save(
                User.builder()
                        .name("Other")
                        .email("other@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        Category categorySaved = categoryRepository.save(
                Category.builder()
                        .name("Technology")
                        .description("Electronics and gadgets")
                        .icon("tech")
                        .owner(owner)
                        .build()
        );

        UserPrincipal ownerPrincipal = new UserPrincipal(owner);
        UserPrincipal otherPrincipal = new UserPrincipal(other);

        InstallmentPlanRequestDTO request = new InstallmentPlanRequestDTO(
                new BigDecimal("18000.00"),
                12,
                LocalDate.of(2026, 7, 1),
                "MacBook Pro 14",
                categorySaved.getId()
        );

        MvcResult createResult = mockMvc.perform(MockMvcRequestBuilders.post("/installment-plans")
                        .with(user(ownerPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isAccepted())
                .andReturn();

        InstallmentPlanResponseDTO created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), InstallmentPlanResponseDTO.class);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.delete("/installment-plans/" + created.id())
                        .with(user(otherPrincipal)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
        assertEquals(12, expenseRepository.count());
    }

    @Test
    @DisplayName("Should return 400 when totalAmount is not positive")
    void create_WhenTotalAmountIsNotPositive_ShouldReturnBadRequest() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        InstallmentPlanRequestDTO request = new InstallmentPlanRequestDTO(
                BigDecimal.ZERO,
                12,
                LocalDate.of(2026, 7, 1),
                "MacBook Pro 14",
                UUID.randomUUID()
        );

        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/installment-plans")
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        StandardException exception = objectMapper.readValue(content, StandardException.class);

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.status());
    }
}
