WILDCARD ly tu dai dien
--[] dai dien cho mot hoac bat ky nao co trong ngoac vuong
--h[oa]t finds hot and hat , but not hit
--^ nguoc lai ko chua cac ky tu
-- - dai dien cho mot khoang ky tu 
--c[a-b]t finds cat 

--Hay loc ra tat ca tat ca khach hang co ten lien he bat dau bang chu 'A'
SELECT *
FROM dbo.Customers
WHERE ContactName LIKE 'A%';

-- hay loc ra tat ca khach hang bat dau bang chu h
--va co ky tu thu 2
SELECT *
FROM dbo.Customers
WHERE ContactName LIKE 'H_%';
--Hay loc ra tat ca don hang duoc gui den  thanh pho 
--co chu cai bat dau la l, chu cai thu hai la u hoac o
SELECT *
FROM dbo.Orders
WHERE  ShipCity LIKE 'L[u,o]%';
--Hay loc ra tat ca don hang duoc gui den  thanh pho 
--co chu cai bat dau la l, chu cai thu hai ko la u hoac o
SELECT *
FROM dbo.Orders
WHERE  ShipCity LIKE 'L[^u,o]%';
--Hay loc ra tat ca don hang duoc gui den cac thanh pho co chu cai bat 
--dau la l , chu cai thu hai la cac ky tu tu a den e
SELECT OrderID ,ShipCity
FROM dbo.Orders
WHERE  ShipCity LIKE 'L[a-e]%';

select *
from dbo.Suppliers
where CompanyName LIKE 'A%[^b]%';
SELECT *
FROM [Suppliers]
WHERE [CompanyName] Like 'A%' And [CompanyName] Not Like '%b%';
