package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.IncomeRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.requests.IncomeUpdateDTO;
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

    // ── save ─────────────────────────────────────────────────────────────────

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

    // ── findById ─────────────────────────────────────────────────────────────

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
    @DisplayName("Should return 404 when income belongs to another user")
    void findById_WhenIncomeBelongsToAnotherUser_ShouldReturnNotFound() throws Exception {
        User owner = createUser("anderson@gmail.com");
        User other = createUser("other@gmail.com");
        Category category = createCategory(owner, "salary");
        UserPrincipal otherPrincipal = new UserPrincipal(other);

        Income income = createIncome(owner, category, LocalDateTime.now(), new BigDecimal("5000.00"), "Monthly salary");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/incomes/{id}", income.getId())
                        .with(user(otherPrincipal)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    // ── findAll ──────────────────────────────────────────────────────────────

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

    @Test
    @DisplayName("Should return only incomes of the authenticated user")
    void findAll_ShouldOnlyReturnIncomesOfAuthenticatedUser() throws Exception {
        User owner = createUser("anderson@gmail.com");
        User other = createUser("other@gmail.com");
        Category ownerCategory = createCategory(owner, "salary");
        Category otherCategory = createCategory(other, "salary");
        UserPrincipal ownerPrincipal = new UserPrincipal(owner);

        createIncome(owner, ownerCategory, LocalDateTime.of(2026, 6, 5, 0, 0), new BigDecimal("5000.00"), "Owner salary");
        createIncome(other, otherCategory, LocalDateTime.of(2026, 6, 5, 0, 0), new BigDecimal("3000.00"), "Other salary");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/incomes")
                        .with(user(ownerPrincipal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        JavaType pageType = objectMapper.getTypeFactory()
                .constructParametricType(PageResponseDTO.class, IncomeResponseDTO.class);
        PageResponseDTO<IncomeResponseDTO> response = objectMapper.readValue(
                result.getResponse().getContentAsString(), pageType);

        assertEquals(1, response.totalElement());
        assertEquals(1, response.content().size());
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should delete income and return 204 when income exists and belongs to user")
    void delete_WhenIncomeExistsAndBelongsToUser_ShouldReturn204() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);

        Income income = createIncome(user, category, LocalDateTime.of(2026, 6, 5, 0, 0),
                new BigDecimal("5000.00"), "Monthly salary");

        mockMvc.perform(MockMvcRequestBuilders.delete("/incomes/{id}", income.getId())
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isNoContent())
                .andDo(MockMvcResultHandlers.print());

        assertEquals(0, incomeRepository.count());
    }

    @Test
    @DisplayName("Should return 404 when income to delete does not exist")
    void delete_WhenIncomeDoesNotExist_ShouldReturn404() throws Exception {
        User user = createUser();
        UserPrincipal principal = new UserPrincipal(user);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.delete("/incomes/{id}", UUID.randomUUID())
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 404 when income to delete belongs to another user")
    void delete_WhenIncomeBelongsToAnotherUser_ShouldReturn404() throws Exception {
        User owner = createUser("anderson@gmail.com");
        User other = createUser("other@gmail.com");
        Category category = createCategory(owner, "salary");
        UserPrincipal otherPrincipal = new UserPrincipal(other);

        Income income = createIncome(owner, category, LocalDateTime.of(2026, 6, 5, 0, 0),
                new BigDecimal("5000.00"), "Monthly salary");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.delete("/incomes/{id}", income.getId())
                        .with(user(otherPrincipal)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
        assertEquals(1, incomeRepository.count());
    }

    @Test
    @DisplayName("Should return 403 when delete request is unauthenticated")
    void delete_WhenUnauthenticated_ShouldReturn403() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/incomes/{id}", UUID.randomUUID()))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andDo(MockMvcResultHandlers.print());
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 200 with updated fields when request is valid")
    void update_WhenRequestIsValid_ShouldReturn200WithUpdatedIncome() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);

        Income income = createIncome(user, category, LocalDateTime.of(2026, 6, 5, 0, 0),
                new BigDecimal("5000.00"), "Monthly salary");

        IncomeUpdateDTO updateRequest = new IncomeUpdateDTO(
                new BigDecimal("5500.00"), "Updated monthly salary", null, null);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/incomes/{id}", income.getId())
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        IncomeResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), IncomeResponseDTO.class);

        assertEquals(income.getId(), response.id());
        assertEquals(0, new BigDecimal("5500.00").compareTo(response.price()));
        assertEquals("Updated monthly salary", response.description());
        assertEquals(category.getId(), response.category().id());
    }

    @Test
    @DisplayName("Should return 200 with new category when category is changed")
    void update_WhenCategoryChanged_ShouldReturn200WithNewCategory() throws Exception {
        User user = createUser();
        Category categoryA = createCategory(user, "salary");
        Category categoryB = categoryRepository.save(Category.builder()
                .name("freelance").description("Freelance work").icon("code").owner(user).build());
        UserPrincipal principal = new UserPrincipal(user);

        Income income = createIncome(user, categoryA, LocalDateTime.of(2026, 6, 5, 0, 0),
                new BigDecimal("5000.00"), "Monthly salary");

        IncomeUpdateDTO updateRequest = new IncomeUpdateDTO(null, null, null, categoryB.getId());

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/incomes/{id}", income.getId())
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        IncomeResponseDTO response = objectMapper.readValue(
                result.getResponse().getContentAsString(), IncomeResponseDTO.class);

        assertEquals(categoryB.getId(), response.category().id());
    }

    @Test
    @DisplayName("Should return 404 when income to update does not exist")
    void update_WhenIncomeDoesNotExist_ShouldReturn404() throws Exception {
        User user = createUser();
        UserPrincipal principal = new UserPrincipal(user);

        IncomeUpdateDTO updateRequest = new IncomeUpdateDTO(
                new BigDecimal("5500.00"), null, null, null);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/incomes/{id}", UUID.randomUUID())
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 404 when income to update belongs to another user")
    void update_WhenIncomeBelongsToAnotherUser_ShouldReturn404() throws Exception {
        User owner = createUser("anderson@gmail.com");
        User other = createUser("other@gmail.com");
        Category category = createCategory(owner, "salary");
        UserPrincipal otherPrincipal = new UserPrincipal(other);

        Income income = createIncome(owner, category, LocalDateTime.of(2026, 6, 5, 0, 0),
                new BigDecimal("5000.00"), "Monthly salary");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/incomes/{id}", income.getId())
                        .with(user(otherPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new IncomeUpdateDTO(null, "Hacked", null, null))))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 404 when new category does not exist")
    void update_WhenNewCategoryDoesNotExist_ShouldReturn404() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);

        Income income = createIncome(user, category, LocalDateTime.of(2026, 6, 5, 0, 0),
                new BigDecimal("5000.00"), "Monthly salary");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/incomes/{id}", income.getId())
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new IncomeUpdateDTO(null, null, null, UUID.randomUUID()))))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 409 when updated values conflict with another existing income")
    void update_WhenResultIsDuplicate_ShouldReturn409() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);

        createIncome(user, category, LocalDateTime.of(2026, 6, 5, 0, 0),
                new BigDecimal("5000.00"), "Monthly salary");

        Income income2 = createIncome(user, category, LocalDateTime.of(2026, 7, 5, 0, 0),
                new BigDecimal("4500.00"), "Different salary");

        IncomeUpdateDTO updateRequest = new IncomeUpdateDTO(
                new BigDecimal("5000.00"),
                "Monthly salary",
                LocalDateTime.of(2026, 6, 5, 0, 0),
                null
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/incomes/{id}", income2.getId())
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.CONFLICT.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 400 when price is negative on update")
    void update_WhenPriceIsNegative_ShouldReturn400() throws Exception {
        User user = createUser();
        UserPrincipal principal = new UserPrincipal(user);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/incomes/{id}", UUID.randomUUID())
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new IncomeUpdateDTO(new BigDecimal("-50.00"), null, null, null))))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 400 when description is empty string on update")
    void update_WhenDescriptionIsEmpty_ShouldReturn400() throws Exception {
        User user = createUser();
        UserPrincipal principal = new UserPrincipal(user);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/incomes/{id}", UUID.randomUUID())
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new IncomeUpdateDTO(null, "", null, null))))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        StandardException exception = objectMapper.readValue(
                result.getResponse().getContentAsString(), StandardException.class);

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.status());
    }

    @Test
    @DisplayName("Should return 403 when update request is unauthenticated")
    void update_WhenUnauthenticated_ShouldReturn403() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/incomes/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andDo(MockMvcResultHandlers.print());
    }

    // ── findBetweenTransactionDate ────────────────────────────────────────────

    @Test
    @DisplayName("Should return only incomes whose transactionDate falls within the given date range")
    void findBetweenTransactionDate_WhenIncomesExistInRange_ShouldReturnOnlyMatchingIncomes() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);

        createIncome(user, category, LocalDateTime.of(2026, 6, 5, 0, 0), new BigDecimal("5000.00"), "June salary");
        createIncome(user, category, LocalDateTime.of(2026, 6, 20, 0, 0), new BigDecimal("1000.00"), "June bonus");
        createIncome(user, category, LocalDateTime.of(2026, 8, 5, 0, 0), new BigDecimal("5000.00"), "August salary");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/incomes")
                        .param("start", "2026-06-01")
                        .param("finish", "2026-06-30")
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        JavaType pageType = objectMapper.getTypeFactory()
                .constructParametricType(PageResponseDTO.class, IncomeResponseDTO.class);
        PageResponseDTO<IncomeResponseDTO> response = objectMapper.readValue(
                result.getResponse().getContentAsString(), pageType);

        assertEquals(2, response.totalElement());
        assertEquals(2, response.content().size());
    }

    @Test
    @DisplayName("Should include incomes registered at any hour on the finish date")
    void findBetweenTransactionDate_WhenFinishDayIsFullyInclusive_ShouldIncludeIncomesAtAnyHour() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);

        createIncome(user, category, LocalDateTime.of(2026, 6, 30, 23, 59), new BigDecimal("5000.00"), "Late night income on finish day");
        createIncome(user, category, LocalDateTime.of(2026, 7, 1, 9, 0), new BigDecimal("1000.00"), "Income the day after finish");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/incomes")
                        .param("start", "2026-06-01")
                        .param("finish", "2026-06-30")
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        JavaType pageType = objectMapper.getTypeFactory()
                .constructParametricType(PageResponseDTO.class, IncomeResponseDTO.class);
        PageResponseDTO<IncomeResponseDTO> response = objectMapper.readValue(
                result.getResponse().getContentAsString(), pageType);

        assertEquals(1, response.totalElement());
        assertEquals("Late night income on finish day", response.content().get(0).description());
    }

    @Test
    @DisplayName("Should return empty page when no incomes fall within the given date range")
    void findBetweenTransactionDate_WhenNoIncomesInRange_ShouldReturnEmptyPage() throws Exception {
        User user = createUser();
        Category category = createCategory(user, "salary");
        UserPrincipal principal = new UserPrincipal(user);

        createIncome(user, category, LocalDateTime.of(2026, 9, 1, 0, 0), new BigDecimal("5000.00"), "September salary");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/incomes")
                        .param("start", "2026-06-01")
                        .param("finish", "2026-06-30")
                        .with(user(principal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        JavaType pageType = objectMapper.getTypeFactory()
                .constructParametricType(PageResponseDTO.class, IncomeResponseDTO.class);
        PageResponseDTO<IncomeResponseDTO> response = objectMapper.readValue(
                result.getResponse().getContentAsString(), pageType);

        assertEquals(0, response.totalElement());
        assertTrue(response.content().isEmpty());
    }

    @Test
    @DisplayName("Should not return incomes belonging to another user even if they fall within the range")
    void findBetweenTransactionDate_WhenIncomesBelongToAnotherUser_ShouldReturnEmptyPage() throws Exception {
        User owner = createUser("anderson@gmail.com");
        User other = createUser("other@gmail.com");
        Category ownerCategory = createCategory(owner, "salary");
        Category otherCategory = createCategory(other, "salary");
        UserPrincipal otherPrincipal = new UserPrincipal(other);

        createIncome(owner, ownerCategory, LocalDateTime.of(2026, 6, 15, 0, 0), new BigDecimal("5000.00"), "Owner salary in range");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/incomes")
                        .param("start", "2026-06-01")
                        .param("finish", "2026-06-30")
                        .with(user(otherPrincipal)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        JavaType pageType = objectMapper.getTypeFactory()
                .constructParametricType(PageResponseDTO.class, IncomeResponseDTO.class);
        PageResponseDTO<IncomeResponseDTO> response = objectMapper.readValue(
                result.getResponse().getContentAsString(), pageType);

        assertEquals(0, response.totalElement());
        assertTrue(response.content().isEmpty());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User createUser() {
        return createUser("anderson@gmail.com");
    }

    private User createUser(String email) {
        return userService.save(User.builder()
                .name("Anderson")
                .email(email)
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
