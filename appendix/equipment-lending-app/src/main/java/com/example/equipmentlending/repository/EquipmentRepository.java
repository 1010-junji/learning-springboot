package com.example.equipmentlending.repository;

import com.example.equipmentlending.entity.Equipment;
import com.example.equipmentlending.entity.EquipmentStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class EquipmentRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Equipment> equipmentRowMapper = (rs, rowNum) -> new Equipment(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("category"),
            EquipmentStatus.valueOf(rs.getString("status"))
    );

    public EquipmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Equipment> findAll() {
        return jdbcTemplate.query(
                "SELECT id, name, category, status FROM equipments ORDER BY id",
                equipmentRowMapper
        );
    }

    public Optional<Equipment> findById(Long id) {
        List<Equipment> equipments = jdbcTemplate.query(
                "SELECT id, name, category, status FROM equipments WHERE id = ?",
                equipmentRowMapper,
                id
        );
        return equipments.stream().findFirst();
    }

    public Equipment save(String name, String category) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO equipments (name, category, status) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, name);
            statement.setString(2, category);
            statement.setString(3, EquipmentStatus.AVAILABLE.name());
            return statement;
        }, keyHolder);

        Number generatedId = keyHolder.getKey();
        Long id = generatedId == null ? null : generatedId.longValue();
        return findById(id).orElseThrow();
    }

    public void updateStatus(Long id, EquipmentStatus status) {
        jdbcTemplate.update(
                "UPDATE equipments SET status = ? WHERE id = ?",
                status.name(),
                id
        );
    }
}
