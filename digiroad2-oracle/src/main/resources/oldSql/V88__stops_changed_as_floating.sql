
--Insert two new not visible fields at form MassTransitSop
INSERT INTO PROPERTY (ID, ASSET_TYPE_ID, PROPERTY_TYPE, REQUIRED, CREATED_BY, PUBLIC_ID) VALUES
                                            (primary_key_seq.nextval, 10, 'read_only_number', 0, 'db_migration_v88', 'linkin_hallinnollinen_luokka');
INSERT INTO PROPERTY (ID, ASSET_TYPE_ID, PROPERTY_TYPE, REQUIRED, CREATED_BY, PUBLIC_ID) VALUES
                                            (primary_key_seq.nextval, 10, 'read_only_number', 0, 'db_migration_v88', 'kellumisen_syy');
