ALTER TABLE MANOEUVRE
  ADD CREATED_BY VARCHAR2(128)
  ADD CREATED_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL

ALTER TABLE MANOEUVRE MODIFY MODIFIED_DATE DEFAULT (null);

--Copy all modified user info to create column
UPDATE MANOEUVRE
SET CREATED_BY = MODIFIED_BY
, CREATED_DATE = MODIFIED_DATE