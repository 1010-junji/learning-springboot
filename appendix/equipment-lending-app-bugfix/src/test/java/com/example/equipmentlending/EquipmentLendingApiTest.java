package com.example.equipmentlending;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EquipmentLendingApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.update("DELETE FROM rental_records");
        jdbcTemplate.update("DELETE FROM equipments");

        jdbcTemplate.update(
                "INSERT INTO equipments (id, name, category, status) VALUES (?, ?, ?, ?)",
                1L, "WindowsノートPC", "PC", "AVAILABLE"
        );
        jdbcTemplate.update(
                "INSERT INTO equipments (id, name, category, status) VALUES (?, ?, ?, ?)",
                2L, "iPad", "タブレット", "LENT"
        );
        jdbcTemplate.update(
                "INSERT INTO rental_records (id, equipment_id, user_name, rented_at, returned_at) VALUES (?, ?, ?, TIMESTAMP '2026-03-01 09:30:00', NULL)",
                1L, 2L, "佐藤花子"
        );
    }

    @Test
    void shouldGetEquipmentList() throws Exception {
        mockMvc.perform(get("/api/equipments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("WindowsノートPC"))
                .andExpect(jsonPath("$[1].status").value("LENT"));
    }

    @Test
    void shouldCreateEquipment() throws Exception {
        mockMvc.perform(post("/api/equipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Webカメラ",
                                  "category": "周辺機器"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Webカメラ"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void shouldLendAndReturnEquipment() throws Exception {
        mockMvc.perform(post("/api/equipments/1/lend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userName": "山田太郎"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LENT"));

        mockMvc.perform(post("/api/equipments/1/return"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void shouldRejectBlankUserName() throws Exception {
        mockMvc.perform(post("/api/equipments/1/lend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userName": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("貸出者名は必須です。"));
    }

    @Test
    void shouldRejectLendingAlreadyLentEquipment() throws Exception {
        mockMvc.perform(post("/api/equipments/2/lend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userName": "山田太郎"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("この備品はすでに貸出中です。"));
    }

    @Test
    void shouldReturnNotFoundWhenEquipmentDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/equipments/999/lend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userName": "山田太郎"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("ID 999 の備品は存在しません。"));
    }
}

