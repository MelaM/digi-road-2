-- ASSET_TYPE VALUE
INSERT INTO ASSET_TYPE (ID, NAME, GEOMETRY_TYPE, CREATED_BY)
	VALUES (480, 'Muut tiemerkinn�t', 'point', 'db_migration_v238');



-- LOCALIZED_STRING VALUE
-- Asetusnumero
INSERT INTO LOCALIZED_STRING (ID, VALUE_FI, CREATED_BY, CREATED_DATE)
	VALUES (primary_key_seq.nextval, 'Asetusnumero', 'db_migration_v238', SYSDATE);

-- Tiemerkinn�n esitt�m�n liikennemerkin luokka (pakollinen, jos merkint� kuvaa liikennemerkki�)
INSERT INTO LOCALIZED_STRING (ID, VALUE_FI, CREATED_BY, CREATED_DATE)
	VALUES (primary_key_seq.nextval, 'Tiemerkinn�n esitt�m�n liikennemerkin luokka', 'db_migration_v238', SYSDATE);

-- Tiemerkinn�n tai tiemerkinn�n esitt�m�n liikennemerkin arvo (pakollinen, jos merkinn�ll� on arvo)
INSERT INTO LOCALIZED_STRING (ID, VALUE_FI, CREATED_BY, CREATED_DATE)
	VALUES (primary_key_seq.nextval, 'Tiemerkinn�n tai tiemerkinn�n esitt�m�n liikennemerkin arvo', 'db_migration_v238', SYSDATE);
	
-- K��nn�s (pakollinen tiemerkinn�ille M1 ja M2)
INSERT INTO LOCALIZED_STRING (ID, VALUE_FI, CREATED_BY, CREATED_DATE)
	VALUES (primary_key_seq.nextval, 'K��nn�s', 'db_migration_v238', SYSDATE);
	
-- Suhteellinen sijainti (valinnainen)
INSERT INTO LOCALIZED_STRING (ID, VALUE_FI, CREATED_BY, CREATED_DATE)
	VALUES (primary_key_seq.nextval, 'Suhteellinen sijainti', 'db_migration_v238', SYSDATE);

-- M��r� (valinnainen)
INSERT INTO LOCALIZED_STRING (ID, VALUE_FI, CREATED_BY, CREATED_DATE)
	VALUES (primary_key_seq.nextval, 'M��r�', 'db_migration_v238', SYSDATE);

-- Merkinn�n materiaali (valinnainen, LOCALIZED_STRING-taulussa jo Merkin materiaali -value!!)
INSERT INTO LOCALIZED_STRING (ID, VALUE_FI, CREATED_BY, CREATED_DATE)
	VALUES (primary_key_seq.nextval, 'Merkinn�n materiaali', 'db_migration_v238', SYSDATE);

-- Merkinn�n pituus (valinnainen, Tien leveys -value LOCALIZED_STRING-taulussa)
INSERT INTO LOCALIZED_STRING (ID, VALUE_FI, CREATED_BY, CREATED_DATE)
	VALUES (primary_key_seq.nextval, 'Merkinn�n pituus', 'db_migration_v238', SYSDATE);
	
-- Merkinn�n leveys (valinnainen)
INSERT INTO LOCALIZED_STRING (ID, VALUE_FI, CREATED_BY, CREATED_DATE)
	VALUES (primary_key_seq.nextval, 'Merkinn�n leveys', 'db_migration_v238', SYSDATE);

-- Profiilimerkint� (valinnainen)
INSERT INTO LOCALIZED_STRING (ID, VALUE_FI, CREATED_BY, CREATED_DATE)
	VALUES (primary_key_seq.nextval, 'Profiilimerkint�', 'db_migration_v238', SYSDATE);

-- Jyrsitty (valinnainen)
INSERT INTO LOCALIZED_STRING (ID, VALUE_FI, CREATED_BY, CREATED_DATE)
	VALUES (primary_key_seq.nextval, 'Jyrsitty', 'db_migration_v238', SYSDATE);

