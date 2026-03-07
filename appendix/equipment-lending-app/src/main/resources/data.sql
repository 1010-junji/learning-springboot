INSERT INTO equipments (name, category, status) VALUES
    ('WindowsノートPC', 'PC', 'AVAILABLE'),
    ('iPad', 'タブレット', 'LENT'),
    ('プロジェクター', '会議室機材', 'AVAILABLE');

INSERT INTO rental_records (equipment_id, user_name, rented_at, returned_at) VALUES
    (2, '佐藤花子', TIMESTAMP '2026-03-01 09:30:00', NULL),
    (1, '高橋健太', TIMESTAMP '2026-02-20 10:00:00', TIMESTAMP '2026-02-20 17:30:00');
