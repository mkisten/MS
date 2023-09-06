package com.itm.space.backendresources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.service.UserService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    @SneakyThrows
    @WithMockUser(roles = "MODERATOR")
    public void helloTest() {
        MockHttpServletResponse response = mvc.perform(get("/api/users/hello"))
                .andReturn()
                .getResponse();
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertEquals("user", response.getContentAsString());
    }
    @Test
    @WithMockUser(roles = "MODERATOR")
    public void testCreateUser() throws Exception {
        UserRequest userRequest = new UserRequest("testuser", "test@example.com", "password", "John", "Doe");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    public void testGetUserById() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse userResponse = new UserResponse("John", "Doe", "test@example.com", Collections.emptyList(), Collections.emptyList());

        when(userService.getUserById(userId)).thenReturn(userResponse);

        mockMvc.perform(get("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }
    @Test
    @WithMockUser(roles = "MODERATOR")
    public void testCreateUserWithInvalidPassword() throws Exception {
        // Создайте объект UserRequest с недопустимым паролем (менее 4 символов)
        UserRequest userRequest = new UserRequest("testuser", "test@example.com", "123", "John", "Doe");

        // Настройте UserService так, чтобы он выбрасывал исключение при вызове createUser с таким запросом
        doThrow(new IllegalArgumentException("Invalid password")).when(userService).createUser(userRequest);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isBadRequest());
    }
    @Test
    @WithMockUser(roles = "MODERATOR")
    void testGetUserByIdWithInvalidId() throws Exception {
        UUID invalidUserId = UUID.randomUUID();

        mockMvc.perform(get("/api/users/{id}", invalidUserId))
                .andExpect(status().isOk()) // Ожидаем статус 200
                .andReturn();
    }
    @Test
    @WithMockUser(roles = "MODERATOR")
    public void createUserInvalidTest() throws Exception {
        // Отправляем POST-запрос с невалидными данными
        MockHttpServletResponse response = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/users")
                        .contentType("application/json")
                        .content("{\"username\":\"\", \"email\":\"invalid_email\", \"password\":\"123q123e\", \"firstName\":\"123\", \"lastName\":\"Абракадабра\"}")
                )
                .andDo(print()) // Выводить информацию о запросе и ответе в консоль (может быть удобно для отладки)
                .andExpect(status().isBadRequest()) // Ожидаем HTTP-статус 400 Bad Request
                .andExpect(jsonPath("$.email").value("Email should be valid")) // Ожидаем соответствующее сообщение об ошибке для поля email
                .andReturn()
                .getResponse();

        // Проверяем, что статус ответа соответствует ожидаемому
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
    }
}