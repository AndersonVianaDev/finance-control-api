package com.andersonvianadev.finance_control_api.controllers;

import com.andersonvianadev.finance_control_api.controllers.dtos.requests.CategoryRequestDTO;
import com.andersonvianadev.finance_control_api.controllers.dtos.responses.CategoryResponseDTO;
import com.andersonvianadev.finance_control_api.domain.models.Category;
import com.andersonvianadev.finance_control_api.domain.models.User;
import com.andersonvianadev.finance_control_api.domain.models.enums.UserRole;
import com.andersonvianadev.finance_control_api.domain.services.IUserService;
import com.andersonvianadev.finance_control_api.infra.exceptions.StandardException;
import com.andersonvianadev.finance_control_api.infra.repositories.CategoryRepository;
import com.andersonvianadev.finance_control_api.infra.repositories.UserRepository;
import com.andersonvianadev.finance_control_api.infra.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
public class CategoryControllerTest {

    @Autowired
    private CategoryRepository repository;

    @Autowired
    private IUserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        repository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save category successfully when category is valid")
    void save_WhenCategoryIsValid_ShouldReturnSavedCategory() throws Exception{
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        CategoryRequestDTO request = new CategoryRequestDTO("Food", "Food description", "food");

        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/categories")
                .with(user(userPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        CategoryResponseDTO response = objectMapper.readValue(content, CategoryResponseDTO.class);

        assertNotNull(response);
        assertEquals(userSaved.getId(), response.owner().id());
    }

    @Test
    @DisplayName("Should throw ResourceAlreadyExistsException when category name already exists")
    void save_WhenCategoryNameAlreadyExists_ShouldThrowResourceAlreadyExistsException() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        repository.save(
                Category.builder()
                        .name("Food")
                        .description("Food description")
                        .icon("food")
                        .owner(userSaved)
                .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        CategoryRequestDTO request = new CategoryRequestDTO("food", "Food description", "food");

        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/categories")
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        StandardException response = objectMapper.readValue(content, StandardException.class);

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT.value(), response.status());
    }

    @Test
    @DisplayName("Should throw QuotaExceededException when free plan category limit is reached")
    void save_WhenFreePlanCategoryLimitIsReached_ShouldThrowQuotaExceededException() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        repository.save(
                Category.builder()
                        .name("Food")
                        .description("Food description")
                        .icon("food")
                        .owner(userSaved)
                        .build()
        );

        repository.save(
                Category.builder()
                        .name("Fitness")
                        .description("Fitness description")
                        .icon("fitness")
                        .owner(userSaved)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        CategoryRequestDTO request = new CategoryRequestDTO("Home", "Home description", "home");

        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/categories")
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        StandardException response = objectMapper.readValue(content, StandardException.class);

        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN.value(), response.status());
    }

    @Test
    @DisplayName("Should throw MethodArgumentNotValidException when field invalid")
    void save_ShouldThrowMethodArgumentNotValidException_WhenFieldInvalid() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        CategoryRequestDTO request = new CategoryRequestDTO("Food",
                "Food description dsdsjdjsdjsjd sdjsjdsjdjsjds sdjsjdsjd sjdsjdsjdjs dsjdsjdsjds",
                "food");

        String requestJson = objectMapper.writeValueAsString(request);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/categories")
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        StandardException exception = objectMapper.readValue(content, StandardException.class);

        assertNotNull(exception);
        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.status());
    }

    @Test
    @DisplayName("Should return category when a valid ID is provided")
    void findById_WhenCategoryExists_ShouldReturnCategory() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        Category categorySaved = repository.save(
                Category.builder()
                        .name("Food")
                        .description("Food description")
                        .icon("food")
                        .owner(userSaved)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/categories/"+categorySaved.getId())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        CategoryResponseDTO response = objectMapper.readValue(content, CategoryResponseDTO.class);

        assertNotNull(response);
        assertEquals(userSaved.getId(), response.owner().id());
    }

    @Test
    @DisplayName("Should throw NotFoundException when category does not exist")
    void findById_WhenCategoryDoesNotExist_ShouldThrowNotFoundException() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/categories/"+ UUID.randomUUID())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        StandardException exception = objectMapper.readValue(content, StandardException.class);

        assertNotNull(exception);
        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }

    @Test
    @DisplayName("Should delete category successfully when category exists")
    void delete_WhenCategoryExists_ShouldDeleteCategory() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        Category categorySaved = repository.save(
                Category.builder()
                        .name("Food")
                        .description("Food description")
                        .icon("food")
                        .owner(userSaved)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        mockMvc.perform(MockMvcRequestBuilders.delete("/categories/" + categorySaved.getId())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNoContent())
                .andDo(MockMvcResultHandlers.print());

        assertEquals(0, repository.count());
    }

    @Test
    @DisplayName("Should throw NotFoundException when category does not exist")
    void delete_WhenCategoryDoesNotExist_ShouldThrowNotFoundException() throws Exception {
        User userSaved = userService.save(
                User.builder()
                        .name("Anderson")
                        .email("anderson@gmail.com")
                        .password("Arthur@1406")
                        .role(UserRole.ROLE_USER)
                        .build()
        );

        UserPrincipal userPrincipal = new UserPrincipal(userSaved);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.delete("/categories/" + UUID.randomUUID())
                        .with(user(userPrincipal))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();

        StandardException exception = objectMapper.readValue(content, StandardException.class);

        assertNotNull(exception);
        assertEquals(HttpStatus.NOT_FOUND.value(), exception.status());
    }
}
