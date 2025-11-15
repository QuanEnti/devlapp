--hay ra tat ca don hang voi dieu kien
--ShipCountry LIKE'U_'
--ShipCountry LIKE'U%'
SELECT *
FROM dbo.Orders
WHERE ShipCountry LIKE 'U_A' --_dai dien cho mot ky tu
SELECT *
FROM dbo.Orders
WHERE ShipCountry LIKE 'U%A'
--hay lay ra tat ca cac nha cung cap khach hang co chu 'b' trong ten cong ty
SELECT *
FROM dbo.Suppliers
WHERE CompanyName LIKE '%b%';