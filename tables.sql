drop table if exists t_user;

create table t_user ( id int unsigned auto_increment primary key,
		      name varchar(100),
       	     	      email varchar(100) unique key,
		      password varchar(100),
		      salt varchar(1000));

create table t_wall ( id int unsigned auto_increment primary key,
       	     	      user_id int unsigned references t_user.id,
		      comment longtext,
		      at date );
