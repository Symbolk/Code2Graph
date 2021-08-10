SELECT LastName,FirstName FROM Persons;
SELECT * FROM Persons;
SELECT * FROM Persons WHERE City='Beijing';
SELECT * FROM Persons WHERE (FirstName='Thomas' OR FirstName='William')
AND LastName='Carter';
INSERT INTO Persons (LastName, Address) VALUES ('Wilson', 'Champs-Elysees');
UPDATE Person SET Address = 'Zhongshan 23', City = 'Nanjing'
WHERE LastName = 'Wilson';
DELETE FROM Person WHERE LastName = 'Wilson';