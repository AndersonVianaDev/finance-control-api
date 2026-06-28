package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.ExpenseRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.IncomeRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.ExpenseResponseDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.IncomeResponseDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.PageResponseDTO;
import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.Income;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.enums.UserRole;
import com.andersonvianadev.finance_control_api.domain.services.IUserService;
import com.andersonvianadev.finance_control_api.infra.exceptions.StandardException;
import com.andersonvianadev.finance_control_api.infra.repositories.BudgetRepository;
import com.andersonvianadev.finance_control_api.infra.repositories.CategoryRepository;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
class IncomeControllerTest {

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
    private IncomeRepository incomeRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private InstallmentPlanRepository installmentPlanRepository;

    @Autowired
    private RecurringRuleRepository recurringRuleRepository;

    @BeforeEach
    void setup() {
        incomeRepository.deleteAll();
        expenseRepository.deleteAll();
        installmentPlanRepository.deleteAll();
        budgetRepository.deleteAll();
        recurringRuleRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create income successfully and return 201")
    void save_WhenValidIncome_ShouldReturn201() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);

        IncomeRequestDTO request = new IncomeRequestDTO(
                LocalDateTime.of(2026, 6, 5, 0, 0),
                new BigDecimal("5000.00"),
                "Monthly salary",
                category.getId()
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/incomes")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        IncomeResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), IncomeResponseDTO.class);

        assertNotNull(response.id());
        assertEquals(request.description(), response.description());
        assertEquals(0, request.price().compareTo(response.price()));
        assertEquals(category.getId(), response.category().id());
    }

    @Test
    @DisplayName("Should return 409 when a duplicate income is submitted")
    void save_WhenDuplicateIncome_ShouldReturn409() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);

        createIncome(user, category, LocalDateTime.of(2026, 6, 5, 0, 0),
                new BigDecimal("5000.00"), "Monthly salary");

        IncomeRequestDTO request = new IncomeRequestDTO(
                LocalDateTime.of(2026, 6, 5, 0, 0),
                new BigDecimal("5000.00"),
                "Monthly salary",
                category.getId()
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/incomes")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);
        assertEquals(HttpStatus.CONFLICT.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 404 when category does not exist")
    void save_WhenCategoryNotFound_ShouldReturn404() throws Exception {
        User user = createUser();
        UserPrincipal principal = new UserPrincipal(user);

        IncomeRequestDTO request = new IncomeRequestDTO(
                LocalDateTime.of(2026, 6, 5, 0, 0),
                new BigDecimal("5000.00"),
                "Monthly salary",
                UUID.randomUUID()
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/incomes")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);
        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 400 when categoryId is null")
    void save_WhenCategoryIdIsNull_ShouldReturn400() throws Exception {
        User user = createUser();
        UserPrincipal principal = new UserPrincipal(user);

        String body = """
                {
                    "transactionDate": "2026-06-05T00:00:00",
                    "price": 5000.00,
                    "description": "Monthly salary"
                }
                """;

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/incomes")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);
        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 400 when price is null")
    void save_WhenPriceIsNull_ShouldReturn400() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);

        String body = """
                {
                    "transactionDate": "2026-06-05T00:00:00",
                    "description": "Monthly salary",
                    "categoryId": "%s"
                }
                """.formatted(category.getId());

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/incomes")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);
        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 400 when price is negative")
    void save_WhenPriceIsNegative_ShouldReturn400() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);

        IncomeRequestDTO request = new IncomeRequestDTO(
                LocalDateTime.of(2026, 6, 5, 0, 0),
                new BigDecimal("-100.00"),
                "Monthly salary",
                category.getId()
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/incomes")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);
        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 400 when description is blank")
    void save_WhenDescriptionIsBlank_ShouldReturn400() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);

        IncomeRequestDTO request = new IncomeRequestDTO(
                LocalDateTime.of(2026, 6, 5, 0, 0),
                new BigDecimal("5000.00"),
                "   ",
                category.getId()
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/incomes")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);
        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 403 when request is unauthenticated")
    void save_WhenUnauthenticated_ShouldReturn403() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    @DisplayName("Should return 200 with income when ID exists and belongs to user")
    void findById_WhenIncomeExistsAndBelongsToUser_ShouldReturnOk() throws Exception {
        User owner = createUser();
        UserPrincipal principal = new UserPrincipal(owner);

        Category category = createCategory(owner, "Salary");
        Income income = createIncome(owner, category, LocalDateTime.now(), new BigDecimal(4250), "Salario mensal");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/incomes/" + income.getId())
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        IncomeResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                IncomeResponseDTO.class
        );

        assertEquals(income.getId(), response.id());
        assertEquals(income.getOwner(), owner);
    }

    @Test
    @DisplayName("Should return 404 when income ID does not exist")
    void findById_WhenIncomeDoesNotExist_ShouldReturnNotFound() throws Exception {
        User owner = createUser();
        UserPrincipal principal = new UserPrincipal(owner);
        UUID incomeId = UUID.randomUUID();

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/incomes/"+incomeId)
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                StandardException.class
        );

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    @Test
    @DisplayName("Should return paginated incomes for authenticated user")
    void findAll_WhenUserHasIncomes_ShouldReturnPaginatedResults() throws Exception {
        User owner = createUser();
        UserPrincipal principal = new UserPrincipal(owner);

        Category category = createCategory(owner, "Salary");

        for (int i = 1; i <= 3; i++) {
            createIncome(
                    owner,
                    category,
                    LocalDateTime.now().plusMonths(i),
                    new BigDecimal(4500),
                    "salario mensal"
            );
        }

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/incomes")
                        .with(user(principal))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        JavaType pageType = objectMapper.getTypeFactory()
                .constructParametricType(PageResponseDTO.class, IncomeResponseDTO.class);
        PageResponseDTO<IncomeResponseDTO> response = objectMapper.readValue(
                result.getResponse().getContentAsString(), pageType);

        assertEquals(3, response.totalElement());
        assertEquals(3, response.content().size());
        assertEquals(0, response.page());
        assertEquals(1, response.totalPages());
        assertTrue(response.last());
    }

    @Test
    @DisplayName("Should return empty page when user has no incomes")
    void findAll_WhenUserHasNoIncomes_ShouldReturnEmptyPage() throws Exception {
        User user = createUser();
        UserPrincipal principal = new UserPrincipal(user);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/incomes")
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        JavaType pageType = objectMapper.getTypeFactory()
                .constructParametricType(PageResponseDTO.class, IncomeResponseDTO.class);
        PageResponseDTO<IncomeResponseDTO> response = objectMapper.readValue(
                result.getResponse().getContentAsString(), pageType);

        assertEquals(0, response.totalElement());
        assertTrue(response.content().isEmpty());
        assertTrue(response.last());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User createUser() {
        return userService.save(User.builder()
                .name("Anderson")
                .email("anderson@gmail.com")
                .password("Arthur@1406")
                .role(UserRole.ROLE_USER)
                .build());
    }

    private Category createCategory(User owner, String name) {
        return categoryRepository.save(Category.builder()
                .name(name)
                .description("Test category")
                .icon("icon")
                .owner(owner)
                .build());
    }

    private Income createIncome(User owner, Category category, LocalDateTime date,
                                BigDecimal price, String description) {
        return incomeRepository.save(Income.builder()
                .owner(owner)
                .category(category)
                .transactionDate(date)
                .price(price)
                .description(description)
                .build());
    }
}
