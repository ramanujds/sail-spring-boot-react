-- Sample rows so GET /api/messages returns data straight away.

INSERT INTO MESSAGES (ID, MESSAGE_TEXT) VALUES (NEXT VALUE FOR MESSAGES_SEQ, 'Hello from H2');
INSERT INTO MESSAGES (ID, MESSAGE_TEXT) VALUES (NEXT VALUE FOR MESSAGES_SEQ, 'Seeded by data.sql');
