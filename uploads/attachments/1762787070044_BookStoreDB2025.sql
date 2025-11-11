-- =============================================
USE master
GO
CREATE DATABASE HSF302;
GO
USE HSF302;
GO





INSERT INTO user_accounts (username, password, role) VALUES
('admin@bookstore.com', '@1', 'Admin'),
('staff@bookstore.com', '@2', 'Staff'),
('member@bookstore.com', '@3', 'Member');
GO
