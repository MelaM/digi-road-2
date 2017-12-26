ALTER TABLE ROAD_ADDRESS
  DROP CONSTRAINT CK_TERMINATION_END_DATE;

ALTER TABLE ROAD_ADDRESS
  ADD CONSTRAINT CK_TERMINATION_END_DATE CHECK (TERMINATED = 0 OR (TERMINATED IN (1,2) AND END_DATE IS NOT NULL));
