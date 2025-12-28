use MapDatabse;
create table MapTable(
	CityName varchar(255),
    CityID int,
    numberOfMaps int,
    SixMonthPrice double,
    OneTimePrice double
);
    
insert into MapTable values
("Haifa", 9 , 75.90, 25.90),
("Ness Ziona", 5, 60.90, 23.90),
("Tel Aviv", 3, 100.90, 35.90),
("Carmiel", 12, 12.90, 4.90),
("Holon", 2 , 35.90, 13.90);


 create table CityTable(
    CityName varchar(255),
	MapNum int,
    MapDescription varchar(255),
    LatestMapVersion varchar(255)
);