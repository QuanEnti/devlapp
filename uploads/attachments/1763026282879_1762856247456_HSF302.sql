-- =============================================
USE master
GO
CREATE DATABASE HSF302;
GO
USE HSF302;
GO

-- Users Table
CREATE TABLE user_accounts (
    id INT IDENTITY(1,1) PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL
);
GO


-- Insert Sample Data
INSERT INTO ususer_accounts(username, password, role) VALUES
('admin@bookstore.com', '@1', 'Admin'),
('staff@bookstore.com', '@2', 'Staff'),
('member@bookstore.com', '@3', 'Member');
GO
