insert into service_user (id, username, configuration)
values (1, 'test', '{"zoom": 8, "east": 373560, "north": 6677676, "municipalityNumber": 235, "authorizedMunicipalities": [504,407,78,235,734,286,858,444,616,505,611,638,710,753,434,18,186,624]}}');
insert into service_user (id, username, configuration)
values (2, 'test2', '{"zoom": 8, "east": 373560, "north": 6677676, "municipalityNumber": 235, "authorizedMunicipalities": [504], "roles": ["operator"]}');
insert into service_user (id, username, configuration)
values (3, 'test49', '{"zoom": 8, "east": 373560, "north": 6677676, "municipalityNumber": 49, "authorizedMunicipalities": [49]}');
insert into service_user (id, username, configuration)
values (4, 'testpirkanmaa', '{"zoom": 8, "east": 328308, "north": 6822545, "municipalityNumber": 49, "authorizedMunicipalities": [504,407,78,235,734,286,858,444,616,505,611,638,710,753,434,18,49,186,624,837]}');
insert into service_user (id, username, configuration)
values (5, 'testnone', '{"zoom": 8, "east": 328308, "north": 6822545, "authorizedMunicipalities": []}');

insert into service_user (id, username, configuration)
values (6, 'testviewer', '{"zoom": 8, "east": 328308, "north": 6822545, "authorizedMunicipalities": [], "roles": ["viewer"]}');