-- Tila ja lis�tieto poistettu (molemmat on jo olemassa LOCALIZED_STRING-taulussa)


-- Add properties

INSERT INTO PROPERTY (ID, ASSET_TYPE_ID, PROPERTY_TYPE, REQUIRED, CREATED_BY, PUBLIC_ID, NAME_LOCALIZED_STRING_ID)
VALUES (primary_key_seq.nextval, 480, 'single_choice', 1, 'db_migration_v238', 'MT_regulatory_number',
        (SELECT ID FROM LOCALIZED_STRING WHERE VALUE_FI = 'Asetusnumero'));

INSERT INTO PROPERTY (ID, ASSET_TYPE_ID, PROPERTY_TYPE, REQUIRED, CREATED_BY, PUBLIC_ID, NAME_LOCALIZED_STRING_ID)
	VALUES (primary_key_seq.nextval, 480, 'text', 0, 'db_migration_v238', 'MT_road_marking_class', 
			(SELECT ID FROM LOCALIZED_STRING WHERE VALUE_FI = 'Tiemerkinn�n esitt�m�n liikennemerkin luokka'));
		
INSERT INTO PROPERTY (ID, ASSET_TYPE_ID, PROPERTY_TYPE, REQUIRED, CREATED_BY, PUBLIC_ID, NAME_LOCALIZED_STRING_ID)
	VALUES (primary_key_seq.nextval, 480, 'text', 0, 'db_migration_v238', 'MT_road_marking_value',
			(SELECT ID FROM LOCALIZED_STRING WHERE VALUE_FI = 'Tiemerkinn�n tai tiemerkinn�n esitt�m�n liikennemerkin arvo'));
		
INSERT INTO PROPERTY (ID, ASSET_TYPE_ID, PROPERTY_TYPE, REQUIRED, CREATED_BY, PUBLIC_ID, NAME_LOCALIZED_STRING_ID)
	VALUES (primary_key_seq.nextval, 480, 'number', 0, 'db_migration_v238', 'MT_road_marking_turn',
			(SELECT ID FROM LOCALIZED_STRING WHERE VALUE_FI = 'K��nn�s'));

INSERT INTO PROPERTY (ID, ASSET_TYPE_ID, PROPERTY_TYPE, REQUIRED, CREATED_BY, PUBLIC_ID, NAME_LOCALIZED_STRING_ID)
	VALUES (primary_key_seq.nextval, 480, 'number', 0, 'db_migration_v238', 'MT_road_marking_location',
			(SELECT ID FROM LOCALIZED_STRING WHERE VALUE_FI = 'Suhteellinen sijainti'));

INSERT INTO PROPERTY (ID, ASSET_TYPE_ID, PROPERTY_TYPE, REQUIRED, CREATED_BY, PUBLIC_ID, NAME_LOCALIZED_STRING_ID)
	VALUES (primary_key_seq.nextval, 480, 'single_choice', 0, 'db_migration_v238', 'MT_road_marking_condition',
			(SELECT ID FROM LOCALIZED_STRING WHERE VALUE_FI = 'Kunto'));
		
INSERT INTO PROPERTY (ID, ASSET_TYPE_ID, PROPERTY_TYPE, REQUIRED, CREATED_BY, PUBLIC_ID, NAME_LOCALIZED_STRING_ID)
	VALUES (primary_key_seq.nextval, 480, 'text', 0, 'db_migration_v238', 'MT_road_marking_quantity',
			(SELECT ID FROM LOCALIZED_STRING WHERE VALUE_FI = 'M��r�'));

INSERT INTO PROPERTY (ID, ASSET_TYPE_ID, PROPERTY_TYPE, REQUIRED, CREATED_BY, PUBLIC_ID, NAME_LOCALIZED_STRING_ID)
	VALUES (primary_key_seq.nextval, 480, 'single_choice', 0, 'db_migration_v238', 'MT_road_marking_material',
			(SELECT ID FROM LOCALIZED_STRING WHERE VALUE_FI = 'Merkinn�n materiaali'));
		
