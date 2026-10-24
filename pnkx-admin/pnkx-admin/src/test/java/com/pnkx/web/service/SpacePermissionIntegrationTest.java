package com.pnkx.web.service;

import static org.junit.jupiter.api.Assertions.*;

import com.pnkx.system.mapper.SysDataGroupMapper;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@EnabledIfEnvironmentVariable(
    named = "PNKX_TEST_MYSQL_URL",
    matches = "jdbc:mysql://127\\.0\\.0\\.1:.*?/pnkx_test.*")
class SpacePermissionIntegrationTest {
  JdbcTemplate db;
  SysDataGroupMapper mapper;

  @BeforeEach
  void setup() throws Exception {
    var ds =
        new DriverManagerDataSource(
            System.getenv("PNKX_TEST_MYSQL_URL"), "root", "pnkx_local_test_only");
    db = new JdbcTemplate(ds);
    db.execute("drop table if exists px_data_group_member");
    db.execute("drop table if exists px_data_group");
    db.execute(
        "create table px_data_group(id bigint primary key,status varchar(1),visibility_json json)");
    db.execute(
        "create table px_data_group_member(id bigint auto_increment primary key,group_id"
            + " bigint,user_id bigint,role varchar(16),status varchar(16))");
    db.update("insert into px_data_group values(1,'0','[\"todo\",\"shopping\"]')");
    db.update(
        "insert into px_data_group_member(group_id,user_id,role,status)"
            + " values(1,2,'owner','active'),(1,3,'viewer','active'),(1,4,'member','active')");
    var f = new SqlSessionFactoryBean();
    f.setDataSource(ds);
    f.setMapperLocations(
        new FileSystemResource(
            "../pnkx-system/src/main/resources/mapper/system/SysDataGroupMapper.xml"));
    mapper = new SqlSessionTemplate(f.getObject()).getMapper(SysDataGroupMapper.class);
  }

  @Test
  void viewerCanReadButCannotClaim() {
    assertTrue(mapper.selectVisibleUserIdsByModule(3L, "shopping").contains(2L));
    assertEquals(0, mapper.countSharedMembership(3L, 2L, "shopping"));
    assertEquals(1, mapper.countSharedMembership(4L, 2L, "shopping"));
  }

  @Test
  void leavingImmediatelyRevokesReadAndWrite() {
    db.update("update px_data_group_member set status='left' where user_id=4");
    assertFalse(mapper.selectVisibleUserIdsByModule(4L, "shopping").contains(2L));
    assertEquals(0, mapper.countSharedMembership(4L, 2L, "shopping"));
  }

  @Test
  void membershipDoesNotOpenPrivateDomains() {
    assertEquals(0, mapper.countSharedMembership(4L, 2L, "diary"));
    assertFalse(mapper.selectVisibleUserIdsByModule(4L, "diary").contains(2L));
    db.update("update px_data_group set status='1'");
    assertEquals(0, mapper.countSharedMembership(4L, 2L, "shopping"));
  }
}
