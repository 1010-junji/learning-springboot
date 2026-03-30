package com.example.equipmentlending.repository;

import com.example.equipmentlending.entity.RentalRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class RentalRecordRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<RentalRecord> rentalRecordRowMapper = (rs, rowNum) -> new RentalRecord(
            rs.getLong("id"),
            rs.getLong("equipment_id"),
            rs.getString("user_name"),
            rs.getTimestamp("rented_at").toLocalDateTime(),
            rs.getTimestamp("returned_at") == null ? null : rs.getTimestamp("returned_at").toLocalDateTime()
    );

    public RentalRecordRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public RentalRecord save(Long equipmentId, String userName, LocalDateTime rentedAt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO rental_records (equipment_id, user_name, rented_at, returned_at) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, equipmentId);
            statement.setString(2, userName);
            statement.setTimestamp(3, Timestamp.valueOf(rentedAt));
            statement.setTimestamp(4, null);
            return statement;
        }, keyHolder);

        Number generatedId = keyHolder.getKey();
        Long id = generatedId == null ? null : generatedId.longValue();
        return findById(id).orElseThrow();
    }

    public Optional<RentalRecord> findById(Long id) {
        List<RentalRecord> records = jdbcTemplate.query(
                "SELECT id, equipment_id, user_name, rented_at, returned_at FROM rental_records WHERE id = ?",
                rentalRecordRowMapper,
                id
        );
        return records.stream().findFirst();
    }

    public Optional<RentalRecord> findActiveByEquipmentId(Long equipmentId) {
        List<RentalRecord> records = jdbcTemplate.query(
                "SELECT id, equipment_id, user_name, rented_at, returned_at FROM rental_records WHERE equipment_id = ? AND returned_at IS NULL ORDER BY rented_at DESC",
                rentalRecordRowMapper,
                equipmentId
        );
        return records.stream().findFirst();
    }

    public void markReturned(Long id, LocalDateTime returnedAt) {
        jdbcTemplate.update(
                "UPDATE rental_records SET returned_at = ? WHERE id = ?",
                Timestamp.valueOf(returnedAt),
                id
        );
    }

    public List<RentalRecord> findAll() {
        return jdbcTemplate.query(
                "SELECT id, equipment_id, user_name, rented_at, returned_at FROM rental_records ORDER BY rented_at DESC, id DESC",
                rentalRecordRowMapper
        );
    }
}