INSERT INTO PROPERTY (ID, ASSET_TYPE_ID, PROPERTY_TYPE, REQUIRED, CREATED_BY, PUBLIC_ID, NAME_LOCALIZED_STRING_ID)
	VALUES (primary_key_seq.nextval, 480, 'number', 0, 'db_migration_v238', 'MT_road_marking_length',
			(SELECT ID FROM LOCALIZED_STRING WHERE VALUE_FI = 'Merkinn�n pituus'));
		
INSERT INTO PROPERTY (ID, ASSET_TYPE_ID, PROPERTY_TYPE, REQUIRED, CREATED_BY, PUBLIC_ID, NAME_LOCALIZED_STRING_ID)
	VALUES (primary_key_seq.nextval, 480, 'number', 0, 'db_migration_v238', 'MT_road_marking_width',
			(SELECT ID FROM LOCALIZED_STRING WHERE VALUE_FI = 'Merkinn�n leveys'));
		
INSERT INTO PROPERTY (ID, ASSET_TYPE_ID, PROPERTY_TYPE, REQUIRED, CREATED_BY, PUBLIC_ID, NAME_LOCALIZED_STRING_ID)
	VALUES (primary_key_seq.nextval, 480, 'single_choice', 0, 'db_migration_v238', 'MT_road_marking_profile_mark',
			(SELECT ID FROM LOCALIZED_STRING WHERE VALUE_FI = 'Profiilimerkint�'));

INSERT INTO PROPERTY (ID, ASSET_TYPE_ID, PROPERTY_TYPE, REQUIRED, CREATED_BY, PUBLIC_ID, NAME_LOCALIZED_STRING_ID)
	VALUES (primary_key_seq.nextval, 480, 'single_choice', 0, 'db_migration_v238', 'MT_road_marking_milled',
			(SELECT ID FROM LOCALIZED_STRING WHERE VALUE_FI = 'Jyrsitty'));
		
INSERT INTO PROPERTY (ID, ASSET_TYPE_ID, PROPERTY_TYPE, REQUIRED, CREATED_BY, PUBLIC_ID, NAME_LOCALIZED_STRING_ID)
	VALUES (primary_key_seq.nextval, 480, 'long_text', 0, 'db_migration_v238', 'MT_road_marking_additional_information',
			(SELECT ID FROM LOCALIZED_STRING WHERE VALUE_FI = 'Lis�tieto'));
		
INSERT INTO PROPERTY (ID, ASSET_TYPE_ID, PROPERTY_TYPE, REQUIRED, CREATED_BY, PUBLIC_ID, NAME_LOCALIZED_STRING_ID)
	VALUES (primary_key_seq.nextval, 480, 'single_choice', 0, 'db_migration_v238', 'MT_road_marking_state',
			(SELECT ID FROM LOCALIZED_STRING WHERE VALUE_FI = 'Tila'));			
			

-- ENUMARATED VALUE ASETUSNUMERO
-- Tiemerkint�jen asetusnumero
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 1, 'M1 ajokaistanuoli', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_regulatory_number'));

INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 2, 'M2 ajokaistan vaihtamisnuoli', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_regulatory_number'));
			
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 3, 'M3 Pys�k�intialue', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_regulatory_number'));
			
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 4, 'M4 Keltanen reunamerkint�', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_regulatory_number'));
			
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 5, 'M5 Pys�ytt�misrajoitus', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_regulatory_number'));
			
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 6, 'M6 ohjausviiva', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_regulatory_number'));
			
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 7, 'M7 Jalankulkija', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_regulatory_number'));
			
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 8, 'M8 Py�r�ilij�', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_regulatory_number'));
			
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 9, 'M9 V�ist�misvelvollisuutta osoittava ennakkomerkint�', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_regulatory_number'));
			
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 10, 'M10 Stop-ennakkomerkint�', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_regulatory_number'));
		
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 11, 'M11 P-Merkint�', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_regulatory_number'));
		
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 12, 'M12 Invalidin ajoneuvo', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_regulatory_number'));
		
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 13, 'M13 BUS-merkint�', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_regulatory_number'));
		
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 14, 'M14 Taxi-merkint�', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_regulatory_number'));
		
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 15, 'M15 Lataus', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_regulatory_number'));
		
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 16, 'M16 Nopeusrajoitus', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_regulatory_number'));
		
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 17, 'M17 Tienumero', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_regulatory_number'));
		
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 18, 'M18 Risteysruudutus', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_regulatory_number'));
		
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 19, 'M19 Liikennemerkki', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_regulatory_number'));
		

		
-- ENUMERATED VALUE CONDITION
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
VALUES (primary_key_seq.nextval, 99, 'Ei tietoa', '', 'db_migration_v238',
        (SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_road_marking_condition'));
		
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 1,'Eritt�in huono', '', 'db_migration_v238',
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_road_marking_condition'));
		
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 2, 'Huono', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_road_marking_condition'));
		
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 3, 'Tyydytt�v�', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_road_marking_condition'));
		
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 4, 'Hyv�', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_road_marking_condition'));
		
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 5, 'Eritt�in hyv�', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_road_marking_condition'));
		
-- ENUMERATED VALUE MATERIAL
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
VALUES (primary_key_seq.nextval, 99, 'Ei tietoa', '', 'db_migration_v238',
        (SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_road_marking_material'));
       
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
VALUES (primary_key_seq.nextval, 1, 'Maali', '', 'db_migration_v238',
        (SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_road_marking_material'));
       
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
VALUES (primary_key_seq.nextval, 2, 'Massa', '', 'db_migration_v238',
        (SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_road_marking_material'));
		
-- ENUMERATED VALUE PROFILE MARK
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
VALUES (primary_key_seq.nextval, 99, 'Ei tietoa', '', 'db_migration_v238',
        (SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_road_marking_profile_mark'));
       
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
VALUES (primary_key_seq.nextval, 1, 'Ei', '', 'db_migration_v238',
        (SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_road_marking_profile_mark'));
       
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
VALUES (primary_key_seq.nextval, 2, 'Kyll�', '', 'db_migration_v238',
        (SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_road_marking_profile_mark'));
       
-- ENUMERATED VALUE JYRSITTY
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
VALUES (primary_key_seq.nextval, 99, 'Ei tietoa', '', 'db_migration_v238',
        (SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_road_marking_milled'));
       
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
VALUES (primary_key_seq.nextval, 1, 'Ei', '', 'db_migration_v238',
        (SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_road_marking_milled'));
       
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
VALUES (primary_key_seq.nextval, 2, 'Pintamerkint�', '', 'db_migration_v238',
        (SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_road_marking_milled'));
       
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
VALUES (primary_key_seq.nextval, 3, 'Siniaalto', '', 'db_migration_v238',
        (SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_road_marking_milled'));
       
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
VALUES (primary_key_seq.nextval, 4, 'Sylinterimerkint�', '', 'db_migration_v238',
        (SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_road_marking_milled'));
       
-- ENUMERATED VALUE STATE
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 1, 'Suunnitteilla ', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_road_marking_state'));
		
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 2, 'Rakenteilla ', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_road_marking_state'));
		
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 3, 'K�yt�ss� pysyv�sti (oletus)', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_road_marking_state'));
		
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 4, 'K�yt�ss� tilap�isesti', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_road_marking_state'));
		
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 5, 'Pois k�yt�ss� tilap�isesti', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_road_marking_state'));
		
INSERT INTO ENUMERATED_VALUE (ID, VALUE, NAME_FI, NAME_SV, CREATED_BY, PROPERTY_ID)
	VALUES (primary_key_seq.nextval, 6, 'Poistuva pysyv� laite', '', 'db_migration_v238', 
			(SELECT ID FROM PROPERTY WHERE PUBLIC_ID = 'MT_road_marking_state'));
